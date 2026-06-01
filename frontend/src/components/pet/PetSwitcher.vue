<script setup>
import { usePets } from '../../composables/usePets'
import PetCard from './PetCard.vue'

const { pets, currentPet, selectPet } = usePets()
const emit = defineEmits(['create', 'edit'])
</script>

<template>
  <div class="pet-switcher">
    <div class="switcher-label">宠物档案</div>
    <div class="switcher-list" v-if="pets.length > 0">
      <div
        v-for="pet in pets"
        :key="pet.id"
        class="switcher-item"
        :class="{ 'is-active': currentPet?.id === pet.id }"
      >
        <PetCard
          :pet="pet"
          :compact="true"
          :class="{ 'is-active': currentPet?.id === pet.id }"
          @select="selectPet"
        />
        <button class="btn-edit" title="修改宠物档案" @click.stop="emit('edit', pet)">
          修改
        </button>
      </div>
    </div>
    <div class="switcher-empty" v-else>
      <p>还没有宠物，创建第一只吧。</p>
    </div>
    <button class="btn-add" @click="emit('create')">+ 新宠物</button>
  </div>
</template>

<style scoped>
.pet-switcher {
  padding: 0.4rem 0 0;
}
.switcher-label {
  font-family: var(--font-display);
  font-size: 0.8rem;
  color: var(--text-muted);
  letter-spacing: 0;
  margin-bottom: 8px;
}
.switcher-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}
.switcher-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 6px;
  align-items: stretch;
}
.switcher-list :deep(.pet-card) {
  min-width: 0;
}
.switcher-list :deep(.pet-card.is-active) {
  border-color: var(--accent);
  background: var(--accent-surface);
}
.btn-edit {
  align-self: stretch;
  padding: 0 10px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-lg);
  background: rgba(255, 254, 249, 0.76);
  color: var(--text-muted);
  font-family: var(--font-body);
  font-size: 0.74rem;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}
.switcher-item.is-active .btn-edit {
  border-color: rgba(92, 122, 94, 0.28);
  color: var(--accent);
}
.btn-edit:hover {
  border-color: var(--accent);
  background: var(--accent-surface);
  color: var(--accent);
}
.switcher-empty {
  padding: 1rem;
  text-align: center;
  font-size: 0.85rem;
  color: var(--text-muted);
}
.btn-add {
  width: 100%;
  padding: 9px;
  border: 1px dashed var(--border-default);
  background: rgba(245, 239, 224, 0.35);
  color: var(--text-secondary);
  font-family: var(--font-body);
  font-size: 0.85rem;
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}
.btn-add:hover { border-color: var(--accent); color: var(--accent); }
</style>
