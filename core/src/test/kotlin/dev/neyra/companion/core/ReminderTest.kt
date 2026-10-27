package dev.neyra.companion.core
import kotlin.test.*
class ReminderTest {
    @Test fun deliveryIsScopedToMachineAndDeadline() {
        val first = Reminder("pc1", "same", "Call", 100, "fired")
        val second = first.copy(machineId = "pc2")
        val snoozed = first.copy(dueAt = 200, status = "pending")
        assertEquals(listOf(second, snoozed), dueReminders(listOf(first, second, snoozed, first.copy(status="done")), 200, setOf(first.deliveryKey)))
    }
    @Test fun rejectsMalformedCertificatePins() { assertFalse(validFingerprint("anything")); assertTrue(validFingerprint("a".repeat(64))) }
}
