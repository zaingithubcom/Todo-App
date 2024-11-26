package com.example.todoapp

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var taskDao: TaskDao
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var taskRecyclerView: RecyclerView
    private lateinit var addButton: Button
    private lateinit var deleteAllButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val db = TaskDatabase.getDatabase(this)
        taskDao = db.taskDao()

        // Initialize views
        taskRecyclerView = findViewById(R.id.recycler_view)
        addButton = findViewById(R.id.add)
        deleteAllButton = findViewById(R.id.deleteAll)

        // Set up the RecyclerView
        taskRecyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize the adapter with an empty list
        taskAdapter = TaskAdapter(emptyList())
        taskRecyclerView.adapter = taskAdapter

        // Set click listener for the add button
        addButton.setOnClickListener {
            val intent = Intent(this, CreateCard::class.java)
            startActivity(intent)
        }

        // Set click listener for the delete all button
        deleteAllButton.setOnClickListener {
            lifecycleScope.launch {
                taskDao.deleteAllTasks()
                updateRecyclerView()
            }
        }
    }

    private fun updateRecyclerView() {
        lifecycleScope.launch {
            val tasks = taskDao.getAllTasks()
            taskAdapter.updateData(tasks)
        }
    }

    override fun onResume() {
        super.onResume()
        updateRecyclerView()
    }
    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationPermission = android.Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(notificationPermission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(notificationPermission), REQUEST_CODE_NOTIFICATION_PERMISSION)
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_NOTIFICATION_PERMISSION = 1
    }
}