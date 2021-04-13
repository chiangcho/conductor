package com.cvicse.gateway.conductor.nacos;

public class NacosConfiguration {


    /**
     * nacos discovery server address.
     */
    private String serverAddr;

    /**
     * namespace, separation registry of different environments.
     */
    private String namespace;

    /**
     * weight for service instance, the larger the value, the larger the weight.
     */
    private float weight = 1;

    /**
     * cluster name for nacos server.
     */
    private String clusterName = "DEFAULT";

    /**
     * naming load from local cache at application start. true is load.
     */
    private String namingLoadCacheAtStart = "false";


    /**
     * if you just want to subscribe, but don't want to register your service, set it to
     * false.
     */
    private boolean registerEnabled = true;

    /**
     * The ip address your want to register for your service instance, needn't to set it
     * if the auto detect ip works well.
     */
    private String ip;

    /**
     * which network interface's ip you want to register.
     */
    private String networkInterface = "";

    /**
     * access key for namespace.
     */
    private String accessKey;

    /**
     * secret key for namespace.
     */
    private String secretKey;

    /**
     * enable subscribe and register
     */
    private boolean enabled = true;

    public String getServerAddr() {
        return serverAddr;
    }

    public void setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getNamingLoadCacheAtStart() {
        return namingLoadCacheAtStart;
    }

    public void setNamingLoadCacheAtStart(String namingLoadCacheAtStart) {
        this.namingLoadCacheAtStart = namingLoadCacheAtStart;
    }

    public boolean isRegisterEnabled() {
        return registerEnabled;
    }

    public void setRegisterEnabled(boolean registerEnabled) {
        this.registerEnabled = registerEnabled;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getNetworkInterface() {
        return networkInterface;
    }

    public void setNetworkInterface(String networkInterface) {
        this.networkInterface = networkInterface;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
