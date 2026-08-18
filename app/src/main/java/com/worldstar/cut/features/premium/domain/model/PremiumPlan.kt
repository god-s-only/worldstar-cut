package com.worldstar.cut.features.premium.domain.model

data class PremiumPlan(
    val id: String,
    val name: String,
    val price: String,
    val period: String,
    val features: List<String>
)

data class PremiumState(
    val isPremium: Boolean = false,
    val planName: String = "",
    val expiryMs: Long = 0L,
    val availablePlans: List<PremiumPlan> = emptyList(),
    val isLoading: Boolean = false,
    val selectedPlanId: String? = null
) {
    val isActive: Boolean
        get() = isPremium && (expiryMs == 0L || expiryMs > System.currentTimeMillis())

    val expiryFormatted: String
        get() {
            if (expiryMs <= 0L) return ""
            val sdf = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(expiryMs))
        }
}
