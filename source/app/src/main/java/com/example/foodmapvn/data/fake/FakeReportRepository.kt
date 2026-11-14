package com.example.foodmapvn.data.fake

import com.example.foodmapvn.data.repository.ReportReason
import com.example.foodmapvn.data.repository.ReportRepository

class FakeReportRepository : ReportRepository {
    override suspend fun report(
        targetType: String,
        targetId: String,
        reason: ReportReason,
        note: String,
    ): Result<Unit> = Result.success(Unit)
}