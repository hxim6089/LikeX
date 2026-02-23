<template>
  <div class="layout-container">
    <!-- Left Sidebar -->
    <aside class="sidebar">
      <Sidebar />
    </aside>

    <!-- Center Feed -->
    <main class="main-feed">
      <div class="feed-header">
        <h2>Home</h2>
      </div>
      <slot />
    </main>

    <!-- Right Widgets -->
    <aside class="right-panel">
      <RightPanel />
    </aside>
  </div>
</template>

<script setup>
import Sidebar from './Sidebar.vue'
import RightPanel from './RightPanel.vue'
</script>

<style scoped>
.layout-container {
  display: flex;
  justify-content: center;
  min-height: 100vh;
  background-color: #fff; /* or black for dark mode */
}

/* 3-Column Widths */
.sidebar {
  width: 275px;
  padding: 0 10px;
  border-right: 1px solid #eff3f4;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.main-feed {
  width: 600px;
  border-right: 1px solid #eff3f4;
}

.right-panel {
  width: 350px;
  padding-left: 30px;
}

.feed-header {
  height: 53px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  border-bottom: 1px solid #eff3f4;
  position: sticky;
  top: 0;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  z-index: 100;
}
</style>
