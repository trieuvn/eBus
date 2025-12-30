package com.example.project_bus

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.project_bus.data.Instrument
import com.example.project_bus.data.InstrumentInsert
import com.example.project_bus.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SupabaseCrudTestActivity : AppCompatActivity() {

    private lateinit var edtName: EditText
    private lateinit var txtLastId: TextView
    private lateinit var txtLog: TextView

    private var lastInsertedId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_supabase_crud_test)

        edtName = findViewById(R.id.edtName)
        txtLastId = findViewById(R.id.txtLastId)
        txtLog = findViewById(R.id.txtLog)

        findViewById<Button>(R.id.btnLoad).setOnClickListener { loadAll() }
        findViewById<Button>(R.id.btnInsert).setOnClickListener { insertOne() }
        findViewById<Button>(R.id.btnUpdate).setOnClickListener { updateLast() }
        findViewById<Button>(R.id.btnDelete).setOnClickListener { deleteLast() }
    }

    private fun logLine(msg: String) {
        Log.d("SUPA_CRUD", msg)
        txtLog.append("$msg\n")
    }

    private fun loadAll() = lifecycleScope.launch {
        try {
            val list = withContext(Dispatchers.IO) {
                SupabaseProvider.client
                    .from("instruments")
                    .select()
                    .decodeList<Instrument>()
            }
            logLine("LOAD OK -> ${list.size} rows: $list")
        } catch (e: Exception) {
            logLine("LOAD FAIL -> ${e.message}")
            Log.e("SUPA_CRUD", "LOAD FAIL", e)
        }
    }

    private fun insertOne() = lifecycleScope.launch {
        val name = edtName.text.toString().trim().ifEmpty { "bus" }

        try {
            // Insert + return inserted row: dùng select() trong request (docs)
            val inserted = withContext(Dispatchers.IO) {
                SupabaseProvider.client
                    .from("instruments")
                    .insert(InstrumentInsert(name)) { select() }
                    .decodeSingle<Instrument>()
            }

            lastInsertedId = inserted.id
            txtLastId.text = "Last inserted id: ${inserted.id}"
            logLine("INSERT OK -> $inserted")
        } catch (e: Exception) {
            logLine("INSERT FAIL -> ${e.message}")
            Log.e("SUPA_CRUD", "INSERT FAIL", e)
        }
    }

    private fun updateLast() = lifecycleScope.launch {
        val id = lastInsertedId
        if (id == null) {
            logLine("UPDATE SKIP -> chưa có lastInsertedId, bấm INSERT trước")
            return@launch
        }

        val newName = edtName.text.toString().trim().ifEmpty { "bus-renamed" }

        try {
            // update() nên luôn có filter để không update cả bảng (docs)
            val updated = withContext(Dispatchers.IO) {
                SupabaseProvider.client
                    .from("instruments")
                    .update({ set("name", newName) }) {
                        select()
                        filter { eq("id", id) }
                    }
                    .decodeSingle<Instrument>()
            }

            logLine("UPDATE OK -> $updated")
        } catch (e: Exception) {
            logLine("UPDATE FAIL -> ${e.message}")
            Log.e("SUPA_CRUD", "UPDATE FAIL", e)
        }
    }

    private fun deleteLast() = lifecycleScope.launch {
        val id = lastInsertedId
        if (id == null) {
            logLine("DELETE SKIP -> chưa có lastInsertedId, bấm INSERT trước")
            return@launch
        }

        try {
            // delete() luôn có filter; muốn nhận row vừa xóa: select() + decodeSingle() (docs)
            val deleted = withContext(Dispatchers.IO) {
                SupabaseProvider.client
                    .from("instruments")
                    .delete {
                        select()
                        filter { eq("id", id) }
                    }
                    .decodeSingle<Instrument>()
            }

            lastInsertedId = null
            txtLastId.text = "Last inserted id: (none)"
            logLine("DELETE OK -> $deleted")
        } catch (e: Exception) {
            logLine("DELETE FAIL -> ${e.message}")
            Log.e("SUPA_CRUD", "DELETE FAIL", e)
        }
    }
}
