<template>
  <div v-if="d">
    <div class="hint blue">费用单对家属透明：每一项都标明<b>费用来源/定价依据、确认人、是否可退</b>；
      政府补助项目单列减免依据。家属签字确认费用明细后财务方可收款。</div>

    <!-- 减免申请/审核 -->
    <div class="panel" style="box-shadow:none">
      <div class="panel-hd"><h3>低保/政府救助减免</h3>
        <span class="tag" :class="redTag.cls">{{ redTag.text }}</span></div>
      <div class="panel-bd compact">
        <div v-if="o.reductionStatus === 'NONE' || o.reductionStatus === 'REJECTED'" class="btn-row">
          <input v-model="redReason" placeholder="减免申请说明（如：持低保证，证号 XXX）" style="flex:1;min-width:260px" />
          <button v-if="isFamily || isClerk" class="btn warn" @click="requestReduction">家属提交减免申请（财务审核）</button>
        </div>
        <div v-else-if="o.reductionStatus === 'PENDING'" class="hint">减免申请审核中，请财务/馆领导在下方选择适用的政府补助项目并审批。</div>
        <div v-if="o.reductionStatus === 'PENDING' && canReview" class="mt12">
          <div v-for="s in subsidies" :key="s.id" class="check-row mb8">
            <input type="checkbox" :value="s.id" v-model="selectedSubsidies" :id="'sub' + s.id" />
            <label :for="'sub' + s.id"><b>{{ s.name }}</b>（¥{{ fmtMoney(s.unitPrice) }}）
              <span class="muted small">依据：{{ s.sourceNote }}</span></label>
          </div>
          <input v-model="redBasis" placeholder="审批依据/意见" style="margin-bottom:8px" />
          <div class="btn-row">
            <button class="btn success" @click="review(true)">审核通过并加入补助项目</button>
            <button class="btn danger" @click="review(false)">驳回</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 费用明细表 -->
    <table>
      <thead><tr><th>项目</th><th>分类</th><th class="num">单价</th><th class="num">数量</th>
        <th class="num">金额</th><th>来源/定价依据</th><th>确认人</th><th>可退</th></tr></thead>
      <tbody>
        <tr v-for="it in bill.items" :key="it.id">
          <td>{{ it.name }}</td>
          <td><span class="tag" :class="classTag(it.serviceClass).cls">{{ classTag(it.serviceClass).text }}</span></td>
          <td class="num">{{ fmtMoney(it.unitPrice) }}</td>
          <td class="num">{{ it.quantity }}</td>
          <td class="num" :class="{ 'amount-neg': it.subtotal < 0 }">{{ fmtMoney(it.subtotal) }}</td>
          <td class="small muted">{{ it.sourceNote || '服务目录公示价' }}</td>
          <td class="small">{{ it.confirmedByName || '-' }}</td>
          <td><span class="tag" :class="it.refundable ? 'green' : 'red'">{{ it.refundable ? '可退' : '不可退' }}</span></td>
        </tr>
      </tbody>
    </table>
    <div v-if="bill.removedItems?.length" class="muted small mt8">
      已删减（留痕）：<span v-for="it in bill.removedItems" :key="it.id" class="strike">{{ it.name }}；</span>
    </div>

    <div v-if="bill.reductionBasis?.length" class="hint mt12">
      <b>减免依据：</b><div v-for="(b, i) in bill.reductionBasis" :key="i">· {{ b }}</div>
    </div>

    <!-- 合计与结算 -->
    <div class="panel mt16" style="box-shadow:none">
      <div class="panel-bd">
        <div style="max-width:420px;margin-left:auto">
          <div class="flex-between mb8"><span class="muted">费用合计（公益基本+自选增值）</span>
            <b>¥{{ fmtMoney(bill.totalAmount) }}</b></div>
          <div class="flex-between mb8"><span class="muted">政府补助减免</span>
            <b class="amount-neg">- ¥{{ fmtMoney(bill.reductionAmount) }}</b></div>
          <div class="flex-between mb8" style="font-size:16px"><span>应交合计</span>
            <span class="pill-amount" style="font-size:18px">¥{{ fmtMoney(bill.payableAmount) }}</span></div>
          <div class="flex-between mb12"><span class="muted">已收 / 收款状态</span>
            <span>¥{{ fmtMoney(bill.paidAmount) }} ·
              <span class="tag" :class="payTag.cls">{{ payTag.text }}</span></span></div>

          <div class="btn-row" style="justify-content:flex-end">
            <button v-if="isFamily && canConfirmBill" class="btn" @click="billModal = true">家属签字确认费用明细</button>
            <template v-if="isFinance && ['COMPLETED','IN_SERVICE','CONFIRMED','SETTLED'].includes(o.status)">
              <input type="number" v-model.number="payAmount" style="width:130px" min="0.01" step="0.01" />
              <button class="btn success" :disabled="paying" @click="pay">
                {{ o.paymentStatus === 'PARTIAL' ? '补收尾款' : '财务收款' }}
              </button>
            </template>
          </div>
          <div v-if="isFinance && !billSigned" class="muted small mt8" style="text-align:right">
            家属尚未签字确认费用明细，系统将阻止收款</div>
        </div>
      </div>
    </div>

    <!-- 签字记录 -->
    <div class="panel" style="box-shadow:none">
      <div class="panel-hd"><h3>本单签字记录（{{ d.signatures.length }}）</h3></div>
      <div class="panel-bd compact">
        <table v-if="d.signatures.length">
          <thead><tr><th>时间</th><th>签字类型</th><th>内容</th><th>签字家属</th><th>在场经办</th><th>签名</th></tr></thead>
          <tbody>
            <tr v-for="s in d.signatures" :key="s.id">
              <td class="small">{{ fmtTime(s.signedAt) }}</td>
              <td><span class="tag blue">{{ signAction(s.actionType) }}</span></td>
              <td class="small">{{ s.contentSummary }}</td>
              <td class="small">{{ s.signerName }}（{{ s.signerRelation || '-' }}）</td>
              <td class="small">{{ s.witnessName }}</td>
              <td><img :src="s.signatureData" class="sig-thumb" alt="签名" /></td>
            </tr>
          </tbody>
        </table>
        <div v-else class="empty">暂无签字记录</div>
      </div>
    </div>

    <SignModal v-if="billModal" action-desc="请家属逐项核对费用来源、确认人、可退属性与减免依据后，签字确认费用明细。"
               @close="billModal = false" @submit="doConfirmBill" />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { api, getUser } from '../../api'
