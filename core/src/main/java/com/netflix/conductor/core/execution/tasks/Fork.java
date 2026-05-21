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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.core.config.ConductorProperties;
import com.netflix.conductor.core.config.SchedulerConfiguration;
import com.netflix.conductor.core.execution.DeciderService;
import com.netflix.conductor.core.execution.WorkflowExecutor;
import com.netflix.conductor.model.TaskModel;
import com.netflix.conductor.model.WorkflowModel;

import static com.netflix.conductor.common.metadata.tasks.TaskType.TASK_TYPE_FORK;

@Component(TASK_TYPE_FORK)
public class Fork extends WorkflowSystemTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(Fork.class);

    private final DeciderService deciderService;
    private final WorkflowExecutor workflowExecutor;
    private final ConductorProperties properties;
    private final Executor forkExecutor;

    @Autowired
    public Fork(
            @Lazy DeciderService deciderService,
            @Lazy WorkflowExecutor workflowExecutor,
            ConductorProperties properties,
            @Qualifier(SchedulerConfiguration.FORK_EXECUTOR_NAME)
                    @Autowired(required = false)
                    Executor forkExecutor) {
        super(TASK_TYPE_FORK);
        this.deciderService = deciderService;
        this.workflowExecutor = workflowExecutor;
        this.properties = properties;
        this.forkExecutor = forkExecutor;
    }

    @Override
    public void start(WorkflowModel workflow, TaskModel task, WorkflowExecutor executor) {
        if (!properties.isForkParallelEnabled() || forkExecutor == null) {
            // Upstream Netflix behaviour: ForkJoinTaskMapper has already pre-scheduled each
            // branch's first task; Fork itself just marks COMPLETED.
            task.setStatus(TaskModel.Status.COMPLETED);
            return;
        }

        WorkflowTask workflowTask = task.getWorkflowTask();
        int retryCount = task.getRetryCount();

        List<TaskModel> branchHeadTasks = new LinkedList<>();
        for (List<WorkflowTask> wfts : workflowTask.getForkTasks()) {
            branchHeadTasks.addAll(
                    deciderService.getTasksToBeScheduled(workflow, wfts.get(0), retryCount));
        }

        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>(branchHeadTasks.size());
            for (TaskModel branchHead : branchHeadTasks) {
                WorkflowModel branchWorkflow = new WorkflowModel();
                BeanUtils.copyProperties(workflow, branchWorkflow);
                // Each branch thread drives decide on a private workflow view with an empty task
                // list; DeciderService uses the empty/no-fork view to recognise we are inside a
                // branch thread and skip scheduling the JOIN that follows the owning FORK.
                branchWorkflow.setTasks(new CopyOnWriteArrayList<>());
                futures.add(
                        CompletableFuture.runAsync(
                                () -> workflowExecutor.decide(branchWorkflow, branchHead),
                                forkExecutor));
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // Reload the workflow so the caller sees task-state mutations made by branch threads.
            WorkflowModel latest = workflowExecutor.getWorkflow(workflow.getWorkflowId(), true);
            BeanUtils.copyProperties(latest, workflow);
            task.setStatus(TaskModel.Status.COMPLETED);
        } catch (Exception e) {
            LOGGER.error(
                    "Error executing fork task: {} in workflow: {}",
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
