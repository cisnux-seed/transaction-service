package id.co.bni.transactionservice.domains.services

import id.co.bni.transactionservice.domains.dtos.TransactionResp
import id.co.bni.transactionservice.domains.entities.HistoricalTransaction

interface HistoricalTrxService {
    suspend fun getTransactions(
        externalServiceId: String,
        apiKeyId: String,
        ipAddress: String,
        userAgent: String,
        page: Int,
        size: Int
    ): List<TransactionResp>

    suspend fun getTransactionById(
        id: String,
        externalServiceId: String,
        apiKeyId: String,
        ipAddress: String,
        userAgent: String,
        transactionId: String
    ): HistoricalTransaction

    suspend fun getTransactionCount(
    ): Long
}