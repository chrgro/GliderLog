package no.ntnuf.gliderlog.main

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.preference.PreferenceManager
import no.ntnuf.gliderlog.R
import no.ntnuf.gliderlog.common.ColoringUtil
import no.ntnuf.gliderlog.common.Contact
import no.ntnuf.gliderlog.common.ContactListManager
import no.ntnuf.gliderlog.common.DayLog
import no.ntnuf.gliderlog.common.applyContentInsets
import no.ntnuf.gliderlog.common.applyToolbarInsets
import no.ntnuf.gliderlog.common.enableImmersiveFullscreen
import no.ntnuf.gliderlog.dayoverview.DayOverviewActivity
import java.io.FileNotFoundException
import java.io.IOException
import java.io.ObjectInputStream
import java.io.OptionalDataException
import java.io.StreamCorruptedException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.Locale

class DaySetupActivity : AppCompatActivity() {
    private val dayLogFileNamePrefix = "gliderlog_"
    private var dayLogFileNameSuffix = ""

    private lateinit var startDayButton: Button
    private lateinit var resumeDayButton: Button
    private lateinit var datepicker: DatePicker
    private lateinit var toolbar: Toolbar
    private lateinit var daylogdialog: AlertDialog

    private var selectedHead: Contact? = null
    private var selecteddate: Date = Date()

    private lateinit var contactlistmanager: ContactListManager
    private lateinit var headOfOperationsNameIn: AutoCompleteTextView
    private lateinit var headCheckmark: ImageView
    private lateinit var airfieldIn: EditText

    private var foundDaylog = false
    private lateinit var settings: SharedPreferences
    private var autoLoadLog = true
    private lateinit var today: Calendar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveFullscreen()
        setContentView(R.layout.activity_daysetup)

        toolbar = findViewById(R.id.toolbarmain)
        toolbar.title = "Prepare Log"
        setSupportActionBar(toolbar)
        applyToolbarInsets(toolbar)
        applyContentInsets(findViewById(R.id.daySetupContent))

        settings = PreferenceManager.getDefaultSharedPreferences(this)
        contactlistmanager = ContactListManager(this)

