<template>
  <div>
    <div class="panel">
      <div class="panel-bd compact">
        <div class="flex-between">
          <div>
            <button class="btn ghost sm" @click="$emit('back')">← 返回列表</button>
            <span style="font-size:17px;font-weight:700;margin-left:8px">{{ o.orderNo }}</span>
            <span class="tag" :class="orderTag.cls" style="margin-left:10px">{{ orderTag.text }}</span>
            <span class="tag" :class="o.verifyStatus === 'PASS' ? 'green' : o.verifyStatus === 'FAIL' ? 'red' : 'gray'">
              核验{{ { PASS: '通过', FAIL: '未通过', PENDING: '待核' }[o.verifyStatus] }}</span>
            <span class="tag" :class="o.familyConfirmed ? 'green' : 'gray'">方案{{ o.familyConfirmed ? '已签字' : '未签字' }}</span>
          </div>
          <div class="small muted">创建：{{ o.createdByName }} · {{ fmtTime(o.createdAt) }}</div>
        </div>
        <div class="kv mt12">
          <div class="k">逝者</div><div class="v"><b>{{ o.deceasedName }}</b>（{{ o.gender || '-' }}，{{ o.age || '-' }}岁）
            <span v-if="o.fromOtherCity" class="tag purple">外地逝者</span></div>
          <div class="k">死亡时间</div><div class="v">{{ fmtTime(o.deathTime) }}</div>
          <div class="k">身份证号</div><div class="v">{{ o.idCardNo || '-' }}</div>
          <div class="k">死亡原因</div><div class="v">{{ o.deathCause || '-' }}</div>
          <div class="k">死亡证明</div>
          <div class="v"><span class="tag" :class="certTag.cls">{{ certTag.text }}</span>
            <span class="small muted"> {{ o.certificateNo || '' }} {{ o.certificateNote || '' }}</span></div>
          <div class="k">接运地点</div><div class="v">{{ o.pickupAddress || '-' }} <span class="small muted">{{ fmtTime(o.pickupTime) }}</span></div>
          <div class="k">宗教/习俗</div><div class="v">{{ o.religiousCustom || '无特殊要求' }}</div>
          <div class="k">冷藏需求</div><div class="v">{{ o.needRefrigeration ? '需要馆内冷藏' : '不需要' }}</div>
          <div class="k">礼厅规格/时间</div><div class="v">{{ HALL_SPEC[o.hallSpec] || '-' }} / {{ fmtTime(o.farewellTime) }}</div>
          <div class="k">亲属联系人</div><div class="v">{{ o.contactName }}（{{ o.contactRelation || '-' }}） {{ o.contactPhone }}</div>
          <div class="k">对接业务员</div><div class="v">{{ o.clerkName || '待分配' }}</div>
        </div>
      </div>
    </div>

    <div class="panel">
      <div class="panel-bd compact">
        <div class="tabs">
          <button v-for="t in tabs" :key="t.k" :class="{ active: tab === t.k }" @click="tab = t.k">
            {{ t.label }}<span v-if="t.badge" class="tag red" style="margin-left:6px">{{ t.badge }}</span>
          </button>
        </div>

        <VerifyPanel v-if="tab === 'verify'" :d="d" :user="user" @changed="reload" @toast="toast" />
        <CrossRegionPanel v-else-if="tab === 'cross'" :d="d" :user="user" @changed="reload" @toast="toast" />
        <ItemsPanel v-else-if="tab === 'items'" :d="d" :user="user" @changed="reload" @toast="toast" />
        <CommunicationPanel v-else-if="tab === 'comm'" :d="d" :user="user" @changed="reload" @toast="toast" />
        <CollabPanel v-else-if="tab === 'collab'" :d="d" :user="user" @changed="reload" @toast="toast" />
        <BillPanel v-else-if="tab === 'bill'" :d="d" :user="user" @changed="reload" @toast="toast" />
        <ArchivePanel v-else-if="tab === 'archive'" :d="d" :user="user" @changed="reload" @toast="toast" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { api, getUser } from '../api'
import { ORDER_STATUS, CERT_STATUS, HALL_SPEC, tag, fmtTime } from '../labels'
import VerifyPanel from './panels/VerifyPanel.vue'
import CrossRegionPanel from './panels/CrossRegionPanel.vue'
import ItemsPanel from './panels/ItemsPanel.vue'
import CommunicationPanel from './panels/CommunicationPanel.vue'
import CollabPanel from './panels/CollabPanel.vue'
import BillPanel from './panels/BillPanel.vue'
import ArchivePanel from './panels/ArchivePanel.vue'

const props = defineProps({ id: { type: Number, required: true } })
const emit = defineEmits(['back', 'changed', 'toast'])
const user = getUser()
const d = ref(null)
const tab = ref('verify')

const o = computed(() => d.value?.order || {})
const orderTag = computed(() => tag(ORDER_STATUS, o.value.status))
const certTag = computed(() => tag(CERT_STATUS, o.value.certificateStatus || 'PENDING'))

const tabs = computed(() => {
  const openCollab = (d.value?.collaborations || []).filter(c => c.status !== 'RESOLVED').length
  const cross = d.value?.crossRegion
  return [
    { k: 'verify', label: '① 资源核验' },
    { k: 'cross', label: '② 异地/跨县接运',
      badge: cross?.status === 'SUSPENDED' ? '!' : (d.value?.order.fromOtherCity && !cross ? '需登记' : '') },
    { k: 'items', label: '③ 治丧方案与签字' },
    { k: 'comm', label: '④ 沟通与现场变更' },
    { k: 'collab', label: '⑤ 跨岗位协同', badge: openCollab || '' },
    { k: 'bill', label: '⑥ 费用与结算' },
    { k: 'archive', label: '⑦ 完成/领取/归档' }
  ]
})

async function reload(toTab) {
  d.value = await api.get('/orders/' + props.id)
  if (toTab) tab.value = toTab
}
onMounted(() => reload())
watch(() => props.id, () => reload())
function toast(msg, kind) { emit('toast', msg, kind) }
</script>
