package com.worldstar.cut.features.text_sticker.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.worldstar.cut.features.text_sticker.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class TextStickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val projectId: Long = savedStateHandle["project_id"] ?: -1L

    private val _uiState = MutableStateFlow(TextStickerState())
    val uiState: StateFlow<TextStickerState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TextStickerEvent>()
    val events: SharedFlow<TextStickerEvent> = _events.asSharedFlow()

    // ─── Text Operations ──────────────────────────────────────────────────────

    fun onAddTextOverlay() {
        val overlay = TextOverlay(
            id = System.currentTimeMillis(),
            text = "Your text here",
            startMs = 0L,
            endMs = 5000L
        )
        _uiState.update {
            it.copy(
                overlays = it.overlays + overlay,
                selectedOverlayId = overlay.id,
                isEditingText = true
            )
        }
    }

    fun onOverlayTextChange(text: String) {
        val id = _uiState.value.selectedOverlayId ?: return
        _uiState.update {
            it.copy(overlays = it.overlays.map { o -> if (o.id == id) o.copy(text = text) else o })
        }
    }

    fun onOverlayColorChange(color: Long) {
        val id = _uiState.value.selectedOverlayId ?: return
        _uiState.update {
            it.copy(overlays = it.overlays.map { o -> if (o.id == id) o.copy(color = color) else o })
        }
    }

    fun onOverlayAnimationChange(animation: TextAnimation) {
        val id = _uiState.value.selectedOverlayId ?: return
        _uiState.update {
            it.copy(overlays = it.overlays.map { o -> if (o.id == id) o.copy(animation = animation) else o })
        }
    }

    fun onOverlaySelected(id: Long?) {
        _uiState.update { it.copy(selectedOverlayId = id, selectedStickerId = null, isEditingText = id != null) }
    }

    fun onRemoveOverlay(id: Long) {
        _uiState.update {
            it.copy(
                overlays = it.overlays.filter { o -> o.id != id },
                selectedOverlayId = if (it.selectedOverlayId == id) null else it.selectedOverlayId
            )
        }
    }

    // ─── Sticker Operations ───────────────────────────────────────────────────

    fun onAddSticker(emoji: String) {
        val sticker = Sticker(
            id = System.currentTimeMillis(),
            emoji = emoji,
            startMs = 0L,
            endMs = 5000L
        )
        _uiState.update {
            it.copy(
                stickers = it.stickers + sticker,
                selectedStickerId = sticker.id
            )
        }
    }

    fun onStickerSelected(id: Long?) {
        _uiState.update { it.copy(selectedStickerId = id, selectedOverlayId = null, isEditingText = false) }
    }

    fun onStickerAnimationChange(animation: StickerAnimation) {
        val id = _uiState.value.selectedStickerId ?: return
        _uiState.update {
            it.copy(stickers = it.stickers.map { s -> if (s.id == id) s.copy(animation = animation) else s })
        }
    }

    fun onRemoveSticker(id: Long) {
        _uiState.update {
            it.copy(
                stickers = it.stickers.filter { s -> s.id != id },
                selectedStickerId = if (it.selectedStickerId == id) null else it.selectedStickerId
            )
        }
    }

    fun onClearSelection() {
        _uiState.update { it.copy(selectedOverlayId = null, selectedStickerId = null, isEditingText = false) }
    }

    fun onApplyAll() {
        _events.tryEmit(TextStickerEvent.ChangesApplied)
    }
}

sealed class TextStickerEvent {
    data object ChangesApplied : TextStickerEvent()
}
