<template>
  <div v-if="d">
    <div class="hint blue">费用单对家属透明：每一项都标明<b>费用来源/定价依据、确认人、是否可退</b>；困难家庭减免分
      <b>财务初审 → 馆领导确认</b>两级。审核通过后费用单区分<b>已减免、仍需自费、不可减免</b>；
      家属补选用品时系统提示新增差额，且减免范围不自动扩大。</div>

    <!-- 困难家庭减免 -->
    <div class="panel" style="box-shadow:none">
      <div class="panel-hd"><h3>困难家庭减免（低保 / 特困 / 临时救助）</h3>
        <span class="tag" :class="redTag.cls">{{ redTag.text }}</span></div>
      <div class="panel-bd compact">
        <!-- 申请入口 -->
        <div v-if="canApply" class="btn-row">
          <button class="btn warn" @click="applyModal = true">家属提交困难家庭减免申请</button>
          <span class="muted small">需登记证明材料、社区联系人与申请减免项目，财务初审后由馆领导确认</span>
        </div>

        <!-- 审核进度 -->
        <div v-if="['PENDING','FINANCE_PRE_APPROVED'].includes(o.reductionStatus)" class="hint">
          减免审核进行中：<b>{{ o.reductionStatus === 'PENDING' ? '财务初审中…（基础服务可继续预约）' : '财务已初审通过，待馆领导确认' }}</b>
          期间遗体接运、冷藏、火化等公益基础服务可正常进行；
          <b>高价用品和额外仪式需家属二次签字确认</b>（在「② 治丧方案」中操作）。
        </div>
        <div v-if="o.reductionStatus === 'REJECTED' && latestApp" class="hint red">
          减免申请未通过：{{ latestApp.financeOpinion || latestApp.leaderOpinion || '不符合救助条件' }}
          （可核实材料后重新提交）
          <button class="btn sm warn" @click="applyModal = true">重新提交申请</button>
        </div>

        <!-- 财务初审 -->
        <div v-if="latestApp?.status === 'SUBMITTED' && isFinance" class="mt12">
          <div class="kv" style="grid-template-columns:110px 1fr 110px 1fr">
            <div class="k">救助类型</div><div class="v">{{ ASSISTANCE_TYPE[latestApp.assistanceType] }}</div>
            <div class="k">申请人</div><div class="v">{{ latestApp.applicantName }} {{ latestApp.applicantPhone }}</div>
            <div class="k">证明材料</div><div class="v" style="grid-column:3/5">{{ latestApp.materialsNote }}</div>
            <div class="k">社区联系人</div><div class="v" style="grid-column:3/5">
              {{ latestApp.communityContactName || '-' }} {{ latestApp.communityContactPhone || '' }}</div>
          </div>
          <label class="fld mt8"><span>初审意见</span><input v-model="financeForm.opinion" /></label>
          <div class="btn-row">
            <button class="btn success" @click="financeReview(true)">初审通过，转馆领导确认</button>
            <button class="btn danger" @click="financeReview(false)">驳回</button>
          </div>
        </div>

        <!-- 馆领导终审 -->
        <div v-if="latestApp?.status === 'FINANCE_PRE_APPROVED' && isLeader" class="mt12">
          <div class="hint blue">财务初审意见：{{ latestApp.financeOpinion }}。请选择批准的政府补助项目，
            并勾选纳入减免范围的项目（仅范围留痕，实际减免以补助项目为准）。</div>
          <div class="small muted">批准政府补助（冲减费用）：</div>
          <div v-for="s in subsidies" :key="s.id" class="check-row mb8">
            <input type="checkbox" :value="s.id" v-model="leaderForm.subsidyIds" :id="'ls' + s.id" />
            <label :for="'ls' + s.id"><b>{{ s.name }}</b>（¥{{ fmtMoney(s.unitPrice) }}）
              <span class="muted small">{{ s.sourceNote }}</span></label>
          </div>
          <div class="small muted mt8">家属订单中拟纳入减免范围的项目：</div>
          <div v-for="it in selfPayItems" :key="it.id" class="check-row mb8">
            <input type="checkbox" :value="it.catalogId" v-model="leaderForm.eligibleCatalogIds" :id="'ei' + it.id"
                   :disabled="!it.catalogId" />
            <label :for="'ei' + it.id">{{ it.name }}（¥{{ fmtMoney(it.subtotal) }}，
              {{ classTxt(it.serviceClass) }}）</label>
          </div>
          <label class="fld mt8"><span>减免依据</span>
            <input v-model="leaderForm.basis" placeholder="如：县民政局《殡葬救助实施办法》第八条，低保证有效" /></label>
          <label class="fld"><span>终审意见</span><input v-model="leaderForm.opinion" /></label>
          <div class="btn-row">
            <button class="btn success" @click="leaderReview(true)">终审确认通过</button>
            <button class="btn danger" @click="leaderReview(false)">终审驳回</button>
          </div>
        </div>

        <!-- 申请记录 -->
        <table v-if="reductions.length" class="mt12">
          <thead><tr><th>申请时间</th><th>救助类型</th><th>材料/社区联系人</th><th>财务初审</th><th>馆领导终审</th><th>状态</th></tr></thead>
          <tbody>
            <tr v-for="r in reductions" :key="r.id">
              <td class="small">{{ fmtTime(r.createdAt) }}</td>
              <td class="small">{{ ASSISTANCE_TYPE[r.assistanceType] }}</td>
              <td class="small">{{ r.materialsNote }}
                <div class="muted">{{ r.communityContactName }} {{ r.communityContactPhone }}</div></td>
              <td class="small">{{ r.financeReviewerName || '-' }}
                <div class="muted">{{ r.financeOpinion }}</div></td>
              <td class="small">{{ r.leaderReviewerName || '-' }}
                <div class="muted">{{ r.leaderOpinion }}{{ r.approvedBasis ? '｜' + r.approvedBasis : '' }}</div></td>
              <td><span class="tag" :class="appTag(r.status).cls">{{ appTag(r.status).text }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 差额提示（减免通过后调整用品） -->
    <div v-if="o.reductionStatus === 'APPROVED' && delta" class="hint" :class="Number(delta.deltaAmount) > 0 ? 'red' : 'blue'">
      批准时家属自费基线 <b>¥{{ fmtMoney(delta.approvedSelfPayTotal) }}</b>，
      当前应交 <b>¥{{ fmtMoney(delta.currentPayable) }}</b>，
      调整用品新增自费 <b>¥{{ fmtMoney(delta.deltaAmount) }}</b>。
      <div>{{ delta.message }}</div>
      <div v-if="delta.addedAfterReduction?.length" class="small mt8">
        减免后补选（不自动扩大减免，需重新申请确认）：
        <span v-for="it in delta.addedAfterReduction" :key="it.id" class="tag red" style="margin-right:6px">
          {{ it.name }} ¥{{ fmtMoney(it.subtotal) }}</span>
      </div>
    </div>

    <!-- 费用业务板块汇总 -->
    <div class="panel" style="box-shadow:none">
      <div class="panel-hd"><h3>费用板块汇总（异地接运 / 冷藏 / 火化 / 礼厅 / 用品 / 补助）</h3></div>
      <div class="panel-bd compact">
        <table>
          <thead><tr><th>费用板块</th><th>包含项目</th><th class="num">小计</th></tr></thead>
          <tbody>
            <tr v-for="(g, k) in bill.categoryGroups || {}" :key="k"
                :style="g.items.length ? '' : 'display:none'">
              <td><b :class="k === 'SUBSIDY' ? 'amount-neg' : ''">{{ g.label }}</b></td>
              <td class="small muted">
                <span v-for="it in g.items" :key="it.id" class="tag gray" style="margin:2px 4px 2px 0">
                  {{ it.name }}（{{ it.confirmedByName || '待确认' }}·{{ it.refundable ? '可退' : '不可退' }}）
                </span>
              </td>
              <td class="num" :class="Number(g.amount) < 0 ? 'amount-neg' : ''">¥{{ fmtMoney(g.amount) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 费用明细表：三分类列 -->
    <table>
      <thead><tr><th>项目</th><th>分类</th><th>减免状态</th><th class="num">单价</th>
        <th class="num">数量</th><th class="num">金额</th><th>来源/定价依据</th><th>确认人</th><th>可退</th></tr></thead>
      <tbody>
        <tr v-for="it in bill.items" :key="it.id" :class="{ 'amount-neg': it.subtotal < 0 }">
          <td>{{ it.name }}</td>
          <td><span class="tag" :class="classTag(it.serviceClass).cls">{{ classTag(it.serviceClass).text }}</span></td>
          <td><span class="tag" :class="reducedFlag(it).cls">{{ reducedFlag(it).text }}</span>
            <div v-if="it.reviewGuard && !it.reviewGuardConfirmed" class="tag amber mt8">待二次确认</div></td>
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
      <b>已减免依据：</b><div v-for="(b, i) in bill.reductionBasis" :key="i">· {{ b }}</div>
    </div>

    <!-- 合计与结算 -->
    <div class="panel mt16" style="box-shadow:none">
      <div class="panel-bd">
        <div style="max-width:460px;margin-left:auto">
          <div class="flex-between mb8"><span class="muted">费用合计（公益基本+自选增值）</span>
            <b>¥{{ fmtMoney(bill.totalAmount) }}</b></div>
          <div class="flex-between mb8"><span class="muted">其中：已减免（政府补助）</span>
            <b class="amount-neg">- ¥{{ fmtMoney(bill.reductionAmount) }}</b></div>
          <div class="flex-between mb8"><span class="muted">其中：不可减免/补选自费</span>
            <b>¥{{ fmtMoney(nonReducibleSum) }}</b></div>
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

    <!-- 困难家庭减免申请 -->
    <div v-if="applyModal" class="modal-mask" @click.self="applyModal = false">
      <div class="modal hd-lg">
        <div class="modal-hd"><span>困难家庭减免申请</span>
          <button class="btn ghost sm" @click="applyModal = false">关闭</button></div>
        <div class="modal-bd">
          <div class="form-grid">
            <label class="fld"><span>救助类型 *</span>
              <select v-model="applyForm.assistanceType">
                <option v-for="(v, k) in ASSISTANCE_TYPE" :key="k" :value="k">{{ v }}</option>
              </select>
            </label>
            <label class="fld"><span>申请人姓名 *</span><input v-model="applyForm.applicantName" /></label>
            <label class="fld"><span>申请人电话</span><input v-model="applyForm.applicantPhone" /></label>
            <label class="fld span3"><span>证明材料 *（名称/编号/复印件是否已收）</span>
              <textarea v-model="applyForm.materialsNote"
                        placeholder="如：低保证 证号 DB3401210023（有效期2027年），复印件已收；或县民政局临时救助认定书"></textarea>
            </label>
            <label class="fld"><span>社区/村（居）联系人</span>
              <input v-model="applyForm.communityContactName" placeholder="社区民政专干姓名" /></label>
            <label class="fld"><span>社区联系电话</span>
              <input v-model="applyForm.communityContactPhone" /></label>
            <label class="fld span3"><span>申请纳入减免的项目（可选）</span>
              <div class="small muted">勾选希望予以减免考虑的本单项目，最终以馆领导终审为准</div>
              <div v-for="it in selfPayItems" :key="it.id" class="check-row mb8">
                <input type="checkbox" :value="it.catalogId" v-model="applyForm.requestedCatalogIds"
                       :id="'rq' + it.id" :disabled="!it.catalogId" />
                <label :for="'rq' + it.id">{{ it.name }}（¥{{ fmtMoney(it.subtotal) }}）</label>
              </div>
            </label>
          </div>
        </div>
        <div class="modal-ft">
          <button class="btn secondary" @click="applyModal = false">取消</button>
          <button class="btn warn" @click="submitApply">提交申请（财务初审 → 馆领导确认）</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { api, getUser } from '../../api'
import { SERVICE_CLASS, REDUCTION_STATUS, REDUCTION_APP_STATUS, ASSISTANCE_TYPE,
  PAY_STATUS, tag, fmtTime, fmtMoney } from '../../labels'
import SignModal from '../../components/SignModal.vue'

const props = defineProps({ d: Object, user: Object })
const emit = defineEmits(['changed', 'toast'])
const me = getUser()
const isFamily = me.role === 'FAMILY'
const isFinance = me.role === 'FINANCE'
const isLeader = me.role === 'LEADER'

const o = computed(() => props.d.order)
const bill = computed(() => props.d.bill || {})
const reductions = computed(() => props.d.reductions || [])
const latestApp = computed(() => reductions.value[0] || null)
const subsidies = ref([])
const delta = ref(null)

const applyModal = ref(false)
const billModal = ref(false)
const applyForm = reactive({
  assistanceType: 'SUBSISTENCE', applicantName: '', applicantPhone: '',
  materialsNote: '', communityContactName: '', communityContactPhone: '',
  requestedCatalogIds: []
})
const financeForm = reactive({ opinion: '材料齐全，困难情况属实，拟同意按救助政策办理' })
const leaderForm = reactive({ subsidyIds: [], eligibleCatalogIds: [], basis: '', opinion: '同意按政策给予减免' })
const payAmount = ref(null), paying = ref(false)

const redTag = computed(() => tag(REDUCTION_STATUS, o.value.reductionStatus || 'NONE'))
const payTag = computed(() => tag(PAY_STATUS, o.value.paymentStatus || 'UNPAID'))
const classTag = (k) => tag(SERVICE_CLASS, k)
const classTxt = (k) => ({ PUBLIC_BASIC: '公益基本服务', OPTIONAL: '自选增值服务', SUBSIDY: '政府补助项目' }[k])
const appTag = (s) => tag(REDUCTION_APP_STATUS, s || 'SUBMITTED')
const selfPayItems = computed(() => (bill.value.selfPayItems || []))
const nonReducibleSum = computed(() =>
  (bill.value.nonReducibleItems || []).reduce((s, it) => s + Number(it.subtotal), 0))
const billSigned = computed(() => (props.d.signatures || []).some(s => s.actionType === 'CONFIRM_BILL'))
const canApply = computed(() => isFamily
  && !['SETTLED', 'ARCHIVED'].includes(o.value.status)
  && !['PENDING', 'FINANCE_PRE_APPROVED'].includes(o.value.reductionStatus))
const canConfirmBill = computed(() =>
  ['COMPLETED', 'IN_SERVICE', 'CONFIRMED'].includes(o.value.status)
  && !['PENDING', 'FINANCE_PRE_APPROVED'].includes(o.value.reductionStatus) && !billSigned.value)

const SIGN_ACTIONS = {
  CONFIRM_ITEM: '确认项目', REMOVE_ITEM: '删减项目', CONFIRM_PLAN: '确认方案',
  CONFIRM_GUARDED_ITEM: '高价/额外项目二次确认',
  CONFIRM_BILL: '确认费用', CONFIRM_CHANGE: '确认变更', URN_CLAIM: '骨灰领取', FEEDBACK: '反馈'
}
const signAction = (a) => SIGN_ACTIONS[a] || a

function reducedFlag(it) {
  if (it.serviceClass === 'SUBSIDY') return { text: '已减免', cls: 'green' }
  if (it.addedAfterReduction) return { text: '减免后补选·不可减免', cls: 'red' }
  if (it.reductionEligible) return { text: '减免范围内', cls: 'blue' }
  if (o.value.reductionStatus === 'APPROVED') return { text: '不可减免', cls: 'gray' }
  return { text: '待审核', cls: 'gray' }
}

async function loadRefs() {
  const g = await api.get('/catalog/groups')
  subsidies.value = g.SUBSIDY || []
  if (props.d.order.reductionStatus === 'APPROVED') {
    delta.value = await api.get('/orders/' + props.d.order.id + '/reduction/delta')
  }
}
loadRefs()

async function submitApply() {
  if (!applyForm.applicantName.trim() || !applyForm.materialsNote.trim()) {
    return emit('toast', '请填写申请人和证明材料', 'err')
  }
  try {
    await api.post('/orders/' + o.value.id + '/reduction/apply', { ...applyForm })
    applyModal.value = false
    emit('changed', 'bill')
    emit('toast', '减免申请已提交，等待财务初审')
  } catch (e) { emit('toast', e.message, 'err') }
}
async function financeReview(approved) {
  if (approved && !financeForm.opinion.trim()) return emit('toast', '请填写初审意见', 'err')
  if (!approved && !financeForm.opinion.trim()) return emit('toast', '驳回需填写原因', 'err')
  try {
    await api.post('/orders/' + o.value.id + '/reduction/finance-review',
      { approved, opinion: financeForm.opinion })
    emit('changed', 'bill')
    emit('toast', approved ? '初审通过，已转馆领导确认' : '已驳回')
  } catch (e) { emit('toast', e.message, 'err') }
}
async function leaderReview(approved) {
  if (!approved && !leaderForm.opinion.trim()) return emit('toast', '驳回需填写意见', 'err')
  try {
    await api.post('/orders/' + o.value.id + '/reduction/leader-review', {
      approved, subsidyCatalogIds: leaderForm.subsidyIds,
      eligibleCatalogIds: leaderForm.eligibleCatalogIds,
      basis: leaderForm.basis, opinion: leaderForm.opinion
    })
    emit('changed', 'bill')
    emit('toast', approved ? '终审通过，减免已生效' : '终审驳回')
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
