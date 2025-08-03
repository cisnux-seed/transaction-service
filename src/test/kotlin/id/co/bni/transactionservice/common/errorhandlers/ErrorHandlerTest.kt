package id.co.bni.transactionservice.common.errorhandlers

import id.co.bni.transactionservice.applications.controllers.dtos.MetaResponse
import id.co.bni.transactionservice.applications.controllers.dtos.WebResponse
import id.co.bni.transactionservice.commons.errorhandlers.ErrorHandler
import id.co.bni.transactionservice.commons.exceptions.APIException
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.server.ServerWebInputException
import kotlin.jvm.java
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.text.isNotEmpty

class ErrorHandlerTest {

    private lateinit var errorHandler: ErrorHandler

    @BeforeEach
    fun setUp() {
        errorHandler = ErrorHandler()
    }

    @Test
    fun `handleGlobalException should return 500 with internal server error message`() {
        // Given
        val exception = kotlin.RuntimeException("Test error")

        // When
        val response = errorHandler.handleGlobalException(exception)

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("500", response.body?.meta?.code)
        assertEquals("internal server error", response.body?.meta?.message)
        assertNull(response.body?.data)
    }

    @Test
    fun `handleDuplicateKeyException should return 400 with username or email exists message`() {
        // Given
        val exception = DuplicateKeyException("Duplicate key")

        // When
        val response = errorHandler.handleDuplicateKeyException(exception)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("400", response.body?.meta?.code)
        assertEquals("username or email already exists", response.body?.meta?.message)
        assertNull(response.body?.data)
    }

    @Test
    fun `handleMethodArgumentNotValid should return 400 with field error messages`() {
        // Given
        val fieldError1 = FieldError("user", "username", "Username is required")
        val fieldError2 = FieldError("user", "email", "Email is invalid")

        val bindingResult = mockk<BindingResult>(relaxed = true)
        every { bindingResult.fieldErrors } returns listOf(fieldError1, fieldError2)

        // Mock MethodParameter properly to avoid logging issues
        val methodParameter = mockk<MethodParameter>(relaxed = true)
        every { methodParameter.parameterIndex } returns 0
        every { methodParameter.method } returns String::class.java.getMethod("toString")
        every { methodParameter.parameterType } returns String::class.java

        val exception = MethodArgumentNotValidException(methodParameter, bindingResult)

        // When
        val response = errorHandler.handleMethodArgumentNotValid(exception)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("400", response.body?.meta?.code)
        assertEquals("Username is required, Email is invalid", response.body?.meta?.message)
        assertNull(response.body?.data)
    }

    @Test
    fun `handleWebExchangeBindException should return 400 with field error messages`() {
        // Given
        val fieldError1 = FieldError("user", "username", "Username is required")
        val fieldError2 = FieldError("user", "password", "Password too short")

        val bindingResult = mockk<BindingResult>(relaxed = true)
        every { bindingResult.fieldErrors } returns listOf(fieldError1, fieldError2)

        // Mock MethodParameter properly to avoid logging issues
        val methodParameter = mockk<MethodParameter>(relaxed = true)
        every { methodParameter.parameterIndex } returns 0
        every { methodParameter.method } returns String::class.java.getMethod("toString")
        every { methodParameter.parameterType } returns String::class.java

        val exception = WebExchangeBindException(methodParameter, bindingResult)

        // When
        val response = errorHandler.handleWebExchangeBindException(exception)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("400", response.body?.meta?.code)
        assertEquals("Username is required, Password too short", response.body?.meta?.message)
        assertNull(response.body?.data)
    }

    @Test
    fun `handleServerWebInputException should handle null root cause`() {
        // Given
        val serverWebInputException = ServerWebInputException("Invalid input")

        // When
        val response = errorHandler.handleSeverWebInputException(serverWebInputException)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("400", response.body?.meta?.code)
        assertNull(response.body?.meta?.message)
        assertNull(response.body?.data)
    }

