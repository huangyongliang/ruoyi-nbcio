package com.ruoyi.workflow.service;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WfCallbackRegistry {

    private final Map<String, WfCallBackServiceI> callbackServices;

    public WfCallbackRegistry(Map<String, WfCallBackServiceI> callbackServices) {
        this.callbackServices = callbackServices;
    }

    public WfCallBackServiceI getCallback(String serviceImplName) {
        return callbackServices.get(serviceImplName);
    }
}
