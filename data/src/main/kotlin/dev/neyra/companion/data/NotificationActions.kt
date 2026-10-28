package dev.neyra.companion.data

import android.content.*
import androidx.work.*
import dev.neyra.companion.core.*

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val data = workDataOf("machine" to intent.getStringExtra("machine"), "id" to intent.getStringExtra("id"), "due" to intent.getLongExtra("due", 0), "action" to intent.getStringExtra("action"))
        WorkManager.getInstance(context).enqueueUniqueWork("reminder-action:" + intent.action, ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<ReminderActionWorker>().setInputData(data)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build())
    }
}
class ReminderActionWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val store = EncryptedPeerStore.get(applicationContext)
        val peer = store.peers().find { it.id == inputData.getString("machine") } ?: return Result.failure()
        val cache = ReminderCache.get(applicationContext)
        val record = cache.records().find { it.id == inputData.getString("id") && it.machineId == peer.id && it.dueAt == inputData.getLong("due", 0) } ?: return Result.success()
        val action = inputData.getString("action") ?: return Result.failure()
        if (action !in setOf("done", "snooze")) return Result.failure()
        return try {
            val snapshot = TlsCompanionApi().actionRequest(peer, record, action, id.toString())
            synchronized(store) { if (store.peers().any { it.id == peer.id }) { cache.update(snapshot); CompanionDatabase.get(applicationContext).cache(snapshot) } }
            Result.success()
        } catch (e: CompanionHttpException) {
            if (e.status == 400) runCatching { cache.update(TlsCompanionApi().snapshot(peer)) }
            if (e.status in 400..499) Result.failure() else Result.retry()
        }
        catch (_: Exception) { Result.retry() }
    }
}
