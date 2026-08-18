package com.worldstar.cut.features.filters_effects.domain.model

data class VideoFilter(
    val id: String,
    val name: String,
    val category: FilterCategory,
    val intensity: Float = 1.0f,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val hue: Float = 0f,
    val temperature: Float = 0f,
    val vignette: Float = 0f,
    val grain: Float = 0f
)

enum class FilterCategory(val label: String) {
    NONE("Original"),
    VINTAGE("Vintage"),
    BLACK_AND_WHITE("B&W"),
    WARM("Warm"),
    COOL("Cool"),
    DRAMATIC("Dramatic"),
    FILM("Film"),
    VIVID("Vivid"),
    CUSTOM("Custom")
}

data class VideoEffect(
    val id: String,
    val name: String,
    val type: EffectType,
    val durationMs: Long = 0L,
    val parameters: Map<String, Float> = emptyMap()
)

enum class EffectType(val label: String) {
    GLITCH("Glitch"),
    VHS("VHS"),
    CHROMATIC_ABERRATION("Chromatic"),
    SHAKE("Shake"),
    ZOOM_BLUR("Zoom Blur"),
    SPIN("Spin"),
    RGB_SPLIT("RGB Split"),
    FLASH("Flash"),
    SHAKE_INTENSE("Heavy Shake")
}

data class FiltersEffectsState(
    val appliedFilter: VideoFilter = VideoFilter(
        id = "original",
        name = "Original",
        category = FilterCategory.NONE
    ),
    val appliedEffect: VideoEffect? = null,
    val previewProgress: Float = 0f,
    val isPlaying: Boolean = false,
    val filters: List<VideoFilter> = defaultFilters(),
    val effects: List<VideoEffect> = defaultEffects()
)

fun defaultFilters(): List<VideoFilter> = listOf(
    VideoFilter(id = "original", name = "Original", category = FilterCategory.NONE),
    VideoFilter(id = "vintage_1", name = "Retro", category = FilterCategory.VINTAGE,
        brightness = 0.05f, contrast = 1.2f, saturation = 0.8f, temperature = 15f, vignette = 0.3f),
    VideoFilter(id = "vintage_2", name = "Faded", category = FilterCategory.VINTAGE,
        brightness = 0.1f, contrast = 0.9f, saturation = 0.7f, grain = 0.15f),
    VideoFilter(id = "bw_1", name = "Classic B&W", category = FilterCategory.BLACK_AND_WHITE,
        saturation = 0f, contrast = 1.1f),
    VideoFilter(id = "bw_2", name = "High Contrast", category = FilterCategory.BLACK_AND_WHITE,
        saturation = 0f, contrast = 1.5f, brightness = -0.05f),
    VideoFilter(id = "warm_1", name = "Golden Hour", category = FilterCategory.WARM,
        temperature = 25f, saturation = 1.2f, brightness = 0.05f),
    VideoFilter(id = "cool_1", name = "Arctic", category = FilterCategory.COOL,
        temperature = -20f, saturation = 0.9f, contrast = 1.1f),
    VideoFilter(id = "cool_2", name = "Blue Tint", category = FilterCategory.COOL,
        temperature = -30f, saturation = 0.8f),
    VideoFilter(id = "dramatic_1", name = "Cinematic", category = FilterCategory.DRAMATIC,
        contrast = 1.4f, saturation = 0.9f, vignette = 0.5f),
    VideoFilter(id = "dramatic_2", name = "Noir", category = FilterCategory.DRAMATIC,
        contrast = 1.3f, brightness = -0.1f, vignette = 0.6f),
    VideoFilter(id = "film_1", name = "Kodak Gold", category = FilterCategory.FILM,
        saturation = 1.1f, contrast = 1.05f, temperature = 10f, grain = 0.1f),
    VideoFilter(id = "film_2", name = "Fujifilm", category = FilterCategory.FILM,
        saturation = 0.95f, contrast = 1.1f, temperature = -5f),
    VideoFilter(id = "vivid_1", name = "Vivid Pop", category = FilterCategory.VIVID,
        saturation = 1.5f, contrast = 1.1f, brightness = 0.05f),
    VideoFilter(id = "vivid_2", name = "Neon", category = FilterCategory.VIVID,
        saturation = 1.8f, contrast = 1.2f, hue = 15f)
)

fun defaultEffects(): List<VideoEffect> = listOf(
    VideoEffect(id = "glitch", name = "Glitch", type = EffectType.GLITCH, durationMs = 2000),
    VideoEffect(id = "vhs", name = "VHS Static", type = EffectType.VHS, durationMs = 3000),
    VideoEffect(id = "chromatic", name = "Chromatic", type = EffectType.CHROMATIC_ABERRATION, durationMs = 1500),
    VideoEffect(id = "shake", name = "Shake", type = EffectType.SHAKE, durationMs = 1000),
    VideoEffect(id = "zoom_blur", name = "Zoom Blur", type = EffectType.ZOOM_BLUR, durationMs = 2000),
    VideoEffect(id = "rgb_split", name = "RGB Split", type = EffectType.RGB_SPLIT, durationMs = 2500),
    VideoEffect(id = "flash", name = "Flash", type = EffectType.FLASH, durationMs = 500),
    VideoEffect(id = "heavy_shake", name = "Heavy Shake", type = EffectType.SHAKE_INTENSE, durationMs = 1500)
)
