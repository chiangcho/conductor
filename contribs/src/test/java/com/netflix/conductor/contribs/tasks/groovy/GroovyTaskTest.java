/*
 * Copyright 2022 Netflix, Inc.
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
package com.netflix.conductor.contribs.tasks.groovy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.netflix.conductor.model.TaskModel;
import com.netflix.conductor.model.WorkflowModel;

public class GroovyTaskTest {


    @Test
    public void dataShouldBeCorrectlySelected() {
        final GroovyTask GroovyTask = new GroovyTask();
        final WorkflowModel workflow = new WorkflowModel();
        final TaskModel task = new TaskModel();
        final Map<String, Object> inputData = new HashMap<>();
        inputData.put("script", "return key[0] + key[1]");
        final Map<String, Object> data = new HashMap<>();
        data.put("key", Arrays.asList(1,2));
        inputData.put("data", data);
        task.setInputData(inputData);
        task.setOutputData(new HashMap<>());
        
        GroovyTask.start(workflow, task, null);

        assertNull(task.getOutputData().get("error"));
        assertEquals("3", task.getOutputData().get("result").toString());
    }

    @Test
    public void simpleErrorShouldBeDisplayed() {
    	final GroovyTask GroovyTask = new GroovyTask();
        final WorkflowModel workflow = new WorkflowModel();
        final TaskModel task = new TaskModel();
        final Map<String, Object> inputData = new HashMap<>();
        inputData.put("script", "return1");
        task.setInputData(inputData);
        task.setOutputData(new HashMap<>());

        GroovyTask.start(workflow, task, null);

        assertTrue(
                ((String) task.getOutputData().get("error"))
                        .startsWith("No such property: return1 for class: Script1"));
    }


    @Test
    public void mapResultShouldBeCorrectlyExtracted() {
    	final GroovyTask GroovyTask = new GroovyTask();
        final WorkflowModel workflow = new WorkflowModel();
        final TaskModel task = new TaskModel();
        final Map<String, Object> inputData = new HashMap<>();
        inputData.put("script", "return [red: '#FF0000', green: '#00FF00', blue: '#0000FF']");
        task.setInputData(inputData);
        task.setOutputData(new HashMap<>());

        GroovyTask.start(workflow, task, null);

        assertNull(task.getOutputData().get("error"));
        assertTrue(task.getOutputData().get("result") instanceof Map);
        HashMap<String, Object> result =
                (HashMap<String, Object>) task.getOutputData().get("result");
        assertEquals("#FF0000", result.get("red"));
    }

    @Test
    public void stringResultShouldBeCorrectlyExtracted() {
    	final GroovyTask GroovyTask = new GroovyTask();
        final WorkflowModel workflow = new WorkflowModel();
        final TaskModel task = new TaskModel();
        final Map<String, Object> inputData = new HashMap<>();
        inputData.put("script", "'abc'");
        task.setInputData(inputData);
        task.setOutputData(new HashMap<>());

        GroovyTask.start(workflow, task, null);

        assertNull(task.getOutputData().get("error"));
        assertTrue(task.getOutputData().get("result") instanceof String);
        String result = (String) task.getOutputData().get("result");
        assertEquals("abc", result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void listResultShouldBeCorrectlyExtracted() throws JsonProcessingException {
    	final GroovyTask GroovyTask = new GroovyTask();
        final WorkflowModel workflow = new WorkflowModel();
        final TaskModel task = new TaskModel();
        final Map<String, Object> inputData = new HashMap<>();
        inputData.put("script", "return ['a','b','c']");
        task.setInputData(inputData);
        task.setOutputData(new HashMap<>());

        GroovyTask.start(workflow, task, null);

        assertNull(task.getOutputData().get("error"));
        assertTrue(task.getOutputData().get("result") instanceof List);
        List<Object> result = (List<Object>) task.getOutputData().get("result");
        assertEquals(3, result.size());
    }

    @Test
    public void nullResultShouldBeCorrectlyExtracted() throws JsonProcessingException {
    	final GroovyTask GroovyTask = new GroovyTask();
        final WorkflowModel workflow = new WorkflowModel();
        final TaskModel task = new TaskModel();
        final Map<String, Object> inputData = new HashMap<>();
        inputData.put("script", "");
        task.setInputData(inputData);
        task.setOutputData(new HashMap<>());

        GroovyTask.start(workflow, task, null);

        assertNull(task.getOutputData().get("error"));
        assertNull(task.getOutputData().get("result"));
    }
}
