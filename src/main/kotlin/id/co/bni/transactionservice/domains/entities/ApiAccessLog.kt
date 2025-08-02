package id.co.bni.transactionservice.domains.entities

import id.co.bni.transactionservice.commons.constants.HttpMethod
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.UUID

@Table("api_access_logs")
data class ApiAccessLog(
    @Id
    val id: String = UUID.randomUUID().toString(),
    @Column("external_service_id")
    val externalServiceId: String,
    @Column("api_key_id")
    val apiKeyId: String,
    val endpoint: String,
    @Column("http_method")
    val httpMethod: HttpMethod,
    @Column("ip_address")
    val ipAddress: String,
    @Column("user_agent")
    val userAgent: String,
    @Column("response_status")
    val responseStatus: Int,
    @Column("created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
)
