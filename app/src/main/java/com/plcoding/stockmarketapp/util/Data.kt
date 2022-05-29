package com.plcoding.stockmarketapp.util

sealed class Data<T>(val data: T? = null, val message: String? = null) {
    class Success<T>(data: T?): Data<T>(data)
    class Error<T>(message: String, data: T? = null): Data<T>(data, message)
    class Loading<T>(val isLoading: Boolean = true): Data<T>(null)
}