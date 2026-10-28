package dev.neyra.companion.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.UUID

/** Foreground-only PCM16 capture, bounded to 60 seconds. Never uses a speech provider. */
class PhoneRecorder(private val context: Context) {
    val active = MutableStateFlow(false)
    private val finishLock = Mutex()
    val currentFile: File? get() = file
    @Volatile private var input: AudioRecord? = null
    private var writer: Job? = null
    private var file: File? = null
    private var failure: Exception? = null
    @Suppress("MissingPermission")
    fun start() {
        check(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) { "Allow microphone access to record." }
        check(!active.value && input == null) { "Finishing the previous recording. Try again shortly." }
        val size = maxOf(8192, AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT))
        val recorder = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, 16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, size)
        if (recorder.state != AudioRecord.STATE_INITIALIZED) { recorder.release(); error("Microphone unavailable") }
        val directory = File(context.filesDir, "recordings").apply { mkdirs() }
        val target = File(directory, UUID.randomUUID().toString() + ".pcm")
        input = recorder; file = target; failure = null
        try { recorder.startRecording() } catch (e: Exception) { recorder.release(); input = null; throw e }
        active.value = true
        writer = CoroutineScope(Dispatchers.IO).launch {
            try { target.outputStream().use { output ->
                val buffer = ByteArray(8192); var total = 0
                while (active.value && total < 1_920_000) {
                    val count = recorder.read(buffer, 0, minOf(buffer.size, 1_920_000-total))
                    if (count < 0) { if (active.value) error("Microphone disconnected ($count)") else break }
                    if (count > 0) { output.write(buffer, 0, count); total += count }
                }
            } } catch (e: Exception) { failure = e }
            finally { active.value = false }
        }
    }
    suspend fun finish(): File? = finishLock.withLock { withContext(Dispatchers.IO) {
        val recorder = input ?: return@withContext null
        active.value = false
        runCatching { recorder.stop() }
        writer?.join(); recorder.release(); input = null; writer = null
        val result = file; file = null
        failure?.let { result?.delete(); throw it }
        if (result == null || result.length() < 3200) { result?.delete(); error("Recording too short. Try again.") }
        result
    } }
}
