<template>
  <div class="app-container dingtalk-attendance">
    <el-form ref="queryForm" :model="queryParams" size="small" :inline="true" v-show="showSearch" label-width="76px">
      <el-form-item label="时间范围">
        <el-date-picker
          v-model="queryRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="yyyy-MM-dd HH:mm:ss"
          clearable
        />
      </el-form-item>
      <el-form-item label="钉钉用户" prop="dingUserId">
        <el-input
          v-model="queryParams.dingUserId"
          placeholder="钉钉 userId"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="姓名" prop="userName">
        <el-input
          v-model="queryParams.userName"
          placeholder="姓名"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="打卡类型" prop="checkType">
        <el-select v-model="queryParams.checkType" placeholder="全部" clearable>
          <el-option label="上班" value="OnDuty" />
          <el-option label="下班" value="OffDuty" />
        </el-select>
      </el-form-item>
      <el-form-item label="考勤结果" prop="timeResult">
        <el-select v-model="queryParams.timeResult" placeholder="全部" clearable>
          <el-option
            v-for="item in timeResultOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-refresh"
          size="mini"
          @click="openSync"
          v-hasPermi="['system:dingtalkAttendance:sync']"
        >同步考勤</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-setting"
          size="mini"
          @click="openConfig"
          v-hasPermi="['system:dingtalkAttendance:config']"
        >钉钉配置</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="el-icon-upload2"
          size="mini"
          @click="openImport"
          v-hasPermi="['system:dingtalkAttendance:import']"
        >导入历史Excel</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-document-copy"
          size="mini"
          @click="openMerge"
          v-hasPermi="['system:dingtalkAttendance:import']"
        >合并Excel</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <div class="stat-grid">
      <div class="metric-card">
        <div class="metric-title">打卡记录</div>
        <div class="metric-value">{{ summary.totalCount || 0 }}</div>
      </div>
      <div class="metric-card is-green">
        <div class="metric-title">正常记录</div>
        <div class="metric-value">{{ summary.normalCount || 0 }}</div>
      </div>
      <div class="metric-card is-orange">
        <div class="metric-title">异常记录</div>
        <div class="metric-value">{{ summary.exceptionCount || 0 }}</div>
      </div>
      <div class="metric-card is-red">
        <div class="metric-title">迟到</div>
        <div class="metric-value">{{ summary.lateCount || 0 }}</div>
      </div>
      <div class="metric-card is-blue">
        <div class="metric-title">参与人数</div>
        <div class="metric-value">{{ summary.userCount || 0 }}</div>
      </div>
      <div class="metric-card is-indigo">
        <div class="metric-title">平均工时</div>
        <div class="metric-value">{{ summary.averageWorkHours || 0 }}</div>
      </div>
    </div>

    <el-row :gutter="16" class="report-row">
      <el-col :xs="24" :sm="24" :lg="16">
        <div class="report-panel">
          <div class="panel-title">每日考勤趋势</div>
          <div ref="trendChart" class="chart"></div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="24" :lg="8">
        <div class="report-panel">
          <div class="panel-title">考勤结果分布</div>
          <div ref="resultChart" class="chart"></div>
        </div>
      </el-col>
    </el-row>

    <div class="report-panel person-panel">
      <div class="panel-title">个人统计</div>
      <el-table v-loading="loading" :data="personStatsList" border size="mini">
        <el-table-column label="姓名" align="center" prop="userName" width="120" show-overflow-tooltip>
          <template slot-scope="scope">
            <span>{{ scope.row.userName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="UserId" align="center" prop="dingUserId" width="150" show-overflow-tooltip />
        <el-table-column label="部门" align="center" prop="deptName" min-width="150" show-overflow-tooltip />
        <el-table-column label="上班天数" align="center" prop="workDays" width="96" />
        <el-table-column label="请假天数" align="center" prop="leaveDays" width="96" />
        <el-table-column label="休息天数" align="center" prop="restDays" width="96" />
        <el-table-column label="旷工天数" align="center" prop="absenteeismDays" width="96" />
        <el-table-column label="工作小时" align="center" width="96">
          <template slot-scope="scope">
            <span>{{ minutesToHours(scope.row.workMinutes) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="加班小时" align="center" width="96">
          <template slot-scope="scope">
            <span>{{ formatHours(scope.row.overtimeHours) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="出差小时" align="center" prop="businessTripHours" width="96" />
        <el-table-column label="外出小时" align="center" prop="outsideHours" width="96" />
        <el-table-column label="迟到" align="center" prop="lateCount" width="72" />
        <el-table-column label="早退" align="center" prop="earlyCount" width="72" />
        <el-table-column label="缺卡" align="center" prop="missingCardCount" width="72" />
        <el-table-column label="异常天数" align="center" prop="exceptionCount" width="88" />
        <el-table-column label="统计记录" align="center" prop="recordCount" width="88" />
      </el-table>
    </div>

    <el-table v-loading="loading" :data="recordList">
      <el-table-column label="钉钉用户" align="center" prop="dingUserId" width="150" show-overflow-tooltip />
      <el-table-column label="姓名" align="center" prop="userName" width="120" show-overflow-tooltip>
        <template slot-scope="scope">
          <span>{{ scope.row.userName || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="打卡时间" align="center" prop="checkTime" width="170">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.checkTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="工作日" align="center" prop="workDate" width="120">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.workDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="类型" align="center" prop="checkType" width="90">
        <template slot-scope="scope">
          <el-tag size="mini" :type="scope.row.checkType === 'OnDuty' ? 'primary' : 'info'">
            {{ checkTypeLabel(scope.row.checkType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="结果" align="center" prop="timeResult" width="110">
        <template slot-scope="scope">
          <el-tag size="mini" :type="resultTagType(scope.row)">
            {{ resultLabel(scope.row) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="打卡地址" align="left" prop="userAddress" show-overflow-tooltip />
      <el-table-column label="来源" align="center" prop="sourceType" width="110" show-overflow-tooltip />
      <el-table-column label="历史明细" align="center" prop="dailyDetailId" width="100">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.dailyDetailId" size="mini" type="info">已关联</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="来源文件" align="left" prop="sourceFile" min-width="180" show-overflow-tooltip>
        <template slot-scope="scope">
          <span>{{ scope.row.sourceFile || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="同步时间" align="center" prop="updateTime" width="170">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.updateTime || scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="90" fixed="right">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="handleDetail(scope.row)"
            v-hasPermi="['system:dingtalkAttendance:list']"
          >详细</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog title="钉钉考勤同步" :visible.sync="syncOpen" width="620px" append-to-body>
      <el-form ref="syncForm" :model="syncForm" label-width="96px">
        <el-form-item label="同步时间">
          <el-date-picker
            v-model="syncRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="yyyy-MM-dd HH:mm:ss"
            clearable
          />
        </el-form-item>
        <el-form-item label="钉钉用户">
          <el-input
            v-model="syncForm.userIds"
            type="textarea"
            :rows="4"
            placeholder="多个 userId 可用逗号或换行分隔"
          />
        </el-form-item>
        <el-form-item label="AppKey">
          <el-input v-model="syncForm.appKey" placeholder="为空时使用已保存配置" />
        </el-form-item>
        <el-form-item label="AppSecret">
          <el-input v-model="syncForm.appSecret" placeholder="为空时使用已保存配置" show-password />
        </el-form-item>
        <el-form-item label="保存配置">
          <el-checkbox v-model="syncForm.saveConfig">保存本次配置</el-checkbox>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="syncLoading" @click="submitSync">确 定</el-button>
        <el-button @click="syncOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog title="钉钉考勤配置" :visible.sync="configOpen" width="620px" append-to-body>
      <el-form ref="configForm" :model="configForm" label-width="96px">
        <el-form-item label="启用同步">
          <el-switch v-model="configForm.enabled" />
        </el-form-item>
        <el-form-item label="AppKey">
          <el-input v-model="configForm.appKey" />
        </el-form-item>
        <el-form-item label="AppSecret">
          <el-input v-model="configForm.appSecret" show-password />
        </el-form-item>
        <el-form-item label="钉钉用户">
          <el-input
            v-model="configForm.userIds"
            type="textarea"
            :rows="5"
            placeholder="多个 userId 可用逗号或换行分隔"
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="configLoading" @click="submitConfig">确 定</el-button>
        <el-button @click="configOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog :title="upload.title" :visible.sync="upload.open" width="420px" append-to-body>
      <el-upload
        ref="upload"
        :limit="1"
        accept=".xlsx, .xls"
        :headers="upload.headers"
        :action="uploadAction"
        :disabled="upload.isUploading"
        :on-progress="handleFileUploadProgress"
        :on-success="handleFileSuccess"
        :on-error="handleFileError"
        :auto-upload="false"
        drag
      >
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <div class="el-upload__tip" slot="tip">
          <el-checkbox v-model="upload.updateSupport">更新已存在历史明细</el-checkbox>
          <div>支持钉钉导出的每日统计 .xls/.xlsx 文件</div>
        </div>
      </el-upload>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="upload.isUploading" @click="submitFileForm">确 定</el-button>
        <el-button @click="upload.open = false">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog :title="mergeUpload.title" :visible.sync="mergeUpload.open" width="460px" append-to-body>
      <el-upload
        ref="mergeUpload"
        :limit="20"
        accept=".xlsx, .xls"
        action="#"
        :disabled="mergeUpload.isUploading"
        :auto-upload="false"
        :file-list="mergeUpload.fileList"
        :on-change="handleMergeFileChange"
        :on-remove="handleMergeFileRemove"
        multiple
        drag
      >
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将多个文件拖到此处，或<em>点击选择</em></div>
        <div class="el-upload__tip" slot="tip">
          至少选择 2 个钉钉每日统计 Excel，系统会保留格式和数据，并按姓名、日期排序后下载合并文件
        </div>
      </el-upload>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="mergeUpload.isUploading" @click="submitMergeFileForm">合 并 下 载</el-button>
        <el-button @click="mergeUpload.open = false">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog title="钉钉考勤详细信息" :visible.sync="detailOpen" width="900px" append-to-body>
      <div v-loading="detailLoading">
        <div class="detail-section-title">人员与打卡</div>
        <el-descriptions :column="2" border size="small" class="detail-descriptions">
          <el-descriptions-item label="记录ID">{{ detail.recordId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="钉钉记录ID">{{ detail.dingRecordId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ detail.userName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="钉钉用户">{{ detail.dingUserId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="工作日">{{ parseTime(detail.workDate) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="实际打卡时间">{{ parseTime(detail.checkTime) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="排班打卡时间">{{ parseTime(detail.baseCheckTime) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="用户打卡时间">{{ parseTime(detail.userCheckTime) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="打卡类型">{{ checkTypeLabel(detail.checkType) }}</el-descriptions-item>
          <el-descriptions-item label="考勤结果">{{ timeResultLabel(detail.timeResult) }}</el-descriptions-item>
        </el-descriptions>

        <div class="detail-section-title">来源与定位</div>
        <el-descriptions :column="2" border size="small" class="detail-descriptions">
          <el-descriptions-item label="来源">{{ detail.sourceType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="历史明细ID">{{ detail.dailyDetailId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="来源文件">{{ detail.sourceFile || '-' }}</el-descriptions-item>
          <el-descriptions-item label="导入批次">{{ detail.importBatchNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="加班小时">{{ formatHours(detail.overtimeHours) }}</el-descriptions-item>
          <el-descriptions-item label="加班审批统计">{{ detail.overtimeApprovalStats || '-' }}</el-descriptions-item>
          <el-descriptions-item label="位置结果">{{ detail.locationResult || '-' }}</el-descriptions-item>
          <el-descriptions-item label="定位方式">{{ detail.locationMethod || '-' }}</el-descriptions-item>
          <el-descriptions-item label="经度">{{ detail.userLongitude || '-' }}</el-descriptions-item>
          <el-descriptions-item label="纬度">{{ detail.userLatitude || '-' }}</el-descriptions-item>
          <el-descriptions-item label="打卡地址" :span="2">{{ detail.userAddress || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="detail-section-title">钉钉扩展信息</div>
        <el-descriptions :column="2" border size="small" class="detail-descriptions">
          <el-descriptions-item label="业务ID">{{ detail.bizId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="企业ID">{{ detail.corpId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="考勤组ID">{{ detail.groupId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="排班计划ID">{{ detail.planId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审批实例ID">{{ detail.procInstId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审批单ID">{{ detail.approveId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="设备ID">{{ detail.deviceId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ parseTime(detail.updateTime || detail.createTime) || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="detail-section-title">原始数据</div>
        <pre class="raw-data">{{ formatRawData(detail.rawData) }}</pre>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="detailOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import * as echarts from 'echarts'
require('echarts/theme/macarons')
import { getToken } from '@/utils/auth'
import {
  getDingTalkAttendance,
  getDingTalkAttendanceConfig,
  getDingTalkAttendancePersonStats,
  getDingTalkAttendanceSummary,
  getDingTalkAttendanceTrend,
  listDingTalkAttendance,
  mergeDingTalkAttendanceExcels,
  saveDingTalkAttendanceConfig,
  syncDingTalkAttendance
} from '@/api/system/dingtalkAttendance'
import { blobValidate, parseTime } from '@/utils/ruoyi'

export default {
  name: 'DingTalkAttendance',
  data() {
    return {
      loading: true,
      syncLoading: false,
      configLoading: false,
      showSearch: true,
      total: 0,
      recordList: [],
      personStatsList: [],
      queryRange: [],
      syncRange: [],
      summary: {},
      trendData: {},
      trendChart: null,
      resultChart: null,
      syncOpen: false,
      configOpen: false,
      detailOpen: false,
      detailLoading: false,
      detail: {},
      upload: {
        open: false,
        title: '导入钉钉考勤历史Excel',
        isUploading: false,
        updateSupport: true,
        headers: { Authorization: 'Bearer ' + getToken() },
        url: process.env.VUE_APP_BASE_API + '/system/dingtalkAttendance/importData'
      },
      mergeUpload: {
        open: false,
        title: '合并钉钉考勤Excel',
        isUploading: false,
        fileList: []
      },
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        dingUserId: undefined,
        userName: undefined,
        checkType: undefined,
        timeResult: undefined
      },
      syncForm: {
        userIds: '',
        appKey: '',
        appSecret: '',
        saveConfig: true
      },
      configForm: {
        enabled: true,
        appKey: '',
        appSecret: '',
        userIds: ''
      },
      timeResultOptions: [
        { label: '正常', value: 'Normal' },
        { label: '迟到', value: 'Late' },
        { label: '严重迟到', value: 'SeriousLate' },
        { label: '早退', value: 'Early' },
        { label: '旷工', value: 'Absenteeism' },
        { label: '未打卡', value: 'NotSigned' },
        { label: '请假', value: 'Leave' },
        { label: '休息', value: 'Rest' },
        { label: '外出', value: 'Field' },
        { label: '出差', value: 'BusinessTrip' },
        { label: '补卡审批通过', value: 'Approval' }
      ]
    }
  },
  computed: {
    uploadAction() {
      return this.upload.url + '?updateSupport=' + this.upload.updateSupport
    }
  },
  created() {
    this.initRanges()
    this.loadConfig()
    this.getList()
  },
  mounted() {
    window.addEventListener('resize', this.resizeCharts)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resizeCharts)
    if (this.trendChart) {
      this.trendChart.dispose()
    }
    if (this.resultChart) {
      this.resultChart.dispose()
    }
  },
  methods: {
    parseTime,
    initRanges() {
      const end = new Date()
      const begin = new Date()
      begin.setDate(begin.getDate() - 6)
      begin.setHours(0, 0, 0, 0)
      this.queryRange = [parseTime(begin), parseTime(end)]

      const todayBegin = new Date()
      todayBegin.setHours(0, 0, 0, 0)
      this.syncRange = [parseTime(todayBegin), parseTime(end)]
    },
    loadConfig() {
      getDingTalkAttendanceConfig().then(res => {
        this.configForm = Object.assign({}, this.configForm, res.data || {})
        this.configForm.appSecret = ''
        this.syncForm.userIds = this.configForm.userIds || ''
      })
    },
    buildQuery() {
      const params = Object.assign({}, this.queryParams)
      if (this.queryRange && this.queryRange.length === 2) {
        params.beginTime = this.queryRange[0]
        params.endTime = this.queryRange[1]
      }
      return params
    },
    getList() {
      this.loading = true
      const params = this.buildQuery()
      Promise.all([
        listDingTalkAttendance(params),
        getDingTalkAttendanceSummary(params),
        getDingTalkAttendanceTrend(params),
        getDingTalkAttendancePersonStats(params)
      ]).then(([listRes, summaryRes, trendRes, personStatsRes]) => {
        this.recordList = listRes.rows || []
        this.total = listRes.total || 0
        this.summary = summaryRes.data || {}
        this.trendData = trendRes.data || {}
        this.personStatsList = personStatsRes.data || []
        this.$nextTick(() => {
          this.renderCharts()
        })
      }).finally(() => {
        this.loading = false
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.queryParams = {
        pageNum: 1,
        pageSize: 10,
        dingUserId: undefined,
        userName: undefined,
        checkType: undefined,
        timeResult: undefined
      }
      this.initRanges()
      this.getList()
    },
    openSync() {
      if (this.configForm.enabled === false) {
        this.$modal.msgError('请先启用钉钉考勤同步')
        return
      }
      this.syncForm = {
        userIds: this.configForm.userIds || '',
        appKey: '',
        appSecret: '',
        saveConfig: true
      }
      this.syncOpen = true
    },
    submitSync() {
      if (!this.syncRange || this.syncRange.length !== 2) {
        this.$modal.msgError('请选择同步时间范围')
        return
      }
      this.syncLoading = true
      syncDingTalkAttendance({
        checkDateFrom: this.syncRange[0],
        checkDateTo: this.syncRange[1],
        userIds: this.syncForm.userIds,
        appKey: this.syncForm.appKey,
        appSecret: this.syncForm.appSecret,
        saveConfig: this.syncForm.saveConfig
      }).then(res => {
        this.$modal.msgSuccess('同步完成，写入 ' + (res.data || 0) + ' 条记录')
        this.syncOpen = false
        this.loadConfig()
        this.getList()
      }).finally(() => {
        this.syncLoading = false
      })
    },
    openConfig() {
      getDingTalkAttendanceConfig().then(res => {
        this.configForm = Object.assign({
          enabled: true,
          appKey: '',
          appSecret: '',
          userIds: ''
        }, res.data || {})
        this.configForm.appSecret = ''
        this.configOpen = true
      })
    },
    openImport() {
      this.upload.title = '导入钉钉考勤历史Excel'
      this.upload.open = true
    },
    openMerge() {
      this.mergeUpload.title = '合并钉钉考勤Excel'
      this.mergeUpload.fileList = []
      this.mergeUpload.open = true
      this.$nextTick(() => {
        if (this.$refs.mergeUpload) {
          this.$refs.mergeUpload.clearFiles()
        }
      })
    },
    handleFileUploadProgress() {
      this.upload.isUploading = true
    },
    handleFileSuccess(response) {
      this.upload.isUploading = false
      if (response.code === 200) {
        this.upload.open = false
        this.$refs.upload.clearFiles()
        this.$alert(response.msg, '导入结果', { dangerouslyUseHTMLString: true })
        this.getList()
      } else {
        this.$modal.msgError(response.msg || '导入失败')
      }
    },
    handleFileError() {
      this.upload.isUploading = false
      this.$modal.msgError('导入失败，请检查文件格式或服务状态')
    },
    submitFileForm() {
      this.$refs.upload.submit()
    },
    handleMergeFileChange(file, fileList) {
      this.mergeUpload.fileList = fileList
    },
    handleMergeFileRemove(file, fileList) {
      this.mergeUpload.fileList = fileList
    },
    submitMergeFileForm() {
      if (!this.mergeUpload.fileList || this.mergeUpload.fileList.length < 2) {
        this.$modal.msgError('请至少选择两个Excel文件')
        return
      }
      const formData = new FormData()
      this.mergeUpload.fileList.forEach(file => {
        if (file.raw) {
          formData.append('files', file.raw)
        }
      })
      this.mergeUpload.isUploading = true
      mergeDingTalkAttendanceExcels(formData).then(res => {
        if (blobValidate(res.data)) {
          const blob = new Blob([res.data], { type: res.data.type || 'application/octet-stream' })
          const fileName = decodeURIComponent(res.headers['download-filename'] || '融资团队_每日统计_合并_按姓名排序.xlsx')
          this.$download.saveAs(blob, fileName)
          this.mergeUpload.open = false
          this.mergeUpload.fileList = []
          if (this.$refs.mergeUpload) {
            this.$refs.mergeUpload.clearFiles()
          }
        } else {
          this.$download.printErrMsg(res.data)
        }
      }).catch(error => {
        if (error && error.response && error.response.data) {
          this.$download.printErrMsg(error.response.data)
        } else {
          this.$modal.msgError('合并下载失败，请检查文件格式或服务状态')
        }
      }).finally(() => {
        this.mergeUpload.isUploading = false
      })
    },
    handleDetail(row) {
      if (!row || !row.recordId) {
        this.$modal.msgError('未找到考勤记录ID')
        return
      }
      this.detail = Object.assign({}, row)
      this.detailOpen = true
      this.detailLoading = true
      getDingTalkAttendance(row.recordId).then(res => {
        this.detail = res.data || row
      }).finally(() => {
        this.detailLoading = false
      })
    },
    submitConfig() {
      this.configLoading = true
      saveDingTalkAttendanceConfig(this.configForm).then(() => {
        this.$modal.msgSuccess('保存成功')
        this.configOpen = false
        this.loadConfig()
      }).finally(() => {
        this.configLoading = false
      })
    },
    renderCharts() {
      this.renderTrendChart()
      this.renderResultChart()
    },
    renderTrendChart() {
      if (!this.$refs.trendChart) {
        return
      }
      if (!this.trendChart) {
        this.trendChart = echarts.init(this.$refs.trendChart, 'macarons')
      }
      const trend = this.trendData || {}
      this.trendChart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { top: 0, data: ['总记录', '正常', '异常', '迟到', '早退'] },
        grid: { top: 44, left: 34, right: 18, bottom: 28, containLabel: true },
        xAxis: { type: 'category', boundaryGap: false, data: trend.dates || [] },
        yAxis: { type: 'value', minInterval: 1 },
        series: [
          { name: '总记录', type: 'line', smooth: true, data: trend.totalCounts || [] },
          { name: '正常', type: 'line', smooth: true, data: trend.normalCounts || [] },
          { name: '异常', type: 'line', smooth: true, data: trend.exceptionCounts || [] },
          { name: '迟到', type: 'bar', barMaxWidth: 24, data: trend.lateCounts || [] },
          { name: '早退', type: 'bar', barMaxWidth: 24, data: trend.earlyCounts || [] }
        ]
      })
    },
    renderResultChart() {
      if (!this.$refs.resultChart) {
        return
      }
      if (!this.resultChart) {
        this.resultChart = echarts.init(this.$refs.resultChart, 'macarons')
      }
      const data = (this.trendData && this.trendData.resultDistribution && this.trendData.resultDistribution.length)
        ? this.trendData.resultDistribution
        : [{ name: '暂无数据', value: 0 }]
      this.resultChart.setOption({
        tooltip: { trigger: 'item' },
        legend: { bottom: 0 },
        series: [{
          name: '考勤结果',
          type: 'pie',
          radius: ['46%', '70%'],
          center: ['50%', '46%'],
          avoidLabelOverlap: true,
          data: data
        }]
      })
    },
    resizeCharts() {
      if (this.trendChart) {
        this.trendChart.resize()
      }
      if (this.resultChart) {
        this.resultChart.resize()
      }
    },
    checkTypeLabel(value) {
      if (value === 'OnDuty') {
        return '上班'
      }
      if (value === 'OffDuty') {
        return '下班'
      }
      return value || '-'
    },
    resultLabel(row) {
      if (row.timeResult && row.timeResult !== 'Normal') {
        return this.timeResultLabel(row.timeResult)
      }
      if (row.locationResult && row.locationResult !== 'Normal') {
        return '位置异常'
      }
      return '正常'
    },
    timeResultLabel(value) {
      const item = this.timeResultOptions.find(option => option.value === value)
      return item ? item.label : (value || '正常')
    },
    minutesToHours(value) {
      if (value === null || value === undefined || value === '') {
        return '-'
      }
      const minutes = Number(value)
      if (Number.isNaN(minutes)) {
        return value
      }
      return (minutes / 60).toFixed(2)
    },
    formatHours(value) {
      if (value === null || value === undefined || value === '') {
        return '-'
      }
      const hours = Number(value)
      if (Number.isNaN(hours)) {
        return value
      }
      return hours.toFixed(2)
    },
    formatRawData(value) {
      if (!value) {
        return '-'
      }
      try {
        return JSON.stringify(JSON.parse(value), null, 2)
      } catch (error) {
        return value
      }
    },
    resultTagType(row) {
      if ((!row.timeResult || row.timeResult === 'Normal') && (!row.locationResult || row.locationResult === 'Normal')) {
        return 'success'
      }
      if (row.timeResult === 'Leave' || row.timeResult === 'Rest' || row.timeResult === 'Field' || row.timeResult === 'BusinessTrip' || row.timeResult === 'Approval') {
        return 'info'
      }
      if (row.timeResult === 'Late' || row.timeResult === 'SeriousLate' || row.timeResult === 'Early') {
        return 'warning'
      }
      return 'danger'
    }
  }
}
</script>

<style scoped>
.dingtalk-attendance .el-date-editor--datetimerange {
  width: 360px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(120px, 1fr));
  gap: 12px;
  margin: 12px 0 16px;
}

.metric-card {
  min-height: 82px;
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  border-left: 4px solid #409eff;
  background: #fff;
}

.metric-card.is-green {
  border-left-color: #38a169;
}

.metric-card.is-orange {
  border-left-color: #d69e2e;
}

.metric-card.is-red {
  border-left-color: #e53e3e;
}

.metric-card.is-blue {
  border-left-color: #3182ce;
}

.metric-card.is-indigo {
  border-left-color: #5a67d8;
}

.metric-title {
  color: #606266;
  font-size: 13px;
  line-height: 20px;
}

.metric-value {
  margin-top: 8px;
  color: #1f2937;
  font-size: 26px;
  font-weight: 600;
  line-height: 32px;
}

.report-row {
  margin-bottom: 16px;
}

.person-panel {
  margin-bottom: 16px;
}

.report-panel {
  border: 1px solid #e5e7eb;
  background: #fff;
}

.panel-title {
  height: 42px;
  padding: 0 16px;
  border-bottom: 1px solid #ebeef5;
  color: #303133;
  font-size: 14px;
  font-weight: 600;
  line-height: 42px;
}

.chart {
  height: 320px;
}

.detail-section-title {
  margin: 14px 0 10px;
  color: #303133;
  font-size: 14px;
  font-weight: 600;
}

.detail-section-title:first-child {
  margin-top: 0;
}

.detail-descriptions {
  margin-bottom: 4px;
}

.raw-data {
  max-height: 260px;
  margin: 0;
  padding: 12px;
  overflow: auto;
  border: 1px solid #e5e7eb;
  background: #f8fafc;
  color: #1f2937;
  font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
  font-size: 12px;
  line-height: 18px;
  white-space: pre-wrap;
  word-break: break-all;
}

@media (max-width: 1280px) {
  .stat-grid {
    grid-template-columns: repeat(3, minmax(120px, 1fr));
  }
}

@media (max-width: 768px) {
  .dingtalk-attendance .el-date-editor--datetimerange {
    width: 100%;
  }

  .stat-grid {
    grid-template-columns: repeat(2, minmax(120px, 1fr));
  }
}
</style>
