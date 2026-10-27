package dev.neyra.companion
import android.app.Application
import dev.neyra.companion.data.*
class NeyraApplication : Application() {
    val discovery by lazy { NsdDiscovery(this) }
    val peers by lazy { EncryptedPeerStore.get(this) }
    val api by lazy { TlsCompanionApi() }
    val reminders by lazy { ReminderCache.get(this) }
    override fun onCreate() { super.onCreate(); SyncWorker.install(this) }
}
