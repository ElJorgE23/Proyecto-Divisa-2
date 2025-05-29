package com.example.proyectodivisas.retrofit



import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ExchangeRateService {

    @GET("v6/8d7425d4ac3a7f0e7955c29d/latest/{baseCode}")
    suspend fun getLatestRates(@Path("baseCode") baseCode: String): Response<ExchangeRateResponse>
}