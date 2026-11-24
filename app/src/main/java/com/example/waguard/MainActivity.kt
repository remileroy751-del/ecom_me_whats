package com.example.waguard

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "WA Archive"
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 40)
        }
        status = TextView(this).apply { textSize = 16f; setPadding(0, 0, 0, 30) }
        root.addView(status)

        fun btn(label: String, action: () -> Unit) {
            root.addView(Button(this).apply {
                text = label
                setOnClickListener { action() }
            })
        }
        btn("1. Autoriser l'accès aux notifications") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        btn("2. Autoriser l'accès aux fichiers") { askStorage() }
        btn("3. Désactiver l'optimisation batterie (WA Archive)") {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
        btn("💬 Messages archivés") { startActivity(Intent(this, MessagesActivity::class.java)) }
        btn("🎞 Médias (photos, vocaux, vidéos)") { startActivity(Intent(this, MediaActivity::class.java)) }

        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        val notif = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?.contains(packageName) == true
        val files = hasStorage()
        status.text = "Notifications : ${if (notif) "✅" else "❌"}\nFichiers : ${if (files) "✅" else "❌"}\n\n" +
            if (notif && files) "Tout est actif. L'archivage tourne en arrière-plan."
            else "Active les points marqués ❌ ci-dessous."
        NotifService.instance?.startWatcher()
    }

    private fun hasStorage(): Boolean =
        if (Build.VERSION.SDK_INT >= 30) Environment.isExternalStorageManager()
        else checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    private fun askStorage() {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                startActivity(
                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName"))
                )
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 1
            )
        }
    }
}
