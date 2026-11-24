package com.example.waguard

import android.content.Context
import android.os.FileObserver
import java.io.File
import java.util.concurrent.Executors

/** Copie en temps réel les médias WhatsApp (photos, vidéos, vocaux) dans le stockage privé de l'app. */
class MediaWatcher(private val ctx: Context) {

    private val observers = HashMap<String, FileObserver>()
    private val exec = Executors.newSingleThreadExecutor()
    private val prefs = ctx.getSharedPreferences("wa", Context.MODE_PRIVATE)
    private val outDir = File(ctx.filesDir, "media").also { it.mkdirs() }

    private val roots = listOf(
        "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media",
        "/storage/emulated/0/WhatsApp/Media",
        "/storage/emulated/0/Android/media/com.whatsapp.w4b/WhatsApp Business/Media",
        "/storage/emulated/0/WhatsApp Business/Media"
    )
    private val skipDirs = setOf("Sent", "Private")
    private val allowed = setOf(
        "jpg", "jpeg", "png", "webp", "gif", "mp4", "3gp",
        "opus", "ogg", "m4a", "mp3", "aac"
    )

    @Synchronized
    fun start() {
        stop()
        val last = prefs.getLong("last", System.currentTimeMillis())
        exec.execute {
            for (r in roots) walk(File(r), last)
            prefs.edit().putLong("last", System.currentTimeMillis()).apply()
        }
    }

    @Synchronized
    fun stop() {
        observers.values.forEach { it.stopWatching() }
        observers.clear()
        prefs.edit().putLong("last", System.currentTimeMillis()).apply()
    }

    private fun walk(dir: File, since: Long) {
        if (!dir.isDirectory || dir.name in skipDirs) return
        watch(dir)
        val children = dir.listFiles() ?: return
        for (c in children) {
            if (c.isDirectory) walk(c, since)
            else if (c.lastModified() > since) copy(c)
        }
    }

    @Suppress("DEPRECATION")
    @Synchronized
    private fun watch(dir: File) {
        val path = dir.absolutePath
        if (observers.containsKey(path)) return
        val mask = FileObserver.CLOSE_WRITE or FileObserver.MOVED_TO or FileObserver.CREATE or
            FileObserver.DELETE or FileObserver.MOVED_FROM
        val o = object : FileObserver(path, mask) {
            override fun onEvent(event: Int, name: String?) {
                if (name == null) return
                val f = File(dir, name)
                when (event and FileObserver.ALL_EVENTS) {
                    FileObserver.CREATE ->
                        if (f.isDirectory) exec.execute { walk(f, 0L) }
                    FileObserver.CLOSE_WRITE, FileObserver.MOVED_TO ->
                        if (f.isDirectory) exec.execute { walk(f, 0L) } else exec.execute { copy(f) }
                    FileObserver.DELETE, FileObserver.MOVED_FROM ->
                        markSourceGone(name)
                }
            }
        }
        o.startWatching()
        observers[path] = o
    }

    private fun markSourceGone(name: String) {
        if (!File(outDir, name).exists()) return
        synchronized(prefs) {
            val set = HashSet(prefs.getStringSet("gone", emptySet()) ?: emptySet())
            set.add(name)
            prefs.edit().putStringSet("gone", set).apply()
        }
    }

    private fun copy(f: File) {
        try {
            val name = f.name
            if (name.startsWith(".")) return
            val ext = name.substringAfterLast('.', "").lowercase()
            if (ext !in allowed) return
            val dest = File(outDir, name)
            if (dest.exists()) return
            var size = -1L
            var tries = 0
            while (tries < 10 && f.length() != size) {
                size = f.length(); Thread.sleep(150); tries++
            }
            val tmp = File(outDir, "$name.part")
            f.inputStream().use { i -> tmp.outputStream().use { o -> i.copyTo(o) } }
            tmp.renameTo(dest)
            dest.setLastModified(f.lastModified())
            val prev = prefs.getLong("last", 0L)
            if (f.lastModified() > prev) prefs.edit().putLong("last", f.lastModified()).apply()
        } catch (_: Exception) {
        }
    }
}
