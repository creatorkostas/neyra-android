package dev.neyra.companion.data

import dev.neyra.companion.core.*
import org.json.JSONObject

fun parsePairingQr(value: String): Peer {
    require(value.length <= 4096) { "Not a Neyra pairing QR." }
    val json = JSONObject(value)
    require(json.getString("type") == "neyra-pair-v1") { "Scan a QR from desktop Android companion settings." }
    val id = json.getString("id"); val pin = json.getString("fingerprint"); val invite = json.getString("invite")
    val host = json.getString("host"); val port = json.getInt("port")
    require(id.matches(Regex("[a-zA-Z0-9_-]{1,64}")) && validFingerprint(pin) && invite.matches(Regex("[a-f0-9]{64}"))) { "Invalid pairing identity." }
    require(host.matches(Regex("[a-zA-Z0-9.:-]{1,253}")) && port in 1..65535) { "Invalid computer address." }
    return Peer(id, json.getString("name").take(200), host, port, pin.lowercase(), invite)
}
