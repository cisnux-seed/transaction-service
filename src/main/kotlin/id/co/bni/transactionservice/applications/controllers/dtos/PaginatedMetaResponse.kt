package id.co.bni.transactionservice.applications.controllers.dtos

data class PaginatedMetaResponse(
    val code: String,
    val message: String? = null,
    val total: Long,
    val page: Int,
    val size: Int,
)
