package id.co.bni.transactionservice.domains.services

import id.co.bni.transactionservice.commons.constants.CacheKeys
import id.co.bni.transactionservice.commons.constants.HttpMethod
import id.co.bni.transactionservice.commons.constants.PaymentMethod
import id.co.bni.transactionservice.commons.constants.TransactionStatus
import id.co.bni.transactionservice.commons.constants.TransactionType
import id.co.bni.transactionservice.commons.exceptions.APIException
import id.co.bni.transactionservice.domains.dtos.TransactionResp
import id.co.bni.transactionservice.domains.entities.ApiAccessLog
import id.co.bni.transactionservice.domains.entities.HistoricalTransaction
import id.co.bni.transactionservice.domains.repositories.ApiLogRepository
import id.co.bni.transactionservice.domains.repositories.HistoricalTrxRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDateTime

@ExtendWith(MockKExtension::class)
class HistoricalTrxServiceTest {

    @MockK
    private lateinit var historicalTrxRepository: HistoricalTrxRepository

    @MockK
    private lateinit var apiLogRepository: ApiLogRepository

    @MockK
    private lateinit var cacheService: CacheService

    @InjectMockKs
    private lateinit var historicalTrxService: HistoricalTrxServiceImpl

    // Test data
    private val dummyExternalServiceId = "service-123"
    private val dummyApiKeyId = "key-456"
    private val dummyIpAddress = "192.168.1.1"
    private val dummyUserAgent = "TestAgent/1.0"
    private val dummyTimestamp = LocalDateTime.of(2024, 1, 1, 10, 0, 0)

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

    @Test
    fun `getTransactions with cache hit should return cached response and log API access`() = runTest {
        // arrange
        val page = 1
        val size = 10
        val cacheKey = CacheKeys.transactionListKey(page, size)
        val cachedTransactions = listOf(dummyTransactionResp)
        val apiLogSlot = slot<ApiAccessLog>()

        coEvery { cacheService.get(cacheKey, List::class.java) } returns cachedTransactions
        coEvery { apiLogRepository.insertApiLog(capture(apiLogSlot)) } returns ApiAccessLog(
            externalServiceId = dummyExternalServiceId,
            apiKeyId = dummyApiKeyId,
            endpoint = "/api/transactions",
            httpMethod = HttpMethod.GET,
            ipAddress = dummyIpAddress,
            userAgent = dummyUserAgent,
            responseStatus = 200
        )

        // act
        val result = historicalTrxService.getTransactions(
            externalServiceId = dummyExternalServiceId,
            apiKeyId = dummyApiKeyId,
            ipAddress = dummyIpAddress,
            userAgent = dummyUserAgent,
            page = page,
            size = size
        )

        // assert
        assertEquals(1, result.size)
        assertEquals("txn-123", result[0].id)
        assertEquals(1L, result[0].userId)
        assertEquals("acc-123", result[0].accountId)

        // Verify API log was created
        val capturedApiLog = apiLogSlot.captured
        assertEquals(dummyExternalServiceId, capturedApiLog.externalServiceId)
        assertEquals(dummyApiKeyId, capturedApiLog.apiKeyId)
        assertEquals("/api/transactions", capturedApiLog.endpoint)
        assertEquals(HttpMethod.GET, capturedApiLog.httpMethod)
        assertEquals(dummyIpAddress, capturedApiLog.ipAddress)
        assertEquals(dummyUserAgent, capturedApiLog.userAgent)
        assertEquals(200, capturedApiLog.responseStatus)

        coVerify(exactly = 1) { cacheService.get(cacheKey, List::class.java) }
        coVerify(exactly = 0) { historicalTrxRepository.getTransactions(any(), any()) }
        coVerify(exactly = 0) { cacheService.set(any(), any(), any()) }
        coVerify(exactly = 1) { apiLogRepository.insertApiLog(any()) }
    }

