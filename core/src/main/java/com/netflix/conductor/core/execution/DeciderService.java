/*
 * Copyright 2016 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.conductor.core.execution;

import static com.netflix.conductor.common.metadata.tasks.Task.Status.COMPLETED_WITH_ERRORS;
import static com.netflix.conductor.common.metadata.tasks.Task.Status.IN_PROGRESS;
import static com.netflix.conductor.common.metadata.tasks.Task.Status.SCHEDULED;
import static com.netflix.conductor.common.metadata.tasks.Task.Status.SKIPPED;
import static com.netflix.conductor.common.metadata.tasks.Task.Status.TIMED_OUT;

import com.google.common.annotations.VisibleForTesting;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.Task.Status;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.metadata.workflow.TaskType;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.common.run.Workflow.WorkflowStatus;
import com.netflix.conductor.common.utils.ExternalPayloadStorage;
import com.netflix.conductor.core.execution.mapper.TaskMapper;
import com.netflix.conductor.core.execution.mapper.TaskMapperContext;
import com.netflix.conductor.core.utils.ExternalPayloadStorageUtils;
import com.netflix.conductor.core.utils.IDGenerator;
import com.netflix.conductor.core.utils.QueueUtils;
import com.netflix.conductor.dao.MetadataDAO;
import com.netflix.conductor.dao.QueueDAO;
import com.netflix.conductor.metrics.Monitors;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Viren
 * @author Vikram
 * Decider evaluates the state of the workflow by inspecting the current state along with the blueprint.
 * The result of the evaluation is either to schedule further tasks, complete/fail the workflow or do nothing.
 */
