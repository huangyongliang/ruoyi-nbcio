<template>
  <div class="app-container">
    <el-tabs tab-position="top" v-model="activeName" :value="processed === true ? 'approval' : 'form'" @tab-click="changeTab">

      <el-tab-pane label="任务办理" name="approval" v-if="processed === true">
        <el-card class="box-card" shadow="hover" v-if="taskFormOpen">
          <div slot="header" class="clearfix">
            <span>填写表单</span>
          </div>
          <el-col :span="20" :offset="2">
            <form-builder ref="taskFormParser" v-model="taskFormVal" :buildData="taskFormCode" />
            <!--<parser :form-conf="taskFormData" ref="taskFormParser"/> -->
          </el-col>
        </el-card>
        <el-card class="box-card" shadow="hover">
          <div slot="header" class="clearfix">
            <span>审批流程</span>
          </div>
          <el-row>
            <el-col :span="20" :offset="2">
              <el-form ref="taskForm" :model="taskForm" :rules="rules" label-width="120px">
                <el-form-item label="审批意见" prop="comment">
                  <el-input type="textarea" :rows="5" v-model="taskForm.comment" placeholder="请输入 审批意见" />
                </el-form-item>
                <el-form-item label="抄送人" prop="copyUserIds">
                  <el-tag
                    :key="index"
                    v-for="(item, index) in copyUser"
                    closable
                    :disable-transitions="false"
                    @close="handleClose('copy', item)">
                    {{ item.nickName }}
                  </el-tag>
                  <el-button class="button-new-tag" type="primary" icon="el-icon-plus" size="mini" circle @click="onSelectCopyUsers" />
                </el-form-item>
                <el-form-item label="指定审批人" prop="copyUserIds">
                  <el-tag
                    :key="index"
                    v-for="(item, index) in nextUser"
                    closable
                    :disable-transitions="false"
                    @close="handleClose('next', item)">
                    {{ item.nickName }}
                  </el-tag>
                  <el-button class="button-new-tag" type="primary" icon="el-icon-plus" size="mini" circle @click="onSelectNextUsers" />
                </el-form-item>
              </el-form>
            </el-col>
          </el-row>
          <el-row :gutter="10" type="flex" justify="center" >
            <el-col :span="1.5">
              <el-button icon="el-icon-circle-check" type="success" @click="handleComplete">通过</el-button>
            </el-col>
            <el-col :span="1.5">
              <el-button icon="el-icon-chat-line-square" type="primary" @click="handleDelegate">委派</el-button>
            </el-col>
            <el-col :span="1.5">
              <el-button icon="el-icon-thumb" type="success" @click="handleTransfer">转办</el-button>
            </el-col>
            <el-col :span="1.5">
              <el-button icon="el-icon-circle-check" type="success" @click="handleAddSign">加签</el-button>
            </el-col>
            <el-col :span="1.5">
              <el-button icon="el-icon-circle-check" type="success" @click="handleJump">跳转</el-button>
            </el-col>
            <el-col :span="1.5">
              <el-button icon="el-icon-refresh-left" type="warning" @click="handleReturn">退回</el-button>
            </el-col>
            <el-col :span="1.5">
              <el-button icon="el-icon-circle-close" type="danger" @click="handleReject">驳回</el-button>
            </el-col>
            <el-col :span="1.5">
              <el-button icon="el-icon-circle-close" type="danger" @click="handleRefuse">拒绝</el-button>
            </el-col>
          </el-row>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="表单信息" name="form">
        <div v-if="customForm.visible"> <!-- 自定义表单 -->
            <component ref="refCustomForm" :disabled="customForm.disabled" v-bind:is="customForm.formComponent" :model="customForm.model"
                        :customFormData="customForm.customFormData" :isNew = "customForm.isNew"></component>
        </div>
        <div v-if="formOpen"> <!-- formdesigner表单 -->
          <el-card class="box-card" shadow="never" v-for="(formInfo, index) in formViewData" :key="index">
            <!--流程处理表单模块-->
            <el-col :span="20" :offset="2">
              <form-builder v-if = "startUserForm.isStartUserNode && startUserForm.editFormType === 'OA' && processed === true && index === 0" ref="refStartBuilder" v-model="formVal[0]" :buildData="formInfo" />
              <form-viewer v-else ref="formViewer" v-model="formVal[index]" :buildData="formInfo" />
            </el-col>
          </el-card>
        </div>
        <div style="margin-left:10%;margin-bottom: 30px">
           <!--对上传文件进行显示处理，临时方案 add by nbacheng 2022-07-27 -->
           <el-upload action="#" :on-preview="handleFilePreview" :file-list="fileList" v-if="fileDisplay" />
        </div>
      </el-tab-pane >

      <el-tab-pane label="流转记录" name="record">
        <el-card class="box-card" shadow="never">
          <el-col :span="20" :offset="2">
            <div class="block">
              <el-timeline>
                <el-timeline-item v-for="(item,index) in historyProcNodeList" :key="index" :icon="setIcon(item.endTime)" :color="setColor(item.endTime)">
                  <p style="font-weight: 700">{{ item.activityName }}</p>
                  <el-card v-if="item.activityType === 'startEvent'" class="box-card" shadow="hover">
                    {{ item.assigneeName }} 在 {{ item.createTime }} 发起流程
                  </el-card>
                  <el-card v-if="item.activityType === 'userTask'" class="box-card" shadow="hover">
                    <el-descriptions :column="5" :labelStyle="{'font-weight': 'bold'}">
                      <el-descriptions-item label="实际办理">{{ item.assigneeName || '-'}}</el-descriptions-item>
                      <el-descriptions-item label="候选办理">{{ item.candidate || '-'}}</el-descriptions-item>
                      <el-descriptions-item label="接收时间">{{ item.createTime || '-'}}</el-descriptions-item>
                      <el-descriptions-item label="办结时间">{{ item.endTime || '-' }}</el-descriptions-item>
                      <el-descriptions-item label="耗时">{{ item.duration || '-'}}</el-descriptions-item>
                    </el-descriptions>
                    <div v-if="item.commentList && item.commentList.length > 0">
                      <div v-for="(comment, index) in item.commentList" :key="index">
                        <el-divider content-position="left">
                          <el-tag :type="approveTypeTag(comment.type)" size="mini">{{ commentType(comment.type) }}</el-tag>
                          <el-tag type="info" effect="plain" size="mini">{{ comment.time }}</el-tag>
                        </el-divider>
                        <span>{{ comment.fullMessage }}</span>
                      </div>
                    </div>
                  </el-card>
                  <el-card v-if="item.activityType === 'endEvent'" class="box-card" shadow="hover">
                    {{ item.createTime }} 结束流程
                  </el-card>
                </el-timeline-item>
              </el-timeline>
            </div>
          </el-col>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="流程跟踪" name="track">
        <el-card class="box-card" shadow="never">
          <process-viewer :key="`designer-${loadIndex}`" :style="'height:' + height" :xml="xmlData"
                          :finishedInfo="finishedInfo" :allCommentList="historyProcNodeList"
          />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!--退回流程-->
    <el-dialog :title="returnTitle" :visible.sync="returnOpen" width="40%" append-to-body>
      <el-form ref="taskForm" :model="taskForm" label-width="80px" >
        <el-form-item label="退回节点" prop="targetKey">
          <el-radio-group v-model="taskForm.targetKey">
            <el-radio-button
              v-for="item in returnTaskList"
              :key="item.id"
              :label="item.id"
            >{{item.name}}</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="returnOpen = false">取 消</el-button>
        <el-button type="primary" @click="submitReturn">确 定</el-button>
      </span>
    </el-dialog>

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
    <!--加签流程-->
    <a-modal :z-index="100" :title="addSignTitle" @cancel="addSignOpen = false" :visible.sync="addSignOpen" :width="'40%'" append-to-body>
      <el-form ref="addSignForm" :model="addSignForm" label-width="160px">
        <el-form-item label="加签类型" prop="addSignType" :rules="[{ required: true, message: '请选择加签类型', trigger: 'blur' }]">
          <el-radio-group v-model="addSignForm.addSignType" @change="changeAddSignType">
              <el-radio :label="0">前加签</el-radio>
              <el-radio :label="1">后加签</el-radio>
              <el-radio :label="2">多实例加签</el-radio>
            </el-radio-group>
        </el-form-item>
        <el-form-item label="用户选择" prop="copyUserIds" :rules="[{ required: true, message: '请选择用户', trigger: 'blur' }]">
          <el-tag
            :key="index"
            v-for="(item, index) in addSignUser"
            closable
            :disable-transitions="false"
            @close="handleClose('next', item)">
            {{ item.nickName }}
          </el-tag>
          <el-button class="button-new-tag" type="primary" icon="el-icon-plus" size="mini" circle @click="onSelectAddSignUsers" />
        </el-form-item>
        <!-- <el-form-item label="处理意见" prop="comment" :rules="[{ required: true, message: '请输入处理意见', trigger: 'blur' }]">
          <el-input type="textarea" v-model="addSignForm.comment" placeholder="请输入处理意见" />
        </el-form-item>
        <el-form-item label="附件"  prop="commentFileDto.fileurl">
          <j-upload v-model="addSignForm.commentFileDto.fileurl"   ></j-upload>
        </el-form-item>-->
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="addSignOpen = false">取 消</el-button>
        <el-button type="primary" @click="addSignComplete(true)">确 定</el-button>
      </span>
    </a-modal>
    <!--跳转流程-->
    <a-modal :z-index="100" :title="jumpTitle" @cancel="jumpOpen = false" :visible.sync="jumpOpen" :width="'40%'" append-to-body>
      <el-form ref="jumpForm" :model="jumpForm" label-width="160px">
        <el-form-item label="跳转节点" prop="jumpType" :rules="[{ required: true, message: '请选择跳转节点', trigger: 'blur' }]">
          <a-table
            size="middle"
            :columns="jumpNodeColumns"
            :loading="jumpNodeLoading"
            :pagination="false"
            :dataSource="jumpNodeData"
            :rowKey="(record) => record.id"
            :rowSelection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange ,type:'radio' }"
          />
        </el-form-item>

        <!-- <el-form-item label="处理意见" prop="comment" :rules="[{ required: true, message: '请输入处理意见', trigger: 'blur' }]">
          <el-input type="textarea" v-model="jumpForm.comment" placeholder="请输入处理意见" />
        </el-form-item>
        <el-form-item label="附件"  prop="commentFileDto.fileurl">
          <j-upload v-model="jumpForm.commentFileDto.fileurl"   ></j-upload>
        </el-form-item> -->
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="jumpOpen = false">取 消</el-button>
        <el-button type="primary" @click="jumpComplete(true)">确 定</el-button>
      </span>
    </a-modal>
  </div>
