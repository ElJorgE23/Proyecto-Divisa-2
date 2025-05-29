package com.example.appcliente

import android.annotation.SuppressLint
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.datetime.date.datepicker
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import androidx.core.net.toUri
import com.example.appcliente.ui.theme.AppClienteTheme

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults

// URI del ContentProvider remoto (de proyectodivisas)
private val CONTENT_URI = "content://com.example.proyectodivisas.provider/tipo_cambio_detalle".toUri()

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppClienteTheme() {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        Greeting(
                            name = "Divisas",
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


    // Estado para almacenar los resultados de la consulta
    val (tipoCambioList, setTipoCambioList) = remember { mutableStateOf<List<String>>(emptyList()) }

    // Estado para controlar cuándo realizar la consulta
    val (consultar, setConsultar) = remember { mutableStateOf(false) }

    // Estado para la gráfica
    val (chartData, setChartData) = remember { mutableStateOf<List<Entry>>(emptyList()) }

    // Obtener el contexto
    val context = LocalContext.current

    var selectedCurrency by remember { mutableStateOf(currencies[0]) }

    // Estados para los DatePickers
    val startDateDialog = rememberMaterialDialogState()
    val endDateDialog = rememberMaterialDialogState()
    val (startDate, setStartDate) = remember { mutableStateOf<LocalDate?>(null) }
    val (endDate, setEndDate) = remember { mutableStateOf<LocalDate?>(null) }
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    Log.d("DebugContentProvider", "Consultando con $selectedCurrency")

    // Si se activa la consulta, se lanza la corrutina
    if (consultar) {
        // Convertir las fechas a timestamp (en segundos)
        // Se asume que timeLastUpdate está en milisegundos; se convierte dividiendo entre 86.400.000 si solo queremos fecha
        // Pero para incluir horas, usamos directamente el timestamp en milisegundos y lo convertimos a LocalDateTime
        val startStamp = startDate?.atStartOfDay(ZoneOffset.UTC)?.toEpochSecond() ?: 0L
        val endStamp = endDate?.atStartOfDay(ZoneOffset.UTC)?.toEpochSecond() ?: 10000000000L
        Log.d("DebugContentProvider", "Consultando con startStamp: $startStamp y endStamp: $endStamp")

        LaunchedEffect(Unit) {
            try {
                val cursor: Cursor? = withContext(Dispatchers.IO) {
                    // Realizar la consulta al ContentProvider remoto
                    context.contentResolver.query(
                        CONTENT_URI,
                        null,
                        "codigoDeMoneda = ? AND timeLastUpdate BETWEEN ? AND ?",
                        arrayOf(selectedCurrency, startStamp.toString(), endStamp.toString()),
                        null
                    )
                }

                if (cursor == null) {
                    Log.e("DebugContentProvider", "El cursor es null.")
                } else {
                    Log.d("DebugContentProvider", "Cursor obtenido. Cantidad de registros: ${cursor.count}")
                }

                // Procesar el cursor y actualizar los estados
                cursor?.use {
                    val resultados = mutableListOf<String>()
                    val entries = mutableListOf<Entry>()
                    var index = 0f
                    // Formato para mostrar la fecha en la lista
                    val dateFormatterOutput = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                    while (it.moveToNext()) {
                        val moneda = it.getString(it.getColumnIndex("codigoDeMoneda"))
                        val valor = it.getDouble(it.getColumnIndex("valor"))
                        val fechaLong = it.getLong(it.getColumnIndex("timeLastUpdate"))

                        // Convertir el timestamp (milisegundos) a LocalDateTime para el texto de la lista
                        val fecha = LocalDateTime.ofInstant(Instant.ofEpochMilli(fechaLong), ZoneId.systemDefault())
                        val fechaFormateada = fecha.format(dateFormatterOutput)

                        resultados.add("Moneda: $moneda, Valor: $valor, Fecha: $fechaFormateada")

                        // Convertir la fecha a segundos para usarla como coordenada X en la gráfica
                        val timestampSeconds = (fechaLong / 1000).toFloat()
                        entries.add(Entry(timestampSeconds, valor.toFloat()))
                    }
                    setTipoCambioList(resultados)
                    setChartData(entries)
                }
            } catch (e: Exception) {
                Log.e("DebugContentProvider", "Error al consultar el ContentProvider: ${e.message}", e)
            } finally {
                // Reiniciar el estado de consulta para evitar múltiples ejecuciones
                setConsultar(false)
            }
        }
    }

    // Interfaz de usuario
    Column(modifier = Modifier.padding(16.dp)) {
        CurrencyDropdown(
            currencies = currencies,
            selectedCurrency = selectedCurrency,
            onCurrencySelected = { selectedCurrency = it }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { startDateDialog.show() },
                modifier = Modifier.weight(1f)
            ) {
                Text(startDate?.format(dateFormatter) ?: "Seleccione fecha inicio")
            }
            Button(
                onClick = { endDateDialog.show() },
                modifier = Modifier.weight(1f)
            ) {
                Text(endDate?.format(dateFormatter) ?: "Seleccione fecha fin")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                // Se activa la consulta
                setConsultar(true)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Consultar")
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (chartData.isNotEmpty()) {
            LineChartView(chartData = chartData)
        }

    }

    // Diálogos para selección de fechas
    MaterialDialog(dialogState = startDateDialog, buttons = {
        positiveButton("Aceptar")
        negativeButton("Cancelar")
    }) {
        datepicker(initialDate = LocalDate.now()) { date ->
            setStartDate(date)
        }
    }
    MaterialDialog(dialogState = endDateDialog, buttons = {
        positiveButton("Aceptar")
        negativeButton("Cancelar")
    }) {
        datepicker(initialDate = LocalDate.now()) { date ->
            setEndDate(date)
        }
    }
}
@Composable
fun LineChartView(chartData: List<Entry>) {
    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            LineChart(ctx)
        },
        update = { chart ->
            val dataSet = LineDataSet(chartData, "Tipo de cambio").apply {
                setColor(android.graphics.Color.RED)
                setValueTextColor(android.graphics.Color.BLACK)
                setDrawFilled(true)
                // Aumentar el tamaño del texto de los valores
                valueTextSize = 10f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return String.format("%.3f", value)
                    }
                }
            }
            chart.data = LineData(dataSet)

            // Configurar el eje X para mostrar fecha en la parte inferior
            chart.xAxis.apply {
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                // Formateador para convertir el timestamp (segundos) a fecha y hora
                valueFormatter = object : ValueFormatter() {
                    @RequiresApi(Build.VERSION_CODES.O)
                    override fun getFormattedValue(value: Float): String {
                        // Convertir de segundos a milisegundos
                        val timestamp = (value * 1000).toLong()
                        val date = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(timestamp),
                            ZoneId.systemDefault()
                        )
                        return date.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
                    }
                }
                // Rotar las etiquetas para mostrarlas verticalmente
                labelRotationAngle = 90f
                // Aumentar el tamaño del texto del eje X
                textSize = 8f

                // Agregar padding interno ajustando el mínimo y máximo del eje X
                // Por ejemplo, se agrega un offset de 100 segundos a ambos extremos
                axisMinimum = chart.data.xMin - 20f
                axisMaximum = chart.data.xMax + 20f
            }

            // Configurar el eje Y: habilitar solo el eje izquierdo y agregar un margen interno
            chart.axisLeft.apply {
                // Calcula un offset proporcional a la diferencia entre los valores máximos y mínimos
                val yOffset = (chart.data.yMax - chart.data.yMin) * 0.1f
                axisMinimum = chart.data.yMin - yOffset
                axisMaximum = chart.data.yMax + yOffset
            }

            // Deshabilitar el eje derecho
            chart.axisRight.isEnabled = false

            // Opcional: quitar la descripción y ajustar la leyenda
            chart.description.isEnabled = false
            chart.legend.textSize = 14f

            chart.invalidate()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    )
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyDropdown(
    currencies: Array<String>,
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            readOnly = true,
            value = selectedCurrency,
            onValueChange = {},
            label = { Text("Código de Moneda") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            currencies.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(text = currency) },
                    onClick = {
                        onCurrencySelected(currency)
                        expanded = false
                    }
                )
            }
        }
    }
}



