package com.cvicse.gateway.conductor;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.common.run.WorkflowSummary;
import com.netflix.conductor.core.events.queue.Message;
import com.netflix.conductor.core.execution.WorkflowStatusListener;

import redis.clients.jedis.commands.JedisCommands;



/**
 * 在conductor的workflow执行结束后发布消息到redis
 * @author jiang_zhuo
 *
 */
public class RedisStatusPublisher implements WorkflowStatusListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(RedisStatusPublisher.class);

	private ObjectMapper objectMapper;
	private JedisCommands dynoClient;

	
	@Inject
    public RedisStatusPublisher(JedisCommands dynoClient, ObjectMapper objectMapper) {
		LOGGER.info("construct RedisStatusPublisher");
		this.dynoClient = dynoClient;
		this.objectMapper = objectMapper;
    }
	
	public void onWorkflowCompleted(Workflow workflow) {
		LOGGER.info("Publishing callback of workflow {} on completion ", workflow.getWorkflowId());
		this.dynoClient.lpush(("conductor-result-" + workflow.getWorkflowId()),
				workflowToMessage(workflow).getPayload());

	}

	public void onWorkflowTerminated(Workflow workflow) {
		LOGGER.info("Publishing callback of workflow {} on termination", workflow.getWorkflowId());
		this.dynoClient.lpush(("conductor-result-" + workflow.getWorkflowId()),
				workflowToMessage(workflow).getPayload());

	}

	private Message workflowToMessage(Workflow workflow) {
		String jsonWfSummary;
		WorkflowSummary summary = new WorkflowSummary(workflow);
		try {
			jsonWfSummary = objectMapper.writeValueAsString(summary);
		} catch (JsonProcessingException e) {
			LOGGER.error("Failed to convert WorkflowSummary: {} to String. Exception: {}", summary, e);
			throw new RuntimeException(e);
		}
		return new Message(workflow.getWorkflowId(), jsonWfSummary, null);
	}
}
