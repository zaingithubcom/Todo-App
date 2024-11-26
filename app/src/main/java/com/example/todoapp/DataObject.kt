package com.example.todoapp

object DataObject {
    private val dataList: MutableList<cardInfo> = mutableListOf()

    @Synchronized
    fun getAllData(): List<cardInfo> {
        return dataList.toList() // Return a copy to avoid external modification
    }

    @Synchronized
    fun getData(position: Int): cardInfo? {
        return dataList.getOrNull(position) // Safely handle out-of-bounds access
    }

    @Synchronized
    fun setData(title: String, priority: String, date: String, time: String) {
        dataList.add(cardInfo(title, priority, date, time))
    }

    @Synchronized
    fun updateData(position: Int, title: String, priority: String, date: String, time: String) {
        if (position in dataList.indices) {
            val card = dataList[position]
            dataList[position] = card.copy(title = title, priority = priority, date = date, time = time)
        }
    }

    @Synchronized
    fun deleteData(position: Int) {
        if (position in dataList.indices) {
            dataList.removeAt(position)
        }
    }

    @Synchronized
    fun deleteAll() {
        dataList.clear()
    }
}
