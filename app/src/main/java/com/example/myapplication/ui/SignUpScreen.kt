package com.example.myapplication.ui

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.data.repository.AuthRepository
import kotlinx.coroutines.delay

private val Black = Color.Black
private val ScreenBackground = Color(0xFFF2F5F9)
private val FieldBorder = Color(0xFFC5CEDA)
private val HintGray = Color(0xFF8A94A0)

enum class SignUpStep { DETAILS, VERIFICATION }

@Composable
fun SignUpScreen(
    authRepository: AuthRepository,
    onSignUpSuccess: (email: String, fullName: String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var currentStep by remember { mutableStateOf(SignUpStep.DETAILS) }

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }

    var resendCountdown by remember { mutableIntStateOf(60) }
    var isTimerRunning by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val emailIsValid = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()

    LaunchedEffect(isTimerRunning, resendCountdown) {
        if (isTimerRunning && resendCountdown > 0) {
            delay(1000L)
            resendCountdown -= 1
        } else if (resendCountdown == 0) {
            isTimerRunning = false
        }
    }

    fun showToast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    }

    fun handleSendVerificationEmail() {
        when {
            fullName.isBlank() || email.isBlank() || phone.isBlank() || password.isBlank() || confirmPassword.isBlank() -> {
                showToast("Please fill in all fields")
            }
            !emailIsValid -> {
                showToast("Please enter a valid email address")
            }
            password.length < 6 -> {
                showToast("Password must be at least 6 characters")
            }
            password != confirmPassword -> {
                showToast("Passwords do not match. Please try again.")
            }
            else -> {
                isLoading = true
                authRepository.sendFirebaseEmailVerificationLink(
                    email = email.trim(),
                    password = password,
                    fullName = fullName.trim(),
                    phone = phone.trim(),
                    onSuccess = {
                        isLoading = false
                        currentStep = SignUpStep.VERIFICATION
                        resendCountdown = 60
                        isTimerRunning = true
                        showToast("Verification email sent to ${email.trim()}. Check your inbox!")
                    },
                    onError = { error ->
                        isLoading = false
                        showToast(error)
                    }
                )
            }
        }
    }

    fun handleCheckEmailVerificationStatus() {
        isLoading = true
        authRepository.checkEmailVerifiedAndCompleteRegistration(
            email = email.trim(),
            password = password,
            fullName = fullName.trim(),
            phone = phone.trim(),
            onSuccess = {
                isLoading = false
                showToast("Email verified successfully! Welcome to Lost But Found.")
                onSignUpSuccess(email.trim(), fullName.trim())
            },
            onError = { error ->
                isLoading = false
                showToast(error)
            }
        )
    }

    fun handleResendEmailLink() {
        isLoading = true
        authRepository.resendFirebaseEmailVerificationLink(
            email = email.trim(),
            password = password,
            onSuccess = {
                isLoading = false
                resendCountdown = 60
                isTimerRunning = true
                showToast("A new verification link has been sent to ${email.trim()}")
            },
            onError = { error ->
                isLoading = false
                showToast(error)
            }
        )
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Black,
        unfocusedBorderColor = FieldBorder,
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        focusedPlaceholderColor = HintGray,
        unfocusedPlaceholderColor = HintGray,
        cursorColor = Black
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Image(
            painter = painterResource(R.drawable.ic_app_logo),
            contentDescription = "App Logo",
            modifier = Modifier.size(110.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (currentStep == SignUpStep.DETAILS) "Create Account" else "Verify Your Email",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            fontFamily = FontFamily.SansSerif
        )
        Text(
            text = if (currentStep == SignUpStep.DETAILS) "Sign up for Lost But Found" else "Check your email inbox for the verification link",
            fontSize = 14.sp,
            color = Color(0xFF555555),
            fontFamily = FontFamily.SansSerif
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                if (currentStep == SignUpStep.DETAILS) {
                    Text("Full Name", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("John Doe", fontSize = 14.sp, color = HintGray) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Black) },
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Email Address", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("student@domain.com", fontSize = 14.sp, color = HintGray) },
                        isError = email.isNotEmpty() && !emailIsValid,
                        supportingText = {
                            if (email.isNotEmpty() && !emailIsValid) {
                                Text("Invalid email address", fontSize = 12.sp, color = Color(0xFFB3261E))
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Black) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Phone Number", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("+265 ...", fontSize = 14.sp, color = HintGray) },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Black) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Password", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("At least 6 characters", fontSize = 14.sp, color = HintGray) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Black) },
                        trailingIcon = {
                            TextButton(onClick = { passwordVisible = !passwordVisible }) {
                                Text(if (passwordVisible) "Hide" else "Show", fontSize = 12.sp, color = Black)
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Confirm Password", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Re-enter your password", fontSize = 14.sp, color = HintGray) },
                        isError = confirmPassword.isNotEmpty() && password != confirmPassword,
                        supportingText = {
                            if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                                Text("Passwords do not match", fontSize = 12.sp, color = Color(0xFFB3261E))
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Black) },
                        trailingIcon = {
                            TextButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Text(if (confirmPasswordVisible) "Hide" else "Show", fontSize = 12.sp, color = Black)
                            }
                        },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { handleSendVerificationEmail() },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Black)
                    ) {
                        Text(
                            text = if (isLoading) "Sending Link..." else "Send Verification Email",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    // EMAIL LINK VERIFICATION STEP
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.MarkEmailRead,
                            contentDescription = "Email Verification",
                            modifier = Modifier.size(56.dp),
                            tint = Black
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Verification Email Sent",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "We sent a verification link from Firebase to:",
                            fontSize = 13.sp,
                            color = Color(0xFF666666),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = email.trim(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Instructions Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F5F9))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("1. Open your email inbox for ${email.trim()}", fontSize = 13.sp, color = Black)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("2. Click the verification link sent by Firebase", fontSize = 13.sp, color = Black)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("3. Return here & tap 'I Have Verified My Email'", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Black)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { handleCheckEmailVerificationStatus() },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Black)
                        ) {
                            Text(
                                text = if (isLoading) "Checking Verification..." else "I Have Verified My Email",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    currentStep = SignUpStep.DETAILS
                                },
                                enabled = !isLoading
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        modifier = Modifier.size(16.dp),
                                        tint = Black
                                    )
                                    Spacer(modifier = Modifier.size(4.dp))
                                    Text("Edit Info", fontSize = 13.sp, color = Black, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            TextButton(
                                onClick = { handleResendEmailLink() },
                                enabled = !isLoading && !isTimerRunning
                            ) {
                                Text(
                                    text = if (isTimerRunning) "Resend in ${resendCountdown}s" else "Resend Email",
                                    fontSize = 13.sp,
                                    color = if (isTimerRunning) HintGray else Black,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Already have an account?", fontSize = 14.sp, color = Color(0xFF555555))
            Text(
                text = " Log in",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Black,
                modifier = Modifier.clickable { onNavigateToLogin() }
            )
        }
    }
}
