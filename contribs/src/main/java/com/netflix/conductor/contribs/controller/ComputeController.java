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

import javax.websocket.server.PathParam;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.contribs.service.ComputeService;

import io.swagger.v3.oas.annotations.Operation;

import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

@RestController
@RequestMapping("/compute/workflow")
public class ComputeController {
    private ComputeService computeService;

    public ComputeController(ComputeService computeService) {
        this.computeService = computeService;
    }

    @PostMapping(value = "/sync", produces = TEXT_PLAIN_VALUE)
    @Operation(
            summary =
                    "Start a new workflow with StartWorkflowRequest, which allows task to be executed in a domain")
    public Workflow startWorkflow(
            @RequestBody StartWorkflowRequest request,
            @PathParam("includeTasks") String includeTasks) {
        return computeService.startWorkflow(request, true);
    }
}
