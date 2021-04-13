package com.cvicse.gateway.conductor.nacos;

import static com.alibaba.nacos.api.PropertyKeyConst.ACCESS_KEY;
import static com.alibaba.nacos.api.PropertyKeyConst.CLUSTER_NAME;
import static com.alibaba.nacos.api.PropertyKeyConst.NAMESPACE;
import static com.alibaba.nacos.api.PropertyKeyConst.NAMING_LOAD_CACHE_AT_START;
import static com.alibaba.nacos.api.PropertyKeyConst.SECRET_KEY;
import static com.alibaba.nacos.api.PropertyKeyConst.SERVER_ADDR;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.client.naming.utils.NetUtils;
import com.netflix.conductor.contribs.http.HttpTask;
import com.netflix.conductor.core.config.Configuration;
import com.netflix.conductor.core.execution.tasks.WorkflowSystemTask;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Properties;

public class BaseNacosNamingServiceProvider implements NacosNamingServiceProvider {
    private static final Logger logger = LoggerFactory.getLogger(BaseNacosNamingServiceProvider.class);
    public static final String NACOS_SERVER_URL_PROPERTY_NAME = "nacos.server.url";
    public static final String NACOS_CLUSTER_NAME_PROPERTY_NAME = "nacos.cluster.name";
    public static final String NACOS_ENABLED_PROPERTY_NAME = "nacos.enabled";
    public static final String NACOS_NAMESPACE_PROPERTY_NAME = "nacos.namespace";
    public static final String NACOS_ACCESS_KEY_PROPERTY_NAME = "nacos.access.key";
    public static final String NACOS_SECRET_KEY_PROPERTY_NAME = "nacos.secret.key";
    public static final String NACOS_REGISTER_ENABLED_PROPERTY_NAME = "nacos.register.enabled";
    public static final String NACOS_WEIGHT_PROPERTY_NAME = "nacos.weight";
    public static final String NACOS_NAMING_LOAD_CACHE_AT_START_PROPERTY_NAME = "nacos.naming.load.cache.at.start";
    public static final String NACOS_NETWORK_INTERFACE_PROPERTY_NAME = "nacos.network.interface";
    public static final String PORT_PROPERTY_NAME = "conductor.jetty.server.port";
    public static final int PORT_DEFAULT_VALUE = 8080;

    private final Configuration config;

    private final NacosConfiguration nacosConfiguration = new NacosConfiguration();

    @Inject
    public BaseNacosNamingServiceProvider(Configuration configuration) {
        this.config = configuration;
    }

    @Override
    public NamingService get() {
        logger.info("nacos module init nacos naming service ...");
        try {
            this.initNacosConfiguration();
            String serverAddr = nacosConfiguration.getServerAddr();
            int port = config.getIntProperty(PORT_PROPERTY_NAME,PORT_DEFAULT_VALUE);
            String appId = config.getAppId();
            String ip = nacosConfiguration.getIp();
            if(StringUtils.isNotBlank(serverAddr) && nacosConfiguration.isEnabled()) {
                String nameSpace = nacosConfiguration.getNamespace();
                String clusterName = nacosConfiguration.getClusterName();
                logger.info("nacos server addr :{},localip:{},port:{},service name:{}, namespace:{}",serverAddr, ip,port,appId,nameSpace);
                //create nacos namingservice
                Properties properties = new Properties();
                properties.put(SERVER_ADDR, nacosConfiguration.getServerAddr());
                properties.put(NAMESPACE, nacosConfiguration.getNamespace());

                properties.put(ACCESS_KEY, nacosConfiguration.getAccessKey());
                properties.put(SECRET_KEY, nacosConfiguration.getSecretKey());
                properties.put(CLUSTER_NAME, nacosConfiguration.getClusterName());
                properties.put(NAMING_LOAD_CACHE_AT_START, nacosConfiguration.getNamingLoadCacheAtStart());
                final NamingService namingService = NamingFactory.createNamingService(properties);
                HttpTask httpTask = (HttpTask) WorkflowSystemTask.get(HttpTask.NAME);
                httpTask.setNamingService(namingService);
                //register to nacos
                if(nacosConfiguration.isRegisterEnabled()) {
                    Instance instance = new Instance();
                    instance.setIp(ip);
                    instance.setPort(port);
                    instance.setWeight(nacosConfiguration.getWeight());
                    instance.setClusterName(clusterName);
                    namingService.registerInstance(appId, instance);
                    //deregister from nacos when shutdown
                    Runtime.getRuntime().addShutdownHook(new Thread() {
                        @Override
                        public void run() {
                            try {
                                logger.info("deregister from nacos ...");
                                namingService.deregisterInstance(appId, instance);
                            } catch (NacosException e) {
                                logger.warn("deregister from nacos failed", e);
                            }
                        }
                    });
                }
                return namingService;
            }
        } catch (NacosException e) {
            logger.error("init nacos failed",e);
        }
        return null;
    }

