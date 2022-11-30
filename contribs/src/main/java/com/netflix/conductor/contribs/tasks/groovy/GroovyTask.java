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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.netflix.conductor.core.execution.WorkflowExecutor;
import com.netflix.conductor.core.execution.tasks.WorkflowSystemTask;
import com.netflix.conductor.model.TaskModel;
import com.netflix.conductor.model.WorkflowModel;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

@Component(GroovyTask.NAME)
public class GroovyTask extends WorkflowSystemTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroovyTask.class);
    public static final String NAME = "GROOVY";
    private static final String SCRIPT_PARAMETER = "script";
    private static final String DATA_PARAMETER = "data";
    private static final String OUTPUT_RESULT = "result";
    private static final String OUTPUT_RESULT_LIST = "resultList";
    private static final String OUTPUT_ERROR = "error";

    @Autowired
    public GroovyTask() {
        super(NAME);
    }

    @Override
    public void start(WorkflowModel workflow, TaskModel task, WorkflowExecutor executor) {
        final Map<String, Object> taskInput = task.getInputData();
        final Map<String, Object> data = (Map<String, Object>) taskInput.get(DATA_PARAMETER);
        final String scriptExpression = (String) taskInput.get(SCRIPT_PARAMETER);

        if (scriptExpression == null) {
            task.setReasonForIncompletion("Missing '" + SCRIPT_PARAMETER + "' in input parameters");
            task.setStatus(TaskModel.Status.FAILED);
            return;
        }
        try {
            GroovyShell groovyShell =
                    new GroovyShell(this.getClass().getClassLoader(), new Binding(data));
            Object result = groovyShell.evaluate(scriptExpression);
            task.setStatus(TaskModel.Status.COMPLETED);
            task.addOutput(OUTPUT_RESULT, result);
        } catch (final Exception e) {
            LOGGER.error(
                    "Error executing task: {} in workflow: {}",
                    task.getTaskId(),
                    workflow.getWorkflowId(),
                    e);
            task.setStatus(TaskModel.Status.FAILED);
            final String message = extractFirstValidMessage(e);
            task.setReasonForIncompletion(message);
            task.addOutput(OUTPUT_ERROR, message);
        }
    }

    private String extractFirstValidMessage(final Exception e) {
        Throwable currentStack = e;
        final List<String> messages = new ArrayList<>();
        messages.add(currentStack.getMessage());
        while (currentStack.getCause() != null) {
            currentStack = currentStack.getCause();
            messages.add(currentStack.getMessage());
        }
        return messages.stream().filter(it -> !it.contains("N/A")).findFirst().orElse("");
    }
}
