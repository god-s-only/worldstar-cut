package com.worldstar.cut.features.premium.domain.repository

import com.worldstar.cut.features.premium.domain.model.PremiumPlan
import com.worldstar.cut.features.premium.domain.model.PremiumState
import kotlinx.coroutines.flow.Flow

interface PremiumRepository {
    fun getPremiumState(): Flow<PremiumState>
    suspend fun fetchAvailablePlans(): List<PremiumPlan>
    suspend fun purchasePlan(planId: String): Boolean
    suspend fun restorePurchases(): Boolean
    fun isPremium(): Boolean
}
