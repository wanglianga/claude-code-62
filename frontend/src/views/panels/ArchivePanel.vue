<template>
  <div v-if="d">
    <div class="hint">服务结束后，<b>费用明细、减免依据、火化证明、骨灰领取人、未结项目、家属反馈</b>全部归档；
      后续发生费用争议或证件补办时，可凭下方全过程时间线回到原始服务过程。</div>

    <!-- 服务推进 -->
    <div class="panel" style="box-shadow:none" v-if="!['SETTLED','ARCHIVED'].includes(o.status)">
      <div class="panel-hd"><h3>服务推进</h3>
        <span class="tag blue">{{ statusText(o.status) }}</span></div>
      <div class="panel-bd compact">
        <div class="btn-row">
          <button v-if="o.status === 'CONFIRMED'" class="btn" @click="startService">开始治丧服务（接运/冷藏/告别/火化）</button>
        </div>
        <div v-if="o.status === 'IN_SERVICE'" class="form-grid mt12">
          <label class="fld"><span>火化证明编号</span><input v-model="completeForm.cremationCertNo" placeholder="如：县殡火字2026-0088" /></label>
          <label class="fld"><span>火化时间</span><input type="datetime-local" v-model="completeForm.cremationTime" /></label>
          <label class="fld"><span>火化设备</span><input v-model="completeForm.furnaceName" /></label>
          <label class="fld span3"><span>未结项目（如有）</span>
            <input v-model="completeForm.unresolvedItems" placeholder="如：骨灰盒发票次日补开；花圈余款已现场结清" /></label>
          <div class="span3"><button class="btn success" @click="completeService">服务完成（生成火化证明记录并释放资源）</button></div>
        </div>
        <div v-if="['COMPLETED'].includes(o.status)" class="muted small mt8">
          服务已完成。请在「⑤ 费用与结算」完成家属签字确认费用与收款；如存在未结项目可继续补登。</div>
      </div>
    </div>

    <div class="form-grid">
      <!-- 骨灰领取 -->
      <div class="panel" style="box-shadow:none">
        <div class="panel-hd"><h3>骨灰领取</h3></div>
        <div class="panel-bd compact">
          <template v-if="a && a.urnClaimantName">
            <div class="kv" style="grid-template-columns:90px 1fr">
              <div class="k">领取人</div><div class="v">{{ a.urnClaimantName }}（{{ a.urnClaimantRelation }}）</div>
              <div class="k">电话</div><div class="v">{{ a.urnClaimantPhone || '-' }}</div>
              <div class="k">证件号</div><div class="v">{{ a.urnClaimantIdNo || '-' }}</div>
              <div class="k">领取时间</div><div class="v">{{ fmtTime(a.urnClaimTime) }}</div>
            </div>
          </template>
          <template v-else>
            <label class="fld"><span>领取人姓名 *</span><input v-model="urnForm.urnClaimantName" /></label>
            <label class="fld"><span>与逝者关系</span><input v-model="urnForm.urnClaimantRelation" /></label>
            <label class="fld"><span>电话</span><input v-model="urnForm.urnClaimantPhone" /></label>
            <label class="fld"><span>领取人证件号</span><input v-model="urnForm.urnClaimantIdNo" /></label>
            <button class="btn" @click="claimModal = true">领取人签字确认领取</button>
          </template>
        </div>
      </div>

      <!-- 家属反馈 -->
      <div class="panel" style="box-shadow:none">
        <div class="panel-hd"><h3>家属反馈</h3></div>
        <div class="panel-bd compact">
          <template v-if="a && a.feedbackRating">
            <div style="font-size:20px;color:var(--amber)">{{ '★'.repeat(a.feedbackRating) }}{{ '☆'.repeat(5 - a.feedbackRating) }}</div>
            <div class="mt8">{{ a.feedbackContent || '（无文字评价）' }}</div>
          </template>
          <template v-else>
            <label class="fld"><span>评分</span>
              <select v-model.number="fb.rating"><option :value="5">★★★★★ 非常满意</option>
                <option :value="4">★★★★ 满意</option><option :value="3">★★★ 一般</option>
                <option :value="2">★★ 不满意</option><option :value="1">★ 很不满意</option></select>
            </label>
            <label class="fld"><span>意见建议</span><textarea v-model="fb.content"></textarea></label>
            <button class="btn" @click="submitFeedback">提交反馈</button>
          </template>
        </div>
      </div>
    </div>

    <!-- 档案卡片 -->
    <div class="panel" style="box-shadow:none" v-if="a">
      <div class="panel-hd"><h3>服务档案</h3>
        <span class="tag" :class="o.status === 'ARCHIVED' ? 'gray' : 'amber'">
          {{ o.status === 'ARCHIVED' ? '已归档' : '预归档（结清后正式归档）' }}</span></div>
      <div class="panel-bd compact">
        <div class="kv">
          <div class="k">火化证明编号</div><div class="v">{{ a.cremationCertNo || '待补登' }}</div>
          <div class="k">火化时间/设备</div><div class="v">{{ fmtTime(a.cremationTime) }} / {{ a.furnaceName || '-' }}</div>
          <div class="k">减免依据</div><div class="v">{{ a.reductionBasis || '-' }}</div>
          <div class="k">未结项目</div><div class="v">{{ a.unresolvedItems || '无' }}</div>
          <div class="k">归档人</div><div class="v">{{ a.archivedByName || '-' }} {{ fmtTime(a.archivedAt) }}</div>
          <div class="k">反馈</div><div class="v">{{ a.feedbackRating ? a.feedbackRating + ' 星' : '-' }}</div>
        </div>
        <pre v-if="a.feeSnapshot" class="mt12 small"
             style="white-space:pre-wrap;background:#f8fafc;border:1px solid var(--line);border-radius:8px;padding:10px">{{ a.feeSnapshot }}</pre>
      </div>
    </div>

    <div v-if="o.status === 'SETTLED' && canArchive" class="btn-row mt12">
      <input v-model="archiveUnresolved" placeholder="归档前补充未结项目说明（可选）" style="flex:1;min-width:260px" />
      <button class="btn success" @click="archive">正式归档（费用/证明/领取/反馈入档）</button>
    </div>

    <!-- 全过程时间线 -->
    <div class="panel" style="box-shadow:none">
      <div class="panel-hd"><h3>原始服务过程（争议/补办回溯）</h3>
        <span class="muted small">共 {{ d.timeline.length }} 条操作留痕</span></div>
      <div class="panel-bd">
        <ul class="timeline">
          <li v-for="t in d.timeline" :key="t.id">
            <span class="t-time">{{ fmtTime(t.createdAt) }}</span>
            <span class="t-actor">{{ t.actorName }}（{{ ROLES[t.actorRole] || t.actorRole }}）</span>
            <span class="t-evt">[{{ eventText(t.eventType) }}]</span>
            <div>{{ t.content }}</div>
          </li>
        </ul>
      </div>
    </div>

    <SignModal v-if="claimModal" action-desc="骨灰领取确认：请领取人核对逝者信息与骨灰盒后签字，领取记录将入档。"
               @close="claimModal = false" @submit="urnClaim" />
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { api, getUser } from '../../api'
import { ROLES, ORDER_STATUS, txt, fmtTime } from '../../labels'
import SignModal from '../../components/SignModal.vue'

