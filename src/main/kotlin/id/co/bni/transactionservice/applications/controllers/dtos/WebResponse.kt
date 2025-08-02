package id.co.bni.transactionservice.applications.controllers.dtos

data class WebResponse<out T, out S>(
    val meta: S,
    val data: T? = null,
)
