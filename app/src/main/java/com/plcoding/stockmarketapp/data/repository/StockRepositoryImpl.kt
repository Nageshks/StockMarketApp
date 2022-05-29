package com.plcoding.stockmarketapp.data.repository

import com.plcoding.stockmarketapp.data.csv.CSVParser
import com.plcoding.stockmarketapp.data.local.StockDatabase
import com.plcoding.stockmarketapp.data.mapper.toCompanyListing
import com.plcoding.stockmarketapp.data.mapper.toCompanyListingEntity
import com.plcoding.stockmarketapp.data.remote.StockApi
import com.plcoding.stockmarketapp.domain.model.CompanyListing
import com.plcoding.stockmarketapp.domain.repository.StockRepository
import com.plcoding.stockmarketapp.util.Data
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockRepositoryImpl @Inject constructor(
    private val api: StockApi,
    db: StockDatabase,
    private val companyListingParser: CSVParser<CompanyListing>
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
}