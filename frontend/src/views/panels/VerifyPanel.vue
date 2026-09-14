<template>
  <div v-if="d">
    <div class="hint blue">平台统一核验五要素：<b>死亡证件、接运车辆、馆内冷藏位、火化排期、可用礼厅</b>。
      时段冲突或资源检修时自动给出可替代资源，并向责任岗位发起协同。</div>

    <div class="form-grid">
      <label class="fld"><span>死亡证明状态</span>
        <select v-model="form.certificateStatus">
          <option value="PENDING">待核验</option><option value="VERIFIED">证件核验通过</option>
          <option value="MISSING">材料缺失（阻断通过）</option>
        </select>
      </label>
      <label class="fld span2"><span>证明编号 / 备注</span>
        <input v-model="form.certificateNo" placeholder="证明编号" /></label>

      <label class="fld"><span>接运时间</span><input type="datetime-local" v-model="form.pickupTime" /></label>
      <label class="fld"><span>接运车辆</span>
        <select v-model="form.vehicleId"><option value="">请选择</option>
          <option v-for="r in vehicles" :key="r.id" :value="r.id" :disabled="!r.available">
            {{ r.name }}{{ r.available ? '' : '（检修停用）' }}</option>
        </select>
      </label>
      <label class="fld"><span>冷藏位 {{ d.order.needRefrigeration ? '' : '（家属未申请）' }}</span>
        <select v-model="form.coldId" :disabled="!d.order.needRefrigeration">
          <option value="">不安排</option>
          <option v-for="r in colds" :key="r.id" :value="r.id">{{ r.name }}</option>
        </select>
      </label>

      <label class="fld"><span>告别时间</span><input type="datetime-local" v-model="form.farewellTime" /></label>
      <label class="fld"><span>告别厅（预约规格 {{ HALL_SPEC[d.order.hallSpec] || '-' }}）</span>
        <select v-model="form.hallId"><option value="">请选择</option>
          <option v-for="r in halls" :key="r.id" :value="r.id" :disabled="!r.available">
            {{ r.name }}（{{ HALL_SPEC[r.hallSpec] }}）{{ r.available ? '' : '（停用）' }}</option>
        </select>
      </label>
      <label class="fld"><span>火化时间（默认告别后2小时）</span>
        <input type="datetime-local" v-model="form.cremationTime" /></label>
      <label class="fld"><span>火化设备</span>
        <select v-model="form.furnaceId"><option value="">请选择</option>
          <option v-for="r in furnaces" :key="r.id" :value="r.id" :disabled="!r.available">
            {{ r.name }}{{ r.available ? '' : '（检修中' + (r.unavailableUntil ? ' 至 ' + fmtTime(r.unavailableUntil) : '') + '）' }}</option>
        </select>
      </label>
      <label class="fld span3"><span>核验备注</span><input v-model="form.verifyNote" /></label>
    </div>

    <div class="btn-row mt12">
      <button class="btn" :disabled="busy || !canVerify" @click="run">{{ busy ? '核验中…' : '执行五要素核验并预占资源' }}</button>
      <span v-if="!canVerify" class="muted small">仅业务员/礼厅管理员/接运组/馆领导可执行核验，家属可查看结果</span>
    </div>

    <div v-if="result" class="mt16">
      <div :class="['hint', result.pass ? '' : 'red']">
        <b>{{ result.pass ? '✅ 核验通过：' : '⚠ 核验未全部通过：' }}</b>
        {{ result.pass ? '资源已预占，可进入方案沟通与家属签字。' : '未通过项已自动发起跨岗位协同，请在"④ 跨岗位协同"中处理后重新核验。' }}
      </div>
      <table>
        <thead><tr><th>核验项</th><th>结果</th><th>说明</th><th>可替代资源</th></tr></thead>
        <tbody>
          <tr v-for="(r, i) in result.results" :key="i">
            <td>{{ RES_TYPE[r.type] || r.type }}<span v-if="r.skipped" class="tag gray">跳过</span></td>
            <td><span class="tag" :class="r.pass ? 'green' : 'red'">{{ r.pass ? '通过' : '未通过' }}</span>
              <span v-if="r.type === 'HALL_SPEC_WARN'" class="tag amber">规格不符</span></td>
            <td class="small">{{ r.message }}</td>
            <td>
              <div v-if="r.alternatives?.length" class="small">
                <a v-for="a in r.alternatives" :key="a.id" @click="pickAlt(r.type, a)">
                  {{ a.name }}{{ a.hallSpec ? '（' + HALL_SPEC[a.hallSpec] + '）' : '' }}
                </a><span class="muted">（点击切换）</span>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="d.bookings?.length" class="mt16">
      <h4 style="margin:0 0 8px;color:var(--brand)">当前已预占资源</h4>
      <table>
        <thead><tr><th>类型</th><th>资源</th><th>开始</th><th>结束</th><th>状态</th></tr></thead>
        <tbody>
          <tr v-for="b in d.bookings" :key="b.id" :class="{ strike: b.status === 'RELEASED' }">
            <td>{{ RES_TYPE[b.resourceType] }}</td><td>{{ b.resourceName }}</td>
            <td>{{ fmtTime(b.startAt) }}</td><td>{{ fmtTime(b.endAt) }}</td>
            <td><span class="tag" :class="b.status === 'RELEASED' ? 'gray' : 'green'">
              {{ { HELD: '预占', CONFIRMED: '已确认占用', RELEASED: '已释放' }[b.status] }}</span></td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { api, getUser } from '../../api'
