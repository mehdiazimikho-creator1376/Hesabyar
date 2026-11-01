package com.hesabyar.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

data class Installment(
    val id: Long,
    val title: String,
    val amount: Long,
    val dueMillis: Long,
    val reminderDays: Int,
    val paidMonth: String?
)

class InstallmentDb(context: Context) :
    SQLiteOpenHelper(context, "hesabyar.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE installments(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                amount INTEGER NOT NULL,
                due_millis INTEGER NOT NULL,
                reminder_days INTEGER NOT NULL DEFAULT 1,
                paid_month TEXT
            )
        """)

        db.execSQL("""
            CREATE TABLE payments(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                installment_id INTEGER NOT NULL,
                paid_at INTEGER NOT NULL,
                due_month TEXT NOT NULL,
                amount INTEGER NOT NULL
            )
        """)
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {}

    fun add(
        title: String,
        amount: Long,
        due: Long,
        reminderDays: Int
    ): Long {
        val values = ContentValues().apply {
            put("title", title)
            put("amount", amount)
            put("due_millis", due)
            put("reminder_days", reminderDays)
        }

        return writableDatabase.insert(
            "installments",
            null,
            values
        )
    }

    fun all(): List<Installment> {
        val result = mutableListOf<Installment>()

        readableDatabase.rawQuery(
            "SELECT * FROM installments ORDER BY due_millis",
            null
        ).use { cursor ->

            while (cursor.moveToNext()) {
                result += Installment(
                    cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("title")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("amount")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("due_millis")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("reminder_days")),
                    if (cursor.isNull(
                        cursor.getColumnIndexOrThrow("paid_month")
                    )) null
                    else cursor.getString(
                        cursor.getColumnIndexOrThrow("paid_month")
                    )
                )
            }
        }

        return result
    }

    fun markPaid(item: Installment) {
        val dueMonth = SimpleDateFormat(
            "yyyy-MM",
            Locale.US
        ).format(Date(item.dueMillis))

        val nextDue = Calendar.getInstance().apply {
            timeInMillis = item.dueMillis
            add(Calendar.MONTH, 1)
        }.timeInMillis

        val update = ContentValues().apply {
            put("paid_month", dueMonth)
            put("due_millis", nextDue)
        }

        writableDatabase.update(
            "installments",
            update,
            "id=?",
            arrayOf(item.id.toString())
        )

        val payment = ContentValues().apply {
            put("installment_id", item.id)
            put("paid_at", System.currentTimeMillis())
            put("due_month", dueMonth)
            put("amount", item.amount)
        }

        writableDatabase.insert(
            "payments",
            null,
            payment
        )
    }

    fun paymentCount(id: Long): Int {
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM payments WHERE installment_id=?",
            arrayOf(id.toString())
        ).use {
            return if (it.moveToFirst()) it.getInt(0) else 0
        }
    }
    }
