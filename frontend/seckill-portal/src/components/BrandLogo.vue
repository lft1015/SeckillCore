<template>
  <div class="brand" :class="{ compact }">
    <span class="brand-mark" aria-hidden="true"><svg viewBox="0 0 32 32" fill="none"><path d="M18.6 4.5 8.2 17.1h7.1l-1.9 10.4 10.4-13.2h-7l1.8-9.8Z" fill="currentColor"/></svg></span>
    <span class="brand-name">Seckill<span>Core</span></span>
  </div>
</template>
<script setup>
defineProps({ compact: Boolean })
</script>
<style scoped>
.brand{display:inline-flex;align-items:center;gap:12px;color:var(--text-strong)}.brand-mark{display:grid;width:42px;height:42px;place-items:center;border-radius:13px;color:#fff;background:linear-gradient(145deg,var(--primary),#5c9dff);box-shadow:0 8px 20px rgba(45,113,235,.23)}.brand-mark svg{width:25px;height:25px}.brand-name{font-size:21px;font-weight:760;letter-spacing:-.5px}.brand-name span{color:var(--primary)}.compact .brand-mark{width:36px;height:36px;border-radius:11px}.compact .brand-mark svg{width:22px}.compact .brand-name{font-size:19px}
</style>
