<template>
  <div>
    <div class="panel">
      <div class="panel-hd"><h3>新建治丧预约</h3>
        <span class="muted small">提交后进入资源核验（证件 / 接运车辆 / 冷藏位 / 火化排期 / 礼厅）</span>
      </div>
      <div class="panel-bd">
        <div class="hint">请如实填写逝者信息与联系方式。<b>死亡证明暂缺</b>可先登记为"材料缺失"，系统将自动发起业务员协同补办；
          <b>外地逝者</b>将自动通知接运组安排长途车辆。</div>
        <div class="form-grid">
          <label class="fld"><span>逝者姓名 *</span><input v-model="f.deceasedName" /></label>
          <label class="fld"><span>性别</span>
            <select v-model="f.gender"><option value="">请选择</option><option>男</option><option>女</option></select>
          </label>
          <label class="fld"><span>年龄</span><input type="number" v-model="f.age" min="0" /></label>
          <label class="fld span2"><span>身份证号</span><input v-model="f.idCardNo" placeholder="选填，用于证件核验" /></label>
          <label class="fld"><span>死亡时间</span><input type="datetime-local" v-model="f.deathTime" /></label>
          <label class="fld span3"><span>死亡原因/医院或居家</span><input v-model="f.deathCause" /></label>

          <label class="fld"><span>死亡证明状态</span>
            <select v-model="f.certificateStatus">
              <option value="PENDING">待核验（已带证，待业务员核）</option>
              <option value="VERIFIED">已核验</option>
              <option value="MISSING">材料缺失（先登记后补办）</option>
            </select>
          </label>
          <label class="fld span2"><span>死亡证明编号</span><input v-model="f.certificateNo" /></label>
          <label class="fld span3"><span>证明材料备注（缺失时说明补件方式/时限）</span>
            <input v-model="f.certificateNote" placeholder="如：医学死亡证明在医院结算处，今日17点前可取" /></label>

          <label class="fld span2"><span>接运地点 *</span><input v-model="f.pickupAddress" placeholder="详细地址" /></label>
          <label class="fld"><span>期望接运时间</span><input type="datetime-local" v-model="f.pickupTime" /></label>
          <label class="fld span3"><span>宗教/民族习俗</span>
            <input v-model="f.religiousCustom" placeholder="如：回族土葬习俗/佛教诵经/无特殊要求" /></label>
          <label class="fld"><span>是否需要冷藏</span>
            <select v-model="f.needRefrigeration"><option :value="false">不需要</option><option :value="true">需要馆内冷藏</option></select>
          </label>
          <label class="fld"><span>期望告别厅规格</span>
            <select v-model="f.hallSpec">
              <option value="">请选择</option>
              <option v-for="(v, k) in HALL_SPEC" :key="k" :value="k">{{ v }}</option>
            </select>
          </label>
          <label class="fld"><span>期望告别时间</span><input type="datetime-local" v-model="f.farewellTime" /></label>

          <label class="fld"><span>亲属联系人 *</span><input v-model="f.contactName" /></label>
          <label class="fld"><span>与逝者关系</span><input v-model="f.contactRelation" placeholder="配偶/子女..." /></label>
          <label class="fld"><span>联系电话</span><input v-model="f.contactPhone" /></label>
          <label class="fld span3">
            <span class="check-row"><input type="checkbox" v-model="f.fromOtherCity" /> 逝者在外地死亡/需跨县接运（提交后请在「② 异地/跨县接运」登记死亡地、当地联系人、证明机构、车辆司机与许可）</span>
          </label>
        </div>
        <div v-if="err" class="hint red mt12">{{ err }}</div>
        <div class="mt16 btn-row">
          <button class="btn" :disabled="saving" @click="submit">{{ saving ? '提交中…' : '提交预约并进入核验' }}</button>
          <span class="muted small">提交即生成唯一治丧单号，后续沟通、签字、费用全部归集到该单</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { api } from '../api'
import { HALL_SPEC } from '../labels'

const emit = defineEmits(['created'])
const saving = ref(false)
const err = ref('')
const f = reactive({
  deceasedName: '', gender: '', age: null, idCardNo: '', deathTime: '', deathCause: '',
  certificateStatus: 'PENDING', certificateNo: '', certificateNote: '',
  pickupAddress: '', pickupTime: '', religiousCustom: '', needRefrigeration: false,
  hallSpec: '', farewellTime: '', contactName: '', contactRelation: '', contactPhone: '',
  fromOtherCity: false
})

async function submit() {
  err.value = ''
  if (!f.deceasedName.trim()) return (err.value = '请填写逝者姓名')
  if (!f.pickupAddress.trim()) return (err.value = '请填写接运地点')
  if (!f.contactName.trim()) return (err.value = '请填写亲属联系人')
  saving.value = true
  try {
    const o = await api.post('/orders', { ...f })
    emit('created', o.id)
  } catch (e) {
    err.value = e.message
  } finally {
    saving.value = false
  }
}
</script>
