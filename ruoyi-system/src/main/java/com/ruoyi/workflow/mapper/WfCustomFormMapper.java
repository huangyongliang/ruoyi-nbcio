package com.ruoyi.workflow.mapper;

import com.ruoyi.workflow.domain.WfCustomForm;
import com.ruoyi.workflow.domain.bo.WfCustomFormBo;
import com.ruoyi.workflow.domain.vo.CustomFormVo;
import com.ruoyi.workflow.domain.vo.WfCustomFormVo;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.ruoyi.common.core.mapper.BaseMapperPlus;

/**
 * 流程业务单Mapper接口
 *
 * @author nbacheng
 * @date 2023-10-09
 */
public interface WfCustomFormMapper extends BaseMapperPlus<WfCustomFormMapper, WfCustomForm, WfCustomFormVo> {
	void updateCustom(@Param("customFormVo") CustomFormVo customFormVo);
	WfCustomForm selectSysCustomFormById(Long formId);
	List<WfCustomForm> selectSysCustomFormByServiceName(String serviceName);
	WfCustomFormBo selectSysCustomFormByDeployId(String deployId);
	@Select("select business_service from wf_custom_form where deploy_id = #{deployId}")
	String selectServiceNameByDeployId(@Param("deployId") String deployId);
}
