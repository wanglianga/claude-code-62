<template>
  <Login v-if="!user" @login="onLogin" />
  <template v-else>
    <header class="app-header">
      <div class="logo">🕯 县殡仪服务预约与治丧物品结算平台
        <span class="sub">同一治丧单 · 全程留痕 · 签字可溯</span>
      </div>
      <nav class="app-nav">
        <button v-for="m in menus" :key="m.key" :class="{ active: view === m.key }"
                @click="go(m.key)">{{ m.label }}</button>
      </nav>
      <div class="user-box">
        <span class="role-chip">{{ ROLES[user.role] }}</span>
        <span>{{ user.displayName }}</span>
        <button class="btn-logout" @click="logout">退出</button>
      </div>
    </header>

    <div class="container">
      <Dashboard v-if="view === 'dashboard'" @open="openOrder" />
      <OrderList v-else-if="view === 'orders'" @open="openOrder" @create="go('create')" />
      <OrderCreate v-else-if="view === 'create'" @created="openOrder" />
      <CollabCenter v-else-if="view === 'collab'" @open="openOrder" />
      <ArchiveList v-else-if="view === 'archives'" @open="openOrder" />
      <OrderDetail v-else-if="view === 'detail' && selectedId" :id="selectedId"
                   @back="go('orders')" @changed="toast('已更新', 'ok')" @toast="toast" />
    </div>

    <transition>
      <div v-if="toastMsg" class="toast" :class="toastKind">{{ toastMsg }}</div>
    </transition>
  </template>
</template>

<script setup>
import { computed, ref } from 'vue'
import { getUser, clearSession } from './api'
import { ROLES } from './labels'
import Login from './views/Login.vue'
import Dashboard from './views/Dashboard.vue'
import OrderList from './views/OrderList.vue'
import OrderCreate from './views/OrderCreate.vue'
import OrderDetail from './views/OrderDetail.vue'
import CollabCenter from './views/CollabCenter.vue'
import ArchiveList from './views/ArchiveList.vue'

const user = ref(getUser())
const view = ref('dashboard')
const selectedId = ref(null)
const toastMsg = ref('')
const toastKind = ref('ok')

const menus = computed(() => {
  const r = user.value?.role
  const list = [{ key: 'dashboard', label: '工作台' }, { key: 'orders', label: '治丧单' }]
  if (r === 'FAMILY' || r === 'CLERK') list.push({ key: 'create', label: '新建预约' })
  if (r !== 'FAMILY') list.push({ key: 'collab', label: '协同中心' })
  list.push({ key: 'archives', label: '服务档案' })
  return list
})

function onLogin(u) {
  user.value = u
  view.value = 'dashboard'
}
function logout() {
  clearSession()
  user.value = null
}
function go(v) { view.value = v }
function openOrder(id) {
  selectedId.value = id
  view.value = 'detail'
}
let toastTimer = null
function toast(msg, kind = 'ok') {
  toastMsg.value = msg
  toastKind.value = kind
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toastMsg.value = ''), 2600)
}
</script>
