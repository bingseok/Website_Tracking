package com.minseok.a20203041
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var userDatabaseHelper: UserDatabaseHelper
    private lateinit var urlInput: EditText
    private lateinit var intervalInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 알림 권한 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        }

        userDatabaseHelper = UserDatabaseHelper(this)
        val currentUser = intent.getStringExtra("email")

        if (currentUser.isNullOrEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // UI 요소 초기화
        urlInput = findViewById(R.id.urlInput)
        intervalInput = findViewById(R.id.intervalInput)
        emailInput = findViewById(R.id.emailInput)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)

        // 시작 버튼 클릭 리스너
        startButton.setOnClickListener {
            val url = urlInput.text.toString()
            val interval = intervalInput.text.toString().toLongOrNull()
            val email = emailInput.text.toString()

            if (!isValidUrl(url)) {
                Toast.makeText(this, "유효한 URL을 입력해주세요", Toast.LENGTH_SHORT).show()
            } else if (url.isEmpty() || interval == null || email.isEmpty()) {
                Toast.makeText(this, "모든 칸에 입력을 해주세요", Toast.LENGTH_SHORT).show()
            } else {
                val sharedPref = getSharedPreferences("WebChangePref", MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putString("url", url)
                    putString("email", email)
                    putString("lastContent", "")
                    apply()
                }

                val workRequest = OneTimeWorkRequest.Builder(WebChangeWorker::class.java)
                    .setInitialDelay(interval, TimeUnit.SECONDS)  // 초 단위로 설정
                    .build()

                WorkManager.getInstance(this).enqueue(workRequest)
                Toast.makeText(this, "모니터링을 시작합니다 $url", Toast.LENGTH_SHORT).show()
            }
        }

        // 정지 버튼 클릭 리스너
        stopButton.setOnClickListener {
            WorkManager.getInstance(this).cancelAllWork()
            Toast.makeText(this, "모니터링을 중지합니다", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isValidUrl(url: String): Boolean {
        return Patterns.WEB_URL.matcher(url).matches()
    }

    private fun requestNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "알림 권한이 불충분합니다", Toast.LENGTH_SHORT).show()
        }
    }
}


