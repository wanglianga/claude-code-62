<template>
  <div v-if="d">
    <div class="hint">同一订单内统一说明 <b>遗体接运、化妆、告别仪式、花圈挽联、寿衣、骨灰盒、餐饮、休息室及减免政策</b>；
      家属<b>每一次确认或删减都保留手写签字</b>。物品确认时自动扣减库存，删减时退回库存。</div>

    <div v-if="liveStage" class="hint red">
      ⚠ 当前已进入/完成服务，现场增减项目必须<b>先在「③ 沟通与现场变更」保存并锁定沟通记录</b>，再凭记录变更：
      <select v-model="selectedLogId" style="width:320px;margin-left:8px">
        <option value="">选择已锁定的沟通记录…</option>
        <option v-for="l in lockedLogs" :key="l.id" :value="l.id">
          #{{ l.id }}［{{ COMM_TYPE[l.type] }}］{{ l.content.slice(0, 24) }}…（{{ fmtTime(l.createdAt) }}）
        </option>
      </select>
    </div>

    <div class="panel" style="box-shadow:none">
      <div class="panel-hd"><h3>服务/物品目录（三类分开）</h3>
        <div class="tabs" style="margin:0;border:0">
          <button v-for="(g, k) in groups" :key="k" :class="{ active: catTab === k }" @click="catTab = k">
            {{ classTxt(k) }}（{{ g.length }}）
          </button>
        </div>
      </div>
      <div class="panel-bd compact">
        <table>
          <thead><tr><th>项目</th><th>类别</th><th class="num">单价</th><th>库存</th><th>来源/定价依据</th><th></th></tr></thead>
          <tbody>
            <tr v-for="c in (groups[catTab] || [])" :key="c.id">
              <td><b>{{ c.name }}</b><div class="muted small">{{ c.description }}</div></td>
              <td class="small">{{ ITEM_CATEGORY[c.category] }}</td>
              <td class="num" :class="{ 'amount-neg': c.unitPrice < 0 }">¥{{ fmtMoney(c.unitPrice) }}/{{ c.unit }}</td>
              <td class="small">{{ c.stock == null ? '充足' : (c.stock > 0 ? c.stock + ' 件' : '缺货') }}</td>
              <td class="small muted">{{ c.sourceNote }}<br /><span :class="c.refundable ? 'tag green' : 'tag red'">
                {{ c.refundable ? '可退' : '不可退' }}</span></td>
              <td>
                <span v-if="catTab === 'SUBSIDY'" class="small muted">凭证明由财务审核加入</span>
                <template v-else>
                  <input type="number" min="1" value="1" style="width:64px" :ref="el => qtyRefs[c.id] = el" />
                  <button class="btn sm" @click="add(c)">加入订单</button>
                </template>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="panel" style="box-shadow:none">
      <div class="panel-hd"><h3>本单治丧项目与家属签字</h3>
        <button class="btn success" :disabled="!canPlan" @click="openPlan">家属签字确认整体方案</button>
      </div>
      <div class="panel-bd compact">
        <table>
          <thead><tr><th>项目</th><th>分类</th><th class="num">单价</th><th class="num">数量</th>
            <th class="num">小计</th><th>来源/依据</th><th>确认人</th><th>可退</th><th>状态</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="it in d.items" :key="it.id" :class="{ strike: it.status === 'REMOVED' }">
              <td>{{ it.name }}<div class="muted small">{{ it.remark }}</div></td>
              <td><span class="tag" :class="classTag(it.serviceClass).cls">{{ classTag(it.serviceClass).text }}</span>
                <div class="small muted">{{ ITEM_CATEGORY[it.category] }}</div></td>
              <td class="num">¥{{ fmtMoney(it.unitPrice) }}</td>
              <td class="num">{{ it.quantity }}</td>
              <td class="num" :class="{ 'amount-neg': it.subtotal < 0 }">¥{{ fmtMoney(it.subtotal) }}</td>
              <td class="small">{{ it.source === 'POLICY' ? '政策减免' : it.source === 'CUSTOM' ? '现场约定' : '服务目录' }}
                <div class="muted">{{ it.sourceNote }}</div></td>
              <td class="small">{{ it.confirmedByName || '-' }}</td>
              <td><span class="tag" :class="it.refundable ? 'green' : 'red'">{{ it.refundable ? '可退' : '不可退' }}</span></td>
              <td><span class="tag" :class="itemTag(it.status).cls">{{ itemTag(it.status).text }}</span></td>
              <td class="btn-row" style="gap:4px">
                <button v-if="it.status === 'PENDING' && isFamily" class="btn sm success" @click="confirm(it)">签字确认</button>
                <button v-if="it.status !== 'REMOVED' && !ended" class="btn sm danger" @click="remove(it)">删减(签)</button>
              </td>
            </tr>
          </tbody>
        </table>
        <div class="hint blue mt12">签字记录共 <b>{{ d.signatures.length }}</b> 份；
          每次确认/删减均可在下方时间线回溯，签字图片长期保存。</div>
      </div>
    </div>

    <SignModal v-if="modal.show" :action-desc="modal.desc" :show-reason="modal.reason"
               @close="modal.show = false" @submit="doSign" />
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { api, getUser } from '../../api'
import { ITEM_CATEGORY, COMM_TYPE, SERVICE_CLASS, ITEM_STATUS, tag, fmtTime, fmtMoney } from '../../labels'
import SignModal from '../../components/SignModal.vue'

