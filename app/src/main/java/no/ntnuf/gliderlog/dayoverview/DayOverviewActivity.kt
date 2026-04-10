package no.ntnuf.gliderlog.dayoverview

import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.preference.PreferenceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import no.ntnuf.gliderlog.R
import no.ntnuf.gliderlog.common.Contact
import no.ntnuf.gliderlog.common.ContactListManager
import no.ntnuf.gliderlog.common.DayLog
import no.ntnuf.gliderlog.common.FlightEntry
import no.ntnuf.gliderlog.common.FlightStatus
import no.ntnuf.gliderlog.common.LogUploader
import no.ntnuf.gliderlog.common.RegistrationList
import no.ntnuf.gliderlog.common.applyContentInsets
import no.ntnuf.gliderlog.common.applyFabInsets
import no.ntnuf.gliderlog.common.applyToolbarInsets
import no.ntnuf.gliderlog.common.enableImmersiveFullscreen
import no.ntnuf.gliderlog.daysummary.DaySummaryActivity
import no.ntnuf.gliderlog.fiken.FikenContactRequestTask
import no.ntnuf.gliderlog.main.SettingsActivity
import no.ntnuf.gliderlog.newflight.NewFlightActivity
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.OptionalDataException
import java.io.StreamCorruptedException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DayOverviewActivity : AppCompatActivity() {
    private lateinit var daylog: DayLog
    private lateinit var settings: SharedPreferences

    private lateinit var tableLayout: TableLayout
    private lateinit var addFlightButton: FloatingActionButton
    private var editMode = false
    private var roundSumsTo = 5
    private var menuRef: Menu? = null
    private lateinit var loadFikenContactsDialog: AlertDialog

    companion object {
        private const val REQUEST_NEW_FLIGHT = 1
        private const val REQUEST_SEND_LOG_EMAIL = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveFullscreen()
        setContentView(R.layout.activity_day_overview)

        settings = PreferenceManager.getDefaultSharedPreferences(this)
        roundSumsTo = settings.getString("round_sums_to", "5")?.toIntOrNull() ?: 5

        val bundle = savedInstanceState ?: intent.extras ?: Bundle()
        val action = bundle.getSerializable("action") as? String ?: "new"
        val date = bundle.getSerializable("date") as? Date ?: Date()

        val toolbar = findViewById<Toolbar>(R.id.toolbardayoverview)
        val formattedDate = SimpleDateFormat("EEEE d/M", Locale.ENGLISH).format(date)
        toolbar.title = "Day Log  -  $formattedDate"
        setSupportActionBar(toolbar)
        applyToolbarInsets(toolbar)
        tableLayout = findViewById(R.id.dayOverViewTableLayout)
        applyContentInsets(findViewById<ScrollView>(R.id.dayOverviewScroll))

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

        refreshTowTable()

        addFlightButton = findViewById(R.id.addFlightButton)
        applyFabInsets(addFlightButton)
        addFlightButton.setOnClickListener {
            val next = Intent(this, NewFlightActivity::class.java).apply {
                putExtra("date", daylog.date)
                putExtra("action", "add")
            }
            startActivityForResult(next, REQUEST_NEW_FLIGHT)
        }
        updateAddFlightButtonVisibility()

        if (settings.getBoolean("upload_log_enabled", false)) {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.addDefaultNetworkActiveListener {
                LogUploader(this, settings).uploadPending()
            }
        }

        loadFikenContactsDialog = AlertDialog.Builder(this)
            .setTitle("Load Contacts from Fiken")
            .setMessage("Connecting to Fiken...")
            .create()
    }

    override fun onResume() {
        super.onResume()
        roundSumsTo = settings.getString("round_sums_to", "5")?.toIntOrNull() ?: 5
        invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.dayoverview_menu, menu)
        menuRef = menu

        menu.findItem(R.id.menu_loadfikencontacts).isVisible = settings.getBoolean("fiken_api_enabled", false)
        menu.findItem(R.id.menu_reenablelog).isVisible = daylog.logIsLocked
        menu.findItem(R.id.menu_editlog).isVisible = !daylog.logIsLocked
        menu.findItem(R.id.menu_deletedaylog).isVisible = !daylog.logIsLocked

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_senddaylog -> {
                if (daylog.logIsLocked) {
                    AlertDialog.Builder(this)
                        .setMessage("It looks like this log has already been sent. Are you sure you want to resend it?")
                        .setCancelable(false)
                        .setPositiveButton(R.string.yes) { _, _ -> sendLog() }
                        .setNegativeButton(R.string.no, null)
                        .show()
                } else {
                    sendLog()
                }
                true
            }

            R.id.menu_clearpilotlist -> {
                AlertDialog.Builder(this)
                    .setMessage(R.string.confirm_clear_pilot_list)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes) { _, _ ->
                        ContactListManager(applicationContext).clearList()
                    }
                    .setNegativeButton(R.string.no, null)
                    .show()
                true
            }

            R.id.menu_clearregistrationlist -> {
                AlertDialog.Builder(this)
                    .setMessage(R.string.confirm_clear_registration_list)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes) { _, _ ->
                        RegistrationList(applicationContext).clearList()
                    }
                    .setNegativeButton(R.string.no, null)
                    .show()
                true
            }

            R.id.menu_show_pilot_summary,
            R.id.menu_show_plane_summary -> {
                val summaryType = if (item.itemId == R.id.menu_show_pilot_summary) "pilot" else "plane"
                val intent = Intent(this, DaySummaryActivity::class.java).apply {
                    putExtra("daylog", daylog)
                    putExtra("summary_type", summaryType)
                }
                startActivity(intent)
                true
            }

            R.id.menu_editlog -> {
                editMode = !editMode
                item.title = if (editMode) getString(R.string.finish_log_edit) else getString(R.string.edit_log)
                refreshTowTable()
                updateAddFlightButtonVisibility()
                true
            }

            R.id.menu_deletedaylog -> {
                AlertDialog.Builder(this)
                    .setMessage(R.string.confirm_delete_daylog)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes) { _, _ ->
                        deleteDayLog()
                        finish()
                    }
                    .setNegativeButton(R.string.no, null)
                    .show()
                true
            }

            R.id.menu_loadfikencontacts -> {
                val fikenRequest = FikenContactRequestTask().apply {
                    setContext(this@DayOverviewActivity)
                    setDialog(loadFikenContactsDialog)
                    setContactListManager(ContactListManager(this@DayOverviewActivity))
                }
                fikenRequest.execute()
                true
            }

            R.id.menu_reenablelog -> {
                daylog.logIsLocked = false
                saveDayLog()
                menuRef?.findItem(R.id.menu_reenablelog)?.isVisible = false
                menuRef?.findItem(R.id.menu_editlog)?.isVisible = true
                menuRef?.findItem(R.id.menu_deletedaylog)?.isVisible = true
                updateAddFlightButtonVisibility()
                true
            }

            R.id.menu_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }

            R.id.menu_backtocalendar -> {
                val response = Intent().apply {
                    putExtra("action", "backtocalendarmenu")
                }
                setResult(Activity.RESULT_OK, response)
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun sendLog() {
        if (!daylog.isLogComplete()) {
            Toast.makeText(this, R.string.log_must_be_complete_before_send, Toast.LENGTH_LONG).show()
            return
        }

        if (settings.getBoolean("upload_log_enabled", false)) {
            val uploader = LogUploader(this, settings)
            uploader.addToUploadQueue(daylog)
            uploader.uploadPending()
        }

        sendLogViaEmail()
    }

    private fun sendLogViaEmail() {
        val dateForSubject = SimpleDateFormat("EEEE yyyy-MM-dd", Locale.ENGLISH).format(daylog.date)
        val dateForFile = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(daylog.date)
        val htmlFilename = "gliderlog_$dateForFile.html"
        val jsonFilename = "gliderlog_$dateForFile.json"
        val receiverEmail = settings.getString("send_log_email", "") ?: ""

        try {
            val dir = externalCacheDir ?: cacheDir
            val htmlLogFile = File(dir, htmlFilename)
            val jsonLogFile = File(dir, jsonFilename)
            htmlLogFile.writeText(daylog.getHTMLtableoutput(roundSumsTo))
            jsonLogFile.writeText(daylog.getJSONOutput())

            val authority = "${packageName}.fileprovider"
            val htmlFileUri: Uri = FileProvider.getUriForFile(this, authority, htmlLogFile)
            val jsonFileUri: Uri = FileProvider.getUriForFile(this, authority, jsonLogFile)
            val attachmentUris = arrayListOf(htmlFileUri, jsonFileUri)

            val emailIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(receiverEmail))
                putExtra(Intent.EXTRA_SUBJECT, "Glider Log for $dateForSubject")
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, attachmentUris)
                putExtra(Intent.EXTRA_TEXT, daylog.getMarkdownOutput(roundSumsTo))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            if (settings.getBoolean("send_log_to_customers", false)) {
                val customerEmails = linkedSetOf<String>()
                daylog.headOfOperations?.email?.let { if (it.isNotBlank()) customerEmails.add(it) }
                for (flight in daylog.flights) {
                    flight.pilot?.email?.let { if (it.isNotBlank()) customerEmails.add(it) }
                    flight.copilot?.email?.let { if (it.isNotBlank()) customerEmails.add(it) }
                }
                val recipients = customerEmails.toTypedArray()
                if (recipients.isNotEmpty()) {
                    if (settings.getBoolean("send_log_to_customers_using_bcc", true)) {
                        emailIntent.putExtra(Intent.EXTRA_BCC, recipients)
                    } else {
                        emailIntent.putExtra(Intent.EXTRA_CC, recipients)
                    }
                }
            }

            startActivityForResult(Intent.createChooser(emailIntent, "Send email using"), REQUEST_SEND_LOG_EMAIL)

            daylog.setLogHasBeenSent()
            updateAddFlightButtonVisibility()
            menuRef?.findItem(R.id.menu_reenablelog)?.isVisible = true
            menuRef?.findItem(R.id.menu_editlog)?.isVisible = false
            menuRef?.findItem(R.id.menu_deletedaylog)?.isVisible = false
            saveDayLog()
        } catch (_: IOException) {
            Toast.makeText(this, R.string.failed_to_prepare_email_log, Toast.LENGTH_LONG).show()
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
            // Keep behavior best effort.
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_NEW_FLIGHT || resultCode != Activity.RESULT_OK || data == null) {
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

        refreshTowTable()
        saveDayLog()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("action", "resume")
        outState.putSerializable("date", daylog.date)
    }

    private fun addFlightToTable(flight: FlightEntry, flightNumber: Int): TableRow {
        val row = TableRow(this)

        val numberView = TextView(this).apply {
            text = flightNumber.toString()
            textSize = 14f
            setPadding(0, 0, 10, 0)
        }
        row.addView(numberView)

        val registrationView = TextView(this).apply {
            text = flight.registration
            setTypeface(null, Typeface.BOLD)
            textSize = 14f
            setPadding(0, 0, 10, 0)
        }

        val pilotView = TextView(this).apply {
            text = flight.pilot?.name.orEmpty()
            textSize = 14f
            setPadding(0, 0, 10, 0)
        }

        val copilotView = TextView(this).apply {
            text = flight.copilot?.name.orEmpty()
            textSize = 14f
            setPadding(0, 0, 10, 0)
        }

        val notesView = TextView(this).apply {
            text = flight.notes
            textSize = 14f
            setPadding(0, 0, 10, 0)
            setTypeface(null, Typeface.ITALIC)
        }

        val regAndButtons = RelativeLayout(this).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT,
            )
        }

        val buttonsLayout = LinearLayout(this).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT,
            ).also { params -> params.addRule(RelativeLayout.ALIGN_PARENT_END) }
        }

        val flightIndex = flightNumber - 1
        if (!editMode) {
            when (flight.status ?: FlightStatus.NOT_DEPARTED) {
                FlightStatus.NOT_DEPARTED -> buttonsLayout.addView(actionButton(android.R.drawable.ic_media_play) {
                    setTimeFromTimepicker(flight, "takeoff")
                })

                FlightStatus.DEPARTED -> buttonsLayout.addView(actionButton(android.R.drawable.ic_menu_upload) {
                    setTimeFromTimepicker(flight, "release")
                })

                FlightStatus.RELEASED -> buttonsLayout.addView(actionButton(android.R.drawable.ic_menu_compass) {
                    setTimeFromTimepicker(flight, "landing")
                })

                FlightStatus.LANDED -> Unit
            }
        } else {
            buttonsLayout.addView(actionButton(android.R.drawable.ic_menu_edit) {
                val updateIntent = Intent(this, NewFlightActivity::class.java).apply {
                    putExtra("date", daylog.date)
                    putExtra("action", "update")
                    putExtra("flight", flight)
                    putExtra("flightindex", flightIndex)
                }
                startActivityForResult(updateIntent, REQUEST_NEW_FLIGHT)
            })

            buttonsLayout.addView(actionButton(android.R.drawable.ic_menu_delete) {
                AlertDialog.Builder(this)
                    .setMessage(R.string.confirm_delete_log_line)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes) { _, _ ->
                        daylog.deleteLogLine(flightIndex)
                        refreshTowTable()
                        saveDayLog()
                    }
                    .setNegativeButton(R.string.no, null)
                    .show()
            })
        }

        regAndButtons.addView(buttonsLayout)

        val regAndNames = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(registrationView)
            addView(pilotView)
            if (flight.copilot != null) addView(copilotView)
            if (flight.notes.isNotBlank()) addView(notesView)
            addView(regAndButtons)
        }
        row.addView(regAndNames)

        val timeFormat = SimpleDateFormat("HH:mm", Locale.ENGLISH)
        val statusLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        statusLayout.addView(TextView(this).apply {
            textSize = 14f
            setPadding(0, 0, 10, 0)
            text = if (flight.takeoff != null) timeFormat.format(flight.takeoff!!) else getString(R.string.not_departed)
        })

        statusLayout.addView(TextView(this).apply {
            textSize = 14f
            setPadding(0, 0, 10, 0)
            text = when {
                flight.towRelease != null -> "${timeFormat.format(flight.towRelease!!)} (${flight.getTowDurationStr()})"
                (flight.status ?: FlightStatus.NOT_DEPARTED) != FlightStatus.NOT_DEPARTED -> getString(R.string.not_released)
                else -> ""
            }
        })

        statusLayout.addView(TextView(this).apply {
            textSize = 14f
            setPadding(0, 0, 10, 0)
            text = when {
                flight.landing != null -> "${timeFormat.format(flight.landing!!)} (${flight.getFlightDurationStr()})"
                (flight.status ?: FlightStatus.NOT_DEPARTED) != FlightStatus.NOT_DEPARTED -> getString(R.string.not_landed)
                else -> ""
            }
        })

        row.addView(statusLayout)
        return row
    }

    private fun actionButton(iconRes: Int, onClick: () -> Unit): ImageView {
        return ImageView(this).apply {
            setImageResource(iconRes)
            setPadding(30, 0, 20, 0)
            adjustViewBounds = true
            setOnClickListener { onClick() }
        }
    }

    private fun refreshTowTable() {
        val headerRows = 2
        val rowsToRemove = tableLayout.childCount - headerRows
        if (rowsToRemove > 0) {
            tableLayout.removeViews(headerRows, rowsToRemove)
        }

        var flightNumber = 0
        for (flight in daylog.flights) {
            flightNumber += 1
            val row = addFlightToTable(flight, flightNumber)
            val separator = View(this).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
                setBackgroundColor(resources.getColor(R.color.black, theme))
            }
            tableLayout.addView(row, TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT))
            tableLayout.addView(separator)
        }
    }

    private fun setTimeFromTimepicker(flight: FlightEntry, type: String) {
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val minute = now.get(Calendar.MINUTE)
        val picker = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            when (type) {
                "takeoff" -> {
                    flight.setTakeoff(selectedHour, selectedMinute, daylog.date)
                    daylog.sortOnTakeoff()
                }

                "release" -> {
                    flight.setTowRelease(selectedHour, selectedMinute, daylog.date)
                    if (flight.getTowDurationMinutes() < 0) {
                        val tomorrow = Calendar.getInstance().apply {
                            time = daylog.date
                            add(Calendar.HOUR_OF_DAY, 24)
                        }
                        flight.setTowRelease(selectedHour, selectedMinute, tomorrow.time)
                        Toast.makeText(this, R.string.release_before_takeoff_assume_next_day, Toast.LENGTH_LONG).show()
                    }
                }

                "landing" -> {
                    flight.setLanding(selectedHour, selectedMinute, daylog.date)
                    if (flight.getFlightDurationMinutes() < 0) {
                        val tomorrow = Calendar.getInstance().apply {
                            time = daylog.date
                            add(Calendar.HOUR_OF_DAY, 24)
                        }
                        flight.setLanding(selectedHour, selectedMinute, tomorrow.time, true)
                        Toast.makeText(this, R.string.landing_before_takeoff_assume_next_day, Toast.LENGTH_LONG).show()
                    }
                    if (flight.getTowDurationMinutes() > flight.getFlightDurationMinutes()) {
                        Toast.makeText(this, R.string.landing_must_be_after_release, Toast.LENGTH_LONG).show()
                        flight.landing = null
                        flight.status = FlightStatus.RELEASED
                    }
                }
            }
            refreshTowTable()
            saveDayLog()
        }, hour, minute, true)
        picker.setMessage(getString(R.string.set_time_for, type))
        picker.show()
    }

    private fun updateAddFlightButtonVisibility() {
        if (editMode || daylog.logIsLocked) {
            addFlightButton.hide()
        } else {
            addFlightButton.show()
        }
    }

    private fun deleteDayLog(): Boolean {
        return deleteFile(daylog.getFilename())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableImmersiveFullscreen()
        }
    }
}
