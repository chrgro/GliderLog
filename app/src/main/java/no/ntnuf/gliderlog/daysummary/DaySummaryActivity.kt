package no.ntnuf.gliderlog.daysummary

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.preference.PreferenceManager
import no.ntnuf.gliderlog.R
import no.ntnuf.gliderlog.common.DayLog
import no.ntnuf.gliderlog.common.FlightStatus
import no.ntnuf.gliderlog.common.FlightTime
import no.ntnuf.gliderlog.common.FlightTimeList
import no.ntnuf.gliderlog.common.NameRegistrationRole
import no.ntnuf.gliderlog.common.PilotType
import no.ntnuf.gliderlog.common.applyContentInsets
import no.ntnuf.gliderlog.common.applyToolbarInsets
import no.ntnuf.gliderlog.common.enableImmersiveFullscreen
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.random.Random

class DaySummaryActivity : AppCompatActivity() {
    private lateinit var daylog: DayLog
    private lateinit var summaryType: String
    private lateinit var tableLayout: TableLayout
    private var roundSumsTo: Int = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveFullscreen()
        setContentView(R.layout.activity_day_summary)

        daylog = intent.getSerializableExtra("daylog") as? DayLog ?: return finish()
        summaryType = intent.getSerializableExtra("summary_type") as? String ?: "pilot"

        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        roundSumsTo = settings.getString("round_sums_to", "5")?.toIntOrNull() ?: 5

        val toolbar = findViewById<Toolbar>(R.id.toolbardaysummary)
        val dateStr = SimpleDateFormat("EEEE d/M", Locale.ENGLISH).format(daylog.date)
        toolbar.title = if (summaryType == "pilot") {
            "Pilot Summary  -  $dateStr"
        } else {
            "Plane Summary  -  $dateStr"
        }
        setSupportActionBar(toolbar)
        applyToolbarInsets(toolbar)
        applyContentInsets(findViewById<ScrollView>(R.id.daySummaryScroll))

        val title = findViewById<TextView>(R.id.sum_title)
        title.text = getString(R.string.summary_sums_nearest, roundSumsTo)

        tableLayout = findViewById(R.id.daySummaryTableLayout)
        refreshTable()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.daysummary_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_return_to_log -> {
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun refreshTable() {
        val headerRows = 2
        val rowsToRemove = tableLayout.childCount - headerRows
        if (rowsToRemove > 0) {
            tableLayout.removeViews(headerRows, rowsToRemove)
        }

        val flightTimeList = FlightTimeList()
        var allFlightsLanded = true
        for (flight in daylog.flights) {
            if (flight.status != FlightStatus.LANDED) {
                allFlightsLanded = false
            }
            flightTimeList.add(flight)
        }
        flightTimeList.round(roundSumsTo)

        if (!allFlightsLanded) {
            Toast.makeText(this, R.string.summary_only_landed_notice, Toast.LENGTH_LONG).show()
        }

        if (summaryType == "pilot") {
            var previousName: String? = null
            for ((key, value) in flightTimeList.sortedPilotSums()) {
                val separator = View(this).apply {
                    val height = if (previousName != null && previousName != key.name) 5 else 1
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
                    setBackgroundColor(resources.getColor(R.color.black, theme))
                }
                tableLayout.addView(separator)
                tableLayout.addView(addSummaryRow(key, value))
                previousName = key.name
            }
        } else {
            for ((key, value) in flightTimeList.sortedPlaneSums()) {
                tableLayout.addView(addSummaryRow(key, value))
                val separator = View(this).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
                    setBackgroundColor(resources.getColor(R.color.black, theme))
                }
                tableLayout.addView(separator)
            }
        }
    }

    private fun addSummaryRow(info: NameRegistrationRole, flightTime: FlightTime): TableRow {
        val row = TableRow(this)

        val nameContainer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

        if (!info.isPlane) {
            val name = info.name.orEmpty()
            nameContainer.addView(createAvatarView(name))
        }

        val nameRoleReg = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.gravity = Gravity.CENTER_VERTICAL
            layoutParams = lp
        }
        if (!info.isPlane) {
            nameRoleReg.addView(TextView(this).apply { text = info.name.orEmpty() })
            nameRoleReg.addView(TextView(this).apply { text = info.role?.toString() ?: "" })
        }
        nameRoleReg.addView(TextView(this).apply { text = info.registration })
        nameContainer.addView(nameRoleReg)
        row.addView(nameContainer)

        val labels = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val values = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        labels.addView(TextView(this).apply { text = if (flightTime.landed) "Flights:" else "Not landed! Flights:" })
        values.addView(TextView(this).apply { text = flightTime.flights.toString() })

        labels.addView(TextView(this).apply { text = "Flight Duration:" })
        values.addView(TextView(this).apply { text = minutesToDurationStr(flightTime.flightMinutes) })

        labels.addView(TextView(this).apply { text = "Tow Duration:" })
        values.addView(TextView(this).apply { text = minutesToDurationStr(flightTime.towMinutes) })

        if (!info.isPlane && info.role == PilotType.INSTRUCTOR) {
            labels.addView(TextView(this).apply { text = "Instructor Duration:" })
            values.addView(TextView(this).apply { text = minutesToDurationStr(flightTime.instructorMinutes) })
        }

        if (!info.isPlane && info.role == PilotType.STUDENT) {
            labels.addView(TextView(this).apply { text = "Student Duration:" })
            values.addView(TextView(this).apply { text = minutesToDurationStr(flightTime.studentMinutes) })
        }

        row.addView(labels)
        row.addView(values)
        return row
    }

    private fun nameToInitials(name: String): String {
        val words = name.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        return when {
            words.isEmpty() -> "??"
            words.size == 1 -> words[0].take(2).uppercase(Locale.getDefault())
            else -> "${words.first()[0]}${words.last()[0]}".uppercase(Locale.getDefault())
        }
    }

    private fun nameToColor(name: String): Int {
        val rng = Random(name.hashCode())
        val hue = rng.nextFloat() * 360f
        return Color.HSVToColor(floatArrayOf(hue, 0.60f, 0.80f))
    }

    private fun contrastColor(bgColor: Int): Int {
        val luminance = 0.299 * (Color.red(bgColor) / 255.0) +
                        0.587 * (Color.green(bgColor) / 255.0) +
                        0.114 * (Color.blue(bgColor) / 255.0)
        return if (luminance > 0.5) Color.BLACK else Color.WHITE
    }

    private fun createAvatarView(name: String): TextView {
        val bgColor = nameToColor(name)
        val size = (40 * resources.displayMetrics.density).toInt()
        val marginEnd = (8 * resources.displayMetrics.density).toInt()
        return TextView(this).apply {
            text = nameToInitials(name)
            setTextColor(contrastColor(bgColor))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(bgColor)
            }
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(0, 4, marginEnd, 4)
                gravity = Gravity.CENTER_VERTICAL
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableImmersiveFullscreen()
        }
    }

    companion object {
        fun minutesToDurationStr(minutes: Long): String {
            val hours = minutes / 60
            val minutesInHour = minutes % 60
            return "${hours}h ${minutesInHour}m"
        }
    }
}
