package id.co.bni.transactionservice.applications.controllers

import com.ninjasquad.springmockk.MockkBean
import id.co.bni.transactionservice.commons.constants.PaymentMethod
import id.co.bni.transactionservice.commons.constants.TransactionStatus
import id.co.bni.transactionservice.commons.constants.TransactionType
import id.co.bni.transactionservice.commons.exceptions.APIException
import id.co.bni.transactionservice.domains.dtos.TransactionResp
import id.co.bni.transactionservice.domains.entities.HistoricalTransaction
import id.co.bni.transactionservice.domains.services.HistoricalTrxService
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import java.math.BigDecimal
import java.time.LocalDateTime

@WebFluxTest(controllers = [TransactionController::class])
@ActiveProfiles("test")
@TestPropertySource(properties = [
    "spring.r2dbc.url=r2dbc:h2:mem:///testdb",
    "spring.r2dbc.username=sa",
    "spring.r2dbc.password=",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379",
    "server.port=8080"
])
class TransactionControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockkBean
    private lateinit var historicalTrxService: HistoricalTrxService

    // Test data
    private val dummyExternalServiceId = "service-123"
    private val dummyApiKeyId = "key-456"
    private val dummyIpAddress = "192.168.1.1"
    private val dummyUserAgent = "TestAgent/1.0"
    private val dummyTimestamp = LocalDateTime.of(2024, 1, 1, 10, 0, 0)

    private val dummyTransactionResp = TransactionResp(
        id = "txn-123",
        userId = 1L,
        accountId = "acc-123",
        transactionId = "trx-123",
        transactionStatus = TransactionStatus.SUCCESS,
        amount = BigDecimal("100000.00"),
        currency = "IDR",
        createdAt = dummyTimestamp,
        updatedAt = dummyTimestamp
    )

    private val dummyHistoricalTransaction = HistoricalTransaction(
        id = "txn-123",
        userId = 1L,
        accountId = "acc-123",
        transactionId = "trx-123",
        transactionType = TransactionType.PAYMENT,
        transactionStatus = TransactionStatus.SUCCESS,
        amount = BigDecimal("100000.00"),
        balanceBefore = BigDecimal("500000.00"),
        balanceAfter = BigDecimal("400000.00"),
        currency = "IDR",
        description = "Payment for service",
        externalReference = "ext-ref-123",
        paymentMethod = PaymentMethod.GOPAY,
        metadata = "{\"source\": \"mobile\"}",
        isAccessibleExternal = true,
        createdAt = dummyTimestamp,
        updatedAt = dummyTimestamp
    )

    @Test
    fun `getTransactions should return paginated transactions successfully`() = runTest {
        // arrange
        val page = 1
        val size = 10
        val totalCount = 150L
        val transactions = listOf(dummyTransactionResp)

        coEvery { 
            historicalTrxService.getTransactions(
                externalServiceId = dummyExternalServiceId,
                apiKeyId = dummyApiKeyId,
                ipAddress = dummyIpAddress,
                userAgent = dummyUserAgent,
                page = page,
                size = size
            ) 
        } returns transactions
        coEvery { historicalTrxService.getTransactionCount() } returns totalCount

        // act & assert
        webTestClient
            .get()
            .uri("/api/transaction/histories?page=$page&size=$size")
            .header("X-Consumer-Custom-ID", dummyExternalServiceId)
            .header("X-Forwarded-For", dummyIpAddress)
            .header("User-Agent", dummyUserAgent)
            .header("X-API-Key", dummyApiKeyId)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.meta.code").isEqualTo("200")
            .jsonPath("$.meta.message").isEqualTo("transactions retrieved successfully")
            .jsonPath("$.meta.total").isEqualTo(totalCount)
            .jsonPath("$.meta.page").isEqualTo(page)
            .jsonPath("$.meta.size").isEqualTo(size)
            .jsonPath("$.data").isArray
            .jsonPath("$.data.length()").isEqualTo(1)
            .jsonPath("$.data[0].id").isEqualTo("txn-123")
            .jsonPath("$.data[0].user_id").isEqualTo(1)
            .jsonPath("$.data[0].account_id").isEqualTo("acc-123")
            .jsonPath("$.data[0].transaction_id").isEqualTo("trx-123")
            .jsonPath("$.data[0].transaction_status").isEqualTo("SUCCESS")
            .jsonPath("$.data[0].amount").isEqualTo(100000.0)
            .jsonPath("$.data[0].currency").isEqualTo("IDR")

        coVerify(exactly = 1) { 
            historicalTrxService.getTransactions(
                externalServiceId = dummyExternalServiceId,
                apiKeyId = dummyApiKeyId,
                ipAddress = dummyIpAddress,
                userAgent = dummyUserAgent,
                page = page,
                size = size
            ) 
        }
        coVerify(exactly = 1) { historicalTrxService.getTransactionCount() }
    }

    @Test
    fun `getTransactions should use default pagination parameters when not provided`() = runTest {
        // arrange
        val defaultPage = 1
        val defaultSize = 10
        val totalCount = 50L
        val transactions = listOf(dummyTransactionResp)

        coEvery { 
            historicalTrxService.getTransactions(
                externalServiceId = dummyExternalServiceId,
                apiKeyId = dummyApiKeyId,
                ipAddress = dummyIpAddress,
                userAgent = dummyUserAgent,
                page = defaultPage,
                size = defaultSize
            ) 
        } returns transactions
        coEvery { historicalTrxService.getTransactionCount() } returns totalCount

        // act & assert
        webTestClient
            .get()
            .uri("/api/transaction/histories")
            .header("X-Consumer-Custom-ID", dummyExternalServiceId)
            .header("X-Forwarded-For", dummyIpAddress)
            .header("User-Agent", dummyUserAgent)
            .header("X-API-Key", dummyApiKeyId)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.meta.code").isEqualTo("200")
            .jsonPath("$.meta.page").isEqualTo(defaultPage)
            .jsonPath("$.meta.size").isEqualTo(defaultSize)
            .jsonPath("$.meta.total").isEqualTo(totalCount)

        coVerify(exactly = 1) { 
            historicalTrxService.getTransactions(
                externalServiceId = dummyExternalServiceId,
                apiKeyId = dummyApiKeyId,
                ipAddress = dummyIpAddress,
                userAgent = dummyUserAgent,
                page = defaultPage,
                size = defaultSize
            ) 
        }
    }

    @Test
    fun `getTransactions should handle custom pagination parameters`() = runTest {
        // arrange
        val customPage = 5
        val customSize = 25
        val totalCount = 1000L
        val transactions = listOf(dummyTransactionResp)

        coEvery { 
            historicalTrxService.getTransactions(
                externalServiceId = dummyExternalServiceId,
                apiKeyId = dummyApiKeyId,
                ipAddress = dummyIpAddress,
                userAgent = dummyUserAgent,
                page = customPage,
                size = customSize
            ) 
        } returns transactions
        coEvery { historicalTrxService.getTransactionCount() } returns totalCount

        // act & assert
        webTestClient
            .get()
            .uri("/api/transaction/histories?page=$customPage&size=$customSize")
            .header("X-Consumer-Custom-ID", dummyExternalServiceId)
            .header("X-Forwarded-For", dummyIpAddress)
            .header("User-Agent", dummyUserAgent)
            .header("X-API-Key", dummyApiKeyId)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.meta.page").isEqualTo(customPage)
            .jsonPath("$.meta.size").isEqualTo(customSize)
            .jsonPath("$.meta.total").isEqualTo(totalCount)

        coVerify(exactly = 1) { 
            historicalTrxService.getTransactions(
                externalServiceId = dummyExternalServiceId,
                apiKeyId = dummyApiKeyId,
                ipAddress = dummyIpAddress,
                userAgent = dummyUserAgent,
                page = customPage,
                size = customSize
            ) 
        }
    }

    @Test
    fun `getTransactions should return empty list when no transactions found`() = runTest {
        // arrange
        val page = 1
        val size = 10
        val totalCount = 0L
        val emptyTransactions = emptyList<TransactionResp>()

        coEvery { 
            historicalTrxService.getTransactions(
                externalServiceId = dummyExternalServiceId,
                apiKeyId = dummyApiKeyId,
                ipAddress = dummyIpAddress,
                userAgent = dummyUserAgent,
                page = page,
                size = size
            ) 
        } returns emptyTransactions
        coEvery { historicalTrxService.getTransactionCount() } returns totalCount

        // act & assert
        webTestClient
            .get()
            .uri("/api/transaction/histories?page=$page&size=$size")
            .header("X-Consumer-Custom-ID", dummyExternalServiceId)
            .header("X-Forwarded-For", dummyIpAddress)
            .header("User-Agent", dummyUserAgent)
            .header("X-API-Key", dummyApiKeyId)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.meta.code").isEqualTo("200")
            .jsonPath("$.meta.total").isEqualTo(0)
            .jsonPath("$.data").isArray
            .jsonPath("$.data.length()").isEqualTo(0)

        coVerify(exactly = 1) { 
            historicalTrxService.getTransactions(
                externalServiceId = dummyExternalServiceId,
                apiKeyId = dummyApiKeyId,
                ipAddress = dummyIpAddress,
                userAgent = dummyUserAgent,
                page = page,
                size = size
            ) 
        }
        coVerify(exactly = 1) { historicalTrxService.getTransactionCount() }
    }

    @Test
    fun `getTransactions should return 400 for invalid pagination parameters`() = runTest {
        // arrange
        coEvery { 
            historicalTrxService.getTransactions(
                externalServiceId = dummyExternalServiceId,
                apiKeyId = dummyApiKeyId,
                ipAddress = dummyIpAddress,
                userAgent = dummyUserAgent,
                page = 0,
                size = 10
            ) 
        } throws APIException.IllegalParameterException(400, "Page and size must be greater than 0")

        // act & assert
        webTestClient
            .get()
            .uri("/api/transaction/histories?page=0&size=10")
            .header("X-Consumer-Custom-ID", dummyExternalServiceId)
            .header("X-Forwarded-For", dummyIpAddress)
            .header("User-Agent", dummyUserAgent)
            .header("X-API-Key", dummyApiKeyId)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.meta.code").isEqualTo("400")
            .jsonPath("$.meta.message").isEqualTo("Page and size must be greater than 0")

        coVerify(exactly = 1) { 
            historicalTrxService.getTransactions(
                externalServiceId = dummyExternalServiceId,
                apiKeyId = dummyApiKeyId,
                ipAddress = dummyIpAddress,
                userAgent = dummyUserAgent,
                page = 0,
                size = 10
            ) 
        }
        coVerify(exactly = 0) { historicalTrxService.getTransactionCount() }
    }

    @Test
    fun `getTransactions should return 400 for missing required headers`() = runTest {
        // act & assert - missing X-Consumer-Custom-ID header
        webTestClient
            .get()
            .uri("/api/transaction/histories")
            .header("X-Forwarded-For", dummyIpAddress)
            .header("User-Agent", dummyUserAgent)
            .header("X-API-Key", dummyApiKeyId)
            .exchange()
            .expectStatus().isBadRequest

        // act & assert - missing X-API-Key header
        webTestClient
            .get()
            .uri("/api/transaction/histories")
            .header("X-Consumer-Custom-ID", dummyExternalServiceId)
            .header("X-Forwarded-For", dummyIpAddress)
            .header("User-Agent", dummyUserAgent)
            .exchange()
            .expectStatus().isBadRequest

        // act & assert - missing X-Forwarded-For header
        webTestClient
            .get()
            .uri("/api/transaction/histories")
            .header("X-Consumer-Custom-ID", dummyExternalServiceId)
            .header("User-Agent", dummyUserAgent)
            .header("X-API-Key", dummyApiKeyId)
            .exchange()
            .expectStatus().isBadRequest

        // act & assert - missing User-Agent header
        webTestClient
            .get()
            .uri("/api/transaction/histories")
            .header("X-Consumer-Custom-ID", dummyExternalServiceId)
            .header("X-Forwarded-For", dummyIpAddress)
            .header("X-API-Key", dummyApiKeyId)
            .exchange()
            .expectStatus().isBadRequest

        coVerify(exactly = 0) { historicalTrxService.getTransactions(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `getTransactionById should return transaction successfully`() = runTest {
        // arrange
        val transactionId = "txn-123"

        coEvery { 
            historicalTrxService.getTransactionById(
                id = transactionId,
                externalServiceId = dummyExternalServiceId,
                apiKeyId = dummyApiKeyId,
                ipAddress = dummyIpAddress,
                userAgent = dummyUserAgent,
                transactionId = transactionId
            ) 
        } returns dummyHistoricalTransaction

        // act & assert
        webTestClient
            .get()
            .uri("/api/transaction/histories/$transactionId")
            .header("X-Consumer-Custom-ID", dummyExternalServiceId)
            .header("X-Forwarded-For", dummyIpAddress)
            .header("User-Agent", dummyUserAgent)
            .header("X-API-Key", dummyApiKeyId)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.meta.code").isEqualTo("200")
            .jsonPath("$.meta.message").isEqualTo("transaction retrieved successfully")
            .jsonPath("$.data.id").isEqualTo("txn-123")
            .jsonPath("$.data.user_id").isEqualTo(1)
            .jsonPath("$.data.account_id").isEqualTo("acc-123")
            .jsonPath("$.data.transaction_id").isEqualTo("trx-123")
            .jsonPath("$.data.transaction_type").isEqualTo("PAYMENT")
            .jsonPath("$.data.transaction_status").isEqualTo("SUCCESS")
            .jsonPath("$.data.amount").isEqualTo(100000.0)
            .jsonPath("$.data.balance_before").isEqualTo(500000.0)
            .jsonPath("$.data.balance_after").isEqualTo(400000.0)
            .jsonPath("$.data.currency").isEqualTo("IDR")
            .jsonPath("$.data.description").isEqualTo("Payment for service")
            .jsonPath("$.data.external_reference").isEqualTo("ext-ref-123")
            .jsonPath("$.data.payment_method").isEqualTo("GOPAY")
            .jsonPath("$.data.metadata").isEqualTo("{\"source\": \"mobile\"}")
            .jsonPath("$.data.is_accessible_external").isEqualTo(true)

        coVerify(exactly = 1) { 
            historicalTrxService.getTransactionById(
                id = transactionId,
                externalServiceId = dummyExternalServiceId,
                apiKeyId = dummyApiKeyId,
                ipAddress = dummyIpAddress,
                userAgent = dummyUserAgent,
                transactionId = transactionId
            ) 
        }
    }

    @Test
    fun `getTransactionById should return 404 when transaction not found`() = runTest {
        // arrange
        val nonExistentId = "nonexistent-txn"

        coEvery { 
            historicalTrxService.getTransactionById(
                id = nonExistentId,
                externalServiceId = dummyExternalServiceId,
                apiKeyId = dummyApiKeyId,
                ipAddress = dummyIpAddress,
                userAgent = dummyUserAgent,
                transactionId = nonExistentId
            ) 
        } throws APIException.NotFoundResourceException(404, "Transaction with id $nonExistentId not found")

        // act & assert
        webTestClient
            .get()
            .uri("/api/transaction/histories/$nonExistentId")
            .header("X-Consumer-Custom-ID", dummyExternalServiceId)
            .header("X-Forwarded-For", dummyIpAddress)
            .header("User-Agent", dummyUserAgent)
            .header("X-API-Key", dummyApiKeyId)
            .exchange()
            .expectStatus().isNotFound
            .expectBody()
            .jsonPath("$.meta.code").isEqualTo("404")
            .jsonPath("$.meta.message").isEqualTo("Transaction with id $nonExistentId not found")

        coVerify(exactly = 1) { 
            historicalTrxService.getTransactionById(
                id = nonExistentId,
                externalServiceId = dummyExternalServiceId,
                apiKeyId = dummyApiKeyId,
                ipAddress = dummyIpAddress,
                userAgent = dummyUserAgent,
                transactionId = nonExistentId
            ) 
        }
    }

    @Test
    fun `getTransactionById should return 400 for missing required headers`() = runTest {
        // arrange
        val transactionId = "txn-123"

        // act & assert - missing X-Consumer-Custom-ID header
        webTestClient
            .get()
            .uri("/api/transaction/histories/$transactionId")
            .header("X-Forwarded-For", dummyIpAddress)
            .header("User-Agent", dummyUserAgent)
            .header("X-API-Key", dummyApiKeyId)
            .exchange()
            .expectStatus().isBadRequest

        // act & assert - missing X-API-Key header
        webTestClient
            .get()
            .uri("/api/transaction/histories/$transactionId")
            .header("X-Consumer-Custom-ID", dummyExternalServiceId)
            .header("X-Forwarded-For", dummyIpAddress)
            .header("User-Agent", dummyUserAgent)
            .exchange()
            .expectStatus().isBadRequest

        coVerify(exactly = 0) { historicalTrxService.getTransactionById(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `getTransactions should handle multiple transactions with different data`() = runTest {
        // arrange
        val transaction1 = TransactionResp(
            id = "txn-001",
            userId = 1L,
            accountId = "acc-001",
            transactionId = "trx-001",
            transactionStatus = TransactionStatus.SUCCESS,
            amount = BigDecimal("50000.00"),
            currency = "IDR",
            createdAt = dummyTimestamp,
            updatedAt = dummyTimestamp
        )
        val transaction2 = TransactionResp(
            id = "txn-002",
            userId = 2L,
            accountId = "acc-002",
            transactionId = "trx-002",
            transactionStatus = TransactionStatus.PENDING,
            amount = BigDecimal("75000.00"),
            currency = "IDR",
            createdAt = dummyTimestamp,
            updatedAt = dummyTimestamp
        )
        val transaction3 = TransactionResp(
            id = "txn-003",
            userId = 3L,
            accountId = "acc-003",
            transactionId = "trx-003",
            transactionStatus = TransactionStatus.FAILED,
            amount = BigDecimal("25000.00"),
            currency = "IDR",
            createdAt = dummyTimestamp,
            updatedAt = dummyTimestamp
        )
        val transactions = listOf(transaction1, transaction2, transaction3)
        val totalCount = 150L

        coEvery { 
            historicalTrxService.getTransactions(
                externalServiceId = dummyExternalServiceId,
                apiKeyId = dummyApiKeyId,
                ipAddress = dummyIpAddress,
                userAgent = dummyUserAgent,
                page = 1,
                size = 10
            ) 
        } returns transactions
        coEvery { historicalTrxService.getTransactionCount() } returns totalCount

        // act & assert
        webTestClient
            .get()
            .uri("/api/transaction/histories")
            .header("X-Consumer-Custom-ID", dummyExternalServiceId)
            .header("X-Forwarded-For", dummyIpAddress)
            .header("User-Agent", dummyUserAgent)
            .header("X-API-Key", dummyApiKeyId)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.data").isArray
            .jsonPath("$.data.length()").isEqualTo(3)
            .jsonPath("$.data[0].id").isEqualTo("txn-001")
            .jsonPath("$.data[0].user_id").isEqualTo(1)
            .jsonPath("$.data[0].transaction_status").isEqualTo("SUCCESS")
            .jsonPath("$.data[0].amount").isEqualTo(50000.0)
            .jsonPath("$.data[1].id").isEqualTo("txn-002")
            .jsonPath("$.data[1].user_id").isEqualTo(2)
            .jsonPath("$.data[1].transaction_status").isEqualTo("PENDING")
            .jsonPath("$.data[1].amount").isEqualTo(75000.0)
            .jsonPath("$.data[2].id").isEqualTo("txn-003")
            .jsonPath("$.data[2].user_id").isEqualTo(3)
            .jsonPath("$.data[2].transaction_status").isEqualTo("FAILED")
            .jsonPath("$.data[2].amount").isEqualTo(25000.0)

        coVerify(exactly = 1) { 
            historicalTrxService.getTransactions(
                externalServiceId = dummyExternalServiceId,
                apiKeyId = dummyApiKeyId,
                ipAddress = dummyIpAddress,
                userAgent = dummyUserAgent,
                page = 1,
                size = 10
            ) 
        }
        coVerify(exactly = 1) { historicalTrxService.getTransactionCount() }
    }

    @Test
    fun `getTransactionById should handle transaction with null optional fields`() = runTest {
        // arrange
        val transactionId = "txn-minimal"
        val minimalTransaction = dummyHistoricalTransaction.copy(
            id = transactionId,
            description = null,
            externalReference = null,
            paymentMethod = null,
            metadata = null,
            isAccessibleExternal = false
        )

        coEvery { 
            historicalTrxService.getTransactionById(
                id = transactionId,
                externalServiceId = dummyExternalServiceId,
                apiKeyId = dummyApiKeyId,
                ipAddress = dummyIpAddress,
                userAgent = dummyUserAgent,
                transactionId = transactionId
            ) 
        } returns minimalTransaction

        // act & assert
        webTestClient
            .get()
            .uri("/api/transaction/histories/$transactionId")
            .header("X-Consumer-Custom-ID", dummyExternalServiceId)
            .header("X-Forwarded-For", dummyIpAddress)
            .header("User-Agent", dummyUserAgent)
            .header("X-API-Key", dummyApiKeyId)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.data.id").isEqualTo(transactionId)
            .jsonPath("$.data.description").isEmpty
            .jsonPath("$.data.external_reference").isEmpty
            .jsonPath("$.data.payment_method").isEmpty
            .jsonPath("$.data.metadata").isEmpty
            .jsonPath("$.data.is_accessible_external").isEqualTo(false)

        coVerify(exactly = 1) { 
            historicalTrxService.getTransactionById(
                id = transactionId,
                externalServiceId = dummyExternalServiceId,
                apiKeyId = dummyApiKeyId,
                ipAddress = dummyIpAddress,
                userAgent = dummyUserAgent,
                transactionId = transactionId
            ) 
        }
    }

    @Test
    fun `getTransactions should handle large page numbers gracefully`() = runTest {
        // arrange
        val largePage = 9999
        val size = 10
        val totalCount = 50L
        val emptyTransactions = emptyList<TransactionResp>()

        coEvery { 
            historicalTrxService.getTransactions(
                externalServiceId = dummyExternalServiceId,
                apiKeyId = dummyApiKeyId,
                ipAddress = dummyIpAddress,
                userAgent = dummyUserAgent,
                page = largePage,
                size = size
            ) 
        } returns emptyTransactions
        coEvery { historicalTrxService.getTransactionCount() } returns totalCount

        // act & assert
        webTestClient
            .get()
            .uri("/api/transaction/histories?page=$largePage&size=$size")
            .header("X-Consumer-Custom-ID", dummyExternalServiceId)
            .header("X-Forwarded-For", dummyIpAddress)
            .header("User-Agent", dummyUserAgent)
            .header("X-API-Key", dummyApiKeyId)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.meta.page").isEqualTo(largePage)
            .jsonPath("$.meta.size").isEqualTo(size)
            .jsonPath("$.meta.total").isEqualTo(totalCount)
            .jsonPath("$.data").isArray
            .jsonPath("$.data.length()").isEqualTo(0)

        coVerify(exactly = 1) { 
            historicalTrxService.getTransactions(
                externalServiceId = dummyExternalServiceId,
                apiKeyId = dummyApiKeyId,
                ipAddress = dummyIpAddress,
                userAgent = dummyUserAgent,
                page = largePage,
                size = size
            ) 
        }
    }

    @Test
    fun `getTransactions should handle different transaction statuses`() = runTest {
        // arrange
        val successTransaction = dummyTransactionResp.copy(
            id = "txn-success",
            transactionStatus = TransactionStatus.SUCCESS
        )
        val pendingTransaction = dummyTransactionResp.copy(
            id = "txn-pending",
            transactionStatus = TransactionStatus.PENDING
        )
        val failedTransaction = dummyTransactionResp.copy(
            id = "txn-failed",
            transactionStatus = TransactionStatus.FAILED
        )
        val cancelledTransaction = dummyTransactionResp.copy(
            id = "txn-cancelled",
            transactionStatus = TransactionStatus.CANCELLED
        )
        val transactions = listOf(successTransaction, pendingTransaction, failedTransaction, cancelledTransaction)

        coEvery { 
            historicalTrxService.getTransactions(
                externalServiceId = dummyExternalServiceId,
                apiKeyId = dummyApiKeyId,
                ipAddress = dummyIpAddress,
                userAgent = dummyUserAgent,
                page = 1,
                size = 10
            ) 
        } returns transactions
        coEvery { historicalTrxService.getTransactionCount() } returns 4L

        // act & assert
        webTestClient
            .get()
            .uri("/api/transaction/histories")
            .header("X-Consumer-Custom-ID", dummyExternalServiceId)
            .header("X-Forwarded-For", dummyIpAddress)
            .header("User-Agent", dummyUserAgent)
            .header("X-API-Key", dummyApiKeyId)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.data").isArray
            .jsonPath("$.data.length()").isEqualTo(4)
            .jsonPath("$.data[0].transaction_status").isEqualTo("SUCCESS")
            .jsonPath("$.data[1].transaction_status").isEqualTo("PENDING")
            .jsonPath("$.data[2].transaction_status").isEqualTo("FAILED")
            .jsonPath("$.data[3].transaction_status").isEqualTo("CANCELLED")

        coVerify(exactly = 1) { 
            historicalTrxService.getTransactions(
                externalServiceId = dummyExternalServiceId,
                apiKeyId = dummyApiKeyId,
                ipAddress = dummyIpAddress,
                userAgent = dummyUserAgent,
                page = 1,
                size = 10
            ) 
        }
    }

    @Test
    fun `endpoints should handle special characters in headers gracefully`() = runTest {
        // arrange
        val specialExternalServiceId = "service-123_special-chars.test"
        val specialApiKeyId = "key-456@domain.com"
        val specialUserAgent = "Mozilla/5.0 (special; test) Agent/1.0"
        val transactions = listOf(dummyTransactionResp)

        coEvery { 
            historicalTrxService.getTransactions(
                externalServiceId = specialExternalServiceId,
                apiKeyId = specialApiKeyId,
                ipAddress = dummyIpAddress,
                userAgent = specialUserAgent,
                page = 1,
                size = 10
            ) 
        } returns transactions
        coEvery { historicalTrxService.getTransactionCount() } returns 1L

        // act & assert
        webTestClient
            .get()
            .uri("/api/transaction/histories")
            .header("X-Consumer-Custom-ID", specialExternalServiceId)
            .header("X-Forwarded-For", dummyIpAddress)
            .header("User-Agent", specialUserAgent)
            .header("X-API-Key", specialApiKeyId)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.meta.code").isEqualTo("200")

        coVerify(exactly = 1) { 
            historicalTrxService.getTransactions(
                externalServiceId = specialExternalServiceId,
                apiKeyId = specialApiKeyId,
                ipAddress = dummyIpAddress,
                userAgent = specialUserAgent,
                page = 1,
                size = 10
            ) 
        }
    }

    @Test
    fun `getTransactions should handle IPv6 addresses`() = runTest {
        // arrange
        val ipv6Address = "2001:0db8:85a3:0000:0000:8a2e:0370:7334"
        val transactions = listOf(dummyTransactionResp)

        coEvery { 
            historicalTrxService.getTransactions(
                externalServiceId = dummyExternalServiceId,
                apiKeyId = dummyApiKeyId,
                ipAddress = ipv6Address,
                userAgent = dummyUserAgent,
                page = 1,
                size = 10
            ) 
        } returns transactions
        coEvery { historicalTrxService.getTransactionCount() } returns 1L

        // act & assert
        webTestClient
            .get()
            .uri("/api/transaction/histories")
            .header("X-Consumer-Custom-ID", dummyExternalServiceId)
            .header("X-Forwarded-For", ipv6Address)
            .header("User-Agent", dummyUserAgent)
            .header("X-API-Key", dummyApiKeyId)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.meta.code").isEqualTo("200")

        coVerify(exactly = 1) { 
            historicalTrxService.getTransactions(
                externalServiceId = dummyExternalServiceId,
                apiKeyId = dummyApiKeyId,
                ipAddress = ipv6Address,
                userAgent = dummyUserAgent,
                page = 1,
                size = 10
            ) 
        }
    }
}