package com.hesabyar.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val id = intent.getLongExtra("id", 0L)
        val type = intent.getIntExtra("type", 0)
        val title = intent.getStringExtra("title") ?: "قسط شما"

        val channelId = "hesabyar_reminders"

        val manager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "یادآوری اقساط",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val message = if (type == 0) {
            "موعد پرداخت قسط «$title» نزدیک است."
        } else {
            "امروز موعد پرداخت قسط «$title» است."
        }

        val openApp = PendingIntent.getActivity(
            context,
            id.toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(context, channelId)
        } else {
            Notification.Builder(context)
        }

        val notification = builder
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("یادآوری حساب‌یار")
            .setContentText(message)
            .setStyle(
                Notification.BigTextStyle().bigText(message)
            )
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()

        manager.notify(
            (id * 10 + type).toInt(),
            notification
        )

        // پس از اعلان روز سررسید، یادآوری ماه بعد تنظیم می‌شود.
        if (type == 1) {
            val db = InstallmentDb(context)

            try {
                db.all()
                    .firstOrNull { it.id == id }
                    ?.let {
                        ReminderScheduler.schedule(context, it)
                    }
            } finally {
                db.close()
            }
        }
    }
}
