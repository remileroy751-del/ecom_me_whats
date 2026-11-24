package com.example.waguard

import android.app.Activity
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.MediaController
import android.widget.VideoView

class ViewerActivity : Activity() {

    private var mp: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val path = intent.getStringExtra("path") ?: run { finish(); return }
        val ext = path.substringAfterLast('.').lowercase()
        when (ext) {
            "mp4", "3gp" -> {
                val v = VideoView(this)
                v.setMediaController(MediaController(this))
                v.setVideoPath(path)
                setContentView(v)
                v.start()
            }
            "opus", "ogg", "m4a", "mp3", "aac" -> {
                val btn = Button(this).apply { text = "▶ Écouter"; textSize = 24f }
                val player = MediaPlayer()
                try {
                    player.setDataSource(path)
                    player.prepare()
                } catch (e: Exception) {
                    btn.text = "Lecture impossible"
                }
                player.setOnCompletionListener { btn.text = "▶ Écouter" }
                btn.setOnClickListener {
                    if (player.isPlaying) {
                        player.pause(); btn.text = "▶ Écouter"
                    } else {
                        player.start(); btn.text = "⏸ Pause"
                    }
                }
                mp = player
                setContentView(LinearLayout(this).apply {
                    gravity = Gravity.CENTER
                    addView(btn)
                })
            }
            else -> {
                val iv = ImageView(this)
                val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                iv.setImageBitmap(BitmapFactory.decodeFile(path, opts))
                setContentView(iv)
            }
        }
    }

    override fun onDestroy() {
        mp?.release()
        super.onDestroy()
    }
}
