package com.example.proyectodivisas.worker

import com.example.proyectodivisas.data.AppDatabase
import com.example.proyectodivisas.data.TipoCambio
import com.example.proyectodivisas.data.TipoCambioDetalle
import com.example.proyectodivisas.retrofit.ExchangeRateService

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.room.withTransaction
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExchangeRateWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        println("WorkManager: Iniciando sincronización...") // Mensaje de log
        return withContext(Dispatchers.IO) {
            try {
                // Configura Retrofit y realiza la solicitud a la API
                println("WorkManager: Realizando solicitud a la API...") // Mensaje de log
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://v6.exchangerate-api.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val service = retrofit.create(ExchangeRateService::class.java)
                val response = service.getLatestRates("MXN")

                if (response.isSuccessful) {
                    val exchangeRate = response.body()
                    if (exchangeRate != null) {
                        println("WorkManager: Datos recibidos de la API") // Mensaje de log

                        // Guarda los datos en la base de datos
                        val db = AppDatabase.getDatabase(applicationContext)
                        db.withTransaction {
                            val id = db.tipoCambioDao().insertTipoCambio(
                                TipoCambio(
                                    timeLastUpdate = exchangeRate.time_last_update_unix,
                                    timeNextUpdate = exchangeRate.time_next_update_unix,
                                    baseCode = exchangeRate.base_code
                                )
                            )
                            val detalles = exchangeRate.conversion_rates.map { (codigo, valor) ->
                                TipoCambioDetalle(
                                    idTipoCambio = id,
                                    codigoDeMoneda = codigo,
                                    valor = valor
                                )
                            }
                            db.tipoCambioDao().insertDetalles(detalles)
                        }

                        println("WorkManager: Sincronización completada con éxito") // Mensaje de log
                        Result.success()
                    } else {
                        println("WorkManager: Respuesta de la API vacía") // Mensaje de log
                        Result.failure()
                    }
                } else {
                    println("WorkManager: Error en la respuesta de la API") // Mensaje de log
                    Result.failure()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println("WorkManager: Error durante la sincronización - ${e.message}") // Mensaje de log
                Result.failure()
            }
        }
    }
}