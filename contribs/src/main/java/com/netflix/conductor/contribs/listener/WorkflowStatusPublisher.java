/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 *
 */
package com.netflix.conductor.contribs.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.common.run.WorkflowSummary;
import com.netflix.conductor.core.config.Configuration;
import com.netflix.conductor.core.events.queue.Message;
import com.netflix.conductor.core.execution.WorkflowStatusListener;
import com.netflix.conductor.dao.QueueDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collections;


public class WorkflowStatusPublisher implements WorkflowStatusListener {

    private static final Logger LOG = LoggerFactory.getLogger(WorkflowStatusPublisher.class);
    private final QueueDAO queueDAO;
    private final ObjectMapper objectMapper;
    private final Configuration config;
    private final String publisherQueue;

    @Inject
    public WorkflowStatusPublisher(QueueDAO queueDAO, ObjectMapper objectMapper, Configuration config) {
        this.queueDAO = queueDAO;
        this.objectMapper = objectMapper;
        this.config = config;
        this.publisherQueue = config.getProperty("workflow.status.publisher.queue.name", "_callbackQueue");
    }

    @Override
    public void onWorkflowCompleted(Workflow workflow) {
        LOG.info("Publishing callback of workflow {} on completion ", workflow.getWorkflowId());
        queueDAO.push(publisherQueue, Collections.singletonList(workflowToMessage(workflow)));
    }

    @Override
    public void onWorkflowTerminated(Workflow workflow) {
        LOG.info("Publishing callback of workflow {} on termination", workflow.getWorkflowId());
        queueDAO.push(publisherQueue, Collections.singletonList(workflowToMessage(workflow)));
    }

    private Message workflowToMessage(Workflow workflow) {
        String jsonWfSummary;
        WorkflowSummary summary = new WorkflowSummary(workflow);
        try {
            jsonWfSummary = objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to convert WorkflowSummary: {} to String. Exception: {}", summary, e);
            throw new RuntimeException(e);
        }
        return new Message(workflow.getWorkflowId(), jsonWfSummary, null);
    }

}
