<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="流程标识" prop="processKey">
        <el-input
          v-model="queryParams.processKey"
          placeholder="请输入流程标识"
          clearable
          size="small"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="流程名称" prop="processName">
        <el-input
          v-model="queryParams.processName"
          placeholder="请输入流程名称"
          clearable
          size="small"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="流程分类" prop="category">
        <el-select v-model="queryParams.category" clearable placeholder="请选择" size="small">
          <el-option
            v-for="item in categoryOptions"
            :key="item.categoryId"
            :label="item.categoryName"
            :value="item.code">
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="提交时间">
        <el-date-picker
          v-model="dateRange"
          style="width: 240px"
          value-format="yyyy-MM-dd HH:mm:ss"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :default-time="['00:00:00', '23:59:59']"
        ></el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          v-hasPermi="['workflow:process:allExport']"
          @click="handleExport"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="ownProcessList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="流程编号" align="center" prop="procInsId" :show-overflow-tooltip="true"/>
      <el-table-column label="流程名称" align="center" prop="procDefName" :show-overflow-tooltip="true"/>
      <el-table-column label="流程类别" align="center" prop="category" :formatter="categoryFormat" />
      <el-table-column label="流程版本" align="center" width="80px">
        <template slot-scope="scope">
          <el-tag size="medium" >v{{ scope.row.procDefVersion }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="当前节点" align="center" prop="taskName"/>
      <el-table-column label="提交时间" align="center" prop="createTime" width="180"/>
      <el-table-column label="流程状态" align="center" width="100">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.wf_process_status" :value="scope.row.processStatus"/>
        </template>
      </el-table-column>
      <el-table-column label="耗时" align="center" prop="duration" width="180"/>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            type="text"
            size="mini"
            icon="el-icon-tickets"
            @click="handleFlowRecord(scope.row)"
            v-hasPermi="['workflow:process:query']"
          >详情</el-button>
          <el-button
            type="text"
            size="mini"
            icon="el-icon-delete"
            @click="handleDelegate(scope.row)"
            v-if="!scope.row.finishTime"
            v-hasPermi="['workflow:process:query']"
          >委派</el-button>
          <el-button
            type="text"
            size="mini"
            icon="el-icon-delete"
            @click="handleTransfer(scope.row)"
            v-if="!scope.row.finishTime"
            v-hasPermi="['workflow:process:query']"
          >转办</el-button>
          <el-button
            type="text"
            size="mini"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-if="scope.row.finishTime"
            v-hasPermi="['workflow:process:remove']"
          >删除</el-button>
          <el-button
            type="text"
            size="mini"
            icon="el-icon-circle-close"
            @click="handleStop(scope.row)"
            v-hasPermi="['workflow:process:cancel']"
          >取消</el-button>
          <!--<el-button
            type="text"
            size="mini"
            icon="el-icon-refresh-right"
            v-hasPermi="['workflow:process:start']"
            @click="handleAgain(scope.row)"
          >重新发起</el-button> -->
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />
    <!-- 委派转办选择人员窗口  -->
    <el-dialog :title="userData.title" :visible.sync="userData.open" width="60%" append-to-body>
      <el-row type="flex" :gutter="20">
        <!--部门数据-->
        <el-col :span="5">
          <el-card shadow="never" style="height: 100%">
            <div slot="header">
              <span>部门列表</span>
            </div>
            <div class="head-container">
              <el-input v-model="deptName" placeholder="请输入部门名称" clearable size="small" prefix-icon="el-icon-search"/>
              <el-tree
                :data="deptOptions"
                :props="deptProps"
                :expand-on-click-node="false"
                :filter-node-method="filterNode"
                ref="tree"
                default-expand-all
                @node-click="handleNodeClick"
              />
            </div>
          </el-card>
        </el-col>
        <el-col :span="18">
          <el-table ref="userTable"
                    :key="userData.type"
                    height="500"
                    v-loading="userLoading"
                    :data="userList"
                    highlight-current-row
                    @current-change="changeCurrentUser"
                    @selection-change="handleSelectionChange">
            <el-table-column width="50">
              <template slot-scope="scope">
                <el-radio :label="scope.row.userId" v-model="currentUserId">{{''}}</el-radio>
              </template>
            </el-table-column>
            <el-table-column label="用户名" align="center" prop="nickName" />
            <el-table-column label="手机" align="center" prop="phonenumber" />
            <el-table-column label="部门" align="center" prop="dept.deptName" />
          </el-table>
          <pagination
            :total="userTotal"
            :page.sync="queryDeptParams.pageNum"
            :limit.sync="queryDeptParams.pageSize"
            @pagination="getUserList"
          />
        </el-col>
      </el-row>
      <span slot="footer" class="dialog-footer">
        <el-button @click="userData.open = false">取 消</el-button>
        <el-button type="primary" @click="submitUserData">确 定</el-button>
      </span>
    </el-dialog>

  </div>
</template>

<script>
import { listAllProcess, stopProcess, delProcess } from '@/api/workflow/process';
import { delegate, transfer } from '@/api/workflow/task'
import { listAllCategory } from '@/api/workflow/category';
import { selectUser, deptTreeSelect } from '@/api/system/user'

export default {
  name: "All",
  dicts: ['wf_process_status'],
  components: {
  },
  data() {
    return {
      // 遮罩层
      loading: true,
      processLoading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      categoryOptions: [],
      processTotal:0,
      // 我发起的流程列表数据
      ownProcessList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      src: "",
      definitionList:[],
      // 日期范围
      dateRange: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        processKey: undefined,
        processName: undefined,
        category: undefined
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
      },
      userData: {
        title: '',
        type: '',
        open: false,
      },
      userTotal: 0,
      // 部门名称
      deptName: undefined,
      // 部门树选项
      deptOptions: undefined,
      userLoading: false,
      // 用户表格数据
      userList: null,
      deptProps: {
        children: "children",
        label: "label"
      },
      // 部门查询参数
      queryDeptParams: {
        pageNum: 1,
        pageSize: 10,
        deptId: undefined
      },
      currentUserId: null,
      //传入处理委派或转办参数
      taskForm: {
        userId: '',
        taskId: '',
        dataId: '',
        category: '',
        comment: '',
      }
    };
  },
  created() {
    this.getCategoryList();
  },
  beforeRouteEnter(to, from, next) {
    next(vm => {
      vm.getList()
    })
  },
  methods: {
    /** 查询流程分类列表 */
    getCategoryList() {
      listAllCategory().then(response => this.categoryOptions = response.data)
    },
    /** 查询部门下拉树结构 */
    getTreeSelect() {
      deptTreeSelect().then(response => {
        this.deptOptions = response.data;
      });
    },
    /** 查询流程定义列表 */
    getList() {
      this.loading = true;
      listAllProcess(this.addDateRange(this.queryParams, this.dateRange)).then(response => {
        this.ownProcessList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    /** 查询用户列表 */
    getUserList() {
      this.userLoading = true;
      selectUser(this.addDateRange(this.queryDeptParams, this.dateRange)).then(response => {
        this.userList = response.rows;
        this.total = response.total;
        //this.toggleSelection(this.userMultipleSelection);
        this.userLoading = false;
      });
    },
    // 筛选节点
    filterNode(value, data) {
      if (!value) return true;
      return data.label.indexOf(value) !== -1;
    },
    // 节点单击事件
    handleNodeClick(data) {
      this.queryDeptParams.deptId = data.id;
      this.getUserList();
    },
    changeCurrentUser(val) {
      this.currentUserId = val.userId
    },
    /** 委派任务 */
    handleDelegate(row) {
      this.taskForm.taskId = row.taskId;
      this.taskForm.taskId = row.taskId;
      this.taskForm.dataId = row.businessKey;
      this.taskForm.category = row.category;
      this.taskForm.comment = '系统委派';
      this.userData.type = 'delegate';
      this.userData.title = '委派任务'
      this.userData.open = true;
      this.getTreeSelect();
    },
    /** 转办任务 */
    handleTransfer(row){
      this.taskForm.taskId = row.taskId;
      this.taskForm.taskId = row.taskId;
      this.taskForm.dataId = row.businessKey;
      this.taskForm.category = row.category;
      this.taskForm.comment = '系统转办';
      this.userData.type = 'transfer';
      this.userData.title = '转办任务';
      this.userData.open = true;
      this.getTreeSelect();
    },
    submitUserData() {
      let type = this.userData.type;
      if (!this.currentUserId) {
        this.$modal.msgError("请选择用户");
        return false;
      }
      this.userData.open = false;
      this.taskForm.userId = this.currentUserId;
      if (type === 'delegate') {
        delegate(this.taskForm).then(res => {
          this.$modal.msgSuccess(res.msg);
          this.goBack();
        });
      }
      if (type === 'transfer') {
        transfer(this.taskForm).then(res => {
          this.$modal.msgSuccess(res.msg);
          this.goBack();
        });
      }
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        id: null,
        name: null,
        category: null,
        key: null,
        tenantId: null,
        deployTime: null,
        derivedFrom: null,
        derivedFromRoot: null,
        parentDeploymentId: null,
        engineVersion: null
      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.dateRange = [];
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.procInsId);
      this.single = selection.length !== 1;
      this.multiple = !selection.length;
    },
    handleAgain(row) {
      if(row.dataId != null) {

      }
      else {
          this.$router.push({
          path: '/workflow/process/start/' + row.deployId,
          query: {
            definitionId: row.procDefId,
            procInsId: row.procInsId
          }
        })
      }

      console.log(row);
    },
    /**  取消流程申请 */
    handleStop(row){
      const params = {
        procInsId: row.procInsId,
        dataId: row.dataId
      }
      stopProcess(params).then( res => {
        this.$modal.msgSuccess(res.msg);
        this.getList();
      });
    },
    /** 流程流转记录 */
    handleFlowRecord(row) {
      this.$router.push({
        path: '/workflow/process/detail/' + row.procInsId,
        query: {
          taskId: row.taskId,
          processed: false
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.procInsId || this.ids;
      this.$confirm('是否确认删除流程定义编号为"' + ids + '"的数据项?', "警告", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }).then(function() {
        return delProcess(ids);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      })
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('workflow/process/allExport', {
        ...this.queryParams
      }, `wf_own_process_${new Date().getTime()}.xlsx`)
    },
    categoryFormat(row, column) {
      return this.categoryOptions.find(k => k.code === row.category)?.categoryName ?? '';
    }
  }
};
</script>
