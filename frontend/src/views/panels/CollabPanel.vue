<template>
  <div v-if="d">
    <div class="hint blue">围绕<b>同一治丧单</b>，接运组、业务员、财务、礼厅管理员、馆领导协同处理：
      外地逝者、亲属意见不一致、证明材料缺失、低保减免待审核、告别厅临时冲突、火化设备检修。</div>

    <div v-if="canCreate" class="panel" style="box-shadow:none">
      <div class="panel-hd"><h3>发起协同</h3></div>
      <div class="panel-bd compact">
        <div class="form-grid">
          <label class="fld"><span>异常类型</span>
            <select v-model="form.type">
              <option v-for="(v, k) in COLLAB_TYPE" :key="k" :value="k">{{ v[0] }} → {{ ROLES[v[1]] }}</option>
            </select>
          </label>
          <label class="fld span2"><span>标题 *</span><input v-model="form.title" /></label>
          <label class="fld span3"><span>情况说明 *</span><textarea v-model="form.description" rows="2"></textarea></label>
        </div>
        <button class="btn" @click="create">发起协同到责任岗位</button>
      </div>
    </div>

    <table>
      <thead><tr><th>优先级</th><th>类型</th><th>事项</th><th>责任岗位</th><th>发起人</th><th>状态/处理结果</th><th>操作</th></tr></thead>
      <tbody>
        <tr v-for="t in tasks" :key="t.id">
          <td><span class="tag" :class="t.priority === 1 ? 'red' : 'gray'">{{ t.priority === 1 ? '高' : '中' }}</span></td>
          <td class="small">{{ COLLAB_TYPE[t.type]?.[0] || t.type }}</td>
          <td><b>{{ t.title }}</b><div class="muted small">{{ t.description }}</div></td>
          <td><span class="tag blue">{{ ROLES[t.assigneeRole] }}</span></td>
          <td class="small">{{ t.createdByName }}<div class="muted">{{ fmtTime(t.createdAt) }}</div></td>
          <td>
            <span class="tag" :class="t.status === 'RESOLVED' ? 'green' : 'amber'">
              {{ COLLAB_STATUS[t.status] }}</span>
            <div v-if="t.resolution" class="small mt8">处理：{{ t.assigneeName }} — {{ t.resolution }}</div>
          </td>
          <td>
            <template v-if="t.status !== 'RESOLVED'">
              <button v-if="canHandle(t)" class="btn sm success" @click="openResolve(t)">处理并回填结果</button>
              <span v-else class="muted small">等待{{ ROLES[t.assigneeRole] }}处理</span>
            </template>
          </td>
        </tr>
      </tbody>
    </table>
    <div v-if="!tasks.length" class="empty">本单暂无协同事项</div>

    <div v-if="resolving" class="modal-mask" @click.self="resolving = null">
      <div class="modal">
        <div class="modal-hd"><span>处理协同：{{ resolving.title }}</span>
          <button class="btn ghost sm" @click="resolving = null">关闭</button></div>
        <div class="modal-bd">
          <label class="fld"><span>处理结果 *</span>
            <textarea v-model="resolution" rows="4"
                      placeholder="如：已协调思亲厅15:00时段；家属已补交医学死亡证明复印件；长途车皖W·D003已排班…"></textarea>
          </label>
        </div>
        <div class="modal-ft">
          <button class="btn secondary" @click="resolving = null">取消</button>
          <button class="btn success" @click="resolve">提交处理结果</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { api, getUser } from '../../api'
import { ROLES, COLLAB_TYPE, COLLAB_STATUS, fmtTime } from '../../labels'

const props = defineProps({ d: Object, user: Object })
const emit = defineEmits(['changed', 'toast'])
const me = getUser()
const canCreate = me.role !== 'FAMILY'
const form = reactive({ type: 'DOC_MISSING', title: '', description: '' })
const resolving = ref(null)
const resolution = ref('')

const tasks = computed(() => props.d.collaborations || [])
function canHandle(t) {
  return me.role === 'LEADER' || me.role === t.assigneeRole
}

async function create() {
  if (!form.title.trim() || !form.description.trim()) return emit('toast', '请填写标题和说明', 'err')
  try {
    await api.post('/orders/' + props.d.order.id + '/collaborations', { ...form, priority: 1 })
    form.title = ''; form.description = ''
    emit('changed', 'collab')
    emit('toast', '协同已发起')
  } catch (e) { emit('toast', e.message, 'err') }
}
function openResolve(t) { resolving.value = t; resolution.value = '' }
async function resolve() {
  if (!resolution.value.trim()) return emit('toast', '请填写处理结果', 'err')
  try {
    await api.post('/orders/collaborations/' + resolving.value.id + '/resolve', { resolution: resolution.value })
    resolving.value = null
    emit('changed', 'collab')
    emit('toast', '协同已闭环')
  } catch (e) { emit('toast', e.message, 'err') }
}
</script>
