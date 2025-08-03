package id.co.bni.transactionservice.applications.controllers

import id.co.bni.transactionservice.applications.controllers.dtos.MetaResponse
import id.co.bni.transactionservice.applications.controllers.dtos.PaginatedMetaResponse
import id.co.bni.transactionservice.applications.controllers.dtos.WebResponse
import id.co.bni.transactionservice.commons.loggable.Loggable
import id.co.bni.transactionservice.domains.dtos.TransactionResp
import id.co.bni.transactionservice.domains.entities.HistoricalTransaction
import id.co.bni.transactionservice.domains.services.HistoricalTrxService
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/transaction")
class TransactionController(
    private val transactionService: HistoricalTrxService
) : Loggable {
    @GetMapping("/histories", produces = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun getTransactions(
        @RequestParam(value = "page", defaultValue = "1") page: Int,
        @RequestParam(value = "size", defaultValue = "10") size: Int,
        @RequestHeader("X-Consumer-Custom-ID") externalServiceId: String,
        @RequestHeader("X-Forwarded-For") ipAddress: String,
        @RequestHeader("User-Agent") userAgent: String,
        @RequestHeader(value = "X-API-Key") apiKey: String,
    ): WebResponse<List<TransactionResp>, PaginatedMetaResponse> {
        val traceId = UUID.randomUUID().toString()

        return withContext(
            MDCContext(mapOf("traceId" to traceId))
        ) {
            log.info("getting transactions for external service: {}-{}", externalServiceId, apiKey)

            val transactions = transactionService.getTransactions(
                externalServiceId = externalServiceId,
                apiKeyId = apiKey,
                ipAddress = ipAddress,
                userAgent = userAgent,
                page = page,
                size = size
            )
            val total = transactionService.getTransactionCount()
            WebResponse(
                meta = PaginatedMetaResponse(
                    code = HttpStatus.OK.value().toString(),
                    message = "transactions retrieved successfully",
                    total = total,
                    page = page,
                    size = size
                ),
                data = transactions
            )
        }
    }

    @GetMapping("/histories/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun getTransactionById(
        @PathVariable id: String,
        @RequestHeader("X-Consumer-Custom-ID") externalServiceId: String,
        @RequestHeader("X-Forwarded-For") ipAddress: String,
        @RequestHeader("User-Agent") userAgent: String,
        @RequestParam(value = "X-API-Key") apiKey: String,
    ): WebResponse<HistoricalTransaction, MetaResponse> {
        val traceId = UUID.randomUUID().toString()

        return withContext(
            MDCContext(mapOf("traceId" to traceId))
        ) {
            log.info("getting transaction by id and external service id: {} {}-{}", id, externalServiceId, apiKey)

            val transaction = transactionService.getTransactionById(
                id = id,
                externalServiceId = externalServiceId,
                apiKeyId = apiKey,
                ipAddress = ipAddress,
                userAgent = userAgent,
                transactionId = id
            )

            WebResponse(
                meta = MetaResponse(
                    code = HttpStatus.OK.value().toString(),
                    message = "transaction retrieved successfully"
                ),
                data = transaction
            )
        }
    }

}