package com.minseok.a20203041

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.jsoup.Jsoup
import java.util.*
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class WebChangeWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val sharedPref = applicationContext.getSharedPreferences("WebChangePref", Context.MODE_PRIVATE)
        val url = sharedPref.getString("url", "")
        val email = sharedPref.getString("email", "")
        Log.d("email", "$email")
        val lastContent = inputData.getString("lastContent") ?: ""

        Log.d("WebChangeWorker", "Starting work for URL: $url with email: $email")

        try {
            val document = Jsoup.connect(url).get()
            val currentContent = document.body().text()

            if (currentContent != lastContent) {
                Log.d("WebChangeWorker", "Content changed, sending email...")
                sendEmail(email, "웹 사이트 변경이 감지되었습니다", "웹 사이트 변경 : $url")
                sendNotification("웹 사이트 변경이 감지되었습니다", "웹 사이트 변경 : $url")
                showToast("웹 사이트 변경이 감지되었습니다: $url")

                with(sharedPref.edit()) {
                    putString(url, currentContent)
                    apply()
                }
            }

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("WebChangeWorker", "Error during work", e)
            return Result.failure()
        }
    }

    private fun sendEmail(toEmail: String?, subject: String, body: String) {
        val fromEmail = "qqq7384@naver.com"  // 발신자의 이메일 주소로 변경
        val fromPassword = "RKZ45R3CJ1YV"  // 발신자의 이메일 비밀번호로 변경

        val props = Properties()

        if (fromEmail.endsWith("@gmail.com")) {
            props["mail.smtp.auth"] = "true"
            props["mail.smtp.starttls.enable"] = "true"
            props["mail.smtp.host"] = "smtp.gmail.com"
            props["mail.smtp.port"] = "587"
        } else if (fromEmail.endsWith("@naver.com")) {
            props["mail.smtp.auth"] = "true"
            props["mail.smtp.starttls.enable"] = "true"
            props["mail.smtp.host"] = "smtp.naver.com"
            props["mail.smtp.port"] = "587"
        } else {
            Log.e("WebChangeWorker", "Unsupported email provider")
            return
        }
        val session = Session.getInstance(props, object : javax.mail.Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(fromEmail, fromPassword)
            }
        })

        try {
            val message = MimeMessage(session)
            message.setFrom(InternetAddress(fromEmail))
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
            message.subject = subject
            message.setText(body)
            Transport.send(message)
            Log.d("WebChangeWorker", "Email sent successfully to $toEmail")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("WebChangeWorker", "Error sending email: ${e.message}")
        }
    }

    private fun sendNotification(title: String, message: String) {
        val channelId = "web_change_channel"
        val channelName = "Web Change Notifications"
        val notificationId = 1

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(NotificationManagerCompat.from(applicationContext)) {
            notify(notificationId, notificationBuilder.build())
        }
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }
}

