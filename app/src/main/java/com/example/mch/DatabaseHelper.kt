package com.example.mch

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Copia la base de datos precargada (assets/databases/mch.db) al almacenamiento
 * interno de la app la primera vez que se ejecuta, y luego permite abrirla
 * en modo lectura para hacer las consultas.
 */
class DatabaseHelper(private val context: Context) {

    companion object {
        private const val DB_NAME = "mch.db"
        private const val DB_ASSET_PATH = "databases/mch.db"
    }

    private val dbPath: String
        get() = context.getDatabasePath(DB_NAME).path

    /** Copia el archivo desde assets si todavía no existe en el dispositivo. */
    fun copyDatabaseIfNeeded() {
        val dbFile = File(dbPath)
        if (dbFile.exists()) return

        dbFile.parentFile?.mkdirs()

        try {
            context.assets.open(DB_ASSET_PATH).use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: IOException) {
            throw RuntimeException("No se pudo copiar la base de datos desde assets", e)
        }
    }

    fun openDatabase(): SQLiteDatabase {
        copyDatabaseIfNeeded()
        return try {
            SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY)
        } catch (e: SQLiteException) {
            throw RuntimeException("No se pudo abrir la base de datos", e)
        }
    }
}
