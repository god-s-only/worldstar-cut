package com.worldstar.cut.features.export.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.worldstar.cut.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExportService : Service() {

    companion object {
        const val CHANNEL_ID = "export_channel"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_PROJECT_ID = "project_id"
        const val EXTRA_OUTPUT_PATH = "output_path"
        const val EXTRA_RESOLUTION = "resolution"
        const val EXTRA_FRAME_RATE = "frame_rate"

        fun start(context: Context, projectId: Long, resolution: String, frameRate: Int) {
            val intent = Intent(context, ExportService::class.java).apply {
                putExtra(EXTRA_PROJECT_ID, projectId)
                putExtra(EXTRA_RESOLUTION, resolution)
                putExtra(EXTRA_FRAME_RATE, frameRate)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ExportService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val projectId = intent?.getLongExtra(EXTRA_PROJECT_ID, -1L) ?: -1L
        val resolution = intent?.getStringExtra(EXTRA_RESOLUTION) ?: "1080p"

        startForeground(NOTIFICATION_ID, buildNotification(0, resolution))

        // TODO: Implement FFmpeg export pipeline
        // For now, stop immediately
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Video Export",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows video export progress"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(progress: Int, resolution: String): android.app.Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Exporting video...")
            .setContentText("Resolution: $resolution")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setProgress(100, progress, progress == 0)
            .build()
    }
}