const props = defineProps({ d: Object, user: Object })
const emit = defineEmits(['changed', 'toast'])
const me = getUser()
const canArchive = ['FINANCE', 'LEADER', 'CLERK'].includes(me.role)

const o = computed(() => props.d.order)
const a = computed(() => props.d.archive)
const statusText = (s) => txt(ORDER_STATUS, s)

const completeForm = reactive({ cremationCertNo: '', cremationTime: '', furnaceName: '', unresolvedItems: '' })
const urnForm = reactive({ urnClaimantName: '', urnClaimantRelation: '', urnClaimantPhone: '', urnClaimantIdNo: '' })
const fb = reactive({ rating: 5, content: '' })
const claimModal = ref(false)
const archiveUnresolved = ref('')

const EVENT = {
  SUBMIT: '提交预约', VERIFY: '资源核验', NEGOTIATE: '沟通', SIGN: '家属签字',
  ITEM_CHANGE: '项目变更', RESOURCE_CHANGE: '资源变更', COLLAB: '协同',
  REDUCTION: '减免', SERVICE: '服务执行', SETTLE: '结算', URN_CLAIM: '骨灰领取',
  FEEDBACK: '反馈', ARCHIVE: '归档', SYSTEM: '系统'
}
const eventText = (e) => EVENT[e] || e

async function startService() {
  try {
    await api.post('/orders/' + o.value.id + '/start-service')
    emit('changed', 'archive'); emit('toast', '服务已开始')
  } catch (e) { emit('toast', e.message, 'err') }
}
async function completeService() {
  try {
    await api.post('/orders/' + o.value.id + '/complete-service', { ...completeForm })
    emit('changed', 'archive'); emit('toast', '服务完成，火化证明已登记')
  } catch (e) { emit('toast', e.message, 'err') }
}
async function urnClaim(payload) {
  try {
    await api.post('/orders/' + o.value.id + '/urn-claim', { ...urnForm, ...payload })
    claimModal.value = false
    emit('changed', 'archive'); emit('toast', '骨灰领取记录已签字入档')
  } catch (e) { emit('toast', e.message, 'err') }
}
async function submitFeedback() {
  try {
    await api.post('/orders/' + o.value.id + '/feedback', { ...fb })
    emit('changed', 'archive'); emit('toast', '感谢您的反馈')
  } catch (e) { emit('toast', e.message, 'err') }
}
async function archive() {
  try {
    await api.post('/orders/' + o.value.id + '/archive', { unresolvedItems: archiveUnresolved.value })
    emit('changed', 'archive'); emit('toast', '档案已正式归档')
  } catch (e) { emit('toast', e.message, 'err') }
}
</script>
