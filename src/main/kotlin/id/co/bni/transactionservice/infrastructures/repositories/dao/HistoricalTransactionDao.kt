package id.co.bni.transactionservice.infrastructures.repositories.dao

import id.co.bni.transactionservice.domains.entities.HistoricalTransaction
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Component

@Component
interface HistoricalTransactionDao : CoroutineCrudRepository<HistoricalTransaction, String> {
    @Query(
        """
        SELECT * FROM historical_transactions ORDER BY created_at DESC 
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun findAllPaginated(
        limit: Int,
        offset: Int
    ): Flow<HistoricalTransaction>
}