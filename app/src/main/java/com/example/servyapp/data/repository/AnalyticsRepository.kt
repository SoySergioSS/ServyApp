package com.example.servyapp.data.repository

import com.example.servyapp.data.datasource.AnalyticsRemoteDataSource
import com.example.servyapp.domain.model.Order
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepository @Inject constructor(
    private val analyticsDataSource: AnalyticsRemoteDataSource
) {

    suspend fun updateUserAnalytics(userId: String, order: Order): Result<Unit> {
        return analyticsDataSource.updateUserAnalytics(userId, order)
    }

    suspend fun getUserAnalytics(userId: String): Result<Map<String, Any>> {
        return analyticsDataSource.getUserAnalytics(userId)
    }
}