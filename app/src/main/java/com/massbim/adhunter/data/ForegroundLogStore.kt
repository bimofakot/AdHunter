/*
 * Penyimpanan log pemantau di SQLite.
 * Setiap insert langsung memangkas entri lama sehingga jumlah baris tidak pernah
 * melebihi MAX_ENTRIES. Semua akses disk berjalan di Dispatchers.IO.
 */
package com.massbim.adhunter.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.massbim.adhunter.model.ForegroundLogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ForegroundLogStore(context: Context) {

    private val helper = Helper(context.applicationContext)

    suspend fun append(
        timestamp: Long,
        packageName: String,
        appName: String,
        activityName: String?,
    ) = withContext(Dispatchers.IO) {
        val database = helper.writableDatabase
        database.beginTransaction()
        try {
            val values = ContentValues().apply {
                put(COLUMN_TIMESTAMP, timestamp)
                put(COLUMN_PACKAGE, packageName)
                put(COLUMN_APP_NAME, appName)
                put(COLUMN_ACTIVITY, activityName)
            }
            database.insertOrThrow(TABLE, null, values)
            database.execSQL(TRIM_SQL)
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }

    suspend fun loadRecent(): List<ForegroundLogEntry> = withContext(Dispatchers.IO) {
        val database = helper.readableDatabase
        val result = ArrayList<ForegroundLogEntry>()
        database.query(
            TABLE,
            COLUMNS,
            null,
            null,
            null,
            null,
            "$COLUMN_TIMESTAMP DESC, $COLUMN_ID DESC",
            MAX_ENTRIES.toString(),
        ).use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(COLUMN_ID)
            val timestampIndex = cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)
            val packageIndex = cursor.getColumnIndexOrThrow(COLUMN_PACKAGE)
            val appNameIndex = cursor.getColumnIndexOrThrow(COLUMN_APP_NAME)
            val activityIndex = cursor.getColumnIndexOrThrow(COLUMN_ACTIVITY)
            while (cursor.moveToNext()) {
                result += ForegroundLogEntry(
                    id = cursor.getLong(idIndex),
                    timestamp = cursor.getLong(timestampIndex),
                    packageName = cursor.getString(packageIndex),
                    appName = cursor.getString(appNameIndex),
                    activityName = cursor.getString(activityIndex),
                )
            }
        }
        result
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        helper.writableDatabase.delete(TABLE, null, null)
    }

    private class Helper(context: Context) : SQLiteOpenHelper(
        context,
        DATABASE_NAME,
        null,
        DATABASE_VERSION,
    ) {
        override fun onCreate(database: SQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE $TABLE (
                    $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COLUMN_TIMESTAMP INTEGER NOT NULL,
                    $COLUMN_PACKAGE TEXT NOT NULL,
                    $COLUMN_APP_NAME TEXT NOT NULL,
                    $COLUMN_ACTIVITY TEXT
                )
                """.trimIndent(),
            )
            database.execSQL(
                "CREATE INDEX idx_${TABLE}_timestamp ON $TABLE($COLUMN_TIMESTAMP)",
            )
        }

        override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }

    companion object {
        const val MAX_ENTRIES = 2000

        private const val DATABASE_NAME = "foreground_log.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE = "foreground_log"
        private const val COLUMN_ID = "_id"
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_PACKAGE = "package_name"
        private const val COLUMN_APP_NAME = "app_name"
        private const val COLUMN_ACTIVITY = "activity_name"

        private val COLUMNS = arrayOf(
            COLUMN_ID,
            COLUMN_TIMESTAMP,
            COLUMN_PACKAGE,
            COLUMN_APP_NAME,
            COLUMN_ACTIVITY,
        )

        private const val TRIM_SQL =
            "DELETE FROM $TABLE WHERE $COLUMN_ID NOT IN " +
                "(SELECT $COLUMN_ID FROM $TABLE ORDER BY $COLUMN_TIMESTAMP DESC, " +
                "$COLUMN_ID DESC LIMIT $MAX_ENTRIES)"
    }
}
