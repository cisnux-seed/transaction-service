package id.co.bni.transactionservice.common.exceptions

import id.co.bni.transactionservice.commons.exceptions.APIException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.collections.forEach
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.text.isNotEmpty

class APIExceptionTest {

    @Test
    fun `NotFoundResourceException should have correct default values`() {
        // Given
        val message = "Resource not found"

        // When
        val exception = APIException.NotFoundResourceException(message = message)

        // Then
        assertEquals(404, exception.statusCode)
        assertEquals(message, exception.message)
    }

    @Test
    fun `NotFoundResourceException should allow custom status code`() {
        // Given
        val customStatusCode = 410
        val message = "Resource gone"

        // When
        val exception = APIException.NotFoundResourceException(
            statusCode = customStatusCode,
            message = message
        )

        // Then
        assertEquals(customStatusCode, exception.statusCode)
        assertEquals(message, exception.message)
    }

    @Test
    fun `NotFoundResourceException should allow mutation of properties`() {
        // Given
        val exception = APIException.NotFoundResourceException(message = "Original message")

        // When
        exception.statusCode = 410
        exception.message = "Updated message"

        // Then
        assertEquals(410, exception.statusCode)
        assertEquals("Updated message", exception.message)
    }

    @Test
    fun `UnauthenticatedException should have correct default values`() {
        // Given
        val message = "Authentication failed"

        // When
        val exception = APIException.UnauthenticatedException(message = message)

        // Then
        assertEquals(401, exception.statusCode)
        assertEquals(message, exception.message)
    }

    @Test
    fun `UnauthenticatedException should allow custom status code`() {
        // Given
        val customStatusCode = 403
        val message = "Token expired"

        // When
        val exception = APIException.UnauthenticatedException(
            statusCode = customStatusCode,
            message = message
        )

        // Then
        assertEquals(customStatusCode, exception.statusCode)
        assertEquals(message, exception.message)
    }

    @Test
    fun `UnauthenticatedException should allow mutation of properties`() {
        // Given
        val exception = APIException.UnauthenticatedException(message = "Original")

        // When
        exception.statusCode = 498
        exception.message = "Token expired"

        // Then
        assertEquals(498, exception.statusCode)
        assertEquals("Token expired", exception.message)
    }

    @Test
    fun `ForbiddenException should have correct default values`() {
        // Given
        val message = "Access denied"

        // When
        val exception = APIException.ForbiddenException(message = message)

        // Then
        assertEquals(403, exception.statusCode)
        assertEquals(message, exception.message)
    }

    @Test
    fun `ForbiddenException should allow custom status code`() {
        // Given
        val customStatusCode = 451
        val message = "Unavailable for legal reasons"

        // When
        val exception = APIException.ForbiddenException(
            statusCode = customStatusCode,
            message = message
        )

        // Then
        assertEquals(customStatusCode, exception.statusCode)
        assertEquals(message, exception.message)
    }

    @Test
    fun `ForbiddenException should allow mutation of properties`() {
        // Given
        val exception = APIException.ForbiddenException(message = "Access denied")

        // When
        exception.statusCode = 451
        exception.message = "Legal restriction"

        // Then
        assertEquals(451, exception.statusCode)
        assertEquals("Legal restriction", exception.message)
    }

    @Test
    fun `InternalServerException should have correct default values`() {
        // Given
        val message = "Internal server error"

        // When
        val exception = APIException.InternalServerException(message = message)

        // Then
        assertEquals(500, exception.statusCode)
        assertEquals(message, exception.message)
    }

    @Test
    fun `InternalServerException should allow custom status code`() {
        // Given
        val customStatusCode = 502
        val message = "Bad gateway"

        // When
        val exception = APIException.InternalServerException(
            statusCode = customStatusCode,
            message = message
        )

        // Then
        assertEquals(customStatusCode, exception.statusCode)
        assertEquals(message, exception.message)
    }

    @Test
    fun `InternalServerException should allow mutation of properties`() {
        // Given
        val exception = APIException.InternalServerException(message = "Server error")

        // When
        exception.statusCode = 503
        exception.message = "Service unavailable"

        // Then
        assertEquals(503, exception.statusCode)
        assertEquals("Service unavailable", exception.message)
    }

    @Test
    fun `IllegalParameterException should have correct default values`() {
        // Given
        val message = "Invalid parameter"

        // When
        val exception = APIException.IllegalParameterException(message = message)

        // Then
        assertEquals(400, exception.statusCode)
        assertEquals(message, exception.message)
    }

    @Test
    fun `IllegalParameterException should allow custom status code`() {
        // Given
        val customStatusCode = 422
        val message = "Unprocessable entity"

        // When
        val exception = APIException.IllegalParameterException(
            statusCode = customStatusCode,
            message = message
        )

        // Then
        assertEquals(customStatusCode, exception.statusCode)
        assertEquals(message, exception.message)
    }

