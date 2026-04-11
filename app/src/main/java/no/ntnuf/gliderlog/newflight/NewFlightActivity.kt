package no.ntnuf.gliderlog.newflight

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import no.ntnuf.gliderlog.R
import no.ntnuf.gliderlog.common.ColoringUtil
import no.ntnuf.gliderlog.common.Contact
import no.ntnuf.gliderlog.common.ContactListManager
import no.ntnuf.gliderlog.common.FlightEntry
import no.ntnuf.gliderlog.common.FlightStatus
import no.ntnuf.gliderlog.common.NotesAdapter
import no.ntnuf.gliderlog.common.PilotType
import no.ntnuf.gliderlog.common.RegistrationList
import no.ntnuf.gliderlog.common.applyContentInsets
import no.ntnuf.gliderlog.common.applyFabInsets
import no.ntnuf.gliderlog.common.applyToolbarInsets
import no.ntnuf.gliderlog.common.enableImmersiveFullscreen
import java.util.Calendar
import java.util.Date

class NewFlightActivity : AppCompatActivity() {
    private lateinit var pilotIn: AutoCompleteTextView
    private lateinit var pilotTypePicIn: RadioButton
    private lateinit var pilotTypeInstructorIn: RadioButton
    private lateinit var copilotIn: AutoCompleteTextView
    private lateinit var copilotTypeStudentIn: RadioButton
    private lateinit var copilotTypePassengerIn: RadioButton
    private lateinit var registrationIn: AutoCompleteTextView
    private lateinit var notesIn: AutoCompleteTextView
    private lateinit var pilotCheckmark: ImageView
    private lateinit var copilotCheckmark: ImageView

    private lateinit var timestampsTitle: TextView
    private lateinit var takeoffTimeIn: TextView
    private lateinit var releaseTimeIn: TextView
    private lateinit var landingTimeIn: TextView

    private var selectedPilot: Contact? = null
    private var selectedCoPilot: Contact? = null

    private var date: Date = Date()
    private var update: Boolean = false

    private lateinit var contactListManager: ContactListManager
    private lateinit var registrationList: RegistrationList

    private lateinit var flightToUpdate: FlightEntry
    private var flightIndex: Int = -1

    private lateinit var settings: SharedPreferences

    private fun normalizedContactName(input: CharSequence?): String {
        return ContactListManager.normalizeSuggestionLabel(input?.toString().orEmpty())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveFullscreen()
        setContentView(R.layout.activity_new_flight)

        val bundle = intent.extras ?: Bundle()
        date = bundle.getSerializable("date") as? Date ?: Date()
        update = (bundle.getSerializable("action") as? String) == "update"
        if (update) {
            flightToUpdate = bundle.getSerializable("flight") as FlightEntry
            flightIndex = bundle.getSerializable("flightindex") as? Int ?: -1
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbarnewtow)
        setSupportActionBar(toolbar)
        applyToolbarInsets(toolbar)
        applyContentInsets(findViewById(R.id.newFlightScroll))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        contactListManager = ContactListManager(this)
        registrationList = RegistrationList(this)
        settings = PreferenceManager.getDefaultSharedPreferences(this)

        pilotIn = findViewById(R.id.pilotNameIn)
        pilotTypePicIn = findViewById(R.id.radio_pic)
        pilotTypeInstructorIn = findViewById(R.id.radio_instructor)
        pilotCheckmark = findViewById(R.id.pilotNameCheckmark)
        pilotIn.setAdapter(contactListManager.getContactSuggestionListAdapter())
        pilotIn.setOnItemClickListener { _, _, _, _ ->
            val normalized = normalizedContactName(pilotIn.text)
            pilotIn.setText(normalized, false)
            pilotIn.setSelection(normalized.length)
        }
        pilotIn.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                selectedPilot = contactListManager.findContactFromName(normalizedContactName(s))
                if (selectedPilot?.hasAccount == true) {
                    pilotCheckmark.setImageResource(android.R.drawable.checkbox_on_background)
                } else {
                    pilotCheckmark.setImageDrawable(null)
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit
            override fun afterTextChanged(s: Editable) = Unit
        })

