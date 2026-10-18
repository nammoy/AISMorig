package com.app.aisheildme.bytrix.tier2_vault.security

import android.content.Context
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File

/**
 * One-time, non-destructive encryption of the pre-existing plaintext database.
 *
 * sqlcipher_export copies every table, index and row into an encrypted file; only after that
 * succeeds is the plaintext file replaced. If anything fails, the original file is left untouched
 * and the exception propagates (the app must not silently start over with an empty vault).
 */
object DatabaseEncryption {

    private val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    fun loadNativeLibrary() {
        System.loadLibrary("sqlcipher")
    }

    fun isPlaintext(file: File): Boolean {
        if (!file.exists() || file.length() < SQLITE_HEADER.size) return false
        val header = ByteArray(SQLITE_HEADER.size)
        file.inputStream().use { if (it.read(header) != header.size) return false }
        return header.contentEquals(SQLITE_HEADER)
    }

    fun encryptInPlaceIfNeeded(context: Context, name: String, passphrase: String) {
        val db = context.getDatabasePath(name)
        if (!isPlaintext(db)) return
        require(passphrase.all { it in '0'..'9' || it in 'a'..'f' }) { "Unexpected passphrase format" }

        val target = File(db.parentFile, "$name.encrypting")
        sidecars(target).forEach { it.delete() }
        target.delete()

        val plain = SQLiteDatabase.openOrCreateDatabase(db, "", null, null)
        try {
            val version = plain.version
            plain.execSQL("ATTACH DATABASE '${target.absolutePath.replace("'", "''")}' AS encrypted KEY '$passphrase'")
            plain.rawQuery("SELECT sqlcipher_export('encrypted')", null as Array<String>?).use { it.moveToFirst() }
            plain.execSQL("PRAGMA encrypted.user_version = $version")
            plain.execSQL("DETACH DATABASE encrypted")
        } finally {
            plain.close()
        }

        val backup = File(db.parentFile, "$name.plaintext")
        check(db.renameTo(backup)) { "Could not stage the plaintext database" }
        sidecars(db).forEach { it.delete() }
        if (!target.renameTo(db)) {
            backup.renameTo(db)
            error("Could not install the encrypted database")
        }
        backup.delete()
    }

    private fun sidecars(file: File): List<File> =
        listOf("-wal", "-shm", "-journal").map { File(file.path + it) }
}
