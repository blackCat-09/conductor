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
package com.netflix.conductor.config;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import com.netflix.conductor.mysql.MySQLConfiguration;

import java.util.Map;

import java.util.Map;

/**
 * @author Viren
 *
 */
public class TestConfiguration implements MySQLConfiguration {

	private Map<String, String> testProperties = Maps.newHashMap(ImmutableMap.of("test", "dummy"));

	@Override
	public int getSweepFrequency() {
		return 1;
	}

	@Override
	public boolean disableSweep() {
		return false;
	}

	@Override
	public boolean disableAsyncWorkers() {
		return false;
	}

	@Override
	public String getServerId() {
		return "server_id";
	}

	@Override
	public String getEnvironment() {
		return "test";
	}

	@Override
	public String getStack() {
		return "junit";
	}

	@Override
	public String getAppId() {
		return "workflow";
	}

	@Override
	public Long getWorkflowInputPayloadSizeThresholdKB() {
		return 5120L;
	}

	@Override
	public Long getMaxWorkflowInputPayloadSizeThresholdKB() {
		return 10240L;
	}

	@Override
	public Long getWorkflowOutputPayloadSizeThresholdKB() {
		return 5120L;
	}

	@Override
	public Long getMaxWorkflowOutputPayloadSizeThresholdKB() {
		return 10240L;
	}

	@Override
	public Long getTaskInputPayloadSizeThresholdKB() {
		return 3072L;
	}

	@Override
	public Long getMaxTaskInputPayloadSizeThresholdKB() {
		return 10240L;
	}

	@Override
	public Long getTaskOutputPayloadSizeThresholdKB() {
		return 3072L;
	}

	@Override
	public Long getMaxTaskOutputPayloadSizeThresholdKB() {
		return 10240L;
	}

	@Override
	public String getProperty(String string, String def) {
		String val = testProperties.get(string);
		return val != null ? val : def;
	}

    @Override
    public boolean getBooleanProperty(String name, boolean defaultValue) {
        return false;
    }

    public void setProperty(String key, String value) {
		testProperties.put(key, value);
	}

	@Override
	public String getAvailabilityZone() {
		return "us-east-1a";
	}

	@Override
	public int getIntProperty(String string, int def) {
		return 100;
	}

	@Override
	public long getLongProperty(String name, long defaultValue) {
		return 0;
	}

	@Override
	public String getRegion() {
		return "us-east-1";
	}

	@Override
	public Map<String, Object> getAll() {
		return null;
	}
}
