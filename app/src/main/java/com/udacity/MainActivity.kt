package com.udacity

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.udacity.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var downloadID: Long = 0

    private lateinit var notificationManager: NotificationManager
    private lateinit var pendingIntent: PendingIntent
    private lateinit var action: NotificationCompat.Action
    private val notificationId = 0

    private var gitUrl: String? = null
    private var gitFilename: String? = null
    lateinit var loadingButton: LoadingButton
    private lateinit var radioGroup: RadioGroup

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        loadingButton = binding.contentMain.customButton
        radioGroup = binding.contentMain.radioGroup
        loadingButton.setOnClickListener {
            if (isInternetAvailable()) {
                if (radioGroup.checkedRadioButtonId == -1) {
                    loadingButton.buttonState = ButtonState.Clicked
                    Toast.makeText(this, "Please select the file to download", Toast.LENGTH_SHORT).show()
                } else {
                    loadingButton.buttonState = ButtonState.Loading
                    download()
                }
            } else{
                Toast.makeText(this, "No Internet Connection", Toast.LENGTH_SHORT).show()
            }
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.glide_radio_button -> {
                    gitUrl = URL_GLIDE
                    gitFilename = getString(R.string.glide_desc)
                }
                R.id.udacity_radio_button -> {
                    gitUrl = URL_LOADAPP
                    gitFilename = getString(R.string.loadapp_desc)
                }
                R.id.retrofit_radio_button -> {
                    gitUrl = URL_RETROFIT
                    gitFilename = getString(R.string.retrofit_desc)
                }
            }
        }

        // Initialize notification manager, pending intent, and action
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the notification channel
        createChannel(CHANNEL_ID, CHANNEL_NAME)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            loadingButton.buttonState = ButtonState.Completed
            if (!gitFilename.isNullOrEmpty()) {
                // Check if the download was successful
                val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val query = DownloadManager.Query().setFilterById(id!!)
                val cursor = downloadManager.query(query)
                if (cursor.moveToFirst()) {
                    val statusColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    if (statusColumnIndex != -1) {
                        val status = cursor.getInt(statusColumnIndex)
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            showDownloadNotification(gitFilename!!, true)
                            Log.i(
                                "status_download",
                                "$gitFilename Download status: true"
                            )
                        } else {
                            showDownloadNotification(gitFilename!!, false)
                            Log.i(
                                "status_download",
                                "$gitFilename Download status: false"
                            )
                        }
                    } else {
                        Log.i("download_fail", "deu ruim!")
                        // Handle case when the status column is not found
                    }
                }
            }
        }
    }

    private fun download() {
        val request =
            DownloadManager.Request(Uri.parse(gitUrl))
                .setTitle(getString(R.string.app_name))
                .setDescription(getString(R.string.app_description))
                .setRequiresCharging(false)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        downloadID =
            downloadManager.enqueue(request)// enqueue puts the download request in the queue.
    }

    companion object {
        private const val URL_LOADAPP =
            "https://github.com/udacity/nd940-c3-advanced-android-programming-project-starter/archive/master.zip"
        private const val URL_GLIDE = "https://github.com/bumptech/glide"
        private const val URL_RETROFIT = "https://github.com/square/retrofit"
        private const val CHANNEL_ID = "channelId"
        private const val CHANNEL_NAME = "loadApp"
    }

    private fun createChannel(channelId: String, channelName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setShowBadge(false)
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                description = getString(R.string.notification_description)
            }

            notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun showDownloadNotification(fileName: String, isSuccess: Boolean) {
        val contentIntent = Intent(this@MainActivity, DetailActivity::class.java).apply {
            putExtra("file_name", fileName)
            putExtra("file_status", isSuccess)
            action = "CHECK_STATUS"
            putExtra("notification_id", notificationId)
        }

        pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        action = NotificationCompat.Action.Builder(
            0,
            "Check the status",
            pendingIntent
        ).build()

        val bigTextStyle = NotificationCompat.BigTextStyle()

        val builder = NotificationCompat.Builder(
            applicationContext,
            CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_assistant_black_24dp)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_description))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(bigTextStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(action)

        notificationManager.notify(notificationId, builder.build())
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}