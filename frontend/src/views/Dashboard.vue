<template>
  <div>
    <div class="stat-grid">
      <div class="stat"><div class="v">{{ data.totalOrders ?? '-' }}</div><div class="k">治丧单总数</div></div>
      <div class="stat warn"><div class="v">{{ data.openCollaborationCount ?? '-' }}</div>
        <div class="k">{{ user.role === 'LEADER' ? '全部待办协同' : '本岗位待办协同' }}</div></div>
      <div class="stat"><div class="v">{{ (data.statusGroups?.IN_SERVICE || 0) + (data.statusGroups?.CONFIRMED || 0) }}</div>
        <div class="k">已确认/服务中</div></div>
      <div class="stat"><div class="v">{{ data.archiveCount ?? '-' }}</div><div class="k">已归档档案</div></div>
    </div>

    <div class="panel">
      <div class="panel-hd"><h3>单据状态分布</h3></div>
      <div class="panel-bd">
        <div class="status-bar">
          <div v-for="(s, k) in data.statusGroups" :key="k" class="status-pill">
            {{ orderTxt(k) }} <b>{{ s }}</b>
          </div>
        </div>
      </div>
    </div>

    <div class="panel" v-if="user.role !== 'FAMILY'">
      <div class="panel-hd"><h3>待办协同（同一治丧单跨岗位处理）</h3>
        <span class="muted small">馆领导可见全部</span></div>
      <div class="panel-bd compact">
        <table v-if="tasks.length">
          <thead><tr><th>优先级</th><th>类型</th><th>事项</th><th>治丧单</th><th>责任岗位</th><th>发起人</th><th></th></tr></thead>
          <tbody>
            <tr v-for="t in tasks" :key="t.id">
              <td><span class="tag" :class="t.priority === 1 ? 'red' : 'gray'">
                {{ t.priority === 1 ? '高' : t.priority === 2 ? '中' : '低' }}</span></td>
              <td>{{ collabTxt(t.type) }}</td>
              <td><b>{{ t.title }}</b><div class="muted small">{{ t.description }}</div></td>
              <td><span class="order-link" @click="$emit('open', t.orderId)">#{{ t.orderId }}</span></td>
              <td><span class="tag blue">{{ ROLES[t.assigneeRole] }}</span></td>
              <td class="small">{{ t.createdByName }}</td>
              <td><button class="btn sm" @click="$emit('open', t.orderId)">处理</button></td>
            </tr>
          </tbody>
        </table>
        <div v-else class="empty">暂无待办协同 🎉</div>
      </div>
    </div>

    <div class="panel">
      <div class="panel-hd"><h3>最近治丧单</h3></div>
      <div class="panel-bd compact">
        <table>
          <thead><tr><th>单号</th><th>逝者</th><th>联系人</th><th>状态</th><th>证件</th><th>减免</th><th>创建时间</th><th></th></tr></thead>
          <tbody>
            <tr v-for="o in data.recentOrders || []" :key="o.id">
              <td><span class="order-link" @click="$emit('open', o.id)">{{ o.orderNo }}</span></td>
              <td>{{ o.deceasedName }}</td>
              <td class="small">{{ o.contactName }} {{ o.contactPhone }}</td>
              <td><span class="tag" :class="orderTag(o.status).cls">{{ orderTag(o.status).text }}</span></td>
              <td><span class="tag" :class="certTag(o.certificateStatus).cls">{{ certTag(o.certificateStatus).text }}</span></td>
              <td><span class="tag" :class="redTag(o.reductionStatus).cls">{{ redTxt(o.reductionStatus) }}</span></td>
              <td class="small muted">{{ fmtTime(o.createdAt) }}</td>
              <td><button class="btn sm secondary" @click="$emit('open', o.id)">进入</button></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { api, getUser } from '../api'
import { ROLES, ORDER_STATUS, CERT_STATUS, REDUCTION_STATUS, tag, txt, fmtTime } from '../labels'

const emit = defineEmits(['open'])
const user = getUser()
const data = ref({})
const tasks = ref([])
const orderTag = (k) => tag(ORDER_STATUS, k)
const orderTxt = (k) => txt(ORDER_STATUS, k)
const certTag = (k) => tag(CERT_STATUS, k)
const redTag = (k) => tag(REDUCTION_STATUS, k || 'NONE')
const redTxt = (k) => txt(REDUCTION_STATUS, k || 'NONE')
const collabTxt = (k) => ({
  DOC_MISSING: '材料缺失', FAMILY_DISAGREE: '亲属分歧', REDUCTION_REVIEW: '减免审核',
  HALL_CONFLICT: '礼厅冲突', FURNACE_MAINT: '设备检修', NONLOCAL: '外地逝者',
  CREMATION_CONFLICT: '火化排期冲突', COLD_SHORTAGE: '冷藏位不足',
  VEHICLE_DELAY: '车辆延误', VEHICLE_ISSUE: '车辆资质异常', PERMIT_MISSING: '接运许可待补', GENERAL: '其他'
}[k] || k)

async function load() {
  data.value = await api.get('/dashboard')
  tasks.value = data.value.myCollaborations || []
}
onMounted(load)
defineExpose({ reload: load })
</script>
