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
package com.netflix.conductor.core.execution.tasks;

import static com.netflix.conductor.common.metadata.tasks.TaskType.TASK_TYPE_FORK;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.core.execution.AsyncSystemTaskExecutor;
import com.netflix.conductor.core.execution.DeciderService;
import com.netflix.conductor.core.execution.WorkflowExecutor;
import com.netflix.conductor.model.TaskModel;
import com.netflix.conductor.model.WorkflowModel;

@Component(TASK_TYPE_FORK)
public class Fork extends WorkflowSystemTask {
	private static final Logger LOGGER = LoggerFactory.getLogger(Fork.class);
	
	@Autowired
    public Fork() {
        super(TASK_TYPE_FORK);
    }
    @Autowired
    @Lazy
    private DeciderService deciderService;
    @Autowired
    @Lazy
    private WorkflowExecutor workflowExecutor;
    @Autowired
    @Lazy
    private AsyncSystemTaskExecutor asyncSystemTaskExecutor;
    
    @Override
    public void start(WorkflowModel workflow, TaskModel task, WorkflowExecutor executor) {
        WorkflowTask workflowTask = task.getWorkflowTask();
        int retryCount = task.getRetryCount();

        List<TaskModel> tasksToBeScheduled = new LinkedList<>();
        List<List<WorkflowTask>> forkTasks = workflowTask.getForkTasks();
        for (List<WorkflowTask> wfts : forkTasks) {
            WorkflowTask wft = wfts.get(0);
            List<TaskModel> tasks2 = deciderService
                            .getTasksToBeScheduled(workflow, wft, retryCount);
            tasksToBeScheduled.addAll(tasks2);
        }
        
        try {
        	List<CompletableFuture<Void>> taskCompletableFutures = new ArrayList<>();
        	
        	for (TaskModel taskScheduled : tasksToBeScheduled) {
        		WorkflowModel workflowModel = new WorkflowModel();
                BeanUtils.copyProperties(workflow, workflowModel);
                workflowModel.setTasks(new CopyOnWriteArrayList<TaskModel>());;
                CompletableFuture<Void> taskCompletableFuture =
                        CompletableFuture.runAsync(
                                () -> workflowExecutor.decide(workflowModel, taskScheduled));
                taskCompletableFutures.add(taskCompletableFuture);
            }
        	CompletableFuture<Void> allCompletableFuture = CompletableFuture.allOf(taskCompletableFutures.toArray(new CompletableFuture[0]));
        	allCompletableFuture.join();
        	LOGGER.debug("all thread complted");
        	//load latest workflow data ,load changing in sub thread 
        	WorkflowModel workflowNew = workflowExecutor.getWorkflow(workflow.getWorkflowId(), true);
        	BeanUtils.copyProperties(workflowNew, workflow);
            task.setStatus(TaskModel.Status.COMPLETED);
        } catch (final Exception e) {
            LOGGER.error(
                    "Error executing task: {} in workflow: {}",
                    task.getTaskId(),
                    workflow.getWorkflowId(),
                    e);
            task.setStatus(TaskModel.Status.FAILED);
            task.setReasonForIncompletion(e.getMessage());
            task.addOutput(
                    "error", e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
        }
    }

}
