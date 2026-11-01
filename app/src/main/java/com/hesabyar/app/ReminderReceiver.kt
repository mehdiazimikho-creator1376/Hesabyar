package com.hesabyar.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object ReminderScheduler {

    fun scheduleAll(context: Context) {
        val db = InstallmentDb(context)

        try {
            db.all().forEach {
                schedule(context, it)
            }
        } finally {
            db.close()
        }
    }

    fun schedule(context: Context, item: Installment) {
        val now = System.currentTimeMillis()

        val due = Calendar.getInstance().apply {
            timeInMillis = item.dueMillis
        }

        val reminder = Calendar.getInstance().apply {
            timeInMillis = item.dueMillis
            add(Calendar.DAY_OF_MONTH, -item.reminderDays)
        }

        val nextDue = nextFutureMonth(item.dueMillis, now)

        val nextReminder = Calendar.getInstance().apply {
            timeInMillis = nextDue
            add(Calendar.DAY_OF_MONTH, -item.reminderDays)
        }

        val chosenDue = if (item.dueMillis > now) {
            item.dueMillis
        } else {
            nextDue
        }

        val chosenReminder = if (reminder.timeInMillis > now) {
            reminder.timeInMillis
        } else {
            nextReminder.timeInMillis
        }

        setAlarm(context, item.id, chosenReminder, 0, item.title)
        setAlarm(context, item.id, chosenDue, 1, item.title)
    }

    private fun nextFutureMonth(
        original: Long,
        now: Long
    ): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = original
        }

        while (calendar.timeInMillis <= now) {
            calendar.add(Calendar.MONTH, 1)
        }

        return calendar.timeInMillis
    }

    private fun setAlarm(
        context: Context,
        id: Long,
        time: Long,
        type: Int,
        title: String
    ) {
        val intent = Intent(
            context,
            ReminderReceiver::class.java
        ).apply {
            putExtra("id", id)
            putExtra("type", type)
            putExtra("title", title)
        }

        val pending = PendingIntent.getBroadcast(
            context,
            (id * 10 + type).toInt(),
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
