package no.ntnuf.gliderlog.dayoverview

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import no.ntnuf.gliderlog.R
import no.ntnuf.gliderlog.common.Contact
import no.ntnuf.gliderlog.common.DayLog
import no.ntnuf.gliderlog.common.FlightEntry
import no.ntnuf.gliderlog.common.applyContentInsets
import no.ntnuf.gliderlog.common.applyFabInsets
import no.ntnuf.gliderlog.common.applyToolbarInsets
import no.ntnuf.gliderlog.common.enableImmersiveFullscreen
import no.ntnuf.gliderlog.newflight.NewFlightActivity
import java.io.FileNotFoundException
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.OptionalDataException
import java.io.StreamCorruptedException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DayOverviewActivity : AppCompatActivity() {
    private lateinit var daylog: DayLog

    companion object {
        private const val REQUEST_NEW_FLIGHT = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveFullscreen()
        setContentView(R.layout.activity_day_overview)

        val bundle = savedInstanceState ?: intent.extras ?: Bundle()
        val action = bundle.getSerializable("action") as? String ?: "new"
        val date = bundle.getSerializable("date") as? Date ?: Date()

        val toolbar = findViewById<Toolbar>(R.id.toolbardayoverview)
        val formattedDate = SimpleDateFormat("EEEE d/M", Locale.ENGLISH).format(date)
        toolbar.title = "Day Log  -  $formattedDate"
        setSupportActionBar(toolbar)
        applyToolbarInsets(toolbar)

        onBackPressedDispatcher.addCallback(this) {
            val response = Intent().apply {
                putExtra("action", "backtocalendarmenu")
            }
            setResult(Activity.RESULT_OK, response)
            finish()
        }

        if (action == "new" || !loadDayLog(date)) {
            daylog = DayLog().apply {
                headOfOperations = bundle.getSerializable("headOfOperations") as? Contact
                airfield = bundle.getSerializable("airfield") as? String
                this.date = date
            }
            saveDayLog()
        }

        val info = findViewById<TextView>(R.id.dayOverviewInfo)
        applyContentInsets(info)
        info.text = getString(
            R.string.day_overview_placeholder,
            daylog.headOfOperations?.name ?: "",
            daylog.airfield ?: "",
        )

        val addFlightButton = findViewById<FloatingActionButton>(R.id.addFlightButton)
        applyFabInsets(addFlightButton)
        addFlightButton.setOnClickListener {
            val intent = Intent(this, NewFlightActivity::class.java).apply {
                putExtra("date", daylog.date)
                putExtra("action", "add")
            }
            startActivityForResult(intent, REQUEST_NEW_FLIGHT)
        }
    }

    private fun loadDayLog(date: Date): Boolean {
        val suffix = SimpleDateFormat("yyyy_MM_dd", Locale.ENGLISH).format(date)
        val fileName = DayLog.dayLogFileName + suffix

        return try {
            openFileInput(fileName).use { fis ->
                ObjectInputStream(fis).use { input ->
                    daylog = input.readObject() as DayLog
                }
            }
            true
        } catch (_: ClassNotFoundException) {
            false
        } catch (_: OptionalDataException) {
            false
        } catch (_: FileNotFoundException) {
            false
        } catch (_: StreamCorruptedException) {
            false
        } catch (_: IOException) {
            false
        }
    }

    private fun saveDayLog() {
        val fileName = daylog.getFilename()
        try {
            openFileOutput(fileName, MODE_PRIVATE).use { fos ->
                ObjectOutputStream(fos).use { out ->
                    out.writeObject(daylog)
                }
            }
        } catch (_: IOException) {
            // Keep behavior simple in first migration slice.
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK || requestCode != REQUEST_NEW_FLIGHT || data == null) {
            return
        }

        val action = data.getSerializableExtra("action") as? String ?: return
        val flight = data.getSerializableExtra("flight") as? FlightEntry ?: return

        when (action) {
            "add" -> daylog.flights.add(flight)
            "update" -> {
                val index = data.getSerializableExtra("flightindex") as? Int ?: -1
                if (index in daylog.flights.indices) {
                    daylog.flights[index] = flight
                }
            }
        }
        saveDayLog()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableImmersiveFullscreen()
        }
    }
}
