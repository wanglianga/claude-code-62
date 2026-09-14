<template>
  <div class="panel">
    <div class="panel-hd">
      <h3>治丧单列表</h3>
      <div class="btn-row">
        <input type="text" v-model="kw" placeholder="搜索单号/逝者/联系人" style="width:220px" />
        <select v-model="statusFilter" style="width:150px">
          <option value="">全部状态</option>
          <option v-for="(v, k) in ORDER_STATUS" :key="k" :value="k">{{ v[0] }}</option>
        </select>
        <button v-if="canCreate" class="btn" @click="$emit('create')">＋ 新建预约</button>
      </div>
    </div>
    <div class="panel-bd compact">
      <table>
        <thead>
          <tr><th>单号</th><th>逝者</th><th>外地</th><th>联系人</th><th>状态</th><th>核验</th>
            <th>方案签字</th><th>费用/已收</th><th>减免</th><th>创建时间</th><th></th></tr>
        </thead>
        <tbody>
          <tr v-for="o in filtered" :key="o.id">
            <td><span class="order-link" @click="$emit('open', o.id)">{{ o.orderNo }}</span></td>
            <td>{{ o.deceasedName }}<span class="muted small"> {{ o.gender || '' }}{{ o.age ? o.age + '岁' : '' }}</span></td>
            <td><span v-if="o.fromOtherCity" class="tag purple">外地</span><span v-else class="muted">-</span></td>
            <td class="small">{{ o.contactName }}<div class="muted">{{ o.contactPhone }}</div></td>
            <td><span class="tag" :class="tag(ORDER_STATUS, o.status).cls">{{ tag(ORDER_STATUS, o.status).text }}</span></td>
            <td><span class="tag" :class="o.verifyStatus === 'PASS' ? 'green' : o.verifyStatus === 'FAIL' ? 'red' : 'gray'">
              {{ { PASS: '通过', FAIL: '未通过', PENDING: '待核验' }[o.verifyStatus] }}</span></td>
            <td><span class="tag" :class="o.familyConfirmed ? 'green' : 'gray'">{{ o.familyConfirmed ? '已签' : '未签' }}</span></td>
            <td class="num small">{{ fmtMoney(o.payableAmount) }}<div class="muted">已收 {{ fmtMoney(o.paidAmount) }}</div></td>
            <td><span class="tag" :class="tag(REDUCTION_STATUS, o.reductionStatus || 'NONE').cls">
              {{ txt(REDUCTION_STATUS, o.reductionStatus || 'NONE') }}</span></td>
            <td class="small muted">{{ fmtTime(o.createdAt) }}</td>
            <td><button class="btn sm secondary" @click="$emit('open', o.id)">打开</button></td>
          </tr>
        </tbody>
      </table>
      <div v-if="!filtered.length" class="empty">暂无符合条件的治丧单</div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { api, getUser } from '../api'
import { ORDER_STATUS, REDUCTION_STATUS, tag, txt, fmtMoney, fmtTime } from '../labels'

defineEmits(['open', 'create'])
const user = getUser()
const canCreate = ['FAMILY', 'CLERK'].includes(user.role)
const orders = ref([])
const kw = ref('')
const statusFilter = ref('')

const filtered = computed(() => orders.value.filter(o => {
  if (statusFilter.value && o.status !== statusFilter.value) return false
  if (kw.value) {
    const k = kw.value.toLowerCase()
    return [o.orderNo, o.deceasedName, o.contactName, o.contactPhone].some(v => (v || '').toLowerCase().includes(k))
  }
  return true
}))
onMounted(async () => { orders.value = await api.get('/orders') })
</script>
