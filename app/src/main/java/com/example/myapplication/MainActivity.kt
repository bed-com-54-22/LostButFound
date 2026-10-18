package com.example.myapplication

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.UserDao
import com.example.myapplication.data.model.AppNotification
import com.example.myapplication.data.model.ItemType
import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.data.repository.ItemRepository
import com.example.myapplication.ui.NotificationsScreen
import com.example.myapplication.ui.SignUpScreen
import com.example.myapplication.ui.SplashScreen
import com.google.firebase.auth.FirebaseAuth

private val Black = Color.Black
private val ScreenBackground = Color(0xFFF2F5F9)
private val FieldBorder = Color(0xFFC5CEDA)
private val HintGray = Color(0xFF8A94A0)

class MainActivity : ComponentActivity() {

    private lateinit var authRepository: AuthRepository
    private lateinit var itemRepository: ItemRepository
    private lateinit var userDao: UserDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getDatabase(this)
        userDao = db.userDao()
        authRepository = AuthRepository(userDao)
        itemRepository = ItemRepository(db.reportedItemDao())

        setContent {
            val navController = rememberNavController()
            var loggedInUserEmail by remember { mutableStateOf("") }
            var userFullName by remember { mutableStateOf("") }
            var lastSeenNotificationsCount by remember { mutableIntStateOf(0) }
            var targetItemIdToOpen by remember { mutableStateOf<String?>(null) }
            val context = LocalContext.current

            LaunchedEffect(loggedInUserEmail) {
                if (loggedInUserEmail.isNotBlank()) {
                    val user = userDao.getUserByEmail(loggedInUserEmail)
                    if (user != null && user.fullName.isNotBlank()) {
                        userFullName = user.fullName
                    }
                }
            }

            val realTimeItems by itemRepository.getItemsFlow().collectAsState(initial = emptyList())

            var previousItemCount by remember { mutableIntStateOf(-1) }
            LaunchedEffect(realTimeItems) {
                if (previousItemCount >= 0 && realTimeItems.size > previousItemCount) {
                    val newItem = realTimeItems.firstOrNull()
                    if (newItem != null) {
                        val typeStr = if (newItem.type == ItemType.LOST) "LOST" else "FOUND"
                        sendSystemNotification(
                            context = context,
                            title = "New $typeStr Item Reported!",
                            message = "${newItem.name} at ${newItem.location}"
                        )
                    }
                }
                previousItemCount = realTimeItems.size
            }

            val notifications = remember(realTimeItems) {
                realTimeItems.map { item ->
                    val typeText = if (item.type == ItemType.LOST) "LOST" else "FOUND"
                    AppNotification(
                        id = item.id,
                        title = "New $typeText Item: ${item.name}",
                        message = "Reported at ${item.location} (${item.date}). Contact: ${item.contactName} ${item.contactPhone}",
                        timestamp = item.timestamp,
                        type = item.type
                    )
                }
            }

            val unseenCount = remember(notifications.size, lastSeenNotificationsCount) {
                maxOf(0, notifications.size - lastSeenNotificationsCount)
            }

            NavHost(navController = navController, startDestination = "splash") {
                composable("splash") {
                    SplashScreen(
                        onTimeout = {
                            navController.navigate("login") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                    )
                }
                composable("login") {
                    LoginScreen(
                        authRepository = authRepository,
                        onLoginSuccess = { email ->
                            loggedInUserEmail = email
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        },
                        onNavigateToSignUp = {
                            navController.navigate("signup")
                        }
                    )
                }
                composable("signup") {
                    SignUpScreen(
                        authRepository = authRepository,
                        onSignUpSuccess = { email, fullName ->
                            loggedInUserEmail = email
                            userFullName = fullName
                            navController.navigate("home") {
                                popUpTo("signup") { inclusive = true }
                            }
                        },
                        onNavigateToLogin = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("home") {
                    val displayName = userFullName.ifBlank {
                        if (loggedInUserEmail.isNotBlank()) loggedInUserEmail.substringBefore("@").replaceFirstChar { it.uppercase() } else "User"
                    }

                    HomeScreen(
                        userName = displayName,
                        userEmail = loggedInUserEmail,
                        unseenNotificationsCount = unseenCount,
                        targetItemIdToOpen = targetItemIdToOpen,
                        onClearTargetItemId = { targetItemIdToOpen = null },
                        itemRepository = itemRepository,
                        onNavigateToProfile = {
                            navController.navigate("profile")
                        },
                        onNavigateToAbout = {
                            navController.navigate("about")
                        },
                        onNavigateToNotifications = {
                            lastSeenNotificationsCount = notifications.size
                            navController.navigate("notifications")
                        },
                        onLogout = {
                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    )
                }
                composable("profile") {
                    ProfileScreen(
                        userEmail = loggedInUserEmail,
                        userDao = userDao,
                        itemRepository = itemRepository,
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onLogout = {
                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    )
                }
                composable("about") {
                    AboutScreen(
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("notifications") {
                    LaunchedEffect(Unit) {
                        lastSeenNotificationsCount = notifications.size
                    }
                    NotificationsScreen(
                        notifications = notifications,
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onNotificationClick = { itemId ->
                            targetItemIdToOpen = itemId
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }

    private fun sendSystemNotification(context: Context, title: String, message: String) {
        try {
            val channelId = "lost_found_realtime_channel"
            val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Real-time Lost But Found Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_app_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
            }
        } catch (_: Exception) {}
    }
}

@Composable
private fun AppText(
    text: String,
    fontSize: TextUnit,
    color: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun LoginScreen(
    authRepository: AuthRepository,
    onLoginSuccess: (String) -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val emailIsValid = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val context = LocalContext.current

    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun attemptLogin() {
        when {
            email.isBlank() || password.isBlank() -> showToast("Fill in both fields")
            !emailIsValid -> showToast("Enter a valid email address")
            else -> {
                isLoading = true
                authRepository.login(
                    email = email,
                    password = password,
                    onSuccess = { userEmail ->
                        isLoading = false
                        showToast("Login successful")
                        onLoginSuccess(userEmail)
                    },
                    onError = { error ->
                        isLoading = false
                        showToast(error)
                    }
                )
            }
        }
    }

    fun handleForgotPassword() {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            showToast("Please enter your registered email address above first")
        } else {
            FirebaseAuth.getInstance().sendPasswordResetEmail(trimmedEmail)
                .addOnSuccessListener {
                    showToast("Password reset email sent to $trimmedEmail")
                }
                .addOnFailureListener { e ->
                    showToast(e.localizedMessage ?: "Failed to send password reset email")
                }
        }
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
        Spacer(modifier = Modifier.height(40.dp))

        Image(
            painter = painterResource(R.drawable.ic_app_logo),
            contentDescription = "App Logo",
            modifier = Modifier.size(130.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        AppText("Lost But Found", 30.sp, Color.Black)
        Spacer(modifier = Modifier.height(4.dp))
        AppText("Log in to continue", 16.sp, Color(0xFF555555))

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {

                AppText("Email", 16.sp, Color.Black)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { AppText("Enter your email", 14.sp, HintGray) },
                    isError = email.isNotEmpty() && !emailIsValid,
                    supportingText = {
                        if (email.isNotEmpty() && !emailIsValid) {
                            AppText("Invalid email address", 12.sp, Color(0xFFB3261E))
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Black
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors
                )

                Spacer(modifier = Modifier.height(16.dp))

                AppText("Password", 16.sp, Color.Black)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { AppText("Enter your password", 14.sp, HintGray) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Black
                        )
                    },
                    trailingIcon = {
                        TextButton(onClick = { passwordVisible = !passwordVisible }) {
                            AppText(
                                if (passwordVisible) "Hide" else "Show",
                                12.sp,
                                Black
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { attemptLogin() }),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors
                )

                Spacer(modifier = Modifier.height(12.dp))

                AppText(
                    text = "Forgot password?",
                    fontSize = 14.sp,
                    color = Black,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable { handleForgotPassword() }
                        .padding(4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { attemptLogin() },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Black)
                ) {
                    AppText(
                        if (isLoading) "Logging in..." else "Login",
                        18.sp,
                        Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            AppText("Don't have an account?", 14.sp, Color(0xFF555555))
            AppText(
                text = " Sign up",
                fontSize = 14.sp,
                color = Black,
                modifier = Modifier.clickable { onNavigateToSignUp() }
            )
        }
    }
}
