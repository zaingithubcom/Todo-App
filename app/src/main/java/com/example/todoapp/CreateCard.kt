package com.example.todoapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Calendar

class CreateCard : AppCompatActivity() {

    private lateinit var taskDao: TaskDao

    private lateinit var date: TextView
    private lateinit var time: TextView
    private lateinit var saveButton: Button
    private lateinit var createTitle: EditText
    private lateinit var createPriority: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_card)

        val db = TaskDatabase.getDatabase(this)
        taskDao = db.taskDao()

        // Initialize views
        date = findViewById(R.id.date)
        time = findViewById(R.id.time)
        saveButton = findViewById(R.id.save_button)
        createTitle = findViewById(R.id.create_title)
        createPriority = findViewById(R.id.create_priority)

        // Handle date selection
        date.setOnClickListener {
            showDatePicker()
        }

        // Handle time selection
        time.setOnClickListener {
            showTimePicker()
        }

        // Save the task
        saveButton.setOnClickListener {
            val title = createTitle.text.toString().trim()
            val priority = createPriority.text.toString().trim()
            val selectedDate = date.text.toString()
            val selectedTime = time.text.toString()

            if (title.isNotEmpty() && priority.isNotEmpty() && selectedDate.isNotEmpty() && selectedTime.isNotEmpty()) {
                lifecycleScope.launch {
                    val task = Task(
                        title = title,
                        priority = priority,
                        date = selectedDate,
                        time = selectedTime
                    )
                    taskDao.insertTask(task)
                    scheduleNotificationForTask(task)  // Schedule notification
                    val intent = Intent(this@CreateCard, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    // Show Date Picker
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                date.text = "$selectedDay/${selectedMonth + 1}/$selectedYear"
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    // Show Time Picker
    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                time.text = String.format("%02d:%02d", selectedHour, selectedMinute)
            },
            hour, minute, true
        )
        timePickerDialog.show()
    }

    private fun scheduleNotificationForTask(task: Task) {
        val calendar = Calendar.getInstance().apply {
            val dateParts = task.date.split("/").map { it.toInt() }
            val timeParts = task.time.split(":").map { it.toInt() }
            set(Calendar.YEAR, dateParts[2])
            set(Calendar.MONTH, dateParts[1] - 1)
            set(Calendar.DAY_OF_MONTH, dateParts[0])
            set(Calendar.HOUR_OF_DAY, timeParts[0])
            set(Calendar.MINUTE, timeParts[1])
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val notificationIntent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra(TITLE_EXTRA, task.title)
            putExtra(MESSAGE_EXTRA, "Reminder for task: ${task.title}")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            task.id,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExactAlarms()) {
            // If can't schedule exact alarms, use set() method
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    private fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
}