const props = defineProps({ d: Object, user: Object })
const emit = defineEmits(['changed', 'toast'])
const me = getUser()
const isFamily = me.role === 'FAMILY'
const catTab = ref('PUBLIC_BASIC')
const groups = ref({ PUBLIC_BASIC: [], OPTIONAL: [], SUBSIDY: [] })
const qtyRefs = reactive({})
const selectedLogId = ref('')

const modal = reactive({ show: false, mode: '', item: null, desc: '', reason: false })

const liveStage = computed(() => ['CONFIRMED', 'IN_SERVICE', 'COMPLETED'].includes(props.d.order.status))
const ended = computed(() => ['SETTLED', 'ARCHIVED'].includes(props.d.order.status))
const canPlan = computed(() => isFamily && props.d.order.verifyStatus === 'PASS'
  && props.d.items.some(i => i.status !== 'REMOVED')
  && !ended.value)
const lockedLogs = computed(() => (props.d.communicationLogs || [])
  .filter(l => l.locked && ['TEMP_CHANGE', 'EMOTIONAL', 'DISPUTE'].includes(l.type)))
const classTag = (k) => tag(SERVICE_CLASS, k)
const classTxt = (k) => ({ PUBLIC_BASIC: '公益基本服务', OPTIONAL: '自选增值服务', SUBSIDY: '政府补助项目' }[k])
const itemTag = (k) => tag(ITEM_STATUS, k)

async function loadGroups() { groups.value = await api.get('/catalog/groups') }
loadGroups()

async function add(c) {
  const qty = Math.max(1, parseInt(qtyRefs[c.id]?.value || '1', 10))
  if (liveStage.value) {
    if (!selectedLogId.value) {
      return emit('toast', '服务已开始：请先到③保存并锁定沟通记录，再选择记录后增项', 'err')
    }
    // 现场临时增项：先有锁定沟通记录，还必须由家属当场手写签字
    Object.assign(modal, {
      show: true, mode: 'addLive', item: { catalog: c, qty }, reason: false,
      desc: '现场临时新增（凭已锁定沟通记录）：' + c.name + ' ×' + qty
        + '，¥' + fmtMoney(Number(c.unitPrice) * qty) + '。请家属手写签字确认，确认后立即扣减库存。'
    })
    return
  }
  try {
    await api.post('/orders/' + props.d.order.id + '/items', { catalogId: c.id, quantity: qty })
    emit('toast', '已加入订单：' + c.name)
    emit('changed', 'items')
    await loadGroups()
  } catch (e) { emit('toast', e.message, 'err') }
}

function confirm(it) {
  Object.assign(modal, {
    show: true, mode: 'confirm', item: it, reason: false,
    desc: '请家属确认项目：' + it.name + ' ×' + it.quantity + '，小计 ¥' + fmtMoney(it.subtotal)
      + '（' + classTxt(it.serviceClass) + '，' + (it.refundable ? '可退' : '不可退') + '）'
  })
}
function remove(it) {
  Object.assign(modal, {
    show: true, mode: 'remove', item: it, reason: true,
    desc: '家属申请删减项目：' + it.name + '。删减记录将永久留痕，已确认物品退回库存。'
  })
}
function openPlan() {
  Object.assign(modal, {
    show: true, mode: 'plan', item: null, reason: false,
    desc: '请家属核对全部治丧项目、来源与可退属性后，签字确认整体治丧方案。'
  })
}

async function doSign(payload) {
  const id = props.d.order.id
  try {
    if (modal.mode === 'confirm') {
      await api.post(`/orders/${id}/items/${modal.item.id}/confirm`,
        { ...payload, communicationLogId: selectedLogId.value || null })
    } else if (modal.mode === 'remove') {
      await api.post(`/orders/${id}/items/${modal.item.id}/remove`,
        { ...payload, communicationLogId: selectedLogId.value || null })
    } else if (modal.mode === 'plan') {
      await api.post(`/orders/${id}/confirm-plan`, payload)
    } else if (modal.mode === 'addLive') {
      await api.post(`/orders/${id}/items`, {
        catalogId: modal.item.catalog.id, quantity: modal.item.qty,
        communicationLogId: Number(selectedLogId.value), ...payload
      })
      await loadGroups()
    }
    modal.show = false
    emit('toast', '签字已保存')
    emit('changed', 'items')
  } catch (e) { emit('toast', e.message, 'err') }
}

watch(() => props.d, () => {
  if (liveStage.value && !selectedLogId.value && lockedLogs.value.length) {
    selectedLogId.value = lockedLogs.value[0].id
  }
})
</script>