    @Test
    fun `getTransactions with cache miss should fetch from repository, cache result, and log API access`() = runTest {
        // arrange
        val page = 1
        val size = 10
        val limit = page * size
        val offset = (page - 1) * size
        val cacheKey = CacheKeys.transactionListKey(page, size)
        val transactionFlow = flowOf(dummyHistoricalTransaction)
        val transactionListSlot = slot<List<TransactionResp>>()
        val apiLogSlot = slot<ApiAccessLog>()

        coEvery { cacheService.get(cacheKey, List::class.java) } returns null
        coEvery { historicalTrxRepository.getTransactions(limit, offset) } returns transactionFlow
        coEvery { cacheService.set(cacheKey, capture(transactionListSlot), 15) } returns Unit
        coEvery { apiLogRepository.insertApiLog(capture(apiLogSlot)) } returns ApiAccessLog(
            externalServiceId = dummyExternalServiceId,
            apiKeyId = dummyApiKeyId,
            endpoint = "/api/transactions",
            httpMethod = HttpMethod.GET,
            ipAddress = dummyIpAddress,
            userAgent = dummyUserAgent,
            responseStatus = 200
        )

        // act
        val result = historicalTrxService.getTransactions(
            externalServiceId = dummyExternalServiceId,
            apiKeyId = dummyApiKeyId,
            ipAddress = dummyIpAddress,
            userAgent = dummyUserAgent,
            page = page,
            size = size
        )

        // assert
        assertEquals(1, result.size)
        assertEquals("txn-123", result[0].id)
        assertEquals("trx-123", result[0].transactionId)
        assertEquals(BigDecimal("100000.00"), result[0].amount)
        assertEquals("IDR", result[0].currency)
        assertEquals(TransactionStatus.SUCCESS, result[0].transactionStatus)
        assertEquals(dummyTimestamp, result[0].createdAt)
        assertEquals(dummyTimestamp, result[0].updatedAt)

        // Verify cached value
        val cachedTransactions = transactionListSlot.captured
        assertEquals(1, cachedTransactions.size)
        assertEquals("txn-123", cachedTransactions[0].id)

        // Verify API log
        val capturedApiLog = apiLogSlot.captured
        assertEquals(dummyExternalServiceId, capturedApiLog.externalServiceId)
        assertEquals(dummyApiKeyId, capturedApiLog.apiKeyId)

        coVerify(exactly = 1) { cacheService.get(cacheKey, List::class.java) }
        coVerify(exactly = 1) { historicalTrxRepository.getTransactions(limit, offset) }
        coVerify(exactly = 1) { cacheService.set(cacheKey, any(), 15) }
        coVerify(exactly = 1) { apiLogRepository.insertApiLog(any()) }
    }

    @Test
    fun `getTransactions with invalid page should throw IllegalParameterException`() = runTest {
        // arrange
        val invalidPage = 0
        val size = 10

        // act & assert
        val exception = assertThrows(APIException.IllegalParameterException::class.java) {
            runBlocking {
                historicalTrxService.getTransactions(
                    externalServiceId = dummyExternalServiceId,
                    apiKeyId = dummyApiKeyId,
                    ipAddress = dummyIpAddress,
                    userAgent = dummyUserAgent,
                    page = invalidPage,
                    size = size
                )
            }
        }

        assertEquals("Page and size must be greater than 0", exception.message)
        assertEquals(400, exception.statusCode)

        coVerify(exactly = 0) { historicalTrxRepository.getTransactions(any(), any()) }
        coVerify(exactly = 0) { apiLogRepository.insertApiLog(any()) }
    }

    @Test
    fun `getTransactions with invalid size should throw IllegalParameterException`() = runTest {
        // arrange
        val page = 1
        val invalidSize = -5

        // act & assert
        val exception = assertThrows(APIException.IllegalParameterException::class.java) {
            runBlocking {
                historicalTrxService.getTransactions(
                    externalServiceId = dummyExternalServiceId,
                    apiKeyId = dummyApiKeyId,
                    ipAddress = dummyIpAddress,
                    userAgent = dummyUserAgent,
                    page = page,
                    size = invalidSize
                )
            }
        }

        assertEquals("Page and size must be greater than 0", exception.message)
        assertEquals(400, exception.statusCode)

        coVerify(exactly = 0) { historicalTrxRepository.getTransactions(any(), any()) }
        coVerify(exactly = 0) { apiLogRepository.insertApiLog(any()) }
    }