        headCheckmark = findViewById(R.id.headNameCheckmark)
        headOfOperationsNameIn = findViewById(R.id.headNameIn)
        headOfOperationsNameIn.setAdapter(contactlistmanager.getContactNameListAdapter())
        headOfOperationsNameIn.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val selected = contactlistmanager.findContactFromName(s.toString())
                selectedHead = selected
                if (selected?.hasAccount == true) {
                    headCheckmark.setImageResource(R.mipmap.green_checkmark)
                } else {
                    headCheckmark.setImageDrawable(null)
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit

            override fun afterTextChanged(s: Editable) = Unit
        })

        airfieldIn = findViewById(R.id.airfieldIn)
        airfieldIn.setText(settings.getString("airfield_default", ""))

        datepicker = findViewById(R.id.datePicker)
        today = Calendar.getInstance()
        datepicker.init(
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH),
            today.get(Calendar.DAY_OF_MONTH),
        ) { _, year, monthOfYear, dayOfMonth ->
            val c = Calendar.getInstance()
            c.set(year, monthOfYear, dayOfMonth)
            selecteddate = c.time

            dayLogFileNameSuffix = SimpleDateFormat("yyyy_MM_dd", Locale.ENGLISH).format(selecteddate)
            foundDaylog = loadDayLog()
            updateDayActionButtonsVisibility()
        }

        startDayButton = findViewById(R.id.startDayButton)
        ColoringUtil.colorMe(startDayButton, resources.getColor(R.color.colorPrimary, theme))
        startDayButton.setTextColor(resources.getColor(R.color.white, theme))
        startDayButton.setOnClickListener {
            when {
                foundDaylog -> {
                    AlertDialog.Builder(this)
                        .setMessage("There is already a log for this date. Are you sure you want to overwrite it?")
                        .setCancelable(false)
                        .setPositiveButton("Yes") { _, _ -> startNewDay() }
                        .setNegativeButton("No", null)
                        .show()
                }

                datepicker.dayOfMonth != today.get(Calendar.DAY_OF_MONTH) ||
                    datepicker.month != today.get(Calendar.MONTH) ||
                    datepicker.year != today.get(Calendar.YEAR) -> {
                    AlertDialog.Builder(this)
                        .setMessage("You did not pick todays date. Are you sure this is the date you want?")
                        .setCancelable(false)
                        .setPositiveButton("Yes") { _, _ -> startNewDay() }
                        .setNegativeButton("No", null)
                        .show()
                }

                else -> startNewDay()
            }
        }

        resumeDayButton = findViewById(R.id.resumeDayButton)
        ColoringUtil.colorMe(resumeDayButton, resources.getColor(R.color.resumeday_button, theme))
        resumeDayButton.setTextColor(resources.getColor(R.color.white, theme))
        updateDayActionButtonsVisibility()
        resumeDayButton.setOnClickListener {
            val intent = Intent(this, DayOverviewActivity::class.java)
            val bundle = bundleDayInfo().apply {
                putSerializable("action", "resume")
                putSerializable("reason", "Resuming day")
            }
            intent.putExtras(bundle)
            startActivityForResult(intent, 0)
        }

        daylogdialog = getPrevLogsAlertDialog()
    }

    private fun startNewDay() {
        contactlistmanager.saveContact(headOfOperationsNameIn.text.toString().trim())
        val intent = Intent(this, DayOverviewActivity::class.java)
        val bundle = bundleDayInfo().apply { putSerializable("action", "new") }
        intent.putExtras(bundle)
        startActivity(intent)
    }

    private fun getPrevLogsAlertDialog(): AlertDialog {
        val daysavailable = availableDayLogs()
        return AlertDialog.Builder(this)
            .setTitle("Previous logs")
            .setItems(daysavailable) { _, which ->
                dayLogFileNameSuffix = getFilenameFromListpos(which)

                val parts = dayLogFileNameSuffix.split("_")
                if (parts.size == 3) {
                    val year = parts[0].toInt()
                    val monthOfYear = parts[1].toInt() - 1
                    val dayOfMonth = parts[2].toInt()

                    val c = Calendar.getInstance()
                    c.set(year, monthOfYear, dayOfMonth)
                    selecteddate = c.time
                }

                resumeDayButton.callOnClick()
            }
            .create()
    }

    private fun getFilenameFromListpos(which: Int): String {
        val listOfFiles = filesDir.list()?.toMutableList() ?: mutableListOf()
        Collections.sort(listOfFiles)

        var c = 0
        for (name in listOfFiles) {
            if (name.startsWith(dayLogFileNamePrefix)) {
                if (c == which) {
                    return name.removePrefix(dayLogFileNamePrefix)
                }
                c++
            }
        }
        return ""
    }

    private fun availableDayLogs(): Array<CharSequence> {
        val listOfFiles = filesDir.list()?.toMutableList() ?: mutableListOf()
        Collections.sort(listOfFiles)

        val entries = ArrayList<String>()
        val df = SimpleDateFormat("yyyy M d", Locale.ENGLISH)
        val outdf = SimpleDateFormat("EEEE yyyy-MM-dd", Locale.ENGLISH)

        for (name in listOfFiles) {
            if (name.startsWith(dayLogFileNamePrefix)) {
                val namesplit = name.split("_")
                if (namesplit.size == 4) {
                    try {
                        val parsestring = "${namesplit[1]} ${namesplit[2]} ${namesplit[3]}"
                        val result = df.parse(parsestring)
                        if (result != null) {
                            entries.add(outdf.format(result))
                        }
                    } catch (_: ParseException) {
                        entries.add("Parse error, name: $name")
                    }
                } else {
                    entries.add("Parse error, split: $name")
                }
            }
        }

        return entries.toTypedArray()
    }

    private fun bundleDayInfo(): Bundle {
        val headOfOperations = selectedHead ?: Contact().apply {
            name = headOfOperationsNameIn.text.toString().trim()
        }

        return Bundle().apply {
            putSerializable("headOfOperations", headOfOperations)
            putSerializable("airfield", airfieldIn.text.toString().trim())
            putSerializable("date", selecteddate)
        }
    }

    private fun loadDayLog(): Boolean {
        val fullfilename = dayLogFileNamePrefix + dayLogFileNameSuffix
        return try {
            openFileInput(fullfilename).use { fis ->
                ObjectInputStream(fis).use { input ->
                    input.readObject() as DayLog
                }
            }
            true
        } catch (e: ClassNotFoundException) {
            deleteFile(fullfilename)
            Toast.makeText(this, "Broken file selected, deleting it...", Toast.LENGTH_LONG).show()
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

    private fun updateDayActionButtonsVisibility() {
        val hasLogForSelectedDate = foundDaylog
        resumeDayButton.visibility = if (hasLogForSelectedDate) View.VISIBLE else View.GONE
        startDayButton.visibility = if (hasLogForSelectedDate) View.GONE else View.VISIBLE
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_listlogs -> {
                daylogdialog.show()
                true
            }

            R.id.menu_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val action = data?.extras?.getSerializable("action") as? String
        when (action) {
            "backbutton" -> {
                finish()
                return
            }

            "backtocalendarmenu" -> {
                autoLoadLog = false
                return
            }
        }
    }

    override fun onResume() {
        super.onResume()

        today = Calendar.getInstance()
        datepicker.updateDate(
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH),
            today.get(Calendar.DAY_OF_MONTH),
        )

        val calendar = Calendar.getInstance().apply {
            set(datepicker.year, datepicker.month, datepicker.dayOfMonth)
        }
        selecteddate = calendar.time

        dayLogFileNameSuffix = SimpleDateFormat("yyyy_MM_dd", Locale.ENGLISH).format(selecteddate)

        foundDaylog = loadDayLog()
        updateDayActionButtonsVisibility()

        if (foundDaylog && autoLoadLog) {
            val datePicked = Calendar.getInstance().apply {
                set(datepicker.year, datepicker.month, datepicker.dayOfMonth)
            }
            if (datePicked.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                datePicked.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
            ) {
                resumeDayButton.callOnClick()
            }
        }

        autoLoadLog = true
        contactlistmanager = ContactListManager(this)
        headOfOperationsNameIn.setAdapter(contactlistmanager.getContactNameListAdapter())
        daylogdialog = getPrevLogsAlertDialog()
        datepicker.requestFocus()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableImmersiveFullscreen()
        }
    }
}
