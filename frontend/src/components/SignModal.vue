<template>
  <div class="modal-mask" @click.self="$emit('close')">
    <div class="modal">
      <div class="modal-hd">
        <span>家属签字确认</span>
        <button class="btn ghost sm" @click="$emit('close')">关闭</button>
      </div>
      <div class="modal-bd">
        <div class="hint blue">{{ actionDesc }}</div>
        <div class="form-grid">
          <label class="fld"><span>签字家属姓名 *</span>
            <input type="text" v-model="form.signerName" placeholder="与逝者关系人姓名" />
          </label>
          <label class="fld"><span>与逝者关系</span>
            <input type="text" v-model="form.signerRelation" placeholder="配偶/子女..." />
          </label>
          <label class="fld"><span>联系电话</span>
            <input type="text" v-model="form.signerPhone" placeholder="选填" />
          </label>
        </div>
        <SignaturePad ref="padRef" v-model:data="form.signatureData" />
        <label class="fld mt12" v-if="showReason"><span>删减/变更原因</span>
          <textarea v-model="form.reason" placeholder="请如实填写删减原因，系统将留痕"></textarea>
        </label>
      </div>
      <div class="modal-ft">
        <button class="btn secondary" @click="padRef.clear()">清除重签</button>
        <button class="btn secondary" @click="$emit('close')">取消</button>
        <button class="btn success" @click="submit">确认签字并提交</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import SignaturePad from './SignaturePad.vue'

const props = defineProps({
  actionDesc: { type: String, default: '请家属阅读后签字确认' },
  showReason: { type: Boolean, default: false },
  extra: { type: Object, default: () => ({}) }
})
const emit = defineEmits(['submit', 'close'])

const padRef = ref(null)
const form = reactive({
  signerName: '', signerRelation: '', signerPhone: '', signatureData: '', reason: ''
})

async function submit() {
  if (!form.signerName.trim()) return alert('请填写签字家属姓名')
  if (!form.signatureData || form.signatureData.length < 300) return alert('请先在签名板手写签名')
  if (props.showReason && !form.reason.trim()) return alert('请填写删减/变更原因')
  emit('submit', { ...form, ...props.extra })
}
</script>
