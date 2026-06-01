<script setup>
import { ref } from 'vue'
import { useAuth } from '../composables/useAuth'

const { doLogin, loading, error } = useAuth()
const form = ref({ username: '', password: '' })
const fieldErrors = ref({})

function validate() {
  const e = {}
  if (!form.value.username.trim()) e.username = '请输入用户名'
  if (!form.value.password) e.password = '请输入密码'
  fieldErrors.value = e
  return Object.keys(e).length === 0
}

async function submit() {
  fieldErrors.value = {}
  if (!validate()) return
  try { await doLogin(form.value) } catch { /* handled in composable */ }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-hero">
        <div class="hero-graphic"><span class="hero-char">&#9742;</span></div>
        <h1 class="auth-title">宠物时光手帐</h1>
        <p class="auth-sub">记录那些与你相伴的生命，<br/>每一刻都值得被记住。</p>
      </div>

      <form class="auth-form" @submit.prevent="submit">
        <div class="field">
          <label class="field-label" for="username">用户名</label>
          <input id="username" v-model="form.username" type="text" class="field-input" :class="{ 'has-error': fieldErrors.username }" placeholder="你的账号" autocomplete="username" />
          <span class="field-err" v-if="fieldErrors.username">{{ fieldErrors.username }}</span>
        </div>

        <div class="field">
          <label class="field-label" for="password">密码</label>
          <input id="password" v-model="form.password" type="password" class="field-input" :class="{ 'has-error': fieldErrors.password }" placeholder="你的密码" autocomplete="current-password" />
          <span class="field-err" v-if="fieldErrors.password">{{ fieldErrors.password }}</span>
        </div>

        <p class="form-error" v-if="error">{{ error }}</p>

        <button class="btn-submit" type="submit" :disabled="loading">
          <span v-if="loading" class="spinner"></span>
          <span v-else>走进回忆</span>
        </button>

        <p class="auth-switch">还没有记录过？<router-link to="/register">从这里开始</router-link></p>
      </form>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--bg-warm) 0%, var(--bg-aged) 100%);
  padding: 2rem;
}
.auth-card {
  width: 100%;
  max-width: 400px;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-elevated);
  overflow: hidden;
}
.auth-hero { text-align: center; padding: 2.5rem 2rem 1.5rem; }
.hero-graphic { margin-bottom: 1rem; }
.hero-char { font-size: 3rem; color: var(--accent); opacity: 0.7; }
.auth-title {
  font-family: var(--font-display); font-weight: 900; font-size: 1.75rem;
  color: var(--text-primary); letter-spacing: 0.06em; margin-bottom: 0.5rem;
}
.auth-sub { font-size: 0.9rem; color: var(--text-muted); line-height: 1.7; }
.auth-form { padding: 0.5rem 2rem 2rem; display: flex; flex-direction: column; gap: 1.25rem; }
.field { display: flex; flex-direction: column; gap: 4px; }
.field-label { font-size: 0.8rem; color: var(--text-secondary); letter-spacing: 0.04em; }
.field-input {
  padding: 10px 14px; border: 1px solid var(--border-default); border-radius: var(--radius-sm);
  background: var(--bg-input); font-family: var(--font-body); font-size: 0.95rem;
  color: var(--text-primary); outline: none; transition: border-color var(--duration-fast) var(--ease-out);
}
.field-input:focus { border-color: var(--accent); }
.field-input.has-error { border-color: var(--danger); }
.field-input::placeholder { color: var(--text-muted); }
.field-err { font-size: 0.78rem; color: var(--danger); }
.form-error { text-align: center; font-size: 0.85rem; color: var(--danger); }
.btn-submit {
  margin-top: 0.5rem; padding: 12px; border: none; border-radius: var(--radius-sm);
  background: var(--accent); color: var(--text-inverse); font-family: var(--font-body);
  font-size: 1rem; cursor: pointer; transition: background var(--duration-fast) var(--ease-out);
  display: flex; justify-content: center; align-items: center; min-height: 44px;
}
.btn-submit:hover:not(:disabled) { background: var(--accent-light); }
.btn-submit:disabled { opacity: 0.6; cursor: not-allowed; }
.spinner {
  width: 18px; height: 18px; border: 2px solid transparent;
  border-top-color: var(--text-inverse); border-radius: 50%; animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.auth-switch { text-align: center; font-size: 0.85rem; color: var(--text-muted); }
.auth-switch a { color: var(--accent); font-weight: 600; }
</style>
