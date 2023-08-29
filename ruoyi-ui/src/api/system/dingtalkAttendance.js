import axios from 'axios'
import request from '@/utils/request'
import { getToken } from '@/utils/auth'

// 查询钉钉考勤记录
export function listDingTalkAttendance(query) {
  return request({
    url: '/system/dingtalkAttendance/list',
    method: 'get',
    params: query
  })
}

// 查询钉钉考勤记录详细
export function getDingTalkAttendance(recordId) {
  return request({
    url: '/system/dingtalkAttendance/' + recordId,
    method: 'get'
  })
}

// 查询钉钉考勤汇总
export function getDingTalkAttendanceSummary(query) {
  return request({
    url: '/system/dingtalkAttendance/summary',
    method: 'get',
    params: query
  })
}

// 查询钉钉考勤趋势
export function getDingTalkAttendanceTrend(query) {
  return request({
    url: '/system/dingtalkAttendance/trend',
    method: 'get',
    params: query
  })
}

// 查询钉钉考勤个人统计
export function getDingTalkAttendancePersonStats(query) {
  return request({
    url: '/system/dingtalkAttendance/personStats',
    method: 'get',
    params: query
  })
}

// 获取钉钉考勤配置
export function getDingTalkAttendanceConfig() {
  return request({
    url: '/system/dingtalkAttendance/config',
    method: 'get'
  })
}

// 保存钉钉考勤配置
export function saveDingTalkAttendanceConfig(data) {
  return request({
    url: '/system/dingtalkAttendance/config',
    method: 'put',
    data: data
  })
}

// 同步钉钉考勤数据
export function syncDingTalkAttendance(data) {
  return request({
    url: '/system/dingtalkAttendance/sync',
    method: 'post',
    data: data
  })
}

// 合并钉钉考勤每日统计Excel
export function mergeDingTalkAttendanceExcels(data) {
  return axios({
    url: process.env.VUE_APP_BASE_API + '/system/dingtalkAttendance/mergeExcel',
    method: 'post',
    data: data,
    responseType: 'blob',
    timeout: 300000,
    headers: { Authorization: 'Bearer ' + getToken() }
  })
}
