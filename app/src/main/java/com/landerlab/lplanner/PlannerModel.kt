package com.landerlab.lplanner

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

/**
 * PlannerModel.kt — Lplanner Android v1.0.0
 *
 * Direct port of PlannerModel in ZPlannerUI/ZPlannerView.swift. The profile-text
 * builder below is kept line-for-line identical to the Swift original: it is the
 * only thing the engine sees, so any divergence here would silently change dive
 * plans between the two platforms.
 */

data class DiveLevel(
    val id: String = UUID.randomUUID().toString(),
    var enabled: Boolean = true,
    var d: String = "",
    var t: String = "",
    var o2: String = "",
    var he: String = "",
    var set: String = "",
    var sld: String = "",
) {
    val summary: String
        get() = buildString {
            append("$d, $t, $o2")
            he.toDoubleOrNull()?.let { if (it > 0) append("/$he") }
            if (set.isNotEmpty()) append(", $set")
            if (sld.isNotEmpty()) append("-$sld")
        }
}

data class LogEntry(
    val id: String = UUID.randomUUID().toString(),
    val date: Date = Date(),
    /** Which dive and settings produced this plan, so entries are identifiable. */
    val summary: String,
    val text: String,
) {
    /** Date as well as time — entries from different days were indistinguishable. */
    val stamp: String
        get() = SimpleDateFormat("d MMM  HH:mm", Locale.getDefault()).format(date)
}

class PlannerModel(app: Application) : AndroidViewModel(app) {

    // Log entries are persisted to the app's private storage; without this the
    // log was empty on every launch.
    private val store = LogStore(app)

    // ---- Config sheet ----
    var depthsMetric by mutableStateOf(true)        // Depths: Feet / Meters
    var rmvMetric by mutableStateOf(true)           // RMVs: Cu.ft / Liters
    var saltWater by mutableStateOf(true)           // Water: Fresh / Salt
    var o2Narcotic by mutableStateOf(false)         // O2 Narcotic: No / Yes
    var model by mutableStateOf("c")                // "c" (ZHL16-C) or "vval"
    var useGF by mutableStateOf(false)
    var gfLow by mutableStateOf("30")
    var gfHigh by mutableStateOf("85")
    var altGfLow by mutableStateOf("90")
    var altGfHigh by mutableStateOf("90")
    var extraSlow by mutableStateOf(false)
    var ndlLow by mutableStateOf(false)
    var altitude by mutableStateOf("0")
    var conservatism by mutableStateOf(10.0)        // 0-100 %
    var deepStops by mutableStateOf("p")            // n / p
    var pyleTime by mutableStateOf(1)               // 1-5 min
    var stopDistance by mutableStateOf("3")
    var lastStop by mutableStateOf("3")
    var descentRates by mutableStateOf("0-100, 15")
    var ascentRates by mutableStateOf("70-30, 18\n30-12, 9\n12-0, 3")
    var decoSetpoints by mutableStateOf("")         // e.g. "80-30, 1.4\n29-0, 1.2"
    var slideRate by mutableStateOf("0.1")
    var maxPO2 by mutableStateOf("1.6")
    var maxEND by mutableStateOf("40")
    var bottomRMV by mutableStateOf("19")
    var decoRMV by mutableStateOf("14")

    // ---- Main window rows ----
    var si48 by mutableStateOf(false)
    var si24 by mutableStateOf(false)
    var siActual by mutableStateOf("")              // H:MM
    var decoGasesOn by mutableStateOf(true)
    var decoGases by mutableStateOf("50")
    var circuitClosed by mutableStateOf(false)      // Open / Closed
    var plus3m by mutableStateOf(false)             // add 3 m / 10 ft to deepest level
    var plus5min by mutableStateOf(false)           // add 5 min to deepest level
    var useAltGF by mutableStateOf(false)           // use Alternative GF pair

    // ---- Levels ----
    val levels = mutableStateListOf<DiveLevel>()
    var entry by mutableStateOf(DiveLevel())
    var editingID by mutableStateOf<String?>(null)

    // ---- Output ----
    var planText by mutableStateOf("")
    var notes by mutableStateOf("")
    val log = mutableStateListOf<LogEntry>().apply { addAll(store.load()) }
    private var lastTissue: String? = null

