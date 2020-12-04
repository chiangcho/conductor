package com.cvicse.gateway.conductor.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.client.naming.utils.NetUtils;
import com.netflix.conductor.contribs.http.HttpTask;
import com.netflix.conductor.core.config.Configuration;
import com.netflix.conductor.core.execution.tasks.WorkflowSystemTask;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class BaseNacosNamingServiceProvider implements NacosNamingServiceProvider {
    private static final Logger logger = LoggerFactory.getLogger(BaseNacosNamingServiceProvider.class);
    public static final String NACOS_SERVER_URL_PROPERTY_NAME = "nacos.server.url";
    public static final String PORT_PROPERTY_NAME = "conductor.jetty.server.port";
    public static final int PORT_DEFAULT_VALUE = 8080;

    private final Configuration config;

    @Inject
    public BaseNacosNamingServiceProvider(Configuration configuration) {
        this.config = configuration;
    }

    @Override
    public NamingService get() {
        logger.info("nacos module init nacos naming service ...");
        try {
            String serverAddr = config.getProperty(NACOS_SERVER_URL_PROPERTY_NAME,"");
            String ip = NetUtils.localIP();
            int port = config.getIntProperty(PORT_PROPERTY_NAME,PORT_DEFAULT_VALUE);
            String appId = config.getAppId();
            if(StringUtils.isNotBlank(serverAddr)) {
                logger.info("nacos server addr :{},localip:{},port:{},service name:{}",serverAddr,ip,port,appId);
                //create nacos namingservice
                final NamingService namingService = NamingFactory.createNamingService(serverAddr);
                HttpTask httpTask = (HttpTask) WorkflowSystemTask.get(HttpTask.NAME);
                httpTask.setNamingService(namingService);
                //register to nacos
                namingService.registerInstance(appId,ip,port);
                //deregister from nacos when shutdown
                Runtime.getRuntime().addShutdownHook(new Thread(){
                    @Override
                    public void run() {
                        try {
                            logger.info("deregister from nacos ...");
                            namingService.deregisterInstance(appId,ip,port);
                        } catch (NacosException e) {
                            logger.warn("deregister from nacos failed",e);
                        }
                    }
                });
                return namingService;
            }
        } catch (NacosException e) {
            logger.error("init nacos failed",e);
        }
        return null;
    }

}