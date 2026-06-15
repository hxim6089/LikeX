<template>
  <div class="persona-card" ref="cardRef">
    <div class="card-bg"></div>
    <div class="card-content">
        <el-avatar :size="80" :src="persona.avatarUrl" class="profile-avatar" />
        <div class="profile-name">{{ persona.username }}</div>
        <div class="profile-handle">{{ persona.handle }}</div>
        <div class="profile-bio" v-if="persona.bio">{{ persona.bio }}</div>
        
        <div class="stat-row">
            <div class="stat">
                <div class="stat-val">{{ persona.totalLikes || 0 }}</div>
                <div class="stat-label">点赞数</div>
            </div>
            <div class="stat">
                <div class="stat-val">{{ persona.interestTags ? persona.interestTags.length : 0 }}</div>
                <div class="stat-label">兴趣偏好</div>
            </div>
        </div>

        <div class="tags-section">
            <h3>兴趣基因</h3>
            <div class="tags">
                <span v-for="tag in persona.interestTags" :key="tag" class="tag">{{ tag }}</span>
                <span v-if="!persona.interestTags || persona.interestTags.length === 0" class="no-tags">暂无数据</span>
            </div>
        </div>

        <div class="persona-badge">
            {{ persona.personaTitle || '观察者' }}
        </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
    persona: Object
})
</script>

<style scoped>
/* Persona Card Styling */
.persona-card {
    border-radius: 20px;
    overflow: hidden;
    position: relative;
    background: linear-gradient(135deg, #1d9bf0 0%, #8e44ad 100%);
    color: white;
    box-shadow: 0 10px 30px rgba(0,0,0,0.2);
    width: 400px;
    min-height: 500px; /* specific height for export consistency */
}
.card-content {
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(10px);
    height: 100%;
    padding: 30px;
    display: flex;
    flex-direction: column;
    align-items: center;
    position: relative;
}
.profile-avatar {
    border: 4px solid white;
    margin-bottom: 10px;
}
.profile-name { font-size: 24px; font-weight: bold; }
.profile-handle { font-size: 16px; opacity: 0.8; margin-bottom: 10px; }
.profile-bio { font-size: 14px; opacity: 0.9; margin-bottom: 20px; font-style: italic; text-align: center;}

.stat-row {
    display: flex;
    gap: 40px;
    margin-bottom: 20px;
}
.stat { text-align: center; }
.stat-val { font-size: 20px; font-weight: bold; }
.stat-label { font-size: 12px; opacity: 0.8; }

.tags-section h3 { margin: 0 0 10px 0; font-size: 14px; opacity: 0.8; text-transform: uppercase; letter-spacing: 1px; }
.tags { display: flex; gap: 10px; flex-wrap: wrap; justify-content: center; }
.tag {
    background: white;
    color: #1d9bf0;
    padding: 5px 15px;
    border-radius: 20px;
    font-weight: bold;
    font-size: 14px;
    margin-bottom: 5px;
}
.no-tags { font-style: italic; opacity: 0.7; }

.persona-badge {
    position: absolute;
    top: 20px;
    right: 20px;
    background: #ffcc00;
    color: black;
    padding: 5px 10px;
    border-radius: 5px;
    font-weight: bold;
    transform: rotate(10deg);
}
</style>
