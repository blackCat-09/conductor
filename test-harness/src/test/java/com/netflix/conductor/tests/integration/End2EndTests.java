/**
 * Copyright 2016 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
/**
 *
 */
package com.netflix.conductor.tests.integration;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.netflix.conductor.bootstrap.BootstrapModule;
import com.netflix.conductor.bootstrap.ModulesProvider;
import com.netflix.conductor.client.http.MetadataClient;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.http.WorkflowClient;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.Task.Status;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;
import com.netflix.conductor.common.metadata.workflow.TaskType;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.common.run.SearchResult;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.common.run.Workflow.WorkflowStatus;
import com.netflix.conductor.common.run.WorkflowSummary;
import com.netflix.conductor.elasticsearch.ElasticSearchConfiguration;
import com.netflix.conductor.elasticsearch.EmbeddedElasticSearch;
import com.netflix.conductor.elasticsearch.EmbeddedElasticSearchProvider;
import com.netflix.conductor.jetty.server.JettyServer;
import com.netflix.conductor.tests.utils.TestEnvironment;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Viren
 */
public class End2EndTests extends AbstractEndToEndTest {

    private static TaskClient taskClient;
    private static WorkflowClient workflowClient;
    private static EmbeddedElasticSearch search;
    private static MetadataClient metadataClient;

    private static final int SERVER_PORT = 8080;

    @BeforeClass
    public static void setup() throws Exception {
        TestEnvironment.setup();
        System.setProperty(ElasticSearchConfiguration.EMBEDDED_PORT_PROPERTY_NAME, "9201");
        System.setProperty(ElasticSearchConfiguration.ELASTIC_SEARCH_URL_PROPERTY_NAME, "localhost:9301");

        Injector bootInjector = Guice.createInjector(new BootstrapModule());
        Injector serverInjector = Guice.createInjector(bootInjector.getInstance(ModulesProvider.class).get());

        search = serverInjector.getInstance(EmbeddedElasticSearchProvider.class).get().get();
        search.start();

        JettyServer server = new JettyServer(SERVER_PORT, false);
        server.start();

        taskClient = new TaskClient();
        taskClient.setRootURI("http://localhost:8080/api/");

        workflowClient = new WorkflowClient();
        workflowClient.setRootURI("http://localhost:8080/api/");

        metadataClient = new MetadataClient();
        metadataClient.setRootURI("http://localhost:8080/api/");
    }

    @AfterClass
    public static void teardown() throws Exception {
        TestEnvironment.teardown();
        search.stop();
    }

    @Override
    protected String startWorkflow(String workflowExecutionName, WorkflowDef workflowDefinition) {
        StartWorkflowRequest workflowRequest = new StartWorkflowRequest()
                .withName(workflowExecutionName)
                .withWorkflowDef(workflowDefinition);

        return workflowClient.startWorkflow(workflowRequest);
    }

    @Override
    protected Workflow getWorkflow(String workflowId, boolean includeTasks) {
        return workflowClient.getWorkflow(workflowId, includeTasks);
    }

    @Override
    protected TaskDef getTaskDefinition(String taskName) {
        return metadataClient.getTaskDef(taskName);
    }

    @Override
    protected void registerTaskDefinitions(List<TaskDef> taskDefinitionList) {
        metadataClient.registerTaskDefs(taskDefinitionList);
    }

