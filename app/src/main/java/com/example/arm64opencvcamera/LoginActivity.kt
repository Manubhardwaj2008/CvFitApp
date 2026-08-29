package com.example.arm64opencvcamera

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etPassword: EditText
    private lateinit var ivPasswordToggle: ImageView
    private lateinit var cbRememberMe: CheckBox
    private lateinit var btnSignIn: View
    private lateinit var btnDemoSignIn: View
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvErrorMessage: TextView

    private lateinit var userSession: UserSession
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        userSession = UserSession(this)

        // Check if user is already logged in
        if (userSession.isLoggedIn && userSession.isRememberMe) {
            navigateToHome()
            return
        }

        setContentView(R.layout.activity_login)

        initViews()
        setupListeners()
        populateSavedDetails()
    }

    private fun initViews() {
        etName = findViewById(R.id.etName)
        etPhone = findViewById(R.id.etPhone)
        etPassword = findViewById(R.id.etPassword)
        ivPasswordToggle = findViewById(R.id.ivPasswordToggle)
        cbRememberMe = findViewById(R.id.cbRememberMe)
        btnSignIn = findViewById(R.id.btnSignIn)
        btnDemoSignIn = findViewById(R.id.btnDemoSignIn)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tvErrorMessage = findViewById(R.id.tvErrorMessage)
    }

    private fun setupListeners() {
        // Password Visibility Toggle
        ivPasswordToggle.setOnClickListener {
            togglePasswordVisibility()
        }

        // Primary Sign-In
        btnSignIn.setOnClickListener {
            handleSignIn()
        }

        // Quick Demo Sign-In
        btnDemoSignIn.setOnClickListener {
            handleDemoSignIn()
        }

        // Forgot Password / Assistance
        tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun populateSavedDetails() {
        if (userSession.userName.isNotBlank() && userSession.userName != UserSession.DEFAULT_USER_NAME) {
            etName.setText(userSession.userName)
        }
        if (userSession.userPhone.isNotBlank() && userSession.userPhone != UserSession.DEFAULT_USER_PHONE) {
            // Strip any non-digits for the 10-digit input field
            val digits = userSession.userPhone.filter { it.isDigit() }.takeLast(10)
            etPhone.setText(digits)
        }
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        if (isPasswordVisible) {
            etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            ivPasswordToggle.setImageResource(R.drawable.ic_eye_open)
        } else {
            etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            ivPasswordToggle.setImageResource(R.drawable.ic_eye_closed)
        }
        etPassword.setSelection(etPassword.text.length)
    }

    private fun handleSignIn() {
        tvErrorMessage.visibility = View.GONE

        val name = etName.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val rememberMe = cbRememberMe.isChecked

        // Validation
        if (name.isBlank()) {
            showError("Please enter your full name.")
            etName.requestFocus()
            return
        }

        if (name.length < 2) {
            showError("Please enter a valid name (at least 2 characters).")
            etName.requestFocus()
            return
        }

        if (phone.isBlank()) {
            showError("Please enter your 10-digit mobile number.")
            etPhone.requestFocus()
            return
        }

        val digitsOnly = phone.filter { it.isDigit() }
        if (digitsOnly.length != 10) {
            showError("Mobile number must be exactly 10 digits.")
            etPhone.requestFocus()
            return
        }

        if (password.isBlank()) {
            showError("Please enter your password.")
            etPassword.requestFocus()
            return
        }

        if (password.length < 4) {
            showError("Password must be at least 4 characters.")
            etPassword.requestFocus()
            return
        }

        // Save session & navigate
        userSession.saveUser(
            name = name,
            phone = "+91 $digitsOnly",
            rememberMe = rememberMe
        )

        Toast.makeText(this, "Welcome, $name!", Toast.LENGTH_SHORT).show()
        navigateToHome()
    }

    private fun handleDemoSignIn() {
        userSession.saveUser(
            name = "Any name",
            phone = "+91",
            rememberMe = true
        )

        Toast.makeText(this, "Signed in as Demo Patient (any)", Toast.LENGTH_SHORT).show()
        navigateToHome()
    }

    private fun showError(message: String) {
        tvErrorMessage.text = message
        tvErrorMessage.visibility = View.VISIBLE
    }

    private fun showForgotPasswordDialog() {
        AlertDialog.Builder(this)
            .setTitle("Account Recovery")
            .setMessage("For security in offline/clinical environments, please contact your local PHC Administrator or use OTP login via your registered ABHA ID.")
            .setPositiveButton("Use Demo Account") { _, _ ->
                handleDemoSignIn()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}