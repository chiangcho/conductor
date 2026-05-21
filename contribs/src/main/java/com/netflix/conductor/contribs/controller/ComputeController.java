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
package com.netflix.conductor.contribs.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.contribs.service.ComputeService;

import io.swagger.v3.oas.annotations.Operation;

/**
 * 提供同步执行 workflow 的 HTTP 入口。
 *
 * <p>POST /compute/workflow/sync 启动一个 workflow 并阻塞直到执行完成，
 * 直接返回终态的 {@link Workflow} 对象，调用方无需轮询。
 *
 * <p>依赖配置:
 * <pre>
 *   conductor.workflow-status-listener.type=sync-release-lock
 *   conductor.app.workflow-execution-lock-enabled=true
 *   conductor.compute.lock-timeout-ms=60000  # 可选，默认 60s
 * </pre>
 */
@RestController
@RequestMapping("/compute/workflow")
public class ComputeController {

    private final ComputeService computeService;

    public ComputeController(ComputeService computeService) {
        this.computeService = computeService;
    }

    @PostMapping("/sync")
    @Operation(summary = "Start a workflow and block until it completes, returning the final state")
    public Workflow startWorkflowSync(@RequestBody StartWorkflowRequest request) {
        return computeService.startWorkflow(request, true);
    }
}
