package com.example.todoapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import java.util.Calendar

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            Log.d("BootReceiver", "Device booted. Rescheduling notifications.")
            rescheduleNotifications(context)
        }
    }

    private fun rescheduleNotifications(context: Context) {
        // Start a coroutine to handle database operations
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get the TaskDao from your database
                val taskDao = TaskDatabase.getDatabase(context).taskDao()

                // Query to get all tasks or tasks that need to be rescheduled
                val tasks = taskDao.getAllTasks() // Ensure this method is suspend

                tasks.forEach { task ->
                    scheduleNotification(context, task)
                }
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to reschedule notifications", e)
            }
        }
    }

    private fun scheduleNotification(context: Context, task: Task) {
        // Convert date and time to Calendar object
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

        // Create an intent for the notification
        val notificationIntent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(TITLE_EXTRA, task.title)
            putExtra(MESSAGE_EXTRA, "Time to complete your task: ${task.title}")
        }

        // Create a pending intent for the alarm
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Get the AlarmManager service
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Schedule the alarm
        if (canScheduleExactAlarms(context)) {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            Log.e("BootReceiver", "Cannot schedule exact alarms. Permission required.")
        }
    }

    private fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
}
