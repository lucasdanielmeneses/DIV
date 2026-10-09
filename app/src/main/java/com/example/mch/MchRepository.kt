package com.example.mch

import android.content.Context
import java.text.Normalizer

data class Marca(val codigo: String, val nombre: String)
data class Localizacion(val motor: String, val chasis: String)

/**
 * Encapsula todas las consultas a la base de datos mch.db.
 */
class MchRepository(context: Context) {

    private val dbHelper = DatabaseHelper(context.applicationContext)

    /** Devuelve todas las marcas ordenadas alfabéticamente. */
    fun getAllMarcas(): List<Marca> {
        val marcas = mutableListOf<Marca>()
        val db = dbHelper.openDatabase()
        db.rawQuery(
            "SELECT C_MARCA, D_MARCA FROM MARCA WHERE D_MARCA IS NOT NULL ORDER BY D_MARCA COLLATE NOCASE",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                marcas.add(Marca(cursor.getString(0), cursor.getString(1)))
            }
        }
        db.close()
        return marcas
    }

    /** Devuelve los modelos (L_MODELO) disponibles para una marca dada, sin duplicados. */
    fun getModelosForMarca(codigoMarca: String): List<String> {
        val modelos = mutableListOf<String>()
        val db = dbHelper.openDatabase()
        db.rawQuery(
            "SELECT DISTINCT L_MODELO FROM LOCALIZA WHERE C_MARCA = ? AND L_MODELO IS NOT NULL ORDER BY L_MODELO COLLATE NOCASE",
            arrayOf(codigoMarca)
        ).use { cursor ->
            while (cursor.moveToNext()) {
                modelos.add(cursor.getString(0))
            }
        }
        db.close()
        return modelos
    }

    /** Busca la localización de motor y chasis para una marca + modelo exactos. */
    fun getLocalizacion(codigoMarca: String, modelo: String): Localizacion? {
        val db = dbHelper.openDatabase()
        var result: Localizacion? = null
        db.rawQuery(
            "SELECT L_MOTOR, L_CHASIS FROM LOCALIZA WHERE C_MARCA = ? AND L_MODELO = ? LIMIT 1",
            arrayOf(codigoMarca, modelo)
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                val motor = cursor.getString(0) ?: "No hay información disponible."
                val chasis = cursor.getString(1) ?: "No hay información disponible."
                result = Localizacion(motor, chasis)
            }
        }
        db.close()
        return result
    }

    companion object {
        /** Normaliza texto: mayúsculas y sin acentos, para poder filtrar/comparar sin errores. */
        fun normalize(text: String): String {
            val upper = text.uppercase()
            val normalized = Normalizer.normalize(upper, Normalizer.Form.NFD)
            return normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        }
    }
}