public class DeciderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeciderService.class);

    private final QueueDAO queueDAO;
    private final ParametersUtils parametersUtils;
    private final ExternalPayloadStorageUtils externalPayloadStorageUtils;
    private final MetadataDAO metadataDAO;

    private final Map<String, TaskMapper> taskMappers;

    private final Predicate<Task> isNonPendingTask = task -> !task.isRetried() && !task.getStatus().equals(SKIPPED) && !task.isExecuted();

    @Inject
    public DeciderService(ParametersUtils parametersUtils, QueueDAO queueDAO, MetadataDAO metadataDAO,
                          ExternalPayloadStorageUtils externalPayloadStorageUtils,
                          @Named("TaskMappers") Map<String, TaskMapper> taskMappers) {
        this.queueDAO = queueDAO;
        this.metadataDAO = metadataDAO;
        this.parametersUtils = parametersUtils;
        this.taskMappers = taskMappers;
        this.externalPayloadStorageUtils = externalPayloadStorageUtils;
    }

    //QQ public method validation of the input params
    public DeciderOutcome decide(Workflow workflow) throws TerminateWorkflowException {

        //In case of a new workflow the list of tasks will be empty.
        final List<Task> tasks = workflow.getTasks();
        // Filter the list of tasks and include only tasks that are not executed,
        // not marked to be skipped and not ready for rerun.
        // For a new workflow, the list of unprocessedTasks will be empty
        List<Task> unprocessedTasks = tasks.stream()
                .filter(t -> !t.getStatus().equals(SKIPPED) && !t.isExecuted())
                .collect(Collectors.toList());

        List<Task> tasksToBeScheduled = new LinkedList<>();
        if (unprocessedTasks.isEmpty()) {
            //this is the flow that the new workflow will go through
            tasksToBeScheduled = startWorkflow(workflow);
            if (tasksToBeScheduled == null) {
                tasksToBeScheduled = new LinkedList<>();
            }
        }
        // [decide]
        return decide(workflow, tasksToBeScheduled);
    }

    private DeciderOutcome decide(final Workflow workflow, List<Task> preScheduledTasks) throws TerminateWorkflowException {
        // Decider 选择结果类
        DeciderOutcome outcome = new DeciderOutcome();

        if (workflow.getStatus().equals(WorkflowStatus.PAUSED)) {
            LOGGER.debug("Workflow " + workflow.getWorkflowId() + " is paused");
            return outcome;
        }

        if (workflow.getStatus().isTerminal()) {
            //you cannot evaluate a terminal workflow
            LOGGER.debug("Workflow {} is already finished. Reason: {}", workflow, workflow.getReasonForIncompletion());
            return outcome;
        }

        // Filter the list of tasks and include only tasks that are not retried, not executed
        // marked to be skipped and not part of System tasks that is DECISION, FORK, JOIN
        // This list will be empty for a new workflow being started
        /**
         * 筛选列表，只留未重试、和未执行的任务。跳过不是系统任务的部分，比如 DECISION、FORK、JOIN
         * 对于一个正在启动的新工作流。这个任务列表为空。
         */
        List<Task> pendingTasks = workflow.getTasks()
                .stream()
                .filter(isNonPendingTask)
                .collect(Collectors.toList());

        // Get all the tasks that have not completed their lifecycle yet
        // This list will be empty for a new workflow
        Set<String> executedTaskRefNames = workflow.getTasks()
                .stream()
                .filter(Task::isExecuted)
                .map(Task::getReferenceTaskName)
                .collect(Collectors.toSet());

        Map<String, Task> tasksToBeScheduled = new LinkedHashMap<>();

        preScheduledTasks.forEach(preScheduledTask -> {
            tasksToBeScheduled.put(preScheduledTask.getReferenceTaskName(), preScheduledTask);
        });

        // A new workflow does not enter this code branch
        for (Task pendingTask : pendingTasks) {

            if (SystemTaskType.is(pendingTask.getTaskType()) && !pendingTask.getStatus().isTerminal()) {
                tasksToBeScheduled.putIfAbsent(pendingTask.getReferenceTaskName(), pendingTask);
                executedTaskRefNames.remove(pendingTask.getReferenceTaskName());
            }

            Optional<TaskDef> taskDefinition = pendingTask.getTaskDefinition();
            if (!taskDefinition.isPresent()) {
               taskDefinition = Optional.ofNullable(workflow.getWorkflowDefinition().getTaskByRefName(pendingTask.getReferenceTaskName()))
                       .map(WorkflowTask::getTaskDefinition);
            }

            if (taskDefinition.isPresent()) {
                checkForTimeout(taskDefinition.get(), pendingTask);
                // If the task has not been updated for "responseTimeoutSeconds" then mark task as TIMED_OUT
                if (isResponseTimedOut(taskDefinition.get(), pendingTask)) {
                    timeoutTask(taskDefinition.get(), pendingTask);
                }
            }

            if (!pendingTask.getStatus().isSuccessful()) {
                WorkflowTask workflowTask = pendingTask.getWorkflowTask();
                if (workflowTask == null) {
                    workflowTask = workflow.getWorkflowDefinition().getTaskByRefName(pendingTask.getReferenceTaskName());
                }

                Optional<Task> retryTask = retry(taskDefinition.orElse(null), workflowTask, pendingTask, workflow);
                if (retryTask.isPresent()) {
                    tasksToBeScheduled.put(retryTask.get().getReferenceTaskName(), retryTask.get());
                    executedTaskRefNames.remove(retryTask.get().getReferenceTaskName());
                    outcome.tasksToBeUpdated.add(pendingTask);
                } else {
                    pendingTask.setStatus(COMPLETED_WITH_ERRORS);
                }
            }
            /**
             * 如果任务没有执行、也没有正在重试状态。并且当前以及终止。则通过
             * getNextTask 方法获取下一个任务。
             */
            if (!pendingTask.isExecuted() && !pendingTask.isRetried() && pendingTask.getStatus().isTerminal()) {
                pendingTask.setExecuted(true);
                // 获取下一个任务。【getNextTask】
                List<Task> nextTasks = getNextTask(workflow, pendingTask);
                nextTasks.forEach(nextTask -> tasksToBeScheduled.putIfAbsent(nextTask.getReferenceTaskName(), nextTask));
                outcome.tasksToBeUpdated.add(pendingTask);
                LOGGER.debug("Scheduling Tasks from {}, next = {} for workflowId: {}", pendingTask.getTaskDefName(),
                        nextTasks.stream()
                                .map(Task::getTaskDefName)
                                .collect(Collectors.toList()),
                        workflow.getWorkflowId());
            }
        }

        //All the tasks that need to scheduled are added to the outcome, in case of
        List<Task> unScheduledTasks = tasksToBeScheduled.values().stream()
                .filter(task -> !executedTaskRefNames.contains(task.getReferenceTaskName()))
                .collect(Collectors.toList());
        if (!unScheduledTasks.isEmpty()) {
            LOGGER.debug("Scheduling Tasks: {} for workflow: {}", unScheduledTasks.stream()
                            .map(Task::getTaskDefName)
                            .collect(Collectors.toList()),
                    workflow.getWorkflowId());
            outcome.tasksToBeScheduled.addAll(unScheduledTasks);
        }
        if (outcome.tasksToBeScheduled.isEmpty() && checkForWorkflowCompletion(workflow)) {
            LOGGER.debug("Marking workflow: {} as complete.", workflow);
            outcome.isComplete = true;
        }

        return outcome;
    }

    private List<Task> startWorkflow(Workflow workflow) throws TerminateWorkflowException {
        final WorkflowDef workflowDef = workflow.getWorkflowDefinition();

        LOGGER.debug("Starting workflow: {}", workflow);

        //The tasks will be empty in case of new workflow
        List<Task> tasks = workflow.getTasks();
        // Check if the workflow is a re-run case or if it is a new workflow execution
        if (workflow.getReRunFromWorkflowId() == null || tasks.isEmpty()) {

            if (workflowDef.getTasks().isEmpty()) {
                throw new TerminateWorkflowException("No tasks found to be executed", WorkflowStatus.COMPLETED);
            }

            WorkflowTask taskToSchedule = workflowDef.getTasks().get(0); //Nothing is running yet - so schedule the first task
            //Loop until a non-skipped task is found
            while (isTaskSkipped(taskToSchedule, workflow)) {
                taskToSchedule = workflowDef.getNextTask(taskToSchedule.getTaskReferenceName());
            }

            //In case of a new workflow, the first non-skippable task will be scheduled
            return getTasksToBeScheduled(workflow, taskToSchedule, 0);
        }

        // Get the first task to schedule
        Task rerunFromTask = tasks.stream()
                .findFirst()
                .map(task -> {
                    task.setStatus(SCHEDULED);
                    task.setRetried(true);
                    task.setRetryCount(0);
                    return task;
                })
                .orElseThrow(() -> {
                    String reason = String.format("The workflow %s is marked for re-run from %s but could not find the starting task",
                            workflow.getWorkflowId(), workflow.getReRunFromWorkflowId());
                    return new TerminateWorkflowException(reason);
                });

        return Collections.singletonList(rerunFromTask);
    }

    /**
     * Updates the workflow output.
     *
     * @param workflow the workflow instance
     * @param task     if not null, the output of this task will be copied to workflow output if no output parameters are specified in the workflow defintion
     *                 if null, the output of the last task in the workflow will be copied to workflow output of no output parameters are specified in the workflow definition
     */
    void updateWorkflowOutput(final Workflow workflow, @Nullable Task task) {
        List<Task> allTasks = workflow.getTasks();
        if (allTasks.isEmpty()) {
            return;
        }

        Task last = Optional.ofNullable(task).orElse(allTasks.get(allTasks.size() - 1));

        WorkflowDef workflowDef = workflow.getWorkflowDefinition();
        Map<String, Object> output;
        if (workflowDef.getOutputParameters() != null && !workflowDef.getOutputParameters().isEmpty()) {
            Workflow workflowInstance = populateWorkflowAndTaskData(workflow);
            output = parametersUtils.getTaskInput(workflowDef.getOutputParameters(), workflowInstance, null, null);
        } else if (StringUtils.isNotBlank(last.getExternalOutputPayloadStoragePath())) {
            output = externalPayloadStorageUtils.downloadPayload(last.getExternalOutputPayloadStoragePath());
            Monitors.recordExternalPayloadStorageUsage(last.getTaskDefName(), ExternalPayloadStorage.Operation.READ.toString(), ExternalPayloadStorage.PayloadType.TASK_OUTPUT.toString());
        } else {
            output = last.getOutputData();
        }

        workflow.setOutput(output);
        externalPayloadStorageUtils.verifyAndUpload(workflow, ExternalPayloadStorage.PayloadType.WORKFLOW_OUTPUT);
    }

    private boolean checkForWorkflowCompletion(final Workflow workflow) throws TerminateWorkflowException {
        List<Task> allTasks = workflow.getTasks();
        if (allTasks.isEmpty()) {
            return false;
        }

        Map<String, Status> taskStatusMap = new HashMap<>();
        workflow.getTasks().forEach(task -> taskStatusMap.put(task.getReferenceTaskName(), task.getStatus()));

        List<WorkflowTask> workflowTasks = workflow.getWorkflowDefinition().getTasks();
        boolean allCompletedSuccessfully = workflowTasks.stream()
                .parallel()
                .allMatch(wftask -> {
                    Status status = taskStatusMap.get(wftask.getTaskReferenceName());
                    return status != null && status.isSuccessful() && status.isTerminal();
                });

        boolean noPendingTasks = taskStatusMap.values()
                .stream()
                .allMatch(Status::isTerminal);

        boolean noPendingSchedule = workflow.getTasks().stream()
                .parallel()
                .noneMatch(wftask -> {
                    String next = getNextTasksToBeScheduled(workflow, wftask);
                    return next != null && !taskStatusMap.containsKey(next);
                });

        return allCompletedSuccessfully && noPendingTasks && noPendingSchedule;
    }

    private List<Task> getNextTask(Workflow workflow, Task task) {
        final WorkflowDef workflowDef = workflow.getWorkflowDefinition();

        // Get the following task after the last completed task
        if (SystemTaskType.is(task.getTaskType()) && SystemTaskType.DECISION.name().equals(task.getTaskType())) {
            if (task.getInputData().get("hasChildren") != null) {
                return Collections.emptyList();
            }
        }

        String taskReferenceName = task.getReferenceTaskName();
        /**
         * 根据任务名称从工作流定义中获取下一个任务，可能是分支、也可能是并行。分支等判断逻辑
         */
        WorkflowTask taskToSchedule = workflowDef.getNextTask(taskReferenceName);
        while (isTaskSkipped(taskToSchedule, workflow)) {
            taskToSchedule = workflowDef.getNextTask(taskToSchedule.getTaskReferenceName());
        }
        if (taskToSchedule != null) {
            // [getTasksToBeScheduled]
            return getTasksToBeScheduled(workflow, taskToSchedule, 0);
        }

        return Collections.emptyList();
    }

    private String getNextTasksToBeScheduled(Workflow workflow, Task task) {
        final WorkflowDef def = workflow.getWorkflowDefinition();

        String taskReferenceName = task.getReferenceTaskName();
        WorkflowTask taskToSchedule = def.getNextTask(taskReferenceName);
        while (isTaskSkipped(taskToSchedule, workflow)) {
            taskToSchedule = def.getNextTask(taskToSchedule.getTaskReferenceName());
        }
        return taskToSchedule == null ? null : taskToSchedule.getTaskReferenceName();
    }

    @VisibleForTesting
    Optional<Task> retry(TaskDef taskDefinition, WorkflowTask workflowTask, Task task, Workflow workflow) throws TerminateWorkflowException {

        int retryCount = task.getRetryCount();

        if (taskDefinition == null) {
            taskDefinition = metadataDAO.getTaskDef(task.getTaskDefName());
        }

        if (!task.getStatus().isRetriable() || SystemTaskType.isBuiltIn(task.getTaskType()) || taskDefinition == null || taskDefinition.getRetryCount() <= retryCount) {
            if (workflowTask != null && workflowTask.isOptional()) {
                return Optional.empty();
            }
            WorkflowStatus status = task.getStatus().equals(TIMED_OUT) ? WorkflowStatus.TIMED_OUT : WorkflowStatus.FAILED;
            updateWorkflowOutput(workflow, task);
            throw new TerminateWorkflowException(task.getReasonForIncompletion(), status, task);
        }

        // retry... - but not immediately - put a delay...
        int startDelay = taskDefinition.getRetryDelaySeconds();
        switch (taskDefinition.getRetryLogic()) {
            case FIXED:
                startDelay = taskDefinition.getRetryDelaySeconds();
                break;
            case EXPONENTIAL_BACKOFF:
                startDelay = taskDefinition.getRetryDelaySeconds() * (1 + task.getRetryCount());
                break;
        }

        task.setRetried(true);

        Task rescheduled = task.copy();
        rescheduled.setStartDelayInSeconds(startDelay);
        rescheduled.setCallbackAfterSeconds(startDelay);
        rescheduled.setRetryCount(task.getRetryCount() + 1);
        rescheduled.setRetried(false);
        rescheduled.setTaskId(IDGenerator.generate());
        rescheduled.setRetriedTaskId(task.getTaskId());
        rescheduled.setStatus(SCHEDULED);
        rescheduled.setPollCount(0);
        rescheduled.setInputData(new HashMap<>());
        rescheduled.getInputData().putAll(task.getInputData());
        rescheduled.setReasonForIncompletion(null);

        if (StringUtils.isNotBlank(task.getExternalInputPayloadStoragePath())) {
            rescheduled.setExternalInputPayloadStoragePath(task.getExternalInputPayloadStoragePath());
        } else {
            rescheduled.getInputData().putAll(task.getInputData());
        }
        if (workflowTask != null && workflow.getSchemaVersion() > 1) {
            Workflow workflowInstance = populateWorkflowAndTaskData(workflow);
            Map<String, Object> taskInput = parametersUtils.getTaskInputV2(workflowTask.getInputParameters(), workflowInstance, rescheduled.getTaskId(), taskDefinition);
            rescheduled.getInputData().putAll(taskInput);
        }
        externalPayloadStorageUtils.verifyAndUpload(rescheduled, ExternalPayloadStorage.PayloadType.TASK_INPUT);
        //for the schema version 1, we do not have to recompute the inputs
        return Optional.of(rescheduled);
    }

    /**
     * Populates the workflow input data and the tasks input/output data if stored in external payload storage.
     * This method creates a deep copy of the workflow instance where the payloads will be stored after downloading from external payload storage.
     *
     * @param workflow the workflow for which the data needs to be populated
     * @return a copy of the workflow with the payload data populated
     */
    @VisibleForTesting
    Workflow populateWorkflowAndTaskData(Workflow workflow) {
        Workflow workflowInstance = workflow.copy();

        if (StringUtils.isNotBlank(workflow.getExternalInputPayloadStoragePath())) {
            // download the workflow input from external storage here and plug it into the workflow
            Map<String, Object> workflowInputParams = externalPayloadStorageUtils.downloadPayload(workflow.getExternalInputPayloadStoragePath());
            Monitors.recordExternalPayloadStorageUsage(workflow.getWorkflowName(), ExternalPayloadStorage.Operation.READ.toString(), ExternalPayloadStorage.PayloadType.WORKFLOW_INPUT.toString());
            workflowInstance.setInput(workflowInputParams);
            workflowInstance.setExternalInputPayloadStoragePath(null);
        }

        workflowInstance.getTasks().stream()
                .filter(task -> StringUtils.isNotBlank(task.getExternalInputPayloadStoragePath()) || StringUtils.isNotBlank(task.getExternalOutputPayloadStoragePath()))
                .forEach(task -> {
                    if (StringUtils.isNotBlank(task.getExternalOutputPayloadStoragePath())) {
                        task.setOutputData(externalPayloadStorageUtils.downloadPayload(task.getExternalOutputPayloadStoragePath()));
                        Monitors.recordExternalPayloadStorageUsage(task.getTaskDefName(), ExternalPayloadStorage.Operation.READ.toString(), ExternalPayloadStorage.PayloadType.TASK_OUTPUT.toString());
                        task.setExternalOutputPayloadStoragePath(null);
                    }
                    if (StringUtils.isNotBlank(task.getExternalInputPayloadStoragePath())) {
                        task.setInputData(externalPayloadStorageUtils.downloadPayload(task.getExternalInputPayloadStoragePath()));
                        Monitors.recordExternalPayloadStorageUsage(task.getTaskDefName(), ExternalPayloadStorage.Operation.READ.toString(), ExternalPayloadStorage.PayloadType.TASK_INPUT.toString());
                        task.setExternalInputPayloadStoragePath(null);
                    }
                });
        return workflowInstance;
    }

    @VisibleForTesting
    void checkForTimeout(TaskDef taskDef, Task task) {

        if (taskDef == null) {
            LOGGER.warn("missing task type " + task.getTaskDefName() + ", workflowId=" + task.getWorkflowInstanceId());
            return;
        }
        if (task.getStatus().isTerminal() || taskDef.getTimeoutSeconds() <= 0 || !task.getStatus().equals(IN_PROGRESS)) {
            return;
        }

        long timeout = 1000L * taskDef.getTimeoutSeconds();
        long now = System.currentTimeMillis();
        long elapsedTime = now - (task.getStartTime() + ((long) task.getStartDelayInSeconds() * 1000L));

        if (elapsedTime < timeout) {
            return;
        }

        String reason = "Task timed out after " + elapsedTime + " millisecond.  Timeout configured as " + timeout;
        Monitors.recordTaskTimeout(task.getTaskDefName());

        switch (taskDef.getTimeoutPolicy()) {
            case ALERT_ONLY:
                return;
            case RETRY:
                task.setStatus(TIMED_OUT);
                task.setReasonForIncompletion(reason);
                return;
            case TIME_OUT_WF:
                task.setStatus(TIMED_OUT);
                task.setReasonForIncompletion(reason);
                throw new TerminateWorkflowException(reason, WorkflowStatus.TIMED_OUT, task);
        }
    }

    @VisibleForTesting
    boolean isResponseTimedOut(TaskDef taskDefinition, Task task) {
        if (taskDefinition == null) {
            LOGGER.warn("missing task type : {}, workflowId= {}", task.getTaskDefName(), task.getWorkflowInstanceId());
            return false;
        }
        if (task.getStatus().isTerminal() || !task.getStatus().equals(IN_PROGRESS) || taskDefinition.getResponseTimeoutSeconds() == 0) {
            return false;
        }

        if (queueDAO.exists(QueueUtils.getQueueName(task), task.getTaskId())) {
            // this task is present in the queue
            // this means that it has been updated with callbackAfterSeconds and is not being executed in a worker
            return false;
        }

        LOGGER.debug("Evaluating responseTimeOut for Task: {}, with Task Definition: {}", task, taskDefinition);

        long responseTimeout = 1000L * taskDefinition.getResponseTimeoutSeconds();
        long now = System.currentTimeMillis();
        long noResponseTime = now - task.getUpdateTime();

        if (noResponseTime < responseTimeout) {
            LOGGER.debug("Current responseTime: {} has not exceeded the configured responseTimeout of {} " +
                    "for the Task: {} with Task Definition: {}", noResponseTime, responseTimeout, task, taskDefinition);
            return false;
        }

        Monitors.recordTaskResponseTimeout(task.getTaskDefName());
        return true;
    }

    private void timeoutTask(TaskDef taskDef, Task task) {
        String reason = "responseTimeout: " + taskDef.getResponseTimeoutSeconds() + " exceeded for the taskId: " + task.getTaskId() + " with Task Definition: " + task.getTaskDefName();
        LOGGER.debug(reason);
        task.setStatus(TIMED_OUT);
        task.setReasonForIncompletion(reason);
    }

    public List<Task> getTasksToBeScheduled(Workflow workflow,
                                            WorkflowTask taskToSchedule, int retryCount) {
        return getTasksToBeScheduled(workflow, taskToSchedule, retryCount, null);
    }

    public List<Task> getTasksToBeScheduled(Workflow workflow,
                                            WorkflowTask taskToSchedule, int retryCount, String retriedTaskId) {
        /**
         * 作用，如果任务的输入数据存储在外部存储介质中。则要先从外部存储介质中下载任务的输入数据。然后
         * 深拷贝到 workflow 实例中返回。
         */
        workflow = populateWorkflowAndTaskData(workflow);
        Map<String, Object> input = parametersUtils.getTaskInput(taskToSchedule.getInputParameters(),
                workflow, null, null);

        TaskType taskType = TaskType.USER_DEFINED;
        String type = taskToSchedule.getType();
        if (TaskType.isSystemTask(type)) {
            taskType = TaskType.valueOf(type);
        }

        // get tasks already scheduled (in progress/terminal) for  this workflow instance
        // 获取工作流中正在运行的任务列表。
        List<String> tasksInWorkflow = workflow.getTasks().stream()
                .filter(runningTask -> runningTask.getStatus().equals(Status.IN_PROGRESS) || runningTask.getStatus().isTerminal())
                .map(Task::getReferenceTaskName)
                .collect(Collectors.toList());

        String taskId = IDGenerator.generate();
        TaskMapperContext taskMapperContext = TaskMapperContext.newBuilder()
                .withWorkflowDefinition(workflow.getWorkflowDefinition())
                .withWorkflowInstance(workflow)
                .withTaskDefinition(taskToSchedule.getTaskDefinition())
                .withTaskToSchedule(taskToSchedule)
                .withTaskInput(input)
                .withRetryCount(retryCount)
                .withRetryTaskId(retriedTaskId)
                .withTaskId(taskId)
                .withDeciderService(this)
                .build();

        // for static forks, each branch of the fork creates a join task upon completion
        // for dynamic forks, a join task is created with the fork and also with each branch of the fork
        // a new task must only be scheduled if a task with the same reference name is not already in this workflow instance
        List<Task> tasks = taskMappers.get(taskType.name()).getMappedTasks(taskMapperContext).stream()
                .filter(task -> !tasksInWorkflow.contains(task.getReferenceTaskName()))
                .collect(Collectors.toList());
        tasks.forEach(task -> externalPayloadStorageUtils.verifyAndUpload(task, ExternalPayloadStorage.PayloadType.TASK_INPUT));
        return tasks;
    }

    private boolean isTaskSkipped(WorkflowTask taskToSchedule, Workflow workflow) {
        try {
            boolean isTaskSkipped = false;
            if (taskToSchedule != null) {
                Task t = workflow.getTaskByRefName(taskToSchedule.getTaskReferenceName());
                if (t == null) {
                    isTaskSkipped = false;
                } else if (t.getStatus().equals(SKIPPED)) {
                    isTaskSkipped = true;
                }
            }
            return isTaskSkipped;
        } catch (Exception e) {
            throw new TerminateWorkflowException(e.getMessage());
        }

    }


    public static class DeciderOutcome {

        List<Task> tasksToBeScheduled = new LinkedList<>();

        List<Task> tasksToBeUpdated = new LinkedList<>();

        List<Task> tasksToBeRequeued = new LinkedList<>();

        boolean isComplete;

        private DeciderOutcome() {
        }

    }
}
