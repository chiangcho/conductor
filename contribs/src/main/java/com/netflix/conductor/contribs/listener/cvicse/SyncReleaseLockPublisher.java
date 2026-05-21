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
package com.netflix.conductor.contribs.listener.cvicse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.conductor.contribs.service.ComputeService;
import com.netflix.conductor.core.listener.WorkflowStatusListener;
import com.netflix.conductor.model.WorkflowModel;

/**
 * workflow 执行结束后释放同步锁，配合 {@link ComputeService#startWorkflow} 使用。
 *
 * <p>当 workflow 完成或终止时，释放 ComputeService 在启动时持有的锁，
 * 使阻塞在第二次 acquireLock 的调用方得以继续并取回结果。
 *
 * <p>通过 conductor.workflow-status-listener.type=sync-release-lock 启用。
 */
public class SyncReleaseLockPublisher implements WorkflowStatusListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncReleaseLockPublisher.class);

    private final ComputeService computeService;

    public SyncReleaseLockPublisher(ComputeService computeService) {
        this.computeService = computeService;
    }

    @Override
    public void onWorkflowCompleted(WorkflowModel workflow) {
        LOGGER.info("Releasing sync lock for completed workflow {}", workflow.getWorkflowId());
        computeService.releaseWorkflowLock(workflow.getWorkflowId());
    }

    @Override
    public void onWorkflowTerminated(WorkflowModel workflow) {
        LOGGER.info("Releasing sync lock for terminated workflow {}", workflow.getWorkflowId());
        computeService.releaseWorkflowLock(workflow.getWorkflowId());
    }
}
