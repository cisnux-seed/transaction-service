package id.co.bni.transactionservice.domains.services

import id.co.bni.transactionservice.commons.constants.CacheKeys
import id.co.bni.transactionservice.commons.constants.HttpMethod
import id.co.bni.transactionservice.commons.exceptions.APIException
import id.co.bni.transactionservice.domains.dtos.TransactionResp
import id.co.bni.transactionservice.domains.entities.ApiAccessLog
import id.co.bni.transactionservice.domains.entities.HistoricalTransaction
import id.co.bni.transactionservice.domains.repositories.ApiLogRepository
import id.co.bni.transactionservice.domains.repositories.HistoricalTrxRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class HistoricalTrxServiceImpl(
    private val historicalTrxRepository: HistoricalTrxRepository,
    private val apiLogRepository: ApiLogRepository,
    private val cacheService: CacheService
) : HistoricalTrxService {
    override suspend fun getTransactions(
        externalServiceId: String,
        apiKeyId: String,
        ipAddress: String,
        userAgent: String,
        page: Int,
        size: Int
    ): List<TransactionResp> {
        if(page <= 0 || size <= 0) {
            throw APIException.IllegalParameterException(
                message = "Page and size must be greater than 0",
                statusCode = HttpStatus.BAD_REQUEST.value()
            )
        }

        val cacheKey = CacheKeys.transactionListKey(page, size)
        var transactions = cacheService.get(cacheKey, List::class.java) as? List<TransactionResp>

        if (transactions == null) {
            val limit = page * size
            val offset = (page - 1) * size

            transactions = historicalTrxRepository.getTransactions(limit, offset)
                .map {
                    TransactionResp(
                        transactionId = it.transactionId,
                        amount = it.amount,
                        currency = it.currency,
                        transactionStatus = it.transactionStatus,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt,
                        id = it.id,
                        userId = it.userId,
                        accountId = it.accountId
                    )
                }.toList()

            cacheService.set(cacheKey, transactions, 15)
        }

        val apiLog = ApiAccessLog(
            externalServiceId = externalServiceId,
            apiKeyId = apiKeyId,
            endpoint = "/api/transactions",
            httpMethod = HttpMethod.GET,
            ipAddress = ipAddress,
            userAgent = userAgent,
            responseStatus = HttpStatus.OK.value()
        )
        apiLogRepository.insertApiLog(apiLog)

        return transactions
    }

    override suspend fun getTransactionById(
        id: String,
        externalServiceId: String,
        apiKeyId: String,
        ipAddress: String,
        userAgent: String,
        transactionId: String
    ): HistoricalTransaction {
        val cacheKey = CacheKeys.transactionKey(id)
        var transaction = cacheService.get(cacheKey, HistoricalTransaction::class.java)

        if (transaction == null) {
            transaction = historicalTrxRepository.getTransactionById(id)
                ?: throw APIException.NotFoundResourceException(
                    message = "Transaction with id $id not found",
                    statusCode = HttpStatus.NOT_FOUND.value()
                )

            cacheService.set(cacheKey, transaction, 30)
        }

        // Log API access
        val apiLog = ApiAccessLog(
            externalServiceId = externalServiceId,
            apiKeyId = apiKeyId,
            endpoint = "/api/transactions/$transactionId",
            httpMethod = HttpMethod.GET,
            ipAddress = ipAddress,
            userAgent = userAgent,
            responseStatus = HttpStatus.OK.value()
        )
        apiLogRepository.insertApiLog(apiLog)

        return transaction
    }

    override suspend fun getTransactionCount(): Long {
        var count = cacheService.get(CacheKeys.TRANSACTION_COUNT, Long::class.java)

        if (count == null) {
            count = historicalTrxRepository.getTransactionCount()
            cacheService.set(CacheKeys.TRANSACTION_COUNT, count, 5)
        }

        return count
    }

}