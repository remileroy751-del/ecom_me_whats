package com.example.waguard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaActivity : Activity() {

    private lateinit var lv: ListView
    private var files: List<File> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Médias"
        lv = ListView(this)
        setContentView(lv)
        lv.setOnItemClickListener { _, _, pos, _ ->
            startActivity(Intent(this, ViewerActivity::class.java).putExtra("path", files[pos].absolutePath))
        }
    }

    override fun onResume() {
        super.onResume()
        val dir = File(filesDir, "media")
        files = (dir.listFiles() ?: emptyArray())
            .filter { !it.name.endsWith(".part") }
            .sortedByDescending { it.lastModified() }
        val gone = getSharedPreferences("wa", MODE_PRIVATE).getStringSet("gone", emptySet()) ?: emptySet()
        val fmt = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        val labels = files.map { f ->
            val ext = f.name.substringAfterLast('.').lowercase()
            val icon = when (ext) {
                "mp4", "3gp" -> "🎬"
                "opus", "ogg", "m4a", "mp3", "aac" -> "🎤"
                else -> "🖼"
            }
            "$icon ${f.name}\n${fmt.format(Date(f.lastModified()))}" +
                if (f.name in gone) "   🗑 retiré de WhatsApp" else ""
        }
        lv.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, labels)
    }
}