import { HALL_SPEC, RES_TYPE, fmtTime } from '../../labels'

const props = defineProps({ d: Object, user: Object })
const emit = defineEmits(['changed', 'toast'])
const canVerify = ['CLERK', 'HALL_ADMIN', 'TRANSPORT', 'LEADER'].includes(getUser().role)
const vehicles = ref([]), colds = ref([]), halls = ref([]), furnaces = ref([])
const result = ref(null), busy = ref(false)

const toInput = (s) => s ? String(s).replace(' ', 'T').substring(0, 16) : ''
const form = reactive({
  certificateStatus: 'PENDING', certificateNo: '',
  pickupTime: '', farewellTime: '', cremationTime: '',
  vehicleId: '', coldId: '', hallId: '', furnaceId: '', verifyNote: ''
})

function fillFromOrder() {
  const o = props.d.order
  form.certificateStatus = o.certificateStatus || 'PENDING'
  form.certificateNo = o.certificateNo || ''
  form.pickupTime = toInput(o.pickupTime)
  form.farewellTime = toInput(o.farewellTime)
  const booked = {}
  ;(props.d.bookings || []).filter(b => b.status !== 'RELEASED').forEach(b => {
    const map = { VEHICLE: 'vehicleId', COLD: 'coldId', HALL: 'hallId', FURNACE: 'furnaceId' }
    form[map[b.resourceType]] = b.resourceId
    if (b.resourceType === 'VEHICLE') form.pickupTime = toInput(b.startAt)
    if (b.resourceType === 'HALL') form.farewellTime = toInput(b.startAt)
    if (b.resourceType === 'FURNACE' && !form.cremationTime) form.cremationTime = toInput(b.startAt)
    booked[b.resourceType] = b
  })
}

async function loadResources() {
  const [v, c, h, f] = await Promise.all([
    api.get('/resources?type=VEHICLE'), api.get('/resources?type=COLD'),
    api.get('/resources?type=HALL'), api.get('/resources?type=FURNACE')
  ])
  vehicles.value = v; colds.value = c; halls.value = h; furnaces.value = f
  fillFromOrder()
}
onMounted(loadResources)

function pickAlt(type, a) {
  const map = { VEHICLE: 'vehicleId', COLD: 'coldId', HALL: 'hallId', FURNACE: 'furnaceId' }
  form[map[type]] = a.id
  emit('toast', '已切换为 ' + a.name)
}

async function run() {
  busy.value = true
  try {
    result.value = await api.post('/orders/' + props.d.order.id + '/verify', {
      certificateStatus: form.certificateStatus,
      certificateNo: form.certificateNo,
      pickupTime: form.pickupTime || undefined,
      farewellTime: form.farewellTime || undefined,
      cremationTime: form.cremationTime || undefined,
      vehicleId: form.vehicleId || null,
      coldId: form.coldId || null,
      hallId: form.hallId || null,
      furnaceId: form.furnaceId || null,
      verifyNote: form.verifyNote
    })
    emit('changed')
    emit('toast', result.value.pass ? '核验通过，资源已预占' : '核验未通过，已发起协同')
  } catch (e) {
    emit('toast', e.message, 'err')
  } finally {
    busy.value = false
  }
}
</script>
