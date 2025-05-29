package com.example.proyectodivisas.data


import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "tipo_cambio_detalle",
    foreignKeys = [
        ForeignKey(
            entity = TipoCambio::class,
            parentColumns = ["id"],
            childColumns = ["idTipoCambio"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TipoCambioDetalle(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val idTipoCambio: Long,
    val codigoDeMoneda: String,
    val valor: Double
)