    @Test
    fun `getTransactions with negative page should throw IllegalParameterException`() = runTest {
        // arrange
        val invalidPage = -1
        val size = 10

        // act & assert
        val exception = assertThrows(APIException.IllegalParameterException::class.java) {
            runBlocking {
                historicalTrxService.getTransactions(
                    externalServiceId = dummyExternalServiceId,
                    apiKeyId = dummyApiKeyId,
                    ipAddress = dummyIpAddress,
                    userAgent = dummyUserAgent,
                    page = invalidPage,
                    size = size
                )
            }
        }

        assertEquals("Page and size must be greater than 0", exception.message)
        assertEquals(400, exception.statusCode)
    }

    @Test
    fun `getTransactionById with cache hit should return cached transaction and log API access`() = runTest {
        // arrange
        val transactionId = "txn-123"
        val cacheKey = CacheKeys.transactionKey(transactionId)
        val apiLogSlot = slot<ApiAccessLog>()

        coEvery { cacheService.get(cacheKey, HistoricalTransaction::class.java) } returns dummyHistoricalTransaction
        coEvery { apiLogRepository.insertApiLog(capture(apiLogSlot)) } returns ApiAccessLog(
            externalServiceId = dummyExternalServiceId,
            apiKeyId = dummyApiKeyId,
            endpoint = "/api/transactions/$transactionId",
            httpMethod = HttpMethod.GET,
            ipAddress = dummyIpAddress,
            userAgent = dummyUserAgent,
            responseStatus = 200
        )

        // act
        val result = historicalTrxService.getTransactionById(
            id = transactionId,
            externalServiceId = dummyExternalServiceId,
            apiKeyId = dummyApiKeyId,
            ipAddress = dummyIpAddress,
            userAgent = dummyUserAgent,
            transactionId = transactionId
        )

        // assert
        assertEquals(dummyHistoricalTransaction, result)
        assertEquals("txn-123", result.id)
        assertEquals(1L, result.userId)
        assertEquals("acc-123", result.accountId)

        // Verify API log
        val capturedApiLog = apiLogSlot.captured
        assertEquals(dummyExternalServiceId, capturedApiLog.externalServiceId)
        assertEquals(dummyApiKeyId, capturedApiLog.apiKeyId)
        assertEquals("/api/transactions/$transactionId", capturedApiLog.endpoint)
        assertEquals(HttpMethod.GET, capturedApiLog.httpMethod)
        assertEquals(200, capturedApiLog.responseStatus)

        coVerify(exactly = 1) { cacheService.get(cacheKey, HistoricalTransaction::class.java) }
        coVerify(exactly = 0) { historicalTrxRepository.getTransactionById(any()) }
        coVerify(exactly = 0) { cacheService.set(any(), any(), any()) }
        coVerify(exactly = 1) { apiLogRepository.insertApiLog(any()) }
    }

    @Test
    fun `getTransactionById with cache miss should fetch from repository, cache result, and log API access`() = runTest {
        // arrange
        val transactionId = "txn-123"
        val cacheKey = CacheKeys.transactionKey(transactionId)
        val transactionSlot = slot<HistoricalTransaction>()
        val apiLogSlot = slot<ApiAccessLog>()

        coEvery { cacheService.get(cacheKey, HistoricalTransaction::class.java) } returns null
        coEvery { historicalTrxRepository.getTransactionById(transactionId) } returns dummyHistoricalTransaction
        coEvery { cacheService.set(cacheKey, capture(transactionSlot), 30) } returns Unit
        coEvery { apiLogRepository.insertApiLog(capture(apiLogSlot)) } returns ApiAccessLog(
            externalServiceId = dummyExternalServiceId,
            apiKeyId = dummyApiKeyId,
            endpoint = "/api/transactions/$transactionId",
            httpMethod = HttpMethod.GET,
            ipAddress = dummyIpAddress,
            userAgent = dummyUserAgent,
            responseStatus = 200
        )

        // act
        val result = historicalTrxService.getTransactionById(
            id = transactionId,
            externalServiceId = dummyExternalServiceId,
            apiKeyId = dummyApiKeyId,
            ipAddress = dummyIpAddress,
            userAgent = dummyUserAgent,
            transactionId = transactionId
        )

        // assert
        assertEquals(dummyHistoricalTransaction, result)
        assertEquals("txn-123", result.id)
        assertEquals(TransactionType.PAYMENT, result.transactionType)
        assertEquals(TransactionStatus.SUCCESS, result.transactionStatus)

        // Verify cached value
        val cachedTransaction = transactionSlot.captured
        assertEquals("txn-123", cachedTransaction.id)
        assertEquals(1L, cachedTransaction.userId)

        // Verify API log
        val capturedApiLog = apiLogSlot.captured
        assertEquals("/api/transactions/$transactionId", capturedApiLog.endpoint)

        coVerify(exactly = 1) { cacheService.get(cacheKey, HistoricalTransaction::class.java) }
        coVerify(exactly = 1) { historicalTrxRepository.getTransactionById(transactionId) }
        coVerify(exactly = 1) { cacheService.set(cacheKey, any(), 30) }
        coVerify(exactly = 1) { apiLogRepository.insertApiLog(any()) }
    }

