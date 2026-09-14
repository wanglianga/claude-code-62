<template>
  <div class="panel">
    <div class="panel-hd"><h3>服务档案（已归档）</h3>
      <span class="muted small">费用争议 / 证件补办可在此检索并回到原始服务过程</span></div>
    <div class="panel-bd compact">
      <table v-if="rows.length">
        <thead><tr><th>单号</th><th>火化证明</th><th>减免依据</th><th>骨灰领取人</th><th>未结项目</th>
          <th>反馈</th><th>归档人/时间</th><th></th></tr></thead>
        <tbody>
          <tr v-for="a in rows" :key="a.id">
            <td><span class="order-link" @click="$emit('open', a.orderId)">{{ a.orderNo }}</span></td>
            <td class="small">{{ a.cremationCertNo || '-' }}<div class="muted">{{ fmtTime(a.cremationTime) }}</div></td>
            <td class="small" style="max-width:220px">{{ a.reductionBasis || '-' }}</td>
            <td class="small">{{ a.urnClaimantName || '-' }}<div class="muted">{{ a.urnClaimantRelation }}</div></td>
            <td class="small" style="max-width:180px">{{ a.unresolvedItems || '无' }}</td>
            <td>{{ a.feedbackRating ? a.feedbackRating + '★' : '-' }}</td>
            <td class="small">{{ a.archivedByName }}<div class="muted">{{ fmtTime(a.archivedAt) }}</div></td>
            <td><button class="btn sm secondary" @click="$emit('open', a.orderId)">回溯全过程</button></td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty">暂无归档档案（完成服务、结算并归档后显示）</div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api'
import { fmtTime } from '../labels'

defineEmits(['open'])
const rows = ref([])
onMounted(async () => { rows.value = await api.get('/archives') })
</script>
