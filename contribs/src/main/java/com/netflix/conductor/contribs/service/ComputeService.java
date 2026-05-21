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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.core.exception.NonTransientException;
import com.netflix.conductor.service.ExecutionLockService;
import com.netflix.conductor.service.WorkflowService;

/**
 * 同步执行 workflow 的服务。
 *
 * <p>调用方通过 {@link #startWorkflow} 启动一个 workflow 并阻塞等待其执行完成，
 * 无需轮询。实现依赖 {@link ExecutionLockService}：
 * <ol>
 *   <li>启动 workflow 后立即抢锁（此时锁未被持有，应当成功）；
 *   <li>查询状态，若已终结直接返回；
 *   <li>释放锁，等待 {@link SyncReleaseLockPublisher} 在 onWorkflowCompleted/Terminated
 *       回调里释放同一把锁；
 *   <li>再次抢锁，抢到即说明 workflow 已终结，查询并返回结果。
 * </ol>
 *
 * <p>需要同时配置:
 * <pre>
 *   conductor.workflow-status-listener.type=sync-release-lock
 *   conductor.app.workflow-execution-lock-enabled=true
 * </pre>
 */
@Service
public class ComputeService {

    private final WorkflowService workflowService;
    private final ExecutionLockService lockService;
    private final long lockTimeoutMs;

    public ComputeService(
            WorkflowService workflowService,
            ExecutionLockService lockService,
            @Value("${conductor.compute.lock-timeout-ms:60000}") long lockTimeoutMs) {
        this.workflowService = workflowService;
        this.lockService = lockService;
        this.lockTimeoutMs = lockTimeoutMs;
    }

    /**
     * 启动 workflow 并同步等待执行完成后返回结果。
     *
     * @param request      workflow 启动请求
     * @param includeTasks 返回结果是否包含 task 列表
     * @return 执行完成的 Workflow 对象
     * @throws NonTransientException 超过 lock-timeout-ms 仍未完成时抛出
     */
    public Workflow startWorkflow(StartWorkflowRequest request, boolean includeTasks) {
        String workflowId = workflowService.startWorkflow(request);
        try {
            // 第一次抢锁：workflow 刚启动，锁未被持有，应当立即成功
            if (!lockWorkflow(workflowId)) {
                throw new NonTransientException(
                        "acquire workflow lock timeout: " + workflowId);
            }
            // 同步路径下 workflow 可能已经执行完毕，先查一次
            Workflow workflow = workflowService.getExecutionStatus(workflowId, includeTasks);
            if (workflow.getStatus().isTerminal()) {
                return workflow;
            }
            // 释放锁，让 SyncReleaseLockPublisher 在 workflow 结束时释放同一把锁
            releaseWorkflowLock(workflowId);
            // 第二次抢锁：等待 listener 释放后才能抢到，抢到即 workflow 已终结
            if (!lockWorkflow(workflowId)) {
                throw new NonTransientException(
                        "wait for workflow to complete timeout: " + workflowId);
            }
            return workflowService.getExecutionStatus(workflowId, includeTasks);
        } finally {
            releaseWorkflowLock(workflowId);
        }
    }

    public boolean lockWorkflow(String workflowId) {
        return lockService.acquireLock(workflowId, lockTimeoutMs);
    }

    /** lock 和 release 使用同一个 key，避免 slim 版本中 key 不一致导致锁释放失败的 bug。 */
    public void releaseWorkflowLock(String workflowId) {
        lockService.releaseLock(workflowId);
    }
}
