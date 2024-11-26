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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Calendar

class UpdateCard : AppCompatActivity() {

    private lateinit var taskDao: TaskDao

    private lateinit var selectDate: TextView
    private lateinit var selectTime: TextView
    private lateinit var createTitle: EditText
    private lateinit var createPriority: EditText
    private lateinit var updateButton: Button
    private lateinit var deleteButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_card)

        val db = TaskDatabase.getDatabase(this)
        taskDao = db.taskDao()

        selectDate = findViewById(R.id.selectDate)
        selectTime = findViewById(R.id.selectTime)
        createTitle = findViewById(R.id.create_title)
        createPriority = findViewById(R.id.create_priority)
        updateButton = findViewById(R.id.update_button)
        deleteButton = findViewById(R.id.delete_button)

        val taskId = intent.getIntExtra("id", -1)
        if (taskId != -1) {
            lifecycleScope.launch {
                val task = taskDao.getTaskById(taskId)
                if (task != null) {
                    createTitle.setText(task.title)
                    createPriority.setText(task.priority)
                    selectDate.text = task.date
                    selectTime.text = task.time
                }
            }

            selectDate.setOnClickListener {
                showDatePicker()
            }

            selectTime.setOnClickListener {
                showTimePicker()
            }

            deleteButton.setOnClickListener {
                lifecycleScope.launch {
                    val task = taskDao.getTaskById(taskId)
                    if (task != null) {
                        taskDao.deleteTask(task)
                        cancelScheduledNotification(taskId)  // Cancel the notification if the task is deleted
                        navigateToMain()
                    } else {
                        showToast("Task not found")
                    }
                }
            }

            updateButton.setOnClickListener {
                val updatedTask = Task(
                    id = taskId,
                    title = createTitle.text.toString(),
                    priority = createPriority.text.toString(),
                    date = selectDate.text.toString(),
                    time = selectTime.text.toString()
                )
                lifecycleScope.launch {
                    val existingTask = taskDao.getTaskById(taskId)
                    if (existingTask != null) {
                        taskDao.updateTask(updatedTask)
                        scheduleNotificationForTask(updatedTask)  // Schedule new notification
                        navigateToMain()
                    } else {
                        showToast("Task not found")
                    }
                }
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                selectDate.text = "$selectedDay/${selectedMonth + 1}/$selectedYear"
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                selectTime.text = String.format("%02d:%02d", selectedHour, selectedMinute)
            },
            hour, minute, true
        )
        timePickerDialog.show()
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Finish the current activity to remove it from the back stack
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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

    private fun cancelScheduledNotification(taskId: Int) {
        val notificationIntent = Intent(this, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            taskId,
            notificationIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
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