    /** Gradient factors active — they override Conservatism. */
    val gfOn: Boolean get() = (useGF || useAltGF) && model != "vval"

    val repetitive: Boolean get() = si48 || si24 || siActual.isNotEmpty()

    val surfaceInterval: String
        get() = when {
            siActual.isNotEmpty() -> siActual
            si48 -> "48:00"
            si24 -> "24:00"
            else -> "900:00"
        }

    val profileText: String
        get() {
            val p = StringBuilder()
            // Built line by line rather than with a trimIndent() raw string: trimIndent
            // measures indentation AFTER interpolation, so a newline pasted into any
            // config field would silently mangle every following key.
            listOf(
                "UseMetric: ${yn(depthsMetric)}",
                "RmvMetric: ${yn(rmvMetric)}",
                "SaltWater: ${yn(saltWater)}",
                "Model: ${if (model == "vval") "vval18" else "zhl16c"}",
                "Altitude: ${one(altitude)}",
                "Conservatism: ${conservatism.toInt()}",
                "Precision: 1",
                "StopDistance: ${one(stopDistance)}",
                "LastStopDepth: ${one(lastStop)}",
                "OxyNarc: ${yn(o2Narcotic)}",
                "DeepStops: $deepStops",
                "PyleStopTime: $pyleTime",
                "TissueFile:",
                "SurfaceInterval: ${one(surfaceInterval)}",
                "Rmv: ${one(bottomRMV)}",
                "DecoRmv: ${one(decoRMV)}",
                "SlideRate: ${one(slideRate)}",
                "UseOCDeco: ${yn(decoGasesOn)}",
                "OcDecoGas: ${one(decoGases)}",
                "OcDecoMaxPO2: ${one(maxPO2)}",
                "MaxEND: ${one(maxEND)}",
            ).joinTo(p, "\n")
            if ((useGF || useAltGF) && model != "vval") {
                val lo = if (useAltGF) altGfLow else gfLow
                val hi = if (useAltGF) altGfHigh else gfHigh
                p.append("\nGradientFactors: $lo, $hi")
            }
            p.append("\nExtraSlow: ${if (extraSlow) "y" else "n"}")
            p.append("\nNdlGF: ${if (ndlLow) "low" else "high"}")
            for (r in descentRates.lines().filter { it.isNotBlank() }) p.append("\nDescentRate: $r")
            for (r in ascentRates.lines().filter { it.isNotBlank() }) p.append("\nAscentRate: $r")
            if (decoSetpoints.isBlank()) {
                p.append("\nUseDecoSetpoint: n")
            } else {
                p.append("\nUseDecoSetpoint: y")
                for (r in decoSetpoints.lines().filter { it.isNotBlank() }) p.append("\nDecoSetpoint: $r")
            }
            p.append("\n")

            // +3m / +5min apply to the deepest enabled level
            val enabled = levels.filter { it.enabled }
            val deepest = enabled.mapNotNull { it.d.toDoubleOrNull() }.maxOrNull() ?: 0.0
            for (l in enabled) {
                var d = l.d.toDoubleOrNull() ?: 0.0
                var t = l.t.toDoubleOrNull() ?: 0.0
                if (d >= deepest - 0.001 && deepest > 0) {
                    if (plus3m) d += if (depthsMetric) 3.0 else 10.0
                    if (plus5min) t += 5.0
                }
                val line = StringBuilder("${fmt(d)}, ${fmt(t)}, ${l.o2}")
                l.he.toDoubleOrNull()?.let { if (it > 0) line.append("/${l.he}") }
                if (l.set.isNotEmpty()) line.append(", ${l.set}")
                if (l.sld.isNotEmpty()) line.append("-${l.sld}")
                p.append(line).append("\n")
            }
            return p.toString()
        }

    private fun yn(b: Boolean) = if (b) "y" else "n"

    /** Collapse any stray newlines so one field can never spill into the next key. */
    private fun one(s: String) = s.replace('\n', ' ').replace('\r', ' ').trim()

    private fun fmt(v: Double): String =
        if (v == v.roundToInt().toDouble()) v.roundToInt().toString() else v.toString()

