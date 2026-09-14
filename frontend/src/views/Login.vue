<template>
  <div class="login-wrap">
    <div class="login-card">
      <h1>县殡仪服务预约与治丧物品结算平台</h1>
      <div class="sub">请使用岗位账号登录（演示环境）</div>
      <label class="fld"><span>用户名</span>
        <input type="text" v-model="username" placeholder="请输入用户名" @keyup.enter="login" />
      </label>
      <label class="fld"><span>密码</span>
        <input type="password" v-model="password" placeholder="请输入密码" @keyup.enter="login" />
      </label>
      <button class="btn" style="width:100%;padding:10px" :disabled="loading" @click="login">
        {{ loading ? '登录中…' : '登 录' }}
      </button>
      <div v-if="err" class="hint red mt12">{{ err }}</div>

      <div class="demo-accounts">
        <div class="muted small mb8">演示账号（点击可填充，密码均见下）：</div>
        <table>
          <tbody>
            <tr v-for="a in accounts" :key="a.u" @click="fill(a)">
              <td class="u">{{ a.u }}</td>
              <td>{{ a.label }}</td>
              <td class="muted">密码 {{ a.p }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { api, setSession } from '../api'

const emit = defineEmits(['login'])
const username = ref('family')
const password = ref('family123')
const err = ref('')
const loading = ref(false)

const accounts = [
  { u: 'family', p: 'family123', label: '家属（配偶 张淑芬）' },
  { u: 'family2', p: 'family123', label: '家属（长子 李建国，演示意见不一致）' },
  { u: 'clerk', p: 'clerk123', label: '业务员' },
  { u: 'transport', p: 'transport123', label: '接运组' },
  { u: 'halladmin', p: 'hall123', label: '礼厅管理员' },
  { u: 'finance', p: 'finance123', label: '财务' },
  { u: 'leader', p: 'leader123', label: '馆领导' }
]
function fill(a) {
  username.value = a.u
  password.value = a.p
  err.value = ''
}
async function login() {
  err.value = ''
  loading.value = true
  try {
    const data = await api.post('/auth/login', { username: username.value, password: password.value })
    setSession(data.token, data.user)
    emit('login', data.user)
  } catch (e) {
    err.value = e.message
  } finally {
    loading.value = false
  }
}
</script>
