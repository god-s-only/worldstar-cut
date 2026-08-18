package com.worldstar.cut.features.export.domain.model

data class ExportSettings(
    val resolution: ExportResolution = ExportResolution.HD_1080,
    val frameRate: Int = 30,
    val quality: ExportQuality = ExportQuality.HIGH,
    val includeAudio: Boolean = true,
    val preserveOriginal: Boolean = false
) {
    val resolutionLabel: String get() = resolution.label
    val width: Int get() = resolution.width
    val height: Int get() = resolution.height
}

enum class ExportResolution(val width: Int, val height: Int, val label: String) {
    SD_480(854, 480, "480p"),
    HD_720(1280, 720, "720p"),
    HD_1080(1920, 1080, "1080p"),
    UHD_4K(3840, 2160, "4K")
}

enum class ExportQuality(val bitrateMultiplier: Float, val label: String) {
    LOW(0.5f, "Low"),
    MEDIUM(0.75f, "Medium"),
    HIGH(1.0f, "High"),
    MAX(1.5f, "Max")
}

sealed class ExportState {
    data object Idle : ExportState()
    data class Preparing(val projectId: Long) : ExportState()
    data class InProgress(val progress: Float, val phase: String) : ExportState()
    data class Completed(val outputPath: String) : ExportState()
    data class Failed(val error: String) : ExportState()
}
