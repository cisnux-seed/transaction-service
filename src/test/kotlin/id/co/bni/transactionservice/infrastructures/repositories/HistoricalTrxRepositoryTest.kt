package id.co.bni.transactionservice.infrastructures.repositories

import id.co.bni.transactionservice.domains.entities.HistoricalTransaction
import id.co.bni.transactionservice.infrastructures.repositories.dao.HistoricalTransactionDao
import id.co.bni.transactionservice.commons.constants.PaymentMethod
import id.co.bni.transactionservice.commons.constants.TransactionStatus
import id.co.bni.transactionservice.commons.constants.TransactionType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.*

@ExtendWith(MockKExtension::class)
class HistoricalTrxRepositoryTest {

    @MockK
    private lateinit var historicalTrxDao: HistoricalTransactionDao

    @InjectMockKs
    private lateinit var historicalTrxRepository: HistoricalTrxRepositoryImpl

    @Test
    fun `getTransactions should return flow of historical transactions`() = runTest {
        // arrange
        val now = LocalDateTime.now()
        val transaction1 = HistoricalTransaction(
            id = "txn-001",
            userId = 1L,
            accountId = "acc-001",
            transactionId = "trx-001",
            transactionType = TransactionType.PAYMENT,
            transactionStatus = TransactionStatus.SUCCESS,
            amount = BigDecimal("100000.00"),
            balanceBefore = BigDecimal("1000000.00"),
            balanceAfter = BigDecimal("900000.00"),
            currency = "IDR",
            description = "Payment for service",
            externalReference = "ext-ref-001",
            paymentMethod = PaymentMethod.GOPAY,
            metadata = null,
            isAccessibleExternal = true,
            createdAt = now,
            updatedAt = now
        )
        val transaction2 = HistoricalTransaction(
            id = "txn-002",
            userId = 1L,
            accountId = "acc-001",
            transactionId = "trx-002",
            transactionType = TransactionType.REFUND,
            transactionStatus = TransactionStatus.SUCCESS,
            amount = BigDecimal("250500.00"),
            balanceBefore = BigDecimal("900000.00"),
            balanceAfter = BigDecimal("1150500.00"),
            currency = "IDR",
            description = "Online purchase refund",
            externalReference = "ext-ref-002",
            paymentMethod = PaymentMethod.SHOPEE_PAY,
            metadata = "{\"source\": \"mobile_app\"}",
            isAccessibleExternal = false,
            createdAt = now,
            updatedAt = now
        )
        coEvery { historicalTrxDao.findAllPaginated(10, 0) } returns flowOf(transaction1, transaction2)

        // act
        val result = historicalTrxRepository.getTransactions(10, 0).toList()

        // assert
        assertEquals(2, result.size)
        assertEquals("txn-001", result[0].id)
        assertEquals("txn-002", result[1].id)
        assertEquals(BigDecimal("100000.00"), result[0].amount)
        assertEquals(BigDecimal("250500.00"), result[1].amount)
        assertEquals(TransactionType.PAYMENT, result[0].transactionType)
        assertEquals(TransactionType.REFUND, result[1].transactionType)
        coVerify { historicalTrxDao.findAllPaginated(10, 0) }
    }

    @Test
    fun `getTransactions should return empty flow when no transactions found`() = runTest {
        // arrange
        coEvery { historicalTrxDao.findAllPaginated(10, 0) } returns flowOf()

        // act
        val result = historicalTrxRepository.getTransactions(10, 0).toList()

        // assert
        assertTrue(result.isEmpty())
        coVerify { historicalTrxDao.findAllPaginated(10, 0) }
    }

    @Test
    fun `getTransactions should handle different limit and offset values`() = runTest {
        // arrange
        val now = LocalDateTime.now()
        val transaction = HistoricalTransaction(
            id = "txn-003",
            userId = 2L,
            accountId = "acc-002",
            transactionId = "trx-003",
            transactionType = TransactionType.TOPUP,
            transactionStatus = TransactionStatus.PENDING,
            amount = BigDecimal("75000.00"),
            balanceBefore = BigDecimal("500000.00"),
            balanceAfter = BigDecimal("575000.00"),
            currency = "IDR",
            description = "Top up balance",
            externalReference = null,
            paymentMethod = PaymentMethod.GOPAY,
            metadata = null,
            isAccessibleExternal = true,
            createdAt = now,
            updatedAt = now
        )
        coEvery { historicalTrxDao.findAllPaginated(5, 10) } returns flowOf(transaction)

        // act
        val result = historicalTrxRepository.getTransactions(5, 10).toList()

        // assert
        assertEquals(1, result.size)
        assertEquals("txn-003", result[0].id)
        assertEquals(2L, result[0].userId)
        assertEquals("acc-002", result[0].accountId)
        assertEquals(TransactionStatus.PENDING, result[0].transactionStatus)
        coVerify { historicalTrxDao.findAllPaginated(5, 10) }
    }

