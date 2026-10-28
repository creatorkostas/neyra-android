package dev.neyra.companion.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import dev.neyra.companion.core.*
import java.io.File
import java.util.UUID

/** Durable local notes and outbox. Never contains pairing tokens or chat history. */
class CompanionDatabase private constructor(context: Context) : SQLiteOpenHelper(context, "companion.sqlite", null, 1) {
    companion object {
        @Volatile private var instance: CompanionDatabase? = null
        fun get(context: Context) = instance ?: synchronized(this) {
            instance ?: CompanionDatabase(context.applicationContext).also { instance = it }
        }
    }
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE notes(machine TEXT, id TEXT, body TEXT, time TEXT, title TEXT, PRIMARY KEY(machine,id))")
        db.execSQL("CREATE TABLE outbox(id TEXT PRIMARY KEY, machine TEXT, kind TEXT, body TEXT, audio TEXT, created INTEGER, error TEXT)")
    }
    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) { error("Unsupported database upgrade") }
    @Synchronized fun cache(snapshot: Snapshot) {
        writableDatabase.beginTransaction()
        try {
            writableDatabase.delete("notes", "machine=?", arrayOf(snapshot.machineId))
            snapshot.notes.forEach { note -> writableDatabase.insertOrThrow("notes", null, ContentValues().apply {
                put("machine", note.machineId); put("id", note.id); put("body", note.text); put("time", note.time); put("title", note.title)
            }) }
            writableDatabase.setTransactionSuccessful()
        } finally { writableDatabase.endTransaction() }
    }
    @Synchronized fun notes(): List<Note> = readableDatabase.rawQuery("SELECT machine,id,body,time,title FROM notes ORDER BY time DESC", null).use { cursor ->
        buildList { while (cursor.moveToNext()) add(Note(cursor.getString(0),cursor.getString(1),cursor.getString(2),cursor.getString(3),cursor.getString(4))) }
    }
    @Synchronized fun enqueue(machine: String, text: String = "", audio: File? = null): String {
        if (audio != null) queue().firstOrNull { it.audioPath == audio.absolutePath }?.let { return it.id }
        require(queue().size < 100) { "Outbox is full. Send or delete queued notes first." }
        val id = UUID.randomUUID().toString()
        writableDatabase.insertOrThrow("outbox", null, ContentValues().apply {
            put("id", id);put("machine", machine);put("kind", if (audio == null) "NOTE" else "AUDIO");put("body", text)
            put("audio", audio?.absolutePath);put("created", System.currentTimeMillis());put("error", "")
        })
        return id
    }
    @Synchronized fun queue(): List<QueuedCapture> = readableDatabase.rawQuery("SELECT id,machine,kind,body,audio,created,error FROM outbox ORDER BY created", null).use { cursor ->
        buildList { while (cursor.moveToNext()) add(QueuedCapture(cursor.getString(0),cursor.getString(1),cursor.getString(2),cursor.getString(3),cursor.getString(4),cursor.getLong(5),cursor.getString(6))) }
    }
    @Synchronized fun transcribed(id: String, text: String) { writableDatabase.update("outbox", ContentValues().apply { put("body",text);put("kind","NOTE") }, "id=?", arrayOf(id)) }
    @Synchronized fun error(id: String, message: String) { writableDatabase.update("outbox", ContentValues().apply { put("error",message.take(1000)) }, "id=?", arrayOf(id)) }
    @Synchronized fun remove(id: String) {
        queue().find { it.id == id }?.audioPath?.let { File(it).delete() }
        writableDatabase.delete("outbox", "id=?", arrayOf(id))
    }
    @Synchronized fun forget(machine: String) {
        queue().filter { it.machineId == machine }.forEach { remove(it.id) }
        writableDatabase.delete("notes", "machine=?", arrayOf(machine))
    }
}
