package com.landerlab.lplanner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.landerlab.lplanner.PlannerModel
import com.landerlab.lplanner.ZPlan
import kotlin.math.roundToInt

/**
 * ConfigSheet.kt — Lplanner Android v1.0.0
 *
 * Every setting in one place, with the same descriptions as the SwiftUI ConfigSheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigSheet(m: PlannerModel, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Text("Config", style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
            Column(Modifier.weight(1f)) {}
            TextButton(onClick = onDismiss) { Text("Done") }
        }
        HorizontalDivider()

        Column(
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            ConfigGroup(
                "Units",
                "Depths sets the units for depth, altitude, stop distance and END. RMVs sets the " +
                    "units for breathing-rate and gas-consumption figures — the two can differ.",
            ) {
                SettingRow("Depths") {
                    Seg(listOf("Feet", "Meters"), if (m.depthsMetric) 1 else 0) { m.depthsMetric = it == 1 }
                }
                SettingRow("RMVs") {
                    Seg(listOf("Cu.ft.", "Liters"), if (m.rmvMetric) 1 else 0) { m.rmvMetric = it == 1 }
                }
            }

            ConfigGroup(
                "Environment",
                "Fresh or salt water changes the depth-to-pressure conversion. O2 Narcotic controls " +
                    "whether oxygen counts as narcotic when calculating equivalent narcotic depths (ENDs).",
            ) {
                SettingRow("Water") {
                    Seg(listOf("Fresh", "Salt"), if (m.saltWater) 1 else 0) { m.saltWater = it == 1 }
                }
                SettingRow("O2 Narcotic") {
                    Seg(listOf("No", "Yes"), if (m.o2Narcotic) 1 else 0) { m.o2Narcotic = it == 1 }
                }
            }

            ConfigGroup(
                "Model",
                "ZHL16-C is the Buhlmann set used here. VVAL-18 is the U.S. Navy Thalmann EL-DCM " +
                    "(exponential uptake, linear elimination); gradient factors and Conservatism do not " +
                    "apply to it. With gradient factors enabled, Pyle deep stops are disabled — GF Low " +
                    "provides the deep-stop function — and Conservatism is ignored.",
            ) {
                Seg(
                    listOf("ZHL16-C", "VVAL-18"),
                    if (m.model == "vval") 1 else 0,
                    modifier = Modifier.fillMaxWidth(),
                ) { m.model = if (it == 1) "vval" else "c" }

                if (m.model != "vval") {
                    Check("Gradient factors", m.useGF) { m.useGF = it }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LabeledField("GF Low", m.gfLow, enabled = m.useGF) { m.gfLow = it }
                        LabeledField("GF High", m.gfHigh, enabled = m.useGF) { m.gfHigh = it }
                    }
                }
            }

            if (m.model != "vval") {
                ConfigGroup(
                    "Alternative gradient factors",
                    "A second GF pair, used instead of the main pair whenever altGF is checked on the " +
                        "main screen. Set these to whatever you like — any values are accepted, low and " +
                        "high independently, and they need not bracket the main pair. 100/100 gives the " +
                        "pure Buhlmann ZHL-16C ceiling; values above 100 go beyond it (less conservative " +
                        "than the raw model); a low GF Low with a high GF High deepens the first stop " +
                        "while keeping the shallow stops short. Editable here or directly beside the " +
                        "altGF checkbox on the main screen.",
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LabeledField("Alt GF Low", m.altGfLow) { m.altGfLow = it }
                        LabeledField("Alt GF High", m.altGfHigh) { m.altGfHigh = it }
                    }
                }

                ConfigGroup(
                    "NDL calculation",
                    "Which gradient factor decides whether a direct, no-stop ascent to the surface is " +
                        "still allowed. GF High is the standard behaviour for ZHL16-C. GF Low is stricter " +
                        "and ends the no-decompression phase earlier.",
                ) {
                    Text("Calculate NDL by", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                    Seg(
                        listOf("GF High (standard)", "GF Low"),
                        if (m.ndlLow) 1 else 0,
                        enabled = m.gfOn,
                        modifier = Modifier.fillMaxWidth(),
                    ) { m.ndlLow = it == 1 }
                }
            }

            ConfigGroup(
                "Conditions",
                "Altitude of the dive site (0 for sea level; be extra conservative if you are still " +
                    "off-gassing from travel to altitude). Conservatism applies only when gradient " +
                    "factors are switched off. It (0–50 %) preloads the tissue compartments with " +
                    "additional inert gas — nitrogen, and helium in proportion when the profile uses " +
                    "trimix — weighted from the fast compartments (none) to the slow ones (the full " +
                    "percentage), as if a previous dive had been made. Zero is the clean-diver profile.",
            ) {
                LabeledField("Altitude", m.altitude) { m.altitude = it }
                Column(Modifier.alphaIf(!m.gfOn)) {
                    Text(
                        if (m.gfOn) "Conservatism — not used with gradient factors"
                        else "Conservatism: ${m.conservatism.toInt()} %  (0–50 maximum)",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    )
                    Slider(
                        value = m.conservatism.toFloat(),
                        onValueChange = { m.conservatism = it.roundToInt().toDouble() },
                        valueRange = 0f..50f,
                        steps = 49,
                        enabled = !m.gfOn,
                    )
                }
            }

            if (!(m.useGF && m.model != "vval")) {
                ConfigGroup(
                    "Deep stops",
                    "Pyle deep stops insert short stops between the bottom and the first normal stop " +
                        "(mean-depth rule, re-run iteratively) to reduce microbubble formation and " +
                        "post-dive fatigue. Pyle stop time is the minutes spent at each generated stop " +
                        "(1–5). Stop distance is the interval between normal stops; Last stop is the " +
                        "depth of the final stop — some prefer pulling the 10 ft / 3 m stop deeper. " +
                        "Not shown when gradient factors are enabled: GF Low takes over the deep-stop role.",
                ) {
                    Seg(
                        listOf("None", "Pyle"),
                        if (m.deepStops == "p") 1 else 0,
                        modifier = Modifier.fillMaxWidth(),
                    ) { m.deepStops = if (it == 1) "p" else "n" }

                    if (m.deepStops == "p") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("Pyle stop time: ${m.pyleTime} min  (1–5)")
                            TextButton(onClick = { if (m.pyleTime > 1) m.pyleTime-- }) { Text("−") }
                            TextButton(onClick = { if (m.pyleTime < 5) m.pyleTime++ }) { Text("+") }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LabeledField("Stop distance", m.stopDistance) { m.stopDistance = it }
                        LabeledField("Last stop", m.lastStop) { m.lastStop = it }
                    }
                }
            }

            ConfigGroup(
                "Ascent behaviour (experimental)",
                "Extra slow delays the ascent to the next stop while the off-gassing gradient of any " +
                    "compartment — tissue inert tension minus ambient pressure, i.e. supersaturation — " +
                    "exceeds 1.25 bar. It only ever adds time at the deeper depth, so the schedule " +
                    "stays below the gradient factor regardless of the rule. Two limits keep it " +
                    "practical: it never applies to the final ascent to the surface, and it adds at " +
                    "most 5 minutes per stop. Time spent held is counted in the total decompression " +
                    "time. Noticeable on dives that leave a compartment strongly supersaturated at " +
                    "the stop.",
            ) {
                Check("Extra slow ascent rule", m.extraSlow) { m.extraSlow = it }
            }

            ConfigGroup(
                "Descent — range, rate",
                "One range per line: depth1-depth2, rate (ft or m per minute). List shallowest range " +
                    "first, leave no gaps.",
            ) {
                RateEditor(m.descentRates, minHeight = 60) { m.descentRates = it }
            }

            ConfigGroup(
                "Ascent — range, rate (deepest first)",
                "One range per line, deepest range first, no gaps. Slow shallow ascent rates are " +
                    "credited to the decompression and can shorten stops or remove them entirely.",
            ) {
                RateEditor(m.ascentRates, minHeight = 96) { m.ascentRates = it }
            }

            ConfigGroup(
                "Deco Set Point (CCR) / Slide rate",
                "Setpoint changes by depth range during CCR deco, one per line, e.g. 80-30, 1.4 — a " +
                    "setpoint of 0 switches to open circuit for that range. Only active when the circuit " +
                    "is set to Closed on the main screen (disabled for open-circuit dives). Slide rate is " +
                    "the PO2 burned off per minute during a Scamahorn Slide: enter a bottom setpoint like " +
                    "1.2-1.6 to ride the descent PO2 spike down to the setpoint for a deco advantage.",
            ) {
                RateEditor(m.decoSetpoints, enabled = m.circuitClosed, minHeight = 60) {
                    m.decoSetpoints = it
                }
                LabeledField("Slide rate (PO2/min)", m.slideRate) { m.slideRate = it }
            }

            ConfigGroup(
                "Deco gas limits",
                "The planner auto-selects the deco gas with the highest PO2 that stays within Max PO2 " +
                    "and Max END. Set Max PO2 to 1.6 if you want 100% O2 at the 20 ft / 6 m stop; tune it " +
                    "down to lower CNS exposure at the cost of longer deco.",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LabeledField("Max PO2", m.maxPO2) { m.maxPO2 = it }
                    LabeledField("Max END", m.maxEND) { m.maxEND = it }
                }
            }

            ConfigGroup(
                "RMV values",
                "Respiratory Minute Volume for gas-consumption planning, in the RMV units above. Deco " +
                    "is usually lower than Bottom, since you are more at rest hanging on the line. If you " +
                    "don't know your RMV, measure it.",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LabeledField("Bottom", m.bottomRMV) { m.bottomRMV = it }
                    LabeledField("Deco", m.decoRMV) { m.decoRMV = it }
                }
            }

            Text(
                "Lplanner 1.0.0 · engine ZPlanKit ${ZPlan.version}",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun SettingRow(label: String, content: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(120.dp))
        Column(Modifier.width(240.dp)) { content() }
    }
}