    /** Add a new level, or commit changes to the one being edited. */
    fun addEntry() {
        if (entry.d.isEmpty() || entry.t.isEmpty() || entry.o2.isEmpty()) return
        val id = editingID
        if (id != null) {
            val i = levels.indexOfFirst { it.id == id }
            if (i >= 0) {
                val wasEnabled = levels[i].enabled
                levels[i] = entry.copy(id = id, enabled = wasEnabled)
            }
            editingID = null
        } else {
            levels.add(entry)
        }
        entry = DiveLevel()
    }

    /** Load an existing level back into the entry fields for editing. */
    fun beginEdit(l: DiveLevel) {
        entry = l.copy()
        editingID = l.id
        if (l.set.isNotEmpty() || l.sld.isNotEmpty()) circuitClosed = true
    }

    fun cancelEdit() {
        editingID = null
        entry = DiveLevel()
    }

    fun move(l: DiveLevel, up: Boolean) {
        val i = levels.indexOfFirst { it.id == l.id }
        if (i < 0) return
        val j = if (up) i - 1 else i + 1
        if (j !in levels.indices) return
        val tmp = levels[i]; levels[i] = levels[j]; levels[j] = tmp
    }

    fun remove(l: DiveLevel) {
        if (editingID == l.id) cancelEdit()
        levels.removeAll { it.id == l.id }
    }

    fun setEnabled(l: DiveLevel, on: Boolean) {
        val i = levels.indexOfFirst { it.id == l.id }
        if (i >= 0) levels[i] = levels[i].copy(enabled = on)
    }

    fun calculate() {
        if (levels.none { it.enabled }) {
            notes = "No enabled dive levels — add a Depth / Time / O2 row first."
            planText = ""
            return
        }
        try {
            val r = ZPlan.plan(profileText, if (repetitive) lastTissue else null)
            planText = r.reportText
            notes = r.warnings
            lastTissue = r.tissueFileText
            // Log at the moment of calculation. Logging used to happen when the
            // Log button was pressed, which saved whatever planText happened to
            // hold — i.e. the previous calculation if any setting had changed
            // since — and appended a duplicate every time the log was merely
            // viewed. Recording it here means an entry always matches the
            // settings that produced it.
            appendLog()
        } catch (e: ZPlanException) {
            planText = ""
            notes = e.message ?: "Decompression planning failed"
        }
    }

    /** Short description of the dive and the settings behind a logged plan. */
    private val diveSummary: String
        get() {
            val du = if (depthsMetric) "m" else "ft"
            val dive = levels.filter { it.enabled }.joinToString(" + ") { l ->
                buildString {
                    append("${l.d}$du/${l.t}min ${l.o2}%")
                    l.he.toDoubleOrNull()?.let { if (it > 0) append("/${l.he}he") }
                }
            }.ifEmpty { "no levels" }

            // Named modelText, not model: a local called `model` would shadow the
            // property of the same name and read very confusingly.
            val modelText = if (model == "vval") "VVAL-18" else buildString {
                append("ZHL16-C")
                if (gfOn) {
                    val lo = if (useAltGF) altGfLow else gfLow
                    val hi = if (useAltGF) altGfHigh else gfHigh
                    append(" GF$lo/$hi")
                } else append(" cons ${conservatism.toInt()}%")
            }

            val extras = buildList {
                if (circuitClosed) add("CCR")
                if (decoGasesOn && decoGases.isNotBlank()) add("deco $decoGases")
                if (deepStops == "p" && !gfOn) add("Pyle $pyleTime min")
                if (extraSlow) add("extra-slow")
                if (plus3m) add(if (depthsMetric) "+3m" else "+10ft")
                if (plus5min) add("+5min")
                if (repetitive) add("SI $surfaceInterval")
            }

            return (listOf(dive, modelText) + extras).joinToString(" · ")
        }

    private fun appendLog() {
        if (planText.isEmpty()) return
        if (log.firstOrNull()?.text == planText) return   // don't stack duplicates
        log.add(0, LogEntry(summary = diveSummary, text = planText))
        store.save(log)
    }

    fun removeLog(e: LogEntry) {
        log.removeAll { it.id == e.id }
        store.save(log)
    }

    fun clearLog() {
        log.clear()
        store.save(log)
    }
}
