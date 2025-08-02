package id.co.bni.transactionservice.commons.constants

enum class TransactionStatus(val status: String) {
    PENDING("PENDING"),
    SUCCESS("SUCCESS"),
    FAILED("FAILED"),
    CANCELLED("CANCELLED"),
}