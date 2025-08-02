package id.co.bni.transactionservice.infrastructures.repositories

import id.co.bni.transactionservice.domains.entities.HistoricalTransaction
import id.co.bni.transactionservice.domains.repositories.HistoricalTrxRepository
import id.co.bni.transactionservice.infrastructures.repositories.dao.HistoricalTransactionDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Repository

@Repository
class HistoricalTrxRepositoryImpl(
    private val historicalTrxDao: HistoricalTransactionDao
) : HistoricalTrxRepository {
    override suspend fun getTransactions(
        limit: Int,
        offset: Int
    ): Flow<HistoricalTransaction> =
        historicalTrxDao.findAllPaginated(limit, offset).flowOn(Dispatchers.IO)

    override suspend fun getTransactionById(transactionId: String): HistoricalTransaction? =
        withContext(Dispatchers.IO) {
            historicalTrxDao.findById(transactionId)
        }

    override suspend fun getTransactionCount(): Long = withContext(Dispatchers.IO) {
        historicalTrxDao.count()
    }
}