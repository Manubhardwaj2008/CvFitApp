package com.example.arm64opencvcamera

import android.content.Intent
import com.example.arm64opencvcamera.R
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.ComposeView
import androidx.compose.material3.MaterialTheme
import com.manu.bsw017.HealthMonitorScreen

class HomeActivity : AppCompatActivity() {

    private lateinit var userSession: UserSession

    private lateinit var tabHome: View
    private lateinit var tabCare: View
    private lateinit var tabRecords: View
    private lateinit var tabProfile: View

    private lateinit var navItemHome: LinearLayout
    private lateinit var navItemCare: LinearLayout
    private lateinit var navItemRecords: LinearLayout
    private lateinit var navItemProfile: LinearLayout

    private lateinit var ivNavHome: ImageView
    private lateinit var ivNavCare: ImageView
    private lateinit var ivNavRecords: ImageView
    private lateinit var ivNavProfile: ImageView

    private lateinit var tvNavHome: TextView
    private lateinit var tvNavCare: TextView
    private lateinit var tvNavRecords: TextView
    private lateinit var tvNavProfile: TextView

    private lateinit var fabCenterScan: LinearLayout

    // Dynamic Greeting & Profile views
    private lateinit var tvGreeting: TextView
    private lateinit var tvGreetingSub: TextView
    private var tvProfileName: TextView? = null
    private var tvProfileAbha: TextView? = null
    private var btnProfileLogout: View? = null

    // Care tab slot selection state
    private var selectedSlotIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        userSession = UserSession(this)
        setContentView(R.layout.activity_home)

