package dev.neyra.companion

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Locale

class PhoneSpeech(context: Context) {
    val speaking = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    private var failed = false
    private var ready = false
    private var pending: String? = null
    private var engine: TextToSpeech? = null
    init {
        engine = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            failed = !ready
            if (ready) {
                engine?.language = Locale.getDefault()
                pending?.let { speak(it) }; pending = null
            } else if (pending != null) { pending = null; error.value = "Text-to-speech is unavailable. Enable an offline voice in Android settings." }
        }
        engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) { speaking.value = true }
            override fun onDone(id: String?) { speaking.value = false }
            @Deprecated("Platform callback") override fun onError(id: String?) { speaking.value = false }
        })
    }
    fun speak(text: String) {
        error.value = null
        if (!ready) {
            if (failed) error.value = "Text-to-speech is unavailable. Enable an offline voice in Android settings."
            else pending = text
            return
        }
        val offline = engine?.voices?.firstOrNull { !it.isNetworkConnectionRequired && it.locale.language == Locale.getDefault().language && !it.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) }
        if (offline == null) { error.value = "Install an offline voice for your language in Android text-to-speech settings. The answer is available as text."; return }
        engine?.voice = offline
        speaking.value = true
        val chunks = text.chunked(TextToSpeech.getMaxSpeechInputLength())
        chunks.forEachIndexed { index, chunk ->
            if (engine?.speak(chunk, if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, "neyra-$index") == TextToSpeech.ERROR) speaking.value = false
        }
    }
    fun stop() { pending = null; engine?.stop(); speaking.value = false }
    fun close() { stop(); engine?.shutdown() }
}
