package id.co.bni.transactionservice.commons.constants

object CacheKeys {
    fun transactionKey(id: String) = "trx:$id"
    fun transactionListKey(page: Int, size: Int) = "trx_list:$page:$size"
    const val TRANSACTION_COUNT = "trx_count"
}