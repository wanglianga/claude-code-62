<template>
  <div>
    <div class="panel">
      <div class="panel-hd"><h3>协同中心</h3>
        <span class="muted small">{{ me.role === 'LEADER' ? '馆领导视角：全部待办' : '本岗位待办（馆领导可见全部）' }}</span></div>
      <div class="panel-bd compact">
        <table v-if="tasks.length">
          <thead><tr><th>优先级</th><th>类型</th><th>事项</th><th>治丧单</th><th>责任岗位</th><th>发起人</th><th>状态</th><th></th></tr></thead>
          <tbody>
            <tr v-for="t in tasks" :key="t.id">
              <td><span class="tag" :class="t.priority === 1 ? 'red' : 'gray'">{{ t.priority === 1 ? '高' : '中' }}</span></td>
              <td>{{ typeText(t.type) }}</td>
              <td><b>{{ t.title }}</b><div class="muted small">{{ t.description }}</div></td>
              <td><span class="order-link" @click="$emit('open', t.orderId)">#{{ t.orderId }}</span></td>
              <td><span class="tag blue">{{ ROLES[t.assigneeRole] }}</span></td>
              <td class="small">{{ t.createdByName }}</td>
              <td><span class="tag amber">{{ COLLAB_STATUS[t.status] }}</span></td>
              <td><button class="btn sm" @click="$emit('open', t.orderId)">进入治丧单处理</button></td>
            </tr>
          </tbody>
        </table>
        <div v-else class="empty">暂无待办协同</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { api, getUser } from '../api'
import { ROLES, COLLAB_STATUS } from '../labels'

defineEmits(['open'])
const me = getUser()
const tasks = ref([])
const typeText = (k) => ({
  DOC_MISSING: '材料缺失', FAMILY_DISAGREE: '亲属分歧', REDUCTION_REVIEW: '减免审核',
  HALL_CONFLICT: '礼厅冲突', FURNACE_MAINT: '设备检修', NONLOCAL: '外地逝者', GENERAL: '其他'
}[k] || k)

onMounted(async () => {
  const d = await api.get('/dashboard')
  tasks.value = d.myCollaborations || []
})
</script>
