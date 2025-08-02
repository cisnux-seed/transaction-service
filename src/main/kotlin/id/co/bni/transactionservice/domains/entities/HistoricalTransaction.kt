package id.co.bni.transactionservice.domains.entities

import id.co.bni.transactionservice.commons.constants.PaymentMethod
import id.co.bni.transactionservice.commons.constants.TransactionStatus
import id.co.bni.transactionservice.commons.constants.TransactionType
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDateTime

@Table("historical_transactions")
data class HistoricalTransaction(
    @Id
    val id: String,
    @Column("user_id")
    val userId: Long,
    @Column("account_id")
    val accountId: String,
    @Column("transaction_id")
    val transactionId: String,
    @Column("transaction_type")
    val transactionType: TransactionType,
    @Column("transaction_status")
    val transactionStatus: TransactionStatus,
    val amount: BigDecimal,
    @Column("balance_before")
    val balanceBefore: BigDecimal,
    @Column("balance_after")
    val balanceAfter: BigDecimal,
    val currency: String,
    val description: String? = null,
    @Column("external_reference")
    val externalReference: String? = null,
    @Column("payment_method")
    val paymentMethod: PaymentMethod? = null,
    val metadata: String? = null,
    @Column("is_accessible_external")
    val isAccessibleExternal: Boolean,
    @Column("created_at")
    val createdAt: LocalDateTime,
    @Column("updated_at")
    val updatedAt: LocalDateTime,
)