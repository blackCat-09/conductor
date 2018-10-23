/**
 * Copyright 2017 Netflix, Inc.
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
package com.netflix.conductor.common.metadata.events;

import com.google.protobuf.Any;
import com.github.vmg.protogen.annotations.*;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Viren
 * Defines an event handler
 */
@ProtoMessage
public class EventHandler {

	@ProtoField(id = 1)
	private String name;

	@ProtoField(id = 2)
	private String event;

	@ProtoField(id = 3)
	private String condition;
	
	@ProtoField(id = 4)
	private List<Action> actions = new LinkedList<>();

	@ProtoField(id = 5)
	private boolean active;
	
	public EventHandler() {
		
	}

	/**
	 * @return the name MUST be unique within a conductor instance
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 * 
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the event
	 */
	public String getEvent() {
		return event;
	}

	/**
	 * @param event the event to set
	 * 
	 */
	public void setEvent(String event) {
		this.event = event;
	}

	/**
	 * @return the condition
	 */
	public String getCondition() {
		return condition;
	}

	/**
	 * @param condition the condition to set
	 * 
	 */
	public void setCondition(String condition) {
		this.condition = condition;
	}

	/**
	 * @return the actions
	 */
	public List<Action> getActions() {
		return actions;
	}

	/**
	 * @param actions the actions to set
	 * 
	 */
	public void setActions(List<Action> actions) {
		this.actions = actions;
	}

	/**
	 * @return the active
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * @param active if set to false, the event handler is deactivated
	 * 
	 */
	public void setActive(boolean active) {
		this.active = active;
	}


	@ProtoMessage
	public static class Action {

		@ProtoEnum
		public enum Type {
			start_workflow, complete_task, fail_task
		}

		@ProtoField(id = 1)
		private Type action;

		@ProtoField(id = 2)
		private StartWorkflow startWorkflow;

		@ProtoField(id = 3)
		private TaskDetails completeTask;

		@ProtoField(id = 4)
		private TaskDetails failTask;

		@ProtoField(id = 5)
		private boolean expandInlineJson;

		/**
		 * @return the action
		 */
		public Type getAction() {
			return action;
		}

		/**
		 * @param action the action to set
		 * 
		 */
		public void setAction(Type action) {
			this.action = action;
		}

		/**
		 * @return the startWorkflow
		 */
		public StartWorkflow getStartWorkflow() {
			return startWorkflow;
		}

		/**
		 * @param startWorkflow the startWorkflow to set
		 * 
		 */
		public void setStartWorkflow(StartWorkflow startWorkflow) {
			this.startWorkflow = startWorkflow;
		}

		/**
		 * @return the completeTask
		 */
		public TaskDetails getCompleteTask() {
			return completeTask;
		}

		/**
		 * @param completeTask the completeTask to set
		 * 
		 */
		public void setCompleteTask(TaskDetails completeTask) {
			this.completeTask = completeTask;
		}

		/**
		 * @return the failTask
		 */
		public TaskDetails getFailTask() {
			return failTask;
		}

		/**
		 * @param failTask the failTask to set
		 * 
		 */
		public void setFailTask(TaskDetails failTask) {
			this.failTask = failTask;
		}
		
		/**
		 * 
		 * @param expandInlineJson when set to true, the in-lined JSON strings are expanded to a full json document
		 */
		public void setExpandInlineJson(boolean expandInlineJson) {
			this.expandInlineJson = expandInlineJson;
		}
		
		/**
		 * 
		 * @return true if the json strings within the payload should be expanded.
		 */
		public boolean isExpandInlineJson() {
			return expandInlineJson;
		}
	}

	@ProtoMessage
	public static class TaskDetails {

		@ProtoField(id = 1)
		private String workflowId;

		@ProtoField(id = 2)
		private String taskRefName;

		@ProtoField(id = 3)
		private Map<String, Object> output = new HashMap<>();

		@ProtoField(id = 4)
		private Any outputMessage;

		/**
		 * @return the workflowId
		 */
		public String getWorkflowId() {
			return workflowId;
		}

		/**
		 * @param workflowId the workflowId to set
		 * 
		 */
		public void setWorkflowId(String workflowId) {
			this.workflowId = workflowId;
		}

		/**
		 * @return the taskRefName
		 */
		public String getTaskRefName() {
			return taskRefName;
		}

		/**
		 * @param taskRefName the taskRefName to set
		 * 
		 */
		public void setTaskRefName(String taskRefName) {
			this.taskRefName = taskRefName;
		}

		/**
		 * @return the output
		 */
		public Map<String, Object> getOutput() {
			return output;
		}

		/**
		 * @param output the output to set
		 * 
		 */
		public void setOutput(Map<String, Object> output) {
			this.output = output;
		}

		public Any getOutputMessage() {
			return outputMessage;
		}

		public void setOutputMessage(Any outputMessage) {
			this.outputMessage = outputMessage;
		}
	}

	@ProtoMessage
	public static class StartWorkflow {

		@ProtoField(id = 1)
		private String name;

		@ProtoField(id = 2)
		private Integer version;

		@ProtoField(id = 3)
		private String correlationId;

		@ProtoField(id = 4)
		private Map<String, Object> input = new HashMap<>();

		@ProtoField(id = 5)
		private Any inputMessage;

		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * @param name the name to set
		 * 
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * @return the version
		 */
		public Integer getVersion() {
			return version;
		}

		/**
		 * @param version the version to set
		 * 
		 */
		public void setVersion(Integer version) {
			this.version = version;
		}

		
		/**
		 * @return the correlationId
		 */
		public String getCorrelationId() {
			return correlationId;
		}

		/**
		 * @param correlationId the correlationId to set
		 * 
		 */
		public void setCorrelationId(String correlationId) {
			this.correlationId = correlationId;
		}

		/**
		 * @return the input
		 */
		public Map<String, Object> getInput() {
			return input;
		}

		/**
		 * @param input the input to set
		 * 
		 */
		public void setInput(Map<String, Object> input) {
			this.input = input;
		}

		public Any getInputMessage() {
			return inputMessage;
		}

		public void setInputMessage(Any inputMessage) {
			this.inputMessage = inputMessage;
		}
	}
	
}
