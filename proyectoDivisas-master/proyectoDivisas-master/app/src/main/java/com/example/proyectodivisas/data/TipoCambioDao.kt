package com.example.proyectodivisas.data

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query


@Dao
interface TipoCambioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTipoCambio(tipoCambio: TipoCambio): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetalles(detalles: List<TipoCambioDetalle>)



    @Query(
        """SELECT tcd.*, tc.timeLastUpdate 
       FROM tipo_cambio_detalle tcd
       INNER JOIN tipo_cambio tc ON tcd.idTipoCambio = tc.id
       WHERE tcd.codigoDeMoneda = :moneda 
       AND tc.timeLastUpdate BETWEEN :fechaInicio AND :fechaFin"""
    )
    fun getTipoCambioPorMonedaYRango(moneda: String, fechaInicio: Long, fechaFin: Long): Cursor

}