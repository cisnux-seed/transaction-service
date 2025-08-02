package id.co.bni.transactionservice.infrastructures.repositories

import id.co.bni.transactionservice.domains.entities.ApiAccessLog
import id.co.bni.transactionservice.domains.repositories.ApiLogRepository
import id.co.bni.transactionservice.infrastructures.repositories.dao.ApiLogDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Repository

@Repository
class ApiLogRepositoryImpl(
    private val apiLogDao: ApiLogDao
) : ApiLogRepository {
    override suspend fun insertApiLog(apiLog: ApiAccessLog): ApiAccessLog = withContext(Dispatchers.IO) {
        apiLogDao.insertApiLog(
            id = apiLog.id,
            externalServiceId = apiLog.externalServiceId,
            apiKeyId = apiLog.apiKeyId,
            endpoint = apiLog.endpoint,
            httpMethod = apiLog.httpMethod,
            ipAddress = apiLog.ipAddress,
            userAgent = apiLog.userAgent,
            responseStatus = apiLog.responseStatus,
            createdAt = apiLog.createdAt
        )
        apiLog
    }
}