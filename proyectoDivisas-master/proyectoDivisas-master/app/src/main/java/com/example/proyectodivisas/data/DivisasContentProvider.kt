package com.example.proyectodivisas.data

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import android.util.Log
import androidx.room.Room

class DivisasContentProvider : ContentProvider() {

    // Instancia de la base de datos
    private lateinit var db: AppDatabase

    // Códigos para el UriMatcher
    private val URI_MATCHER_CODE_TIPO_CAMBIO_DETALLE = 1

    // UriMatcher para manejar las URIs
    //UriMatcher: Es un objeto que facilita la identificación y coincidencia de URIs
    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(AUTHORITY, "tipo_cambio_detalle", URI_MATCHER_CODE_TIPO_CAMBIO_DETALLE)
    }

    companion object {
        // Autoridad del ContentProvider (debe coincidir con el valor en el AndroidManifest.xml)
        const val AUTHORITY = "com.example.proyectodivisas.provider"

        // URI base para acceder a los detalles del tipo de cambio
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/tipo_cambio_detalle")
    }

    override fun onCreate(): Boolean {
        // Inicializar la base de datos
        db = Room.databaseBuilder(
            context!!.applicationContext,
            AppDatabase::class.java,
            "divisas_database"
        ).fallbackToDestructiveMigration().build()
        return true
    }

    override fun query(
        uri: Uri,// URI que invoca el método, la cual define qué datos se están solicitando.
        projection: Array<String>?,//arreglo opcional que indica qué columnas se desean en el resultado.
        selection: String?,//cadena opcional que representa la cláusula WHERE de una consulta SQL.
        selectionArgs: Array<String>?,//arreglo de argumentos que se usan para complementar la consulta
        sortOrder: String?//cadena opcional que define el orden en que se deben ordenar los resultados
    ): Cursor? {
        Log.d("DivisasContentProvider", "query() llamado con URI: $uri")
        Log.d("DivisasContentProvider", "selectionArgs: ${selectionArgs?.joinToString(", ")}")

        return when (uriMatcher.match(uri)) {
            URI_MATCHER_CODE_TIPO_CAMBIO_DETALLE -> {

                //Extracción y Validación de Parámetros desde selectionArgs
                val moneda = selectionArgs?.get(0)
                    ?: throw IllegalArgumentException("Moneda no proporcionada")
                val fechaInicio = selectionArgs?.get(1)?.toLong()
                    ?: throw IllegalArgumentException("Fecha de inicio no proporcionada")
                val fechaFin = selectionArgs?.get(2)?.toLong()
                    ?: throw IllegalArgumentException("Fecha de fin no proporcionada")

                Log.d("DivisasContentProvider", "Parámetros: moneda=$moneda, fechaInicio=$fechaInicio, fechaFin=$fechaFin")
                val cursor = db.tipoCambioDao().getTipoCambioPorMonedaYRango(moneda, fechaInicio, fechaFin)
                Log.d("DivisasContentProvider", "Cursor obtenido. Cantidad de registros: ${cursor.count}")
                cursor
            }
            else -> throw IllegalArgumentException("URI no soportada: $uri")
        }
    }


    // Los siguientes métodos no son necesarios para esta implementación
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0
}