    @Test
    fun `IllegalParameterException should allow mutation of properties`() {
        // Given
        val exception = APIException.IllegalParameterException(message = "Bad request")

        // When
        exception.statusCode = 422
        exception.message = "Validation failed"

        // Then
        assertEquals(422, exception.statusCode)
        assertEquals("Validation failed", exception.message)
    }

    @Test
    fun `ConflictResourceException should have correct default values`() {
        // Given
        val message = "Resource conflict"

        // When
        val exception = APIException.ConflictResourceException(message = message)

        // Then
        assertEquals(409, exception.statusCode)
        assertEquals(message, exception.message)
    }

    @Test
    fun `ConflictResourceException should allow custom status code`() {
        // Given
        val customStatusCode = 412
        val message = "Precondition failed"

        // When
        val exception = APIException.ConflictResourceException(
            statusCode = customStatusCode,
            message = message
        )

        // Then
        assertEquals(customStatusCode, exception.statusCode)
        assertEquals(message, exception.message)
    }

    @Test
    fun `ConflictResourceException should allow mutation of properties`() {
        // Given
        val exception = APIException.ConflictResourceException(message = "Conflict")

        // When
        exception.statusCode = 412
        exception.message = "Precondition failed"

        // Then
        assertEquals(412, exception.statusCode)
        assertEquals("Precondition failed", exception.message)
    }

    @Test
    fun `all exceptions should be throwable`() {
        // Test that all exception types can be thrown and caught

        assertThrows<APIException.NotFoundResourceException> {
            throw APIException.NotFoundResourceException(message = "Not found")
        }

        assertThrows<APIException.UnauthenticatedException> {
            throw APIException.UnauthenticatedException(message = "Unauthorized")
        }

        assertThrows<APIException.ForbiddenException> {
            throw APIException.ForbiddenException(message = "Forbidden")
        }

        assertThrows<APIException.InternalServerException> {
            throw APIException.InternalServerException(message = "Internal error")
        }

        assertThrows<APIException.IllegalParameterException> {
            throw APIException.IllegalParameterException(message = "Bad request")
        }

        assertThrows<APIException.ConflictResourceException> {
            throw APIException.ConflictResourceException(message = "Conflict")
        }
    }

    @Test
    fun `exceptions should be catchable as base APIException`() {
        // Test polymorphism - all specific exceptions can be caught as APIException

        val exceptions = listOf(
            APIException.NotFoundResourceException(message = "Not found"),
            APIException.UnauthenticatedException(message = "Unauthorized"),
            APIException.ForbiddenException(message = "Forbidden"),
            APIException.InternalServerException(message = "Internal error"),
            APIException.IllegalParameterException(message = "Bad request"),
            APIException.ConflictResourceException(message = "Conflict")
        )

        exceptions.forEach { exception ->
            try {
                throw exception
            } catch (e: APIException) {
                // Should be able to catch as base class
                assertTrue(e.statusCode > 0)
                assertTrue(e.message.isNotEmpty())
            }
        }
    }

    @Test
    fun `exception equality should work correctly`() {
        // Test data class equality
        val exception1 = APIException.NotFoundResourceException(404, "Not found")
        val exception2 = APIException.NotFoundResourceException(404, "Not found")
        val exception3 = APIException.NotFoundResourceException(404, "Different message")

        assertEquals(exception1, exception2)
        assert(exception1 != exception3)
    }

    @Test
    fun `exception copy should work correctly`() {
        // Test data class copy functionality
        val original = APIException.IllegalParameterException(400, "Invalid input")
        val copied = original.copy(message = "Modified message")

        assertEquals(400, copied.statusCode)
        assertEquals("Modified message", copied.message)
        assertEquals(400, original.statusCode)
        assertEquals("Invalid input", original.message)
    }

    @Test
    fun `multiple mutations should work correctly`() {
        // Test multiple property mutations
        val exception = APIException.UnauthenticatedException(message = "Initial message")

        // First mutation
        exception.statusCode = 498
        exception.message = "Invalid token"
        assertEquals(498, exception.statusCode)
        assertEquals("Invalid token", exception.message)

        // Second mutation
        exception.statusCode = 499
        exception.message = "Token required"
        assertEquals(499, exception.statusCode)
        assertEquals("Token required", exception.message)
    }

    @Test
    fun `mutation should not affect other instances`() {
        // Test that mutating one instance doesn't affect others
        val exception1 = APIException.ConflictResourceException(message = "Conflict 1")
        val exception2 = APIException.ConflictResourceException(message = "Conflict 2")

        exception1.statusCode = 412
        exception1.message = "Modified conflict 1"

        assertEquals(412, exception1.statusCode)
        assertEquals("Modified conflict 1", exception1.message)
        assertEquals(409, exception2.statusCode)
        assertEquals("Conflict 2", exception2.message)
    }
}