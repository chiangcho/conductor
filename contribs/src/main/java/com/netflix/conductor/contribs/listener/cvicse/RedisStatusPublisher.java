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
 * 在conductor的workflow执行结束后释放锁
 *
 * @author jiang_zhuo
 */
public class RedisStatusPublisher implements WorkflowStatusListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisStatusPublisher.class);

    private ComputeService computeService;

    public RedisStatusPublisher(ComputeService computeService) {
        this.computeService = computeService;
    }

    public void onWorkflowCompleted(WorkflowModel workflow) {
        LOGGER.info("Publishing callback of workflow {} on completion ", workflow.getWorkflowId());
        this.computeService.releaseWorkflowLock(workflow.getWorkflowId());
    }

    public void onWorkflowTerminated(WorkflowModel workflow) {
        LOGGER.info("Publishing callback of workflow {} on termination", workflow.getWorkflowId());
        this.computeService.releaseWorkflowLock(workflow.getWorkflowId());
    }
}
