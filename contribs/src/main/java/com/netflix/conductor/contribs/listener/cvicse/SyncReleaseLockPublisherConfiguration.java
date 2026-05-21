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

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.conductor.contribs.service.ComputeService;
import com.netflix.conductor.core.listener.WorkflowStatusListener;

/**
 * 注册 {@link SyncReleaseLockPublisher} 为 WorkflowStatusListener。
 *
 * <p>通过以下配置启用（与原 RedisStatusPublisherConfiguration 互斥）:
 * <pre>
 *   conductor.workflow-status-listener.type=sync-release-lock
 * </pre>
 */
@Configuration
@ConditionalOnProperty(
        name = "conductor.workflow-status-listener.type",
        havingValue = "sync-release-lock")
public class SyncReleaseLockPublisherConfiguration {

    @Bean
    public WorkflowStatusListener syncReleaseLockPublisher(ComputeService computeService) {
        return new SyncReleaseLockPublisher(computeService);
    }
}
