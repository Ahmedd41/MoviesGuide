package com.hanynemr.apiappg2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Get movie title from intent
        val movieTitle = intent.getStringExtra("movieTitle") ?: "Movie"

        // Show Toast with movie name
        Toast.makeText(context, "Reminder: Time to watch $movieTitle", Toast.LENGTH_LONG).show()

        // Create notification
        val notification = NotificationCompat.Builder(context, "movie_reminder_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Movie Reminder")
            .setContentText("Don't miss your movie: $movieTitle")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(context).notify(movieTitle.hashCode(), notification)
            }
        } else {
            // For older Android versions
            NotificationManagerCompat.from(context).notify(movieTitle.hashCode(), notification)
        }
    }
}