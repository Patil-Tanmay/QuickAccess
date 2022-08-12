package utils

sealed class Resource<out T: Any> {
    object Loading : Resource<Nothing>()
    object Error : Resource<Nothing>()
    data class Success<T: Any>(val data: T?=null) : Resource<T>()
}