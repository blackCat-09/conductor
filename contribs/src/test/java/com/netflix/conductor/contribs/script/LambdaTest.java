package com.netflix.conductor.contribs.script;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.contribs.lambda.LambdaTask;
import com.netflix.conductor.core.execution.WorkflowExecutor;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class LambdaTest {

    private Workflow workflow = new Workflow();
    private WorkflowExecutor executor = mock(WorkflowExecutor.class);


    @Test
    public void start() throws Exception {
        LambdaTask lambda = new LambdaTask();

        Map inputObj = new HashMap();
        inputObj.put("a",1);

        /**
         * test for scriptExpression == null
         */
        Task task = new Task();
        task.getInputData().put("input", inputObj);
        lambda.start(workflow, task, executor);
        assertEquals(Task.Status.FAILED, task.getStatus());

        /**
         * test for normal
         */
        task = new Task();
        task.getInputData().put("input", inputObj);
        task.getInputData().put("scriptExpression", "if ($.input.a==1){return 1}else{return 0 } ");
        lambda.start(workflow, task, executor);
        assertEquals(Task.Status.COMPLETED, task.getStatus());
        assertEquals(task.getOutputData().toString(), "{result=1}");

        /**
         * test for scriptExpression ScriptException
         */
        task = new Task();
        task.getInputData().put("input", inputObj);
        task.getInputData().put("scriptExpression", "if ($.a.size==1){return 1}else{return 0 } ");
        lambda.start(workflow, task, executor);
        assertEquals(Task.Status.FAILED, task.getStatus());
    }

}