    @Test
    public void testAll() throws Exception {
        List<TaskDef> definitions = createAndRegisterTaskDefinitions("t", 5);

        List<TaskDef> found = taskClient.getTaskDef().stream()
                .filter(taskDefinition -> taskDefinition.getName().startsWith("t"))
                .collect(Collectors.toList());
        assertNotNull(found);
        assertEquals(definitions.size(), found.size());

        WorkflowDef def = new WorkflowDef();
        def.setName("test");
        WorkflowTask t0 = new WorkflowTask();
        t0.setName("t0");
        t0.setWorkflowTaskType(TaskType.SIMPLE);
        t0.setTaskReferenceName("t0");

        WorkflowTask t1 = new WorkflowTask();
        t1.setName("t1");
        t1.setWorkflowTaskType(TaskType.SIMPLE);
        t1.setTaskReferenceName("t1");


        def.getTasks().add(t0);
        def.getTasks().add(t1);

        workflowClient.registerWorkflow(def);
        WorkflowDef workflowDefinitionFromSystem = workflowClient.getWorkflowDef(def.getName(), null);
        assertNotNull(workflowDefinitionFromSystem);
        assertEquals(def, workflowDefinitionFromSystem);

        String correlationId = "test_corr_id";
        String workflowId = workflowClient.startWorkflow(def.getName(), null, correlationId, new HashMap<>());
        assertNotNull(workflowId);

        Workflow wf = workflowClient.getWorkflow(workflowId, false);
        assertEquals(0, wf.getTasks().size());
        assertEquals(workflowId, wf.getWorkflowId());

        wf = workflowClient.getWorkflow(workflowId, true);
        assertNotNull(wf);
        assertEquals(WorkflowStatus.RUNNING, wf.getStatus());
        assertEquals(1, wf.getTasks().size());
        assertEquals(t0.getTaskReferenceName(), wf.getTasks().get(0).getReferenceTaskName());
        assertEquals(workflowId, wf.getWorkflowId());

        List<String> runningIds = workflowClient.getRunningWorkflow(def.getName(), def.getVersion());
        assertNotNull(runningIds);
        assertEquals(1, runningIds.size());
        assertEquals(workflowId, runningIds.get(0));

        List<Task> polled = taskClient.batchPollTasksByTaskType("non existing task", "test", 1, 100);
        assertNotNull(polled);
        assertEquals(0, polled.size());

        polled = taskClient.batchPollTasksByTaskType(t0.getName(), "test", 1, 100);
        assertNotNull(polled);
        assertEquals(1, polled.size());
        assertEquals(t0.getName(), polled.get(0).getTaskDefName());
        Task task = polled.get(0);

        Boolean acked = taskClient.ack(task.getTaskId(), "test");
        assertNotNull(acked);
        assertTrue(acked.booleanValue());

        task.getOutputData().put("key1", "value1");
        task.setStatus(Status.COMPLETED);
        taskClient.updateTask(new TaskResult(task), task.getTaskType());

        polled = taskClient.batchPollTasksByTaskType(t0.getName(), "test", 1, 100);
        assertNotNull(polled);
        assertTrue(polled.toString(), polled.isEmpty());

        wf = workflowClient.getWorkflow(workflowId, true);
        assertNotNull(wf);
        assertEquals(WorkflowStatus.RUNNING, wf.getStatus());
        assertEquals(2, wf.getTasks().size());
        assertEquals(t0.getTaskReferenceName(), wf.getTasks().get(0).getReferenceTaskName());
        assertEquals(t1.getTaskReferenceName(), wf.getTasks().get(1).getReferenceTaskName());
        assertEquals(Task.Status.COMPLETED, wf.getTasks().get(0).getStatus());
        assertEquals(Task.Status.SCHEDULED, wf.getTasks().get(1).getStatus());

        Task taskById = taskClient.getTaskDetails(task.getTaskId());
        assertNotNull(taskById);
        assertEquals(task.getTaskId(), taskById.getTaskId());


        List<Task> getTasks = taskClient.getPendingTasksByType(t0.getName(), null, 1);
        assertNotNull(getTasks);
        assertEquals(0, getTasks.size());        //getTasks only gives pending tasks


        getTasks = taskClient.getPendingTasksByType(t1.getName(), null, 1);
        assertNotNull(getTasks);
        assertEquals(1, getTasks.size());


        Task pending = taskClient.getPendingTaskForWorkflow(workflowId, t1.getTaskReferenceName());
        assertNotNull(pending);
        assertEquals(t1.getTaskReferenceName(), pending.getReferenceTaskName());
        assertEquals(workflowId, pending.getWorkflowInstanceId());

        Thread.sleep(1000);
        SearchResult<WorkflowSummary> searchResult = workflowClient.search("workflowType='" + def.getName() + "'");
        assertNotNull(searchResult);
        assertEquals(1, searchResult.getTotalHits());

        workflowClient.terminateWorkflow(workflowId, "terminate reason");
        wf = workflowClient.getWorkflow(workflowId, true);
        assertNotNull(wf);
        assertEquals(WorkflowStatus.TERMINATED, wf.getStatus());

        workflowClient.restart(workflowId);
        wf = workflowClient.getWorkflow(workflowId, true);
        assertNotNull(wf);
        assertEquals(WorkflowStatus.RUNNING, wf.getStatus());
        assertEquals(1, wf.getTasks().size());
    }

    @Test
    public void testMetadataWorkflowDefinition() {
        WorkflowDef def = new WorkflowDef();
        def.setName("testWorkflowDel");
        def.setVersion(1);
        WorkflowTask t0 = new WorkflowTask();
        t0.setName("t0");
        t0.setWorkflowTaskType(TaskType.SIMPLE);
        t0.setTaskReferenceName("t0");
        WorkflowTask t1 = new WorkflowTask();
        t1.setName("t1");
        t1.setWorkflowTaskType(TaskType.SIMPLE);
        t1.setTaskReferenceName("t1");
        def.getTasks().add(t0);
        def.getTasks().add(t1);
        metadataClient.registerWorkflowDef(def);
        metadataClient.unregisterWorkflowDef("testWorkflowDel", 1);
        try {
            WorkflowDef workflowDefinition = metadataClient.getWorkflowDef("testWorkflowDel", 1);
        } catch (UniformInterfaceException ue) {
            ClientResponse response = ue.getResponse();
            // TODO: fix this to NOT_FOUND once error handling is fixed
            assertEquals(404, response.getStatus());
        }
    }

}
