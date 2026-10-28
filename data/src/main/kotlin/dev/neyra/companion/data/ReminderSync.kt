package dev.neyra.companion.data

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import dev.neyra.companion.core.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ReminderCache private constructor(private val context: Context) {
    companion object {
        @Volatile private var instance: ReminderCache? = null
        fun get(context: Context): ReminderCache = instance ?: synchronized(this) {
            instance ?: ReminderCache(context.applicationContext).also { instance = it }
        }
    }
    private val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)
    private val alarms = context.getSystemService(AlarmManager::class.java)
    @Synchronized fun records(): List<Reminder> {
        val array = JSONArray(prefs.getString("records", "[]"))
        return (0 until array.length()).map { i -> array.getJSONObject(i).let {
            Reminder(it.getString("machine"), it.getString("id"), it.getString("text"), it.getLong("due"), it.getString("status"), it.optInt("repeat"))
        } }
    }
    private fun pending(record: Reminder): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(record.deliveryKey)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    @Synchronized fun update(snapshot: Snapshot) {
        val old = records()
        val current = old.filterNot { it.machineId == snapshot.machineId } + snapshot.reminders
        old.filter { it.machineId == snapshot.machineId }.forEach {
            alarms.cancel(pending(it))
            if (current.none { next -> next.deliveryKey == it.deliveryKey && next.status != "done" })
                NotificationManagerCompat.from(context).cancel(it.deliveryKey, 1)
        }
        val array = JSONArray()
        current.forEach { array.put(JSONObject().put("machine", it.machineId).put("id", it.id).put("text", it.text).put("due", it.dueAt).put("status", it.status).put("repeat", it.repeatSeconds)) }
        check(prefs.edit().putString("records", array.toString()).putLong("sync:${snapshot.machineId}", System.currentTimeMillis()).commit())
        schedule(current)
        notifyDue()
    }
    fun lastSync(id: String): Long = prefs.getLong("sync:$id", 0)
    @Synchronized fun forget(id: String) { update(Snapshot(id, "", emptyList())); prefs.edit().remove("sync:$id").apply() }
    fun schedule(records: List<Reminder> = records()) {
        records.filter { it.status == "pending" && it.dueAt > System.currentTimeMillis() }.forEach {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, it.dueAt, pending(it))
        }
    }
    @Synchronized fun notifyDue() {
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(NotificationChannel("reminders", "Neyra reminders", NotificationManager.IMPORTANCE_HIGH))
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        if (!manager.areNotificationsEnabled()) return
        val delivered = prefs.getStringSet("delivered", emptySet())!!.toMutableSet()
        dueReminders(records(), System.currentTimeMillis(), delivered).forEach {
            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val open = launch?.let { intent -> PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE) }
            fun action(name: String): PendingIntent {
                val actionIntent = Intent(context, NotificationActionReceiver::class.java).setAction(it.deliveryKey + ":" + name)
                    .putExtra("machine", it.machineId).putExtra("id", it.id).putExtra("due", it.dueAt).putExtra("action", name)
                return PendingIntent.getBroadcast(context, 0, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            }
            val notification = NotificationCompat.Builder(context, "reminders")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("Neyra reminder")
                .setContentText(it.text).setStyle(NotificationCompat.BigTextStyle().bigText(it.text))
                .addAction(0, "Done", action("done")).addAction(0, "Snooze 10 min", action("snooze"))
                .setContentIntent(open).setAutoCancel(true).setCategory(NotificationCompat.CATEGORY_REMINDER).build()
            manager.notify(it.deliveryKey, 1, notification)
            delivered.add(it.deliveryKey)
        }
        val valid = records().map { it.deliveryKey }.toSet()
        prefs.edit().putStringSet("delivered", delivered.intersect(valid)).commit()
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        Thread { try { ReminderCache.get(context).notifyDue() } finally { result.finish() } }.start()
    }
}
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val result = goAsync()
        Thread { try { ReminderCache.get(context).apply { schedule(); notifyDue() }; SyncWorker.install(context) } finally { result.finish() } }.start()
    }
}
class SyncWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val api = TlsCompanionApi(); val cache = ReminderCache.get(applicationContext)
        val store = EncryptedPeerStore.get(applicationContext)
        val peers = runCatching { store.peers() }.getOrElse { return Result.failure() }
        val discovery = NsdDiscovery(applicationContext)
        val nearby = try {
            discovery.start(); kotlinx.coroutines.delay(2500); discovery.candidates.value
        } finally { discovery.stop() }
        val reachable = peers.map { peer -> nearby.find { it.id == peer.id }?.let { peer.copy(host = it.host, port = it.port) } ?: peer }
        var failed = false
        reachable.forEach { peer -> runCatching {
            val snapshot = api.snapshot(peer)
            synchronized(store) { if (store.peers().any { it.id == peer.id }) { cache.update(snapshot); CompanionDatabase.get(applicationContext).cache(snapshot); store.save(peer) } }
        }.onFailure { failed = true } }
        OutboxSync.flush(applicationContext, api, reachable)
        cache.notifyDue()
        return if (failed) Result.retry() else Result.success()
    }
    companion object {
        fun install(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("neyra-reminder-sync", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
