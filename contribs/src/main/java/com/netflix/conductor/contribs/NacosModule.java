package com.netflix.conductor.contribs;

import com.alibaba.nacos.api.naming.NamingService;
import com.google.inject.AbstractModule;
import com.netflix.conductor.contribs.nacos.BaseNacosNamingServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NacosModule extends AbstractModule {
    private static final Logger logger = LoggerFactory.getLogger(NacosModule.class);

    @Override
    protected void configure() {
        logger.info("Initializing nacos");
        bind(NamingService.class).toProvider(BaseNacosNamingServiceProvider.class).asEagerSingleton();
    }


}