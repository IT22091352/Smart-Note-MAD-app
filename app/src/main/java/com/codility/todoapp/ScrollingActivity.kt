package com.codility.recyclerview


import MyAdapter
import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.codility.todoapp.R
import com.codility.todoapp.R.*
import com.codility.todoapp.databinding.ActivityScrollingBinding
import com.codility.todoapp.helper.Todo
import com.codility.todoapp.helper.TodoDao
import com.codility.todoapp.helper.TodoDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class ScrollingActivity : AppCompatActivity(), MyAdapter.OnClickListener {

    private lateinit var binding: ActivityScrollingBinding
    private lateinit var todoDao: TodoDao
    private lateinit var myAdapter: MyAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScrollingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val db = TodoDatabase.getInstance(this)
        todoDao = db.todoDao()

        setupRecyclerView()

        binding.fab.setOnClickListener {
            showNoteDialog(false, null, -1)
        }

        getTodoList()
    }

    private fun setupRecyclerView() {
        binding.list.layoutManager = LinearLayoutManager(this)
        myAdapter = MyAdapter(ArrayList())
        myAdapter.setListener(this)
        binding.list.adapter = myAdapter
    }

    private fun getTodoList() {
        CoroutineScope(Dispatchers.IO).launch {
            val todoList = todoDao.getAllTodos()
            CoroutineScope(Dispatchers.Main).launch {
                myAdapter.updateData(todoList)
            }
        }
    }

    private fun deleteConfirmation(todo: Todo) {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Confirm Delete...")
        alertDialog.setMessage("Are you sure you want to delete this?")
        alertDialog.setIcon(drawable.ic_delete)
        alertDialog.setPositiveButton("YES") { dialog, which ->
            CoroutineScope(Dispatchers.IO).launch {
                todoDao.delete(todo)
                getTodoList() // Refresh the list
            }
        }

        alertDialog.setNegativeButton("NO") { dialog, which ->
            dialog.cancel() // Cancel the dialog
        }
        alertDialog.show()
    }

    @SuppressLint("MissingInflatedId")
    private fun showNoteDialog(shouldUpdate: Boolean, todo: Todo?, position: Int) {
        val view = LayoutInflater.from(applicationContext).inflate(layout.add_todo, null)
        val alertDialogView = AlertDialog.Builder(this).create()
        alertDialogView.setView(view)

        val tvHeader = view.findViewById<TextView>(id.tvHeader)
        val edTitle = view.findViewById<EditText>(id.edTitle)
        val edPriority =view.findViewById<EditText>(id.edPriority)
        val edDesc = view.findViewById<EditText>(id.edDesc)
        val btAddUpdate = view.findViewById<Button>(id.btAddUpdate)
        val btCancel = view.findViewById<Button>(id.btCancel)

        if (shouldUpdate) btAddUpdate.text = "Update" else btAddUpdate.text = "Save"

        if (shouldUpdate && todo != null) {
            edTitle.setText(todo.title)
            edPriority.setText(todo.priority)
            edDesc.setText(todo.description)
        }

        btAddUpdate.setOnClickListener {
            val title = edTitle.text.toString()
            val priority = edPriority.text.toString()
            val desc = edDesc.text.toString()

            if (TextUtils.isEmpty(title)) {
                Toast.makeText(this, "Enter Your Title!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else if (TextUtils.isEmpty(priority)) {
                Toast.makeText(this, "Enter Your Priority!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener

            } else if (TextUtils.isEmpty(desc)) {
                Toast.makeText(this, "Enter Your Description!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newTodo = if (shouldUpdate && todo != null) {
                todo.copy(title = title, priority = priority, description = desc)
            } else {
                Todo(title = title,priority = priority, description = desc, timestamp = "") // Provide a timestamp value
            }

            if (shouldUpdate && todo != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    todoDao.update(newTodo)
                    getTodoList() // Refresh the list
                }
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    todoDao.insert(newTodo)
                    getTodoList() // Refresh the list
                }
            }
            alertDialogView.dismiss()
        }

        btCancel.setOnClickListener {
            alertDialogView.dismiss()
        }
        tvHeader.text = if (!shouldUpdate) getString(string.lbl_new_todo_title) else getString(
            string.lbl_edit_todo_title)

        alertDialogView.setCancelable(false)
        alertDialogView.show()
    }


    override fun onItemDelete(todo: Todo) {
        deleteConfirmation(todo)
    }

    override fun onItemClick(todo: Todo, position: Int) {
        showNoteDialog(true, todo, position)
    }
}
