package com.ruoyi.workflow.handler;


import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import cn.hutool.core.util.ObjectUtil;
import lombok.AllArgsConstructor;

/**
 * 指定下一个接收人处理类
 *
 * @author nbacheng
 */
@AllArgsConstructor
@Component("getNextApprovalHandler")
public class GetNextApprovalHandler {  //目前暂时不做逻辑方面的处理
	public String getApproval(DelegateExecution execution) {
		FlowElement flowElement = execution.getCurrentFlowElement();
        if (ObjectUtil.isNotEmpty(flowElement) && flowElement instanceof UserTask) {
            UserTask userTask = (UserTask) flowElement;
        }
		return null;
	}
}
