import { reactive } from 'vue'

export const uiState = reactive({
    isComposeOpen: false
})

export const toggleCompose = () => {
    uiState.isComposeOpen = !uiState.isComposeOpen
}
