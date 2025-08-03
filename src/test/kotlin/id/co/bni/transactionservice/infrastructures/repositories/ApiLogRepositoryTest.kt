package id.co.bni.transactionservice.infrastructures.repositories

import id.co.bni.transactionservice.commons.constants.HttpMethod
import id.co.bni.transactionservice.domains.entities.ApiAccessLog
import id.co.bni.transactionservice.infrastructures.repositories.dao.ApiLogDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime
import kotlin.test.*

@ExtendWith(MockKExtension::class)
class ApiLogRepositoryTest {

    @MockK
    private lateinit var apiLogDao: ApiLogDao

    @InjectMockKs
    private lateinit var apiLogRepository: ApiLogRepositoryImpl

    @Test
    fun `insertApiLog should return api log when successful`() = runTest {
        // arrange
        val apiLog = ApiAccessLog(
            id = "log-123",
            externalServiceId = "service-456",
            apiKeyId = "key-789",
            endpoint = "/api/transactions",
            httpMethod = HttpMethod.GET,
            ipAddress = "192.168.1.1",
            userAgent = "Mozilla/5.0",
            responseStatus = 200,
            createdAt = LocalDateTime.of(2024, 1, 1, 10, 0, 0)
        )
        
        coEvery { 
            apiLogDao.insertApiLog(
                id = "log-123",
                externalServiceId = "service-456",
                apiKeyId = "key-789",
                endpoint = "/api/transactions",
                httpMethod = HttpMethod.GET,
                ipAddress = "192.168.1.1",
                userAgent = "Mozilla/5.0",
                responseStatus = 200,
                createdAt = apiLog.createdAt
            ) 
        } returns 1

        // act
        val result = apiLogRepository.insertApiLog(apiLog)

        // assert
        assertEquals("log-123", result.id)
        assertEquals("service-456", result.externalServiceId)
        assertEquals("key-789", result.apiKeyId)
        assertEquals("/api/transactions", result.endpoint)
        assertEquals(HttpMethod.GET, result.httpMethod)
        assertEquals("192.168.1.1", result.ipAddress)
        assertEquals("Mozilla/5.0", result.userAgent)
        assertEquals(200, result.responseStatus)
        assertEquals(LocalDateTime.of(2024, 1, 1, 10, 0, 0), result.createdAt)
        
        coVerify(exactly = 1) { 
            apiLogDao.insertApiLog(
                id = "log-123",
                externalServiceId = "service-456",
                apiKeyId = "key-789",
                endpoint = "/api/transactions",
                httpMethod = HttpMethod.GET,
                ipAddress = "192.168.1.1",
                userAgent = "Mozilla/5.0",
                responseStatus = 200,
                createdAt = apiLog.createdAt
            ) 
        }
    }

    @Test
    fun `insertApiLog should handle POST method`() = runTest {
        // arrange
        val apiLog = ApiAccessLog(
            id = "log-post",
            externalServiceId = "service-post",
            apiKeyId = "key-post",
            endpoint = "/api/transactions/create",
            httpMethod = HttpMethod.POST,
            ipAddress = "10.0.0.1",
            userAgent = "curl/7.68.0",
            responseStatus = 201,
            createdAt = LocalDateTime.now()
        )
        
        coEvery { 
            apiLogDao.insertApiLog(
                id = "log-post",
                externalServiceId = "service-post",
                apiKeyId = "key-post",
                endpoint = "/api/transactions/create",
                httpMethod = HttpMethod.POST,
                ipAddress = "10.0.0.1",
                userAgent = "curl/7.68.0",
                responseStatus = 201,
                createdAt = apiLog.createdAt
            ) 
        } returns 1

        // act
        val result = apiLogRepository.insertApiLog(apiLog)

        // assert
        assertEquals(HttpMethod.POST, result.httpMethod)
        assertEquals(201, result.responseStatus)
        coVerify(exactly = 1) { 
            apiLogDao.insertApiLog(
                id = "log-post",
                externalServiceId = "service-post",
                apiKeyId = "key-post",
                endpoint = "/api/transactions/create",
                httpMethod = HttpMethod.POST,
                ipAddress = "10.0.0.1",
                userAgent = "curl/7.68.0",
                responseStatus = 201,
                createdAt = apiLog.createdAt
            ) 
        }
    }