    @Test
    fun `handleApiException should return custom status code and message for NotFoundResourceException`() {
        // Given
        val apiException = APIException.NotFoundResourceException(404, "User not found")

        // When
        val response = errorHandler.handleApiException(apiException)

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals("404", response.body?.meta?.code)
        assertEquals("User not found", response.body?.meta?.message)
        assertNull(response.body?.data)
    }

    @Test
    fun `handleApiException should return custom status code and message for UnauthenticatedException`() {
        // Given
        val apiException = APIException.UnauthenticatedException(401, "Invalid credentials")

        // When
        val response = errorHandler.handleApiException(apiException)

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertEquals("401", response.body?.meta?.code)
        assertEquals("Invalid credentials", response.body?.meta?.message)
        assertNull(response.body?.data)
    }

    @Test
    fun `handleApiException should return custom status code and message for ForbiddenException`() {
        // Given
        val apiException = APIException.ForbiddenException(403, "Access denied")

        // When
        val response = errorHandler.handleApiException(apiException)

        // Then
        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        assertEquals("403", response.body?.meta?.code)
        assertEquals("Access denied", response.body?.meta?.message)
        assertNull(response.body?.data)
    }

    @Test
    fun `handleApiException should return custom status code and message for InternalServerException`() {
        // Given
        val apiException = APIException.InternalServerException(500, "Database connection failed")

        // When
        val response = errorHandler.handleApiException(apiException)

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("500", response.body?.meta?.code)
        assertEquals("Database connection failed", response.body?.meta?.message)
        assertNull(response.body?.data)
    }

    @Test
    fun `handleApiException should return custom status code and message for IllegalParameterException`() {
        // Given
        val apiException = APIException.IllegalParameterException(400, "Invalid parameter format")

        // When
        val response = errorHandler.handleApiException(apiException)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("400", response.body?.meta?.code)
        assertEquals("Invalid parameter format", response.body?.meta?.message)
        assertNull(response.body?.data)
    }

    @Test
    fun `handleApiException should return custom status code and message for ConflictResourceException`() {
        // Given
        val apiException = APIException.ConflictResourceException(409, "Resource already exists")

        // When
        val response = errorHandler.handleApiException(apiException)

        // Then
        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertEquals("409", response.body?.meta?.code)
        assertEquals("Resource already exists", response.body?.meta?.message)
        assertNull(response.body?.data)
    }

    @Test
    fun `handleApiException should handle custom status codes`() {
        // Given
        val apiException = APIException.NotFoundResourceException(410, "Resource permanently deleted")

        // When
        val response = errorHandler.handleApiException(apiException)

        // Then
        assertEquals(HttpStatus.GONE, response.statusCode)
        assertEquals("410", response.body?.meta?.code)
        assertEquals("Resource permanently deleted", response.body?.meta?.message)
        assertNull(response.body?.data)
    }

    @Test
    fun `all handlers should return WebResponse with null data`() {
        // Test that all handlers consistently return null data
        val globalResponse = errorHandler.handleGlobalException(kotlin.RuntimeException("test"))
        val duplicateResponse = errorHandler.handleDuplicateKeyException(DuplicateKeyException("test"))
        val apiResponse = errorHandler.handleApiException(APIException.NotFoundResourceException(message = "test"))

        assertNull(globalResponse.body?.data)
        assertNull(duplicateResponse.body?.data)
        assertNull(apiResponse.body?.data)
    }

    @Test
    fun `all handlers should return proper response structure`() {
        // Test that all handlers return properly structured WebResponse
        val response = errorHandler.handleGlobalException(kotlin.RuntimeException("test"))

        // Verify response structure
        assert(response.body is WebResponse<String?, MetaResponse>)
        assert(response.body?.meta is MetaResponse)
        assert(response.body?.meta?.code?.isNotEmpty() == true)
        assert(response.body?.meta?.message?.isNotEmpty() == true)
    }
}