import { SERVICE_CLASS, REDUCTION_STATUS, PAY_STATUS, tag, fmtTime, fmtMoney } from '../../labels'
import SignModal from '../../components/SignModal.vue'

const props = defineProps({ d: Object, user: Object })
const emit = defineEmits(['changed', 'toast'])
const me = getUser()
const isFamily = me.role === 'FAMILY'
const isClerk = me.role === 'CLERK'
const isFinance = me.role === 'FINANCE' || me.role === 'LEADER'
const canReview = me.role === 'FINANCE' || me.role === 'LEADER'

const o = computed(() => props.d.order)
const bill = computed(() => props.d.bill || {})
const subsidies = ref([])
const selectedSubsidies = ref([])
const redReason = ref(''), redBasis = ref('')
const payAmount = ref(null), paying = ref(false)
const billModal = ref(false)

const redTag = computed(() => tag(REDUCTION_STATUS, o.value.reductionStatus || 'NONE'))
const payTag = computed(() => tag(PAY_STATUS, o.value.paymentStatus || 'UNPAID'))
const classTag = (k) => tag(SERVICE_CLASS, k)
const billSigned = computed(() => (props.d.signatures || []).some(s => s.actionType === 'CONFIRM_BILL'))
const canConfirmBill = computed(() =>
  ['COMPLETED', 'IN_SERVICE', 'CONFIRMED'].includes(o.value.status)
  && o.value.reductionStatus !== 'PENDING' && !billSigned.value)

const SIGN_ACTIONS = {
  CONFIRM_ITEM: '确认项目', REMOVE_ITEM: '删减项目', CONFIRM_PLAN: '确认方案',
  CONFIRM_BILL: '确认费用', CONFIRM_CHANGE: '确认变更', URN_CLAIM: '骨灰领取', FEEDBACK: '反馈'
}
const signAction = (a) => SIGN_ACTIONS[a] || a

async function loadSubsidies() {
  const g = await api.get('/catalog/groups')
  subsidies.value = g.SUBSIDY || []
}
loadSubsidies()

async function requestReduction() {
  try {
    await api.post('/orders/' + o.value.id + '/reduction/request', { reason: redReason.value })
    emit('changed', 'bill')
    emit('toast', '减免申请已提交，等待财务审核')
  } catch (e) { emit('toast', e.message, 'err') }
}
async function review(approved) {
  try {
    await api.post('/orders/' + o.value.id + '/reduction/review', {
      approved, basis: redBasis.value, subsidyCatalogIds: selectedSubsidies.value
    })
    emit('changed', 'bill')
    emit('toast', approved ? '减免已批准，补助项目已加入费用单' : '已驳回减免申请')
  } catch (e) { emit('toast', e.message, 'err') }
}
async function doConfirmBill(payload) {
  try {
    await api.post('/orders/' + o.value.id + '/confirm-bill', payload)
    billModal.value = false
    emit('changed', 'bill')
    emit('toast', '费用明细已由家属签字确认')
  } catch (e) { emit('toast', e.message, 'err') }
}
async function pay() {
  if (!payAmount.value || payAmount.value <= 0) return emit('toast', '请输入收款金额', 'err')
  paying.value = true
  try {
    await api.post('/orders/' + o.value.id + '/pay', { amount: payAmount.value })
    payAmount.value = null
    emit('changed', 'bill')
    emit('toast', '收款成功')
  } catch (e) { emit('toast', e.message, 'err') } finally { paying.value = false }
}
</script>
