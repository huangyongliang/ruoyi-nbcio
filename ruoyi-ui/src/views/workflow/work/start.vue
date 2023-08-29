<template>
  <div class="app-container">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <span>发起流程</span>
      </div>
      <el-col :span="18" :offset="3" v-if="formOpen">
        <form-builder ref="formBuilder" v-model="formVal" :buildData="formCode" />
        <div style="margin-bottom:15px;text-align:center">
            <el-button type="primary" class="button" @click="submitForm">提交</el-button>
        </div>
      </el-col>
      <!--初始化流程加载显示formdesigner表单-->
      <el-col :span="18" :offset="3" v-if="formViewOpen">
        <div class="test-form">
          <form-viewer ref="formView" v-model="formVal" :buildData="formCode" />
        </div>
      </el-col>
    </el-card>
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
            <el-table-column v-if="userData.type === 'copy' || userData.type === 'next' || userData.type === 'addSign' || userData.type === 'approval'" width="55" type="selection" />
            <el-table-column v-else width="30">
              <template slot-scope="scope">
                <el-radio :label="scope.row.userId" v-model="currentUserId">{{''}}</el-radio>
              </template>
            </el-table-column>
            <el-table-column label="用户名" align="center" prop="nickName" />
            <el-table-column label="手机" align="center" prop="phonenumber" />
            <el-table-column label="部门" align="center" prop="dept.deptName" />
          </el-table>
          <pagination
            :total="total"
            :page.sync="queryParams.pageNum"
            :limit.sync="queryParams.pageSize"
            @pagination="getList"
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
  import { selectUser, deptTreeSelect } from '@/api/system/user'
  import { getProcessForm, startProcess } from '@/api/workflow/process'
  import { complete} from '@/api/workflow/task'
  import Parser from '@/utils/generator/parser'
  //for formdesigner
  import formBuilder from '@/components/formdesigner/components/formBuilder'
  import formViewer from '@/components/formdesigner/components/formViewer'

