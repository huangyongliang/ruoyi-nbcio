package com.ruoyi.workflow.service.task;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.service.CommonService;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.flowable.core.domain.ActStatus;
import com.ruoyi.flowable.core.domain.dto.FlowNextDto;
import com.ruoyi.workflow.domain.WfMyBusiness;
import com.ruoyi.workflow.domain.bo.WfTaskBo;
import com.ruoyi.workflow.service.WfCallBackServiceI;
import com.ruoyi.workflow.service.WfCallbackRegistry;
import com.ruoyi.workflow.service.impl.WfMyBusinessServiceImpl;
import lombok.RequiredArgsConstructor;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WfTaskBusinessSyncService {

    private final WfMyBusinessServiceImpl wfMyBusinessService;
    private final WfCallbackRegistry callbackRegistry;
    private final CommonService commonService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;

    public void syncAfterComplete(WfTaskBo taskBo, FlowNextDto flowNextDto) {
        if (StringUtils.isBlank(taskBo.getDataId())) {
            return;
        }

        WfMyBusiness business = wfMyBusinessService.getByDataId(taskBo.getDataId());
        if (flowNextDto != null) {
            updateDoingBusiness(taskBo, business, flowNextDto);
        } else {
            business.setActStatus(ActStatus.pass);
            business.setTaskId("");
            business.setTaskNameId("");
            business.setTaskName("");
            business.setTodoUsers("");
        }

        wfMyBusinessService.updateById(business);
        WfCallBackServiceI callback = callbackRegistry.getCallback(business.getServiceImplName());
        if (callback != null) {
            callback.afterFlowHandle(business);
        }
    }

    private void updateDoingBusiness(WfTaskBo taskBo, WfMyBusiness business, FlowNextDto flowNextDto) {
        UserTask nextUserTask = flowNextDto.getUserTask();
        List<SysUser> nextFlowNodeUserList = flowNextDto.getUserList();
        Task nextTask = queryNextTask(business.getProcessInstanceId());

        if (nextFlowNodeUserList != null) {
            business.setActStatus(ActStatus.doing);
            business.setTaskId(nextTask.getId());
            business.setTaskNameId(nextUserTask.getId());
            business.setTaskName(nextUserTask.getName());
            business.setPriority(nextUserTask.getPriority());
            business.setTodoUsers(resolveTodoUsers(taskBo, nextFlowNodeUserList));
        } else {
            business.setActStatus(ActStatus.doing);
            business.setTaskId(nextTask.getId());
            business.setTaskNameId("");
            business.setTaskName("");
            business.setPriority("");
            business.setTodoUsers("");
        }

        if (ObjectUtil.isNotEmpty(taskBo.getNextApproval())) {
            business.setActStatus(ActStatus.doing);
            business.setTaskId(nextTask.getId());
            business.setTaskNameId(nextUserTask.getId());
            business.setTaskName(nextUserTask.getName());
            business.setPriority(nextUserTask.getPriority());
            business.setTodoUsers(taskBo.getNextApproval());
        }
    }

    private Task queryNextTask(String processInstanceId) {
        List<Task> taskList = taskService.createTaskQuery().processInstanceId(processInstanceId).active().list();
        if (taskList.size() == 1) {
            return taskService.createTaskQuery().processInstanceId(processInstanceId).active().singleResult();
        }
        return taskService.createTaskQuery().processInstanceId(processInstanceId).active().list().get(0);
    }

    private String resolveTodoUsers(WfTaskBo taskBo, List<SysUser> nextFlowNodeUserList) {
        if (nextFlowNodeUserList.isEmpty() || nextFlowNodeUserList.get(0) == null) {
            return "";
        }

        List<String> nickNames = new ArrayList<>();
        List<String> userNames = nextFlowNodeUserList.stream()
            .filter(Objects::nonNull)
            .filter(item -> item.getUserName() != null)
            .map(SysUser::getUserName)
            .collect(Collectors.toList());

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
            .processInstanceId(taskBo.getProcInsId())
            .singleResult();
        String startUserId = processInstance.getStartUserId();

        if (taskBo.getVariables() != null && taskBo.getVariables().containsKey("approval")) {
            SysUser sysUser = commonService.getSysUserByUserName(taskBo.getVariables().get("approval").toString());
            nickNames.add(sysUser.getNickName());
            return JSON.toJSONString(nickNames);
        }

        for (String userName : userNames) {
            if (StrUtil.equalsAnyIgnoreCase(userName, "${INITIATOR}")) {
                SysUser sysUser = commonService.getSysUserByUserName(startUserId);
                nickNames.add(sysUser.getNickName());
            } else {
                SysUser sysUser = commonService.getSysUserByUserName(userName);
                nickNames.add(sysUser.getNickName());
            }
        }
        return JSON.toJSONString(nickNames);
    }
}
