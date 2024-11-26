package com.example.todoapp

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(private var data: List<Task>) : RecyclerView.Adapter<TaskAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.title)
        val priority: TextView = itemView.findViewById(R.id.priority)
        val date: TextView = itemView.findViewById(R.id.dateText)
        val time: TextView = itemView.findViewById(R.id.timeText)
        val layout: View = itemView.findViewById(R.id.mylayout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val viewItem = LayoutInflater.from(parent.context).inflate(R.layout.view, parent, false)
        return ViewHolder(viewItem)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]

        // Set background color based on priority
        when (item.priority.lowercase()) {
            "high" -> holder.layout.setBackgroundColor(Color.parseColor("#F05454"))
            "medium" -> holder.layout.setBackgroundColor(Color.parseColor("#EDC988"))
            else -> holder.layout.setBackgroundColor(Color.parseColor("#00917C"))
        }

        // Set task information
        holder.title.text = item.title
        holder.priority.text = "Priority: ${item.priority}"
        holder.date.text = "Date: ${item.date}"
        holder.time.text = "Time: ${item.time}"

        // Handle click to update card
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, UpdateCard::class.java)
            intent.putExtra("id", item.id) // Pass task ID instead of position
            holder.itemView.context.startActivity(intent)
        }
    }

    fun updateData(newData: List<Task>) {
        data=newData
        notifyDataSetChanged()
    }
}
