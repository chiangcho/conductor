package com.cvicse.gateway.conductor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.netflix.conductor.core.execution.WorkflowStatusListener;

/**
 * @author jiang_zhuo
 */
public class RedisStatusPublisherModule extends AbstractModule {
	private static final Logger LOGGER = LoggerFactory.getLogger(RedisStatusPublisher.class);
	@Override
	protected void configure() {
		LOGGER.info("register RedisStatusPublisher");
		bind(WorkflowStatusListener.class).to(RedisStatusPublisher.class);
	}
}
