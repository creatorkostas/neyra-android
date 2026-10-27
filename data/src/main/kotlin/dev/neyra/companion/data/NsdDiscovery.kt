package dev.neyra.companion.data

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import dev.neyra.companion.core.*
import kotlinx.coroutines.flow.MutableStateFlow

@Suppress("DEPRECATION")
class NsdDiscovery(context: Context) : Discovery {
    private val manager = context.getSystemService(NsdManager::class.java)
    override val candidates = MutableStateFlow<List<Candidate>>(emptyList())
    override val error = MutableStateFlow<String?>(null)
    private var listener: NsdManager.DiscoveryListener? = null
    private val pending = ArrayDeque<NsdServiceInfo>()
    private var resolving = false
    private var generation = 0
    private val names = mutableMapOf<String, String>()
    @Synchronized override fun start() {
        if (listener != null) return
        error.value = null
        val run = ++generation
        val next = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(type: String) {}
            override fun onDiscoveryStopped(type: String) {}
            override fun onStartDiscoveryFailed(type: String, code: Int) { error.value = "Discovery unavailable ($code). Try scanning again."; stop() }
            override fun onStopDiscoveryFailed(type: String, code: Int) { error.value = "Could not stop discovery ($code)." }
            override fun onServiceFound(info: NsdServiceInfo) { synchronized(this@NsdDiscovery) { if (run == generation) { pending.add(info); resolveNext(run) } } }
            override fun onServiceLost(info: NsdServiceInfo) { synchronized(this@NsdDiscovery) {
                pending.removeAll { it.serviceName == info.serviceName }
                names.remove(info.serviceName)?.let { id -> candidates.value = candidates.value.filterNot { it.id == id } }
            } }
        }
        listener = next
        try { manager.discoverServices("_neyra._tcp.", NsdManager.PROTOCOL_DNS_SD, next) }
        catch (e: RuntimeException) { listener = null; error.value = "Allow local network access and try again." }
    }
    private fun resolveNext(run: Int) {
        if (resolving || pending.isEmpty() || run != generation) return
        resolving = true
        val info = pending.removeFirst()
        manager.resolveService(info, object : NsdManager.ResolveListener {
            override fun onResolveFailed(info: NsdServiceInfo, code: Int) { finish() }
            override fun onServiceResolved(info: NsdServiceInfo) {
                synchronized(this@NsdDiscovery) {
                    if (run == generation) {
                        val id = info.attributes["id"]?.toString(Charsets.UTF_8)
                        val name = info.attributes["name"]?.toString(Charsets.UTF_8) ?: info.serviceName
                        val host = info.host?.hostAddress
                        if (id != null && host != null && info.attributes["version"]?.toString(Charsets.UTF_8) == "1") {
                            names[info.serviceName] = id
                            candidates.value = (candidates.value.filterNot { it.id == id } + Candidate(id, name, host, info.port)).sortedBy { it.name }
                        }
                    }
                }
                finish()
            }
            private fun finish() { synchronized(this@NsdDiscovery) { resolving = false; resolveNext(generation) } }
        })
    }
    @Synchronized override fun stop() {
        generation++
        listener?.let { runCatching { manager.stopServiceDiscovery(it) } }
        listener = null; pending.clear(); candidates.value = emptyList(); names.clear()
    }
}