val currencies = arrayOf(
    "AED", "AFN", "ALL", "AMD", "ANG", "AOA", "ARS", "AUD", "AWG", "AZN",
    "BAM", "BBD", "BDT", "BGN", "BHD", "BIF", "BMD", "BND", "BOB", "BRL", "BSD",
    "BTN", "BWP", "BYN", "BZD", "CAD", "CDF", "CHF", "CLP", "CNY", "COP", "CRC",
    "CUP", "CVE", "CZK", "DJF", "DKK", "DOP", "DZD", "EGP", "ERN", "ETB", "EUR",
    "FJD", "FKP", "FOK", "GBP", "GEL", "GGP", "GHS", "GIP", "GMD", "GNF", "GTQ",
    "GYD", "HKD", "HNL", "HRK", "HTG", "HUF", "IDR", "ILS", "IMP", "INR", "IQD",
    "IRR", "ISK", "JEP", "JMD", "JOD", "JPY", "KES", "KGS", "KHR", "KID", "KMF",
    "KRW", "KWD", "KYD", "KZT", "LAK", "LBP", "LKR", "LRD", "LSL", "LYD", "MAD",
    "MDL", "MGA", "MKD", "MMK", "MNT", "MOP", "MRU", "MUR", "MVR", "MWK", "MYR",
    "MZN", "NAD", "NGN", "NIO", "NOK", "NPR", "NZD", "OMR", "PAB", "PEN", "PGK",
    "PHP", "PKR", "PLN", "PYG", "QAR", "RON", "RSD", "RUB", "RWF", "SAR", "SBD",
    "SCR", "SDG", "SEK", "SGD", "SHP", "SLE", "SLL", "SOS", "SRD", "SSP", "STN",
    "SYP", "SZL", "THB", "TJS", "TMT", "TND", "TOP", "TRY", "TTD", "TVD", "TWD",
    "TZS", "UAH", "UGX", "USD", "UYU", "UZS", "VES", "VND", "VUV", "WST", "XAF",
    "XCD", "XDR", "XOF", "XPF", "YER", "ZAR", "ZMW", "ZWL"
)
