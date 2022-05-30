package com.plcoding.stockmarketapp.data.remote

import com.plcoding.stockmarketapp.BuildConfig
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

interface StockApi {

    @GET("query?function=LISTING_STATUS")
    suspend fun getCompanyListings(
        @Query("apikey") apiKey : String = BuildConfig.ALPHA_VANTAGE_ACCESS_KEY
    ) : ResponseBody

    companion object{
        const val BASE_URL = "https://www.alphavantage.co/"
    }

}