    @Test
    fun `getTransactionById with non-existent transaction should throw NotFoundResourceException`() = runTest {
        // arrange
        val nonExistentId = "nonexistent-txn"
        val cacheKey = CacheKeys.transactionKey(nonExistentId)

        coEvery { cacheService.get(cacheKey, HistoricalTransaction::class.java) } returns null
        coEvery { historicalTrxRepository.getTransactionById(nonExistentId) } returns null

        // act & assert
        val exception = assertThrows(APIException.NotFoundResourceException::class.java) {
            runBlocking {
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

        assertEquals("Transaction with id $nonExistentId not found", exception.message)
        assertEquals(404, exception.statusCode)

        coVerify(exactly = 1) { cacheService.get(cacheKey, HistoricalTransaction::class.java) }
        coVerify(exactly = 1) { historicalTrxRepository.getTransactionById(nonExistentId) }
        coVerify(exactly = 0) { cacheService.set(any(), any(), any()) }
        coVerify(exactly = 0) { apiLogRepository.insertApiLog(any()) }
    }

    @Test
    fun `getTransactionCount with cache hit should return cached count`() = runTest {
        // arrange
        val cachedCount = 150L
        coEvery { cacheService.get(CacheKeys.TRANSACTION_COUNT, Long::class.java) } returns cachedCount

        // act
        val result = historicalTrxService.getTransactionCount()

        // assert
        assertEquals(cachedCount, result)
        coVerify(exactly = 1) { cacheService.get(CacheKeys.TRANSACTION_COUNT, Long::class.java) }
        coVerify(exactly = 0) { historicalTrxRepository.getTransactionCount() }
        coVerify(exactly = 0) { cacheService.set(any(), any(), any()) }
    }

    @Test
    fun `getTransactionCount with cache miss should fetch from repository and cache result`() = runTest {
        // arrange
        val actualCount = 275L
        val countSlot = slot<Long>()

        coEvery { cacheService.get(CacheKeys.TRANSACTION_COUNT, Long::class.java) } returns null
        coEvery { historicalTrxRepository.getTransactionCount() } returns actualCount
        coEvery { cacheService.set(CacheKeys.TRANSACTION_COUNT, capture(countSlot), 5) } returns Unit

        // act
        val result = historicalTrxService.getTransactionCount()

        // assert
        assertEquals(actualCount, result)

        // Verify cached value
        val cachedCount = countSlot.captured
        assertEquals(actualCount, cachedCount)

        coVerify(exactly = 1) { cacheService.get(CacheKeys.TRANSACTION_COUNT, Long::class.java) }
        coVerify(exactly = 1) { historicalTrxRepository.getTransactionCount() }
        coVerify(exactly = 1) { cacheService.set(CacheKeys.TRANSACTION_COUNT, actualCount, 5) }
    }

    @Test
    fun `getTransactions should handle empty result from repository`() = runTest {
        // arrange
        val page = 1
        val size = 10
        val limit = page * size
        val offset = (page - 1) * size
        val cacheKey = CacheKeys.transactionListKey(page, size)
        val emptyFlow = flowOf<HistoricalTransaction>()
        val transactionListSlot = slot<List<TransactionResp>>()

        coEvery { cacheService.get(cacheKey, List::class.java) } returns null
        coEvery { historicalTrxRepository.getTransactions(limit, offset) } returns emptyFlow
        coEvery { cacheService.set(cacheKey, capture(transactionListSlot), 15) } returns Unit
        coEvery { apiLogRepository.insertApiLog(any()) } returns ApiAccessLog(
            externalServiceId = dummyExternalServiceId,
            apiKeyId = dummyApiKeyId,
            endpoint = "/api/transactions",
            httpMethod = HttpMethod.GET,
            ipAddress = dummyIpAddress,
            userAgent = dummyUserAgent,
            responseStatus = 200
        )

        // act
        val result = historicalTrxService.getTransactions(
            externalServiceId = dummyExternalServiceId,
            apiKeyId = dummyApiKeyId,
            ipAddress = dummyIpAddress,
            userAgent = dummyUserAgent,
            page = page,
            size = size
        )

        // assert
        assertTrue(result.isEmpty())

        // Verify empty list was cached
        val cachedTransactions = transactionListSlot.captured
        assertTrue(cachedTransactions.isEmpty())

        coVerify(exactly = 1) { cacheService.get(cacheKey, List::class.java) }
        coVerify(exactly = 1) { historicalTrxRepository.getTransactions(limit, offset) }
        coVerify(exactly = 1) { cacheService.set(cacheKey, any(), 15) }
        coVerify(exactly = 1) { apiLogRepository.insertApiLog(any()) }
    }

    @Test
    fun `getTransactions should handle multiple transactions with different types and statuses`() = runTest {
        // arrange
        val page = 1
        val size = 10
        val limit = page * size
        val offset = (page - 1) * size
        val cacheKey = CacheKeys.transactionListKey(page, size)

        val transaction1 = dummyHistoricalTransaction.copy(
            id = "txn-001",
            transactionType = TransactionType.TOPUP,
            transactionStatus = TransactionStatus.SUCCESS,
            paymentMethod = PaymentMethod.GOPAY
        )
        val transaction2 = dummyHistoricalTransaction.copy(
            id = "txn-002",
            transactionType = TransactionType.REFUND,
            transactionStatus = TransactionStatus.PENDING,
            paymentMethod = PaymentMethod.SHOPEE_PAY
        )
        val transaction3 = dummyHistoricalTransaction.copy(
            id = "txn-003",
            transactionType = TransactionType.TRANSFER,
            transactionStatus = TransactionStatus.FAILED,
            paymentMethod = null
        )

        val transactionFlow = flowOf(transaction1, transaction2, transaction3)

        coEvery { cacheService.get(cacheKey, List::class.java) } returns null
        coEvery { historicalTrxRepository.getTransactions(limit, offset) } returns transactionFlow
        coEvery { cacheService.set(cacheKey, any(), 15) } returns Unit
        coEvery { apiLogRepository.insertApiLog(any()) } returns ApiAccessLog(
            externalServiceId = dummyExternalServiceId,
            apiKeyId = dummyApiKeyId,
            endpoint = "/api/transactions",
            httpMethod = HttpMethod.GET,
            ipAddress = dummyIpAddress,
            userAgent = dummyUserAgent,
            responseStatus = 200
        )

        // act
        val result = historicalTrxService.getTransactions(
            externalServiceId = dummyExternalServiceId,
            apiKeyId = dummyApiKeyId,
            ipAddress = dummyIpAddress,
            userAgent = dummyUserAgent,
            page = page,
            size = size
        )

        // assert
        assertEquals(3, result.size)
        assertEquals("txn-001", result[0].id)
        assertEquals("txn-002", result[1].id)
        assertEquals("txn-003", result[2].id)
        assertEquals(TransactionStatus.SUCCESS, result[0].transactionStatus)
        assertEquals(TransactionStatus.PENDING, result[1].transactionStatus)
        assertEquals(TransactionStatus.FAILED, result[2].transactionStatus)

        coVerify(exactly = 1) { cacheService.get(cacheKey, List::class.java) }
        coVerify(exactly = 1) { historicalTrxRepository.getTransactions(limit, offset) }
        coVerify(exactly = 1) { cacheService.set(cacheKey, any(), 15) }
        coVerify(exactly = 1) { apiLogRepository.insertApiLog(any()) }
    }

    @Test
    fun `getTransactions should calculate correct limit and offset for different pages`() = runTest {
        // arrange
        val testCases = listOf(
            Triple(1, 10, Pair(10, 0)),    // page 1, size 10 -> limit 10, offset 0
            Triple(2, 10, Pair(20, 10)),   // page 2, size 10 -> limit 20, offset 10
            Triple(3, 5, Pair(15, 10)),    // page 3, size 5 -> limit 15, offset 10
            Triple(1, 20, Pair(20, 0)),    // page 1, size 20 -> limit 20, offset 0
            Triple(5, 3, Pair(15, 12))     // page 5, size 3 -> limit 15, offset 12
        )

        testCases.forEach { (page, size, expectedLimitOffset) ->
            val (expectedLimit, expectedOffset) = expectedLimitOffset
            val cacheKey = CacheKeys.transactionListKey(page, size)

            coEvery { cacheService.get(cacheKey, List::class.java) } returns null
            coEvery { historicalTrxRepository.getTransactions(expectedLimit, expectedOffset) } returns flowOf()
            coEvery { cacheService.set(cacheKey, any(), 15) } returns Unit
            coEvery { apiLogRepository.insertApiLog(any()) } returns ApiAccessLog(
                externalServiceId = dummyExternalServiceId,
                apiKeyId = dummyApiKeyId,
                endpoint = "/api/transactions",
                httpMethod = HttpMethod.GET,
                ipAddress = dummyIpAddress,
                userAgent = dummyUserAgent,
                responseStatus = 200
            )

            // act
            historicalTrxService.getTransactions(
                externalServiceId = dummyExternalServiceId,
                apiKeyId = dummyApiKeyId,
                ipAddress = dummyIpAddress,
                userAgent = dummyUserAgent,
                page = page,
                size = size
            )

            // assert
            coVerify(exactly = 1) { historicalTrxRepository.getTransactions(expectedLimit, expectedOffset) }
        }
    }

    @Test
    fun `getTransactionCount should return zero when no transactions exist`() = runTest {
        // arrange
        coEvery { cacheService.get(CacheKeys.TRANSACTION_COUNT, Long::class.java) } returns null
        coEvery { historicalTrxRepository.getTransactionCount() } returns 0L
        coEvery { cacheService.set(CacheKeys.TRANSACTION_COUNT, 0L, 5) } returns Unit

        // act
        val result = historicalTrxService.getTransactionCount()

        // assert
        assertEquals(0L, result)
        coVerify(exactly = 1) { cacheService.get(CacheKeys.TRANSACTION_COUNT, Long::class.java) }
        coVerify(exactly = 1) { historicalTrxRepository.getTransactionCount() }
        coVerify(exactly = 1) { cacheService.set(CacheKeys.TRANSACTION_COUNT, 0L, 5) }
    }

    @Test
    fun `getTransactions should handle large page numbers`() = runTest {
        // arrange
        val largePage = 1000
        val size = 50
        val expectedLimit = largePage * size
        val expectedOffset = (largePage - 1) * size
        val cacheKey = CacheKeys.transactionListKey(largePage, size)

        coEvery { cacheService.get(cacheKey, List::class.java) } returns null
        coEvery { historicalTrxRepository.getTransactions(expectedLimit, expectedOffset) } returns flowOf()
        coEvery { cacheService.set(cacheKey, any(), 15) } returns Unit
        coEvery { apiLogRepository.insertApiLog(any()) } returns ApiAccessLog(
            externalServiceId = dummyExternalServiceId,
            apiKeyId = dummyApiKeyId,
            endpoint = "/api/transactions",
            httpMethod = HttpMethod.GET,
            ipAddress = dummyIpAddress,
            userAgent = dummyUserAgent,
            responseStatus = 200
        )

        // act
        val result = historicalTrxService.getTransactions(
            externalServiceId = dummyExternalServiceId,
            apiKeyId = dummyApiKeyId,
            ipAddress = dummyIpAddress,
            userAgent = dummyUserAgent,
            page = largePage,
            size = size
        )

        // assert
        assertTrue(result.isEmpty())
        coVerify(exactly = 1) { historicalTrxRepository.getTransactions(expectedLimit, expectedOffset) }
    }
}