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
package com.netflix.conductor;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Forces all WorkflowSystemTask subclasses (JOIN, StartWorkflow, SubWorkflow, etc.) to report
 * isAsync=false, making them execute inline in the decide loop instead of being queued for the
 * SystemTaskWorker.
 *
 * <p>Disabled by default. Enable with:
 * conductor.app.force-sync-system-task-enabled=true
 *
 * <p>Prerequisites before enabling:
 * 1. conductor.app.workflow-execution-lock-enabled=true (already set in PR-1)
 * 2. Be aware that nested SubWorkflow chains will recurse on the same thread — watch stack depth.
 */
@Aspect
@Component
@ConditionalOnProperty(
        name = "conductor.app.force-sync-system-task-enabled",
        havingValue = "true")
public class ConductorAOP {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConductorAOP.class);

    @Around(
            "execution(public boolean"
                    + " com.netflix.conductor.core.execution.tasks.WorkflowSystemTask+.isAsync())")
    public Object isAsync(JoinPoint joinPoint) {
        LOGGER.debug(
                "AOP: forcing {} isAsync to false",
                joinPoint.getTarget().getClass().getSimpleName());
        return false;
    }
}
