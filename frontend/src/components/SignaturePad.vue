<template>
  <div>
    <canvas ref="canvasRef" class="sig-canvas" :height="140"
            @pointerdown="start" @pointermove="move" @pointerup="end"
            @pointerleave="end"></canvas>
    <div class="sig-tip">请用鼠标/触屏在上方区域手写签名</div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'

const props = defineProps({ clearSignal: { type: Number, default: 0 } })
const emit = defineEmits(['update:data'])

const canvasRef = ref(null)
let ctx = null
let drawing = false
let empty = true

function resize() {
  const canvas = canvasRef.value
  const ratio = window.devicePixelRatio || 1
  const w = canvas.clientWidth || 480
  const h = 140
  canvas.width = w * ratio
  canvas.height = h * ratio
  ctx = canvas.getContext('2d')
  ctx.scale(ratio, ratio)
  ctx.lineWidth = 2.4
  ctx.lineCap = 'round'
  ctx.lineJoin = 'round'
  ctx.strokeStyle = '#1f2933'
}

onMounted(resize)

function pos(e) {
  const rect = canvasRef.value.getBoundingClientRect()
  return { x: e.clientX - rect.left, y: e.clientY - rect.top }
}

function start(e) {
  drawing = true
  empty = false
  canvasRef.value.setPointerCapture(e.pointerId)
  const { x, y } = pos(e)
  ctx.beginPath()
  ctx.moveTo(x, y)
}
function move(e) {
  if (!drawing) return
  const { x, y } = pos(e)
  ctx.lineTo(x, y)
  ctx.stroke()
}
function end() {
  if (!drawing) return
  drawing = false
  emit('update:data', empty ? '' : canvasRef.value.toDataURL('image/png'))
}

function clear() {
  const canvas = canvasRef.value
  ctx.clearRect(0, 0, canvas.width, canvas.height)
  empty = true
  emit('update:data', '')
}

defineExpose({ clear, isEmpty: () => empty })
</script>
