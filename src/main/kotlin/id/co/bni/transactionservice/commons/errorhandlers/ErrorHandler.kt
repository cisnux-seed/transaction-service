package id.co.bni.transactionservice.commons.errorhandlers

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import id.co.bni.transactionservice.applications.controllers.dtos.MetaResponse
import id.co.bni.transactionservice.applications.controllers.dtos.WebResponse
import id.co.bni.transactionservice.commons.exceptions.APIException
import id.co.bni.transactionservice.commons.loggable.Loggable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.server.ServerWebInputException
import java.util.UUID
import kotlin.collections.joinToString
import kotlin.let
import kotlin.to

@RestControllerAdvice
class ErrorHandler : Loggable {

    @ExceptionHandler(Exception::class)
    fun handleGlobalException(exception: Exception): ResponseEntity<WebResponse<String?, MetaResponse>> {
        val traceId = UUID.randomUUID().toString()

        runBlocking {
            withContext(MDCContext(mapOf("traceId" to traceId))) {
                log.error("Error", exception)
            }
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            WebResponse(
                meta = MetaResponse(
                    code = HttpStatus.INTERNAL_SERVER_ERROR.value().toString(),
                    message = "internal server error"
                )
            )
        )
    }

    @ExceptionHandler(DuplicateKeyException::class)
    fun handleDuplicateKeyException(exception: DuplicateKeyException): ResponseEntity<WebResponse<String?, MetaResponse>> {
        val traceId = UUID.randomUUID().toString()

        runBlocking {
            withContext(MDCContext(mapOf("traceId" to traceId))) {
                log.error("Error", exception)
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            WebResponse(
                meta = MetaResponse(
                    code = HttpStatus.BAD_REQUEST.value().toString(),
                    message = "username or email already exists"
                )
            )
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(exception: MethodArgumentNotValidException): ResponseEntity<WebResponse<String?, MetaResponse>> {
        val traceId = UUID.randomUUID().toString()

        runBlocking {
            withContext(MDCContext(mapOf("traceId" to traceId))) {
                log.error("Error", exception)
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            WebResponse(
                meta = MetaResponse(
                    code = HttpStatus.BAD_REQUEST.value().toString(),
                    message = exception.bindingResult.fieldErrors.joinToString(", ") {
                        it.defaultMessage ?: "Invalid field"
                    }
                )
            )
        )
    }

    @ExceptionHandler(WebExchangeBindException::class)
    fun handleWebExchangeBindException(exception: WebExchangeBindException): ResponseEntity<WebResponse<String?, MetaResponse>> {
        val traceId = UUID.randomUUID().toString()

        runBlocking {
            withContext(MDCContext(mapOf("traceId" to traceId))) {
                log.error("Error", exception)
            }
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            WebResponse(
                meta = MetaResponse(
                    code = HttpStatus.BAD_REQUEST.value().toString(),
                    message = exception.bindingResult.fieldErrors.joinToString(", ") {
                        it.defaultMessage ?: "Invalid field"
                    }
                )
            )
        )
    }

    @ExceptionHandler(ServerWebInputException::class)
    fun handleSeverWebInputException(exception: ServerWebInputException): ResponseEntity<WebResponse<String?, MetaResponse>> {
        val traceId = UUID.randomUUID().toString()

        runBlocking {
            withContext(MDCContext(mapOf("traceId" to traceId))) {
                log.error("Error", exception)
            }
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            WebResponse(
                meta = MetaResponse(
                    code = HttpStatus.BAD_REQUEST.value().toString(),
                    message = exception.rootCause?.let {
                        if (it is MismatchedInputException) {
                            val parameters = it.path.joinToString(".") { field -> field.fieldName }
                            "missing required parameters: $parameters"
                        } else {
                            "invalid input"
                        }
                    }
                )
            )
        )
    }

    @ExceptionHandler(APIException::class)
    fun handleApiException(apiException: APIException): ResponseEntity<WebResponse<String?, MetaResponse>> {
        val traceId = UUID.randomUUID().toString()

        runBlocking {
            withContext(MDCContext(mapOf("traceId" to traceId))) {
                log.error("Error", apiException)
            }
        }

        return ResponseEntity.status(apiException.statusCode).body(
            WebResponse(
                meta = MetaResponse(
                    code = apiException.statusCode.toString(),
                    message = apiException.message
                )
            )
        )
    }
}