    @Test
    fun `insertApiLog should handle different HTTP methods`() = runTest {
        // arrange
        val httpMethods = listOf(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.PATCH)
        
        httpMethods.forEach { method ->
            val apiLog = ApiAccessLog(
                id = "log-${method.value.lowercase()}",
                externalServiceId = "service-test",
                apiKeyId = "key-test",
                endpoint = "/api/test",
                httpMethod = method,
                ipAddress = "127.0.0.1",
                userAgent = "TestAgent",
                responseStatus = 200,
                createdAt = LocalDateTime.now()
            )
            
            coEvery { 
                apiLogDao.insertApiLog(
                    id = "log-${method.value.lowercase()}",
                    externalServiceId = "service-test",
                    apiKeyId = "key-test",
                    endpoint = "/api/test",
                    httpMethod = method,
                    ipAddress = "127.0.0.1",
                    userAgent = "TestAgent",
                    responseStatus = 200,
                    createdAt = apiLog.createdAt
                ) 
            } returns 1

            // act
            val result = apiLogRepository.insertApiLog(apiLog)

            // assert
            assertEquals(method, result.httpMethod)
            assertEquals("log-${method.value.lowercase()}", result.id)
        }
    }

    @Test
    fun `insertApiLog should handle error status codes`() = runTest {
        // arrange
        val errorStatuses = listOf(400, 401, 403, 404, 500, 502, 503)
        
        errorStatuses.forEach { status ->
            val apiLog = ApiAccessLog(
                id = "log-error-$status",
                externalServiceId = "service-error",
                apiKeyId = "key-error",
                endpoint = "/api/error",
                httpMethod = HttpMethod.GET,
                ipAddress = "192.168.1.100",
                userAgent = "ErrorAgent",
                responseStatus = status,
                createdAt = LocalDateTime.now()
            )
            
            coEvery { 
                apiLogDao.insertApiLog(
                    id = "log-error-$status",
                    externalServiceId = "service-error",
                    apiKeyId = "key-error",
                    endpoint = "/api/error",
                    httpMethod = HttpMethod.GET,
                    ipAddress = "192.168.1.100",
                    userAgent = "ErrorAgent",
                    responseStatus = status,
                    createdAt = apiLog.createdAt
                ) 
            } returns 1

            // act
            val result = apiLogRepository.insertApiLog(apiLog)

            // assert
            assertEquals(status, result.responseStatus)
            assertEquals("log-error-$status", result.id)
        }
    }

    @Test
    fun `insertApiLog should handle long user agent strings`() = runTest {
        // arrange
        val longUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36 Edg/91.0.864.59"
        val apiLog = ApiAccessLog(
            id = "log-long-ua",
            externalServiceId = "service-ua",
            apiKeyId = "key-ua",
            endpoint = "/api/transactions",
            httpMethod = HttpMethod.GET,
            ipAddress = "203.0.113.1",
            userAgent = longUserAgent,
            responseStatus = 200,
            createdAt = LocalDateTime.now()
        )
        
        coEvery { 
            apiLogDao.insertApiLog(
                id = "log-long-ua",
                externalServiceId = "service-ua",
                apiKeyId = "key-ua",
                endpoint = "/api/transactions",
                httpMethod = HttpMethod.GET,
                ipAddress = "203.0.113.1",
                userAgent = longUserAgent,
                responseStatus = 200,
                createdAt = apiLog.createdAt
            ) 
        } returns 1

        // act
        val result = apiLogRepository.insertApiLog(apiLog)

        // assert
        assertEquals(longUserAgent, result.userAgent)
        coVerify(exactly = 1) { 
            apiLogDao.insertApiLog(
                id = "log-long-ua",
                externalServiceId = "service-ua",
                apiKeyId = "key-ua",
                endpoint = "/api/transactions",
                httpMethod = HttpMethod.GET,
                ipAddress = "203.0.113.1",
                userAgent = longUserAgent,
                responseStatus = 200,
                createdAt = apiLog.createdAt
            ) 
        }
    }