export default {
  name: 'WorkStart',
  components: {
    Parser,
    formBuilder,
    formViewer,
  },
  data() {
    return {
      definitionId: null,
      deployId: null,
      procInsId: null,
      formOpen: false,
      formData: {}, // formdesigner 默认表单数据
      formCode:'', //formdesigner 变量
      formVal:'',  //formdesigner 数据
      formViewOpen: false,  //是否显示formdesigner的输入后提交的表单
      formViewData: '',    //显示formdesigner的输入后提交的表单数据
      userData: {
        title: '',
        type: '',
        open: false,
      },
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
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        deptId: undefined
      },
      total: 0,
      currentUserId: null,
      nextApproval: [],
      userMultipleSelection: [],
      taskForm:{
        comment:"", // 意见内容
        procInsId: "", // 流程实例编号
        taskId: "" ,// 流程任务编号
        copyUserIds: "", // 抄送人Id
        vars: "",
        targetKey:""
      },
    }
  },
  created() {
    this.initData();
  },
  methods: {
    initData() {
      this.deployId = this.$route.params && this.$route.params.deployId;
      this.definitionId = this.$route.query && this.$route.query.definitionId;
      this.procInsId = this.$route.query && this.$route.query.procInsId;
      getProcessForm({
        definitionId: this.definitionId,
        deployId: this.deployId,
        procInsId: this.procInsId
      }).then(res => {
        console.log("getProcessForm res=",res);
        if (res.data) {
          this.formData = res.data;
          this.formCode = JSON.stringify(res.data);
          this.formOpen = true
        }
      })
    },
    /** 接收子组件传的值 */
    getData(data) {
      if (data) {
        const variables = [];
        data.fields.forEach(item => {
          let variableData = {};
          variableData.label = item.__config__.label
          // 表单值为多个选项时
          if (item.__config__.defaultValue instanceof Array) {
            const array = [];
            item.__config__.defaultValue.forEach(val => {
              array.push(val)
            })
            variableData.val = array;
          } else {
            variableData.val = item.__config__.defaultValue
          }
          variables.push(variableData)
        })
        this.variables = variables;
      }
    },
    /** 查询部门下拉树结构 */
    getTreeSelect() {
      deptTreeSelect().then(response => {
        this.deptOptions = response.data;
      });
    },
    /** 查询用户列表 */
    getList() {
      this.userLoading = true;
      selectUser(this.addDateRange(this.queryParams, this.dateRange)).then(response => {
        this.userList = response.rows;
        this.total = response.total;
        this.toggleSelection(this.userMultipleSelection);
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
      this.queryParams.deptId = data.id;
      this.getList();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.userMultipleSelection = selection
    },
    changeCurrentUser(val) {
      this.currentUserId = val.userId
    },
    toggleSelection(selection) {
      if (selection && selection.length > 0) {
        this.$nextTick(()=> {
          selection.forEach(item => {
            let row = this.userList.find(k => k.userId === item.userId);
            this.$refs.userTable.toggleRowSelection(row);
          })
        })
      } else {
        this.$nextTick(() => {
          this.$refs.userTable.clearSelection();
        });
      }
    },
    onSelectUsers(title, type) {
      this.userData.title = title;
      this.userData.type = type;
      this.getTreeSelect();
      this.getList()
      this.userData.open = true;
    },
    onSelectNextApprovals() {
      this.userMultipleSelection = this.nextApproval;
      this.onSelectUsers('指定接收人', 'approval')
    },
    submitUserData() {
      let type = this.userData.type;
      if (type === 'approval' ) {
        if (!this.userMultipleSelection || this.userMultipleSelection.length <= 0) {
          this.$modal.msgError("请选择用户");
          return false;
        }
        let userIds = this.userMultipleSelection.map(k => k.userName);
        if (type === 'approval') {
          // 设置下一级接收人userName信息
          this.nextApproval = this.userMultipleSelection;
          this.taskForm.nextApproval = userIds instanceof Array ? userIds.join(',') : userIds;
        }
        this.userData.open = false;
        console.log("submitUserData type",type);
        if (type === 'approval') {
          this.taskForm.comment = "发起人自动审批";
          console.log("submitUserData taskForm",this.taskForm);
          complete(this.taskForm).then(response => {
            this.$modal.msgSuccess(response.msg);
            //this.goBack();
          });
        }
      } else {
        if (!this.taskForm.comment) {
          this.$modal.msgError("请输入审批意见");
          return false;
        }
        if (!this.currentUserId) {
          this.$modal.msgError("请选择用户");
          return false;
        }
        this.taskForm.userId = this.currentUserId;
      }

    },
    /** 申请流程表单formdesigner数据提交 nbacheng2023-09-10 */
    submitForm() {
      this.$refs.formBuilder.validate();
      if(this.formVal !='') {
        this.formViewOpen = true;
        this.formOpen = false;
        const variables=JSON.parse(this.formVal);
        const formData = JSON.parse(this.formCode);
        formData.formValue = JSON.parse(this.formVal);

        if (this.definitionId) {
          variables.variables = formData;
          console.log("variables=", variables);
          // 启动流程并将表单数据加入流程变量
          startProcess(this.definitionId, JSON.stringify(variables)).then(res => {
            console.log("startProcess res=", res);
            if(res.code === 200 && res.data &&res.data.hasOwnProperty('nextApproval') && res.data.nextApproval === '${approval}') {
              this.taskForm.variables = res.data.variables;
              this.taskForm.taskId = res.data.taskId;
              this.taskForm.procInsId = res.data.procInsId;
              this.onSelectNextApprovals();
            }
            else {
              console.log("startProcess res.msg",res.msg)
              this.$modal.msgSuccess(res.msg);
              this.$tab.closeOpenPage({
              path: '/task/own'
              })
            }
          })
        }
      }
    },

  }
}
</script>

<style lang="scss" scoped>
.form-conf {
  margin: 15px auto;
  width: 80%;
  padding: 15px;
}
</style>
