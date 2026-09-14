<template>
  <div v-if="d">
    <div class="hint red"><b>铁律：先保存沟通记录，再变更礼厅、车辆和物品库存。</b>
      遇到亲属情绪激动、亲属意见不一致或现场临时增减仪式，先在此登记（临时变更/分歧/情绪类记录保存即锁定不可改），
      然后凭锁定记录执行资源变更，避免结算时的口头承诺争议。</div>

    <div class="form-grid">
      <label class="fld"><span>沟通类型</span>
        <select v-model="form.type">
          <option v-for="(v, k) in COMM_TYPE" :key="k" :value="k">{{ v }}</option>
        </select>
      </label>
      <label class="fld span2"><span>在场人员</span>
        <input v-model="form.participants" placeholder="如：配偶、长子、业务员陈晓、礼仪师" /></label>
      <label class="fld span3"><span>沟通内容 *（口头承诺也必须写清楚）</span>
        <textarea v-model="form.content" rows="3"
                  placeholder="如：家属临时提出告别厅由怀远厅换思亲厅；长子主张简办、配偶要求保留鲜花布置，经协调…"></textarea>
      </label>
    </div>
    <button class="btn" :disabled="saving" @click="saveLog">{{ saving ? '保存中…' : '保存沟通记录' }}
      <span v-if="['TEMP_CHANGE','EMOTIONAL','DISPUTE'].includes(form.type)" class="small">（该类型保存即锁定）</span>
    </button>

    <div v-if="lockedTempLogs.length" class="panel mt16" style="box-shadow:none">
      <div class="panel-hd"><h3>凭锁定沟通记录变更资源（礼厅/车辆/火化时段）</h3></div>
      <div class="panel-bd compact">
        <label class="fld"><span>依据沟通记录</span>
          <select v-model="changeForm.communicationLogId">
            <option value="">请选择</option>
            <option v-for="l in lockedTempLogs" :key="l.id" :value="l.id">
              #{{ l.id }}［{{ COMM_TYPE[l.type] }}］{{ l.content.slice(0, 30) }}…
            </option>
          </select>
        </label>
        <div class="form-grid">
          <label class="fld"><span>接运车辆</span>
            <select v-model="changeForm.vehicleId"><option value="">不变更</option>
              <option v-for="r in vehicles" :key="r.id" :value="r.id">{{ r.name }}</option></select>
          </label>
          <label class="fld"><span>冷藏位</span>
            <select v-model="changeForm.coldId"><option value="">不变更</option>
              <option v-for="r in colds" :key="r.id" :value="r.id">{{ r.name }}</option></select>
          </label>
          <label class="fld"><span>告别厅</span>
            <select v-model="changeForm.hallId"><option value="">不变更</option>
              <option v-for="r in halls" :key="r.id" :value="r.id">{{ r.name }}（{{ HALL_SPEC[r.hallSpec] }}）</option></select>
          </label>
          <label class="fld"><span>火化设备</span>
            <select v-model="changeForm.furnaceId"><option value="">不变更</option>
              <option v-for="r in furnaces" :key="r.id" :value="r.id">{{ r.name }}</option></select>
          </label>
          <label class="fld"><span>接运时间</span><input type="datetime-local" v-model="changeForm.pickupTime" /></label>
          <label class="fld"><span>告别时间</span><input type="datetime-local" v-model="changeForm.farewellTime" /></label>
        </div>
        <button class="btn warn" @click="applyChange">先记录后变更：执行资源变更并重新核验</button>
      </div>
    </div>

    <div class="panel mt16" style="box-shadow:none">
      <div class="panel-hd"><h3>沟通记录（{{ logs.length }}）</h3></div>
      <div class="panel-bd compact">
        <table v-if="logs.length">
          <thead><tr><th>时间</th><th>类型</th><th>内容</th><th>在场人员</th><th>记录人</th><th>锁定/执行</th></tr></thead>
          <tbody>
            <tr v-for="l in logs" :key="l.id">
              <td class="small">{{ fmtTime(l.createdAt) }}</td>
              <td><span class="tag" :class="l.type === 'CHAT' ? 'gray' : 'amber'">{{ COMM_TYPE[l.type] }}</span></td>
              <td style="min-width:260px">{{ l.content }}</td>
              <td class="small">{{ l.participants || '-' }}</td>
              <td class="small">{{ l.authorName }}</td>
              <td>
                <span class="tag" :class="l.locked ? 'red' : 'gray'">{{ l.locked ? '已锁定' : '普通' }}</span>
                <div v-if="l.changeApplied" class="tag green mt8">已据此变更</div>
                <div v-if="l.changeNote" class="small muted mt8">{{ l.changeNote }}</div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else class="empty">暂无沟通记录</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '../../api'
import { COMM_TYPE, HALL_SPEC, fmtTime } from '../../labels'

const props = defineProps({ d: Object, user: Object })
const emit = defineEmits(['changed', 'toast'])
const saving = ref(false)
const form = reactive({ type: 'CHAT', content: '', participants: '' })
const vehicles = ref([]), colds = ref([]), halls = ref([]), furnaces = ref([])
const changeForm = reactive({
  communicationLogId: '', vehicleId: '', coldId: '', hallId: '', furnaceId: '',
  pickupTime: '', farewellTime: ''
})

const logs = computed(() => props.d.communicationLogs || [])
const lockedTempLogs = computed(() => logs.value.filter(l => l.locked))

onMounted(async () => {
  const [v, c, h, f] = await Promise.all([
    api.get('/resources?type=VEHICLE'), api.get('/resources?type=COLD'),
    api.get('/resources?type=HALL'), api.get('/resources?type=FURNACE')
  ])
  vehicles.value = v; colds.value = c; halls.value = h; furnaces.value = f
})

async function saveLog() {
  if (!form.content.trim()) return emit('toast', '请填写沟通内容', 'err')
  saving.value = true
  try {
    await api.post('/orders/' + props.d.order.id + '/communications', { ...form })
    form.content = ''; form.participants = ''
    emit('changed', 'comm')
    emit('toast', '沟通记录已保存' + (['TEMP_CHANGE', 'EMOTIONAL', 'DISPUTE'].includes(form.type) ? '并锁定' : ''))
  } catch (e) { emit('toast', e.message, 'err') } finally { saving.value = false }
}

async function applyChange() {
  if (!changeForm.communicationLogId) return emit('toast', '请先选择一条已锁定的沟通记录', 'err')
  try {
    const r = await api.post('/orders/' + props.d.order.id + '/change-resource', {
      communicationLogId: Number(changeForm.communicationLogId),
      vehicleId: changeForm.vehicleId ? Number(changeForm.vehicleId) : null,
      coldId: changeForm.coldId ? Number(changeForm.coldId) : null,
      hallId: changeForm.hallId ? Number(changeForm.hallId) : null,
      furnaceId: changeForm.furnaceId ? Number(changeForm.furnaceId) : null,
      pickupTime: changeForm.pickupTime || undefined,
      farewellTime: changeForm.farewellTime || undefined,
      certificateStatus: props.d.order.certificateStatus
    })
    emit('changed', 'comm')
    emit('toast', r.pass ? '已凭沟通记录完成资源变更并核验通过' : '已发起变更，但仍有资源冲突，请看协同', r.pass ? 'ok' : 'err')
  } catch (e) { emit('toast', e.message, 'err') }
}
</script>
