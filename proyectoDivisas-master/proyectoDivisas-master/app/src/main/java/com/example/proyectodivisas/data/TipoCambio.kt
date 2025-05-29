package com.example.proyectodivisas.data


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tipo_cambio")
data class TipoCambio(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timeLastUpdate: Long,
    val timeNextUpdate: Long,
    val baseCode: String
)