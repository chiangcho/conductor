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
package com.netflix.conductor.contribs.service;

import org.springframework.stereotype.Service;

import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.core.exception.NonTransientException;
import com.netflix.conductor.service.ExecutionLockService;
import com.netflix.conductor.service.WorkflowService;

@Service
public class ComputeService {
    private final WorkflowService workflowService;
    private ExecutionLockService lockService;

    public ComputeService(WorkflowService workflowService, ExecutionLockService lockService) {
        this.workflowService = workflowService;
        this.lockService = lockService;
    }

    /**
     * start workflow sync
     *
     * @param request
     * @param includeTasks
     * @return
     */
    public Workflow startWorkflow(StartWorkflowRequest request, boolean includeTasks) {
        String workflowId = workflowService.startWorkflow(request);
        try {
            // 启动后获得锁,如果锁定成功，则尝试查询结果，如果已经结束则返回
            boolean lockWorkflow = this.lockWorkflow(workflowId);
            if (lockWorkflow) {
                Workflow workflow = workflowService.getExecutionStatus(workflowId, includeTasks);
                if (workflow.getStatus().isSuccessful() || workflow.getStatus().isTerminal()) {
                    return workflow;
                }
            } else {
                throw new NonTransientException("lock workflow timeout");
            }
            // 等待执行完成后释放锁
            boolean lockWorkflow2 = this.lockWorkflow(workflowId);
            if (!lockWorkflow2) {
                throw new NonTransientException("wait for workflow to complete timeout");
            }
            Workflow workflow = workflowService.getExecutionStatus(workflowId, includeTasks);
            return workflow;
        } finally {
            this.releaseWorkflowLock(workflowId);
        }
    }

    public boolean lockWorkflow(String workflowId) {
        return this.lockService.acquireLock(workflowId, 60 * 1000);
    }

    public void releaseWorkflowLock(String workflowId) {
        this.lockService.releaseLock("conductor-result-" + workflowId);
    }
}
