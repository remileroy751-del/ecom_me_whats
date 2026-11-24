package com.example.waguard

import android.app.Activity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ListView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessagesActivity : Activity() {

    private lateinit var list: ListView
    private var onlyDeleted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Messages"
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val cb = CheckBox(this).apply {
            text = "Seulement les messages supprimés"
            setPadding(30, 30, 30, 30)
            setOnCheckedChangeListener { _, checked -> onlyDeleted = checked; load() }
        }
        list = ListView(this)
        root.addView(cb)
        root.addView(list)
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        val fmt = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        val items = Db.get(this).list(onlyDeleted).map { m ->
            (if (m.deleted) "🗑 SUPPRIMÉ  " else "") + m.chat +
                (if (m.sender != m.chat) " · ${m.sender}" else "") +
                "\n" + m.text + "\n" + fmt.format(Date(m.time))
        }
        list.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
    }
}
