package com.example.waguard

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotifService : NotificationListenerService() {

    companion object {
        @Volatile var instance: NotifService? = null
        private val DELETED = listOf(
            "message was deleted", "message a été supprimé", "message supprimé",
            "mensaje eliminado", "mensagem foi apagada", "🚫"
        )
        private val SUMMARY = Regex("^\\d+ (new |nouveaux |nouveau )?(messages?)", RegexOption.IGNORE_CASE)
    }

    private var watcher: MediaWatcher? = null

    override fun onListenerConnected() {
        instance = this
        startWatcher()
    }

    override fun onListenerDisconnected() {
        watcher?.stop()
        instance = null
    }

    fun startWatcher() {
        if (watcher == null) watcher = MediaWatcher(applicationContext)
        watcher?.start()
    }

    private fun isDeleted(t: String): Boolean {
        val l = t.lowercase()
        return DELETED.any { l.contains(it) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != "com.whatsapp" && sbn.packageName != "com.whatsapp.w4b") return
        val n = sbn.notification
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val ex = n.extras
        val title = (ex.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)
            ?: ex.getCharSequence(Notification.EXTRA_TITLE))?.toString() ?: return
        val db = Db.get(this)

        var handled = false
        @Suppress("DEPRECATION")
        val arr = ex.getParcelableArray(Notification.EXTRA_MESSAGES)
        if (arr != null) {
            for (p in arr) {
                val b = p as? Bundle ?: continue
                val text = b.getCharSequence("text")?.toString() ?: continue
                val time = b.getLong("time", sbn.postTime)
                val sender = b.getCharSequence("sender")?.toString() ?: title
                handle(db, title, sender, text, time)
                handled = true
            }
        }
        if (!handled) {
            val text = ex.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return
            if (SUMMARY.containsMatchIn(text)) return
            handle(db, title, title, text, sbn.postTime)
        }
    }

    private fun handle(db: Db, chat: String, sender: String, text: String, time: Long) {
        if (isDeleted(text)) db.markDeleted(chat, time) else db.add(chat, sender, text, time)
    }
}
