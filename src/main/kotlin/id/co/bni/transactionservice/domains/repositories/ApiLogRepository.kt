package id.co.bni.transactionservice.domains.repositories

import id.co.bni.transactionservice.domains.entities.ApiAccessLog

interface ApiLogRepository {
    suspend fun insertApiLog(
        apiLog: ApiAccessLog
    ): ApiAccessLog
}