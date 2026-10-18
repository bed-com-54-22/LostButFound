package com.example.myapplication.data.repository

import android.content.Context
import com.example.myapplication.data.local.UserDao
import com.example.myapplication.data.local.UserEntity
import com.example.myapplication.service.EmailVerificationService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthRepository(private val userDao: UserDao) {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    fun checkEmailExists(email: String, onResult: (Boolean) -> Unit) {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isBlank()) {
            onResult(false)
            return
        }

        firestore.collection("users").document(trimmedEmail).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    onResult(true)
                } else {
                    // Document does NOT exist in Firestore (e.g. deleted from Firebase)
                    // Clean up any stale local database entry so user can re-register
                    CoroutineScope(Dispatchers.IO).launch {
                        userDao.deleteUserByEmail(trimmedEmail)
                        withContext(Dispatchers.Main) {
                            onResult(false)
                        }
                    }
                }
            }
            .addOnFailureListener {
                // Offline fallback
                CoroutineScope(Dispatchers.IO).launch {
                    val localUser = userDao.getUserByEmail(trimmedEmail)
                    withContext(Dispatchers.Main) {
                        onResult(localUser != null)
                    }
                }
            }
    }

    fun sendFirebaseEmailVerificationLink(
        email: String,
        password: String,
        fullName: String,
        phone: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val trimmedEmail = email.trim().lowercase()
        val trimmedPassword = password.trim()

        checkEmailExists(trimmedEmail) { exists ->
            if (exists) {
                onError("An account with this email address already exists.")
                return@checkEmailExists
            }

            auth.createUserWithEmailAndPassword(trimmedEmail, trimmedPassword)
                .addOnSuccessListener { authResult ->
                    val user = authResult.user
                    if (user != null) {
                        user.sendEmailVerification()
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { e ->
                                onError(e.localizedMessage ?: "Failed to send verification email.")
                            }
                    } else {
                        onSuccess()
                    }
                }
                .addOnFailureListener { e ->
                    if (e.message?.contains("already in use", ignoreCase = true) == true) {
                        auth.signInWithEmailAndPassword(trimmedEmail, trimmedPassword)
                            .addOnSuccessListener { authResult ->
                                val user = authResult.user
                                if (user != null) {
                                    if (user.isEmailVerified) {
                                        onError("An account with this email address already exists. Please log in.")
                                    } else {
                                        user.sendEmailVerification()
                                            .addOnSuccessListener { onSuccess() }
                                            .addOnFailureListener { err ->
                                                onError(err.localizedMessage ?: "Failed to send verification email.")
                                            }
                                    }
                                } else {
                                    onError(e.localizedMessage ?: "Failed to send verification email.")
                                }
                            }
                            .addOnFailureListener {
                                onError("An account with this email address already exists. Please log in.")
                            }
                    } else {
                        onError(e.localizedMessage ?: "Failed to create account in Firebase.")
                    }
                }
        }
    }

    fun checkEmailVerifiedAndCompleteRegistration(
        email: String,
        password: String,
        fullName: String,
        phone: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val trimmedEmail = email.trim().lowercase()
        val trimmedPassword = password.trim()
        val currentUser = auth.currentUser

        if (currentUser != null && currentUser.email?.equals(trimmedEmail, ignoreCase = true) == true) {
            currentUser.reload()
                .addOnSuccessListener {
                    if (currentUser.isEmailVerified) {
                        saveVerifiedUserLocallyAndRemote(trimmedEmail, trimmedPassword, fullName, phone, onSuccess)
                    } else {
                        onError("Email is not verified yet. Please check your email inbox ($trimmedEmail) and click the verification link.")
                    }
                }
                .addOnFailureListener {
                    auth.signInWithEmailAndPassword(trimmedEmail, trimmedPassword)
                        .addOnSuccessListener { authResult ->
                            val user = authResult.user
                            user?.reload()?.addOnSuccessListener {
                                if (user.isEmailVerified) {
                                    saveVerifiedUserLocallyAndRemote(trimmedEmail, trimmedPassword, fullName, phone, onSuccess)
                                } else {
                                    onError("Email is not verified yet. Please check your email inbox ($trimmedEmail) and click the verification link.")
                                }
                            } ?: onError("User error. Please try again.")
                        }
                        .addOnFailureListener { err ->
                            onError(err.localizedMessage ?: "Please check your credentials and try again.")
                        }
                }
        } else {
            auth.signInWithEmailAndPassword(trimmedEmail, trimmedPassword)
                .addOnSuccessListener { authResult ->
                    val user = authResult.user
                    user?.reload()?.addOnSuccessListener {
                        if (user.isEmailVerified) {
                            saveVerifiedUserLocallyAndRemote(trimmedEmail, trimmedPassword, fullName, phone, onSuccess)
                        } else {
                            onError("Email is not verified yet. Please check your email inbox ($trimmedEmail) and click the verification link.")
                        }
                    } ?: onError("User error. Please try again.")
                }
                .addOnFailureListener { e ->
                    onError(e.localizedMessage ?: "Please check your credentials and try again.")
                }
        }
    }

    fun resendFirebaseEmailVerificationLink(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val trimmedEmail = email.trim().lowercase()
        val currentUser = auth.currentUser

        if (currentUser != null && currentUser.email?.equals(trimmedEmail, ignoreCase = true) == true) {
            currentUser.sendEmailVerification()
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e -> onError(e.localizedMessage ?: "Failed to resend verification email.") }
        } else {
            auth.signInWithEmailAndPassword(trimmedEmail, password)
                .addOnSuccessListener { authResult ->
                    authResult.user?.sendEmailVerification()
                        ?.addOnSuccessListener { onSuccess() }
                        ?.addOnFailureListener { e -> onError(e.localizedMessage ?: "Failed to resend verification email.") }
                }
                .addOnFailureListener { e ->
                    onError(e.localizedMessage ?: "Failed to resend verification email.")
                }
        }
    }

    private fun saveVerifiedUserLocallyAndRemote(
        email: String,
        password: String,
        fullName: String,
        phone: String,
        onSuccess: () -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: ""
        val userData = mapOf(
            "uid" to uid,
            "email" to email,
            "fullName" to fullName,
            "phone" to phone,
            "location" to "",
            "isEmailVerified" to true
        )

        firestore.collection("users").document(email).set(userData)

        val localUser = UserEntity(
            email = email,
            password = password,
            fullName = fullName,
            phone = phone,
            location = ""
        )

        CoroutineScope(Dispatchers.IO).launch {
            userDao.insertUser(localUser)
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    fun sendVerificationCode(
        context: Context,
        email: String,
        onSuccess: (code: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isBlank()) {
            onError("Please enter a valid email address.")
            return
        }

        checkEmailExists(trimmedEmail) { exists ->
            if (exists) {
                onError("An account with this email address already exists.")
                return@checkEmailExists
            }

            val code = EmailVerificationService.generateCode(trimmedEmail)
            CoroutineScope(Dispatchers.IO).launch {
                val result = EmailVerificationService.sendVerificationCode(
                    context = context,
                    recipientEmail = trimmedEmail,
                    code = code
                )
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        onSuccess(code)
                    } else {
                        onError(result.exceptionOrNull()?.localizedMessage ?: "Failed to send verification code email.")
                    }
                }
            }
        }
    }

    fun verifyCodeAndSignUp(
        email: String,
        code: String,
        password: String,
        fullName: String,
        phone: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val trimmedEmail = email.trim().lowercase()
        val (isValid, message) = EmailVerificationService.verifyCode(trimmedEmail, code)

        if (!isValid) {
            onError(message)
            return
        }

        signUp(
            email = trimmedEmail,
            password = password,
            fullName = fullName,
            phone = phone,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    fun signUp(
        email: String,
        password: String,
        fullName: String,
        phone: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()

        checkEmailExists(trimmedEmail) { exists ->
            if (exists) {
                onError("An account with this email address already exists.")
                return@checkEmailExists
            }

            val localUser = UserEntity(
                email = trimmedEmail.lowercase(),
                password = trimmedPassword,
                fullName = fullName,
                phone = phone,
                location = ""
            )

            CoroutineScope(Dispatchers.IO).launch {
                // Save locally first so account creation is instant
                userDao.insertUser(localUser)
                withContext(Dispatchers.Main) {
                    onSuccess()

                    // Register with Firebase in background
                    auth.createUserWithEmailAndPassword(trimmedEmail, trimmedPassword)
                        .addOnSuccessListener { authResult ->
                            val uid = authResult.user?.uid ?: ""
                            val userData = mapOf(
                                "uid" to uid,
                                "email" to trimmedEmail.lowercase(),
                                "fullName" to fullName,
                                "phone" to phone,
                                "location" to ""
                            )
                            firestore.collection("users").document(trimmedEmail.lowercase()).set(userData)
                        }
                        .addOnFailureListener { _ ->
                            // Account already created locally
                        }
                }
            }
        }
    }

    fun login(
        email: String,
        password: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val trimmedEmail = email.trim().lowercase()
        val trimmedPassword = password.trim()

        CoroutineScope(Dispatchers.IO).launch {
            val localUser = userDao.getUserByEmail(trimmedEmail)

            // Fast offline check: If user exists in Room DB and password matches, log in immediately!
            if (localUser != null && localUser.password == trimmedPassword) {
                withContext(Dispatchers.Main) {
                    onSuccess(trimmedEmail)
                }
                // Sync with Firebase in background
                try {
                    auth.signInWithEmailAndPassword(trimmedEmail, trimmedPassword)
                } catch (_: Exception) {}
                return@launch
            }

            // Otherwise, attempt Firebase Auth
            withContext(Dispatchers.Main) {
                auth.signInWithEmailAndPassword(trimmedEmail, trimmedPassword)
                    .addOnSuccessListener {
                        firestore.collection("users").document(trimmedEmail).get()
                            .addOnSuccessListener { doc ->
                                val fullName = doc.getString("fullName") ?: ""
                                val phone = doc.getString("phone") ?: ""
                                val location = doc.getString("location") ?: ""
                                CoroutineScope(Dispatchers.IO).launch {
                                    userDao.insertUser(
                                        UserEntity(
                                            email = trimmedEmail,
                                            password = trimmedPassword,
                                            fullName = fullName.ifBlank { localUser?.fullName ?: "" },
                                            phone = phone.ifBlank { localUser?.phone ?: "" },
                                            profilePictureUrl = localUser?.profilePictureUrl ?: "",
                                            location = location.ifBlank { localUser?.location ?: "" }
                                        )
                                    )
                                    withContext(Dispatchers.Main) { onSuccess(trimmedEmail) }
                                }
                            }
                            .addOnFailureListener {
                                onSuccess(trimmedEmail)
                            }
                    }
                    .addOnFailureListener { e ->
                        onError(e.localizedMessage ?: "Invalid credentials or network timeout. Please try again.")
                    }
            }
        }
    }
}