        initViews()
        updateUserAndGreetingUI()
        setupBottomNavigation()
        setupHomeTabActions()
        setupCareTabActions()
        setupProfileTabActions()
        setupTopBarActions()
        setupHealthMonitor()
    }

    override fun onResume() {
        super.onResume()
        updateUserAndGreetingUI()
    }

    private fun initViews() {
        tabHome = findViewById(R.id.tabHome)
        tabCare = findViewById(R.id.tabCare)
        tabRecords = findViewById(R.id.tabRecords)
        tabProfile = findViewById(R.id.tabProfile)

        navItemHome = findViewById(R.id.navItemHome)
        navItemCare = findViewById(R.id.navItemCare)
        navItemRecords = findViewById(R.id.navItemRecords)
        navItemProfile = findViewById(R.id.navItemProfile)

        ivNavHome = findViewById(R.id.ivNavHome)
        ivNavCare = findViewById(R.id.ivNavCare)
        ivNavRecords = findViewById(R.id.ivNavRecords)
        ivNavProfile = findViewById(R.id.ivNavProfile)

        tvNavHome = findViewById(R.id.tvNavHome)
        tvNavCare = findViewById(R.id.tvNavCare)
        tvNavRecords = findViewById(R.id.tvNavRecords)
        tvNavProfile = findViewById(R.id.tvNavProfile)

        fabCenterScan = findViewById(R.id.fabCenterScan)

        tvGreeting = findViewById(R.id.tvGreeting)
        tvGreetingSub = findViewById(R.id.tvGreetingSub)
        tvProfileName = findViewById(R.id.tvProfileName)
        tvProfileAbha = findViewById(R.id.tvProfileAbha)
        btnProfileLogout = findViewById(R.id.btnProfileLogout)
    }

    private fun updateUserAndGreetingUI() {
        val greeting = userSession.getDynamicGreeting()
        tvGreeting.text = greeting.title
        tvGreetingSub.text = greeting.subtitle

        tvProfileName?.text = userSession.userName
        tvProfileAbha?.text = "ABHA ID: ${userSession.userAbhaId}"
    }

    private fun setupBottomNavigation() {
        // Floating Center Scan Button: Launches OpenCV & AI Camera Scanner in MainActivity!
        fabCenterScan.setOnClickListener {
            launchCameraScanner()
        }

        navItemHome.setOnClickListener {
            selectTab(0)
        }

        navItemCare.setOnClickListener {
            selectTab(1)
        }

        navItemRecords.setOnClickListener {
            selectTab(2)
        }

        navItemProfile.setOnClickListener {
            selectTab(3)
        }
    }

    private fun selectTab(index: Int) {
        // Hide all tabs
        tabHome.visibility = if (index == 0) View.VISIBLE else View.GONE
        tabCare.visibility = if (index == 1) View.VISIBLE else View.GONE
        tabRecords.visibility = if (index == 2) View.VISIBLE else View.GONE
        tabProfile.visibility = if (index == 3) View.VISIBLE else View.GONE

        // Update nav icons and text styling
        val activeColor = ContextCompat.getColor(this, R.color.brand_navy_dark)
        val inactiveColor = ContextCompat.getColor(this, R.color.nav_icon_unselected)
        val inactiveTextColor = ContextCompat.getColor(this, R.color.nav_text_unselected)

        ivNavHome.setColorFilter(if (index == 0) activeColor else inactiveColor)
        tvNavHome.setTextColor(if (index == 0) activeColor else inactiveTextColor)
        tvNavHome.setTypeface(null, if (index == 0) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)

        ivNavCare.setColorFilter(if (index == 1) activeColor else inactiveColor)
        tvNavCare.setTextColor(if (index == 1) activeColor else inactiveTextColor)
        tvNavCare.setTypeface(null, if (index == 1) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)

        ivNavRecords.setColorFilter(if (index == 2) activeColor else inactiveColor)
        tvNavRecords.setTextColor(if (index == 2) activeColor else inactiveTextColor)
        tvNavRecords.setTypeface(null, if (index == 2) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)

        ivNavProfile.setColorFilter(if (index == 3) activeColor else inactiveColor)
        tvNavProfile.setTextColor(if (index == 3) activeColor else inactiveTextColor)
        tvNavProfile.setTypeface(null, if (index == 3) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
    }

    private fun launchCameraScanner() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private fun setupHomeTabActions() {
        // Hero Scan Symptom -> launches MainActivity camera
        val btnHomeScanSymptom = findViewById<View>(R.id.btnHomeScanSymptom)
        btnHomeScanSymptom?.setOnClickListener {
            launchCameraScanner()
        }

        // Hero Scan Report -> open document picker or camera
        val btnHomeScanReport = findViewById<View>(R.id.btnHomeScanReport)
        btnHomeScanReport?.setOnClickListener {
            Toast.makeText(this, "Opening Document & Medical Report Scanner...", Toast.LENGTH_SHORT).show()
            launchCameraScanner()
        }

        // Live Queue Card -> Switch to Care Tab
        val cardLiveQueue = findViewById<View>(R.id.cardLiveQueue)
        cardLiveQueue?.setOnClickListener {
            selectTab(1)
        }

        // Doctor Booking Button
        val btnBookOpdSlot = findViewById<View>(R.id.btnBookOpdSlot)
        btnBookOpdSlot?.setOnClickListener {
            selectTab(1)
        }

        // Call Doctor Button
        val btnCallDoctor = findViewById<View>(R.id.btnCallDoctor)
        btnCallDoctor?.setOnClickListener {
            val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:104"))
            startActivity(callIntent)
        }

        // Quick Actions
        val cardFindHealthcare = findViewById<View>(R.id.cardFindHealthcare)
        cardFindHealthcare?.setOnClickListener {
            selectTab(1)
        }

        val cardMedicineStock = findViewById<View>(R.id.cardMedicineStock)
        cardMedicineStock?.setOnClickListener {
            Toast.makeText(this, "PHC Wakad: Paracetamol (420), Cetirizine (180), Amoxicillin (95) In Stock", Toast.LENGTH_LONG).show()
        }

        val cardMyAppointments = findViewById<View>(R.id.cardMyAppointments)
        cardMyAppointments?.setOnClickListener {
            selectTab(1)
        }

        val cardHealthRecords = findViewById<View>(R.id.cardHealthRecords)
        cardHealthRecords?.setOnClickListener {
            selectTab(2)
        }

        // Emergency 108 Banner
        val cardEmergencyHelp = findViewById<View>(R.id.cardEmergencyHelp)
        cardEmergencyHelp?.setOnClickListener {
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:108"))
            startActivity(dialIntent)
        }

        // Nearby Care item buttons
        val btnViewPhcWakad = findViewById<View>(R.id.btnViewPhcWakad)
        btnViewPhcWakad?.setOnClickListener {
            selectTab(1)
        }

        val btnViewChcBaner = findViewById<View>(R.id.btnViewChcBaner)
        btnViewChcBaner?.setOnClickListener {
            Toast.makeText(this, "Community Health Centre Baner • 6.8 km away • Open 24x7", Toast.LENGTH_SHORT).show()
        }

        val tvSeeAllNearby = findViewById<View>(R.id.tvSeeAllNearby)
        tvSeeAllNearby?.setOnClickListener {
            selectTab(1)
        }
    }

    private fun setupCareTabActions() {
        val btnSelectPhcWakad = findViewById<View>(R.id.btnSelectPhcWakad)
        btnSelectPhcWakad?.setOnClickListener {
            Toast.makeText(this, "Selected PHC Wakad (2.4 km away)", Toast.LENGTH_SHORT).show()
        }

        val btnDirectionsCare = findViewById<View>(R.id.btnDirectionsCare)
        btnDirectionsCare?.setOnClickListener {
            openDirections("Primary Health Centre Wakad")
        }

        val btnDirectionQueue = findViewById<View>(R.id.btnDirectionQueue)
        btnDirectionQueue?.setOnClickListener {
            openDirections("PHC Wakad")
        }

        val btnShowReferralQr = findViewById<View>(R.id.btnShowReferralQr)
        btnShowReferralQr?.setOnClickListener {
            showReferralQrDialog()
        }

        // Facility Filter Chips
        val chipAll = findViewById<TextView>(R.id.chipFacilityAll)
        val chipPhc = findViewById<TextView>(R.id.chipFacilityPhc)
        val chipChc = findViewById<TextView>(R.id.chipFacilityChc)
        val chipHospital = findViewById<TextView>(R.id.chipFacilityHospital)

        val facilityChips = listOf(chipAll, chipPhc, chipChc, chipHospital)
        facilityChips.forEachIndexed { index, chip ->
            chip?.setOnClickListener {
                facilityChips.forEach { c ->
                    c?.setBackgroundResource(R.drawable.bg_chip_unselected)
                    c?.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                }
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(ContextCompat.getColor(this, R.color.text_white))
            }
        }

        // Date Chips
        val chipDateToday = findViewById<TextView>(R.id.chipDateToday)
        val chipDateTomorrow = findViewById<TextView>(R.id.chipDateTomorrow)
        val chipDateSat = findViewById<TextView>(R.id.chipDateSat)

        val dateChips = listOf(chipDateToday, chipDateTomorrow, chipDateSat)
        dateChips.forEachIndexed { _, chip ->
            chip?.setOnClickListener {
                dateChips.forEach { c ->
                    c?.setBackgroundResource(R.drawable.bg_chip_unselected)
                    c?.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                }
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(ContextCompat.getColor(this, R.color.text_white))
            }
        }

        // Appointment Time Slots
        val slot1 = findViewById<LinearLayout>(R.id.slot1)
        val slot2 = findViewById<LinearLayout>(R.id.slot2)
        val slot3 = findViewById<LinearLayout>(R.id.slot3)
        val slot4 = findViewById<LinearLayout>(R.id.slot4)

        val dot1 = findViewById<View>(R.id.dotSlot1)
        val dot2 = findViewById<View>(R.id.dotSlot2)
        val dot3 = findViewById<View>(R.id.dotSlot3)
        val dot4 = findViewById<View>(R.id.dotSlot4)

        val slots = listOf(slot1, slot2, slot3, slot4)
        val dots = listOf(dot1, dot2, dot3, dot4)

        slots.forEachIndexed { index, slotView ->
            slotView?.setOnClickListener {
                selectedSlotIndex = index
                slots.forEachIndexed { i, s ->
                    if (i == index) {
                        s?.setBackgroundResource(R.drawable.bg_slot_selected)
                        dots[i]?.setBackgroundResource(R.drawable.bg_floating_scan_button)
                    } else {
                        s?.setBackgroundResource(R.drawable.bg_slot_unselected)
                        dots[i]?.setBackgroundResource(R.drawable.bg_circle_phone)
                    }
                }
            }
        }

        val btnBookAppointment = findViewById<View>(R.id.btnBookAppointmentGenerateToken)
        btnBookAppointment?.setOnClickListener {
            val time = when (selectedSlotIndex) {
                0 -> "10:30 AM"
                1 -> "12:00 PM"
                2 -> "02:30 PM"
                else -> "04:30 PM"
            }
            Toast.makeText(this, "Token A-42 confirmed for $time at PHC Wakad!", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupTopBarActions() {
        val pillLocation = findViewById<View>(R.id.pillLocation)
        pillLocation?.setOnClickListener {
            Toast.makeText(this, "Location: Wakad, Pune (2.4 km to nearest PHC)", Toast.LENGTH_SHORT).show()
        }

        val btnVoiceAssistant = findViewById<View>(R.id.btnVoiceAssistant)
        btnVoiceAssistant?.setOnClickListener {
            Toast.makeText(this, "Voice Assistant listening... Speak your symptom in Hindi, Marathi, or English.", Toast.LENGTH_SHORT).show()
        }

        val btnLanguageSelect = findViewById<View>(R.id.btnLanguageSelect)
        val tvCurrentLanguage = findViewById<TextView>(R.id.tvCurrentLanguage)
        btnLanguageSelect?.setOnClickListener {
            val languages = arrayOf("English", "मराठी (Marathi)", "हिंदी (Hindi)")
            AlertDialog.Builder(this)
                .setTitle("Select Language")
                .setItems(languages) { _, which ->
                    tvCurrentLanguage?.text = when (which) {
                        0 -> "English"
                        1 -> "मराठी"
                        else -> "हिंदी"
                    }
                    Toast.makeText(this, "Language switched to ${languages[which]}", Toast.LENGTH_SHORT).show()
                }
                .show()
        }
    }

    private fun openDirections(query: String) {
        val gmmIntentUri = Uri.parse("geo:0,0?q=$query")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        try {
            startActivity(mapIntent)
        } catch (e: Exception) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$query"))
            startActivity(browserIntent)
        }
    }

    private fun setupProfileTabActions() {
        btnProfileLogout?.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out from ${userSession.userName}?")
                .setPositiveButton("Sign Out") { _, _ ->
                    userSession.logout()
                    Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show()
                    val loginIntent = Intent(this, LoginActivity::class.java)
                    loginIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(loginIntent)
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showReferralQrDialog() {
        AlertDialog.Builder(this)
            .setTitle("Digital Referral QR")
            .setMessage("Token: A-42\nFacility: PHC Wakad\nPatient: ${userSession.userName} (ABHA: ${userSession.userAbhaId})\nTriage: Moderate Risk (Skin Irritation)\n\nPlease show this QR token at the PHC reception desk.")
            .setIcon(R.drawable.ic_scan_qr)
            .setPositiveButton("Done", null)
            .show()
    }

    private fun setupHealthMonitor() {
        val composeViewHome = findViewById<ComposeView>(R.id.composeViewHealthMonitorHome)
        composeViewHome?.setContent {
            MaterialTheme {
                HealthMonitorScreen(showTitle = false)
            }
        }

        val composeViewProfile = findViewById<ComposeView>(R.id.composeViewHealthMonitorProfile)
        composeViewProfile?.setContent {
            MaterialTheme {
                HealthMonitorScreen(showTitle = false)
            }
        }
    }
}
