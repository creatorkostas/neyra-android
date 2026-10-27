package dev.neyra.companion

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.neyra.companion.ui.CompanionApp

class MainActivity : ComponentActivity() {
    private lateinit var model: CompanionViewModel
    private var receiveSpeech: ((String) -> Unit)? = null
    private val speech = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (result.resultCode == RESULT_OK && text != null) receiveSpeech?.invoke(text)
        receiveSpeech = null
    }
    private val notifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        model.message(if (allowed) "Reminder notifications enabled" else "Notifications are off. You can enable them in Android settings.")
        model.refresh()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); enableEdgeToEdge()
        val app = application as NeyraApplication
        model = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(cls: Class<T>): T = CompanionViewModel(app.discovery, app.peers, app.api, app.reminders) as T
        })[CompanionViewModel::class.java]
        setContent { CompanionApp(model, onSpeak = { receive ->
            receiveSpeech = receive
            try { speech.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true).putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Neyra")) }
            catch (_: android.content.ActivityNotFoundException) { model.message("No speech recognizer installed. Type your request instead.") }
        }, onNotifications = {
            val prefs = getSharedPreferences("permission-history", MODE_PRIVATE)
            val granted = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            val canRequest = Build.VERSION.SDK_INT >= 33 && (!prefs.getBoolean("notifications-requested", false) || shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS))
            if (!granted && canRequest) {
                prefs.edit().putBoolean("notifications-requested", true).apply()
                notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName))
            }
        }) }
    }
    override fun onStart() { super.onStart(); if (::model.isInitialized) model.start() }
    override fun onStop() { if (::model.isInitialized) model.stop(); super.onStop() }
}