        copilotIn = findViewById(R.id.coPilotNameIn)
        copilotTypeStudentIn = findViewById(R.id.radio_student)
        copilotTypePassengerIn = findViewById(R.id.radio_passenger)
        copilotCheckmark = findViewById(R.id.copilotNameCheckmark)
        copilotIn.setAdapter(contactListManager.getContactSuggestionListAdapter())
        copilotIn.setOnItemClickListener { _, _, _, _ ->
            val normalized = normalizedContactName(copilotIn.text)
            copilotIn.setText(normalized, false)
            copilotIn.setSelection(normalized.length)
        }
        copilotIn.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                selectedCoPilot = contactListManager.findContactFromName(normalizedContactName(s))
                if (selectedCoPilot?.hasAccount == true) {
                    copilotCheckmark.setImageResource(android.R.drawable.checkbox_on_background)
                } else {
                    copilotCheckmark.setImageDrawable(null)
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit
            override fun afterTextChanged(s: Editable) = Unit
        })

        registrationIn = findViewById(R.id.gliderRegistrationIn)
        registrationIn.setAdapter(registrationList.getRegistrationListAdapter())
        registrationIn.setText(settings.getString("glider_default_reg", "LN-G"))
        registrationIn.setSelection(registrationIn.text.length)

        val notesAdapter = NotesAdapter(this, settings)
        notesIn = findViewById(R.id.notesIn)
        notesIn.setAdapter(notesAdapter.getNotesAdapter())
        notesIn.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) notesIn.showDropDown()
        }

        timestampsTitle = findViewById(R.id.timestampsTitle)
        takeoffTimeIn = findViewById(R.id.takeoffTimeIn)
        releaseTimeIn = findViewById(R.id.releaseTimeIn)
        landingTimeIn = findViewById(R.id.landingTimeIn)

        timestampsTitle.visibility = View.INVISIBLE
        takeoffTimeIn.visibility = View.INVISIBLE
        releaseTimeIn.visibility = View.INVISIBLE
        landingTimeIn.visibility = View.INVISIBLE

        if (update) {
            applyUpdateValues()
        }

        val saveButton = findViewById<Button>(R.id.startTowButton)
        ColoringUtil.colorMe(saveButton, resources.getColor(R.color.resumeday_button, theme))
        saveButton.setTextColor(resources.getColor(R.color.white, theme))
        applyFabInsets(saveButton)
        saveButton.setOnClickListener {
            finishWithFlightResult()
        }
    }

    private fun applyUpdateValues() {
        registrationIn.setText(flightToUpdate.registration)
        pilotIn.setText(flightToUpdate.pilot?.name ?: "")
        pilotTypePicIn.isChecked = flightToUpdate.pilotType == PilotType.PIC
        pilotTypeInstructorIn.isChecked = flightToUpdate.pilotType == PilotType.INSTRUCTOR

        if (flightToUpdate.copilot != null) {
            copilotIn.setText(flightToUpdate.copilot?.name ?: "")
            copilotTypePassengerIn.isChecked = flightToUpdate.copilotType == PilotType.PASSENGER
            copilotTypeStudentIn.isChecked = flightToUpdate.copilotType == PilotType.STUDENT
        }

        notesIn.setText(flightToUpdate.notes)

        if (flightToUpdate.takeoff != null) {
            timestampsTitle.text = "Takeoff (tap to edit)"
            timestampsTitle.visibility = View.VISIBLE
            takeoffTimeIn.text = FlightEntry.hhcolonmmFromDate(flightToUpdate.takeoff!!)
            takeoffTimeIn.visibility = View.VISIBLE
            takeoffTimeIn.setOnClickListener {
                updateTimeFromTimePicker("takeoff")
            }
        }
        if (flightToUpdate.towRelease != null) {
            timestampsTitle.text = timestampsTitle.text.toString() + " | Release"
            releaseTimeIn.text = FlightEntry.hhcolonmmFromDate(flightToUpdate.towRelease!!)
            releaseTimeIn.visibility = View.VISIBLE
            releaseTimeIn.setOnClickListener {
                updateTimeFromTimePicker("release")
            }
        }
        if (flightToUpdate.landing != null) {
            timestampsTitle.text = timestampsTitle.text.toString() + " | Landing"
            landingTimeIn.text = FlightEntry.hhcolonmmFromDate(flightToUpdate.landing!!)
            landingTimeIn.visibility = View.VISIBLE
            landingTimeIn.setOnClickListener {
                updateTimeFromTimePicker("landing")
            }
        }
    }

    private fun finishWithFlightResult() {
        val reg = registrationIn.text.toString().trim()
        val pilotName = normalizedContactName(pilotIn.text)
        val copilotName = normalizedContactName(copilotIn.text)

        if (pilotName.isBlank() || reg.isBlank()) {
            Toast.makeText(this, "You need a name and a registration", Toast.LENGTH_LONG).show()
            return
        }

        registrationList.addRegistration(reg)

        if (selectedPilot == null) {
            selectedPilot = contactListManager.saveContact(pilotName)
        }
        if (selectedCoPilot == null && copilotName.isNotBlank()) {
            selectedCoPilot = contactListManager.saveContact(copilotName)
        }

        val flightEntry = FlightEntry().apply {
            status = FlightStatus.NOT_DEPARTED
            registration = reg
            pilot = selectedPilot
            pilotType = if (pilotTypePicIn.isChecked) PilotType.PIC else PilotType.INSTRUCTOR
            copilot = selectedCoPilot
            notes = notesIn.text.toString().trim()
        }

        if (flightEntry.pilotType == PilotType.INSTRUCTOR && copilotName.isBlank()) {
            Toast.makeText(
                this,
                "You need a student or a passenger copilot to have the have an instructor pilot.",
                Toast.LENGTH_LONG,
            ).show()
            return
        }

        if (selectedCoPilot != null) {
            flightEntry.copilotType = if (copilotTypePassengerIn.isChecked) {
                PilotType.PASSENGER
            } else {
                PilotType.STUDENT
            }

            if (flightEntry.copilotType == PilotType.STUDENT && flightEntry.pilotType != PilotType.INSTRUCTOR) {
                Toast.makeText(
                    this,
                    "You need an instructor as pilot to have a student as copilot.",
                    Toast.LENGTH_LONG,
                ).show()
                return
            }
        }

        if (update) {
            flightEntry.status = flightToUpdate.status
            flightEntry.takeoff = flightToUpdate.takeoff
            flightEntry.towRelease = flightToUpdate.towRelease
            flightEntry.landing = flightToUpdate.landing
        }

        val response = Intent().apply {
            putExtra("flight", flightEntry)
            putExtra("action", if (update) "update" else "add")
            if (update) {
                putExtra("flightindex", flightIndex)
            }
        }
        setResult(Activity.RESULT_OK, response)
        finish()
    }

    private fun updateTimeFromTimePicker(type: String) {
        val previousTime = Calendar.getInstance()
        when (type) {
            "takeoff" -> previousTime.time = flightToUpdate.takeoff ?: date
            "release" -> previousTime.time = flightToUpdate.towRelease ?: date
            else -> previousTime.time = flightToUpdate.landing ?: date
        }

        val hour = previousTime.get(Calendar.HOUR_OF_DAY)
        val minute = previousTime.get(Calendar.MINUTE)

        val picker = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            when (type) {
                "takeoff" -> {
                    flightToUpdate.setTakeoff(selectedHour, selectedMinute, date, true)
                    takeoffTimeIn.text = FlightEntry.hhcolonmmFromDate(flightToUpdate.takeoff!!)
                }

                "release" -> {
                    flightToUpdate.setTowRelease(selectedHour, selectedMinute, date, true)
                    releaseTimeIn.text = FlightEntry.hhcolonmmFromDate(flightToUpdate.towRelease!!)
                    if (flightToUpdate.getTowDurationMinutes() < 0) {
                        val tomorrow = Calendar.getInstance().apply {
                            time = date
                            add(Calendar.HOUR_OF_DAY, 24)
                        }
                        flightToUpdate.setTowRelease(selectedHour, selectedMinute, tomorrow.time, true)
                        releaseTimeIn.text = FlightEntry.hhcolonmmFromDate(flightToUpdate.towRelease!!)
                        Toast.makeText(
                            this,
                            "Release time set before takeoff time, assuming you mean the next day.",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }

                "landing" -> {
                    val previousLanding = flightToUpdate.landing
                    flightToUpdate.setLanding(selectedHour, selectedMinute, date, true)
                    landingTimeIn.text = FlightEntry.hhcolonmmFromDate(flightToUpdate.landing!!)
                    if (flightToUpdate.getFlightDurationMinutes() < 0) {
                        val tomorrow = Calendar.getInstance().apply {
                            time = date
                            add(Calendar.HOUR_OF_DAY, 24)
                        }
                        flightToUpdate.setLanding(selectedHour, selectedMinute, tomorrow.time, true)
                        landingTimeIn.text = FlightEntry.hhcolonmmFromDate(flightToUpdate.landing!!)
                        Toast.makeText(
                            this,
                            "Landing time set before takeoff time, assuming you mean the next day.",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                    if (flightToUpdate.getTowDurationMinutes() > flightToUpdate.getFlightDurationMinutes()) {
                        Toast.makeText(this, "Landing time must be after release time!", Toast.LENGTH_LONG)
                            .show()
                        flightToUpdate.landing = previousLanding
                        if (flightToUpdate.landing != null) {
                            landingTimeIn.text = FlightEntry.hhcolonmmFromDate(flightToUpdate.landing!!)
                        }
                    }
                }
            }
        }, hour, minute, true)

        picker.setMessage("Set $type time")
        picker.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableImmersiveFullscreen()
        }
    }
}
