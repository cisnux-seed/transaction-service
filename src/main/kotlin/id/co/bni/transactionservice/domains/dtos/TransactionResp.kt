package id.co.bni.transactionservice.domains.dtos

import id.co.bni.transactionservice.commons.constants.TransactionStatus
import java.math.BigDecimal
import java.time.LocalDateTime

data class TransactionResp(
    val id: String,
    val userId: Long,
    val accountId: String,
    val transactionId: String,
    val transactionStatus: TransactionStatus,
    val amount: BigDecimal,
    val currency: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
