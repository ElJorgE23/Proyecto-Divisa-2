package com.example.proyectodivisas

import android.annotation.SuppressLint
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.proyectodivisas.data.DivisasContentProvider
import com.example.proyectodivisas.ui.theme.ProyectoDivisasTheme
import com.example.proyectodivisas.worker.ExchangeRateWorker
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.viewinterop.AndroidView
import androidx.work.ExistingPeriodicWorkPolicy
import com.github.mikephil.charting.formatter.ValueFormatter
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.datetime.date.datepicker
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Programa el Worker para que se ejecute cada hora, asegurando que sólo exista uno
        val workRequest = PeriodicWorkRequestBuilder<ExchangeRateWorker>(
            1, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ExchangeRateWorker", // Nombre único para el worker
            ExistingPeriodicWorkPolicy.KEEP, // Mantiene el worker existente si ya está en cola
            workRequest
        )

        setContent {
            ProyectoDivisasTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        Greeting(
                            name = "Sincronización programada cada hora",
                            modifier = Modifier.padding(16.dp)
                        )
                        // Mostrar la interfaz de consulta
                        TipoCambioList()
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "¡Hola, $name!",
        modifier = modifier
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("Range")
@Composable
fun TipoCambioList() {

    // Obtener el contexto
    val context = LocalContext.current

    var count by remember { mutableStateOf(0) }

    val startStamp =  0L
    val endStamp = 10000000000L
    val codigoMoneda = "MXN"

    LaunchedEffect(Unit) {
        val cursor: Cursor? = withContext(Dispatchers.IO) {
            // Realizar la consulta en un hilo de fondo
            context.contentResolver.query(
                DivisasContentProvider.CONTENT_URI,
                null,
                "codigoDeMoneda = ? AND timeLastUpdate BETWEEN ? AND ?",
                arrayOf(codigoMoneda, startStamp.toString(), endStamp.toString()), // Ejemplo de fechas en UNIX timestamp
                null
            )
        }

        // Procesar el cursor y actualizar el estado
        cursor?.use {
         // Formato de fecha deseado
            while (it.moveToNext()) {
                count = count + 1
            }

        }


    }
    Text("Se encontraron {$count} de {$codigoMoneda}")


}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ProyectoDivisasTheme {
        Greeting("Android")
    }
}
