package com.netflix.conductor;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 通过AOP将内置的SystemTask全部改为同步，包括JOIN,StartWorkflow,SubWorkflow
 *
 * @author jiang_zhuo
 */
@Aspect
@Component
public class ConductorAOP {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConductorAOP.class);

    @Around(
            "execution(* com.netflix.conductor.core.execution.tasks.WorkflowSystemTask.isAsync()) && execution(* com.netflix.conductor.core.execution.tasks..*.*(..)) ")
    public Object isAsync(JoinPoint joinPoint) throws Throwable {
        LOGGER.debug(
                "conductor aop change "
                        + joinPoint.getTarget().getClass().toString()
                        + " isAsync to false");
        return false;
    }
}