    @Test
    fun `insertApiLog should handle IPv6 addresses`() = runTest {
        // arrange
        val ipv6Address = "2001:0db8:85a3:0000:0000:8a2e:0370:7334"
        val apiLog = ApiAccessLog(
            id = "log-ipv6",
            externalServiceId = "service-ipv6",
            apiKeyId = "key-ipv6",
            endpoint = "/api/transactions",
            httpMethod = HttpMethod.GET,
            ipAddress = ipv6Address,
            userAgent = "IPv6TestAgent",
            responseStatus = 200,
            createdAt = LocalDateTime.now()
        )
        
        coEvery { 
            apiLogDao.insertApiLog(
                id = "log-ipv6",
                externalServiceId = "service-ipv6",
                apiKeyId = "key-ipv6",
                endpoint = "/api/transactions",
                httpMethod = HttpMethod.GET,
                ipAddress = ipv6Address,
                userAgent = "IPv6TestAgent",
                responseStatus = 200,
                createdAt = apiLog.createdAt
            ) 
        } returns 1

        // act
        val result = apiLogRepository.insertApiLog(apiLog)

        // assert
        assertEquals(ipv6Address, result.ipAddress)
        coVerify(exactly = 1) { 
            apiLogDao.insertApiLog(
                id = "log-ipv6",
                externalServiceId = "service-ipv6",
                apiKeyId = "key-ipv6",
                endpoint = "/api/transactions",
                httpMethod = HttpMethod.GET,
                ipAddress = ipv6Address,
                userAgent = "IPv6TestAgent",
                responseStatus = 200,
                createdAt = apiLog.createdAt
            ) 
        }
    }

    @Test
    fun `insertApiLog should handle special characters in endpoint`() = runTest {
        // arrange
        val specialEndpoint = "/api/transactions?filter=type:PAYMENT&sort=date"
        val apiLog = ApiAccessLog(
            id = "log-special",
            externalServiceId = "service-special",
            apiKeyId = "key-special",
            endpoint = specialEndpoint,
            httpMethod = HttpMethod.GET,
            ipAddress = "172.16.254.1",
            userAgent = "SpecialAgent",
            responseStatus = 200,
            createdAt = LocalDateTime.now()
        )
        
        coEvery { 
            apiLogDao.insertApiLog(
                id = "log-special",
                externalServiceId = "service-special",
                apiKeyId = "key-special",
                endpoint = specialEndpoint,
                httpMethod = HttpMethod.GET,
                ipAddress = "172.16.254.1",
                userAgent = "SpecialAgent",
                responseStatus = 200,
                createdAt = apiLog.createdAt
            ) 
        } returns 1

        // act
        val result = apiLogRepository.insertApiLog(apiLog)

        // assert
        assertEquals(specialEndpoint, result.endpoint)
        coVerify(exactly = 1) { 
            apiLogDao.insertApiLog(
                id = "log-special",
                externalServiceId = "service-special",
                apiKeyId = "key-special",
                endpoint = specialEndpoint,
                httpMethod = HttpMethod.GET,
                ipAddress = "172.16.254.1",
                userAgent = "SpecialAgent",
                responseStatus = 200,
                createdAt = apiLog.createdAt
            ) 
        }
    }

    @Test
    fun `insertApiLog should preserve exact timestamp`() = runTest {
        // arrange
        val exactTimestamp = LocalDateTime.of(2024, 6, 15, 14, 30, 45, 123456789)
        val apiLog = ApiAccessLog(
            id = "log-timestamp",
            externalServiceId = "service-timestamp",
            apiKeyId = "key-timestamp",
            endpoint = "/api/transactions",
            httpMethod = HttpMethod.GET,
            ipAddress = "198.51.100.1",
            userAgent = "TimestampAgent",
            responseStatus = 200,
            createdAt = exactTimestamp
        )
        
        coEvery { 
            apiLogDao.insertApiLog(
                id = "log-timestamp",
                externalServiceId = "service-timestamp",
                apiKeyId = "key-timestamp",
                endpoint = "/api/transactions",
                httpMethod = HttpMethod.GET,
                ipAddress = "198.51.100.1",
                userAgent = "TimestampAgent",
                responseStatus = 200,
                createdAt = exactTimestamp
            ) 
        } returns 1

        // act
        val result = apiLogRepository.insertApiLog(apiLog)

        // assert
        assertEquals(exactTimestamp, result.createdAt)
        assertEquals(2024, result.createdAt.year)
        assertEquals(6, result.createdAt.monthValue)
        assertEquals(15, result.createdAt.dayOfMonth)
        assertEquals(14, result.createdAt.hour)
        assertEquals(30, result.createdAt.minute)
        assertEquals(45, result.createdAt.second)
        assertEquals(123456789, result.createdAt.nano)
        
        coVerify(exactly = 1) { 
            apiLogDao.insertApiLog(
                id = "log-timestamp",
                externalServiceId = "service-timestamp",
                apiKeyId = "key-timestamp",
                endpoint = "/api/transactions",
                httpMethod = HttpMethod.GET,
                ipAddress = "198.51.100.1",
                userAgent = "TimestampAgent",
                responseStatus = 200,
                createdAt = exactTimestamp
            ) 
        }
    }
}