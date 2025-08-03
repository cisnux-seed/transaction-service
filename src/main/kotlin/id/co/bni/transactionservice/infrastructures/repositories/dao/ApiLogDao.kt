package id.co.bni.transactionservice.infrastructures.repositories.dao

import id.co.bni.transactionservice.commons.constants.HttpMethod
import id.co.bni.transactionservice.domains.entities.ApiAccessLog
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
interface ApiLogDao : CoroutineCrudRepository<ApiAccessLog, String> {
    @Modifying
    @Query(
        """
        INSERT INTO api_access_logs (
            id, external_service_id, api_key_id, endpoint, http_method,
            ip_address, user_agent, response_status, created_at
        ) VALUES (
            :id, :externalServiceId, :apiKeyId, :endpoint, :httpMethod::http_method_enum,
            :ipAddress, :userAgent, :responseStatus, :createdAt)
        """
    )
    suspend fun insertApiLog(
        id: String,
        externalServiceId: String,
        apiKeyId: String,
        endpoint: String,
        httpMethod: HttpMethod,
        ipAddress: String,
        userAgent: String,
        responseStatus: Int,
        createdAt: LocalDateTime = LocalDateTime.now()
    ): Int
}