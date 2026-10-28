package dev.neyra.companion

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import androidx.lifecycle.ViewModelProvider
import dev.neyra.companion.ui.CompanionApp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class MainActivity : ComponentActivity() {
    private lateinit var model: CompanionViewModel
    private var widgetRecord = false
    private lateinit var voice: PhoneSpeech
    private val scanner = registerForActivityResult(ScanContract()) { result -> result.contents?.let { model.pairQr(it) } }
    private val microphone = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) model.toggleRecording() else model.message("Microphone access is off. Type a note, or enable it in Android app permissions.")
    }
    private val notifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        model.message(if (allowed) "Reminder notifications enabled" else "Notifications are off. You can enable them in Android settings.")
        model.refresh()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        val app = application as NeyraApplication
        voice = PhoneSpeech(this)
        model = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(cls: Class<T>): T = CompanionViewModel(app.discovery, app.peers, app.api, app.reminders, app, app.database, app.recorder) as T
        })[CompanionViewModel::class.java]
        lifecycleScope.launch { voice.error.filterNotNull().collect { model.message(it) } }
        if (savedInstanceState == null) handleIntent(intent)
        setContent { CompanionApp(model, onSpeak = { _ -> requestRecording() }, onNotifications = ::notificationSettings,
            onScan = { scanner.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE).setPrompt("Scan the QR in Neyra’s Android companion window").setBeepEnabled(false).setOrientationLocked(false)) },
            onRead = voice::speak, onStopSpeech = voice::stop) }
    }
    private fun requestRecording() {
        voice.stop()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) model.toggleRecording()
        else {
            val prefs = getSharedPreferences("permission-history", MODE_PRIVATE)
            if (prefs.getBoolean("microphone-requested", false) && !shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:$packageName")))
            } else {
                prefs.edit().putBoolean("microphone-requested", true).apply()
                microphone.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
    private fun notificationSettings() {
        val prefs = getSharedPreferences("permission-history", MODE_PRIVATE)
        val granted = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val canRequest = Build.VERSION.SDK_INT >= 33 && (!prefs.getBoolean("notifications-requested", false) || shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS))
        if (!granted && canRequest) {
            prefs.edit().putBoolean("notifications-requested", true).apply(); notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName))
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); handleIntent(intent) }
    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
            val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.take(8000)?.toString().orEmpty()
            if (text.isNotBlank()) model.importText(text) else model.message("No shared text. Copy text or a link and try again.")
        } else if (intent?.action?.startsWith("dev.neyra.") == true) {
            model.importText("")
            if (intent.action == "dev.neyra.record") { widgetRecord = true; startWidgetRecording() }
        }
    }
    private fun startWidgetRecording() {
        if (!widgetRecord) return
        widgetRecord = false
        lifecycleScope.launch {
            model.state.first { it.initialized }
            lifecycle.withResumed {
                if (model.state.value.selected == null) model.message("Pair a computer before using voice capture.")
                else requestRecording()
            }
        }
    }
    override fun onStart() { super.onStart(); if (::model.isInitialized) model.start() }
    override fun onStop() { if (::model.isInitialized) model.stop(); if (::voice.isInitialized) voice.stop(); super.onStop() }
    override fun onDestroy() { if (::voice.isInitialized) voice.close(); super.onDestroy() }
}
