package com.cvicse.gateway.conductor.nacos;

import com.alibaba.nacos.api.naming.NamingService;

import javax.inject.Provider;

public interface NacosNamingServiceProvider extends Provider<NamingService> {
}