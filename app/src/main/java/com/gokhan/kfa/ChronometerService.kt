package com.gokhan.kfa

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat

class ChronometerService : Service() {

    companion object {
        const val ACTION_START = "com.gokhan.kfa.START"
        const val ACTION_STOP = "com.gokhan.kfa.STOP"
        const val CHANNEL_ID = "ChronometerServiceChannel"
    }

    private var chronometerBaseTime: Long = 0L
    private var isChronometerRunning = false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                if (!isChronometerRunning) {
                    chronometerBaseTime = intent.getLongExtra("BASE_TIME", SystemClock.elapsedRealtime())
                    startForegroundService()
                }
            }
            ACTION_STOP -> {
                if (isChronometerRunning) {
                    stopForeground(true)
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundService() {
        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Chronometer Service")
            .setContentText("Chronometer is running...")
            .setSmallIcon(R.drawable.icon_person)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
        isChronometerRunning = true
    }


    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Chronometer Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(serviceChannel)
    }

    override fun onDestroy() {
        super.onDestroy()
        isChronometerRunning = false
    }
}
