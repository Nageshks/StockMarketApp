package com.plcoding.stockmarketapp.data.repository

import com.plcoding.stockmarketapp.data.csv.CSVParser
import com.plcoding.stockmarketapp.data.csv.IntradayInfoParser
import com.plcoding.stockmarketapp.data.local.StockDatabase
import com.plcoding.stockmarketapp.data.mapper.toCompanyInfo
import com.plcoding.stockmarketapp.data.mapper.toCompanyListing
import com.plcoding.stockmarketapp.data.mapper.toCompanyListingEntity
import com.plcoding.stockmarketapp.data.remote.StockApi
import com.plcoding.stockmarketapp.domain.model.CompanyInfo
import com.plcoding.stockmarketapp.domain.model.CompanyListing
import com.plcoding.stockmarketapp.domain.model.IntradayInfo
import com.plcoding.stockmarketapp.domain.repository.StockRepository
import com.plcoding.stockmarketapp.util.Data
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockRepositoryImpl @Inject constructor(
    private val api: StockApi,
    db: StockDatabase,
    private val companyListingParser: CSVParser<CompanyListing>,
    private val intradayInfoParser: CSVParser<IntradayInfo>,
) : StockRepository {

    private val stockDao = db.stockDao

    override suspend fun getCompanyListings(
        fetchFromRemote: Boolean,
        query: String
    ): Flow<Data<List<CompanyListing>>> = flow {
        emit(Data.Loading(true))
        val localListings = stockDao.queryCompanyListings(query)
        emit(Data.Success(data = localListings.map { it.toCompanyListing() }))
        val isEmpty = localListings.isEmpty() && query.isBlank()
        val isLocalCacheEnough = !isEmpty && !fetchFromRemote
        if(isLocalCacheEnough){
            emit(Data.Loading(false))
            return@flow
        }
        kotlin.runCatching {
            companyListingParser.parse(api.getCompanyListings().byteStream())
        }.onSuccess {apiCompanyListings ->
            stockDao.clearCompanyListings()
            stockDao.insertCompanyListings(apiCompanyListings.map {
                it.toCompanyListingEntity()
            })
            emit(Data.Success(
                data = stockDao
                    .queryCompanyListings("")
                    .map { it.toCompanyListing() }
            ))
            emit(Data.Loading(false))
        }.onFailure {
            emit(Data.Error("can't load data"))
        }
    }

    override suspend fun getIntradayInfo(symbol: String): Data<List<IntradayInfo>> {
        return try {
            val response = api.getIntradayInfo(symbol)
            val results = intradayInfoParser.parse(response.byteStream())
            Data.Success(results)
        } catch(e: IOException) {
            e.printStackTrace()
            Data.Error(
                message = "Couldn't load intraday info"
            )
        } catch(e: HttpException) {
            e.printStackTrace()
            Data.Error(
                message = "Couldn't load intraday info"
            )
        }
    }

    override suspend fun getCompanyInfo(symbol: String): Data<CompanyInfo> {
        return try {
            val result = api.getCompanyInfo(symbol)
            Data.Success(result.toCompanyInfo())
        } catch(e: IOException) {
            e.printStackTrace()
            Data.Error(message = "Couldn't load company info")
        } catch(e: HttpException) {
            e.printStackTrace()
            Data.Error(message = "Couldn't load company info")
        }
    }
 }