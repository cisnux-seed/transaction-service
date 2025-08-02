package id.co.bni.transactionservice.domains.repositories

import id.co.bni.transactionservice.domains.entities.HistoricalTransaction
import kotlinx.coroutines.flow.Flow

interface HistoricalTrxRepository {
    suspend fun getTransactions(limit: Int, offset: Int): Flow<HistoricalTransaction>
    suspend fun getTransactionById(transactionId: String): HistoricalTransaction?
    suspend fun getTransactionCount(): Long
}