package id.co.bni.transactionservice.domains.services

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
        val limit = page * size
        val offset = (page - 1) * size

        val trx = historicalTrxRepository.getTransactions(
            limit = limit,
            offset = offset
        ).map {
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

        return trx
    }

    override suspend fun getTransactionById(
        id: String,
        externalServiceId: String,
        apiKeyId: String,
        ipAddress: String,
        userAgent: String,
        transactionId: String
    ): HistoricalTransaction {
        val trx = historicalTrxRepository.getTransactionById(id)

        return trx?.let {
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
            it
        } ?: throw APIException.NotFoundResourceException(
            message = "Transaction with id $id not found",
            statusCode = HttpStatus.NOT_FOUND.value()
        )
    }

    override suspend fun getTransactionCount(
    ): Long = historicalTrxRepository.getTransactionCount()

}