    public void initNacosConfiguration() {
        String serverAddr = config.getProperty(NACOS_SERVER_URL_PROPERTY_NAME,"");
        boolean enabled = config.getBoolProperty(NACOS_ENABLED_PROPERTY_NAME,true);

        String nameSpace = config.getProperty(NACOS_NAMESPACE_PROPERTY_NAME,"");
        String clusterName = config.getProperty(NACOS_CLUSTER_NAME_PROPERTY_NAME,"DEFAULT");
        String accessKey = config.getProperty(NACOS_ACCESS_KEY_PROPERTY_NAME,"");
        String secretKey = config.getProperty(NACOS_SECRET_KEY_PROPERTY_NAME,"");
        String namingLoadCacheAtStart = config.getProperty(NACOS_NAMING_LOAD_CACHE_AT_START_PROPERTY_NAME,"false");
        boolean registerEnabled = config.getBoolProperty(NACOS_REGISTER_ENABLED_PROPERTY_NAME,true);
        int weight = config.getIntProperty(NACOS_WEIGHT_PROPERTY_NAME,1);
        String networkInterface = config.getProperty(NACOS_NETWORK_INTERFACE_PROPERTY_NAME,"");

        nacosConfiguration.setEnabled(enabled);
        nacosConfiguration.setServerAddr(serverAddr);
        nacosConfiguration.setNamespace(nameSpace);
        nacosConfiguration.setClusterName(clusterName);
        nacosConfiguration.setRegisterEnabled(registerEnabled);
        nacosConfiguration.setAccessKey(accessKey);
        nacosConfiguration.setSecretKey(secretKey);
        nacosConfiguration.setWeight(weight);
        nacosConfiguration.setNamingLoadCacheAtStart(namingLoadCacheAtStart);
        nacosConfiguration.setNetworkInterface(networkInterface);
        //if network interface is not blank ,get ip by name
        if(StringUtils.isNotBlank(networkInterface)) {
            nacosConfiguration.setIp(this.getIpByNetworkInterface(networkInterface));
        } else {
            String ip = NetUtils.localIP();
            nacosConfiguration.setIp(ip);
        }

    }


    /**
     * get ip by network interface
     * @param networkInterface
     * @return
     */
    private String getIpByNetworkInterface(String networkInterface) {
        String ip = null;
        NetworkInterface netInterface = null;
        try {
            netInterface = NetworkInterface
                    .getByName(networkInterface);
        } catch (SocketException e) {
            throw new IllegalArgumentException(
                    "failed to get net network interface by  " + networkInterface,e);
        }
        if (null == netInterface) {
            throw new IllegalArgumentException(
                    "no such interface " + networkInterface);
        }

        Enumeration<InetAddress> inetAddress = netInterface.getInetAddresses();
        while (inetAddress.hasMoreElements()) {
            InetAddress currentAddress = inetAddress.nextElement();
            if (currentAddress instanceof Inet4Address
                    && !currentAddress.isLoopbackAddress()) {
                ip = currentAddress.getHostAddress();
                break;
            }
        }

        if (StringUtils.isEmpty(ip)) {
            throw new RuntimeException("cannot find available ip from"
                    + " network interface " + networkInterface);
        }
        return ip;

    }

}