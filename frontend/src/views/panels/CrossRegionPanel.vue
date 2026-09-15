<template>
  <div v-if="d">
    <div class="hint blue">外地死亡 / 跨县接运围绕同一治丧单处理：先核验<b>死亡证明、接运许可、车辆资质、冷藏条件、馆内接收能力</b>，
      再关联<b>火化排期与礼厅</b>。任一异常（证明缺失、车辆延误、冷藏不足、排期冲突）系统将<b>暂停相关资源锁定</b>、
      通知主要联系人补材料或改期，并派单接运组/业务员/礼厅管理员/火化组协同。到馆后回写交接、入库、复核、排期。</div>

    <!-- 状态总览 -->
    <div v-if="c" class="panel" style="box-shadow:none">
      <div class="panel-hd"><h3>跨区接运状态</h3>
        <span class="tag" :class="crossTag.cls">{{ crossTag.text }}</span></div>
      <div class="panel-bd compact">
        <div class="status-bar mb12">
          <span class="tag" :class="c.certVerified ? 'green' : 'red'">死亡证明{{ c.certVerified ? '✓' : '✗' }}</span>
          <span class="tag" :class="c.permitVerified ? 'green' : 'red'">接运许可{{ c.permitVerified ? '✓' : '✗' }}</span>
          <span class="tag" :class="c.vehicleVerified ? 'green' : 'red'">车辆资质{{ c.vehicleVerified ? '✓' : '✗' }}</span>
          <span class="tag" :class="c.coldConditionVerified ? 'green' : 'red'">冷藏条件{{ c.coldConditionVerified ? '✓' : '✗' }}</span>
          <span class="tag" :class="c.receptionCapacityVerified ? 'green' : 'red'">接收能力{{ c.receptionCapacityVerified ? '✓' : '✗' }}</span>
          <span class="tag" :class="c.scheduleVerified ? 'green' : 'red'">火化/礼厅排期{{ c.scheduleVerified ? '✓' : '✗' }}</span>
        </div>
        <div class="kv" style="grid-template-columns:110px 1fr 110px 1fr">
          <div class="k">死亡地</div><div class="v">{{ c.deathPlace }}</div>
          <div class="k">接运地点</div><div class="v">{{ c.pickupAddress }}</div>
          <div class="k">当地机构</div><div class="v">{{ c.localOrgName || '-' }} {{ c.localContactName }} {{ c.localContactPhone }}</div>
          <div class="k">证明出具机构</div><div class="v">{{ c.certIssuingOrg }}</div>
          <div class="k">车辆/司机</div><div class="v">{{ c.vehicleName || '-' }} / {{ c.driverName || '-' }} {{ c.driverPhone }}</div>
          <div class="k">接运许可</div>
          <div class="v">{{ c.transportPermitNo || '未登记' }}
            <span class="tag" :class="tag(PERMIT_STATUS, c.permitStatus).cls">{{ tag(PERMIT_STATUS, c.permitStatus).text }}</span></div>
          <div class="k">预计到馆</div><div class="v">{{ fmtTime(c.estimatedArrivalAt) }}</div>
          <div class="k">实际到馆</div><div class="v">{{ fmtTime(c.arrivedAt) }}</div>
          <div class="k">防腐冷藏</div><div class="v">{{ c.embalmingRequired ? '需要' : '不需要' }} {{ c.coldConditionNote || '' }}</div>
          <div class="k">宗教习俗</div><div class="v">{{ c.religiousCustom || '-' }}</div>
          <div class="k">随行亲属</div><div class="v" style="grid-column:3/5">{{ c.accompanyingRelatives || '-' }}</div>
        </div>
        <div v-if="c.suspendReason" class="hint red mt12">{{ c.suspendReason }}</div>
      </div>
    </div>

    <!-- 登记 -->
    <div class="panel" style="box-shadow:none" v-if="!c || canEdit">
      <div class="panel-hd"><h3>{{ c ? '修改跨区接运登记' : '登记跨县接运信息' }}</h3></div>
      <div class="panel-bd compact">
        <div class="form-grid">
          <label class="fld span2"><span>死亡地（外地医院/地点）*</span>
            <input v-model="form.deathPlace" placeholder="如：邻县人民医院太平间" /></label>
          <label class="fld"><span>预计到馆时间</span>
            <input type="datetime-local" v-model="form.estimatedArrivalAt" /></label>
          <label class="fld span3"><span>跨县接运地点 *</span>
            <input v-model="form.pickupAddress" /></label>
          <label class="fld"><span>当地对接机构</span>
            <input v-model="form.localOrgName" placeholder="医院/公安/殡仪馆" /></label>
          <label class="fld"><span>当地联系人</span><input v-model="form.localContactName" /></label>
          <label class="fld"><span>当地联系电话</span><input v-model="form.localContactPhone" /></label>
          <label class="fld span2"><span>死亡证明出具机构 *</span>
            <input v-model="form.certIssuingOrg" placeholder="如：邻县人民医院 / 当地派出所" /></label>
          <label class="fld"><span>接运许可编号</span>
            <input v-model="form.transportPermitNo" placeholder="跨县接运许可/准运证号" /></label>
          <label class="fld"><span>接运车辆</span>
            <select v-model="form.vehicleId"><option value="">请选择</option>
              <option v-for="v in vehicles" :key="v.id" :value="v.id" :disabled="!v.available">
                {{ v.name }}{{ v.qualified ? '' : '（资质审验中）' }}{{ v.permitNo ? ' 许可' + v.permitNo : '（无接运许可）' }}
              </option>
            </select></label>
          <label class="fld"><span>司机</span><input v-model="form.driverName" /></label>
          <label class="fld"><span>司机电话</span><input v-model="form.driverPhone" /></label>
          <label class="fld"><span>防腐冷藏需求</span>
            <select v-model="form.embalmingRequired"><option :value="true">需要长途冷藏/防腐</option>
              <option :value="false">不需要</option></select></label>
          <label class="fld span2"><span>冷藏条件说明</span>
            <input v-model="form.coldConditionNote" placeholder="车载冷藏温度/到馆后冷柜要求" /></label>
          <label class="fld span2"><span>宗教/民族习俗</span><input v-model="form.religiousCustom" /></label>
          <label class="fld span3"><span>随行亲属</span>
            <input v-model="form.accompanyingRelatives" placeholder="如：配偶张淑芬、长子李建国随车" /></label>
        </div>
        <button class="btn" :disabled="busy" @click="register">{{ busy ? '保存中…' : '保存登记并发起跨区协同' }}</button>
      </div>
    </div>

    <!-- 六项核验 -->
    <div class="panel" style="box-shadow:none" v-if="c && canOperate">
      <div class="panel-hd"><h3>六项核验 + 关联火化排期/礼厅</h3></div>
      <div class="panel-bd compact">
        <div class="form-grid">
          <label class="fld"><span>死亡证明</span>
            <select v-model="verifyForm.certificateStatus">
              <option value="VERIFIED">已核验（出具机构证明齐全）</option>
              <option value="MISSING">材料缺失（到馆前补）</option>
            </select></label>
          <label class="fld"><span>证明编号</span><input v-model="verifyForm.certificateNo" /></label>
          <label class="fld"><span>接运许可</span><input v-model="verifyForm.transportPermitNo" placeholder="准运证号" /></label>
          <label class="fld"><span>车辆</span>
            <select v-model="verifyForm.vehicleId"><option value="">请选择</option>
              <option v-for="v in vehicles" :key="v.id" :value="v.id" :disabled="!v.available">{{ v.name }}</option>
            </select></label>
          <label class="fld"><span>冷藏位（留空自动分配）</span>
            <select v-model="verifyForm.coldId"><option value="">自动选择空闲冷柜</option>
              <option v-for="x in colds" :key="x.id" :value="x.id">{{ x.name }}</option></select></label>
          <label class="fld"><span>预计到馆</span><input type="datetime-local" v-model="verifyForm.estimatedArrivalAt" /></label>
          <label class="fld"><span>告别时间</span><input type="datetime-local" v-model="verifyForm.farewellTime" /></label>
          <label class="fld"><span>告别厅</span>
            <select v-model="verifyForm.hallId"><option value="">请选择</option>
              <option v-for="h in halls" :key="h.id" :value="h.id" :disabled="!h.available">
                {{ h.name }}（{{ HALL_SPEC[h.hallSpec] }}）</option></select></label>
          <label class="fld"><span>火化时间</span><input type="datetime-local" v-model="verifyForm.cremationTime" /></label>
          <label class="fld"><span>火化设备</span>
            <select v-model="verifyForm.furnaceId"><option value="">请选择</option>
              <option v-for="f in furnaces" :key="f.id" :value="f.id" :disabled="!f.available">{{ f.name }}</option></select></label>
        </div>
        <button class="btn" @click="verify">执行核验（异常将暂停锁定并通知联系人）</button>

        <div v-if="verifyResult" class="mt12">
          <div :class="['hint', verifyResult.pass ? '' : 'red']">
            <b>{{ verifyResult.pass ? '✅ 六项核验全部通过，资源已锁定' : '⚠ 核验未通过，相关资源已暂停锁定' }}</b>
            <span v-if="!verifyResult.pass">：{{ verifyResult.failures.join('、') }}，已通知主要联系人并发起协同</span>
          </div>
          <table>
            <thead><tr><th>核验项</th><th>结果</th><th>说明</th></tr></thead>
            <tbody>
              <tr v-for="(r, i) in verifyResult.results" :key="i">
                <td>{{ verifyLabel(r.type) }}</td>
                <td><span class="tag" :class="r.pass ? 'green' : 'red'">{{ r.pass ? '通过' : '未通过' }}</span></td>
                <td class="small">{{ r.message }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 在途操作 -->
    <div class="panel" style="box-shadow:none" v-if="c && ['PLANNED','IN_TRANSIT','SUSPENDED'].includes(c.status)">
      <div class="panel-hd"><h3>发车 / 延误 / 改期</h3></div>
      <div class="panel-bd compact btn-row">
        <button v-if="canOperate && readyToDepart" class="btn success" @click="depart">确认发车（在途）</button>
        <span v-if="canOperate && !readyToDepart" class="muted small">六项核验全部通过后才能发车</span>
        <div class="check-row" style="flex:1;min-width:280px">
          <input type="datetime-local" v-model="delayForm.estimatedArrivalAt" />
          <input v-model="delayForm.reason" placeholder="延误原因（道路/车辆/手续）" style="flex:1" />
          <button class="btn warn" @click="reportDelay" :disabled="canOperate === false">上报车辆延误（暂停排期锁定并通知改期）</button>
        </div>
      </div>
    </div>

    <!-- 到馆回写 -->
    <div class="panel" style="box-shadow:none" v-if="c && canOperate && c.status !== 'ARRIVED'">
      <div class="panel-hd"><h3>到馆回写</h3></div>
      <div class="panel-bd compact">
        <div class="form-grid">
          <label class="fld"><span>到馆接收人 *</span><input v-model="arriveForm.receiverName" /></label>
          <label class="fld span2"><span>车辆交接情况</span>
            <input v-model="arriveForm.handoverNote" placeholder="车辆、遗体、证明材料现场交接情况" /></label>
          <label class="fld span3"><span>冷藏入库情况</span>
            <input v-model="arriveForm.coldStorageNote" placeholder="冷柜编号、温度、入库时间" /></label>
          <label class="fld"><span>证明复核编号</span><input v-model="arriveForm.certificateNo" /></label>
          <label class="fld span2"><span>证明复核说明</span><input v-model="arriveForm.certReverifyNote" /></label>
          <label class="fld"><span>告别时间（可改期）</span><input type="datetime-local" v-model="arriveForm.farewellTime" /></label>
          <label class="fld"><span>告别厅</span>
            <select v-model="arriveForm.hallId"><option value="">沿用已锁定</option>
              <option v-for="h in halls" :key="h.id" :value="h.id">{{ h.name }}</option></select></label>
          <label class="fld"><span>火化时间（可改期）</span><input type="datetime-local" v-model="arriveForm.cremationTime" /></label>
          <label class="fld"><span>火化设备</span>
            <select v-model="arriveForm.furnaceId"><option value="">沿用已锁定</option>
              <option v-for="f in furnaces" :key="f.id" :value="f.id">{{ f.name }}</option></select></label>
          <label class="fld span2"><span>排期确认说明</span><input v-model="arriveForm.scheduleNote" /></label>
        </div>
        <button class="btn success" @click="arrive">到馆交接：回写入库/证明复核/排期确认</button>
      </div>
    </div>

    <!-- 回写结果 -->
    <div class="panel" style="box-shadow:none" v-if="c?.status === 'ARRIVED'">
      <div class="panel-hd"><h3>到馆回写记录（可追溯）</h3><span class="tag green">已到馆交接</span></div>
      <div class="panel-bd compact">
        <div class="kv" style="grid-template-columns:120px 1fr 120px 1fr">
          <div class="k">到馆时间</div><div class="v">{{ fmtTime(c.arrivedAt) }}</div>
          <div class="k">接收人</div><div class="v">{{ c.receiverName }}</div>
          <div class="k">车辆交接</div><div class="v" style="grid-column:3/5">{{ c.handoverNote }}</div>
          <div class="k">冷藏入库</div><div class="v">{{ fmtTime(c.coldStoredAt) }} {{ c.coldStorageNote }}</div>
          <div class="k">证明复核</div><div class="v">{{ fmtTime(c.certReverifiedAt) }} {{ c.certReverifyNote }}</div>
          <div class="k">排期确认</div><div class="v">{{ fmtTime(c.scheduleConfirmedAt) }} {{ c.scheduleNote }}</div>
        </div>
      </div>
    </div>

    <!-- 通知与锁定 -->
    <div class="panel" style="box-shadow:none">
      <div class="panel-hd"><h3>主要联系人通知 / 资源锁定状态</h3></div>
      <div class="panel-bd compact">
        <table v-if="notifications.length" class="mb12">
          <thead><tr><th>时间</th><th>类型</th><th>通知对象</th><th>内容</th><th>通知人</th></tr></thead>
          <tbody>
            <tr v-for="n in notifications" :key="n.id">
              <td class="small">{{ fmtTime(n.createdAt) }}</td>
              <td><span class="tag amber">{{ notifText(n.type) }}</span></td>
              <td class="small">{{ n.targetName }} {{ n.targetPhone }}</td>
              <td class="small">{{ n.content }}</td>
              <td class="small">{{ n.sentByName }}</td>
            </tr>
          </tbody>
        </table>
        <div v-else class="muted small">暂无通知（核验异常或车辆延误时自动生成）</div>
        <table>
          <thead><tr><th>资源</th><th>名称</th><th>时段</th><th>司机/许可</th><th>锁定状态</th></tr></thead>
          <tbody>
            <tr v-for="b in d.bookings" :key="b.id" :class="{ strike: b.status === 'RELEASED' }">
              <td>{{ RES_TYPE[b.resourceType] }}</td><td>{{ b.resourceName }}</td>
              <td class="small">{{ fmtTime(b.startAt) }} ~ {{ fmtTime(b.endAt) }}</td>
              <td class="small">{{ b.driverName || '-' }} {{ b.permitNo || '' }}</td>
              <td><span class="tag" :class="bookingTag(b.status).cls">{{ bookingTag(b.status).text }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { api, getUser } from '../../api'
import { HALL_SPEC, RES_TYPE, PERMIT_STATUS, CROSS_STATUS, tag, fmtTime } from '../../labels'

const props = defineProps({ d: Object, user: Object })
const emit = defineEmits(['changed', 'toast'])
const me = getUser()
const canOperate = ['CLERK', 'TRANSPORT', 'HALL_ADMIN', 'CREMATORIUM', 'LEADER'].includes(me.role)
const canEdit = ['CLERK', 'TRANSPORT', 'LEADER'].includes(me.role)

const vehicles = ref([]), colds = ref([]), halls = ref([]), furnaces = ref([])
const busy = ref(false)
const verifyResult = ref(null)

const c = computed(() => props.d.crossRegion)
const notifications = computed(() => props.d.notifications || [])
const crossTag = computed(() => tag(CROSS_STATUS, c.value?.status || 'PLANNED'))
const readyToDepart = computed(() => c.value && c.value.certVerified && c.value.permitVerified
  && c.value.vehicleVerified && c.value.coldConditionVerified
  && c.value.receptionCapacityVerified && c.value.scheduleVerified
  && c.value.status !== 'SUSPENDED')

const blankForm = () => ({
  deathPlace: '', pickupAddress: '', localOrgName: '', localContactName: '', localContactPhone: '',
  certIssuingOrg: '', transportPermitNo: '', vehicleId: '', driverName: '', driverPhone: '',
  estimatedArrivalAt: '', embalmingRequired: false, coldConditionNote: '',
  religiousCustom: '', accompanyingRelatives: ''
})
const form = reactive(blankForm())
const verifyForm = reactive({
  certificateStatus: 'VERIFIED', certificateNo: '', transportPermitNo: '',
  vehicleId: '', coldId: '', estimatedArrivalAt: '',
  farewellTime: '', hallId: '', cremationTime: '', furnaceId: ''
})
const delayForm = reactive({ estimatedArrivalAt: '', reason: '' })
const arriveForm = reactive({
  receiverName: '', handoverNote: '', coldStorageNote: '', certificateNo: '',
  certReverifyNote: '', farewellTime: '', hallId: '', cremationTime: '', furnaceId: '', scheduleNote: ''
})

const toInput = (s) => s ? String(s).replace(' ', 'T').substring(0, 16) : ''
function fillFromData() {
  if (!c.value) {
    form.pickupAddress = props.d.order.pickupAddress || ''
    form.religiousCustom = props.d.order.religiousCustom || ''
    form.estimatedArrivalAt = toInput(props.d.order.pickupTime)
    form.embalmingRequired = !!props.d.order.needRefrigeration
    return
  }
  Object.assign(form, {
    deathPlace: c.value.deathPlace || '', pickupAddress: c.value.pickupAddress || '',
    localOrgName: c.value.localOrgName || '', localContactName: c.value.localContactName || '',
    localContactPhone: c.value.localContactPhone || '', certIssuingOrg: c.value.certIssuingOrg || '',
    transportPermitNo: c.value.transportPermitNo || '', vehicleId: c.value.vehicleId || '',
    driverName: c.value.driverName || '', driverPhone: c.value.driverPhone || '',
    estimatedArrivalAt: toInput(c.value.estimatedArrivalAt),
    embalmingRequired: !!c.value.embalmingRequired, coldConditionNote: c.value.coldConditionNote || '',
    religiousCustom: c.value.religiousCustom || '', accompanyingRelatives: c.value.accompanyingRelatives || ''
  })
  verifyForm.transportPermitNo = c.value.transportPermitNo || ''
  verifyForm.vehicleId = c.value.vehicleId || ''
  verifyForm.estimatedArrivalAt = toInput(c.value.estimatedArrivalAt)
  verifyForm.farewellTime = toInput(props.d.order.farewellTime)
}

onMounted(async () => {
  const [v, co, h, f] = await Promise.all([
    api.get('/resources?type=VEHICLE'), api.get('/resources?type=COLD'),
    api.get('/resources?type=HALL'), api.get('/resources?type=FURNACE')
  ])
  vehicles.value = v; colds.value = co; halls.value = h; furnaces.value = f
  fillFromData()
})

async function register() {
  if (!form.deathPlace || !form.pickupAddress || !form.certIssuingOrg) {
    return emit('toast', '请填写死亡地、接运地点、证明出具机构', 'err')
  }
  busy.value = true
  try {
    await api.post(`/orders/${props.d.order.id}/cross-region/register`, {
      ...form, vehicleId: form.vehicleId || null,
      needRefrigeration: form.embalmingRequired
    })
    emit('changed', 'cross')
    emit('toast', '跨区接运已登记，已发起接运组协同')
  } catch (e) { emit('toast', e.message, 'err') } finally { busy.value = false }
}
async function verify() {
  try {
    verifyResult.value = await api.post(`/orders/${props.d.order.id}/cross-region/verify`, {
      ...verifyForm, vehicleId: verifyForm.vehicleId || null, coldId: verifyForm.coldId || null,
      hallId: verifyForm.hallId || null, furnaceId: verifyForm.furnaceId || null
    })
    emit('changed', 'cross')
    emit('toast', verifyResult.value.pass ? '六项核验通过，资源已锁定' : '核验未通过，已暂停锁定并通知联系人',
      verifyResult.value.pass ? 'ok' : 'err')
  } catch (e) { emit('toast', e.message, 'err') }
}
async function depart() {
  try {
    await api.post(`/orders/${props.d.order.id}/cross-region/depart`, {})
    emit('changed', 'cross'); emit('toast', '车辆已发车，状态更新为在途')
  } catch (e) { emit('toast', e.message, 'err') }
}
async function reportDelay() {
  if (!delayForm.estimatedArrivalAt || !delayForm.reason) return emit('toast', '请填写新预计到馆时间和延误原因', 'err')
  try {
    await api.post(`/orders/${props.d.order.id}/cross-region/delay`, { ...delayForm })
    delayForm.estimatedArrivalAt = ''; delayForm.reason = ''
    emit('changed', 'cross'); emit('toast', '已上报延误，排期暂停锁定并通知联系人改期')
  } catch (e) { emit('toast', e.message, 'err') }
}
async function arrive() {
  if (!arriveForm.receiverName) return emit('toast', '请填写到馆接收人', 'err')
  try {
    await api.post(`/orders/${props.d.order.id}/cross-region/arrive`, {
      ...arriveForm, hallId: arriveForm.hallId || null, furnaceId: arriveForm.furnaceId || null
    })
    emit('changed', 'cross'); emit('toast', '到馆交接完成，入库/证明/排期已回写治丧单')
  } catch (e) { emit('toast', e.message, 'err') }
}

const VERIFY_LABELS = {
  CERT: '死亡证明', PERMIT: '接运许可', VEHICLE: '车辆资质', COLD: '冷藏条件/接收能力',
  HALL: '告别厅排期', FURNACE: '火化排期'
}
const verifyLabel = (t) => VERIFY_LABELS[t] || t
const notifText = (t) => ({
  DOC_MISSING: '补材料', RESCHEDULE: '改期', VEHICLE_DELAY: '车辆延误', COLD_SHORTAGE: '冷藏不足'
}[t] || t)
const bookingTag = (s) => ({
  HELD: { text: '预占', cls: 'amber' }, CONFIRMED: { text: '已锁定', cls: 'green' },
  SUSPENDED: { text: '暂停锁定', cls: 'red' }, RELEASED: { text: '已释放', cls: 'gray' }
}[s] || { text: s, cls: 'gray' })
</script>
