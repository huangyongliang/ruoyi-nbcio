import Vue from 'vue'

import { listCustomForm } from "@/api/workflow/customForm";

export const flowableMixin = {
  components: {
  },
  data(){
    return {
      customformList: [],
      formQueryParams:{
        pageNum: 1,
        pageSize: 1000,
      },
    }
  },
  created() {
      this.ListCustomForForm();
    },
    computed:{
      /*所有的流程表单，组件化注册，无需维护，从后端获取*/
      allFormComponent:function(){
        return this.customformList;
      }
    },
    methods:{
    /* 挂载自定义业务表单列表 */
    async ListCustomForForm() {
      let that = this;
      listCustomForm(this.formQueryParams).then(res => {
        let  cfList = res.rows;
        cfList.forEach((item, index) => {
          let cms = {
              text:item.flowName,
              routeName:item.routeName,
              component: (resolve) => require([`@/views/${item.routeName}.vue`], resolve),
              businessTable:'wf_demo'
          }
          that.customformList.push(cms);
        })
      })
    },
    getFormComponent(routeName){
      return _.find(this.allFormComponent,{routeName:routeName})||{};
    },

    handleTableChange(pagination, filters, sorter) {
      //分页、排序、筛选变化时触发
      //TODO 筛选
      if (Object.keys(sorter).length > 0) {
        this.isorter.column = sorter.field;
        this.isorter.order = "ascend" == sorter.order ? "asc" : "desc"
      }
      this.ipagination = pagination;
      // this.loadData();
    },
  }

}
