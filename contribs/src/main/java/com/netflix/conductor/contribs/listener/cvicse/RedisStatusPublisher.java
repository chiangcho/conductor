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
import org.springframework.beans.BeanUtils;

import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.common.run.WorkflowSummary;
import com.netflix.conductor.core.events.queue.Message;
import com.netflix.conductor.core.listener.WorkflowStatusListener;
import com.netflix.conductor.model.WorkflowModel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.commands.JedisCommands;

/**
 * 在conductor的workflow执行结束后发布消息到redis
 *
 * @author jiang_zhuo
 */
public class RedisStatusPublisher implements WorkflowStatusListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisStatusPublisher.class);

    private ObjectMapper objectMapper;
    private JedisCommands dynoClient;

    public RedisStatusPublisher(JedisCommands dynoClient, ObjectMapper objectMapper) {
        LOGGER.info("construct RedisStatusPublisher");
        this.dynoClient = dynoClient;
        this.objectMapper = objectMapper;
    }

    public void onWorkflowCompleted(WorkflowModel workflow) {
        LOGGER.info("Publishing callback of workflow {} on completion ", workflow.getWorkflowId());
        this.dynoClient.lpush(
                ("conductor-result-" + workflow.getWorkflowId()),
                workflowToMessage(workflow).getPayload());
    }

    public void onWorkflowTerminated(WorkflowModel workflow) {
        LOGGER.info("Publishing callback of workflow {} on termination", workflow.getWorkflowId());
        this.dynoClient.lpush(
                ("conductor-result-" + workflow.getWorkflowId()),
                workflowToMessage(workflow).getPayload());
    }

    private Message workflowToMessage(WorkflowModel workflow) {
        String jsonWfSummary;

        WorkflowSummary summary = new WorkflowSummary(this.toWorkflow(workflow));
        try {
            jsonWfSummary = objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException e) {
            LOGGER.error(
                    "Failed to convert WorkflowSummary: {} to String. Exception: {}", summary, e);
            throw new RuntimeException(e);
        }
        return new Message(workflow.getWorkflowId(), jsonWfSummary, null);
    }

    private Workflow toWorkflow(WorkflowModel workflow) {
        Workflow wf = new Workflow();
        BeanUtils.copyProperties(workflow, wf);
        wf.setUpdateTime(workflow.getUpdatedTime());
        wf.setStartTime(workflow.getCreateTime());
        return wf;
    }
}
