<style lang="less">
</style>
<template>
    <span>
      <a-button :type="btnType" @click="applySubmit()" :loading="submitLoading">{{text}}</a-button>
      <a-modal :z-index="100" :title="firstInitiatorTitle" @cancel="firstInitiatorOpen = false" :visible.sync="firstInitiatorOpen"
        :width="'50%'" append-to-body>
         <a-descriptions bordered layout="vertical">
           <a-descriptions-item :span="3">
                 <a-badge status="processing" text="选择提醒" />
            </a-descriptions-item>
            <a-descriptions-item label="重新发起新流程按钮" labelStyle="{ color: '#fff', fontWeight: 'bold', fontSize='18px'}">
              重新发起新流程会删除之前发起的任务,重新开始.
            </a-descriptions-item>
            <a-descriptions-item label="继续发起老流程按钮">
              继续发起流程就在原来流程基础上继续流转.
            </a-descriptions-item>
         </a-descriptions>
        <span slot="footer" class="dialog-footer">
          <el-button type="primary" @click="ReStartByDataId(true)">重新发起新流程</el-button>
          <el-button type="primary" @click="ReStartByDataId(false)">继续发起老流程</el-button>
          <el-button @click="firstInitiatorOpen = false">取 消</el-button>
        </span>
      </a-modal>

      <!--挂载关联多个流程-->
      <a-modal @cancel="flowOpen = false" :title="flowTitle" :visible.sync="flowOpen" width="70%" append-to-body>
        <el-row :gutter="64">
          <el-col :span="20" :xs="64" style="width: 100%">
            <el-table ref="singleTable" :data="processList" border highlight-current-row style="width: 100%">
               <el-table-column type="selection" width="55" align="center" />
               <el-table-column label="主键" align="center" prop="id" v-if="true"/>
               <el-table-column label="业务表单名称" align="center" prop="businessName" />
               <el-table-column label="业务服务名称" align="center" prop="businessService" />
               <el-table-column label="流程名称" align="center" prop="flowName" />
               <el-table-column label="关联流程发布主键" align="center" prop="deployId" />
               <el-table-column label="前端路由地址" align="center" prop="routeName" />
               <el-table-column label="组件注入方法" align="center" prop="component" />
               <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
                 <template slot-scope="scope">
                   <el-button size="mini" type="text" @click="selectProcess(scope.row)">确定</el-button>
                 </template>
               </el-table-column>
              </el-table>
          </el-col>
        </el-row>
      </a-modal>

    </span>
</template>

<script>
  import {
    startByDataId,
    isFirstInitiator,
    deleteActivityAndJoin,
    getProcesss
  } from "@/api/workflow/process";

  export default {
    name: 'ActApplyBtn',
    components: {},
    props: {
      btnType: {
        type: String,
        default: 'link',
        required: false
      },
      /**/
      dataId: {
        type: String,
        default: '',
        required: true
      },
      serviceName: {
        type: String,
        default: '',
        required: true
      },
      variables: {
        type: Object,
        default: {},
      },
      text: {
        type: String,
        default: '提交申请',
        required: false
      }

    },
    data() {
      return {
        modalVisible: false,
        submitLoading: false,
        form: {},
        firstInitiatorOpen: false,
        firstInitiatorTitle: '',
        // 关联流程数据
        processList: [],
        flowOpen: false,
        flowTitle: '',
        selectFlowId: '',  //选择或使用的流程ID
      };
    },
    created() {
    },
    watch: {},
    methods: {
      selectProcess(row) {
        this.selectFlowId = row.id;
        this.flowOpen = false;
        var params = Object.assign({
          dataId: this.dataId
        }, this.variables);
        startByDataId(this.dataId, this.selectFlowId, this.serviceName, params)
          .then(res => {
            //console.log("startByDataId res",res);
            if (res.code == 200 ) {
              this.$message.success(res.msg);
              this.$emit('success');
            } else {
              this.$message.error(res.msg);
            }
          })
          .finally(() => (this.submitLoading = false));
      },
      ReStartByDataId(isNewFlow) {
          if(isNewFlow) {
            this.submitLoading = true;
            deleteActivityAndJoin(this.dataId,this.variables)
            .then(res => {
              if (res.success && res.result) { //若删除成功
                var params = Object.assign({
                  dataId: this.dataId
                }, this.variables);
                startByDataId(this.dataId, this.selectFlowId, this.serviceName, params)
                  .then(res => {
                    if (res.success) {
                      this.firstInitiatorOpen = false;
                      this.$message.success(res.message);
                      this.$emit('success');
                    } else {
                      this.$message.error(res.message);
                    }
                  })
              }
            })
            .finally(() => (this.submitLoading = false));
          }
          else {//继续原有流程流转，跳到流程处理界面上
            //console.log("this.variables",this.variables);
            this.$router.push({ path: '/flowable/task/record/index',
              query: {
                procInsId: this.variables.processInstanceId,
                deployId: this.variables.deployId,
                taskId: this.variables.taskId,
                businessKey: this.dataId,
                nodeType: "",
                category: "zdyyw",
                finished: true
              }})
          }
      },
      applySubmit() {
        if (this.dataId && this.dataId.length < 1) {
          this.error = '必须传入参数dataId';
          this.$message.error(this.error);
          return;
        }
        if (this.serviceName && this.serviceName.length < 1) {
          this.error = '必须传入参数serviceName';
          this.$message.error(this.error);
          return;
        } else {
          this.error = '';
        }
        //对于自定义业务,判断是否是驳回或退回的第一个发起人节点
        this.submitLoading = true;
        isFirstInitiator(this.dataId, this.variables)
          .then(res => {
            if (res.code === 200 && res.data) { //若是，弹出窗口选择重新发起新流程还是继续老流程
              this.firstInitiatorTitle = "根据自己需要进行选择"
              this.firstInitiatorOpen = true;
            }
            else {
              this.submitLoading = true;
              const processParams = {
                 serviceName: this.serviceName
              }
              getProcesss(processParams).then(res => {/**查询关联流程信息 */
                this.processList = res.data;
                  this.submitLoading = false;
                  if (this.processList && this.processList.length > 1) {
                    this.flowOpen = true;
                  }
                  else if (this.processList && this.processList.length === 1) {
                    this.selectFlowId = res.data[0].id;
                    var params = Object.assign({
                      dataId: this.dataId
                    }, this.variables);
                    startByDataId(this.dataId, this.selectFlowId, this.serviceName, params)
                      .then(res => {
                        console.log("startByDataId res",res);
                        if (res.code == 200 ) {
                          this.$message.success(res.msg);
                          this.$emit('success');
                        } else {
                          this.$message.error(res.msg);
                        }
                      })
                      .finally(() => (this.submitLoading = false));
                  } else {
                    this.$message.error("检查该业务是否已经关联流程！");
                  }
              })
              .finally(() => (this.submitLoading = false));
            }
          })
          .finally(() => (this.submitLoading = false));
        }
    }

  };
</script>