</template>

<script>
import Vue from 'vue'
import { detailProcess, processIscompleted } from '@/api/workflow/process'
import { getProcessVariables } from '@/api/workflow/task'
import Parser from '@/utils/generator/parser'
import { complete, delegate, transfer, refuseTask, userTaskList, multiInstanceAddSignTask,
         addSignTask, rejectTask, returnList, returnTask, jumpTask, getNextFlowNode } from '@/api/workflow/task'
import { selectUser, deptTreeSelect } from '@/api/system/user'
import ProcessViewer from '@/components/ProcessViewer'
import '@riophae/vue-treeselect/dist/vue-treeselect.css'
import Treeselect from '@riophae/vue-treeselect'
import formViewer from '@/components/formdesigner/components/formViewer'
import formBuilder from '@/components/formdesigner/components/formBuilder'
import { getCustomForm} from '@/api/workflow/customForm'
import {
    flowableMixin
  } from '@/views/workflow/mixins/flowableMixin'

export default {
  name: "WorkDetail",
  mixins: [flowableMixin],
  components: {
    ProcessViewer,
    Parser,
    formBuilder,
    Treeselect,
    formViewer
  },
  props: {},
  computed: {
    commentType() {
      return val => {
        switch (val) {
          case '1': return '通过'
          case '2': return '退回'
          case '3': return '驳回'
          case '4': return '委派'
          case '5': return '转办'
          case '6': return '终止'
          case '7': return '撤回'
          case '8': return '拒绝'
          case '9': return '跳过'
          case '10': return '前加签'
          case '11': return '后加签'
          case '12': return '多实例加签'
          case '13': return '跳转'
          case '14': return '收回'
        }
      }
    },
    approveTypeTag() {
      return val => {
        switch (val) {
          case '1': return 'success'
          case '2': return 'warning'
          case '3': return 'danger'
          case '4': return 'primary'
          case '5': return 'success'
          case '6': return 'danger'
          case '7': return 'info'
        }
      }
    }
  },
  data() {
    return {
      height: document.documentElement.clientHeight - 205 + 'px;',
      // 模型xml数据
      loadIndex: 0,
      xmlData: undefined,
      finishedInfo: {
        finishedSequenceFlowSet: [],
        finishedTaskSet: [],
        unfinishedTaskSet: [],
        rejectedTaskSet: []
      },
      historyProcNodeList: [],
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
      // 遮罩层
      loading: true,
      taskForm:{
        comment:"", // 意见内容
        procInsId: "", // 流程实例编号
        taskId: "" ,// 流程任务编号
        dataId: "",//业务Id
        copyUserIds: "", // 抄送人Id
        vars: "",
        targetKey:""
      },
      rules: {
        comment: [{ required: true, message: '请输入审批意见', trigger: 'blur' }],
      },
      currentUserId: null,
      variables: [], // 流程变量数据
      taskFormOpen: false,
      //taskFormData: {}, // 流程变量数据
      taskFormVal: '', // formdesigner流程数据
      taskFormCode: '', // formdesigner流程变量
      processFormList: [], // 流程变量数据
      formOpen: false, // 是否加载流程变量数据
      customForm: { //自定义业务表单
        formId: '',
        title: '',
        disabled: false,
        visible: false,
        formComponent: null,
        model: {},
        /*流程数据*/
        customFormData: {},
        isNew: false,
        disableSubmit: true
      },
      variables: [], // 流程变量数据
      variablesData: {}, // 流程变量数据
      returnTaskList: [],  // 回退列表数据
      processed: false,
      returnTitle: null,
      returnOpen: false,
      rejectOpen: false,
      rejectTitle: null,
      userData: {
        title: '',
        type: '',
        open: false,
      },
      copyUser: [],
      nextUser: [],
      addSignUser: [],
      nextApproval: [],
      userMultipleSelection: [],
      userDialogTitle: '',
      userOpen: false,
      formVal:[], //formdesigner关联值
      formViewData: [], //显示formdesigner的输入后提交的表单数据
      fileDisplay: false, // formdesigner是否显示上传的文件控件
      fileList: [], //表单设计器上传的文件列表
      activeName:'', //获取当然tabname
      startUserForm : {
        isStartUserNode: false, //第一个用户任务发起节点
        editFormType: 'OA', //第一发起人节点编辑的表单类型
      },
      addSignOpen: false, //前加签
      addSignTitle: null,  //加签标题
      addSignForm: {
        multiple: true,
        comment: "", // 意见内容
        commentFileDto: { //意见里的附件
          type: '',
          fileurl: ''
        },
        procInsId: "", // 流程实例编号
        taskId: "", // 流程任务编号
        vars: "",
        targetKey: "",
        addSignUsers: "", //委托加签人员
        addSignType: 0, //加签类型
      },
      jumpOpen: false, //跳转
      jumpTitle: null,  //跳转标题
      jumpForm: {
        multiple: true,
        comment: "", // 意见内容
        commentFileDto: { //意见里的附件
          type: '',
          fileurl: ''
        },
        procInsId: "", // 流程实例编号
        taskId: "", // 流程任务编号
        vars: "",
        targetKey: "",
        jumpNode: "", //跳转节点
      },
      jumpNodeLoading: false,
      jumpNodeData: [],
      jumpNodeColumns: [
        {
          title: '节点名称',
          dataIndex: 'name'
        }
      ],
      selectedRowKeys: [],
      selectedRows: [],
    };
  },
  created() {
    this.initData();
  },
  mounted() {

  },
  methods: {
    initData() {
      this.taskForm.procInsId = this.$route.params && this.$route.params.procInsId;
      this.taskForm.taskId  = this.$route.query && this.$route.query.taskId;
      this.taskForm.dataId  = this.$route.query && this.$route.query.dataId;
      this.processed = this.$route.query && eval(this.$route.query.processed || false);

      //判断流程是否结束
      processIscompleted({procInsId: this.taskForm.procInsId}).then(res => {
        console.log("processIscompleted res=",res);
        if(res.data) {
         this.processed = false;
        }
        // 获取流程变量
        this.processVariables(this.taskForm.taskId);
        /*// 流程任务重获取变量表单
        this.getProcessDetails(this.taskForm.procInsId, this.taskForm.taskId);
        this.loadIndex = this.taskForm.procInsId;
        if(this.processed) {
          this.activeName = "approval";
        }
        else {
          this.activeName = "form";
          // 回填数据,这里主要是处理文件列表显示,临时解决，以后应该在formdesigner里完成
          this.processFormList.forEach((item, i) => {
            if (item.hasOwnProperty('list')) {
              this.fillFormData(item.list, item)
              // 更新表单
              this.key = +new Date().getTime()
            }
          });
        }*/
      });

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
    changeTab(tab, event) {
      console.log("changeTab tab=",tab);
      if(tab.name === 'form') {
        console.log("changeTab this.processFormList=",this.processFormList);
        if(this.customForm.formId === "") {
          // 回填数据,这里主要是处理文件列表显示,临时解决，以后应该在formdesigner里完成
          this.processFormList.forEach((item, i) => {
            if (item.hasOwnProperty('list')) {
              this.fillFormData(item.list, item)
              // 更新表单
              this.key = +new Date().getTime()
            }
          });
        }
        /*else {
           if(this.processFormList.length == 1 &&
              this.processFormList[0].formValues.hasOwnProperty('routeName')) {
              this.customForm.disabled = true;
              this.customForm.visible = true;
              this.customForm.formComponent = this.getFormComponent(this.processFormList[0].formValues.routeName).component;
              this.customForm.model = this.processFormList[0].formValues.formData;
              this.customForm.customFormData = this.processFormList[0].formValues.formData;
              console.log("detailProcess customForm",this.customForm);
           }
        }*/
      }
    },
    fillFormData(list, formConf) { // for formdesigner
      console.log("fillFormData list=",list);
      console.log("fillFormData formConf=",formConf);
      list.forEach((item, i) => {
        // 特殊处理el-upload，包括 回显图片
        if(formConf.formValues[item.id] != '') {
          const val = formConf.formValues[item.id];
          if (item.ele === 'el-upload') {
            console.log('fillFormData val=',val)
            if(item['list-type'] != 'text') {//图片
              this.fileList = []    //隐藏加的el-upload文件列表
              //item['file-list'] = JSON.parse(val)
              if(val != '') {
                item['file-list'] = JSON.parse(val)
              }
            }
            else {  //列表
              console.log("列表fillFormData val",val)
              this.fileList = JSON.parse(val)
              item['file-list'] = [] //隐藏加的表单设计器的文件列表
            }
            // 回显图片
            this.fileDisplay = true
          }
        }

        if (Array.isArray(item.columns)) {
          this.fillFormData(item.columns, formConf)
        }
      })
    },
    //点击文件列表中已上传文件进行下载
    handleFilePreview(file) {
      location.href=file.url;
    },
    // 节点单击事件
    handleNodeClick(data) {
      this.queryParams.deptId = data.id;
      this.getList();
    },
    setIcon(val) {
      if (val) {
        return "el-icon-check";
      } else {
        return "el-icon-time";
      }
    },
    setColor(val) {
      if (val) {
        return "#2bc418";
      } else {
        return "#b3bdbb";
      }
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.userMultipleSelection = selection
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
    // 关闭标签
    handleClose(type, tag) {
      let userObj = this.userMultipleSelection.find(item => item.userId === tag.id);
      this.userMultipleSelection.splice(this.userMultipleSelection.indexOf(userObj), 1);
      if (type === 'copy') {
        this.copyUser = this.userMultipleSelection;
        // 设置抄送人ID
        if (this.copyUser && this.copyUser.length > 0) {
          const val = this.copyUser.map(item => item.id);
          this.taskForm.copyUserIds = val instanceof Array ? val.join(',') : val;
        } else {
          this.taskForm.copyUserIds = '';
        }
      } else if (type === 'next') {
        this.nextUser = this.userMultipleSelection;
        // 设置抄送人ID
        if (this.nextUser && this.nextUser.length > 0) {
          const val = this.nextUser.map(item => item.id);
          this.taskForm.nextUserIds = val instanceof Array ? val.join(',') : val;
        } else {
          this.taskForm.nextUserIds = '';
        }
      } else if (type === 'addSign') {
        this.addSignUser = this.userMultipleSelection;
        // 设置抄送人ID
        if (this.nextUser && this.nextUser.length > 0) {
          const val = this.nextUser.map(item => item.id);
          this.taskForm.nextUserIds = val instanceof Array ? val.join(',') : val;
        } else {
          this.taskForm.nextUserIds = '';
        }
      }
    },
    /** 流程变量赋值 */
    handleCheckChange(val) {
      if (val instanceof Array) {
        this.taskForm.values = {
          "approval": val.join(',')
        }
      } else {
        this.taskForm.values = {
          "approval": val
        }
      }
    },
    /** 获取流程变量内容 */
    processVariables(taskId) {
      console.log("processVariables taskId",taskId);
      if (taskId) {
        getProcessVariables(taskId).then(res => {
          console.log("getProcessVariables res=",res);
          if(res.code == 200) {
            if(res.data.hasOwnProperty('dataId') && res.data.dataId) {
              this.customForm.formId = res.data.dataId;
              // 流程任务重获取变量表单
              this.getProcessDetails(this.taskForm.procInsId, this.taskForm.taskId, res.data.dataId);
              this.loadIndex = this.taskForm.procInsId;
              if(this.processed) {
                this.activeName = "approval";
              }
              else {
                this.activeName = "form";
              }
            }
            else {
              // 流程任务重获取变量表单
              this.getProcessDetails(this.taskForm.procInsId, this.taskForm.taskId, "");
              this.loadIndex = this.taskForm.procInsId;
              if(this.processed) {
                this.activeName = "approval";
              }
              else {
                this.activeName = "form";
                // 回填数据,这里主要是处理文件列表显示,临时解决，以后应该在formdesigner里完成
                this.processFormList.forEach((item, i) => {
                  if (item.hasOwnProperty('list')) {
                    this.fillFormData(item.list, item)
                    // 更新表单
                    this.key = +new Date().getTime()
                  }
                });
              }
            }
          }
        });
      }
    },
    getProcessDetails(procInsId, taskId, dataId) {
      const params = {procInsId: procInsId, taskId: taskId, dataId: dataId}
      detailProcess(params).then(res => {
        console.log("detailProcess res=",res);
        const data = res.data;
        this.xmlData = data.bpmnXml;
        this.processFormList = data.processFormList;
        if(this.processFormList.length == 1 &&
           this.processFormList[0].formValues.hasOwnProperty('routeName')) {
           this.customForm.disabled = true;
           this.customForm.visible = true;
           this.customForm.formComponent = this.getFormComponent(this.processFormList[0].formValues.routeName).component;
           this.customForm.model = this.processFormList[0].formValues.formData;
           this.customForm.customFormData = this.processFormList[0].formValues.formData;
           if(data.startUserNode) {
             this.customForm.isNew = true;
             this.customForm.disabled = false;
           }
           console.log("detailProcess customForm",this.customForm);
        }
        else {
          this.processFormList.forEach((item, index) => {
            this.formVal[index] = JSON.stringify(item.formValues);
            this.formViewData[index] = JSON.stringify(item);
          });
          this.taskFormOpen = data.existTaskForm;
          if (this.taskFormOpen) {
            this.taskFormCode = JSON.stringify(data.taskFormData);
          }
          if(data.startUserNode) {
            this.startUserForm.isStartUserNode = true;
          }
          this.formOpen = true
        }
        this.historyProcNodeList = data.historyProcNodeList;
        this.finishedInfo = data.flowViewer;
      })
    },
    onSelectCopyUsers() {
      this.userMultipleSelection = this.copyUser;
      this.onSelectUsers('添加抄送人', 'copy')
    },
    onSelectNextUsers() {
      this.userMultipleSelection = this.nextUser;
      this.onSelectUsers('指定审批人', 'next')
    },
    onSelectAddSignUsers() {
      this.userMultipleSelection = this.addSignUser;
      this.onSelectUsers('指定加签人', 'addSign')
    },
    onSelectNextApprovals() {
      this.userMultipleSelection = this.nextApproval;
      this.onSelectUsers('指定接收人', 'approval')
    },
    onSelectUsers(title, type) {
      this.userData.title = title;
      this.userData.type = type;
      this.getTreeSelect();
      this.getList()
      this.userData.open = true;
    },
    /** 通过任务 */
    handleComplete() {
      // 校验表单
      const taskFormRef = this.$refs.taskFormParser;
      const isExistTaskForm = taskFormRef !== undefined;
      // 若无任务表单，则 taskFormPromise 为 true，即不需要校验
      const taskFormPromise = !isExistTaskForm ? true : new Promise((resolve, reject) => {
        taskFormRef.$refs[taskFormRef.formConf.formModel].validate(valid => {
          valid ? resolve() : reject()
        })
      });
      const approvalPromise = new Promise((resolve, reject) => {
        this.$refs['taskForm'].validate(valid => {
          valid ? resolve() : reject()
        })
      });

      //检查下一个审批节点信息
      // 根据当前任务获取流程设计配置的下一步节点  暂时未涉及到考虑网关、表达式和多节点情况
      let taskId = this.taskForm.taskId;
      let nextValues = this.taskForm.values;
      const params = {
        taskId: taskId,
        values: nextValues
      }
      Promise.all([taskFormPromise, approvalPromise]).then(() => {
        if (isExistTaskForm) {
          const taskFormValue = JSON.stringify(taskFormRef.form);
          const variables = JSON.parse(taskFormValue);
          this.taskForm.variables = variables;
        }
        if (this.startUserForm.isStartUserNode && this.startUserForm.editFormType === 'OA' ) {
          const startFormRef = this.$refs.refStartBuilder;
          const taskFormValue = JSON.stringify(startFormRef[0].form);
          const variables = JSON.parse(taskFormValue);
          this.taskForm.variables = variables;
        }
        getNextFlowNode(params).then(res => {
          const data = res.data;
          console.log("getNextFlowNode data=",data)
          if (data && data.type === "assignee" && data.userTask.assignee === "${getNextApprovalHandler.getApproval(execution)}") {
            this.onSelectNextApprovals();
          }
          else {
            console.log("this.taskForm",this.taskForm);
            complete(this.taskForm).then(response => {
              this.$modal.msgSuccess(response.msg);
              this.goBack();
            });
          }
        })
      })
    },
    /** 委派任务 */
    handleDelegate() {
      this.$refs["taskForm"].validate(valid => {
        if (valid) {
          this.userData.type = 'delegate';
          this.userData.title = '委派任务'
          this.userData.open = true;
          this.getTreeSelect();
        }
      })
    },
    /** 转办任务 */
    handleTransfer(){
      this.$refs["taskForm"].validate(valid => {
        if (valid) {
          this.userData.type = 'transfer';
          this.userData.title = '转办任务';
          this.userData.open = true;
          this.getTreeSelect();
        }
      })
    },
    /** 驳回任务 */
    handleReject() {
      this.$refs["taskForm"].validate(valid => {
        if (valid) {
          const _this = this;
          this.$modal.confirm('驳回审批单流程，是否继续？').then(function() {
            return rejectTask(_this.taskForm);
          }).then(res => {
            this.$modal.msgSuccess(res.msg);
            this.goBack();
          });
        }
      });
    },
    /** 拒绝任务 */
    handleRefuse() {
      this.$refs["taskForm"].validate(valid => {
        if (valid) {
          const _this = this;
          this.$modal.confirm('拒绝审批单流程会终止，是否继续？').then(function() {
            return refuseTask(_this.taskForm);
          }).then(res => {
            this.$modal.msgSuccess(res.msg);
            this.goBack();
          });
        }
      });
    },
    /** 加签 */
    handleAddSign() {
      this.$refs["taskForm"].validate(valid => {
        if (valid) {
          this.addSignOpen = true;
          this.addSignTitle = "前加签流程";
        }
      });
    },
    changeAddSignType(val) {
      this.addSignForm.addSignType = val;
      if(this.addSignForm.addSignType === 0) {
        this.addSignTitle = "前加签流程";
      }
      if(this.addSignForm.addSignType === 1) {
        this.addSignTitle = "后加签流程";
      }
      if(this.addSignForm.addSignType === 2) {
        this.addSignTitle = "多实例加签流程";
      }
      console.log("changeAddSignType =",val);
      console.log("this.addSignTitle =",this.addSignTitle);
    },
    /** 加签任务 */
    addSignComplete() {
      this.addSignForm.addSignUsers = this.taskForm.addSignUsers;
      if (!this.addSignForm.addSignUsers ) {
          this.$message.error("请选择用户");
          return;
      }
      // 流程信息
      this.addSignForm.taskId = this.$route.query && this.$route.query.taskId;
      this.addSignForm.procInsId = this.$route.params && this.$route.params.procInsId;

      //对formdesigner后续加签审批的时候需要用到
      this.addSignForm.comment = this.taskForm.comment;
      console.log("this.addSignForm=",this.addSignForm);

      if(this.addSignForm.addSignType === 2) {
        multiInstanceAddSignTask(this.addSignForm).then(response => {
        this.$message.success(response.msg);
        this.addSignOpen = false;
        this.goBack();
        });
      }
      else {
        addSignTask(this.addSignForm).then(response => {
        this.$message.success(response.msg);
        this.addSignOpen = false;
        this.goBack();
        });
      }
    },
    /** 跳转任务 */
    handleJump() {
      this.$refs["taskForm"].validate(valid => {
        if (valid) {
            console.log("handleJump taskForm",this.taskForm);
            this.jumpOpen = true;
            this.jumpTitle = "跳转流程";
            this.jumpNodeLoading = true
            userTaskList({ taskId: this.taskForm.taskId }).then((res) => {
            this.jumpNodeLoading = false
            this.jumpNodeData = res.data
           })
        }
      });
    },
    jumpComplete() {
      if (this.selectedRows.length < 1) {
        this.$message.warning('请选择跳转节点')
        return
      }
      // 流程信息
      this.jumpForm.taskId = this.$route.query && this.$route.query.taskId;
      this.jumpForm.procInsId = this.$route.params && this.$route.params.procInsId;
      //对formdesigner后续加签审批的时候需要用到
      this.jumpForm.comment = this.taskForm.comment;
      //目标选择的节点信息
      this.jumpForm.targetActId = this.selectedRows[0].id;
      this.jumpForm.targetActName = this.selectedRows[0].name;
      console.log("this.jumpForm=",this.jumpForm);
      jumpTask(this.jumpForm).then(res => {
        console.log(" jumpTask",res);
        if (res.code == 200) {
          this.$message.success('跳转成功')
          this.jumpOpen = false;
          this.goBack();
        } else {
          this.$message.error('跳转失败：' + res.message)
        }
      });
    },
    /**
     * 跳转节点列表选择
     */
    onSelectChange (selectedRowKeys, selectedRows) {
      this.selectedRowKeys = selectedRowKeys
      this.selectedRows = selectedRows
    },
    changeCurrentUser(val) {
      this.currentUserId = val.userId
    },
    /** 返回页面 */
    goBack() {
      // 关闭当前标签页并返回上个页面
      this.$tab.closePage(this.$route)
      this.$router.back()
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
    submitUserData() {
      let type = this.userData.type;
      if (type === 'copy' || type === 'next' || type === 'addSign' || type === 'approval' ) {
        if (!this.userMultipleSelection || this.userMultipleSelection.length <= 0) {
          this.$modal.msgError("请选择用户");
          return false;
        }
        let userIds = this.userMultipleSelection.map(k => k.userName);
        if (type === 'copy') {
          // 设置抄送人userName信息
          this.copyUser = this.userMultipleSelection;
          this.taskForm.copyUserIds = userIds instanceof Array ? userIds.join(',') : userIds;
        } else if (type === 'next') {
          // 设置下一级审批人userName信息
          this.nextUser = this.userMultipleSelection;
          this.taskForm.nextUserIds = userIds instanceof Array ? userIds.join(',') : userIds;
        } else if (type === 'addSign') {
          // 设置加签审批人userName信息
          this.addSignUser = this.userMultipleSelection;
          this.taskForm.addSignUsers = userIds instanceof Array ? userIds.join(',') : userIds;
        } else if (type === 'approval') {
          // 设置下一级接收人userName信息
          this.nextApproval = this.userMultipleSelection;
          this.taskForm.nextApproval = userIds instanceof Array ? userIds.join(',') : userIds;
        }
        this.userData.open = false;
        if (type === 'approval') {
          console.log("this.taskForm",this.taskForm);
          complete(this.taskForm).then(response => {
            this.$modal.msgSuccess(response.msg);
            this.goBack();
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
      }

    },
    /** 可退回任务列表 */
    handleReturn() {
      this.$refs['taskForm'].validate(valid => {
        if (valid) {
          this.returnTitle = "退回流程";
          returnList(this.taskForm).then(res => {
            this.returnTaskList = res.data;
            this.taskForm.values = null;
            this.returnOpen = true;
          })
        }
      });

    },
    /** 提交退回任务 */
    submitReturn() {
      this.$refs["taskForm"].validate(valid => {
        if (valid) {
          if (!this.taskForm.targetKey) {
            this.$modal.msgError("请选择退回节点！");
          }
          returnTask(this.taskForm).then(res => {
            this.$modal.msgSuccess(res.msg);
            this.goBack()
          });
        }
      });
    }
  }
};
</script>
<style lang="scss" scoped>
.clearfix:before,
.clearfix:after {
  display: table;
  content: "";
}
.clearfix:after {
  clear: both
}

.box-card {
  width: 100%;
  margin-bottom: 20px;
}

.el-tag + .el-tag {
  margin-left: 10px;
}

.el-row {
  margin-bottom: 20px;
  &:last-child {
    margin-bottom: 0;
  }
}
.el-col {
  border-radius: 4px;
}

.button-new-tag {
  margin-left: 10px;
}
</style>
