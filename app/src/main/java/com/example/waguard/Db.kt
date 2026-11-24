package com.example.waguard

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Msg(val chat: String, val sender: String, val text: String, val time: Long, val deleted: Boolean)

class Db private constructor(ctx: Context) : SQLiteOpenHelper(ctx, "wa.db", null, 1) {

    companion object {
        @Volatile private var inst: Db? = null
        fun get(ctx: Context): Db = inst ?: synchronized(this) {
            inst ?: Db(ctx.applicationContext).also { inst = it }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE msg(id INTEGER PRIMARY KEY AUTOINCREMENT, chat TEXT NOT NULL, " +
                "sender TEXT, text TEXT, time INTEGER, deleted INTEGER DEFAULT 0, k TEXT UNIQUE)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    @Synchronized
    fun add(chat: String, sender: String, text: String, time: Long) {
        val cv = ContentValues().apply {
            put("chat", chat); put("sender", sender); put("text", text)
            put("time", time); put("k", "$chat|$time|$text")
        }
        writableDatabase.insertWithOnConflict("msg", null, cv, SQLiteDatabase.CONFLICT_IGNORE)
    }

    @Synchronized
    fun markDeleted(chat: String, time: Long) {
        val cv = ContentValues().apply { put("deleted", 1) }
        val n = writableDatabase.update("msg", cv, "chat=? AND time=?", arrayOf(chat, time.toString()))
        if (n == 0) {
            // Pas de correspondance exacte : on marque le dernier message du chat
            writableDatabase.execSQL(
                "UPDATE msg SET deleted=1 WHERE id=(SELECT id FROM msg WHERE chat=? AND deleted=0 ORDER BY time DESC LIMIT 1)",
                arrayOf(chat)
            )
        }
    }

    @Synchronized
    fun list(onlyDeleted: Boolean): List<Msg> {
        val out = ArrayList<Msg>()
        val where = if (onlyDeleted) "deleted=1" else null
        readableDatabase.query("msg", null, where, null, null, null, "time DESC", "1000").use { c ->
            while (c.moveToNext()) {
                out.add(
                    Msg(
                        c.getString(c.getColumnIndexOrThrow("chat")),
                        c.getString(c.getColumnIndexOrThrow("sender")) ?: "",
                        c.getString(c.getColumnIndexOrThrow("text")) ?: "",
                        c.getLong(c.getColumnIndexOrThrow("time")),
                        c.getInt(c.getColumnIndexOrThrow("deleted")) == 1
                    )
                )
            }
        }
        return out
    }
}
