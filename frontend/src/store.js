import { reactive } from 'vue'

export const uiState = reactive({
    isComposeOpen: false,
    unreadNotifications: 0
})

export const toggleCompose = () => {
    uiState.isComposeOpen = !uiState.isComposeOpen
}
