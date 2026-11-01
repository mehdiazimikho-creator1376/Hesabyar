package com.hesabyar.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object ReminderScheduler {

    fun scheduleAll(context: Context) {
        InstallmentDb(context).use { db ->
            db.all().forEach { schedule(context, it) }
        }
    }

    fun schedule(context: Context, item: Installment) {
        val now = System.currentTimeMillis()

        val due = nextFutureDue(item.dueMillis, now)
        val calendar = Calendar.getInstance().apply {
            timeInMillis = due
        }

        val dueTime = calendar.timeInMillis

        val reminderTime = Calendar.getInstance().apply {
            timeInMillis = dueTime
            add(Calendar.DAY_OF_MONTH, -item.reminderDays)
        }.timeInMillis

        if (reminderTime > now) {
            setAlarm(context, item, reminderTime, 0)
        }

        if (dueTime > now) {
            setAlarm(context, item, dueTime, 1)
        }
    }

    private fun nextFutureDue(
        originalDue: Long,
        now: Long
    ): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = originalDue
        }

        while (calendar.timeInMillis <= now) {
            calendar.add(Calendar.MONTH, 1)
        }

        return calendar.timeInMillis
    }

    private fun setAlarm(
        context: Context,
        item: Installment,
        time: Long,
        type: Int
    ) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("id", item.id)
            putExtra("title", item.title)
            putExtra("amount", item.amount)
            putExtra("type", type)
        }

        val pending = PendingIntent.getBroadcast(
            context,
            (item.id * 10 + type).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val alarm = context.getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager

        if (
            Build.VERSION.SDK_INT >= 31 &&
            !alarm.canScheduleExactAlarms()
        ) {
            alarm.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                time,
                pending
            )
        } else {
            alarm.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                time,
                pending
            )
        }
    }
}
