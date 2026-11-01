package com.hesabyar.app

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var db: InstallmentDb
    private lateinit var list: LinearLayout
    private var selectedDate = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_MONTH, 7)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = InstallmentDb(this)
        requestNotificationPermission()
        ReminderScheduler.scheduleAll(this)

        val scroll = ScrollView(this)

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
            layoutDirection = LinearLayout.LAYOUT_DIRECTION_RTL
            setBackgroundColor(0xFFF6F7FB.toInt())
        }

        scroll.addView(page)
        setContentView(scroll)

        page.addView(label("حساب‌یار", 28, true))
        page.addView(label("مدیریت اقساط و یادآوری پرداخت", 15, false))
        page.addView(space())

        val titleInput = EditText(this).apply {
            hint = "نام قسط (مثلاً وام بانک)"
        }

        val amountInput = EditText(this).apply {
            hint = "مبلغ قسط (تومان)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        val dateButton = Button(this).apply {
            text = "تاریخ سررسید: ${dateText(selectedDate.timeInMillis)}"
        }

        dateButton.setOnClickListener {
            DatePickerDialog(
                this,
                { _, y, m, d ->
                    selectedDate.set(y, m, d, 9, 0, 0)
                    dateButton.text =
                        "تاریخ سررسید: ${dateText(selectedDate.timeInMillis)}"
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        val reminderInput = EditText(this).apply {
            hint = "چند روز قبل یادآوری شود؟"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("2")
        }

        val addButton = Button(this).apply {
            text = "ثبت قسط و فعال‌سازی یادآوری"
        }

        addButton.setOnClickListener {
            val name = titleInput.text.toString().trim()
            val amount = amountInput.text.toString().toLongOrNull()

            if (name.isBlank() || amount == null || amount <= 0) {
                Toast.makeText(
                    this,
                    "نام و مبلغ معتبر وارد کن",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val days = reminderInput.text.toString()
                .toIntOrNull()?.coerceIn(0, 30) ?: 2

            val id = db.add(
                name,
                amount,
                selectedDate.timeInMillis,
                days
            )

            db.all().firstOrNull { it.id == id }?.let {
                ReminderScheduler.schedule(this, it)
            }

            titleInput.text.clear()
            amountInput.text.clear()

            render()

            Toast.makeText(
                this,
                "قسط ثبت شد",
                Toast.LENGTH_SHORT
            ).show()
        }

        page.addView(label("ثبت قسط جدید", 19, true))
        page.addView(titleInput)
        page.addView(amountInput)
        page.addView(dateButton)
        page.addView(reminderInput)
        page.addView(addButton)

        page.addView(space())
        page.addView(label("اقساط ثبت‌شده", 20, true))

        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        page.addView(list)

        render()
    }

    private fun render() {
        list.removeAllViews()

        val items = db.all()

        if (items.isEmpty()) {
            list.addView(
                label("هنوز قسطی ثبت نکرده‌ای.", 15, false)
            )
        }

        items.forEach { item ->

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 16, 20, 16)
                setBackgroundColor(0xFFFFFFFF.toInt())
            }

            card.addView(label(item.title, 18, true))

            card.addView(
                label(
                    "مبلغ: ${item.amount} تومان",
                    15,
                    false
                )
            )

            card.addView(
                label(
                    "سررسید: ${dateText(item.dueMillis)}",
                    14,
                    false
                )
            )

            val paid = item.paidMonth ==
                SimpleDateFormat("yyyy-MM", Locale.US)
                    .format(Date(item.dueMillis))

            card.addView(
                label(
                    if (paid)
                        "وضعیت این ماه: پرداخت شده"
                    else
                        "وضعیت این ماه: پرداخت نشده",
                    14,
                    false
                )
            )

            val paidButton = Button(this).apply {
                text = "ثبت پرداخت این ماه"
                isEnabled = !paid
            }

            paidButton.setOnClickListener {
                db.markPaid(item)

                db.all().firstOrNull { it.id == item.id }?.let {
                    ReminderScheduler.schedule(this@MainActivity, it)
                }

                render()

                Toast.makeText(
                    this,
                    "پرداخت در سوابق ثبت شد",
                    Toast.LENGTH_SHORT
                ).show()
            }

            card.addView(paidButton)
            list.addView(card)
            list.addView(space())
        }
    }

    private fun requestNotificationPermission() {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                100
            )
        }
    }

    private fun label(
        text: String,
        size: Int,
        bold: Boolean
    ) = TextView(this).apply {

        this.text = text
        textSize = size.toFloat()
        gravity = Gravity.RIGHT

        setTextColor(0xFF172033.toInt())

        if (bold) {
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        setPadding(4, 10, 4, 10)
    }

    private fun space() = TextView(this).apply {
        height = 16
    }

    private fun dateText(millis: Long) =
        SimpleDateFormat(
            "yyyy/MM/dd",
            Locale.getDefault()
        ).format(Date(millis))

    override fun onDestroy() {
        db.close()
        super.onDestroy()
    }
}
