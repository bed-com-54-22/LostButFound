package com.example.myapplication.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.myapplication.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

data class VerificationCodeData(
    val code: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val expiryDurationMillis: Long = 10 * 60 * 1000L // 10 minutes
) {
    fun isExpired(): Boolean = System.currentTimeMillis() > (createdAtMillis + expiryDurationMillis)
}

object EmailVerificationService {

    private val verificationCodes = ConcurrentHashMap<String, VerificationCodeData>()

    // Optional SMTP Configuration for sending real emails via SMTP
    var smtpHost: String = "smtp.gmail.com"
    var smtpPort: String = "465"
    var senderEmail: String = "lostbutfound.verify@gmail.com"
    var senderPassword: String = "" // Set real SMTP password / App Password here if using custom SMTP server

    fun generateCode(email: String): String {
        val trimmedEmail = email.trim().lowercase()
        val code = String.format(Locale.US, "%06d", (0..999999).random())
        verificationCodes[trimmedEmail] = VerificationCodeData(code = code)
        return code
    }

    suspend fun sendVerificationCode(
        context: Context,
        recipientEmail: String,
        code: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val trimmedEmail = recipientEmail.trim().lowercase()

        // 1. Show local system notification & Toast on device so code is instantly viewable
        showLocalNotification(context, recipientEmail, code)
        withContext(Dispatchers.Main) {
            Toast.makeText(
                context,
                "Verification Code for $trimmedEmail: $code",
                Toast.LENGTH_LONG
            ).show()
        }

        // 2. Send real email via JavaMail SMTP if senderPassword/credentials are configured
        if (senderPassword.isNotBlank()) {
            try {
                val props = Properties().apply {
                    put("mail.smtp.host", smtpHost)
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.port", smtpPort)
                    put("mail.smtp.connectiontimeout", "8000")
                    put("mail.smtp.timeout", "8000")

                    if (smtpPort == "465") {
                        put("mail.smtp.socketFactory.port", smtpPort)
                        put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                        put("mail.smtp.ssl.enable", "true")
                    } else {
                        put("mail.smtp.starttls.enable", "true")
                    }
                }

                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(senderEmail, senderPassword)
                    }
                })

                val mimeMessage = MimeMessage(session).apply {
                    setFrom(InternetAddress(senderEmail, "Lost But Found App"))
                    addRecipient(Message.RecipientType.TO, InternetAddress(trimmedEmail))
                    subject = "Your Verification Code - Lost But Found"
                    setText(
                        """
                        Hello,

                        Thank you for registering on Lost But Found!

                        Your email verification code is: $code

                        This code will expire in 10 minutes.

                        Best regards,
                        Lost But Found Team
                        """.trimIndent()
                    )
                }

                Transport.send(mimeMessage)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return@withContext Result.success(Unit)
    }

    fun verifyCode(email: String, inputCode: String): Pair<Boolean, String> {
        val trimmedEmail = email.trim().lowercase()
        val trimmedCode = inputCode.trim()

        val data = verificationCodes[trimmedEmail]
            ?: return Pair(false, "No verification code found for this email. Please request a new code.")

        if (data.isExpired()) {
            verificationCodes.remove(trimmedEmail)
            return Pair(false, "Verification code has expired. Please request a new code.")
        }

        if (data.code != trimmedCode) {
            return Pair(false, "Incorrect verification code. Please check your email or notification.")
        }

        // Code verified -> remove used code
        verificationCodes.remove(trimmedEmail)
        return Pair(true, "Code verified successfully.")
    }

    private fun showLocalNotification(context: Context, email: String, code: String) {
        try {
            val channelId = "email_verification_channel"
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Email Verification Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Delivers verification codes for account registration"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_app_logo)
                .setContentTitle("Email Verification Code")
                .setContentText("Verification code for $email: $code")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("Your Lost But Found account verification code is:\n\n$code\n\n(Expires in 10 minutes)")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(1001, notification)
        } catch (_: Exception) {}
    }
}