    @Test
    fun `getTransactionById should return transaction when found`() = runTest {
        // arrange
        val now = LocalDateTime.now()
        val transaction = HistoricalTransaction(
            id = "txn-001",
            userId = 1L,
            accountId = "acc-001",
            transactionId = "trx-001",
            transactionType = TransactionType.TRANSFER,
            transactionStatus = TransactionStatus.SUCCESS,
            amount = BigDecimal("100000.00"),
            balanceBefore = BigDecimal("1000000.00"),
            balanceAfter = BigDecimal("900000.00"),
            currency = "IDR",
            description = "Transfer to friend",
            externalReference = "ext-ref-001",
            paymentMethod = PaymentMethod.SHOPEE_PAY,
            metadata = "{\"recipient\": \"Jane Doe\"}",
            isAccessibleExternal = true,
            createdAt = now,
            updatedAt = now
        )
        coEvery { historicalTrxDao.findById("txn-001") } returns transaction

        // act
        val result = historicalTrxRepository.getTransactionById("txn-001")

        // assert
        assertNotNull(result)
        assertEquals("txn-001", result.id)
        assertEquals(1L, result.userId)
        assertEquals("acc-001", result.accountId)
        assertEquals("trx-001", result.transactionId)
        assertEquals(TransactionType.TRANSFER, result.transactionType)
        assertEquals(TransactionStatus.SUCCESS, result.transactionStatus)
        assertEquals(BigDecimal("100000.00"), result.amount)
        assertEquals(BigDecimal("1000000.00"), result.balanceBefore)
        assertEquals(BigDecimal("900000.00"), result.balanceAfter)
        assertEquals("IDR", result.currency)
        assertEquals("Transfer to friend", result.description)
        assertEquals("ext-ref-001", result.externalReference)
        assertEquals(PaymentMethod.SHOPEE_PAY, result.paymentMethod)
        assertEquals("{\"recipient\": \"Jane Doe\"}", result.metadata)
        assertTrue(result.isAccessibleExternal)
        coVerify { historicalTrxDao.findById("txn-001") }
    }

    @Test
    fun `getTransactionById should return null when not found`() = runTest {
        // arrange
        coEvery { historicalTrxDao.findById("nonexistent-txn") } returns null

        // act
        val result = historicalTrxRepository.getTransactionById("nonexistent-txn")

        // assert
        assertNull(result)
        coVerify { historicalTrxDao.findById("nonexistent-txn") }
    }

    @Test
    fun `getTransactionCount should return correct count`() = runTest {
        // arrange
        coEvery { historicalTrxDao.count() } returns 150L

        // act
        val result = historicalTrxRepository.getTransactionCount()

        // assert
        assertEquals(150L, result)
        coVerify { historicalTrxDao.count() }
    }

    @Test
    fun `getTransactionCount should return zero when no transactions exist`() = runTest {
        // arrange
        coEvery { historicalTrxDao.count() } returns 0L

        // act
        val result = historicalTrxRepository.getTransactionCount()

        // assert
        assertEquals(0L, result)
        coVerify { historicalTrxDao.count() }
    }

    @Test
    fun `getTransactions should handle errors gracefully`() = runTest {
        // arrange
        coEvery { historicalTrxDao.findAllPaginated(10, 0) } throws RuntimeException("DB Connection Error")

        // act & assert
        assertThrows<RuntimeException> {
            historicalTrxRepository.getTransactions(10, 0).toList()
        }
    }

    @Test
    fun `getTransactionById should handle errors gracefully`() = runTest {
        // arrange
        coEvery { historicalTrxDao.findById("txn-001") } throws RuntimeException("DB Error")

        // act & assert
        assertThrows<RuntimeException> {
            historicalTrxRepository.getTransactionById("txn-001")
        }
    }

    @Test
    fun `getTransactionCount should handle errors gracefully`() = runTest {
        // arrange
        coEvery { historicalTrxDao.count() } throws RuntimeException("Count operation failed")

        // act & assert
        assertThrows<RuntimeException> {
            historicalTrxRepository.getTransactionCount()
        }
    }

    @Test
    fun `getTransactions should work with boundary values`() = runTest {
        // arrange
        val now = LocalDateTime.now()
        val transaction = HistoricalTransaction(
            id = "txn-boundary",
            userId = 999L,
            accountId = "acc-999",
            transactionId = "trx-boundary",
            transactionType = TransactionType.PAYMENT,
            transactionStatus = TransactionStatus.FAILED,
            amount = BigDecimal("1000.00"),
            balanceBefore = BigDecimal("1000.00"),
            balanceAfter = BigDecimal("1000.00"),
            currency = "IDR",
            description = "Failed payment transaction",
            externalReference = null,
            paymentMethod = null,
            metadata = null,
            isAccessibleExternal = false,
            createdAt = now,
            updatedAt = now
        )
        coEvery { historicalTrxDao.findAllPaginated(1, 999) } returns flowOf(transaction)

        // act
        val result = historicalTrxRepository.getTransactions(1, 999).toList()

        // assert
        assertEquals(1, result.size)
        assertEquals("txn-boundary", result[0].id)
        assertEquals(BigDecimal("1000.00"), result[0].amount)
        assertEquals(TransactionStatus.FAILED, result[0].transactionStatus)
        assertNull(result[0].paymentMethod)
        assertFalse(result[0].isAccessibleExternal)
        coVerify { historicalTrxDao.findAllPaginated(1, 999) }
    }
}