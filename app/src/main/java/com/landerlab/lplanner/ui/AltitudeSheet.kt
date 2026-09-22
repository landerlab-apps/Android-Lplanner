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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.landerlab.lplanner.AltitudeAnswer
import com.landerlab.lplanner.AltitudeTrip
import com.landerlab.lplanner.PlannerModel
import com.landerlab.lplanner.TripMode
import com.landerlab.lplanner.ZPlan
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AltitudeSheet(m: PlannerModel, onDismiss: () -> Unit) {
    val ft = if (m.depthsMetric) 1.0 else 0.3048
    val t0 = m.altitudeTrip
    var mode by remember { mutableStateOf(t0.mode) }
    var altitude by remember { mutableStateOf(fmt(t0.altitudeMeters / ft)) }
    var wait by remember { mutableStateOf(hm(t0.waitMinutes, short = true)) }
    var travel by remember { mutableStateOf(fmt(t0.travelMinutes)) }
    var stay by remember { mutableStateOf(fmt(t0.stayMinutes / 60)) }
    var oxygen by remember { mutableStateOf(fmt(t0.oxygenMinutes)) }
    var oxygenLast by remember { mutableStateOf(t0.oxygenBeforeLeaving) }
    var result by remember { mutableStateOf<AltitudeAnswer?>(null) }
    var failed by remember { mutableStateOf(false) }

    fun run() {
        val profile = m.lastProfile ?: return
        val t = AltitudeTrip(
            mode = mode,
            altitudeMeters = num(altitude) * ft,
            waitMinutes = minutes(wait),
            travelMinutes = num(travel),
            stayMinutes = num(stay) * 60,
            oxygenMinutes = num(oxygen),
            oxygenBeforeLeaving = oxygenLast,
        )
        m.altitudeTrip = t
        m.saveState()
        result = ZPlan.altitude(profile, m.lastTissue, m.lastIcm, t, m.altitudeSettings)
        failed = result == null
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Text("Altitude after diving", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
            Column(Modifier.weight(1f)) {}
            TextButton(onClick = onDismiss) { Text("Done") }
        }
        HorizontalDivider()
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            Seg(listOf("Car", "Airplane"), if (mode == TripMode.CAR) 0 else 1,
                modifier = Modifier.width(240.dp)) {
                mode = if (it == 0) TripMode.CAR else TripMode.AIRPLANE
                altitude = fmt((if (mode == TripMode.CAR) 2000.0 else 2438.0) / ft)
                travel = if (mode == TripMode.CAR) "60" else "20"
                stay = if (mode == TripMode.CAR) "24" else "4"
            }
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                LabeledField(
                    (if (mode == TripMode.CAR) "Altitude" else "Cabin altitude") + " (${m.depthUnit})",
                    altitude, Modifier.weight(1f),
                ) { altitude = it }
                LabeledField("Wait (h:mm)", wait, Modifier.weight(1f), numeric = false) { wait = it }
            }
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                LabeledField(if (mode == TripMode.CAR) "Drive (min)" else "Climb (min)",
                    travel, Modifier.weight(1f)) { travel = it }
                LabeledField(if (mode == TripMode.CAR) "At altitude (h)" else "Flight (h)",
                    stay, Modifier.weight(1f)) { stay = it }
            }
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom) {
                LabeledField("Oxygen (min)", oxygen, Modifier.weight(1f)) { oxygen = it }
                Seg(listOf("From surfacing", "Before leaving"), if (oxygenLast) 1 else 0,
                    enabled = num(oxygen) > 0, modifier = Modifier.weight(1.4f)) { oxygenLast = it == 1 }
            }
            OutlinedButton(onClick = { run() }) { Text("Calculate", fontWeight = FontWeight.Bold) }
            if (failed) Text("The dive could not be recomputed.", style = MaterialTheme.typography.bodySmall)
            result?.let { Output(m, it) }
        }
    }
}

@Composable
private fun Output(m: PlannerModel, r: AltitudeAnswer) {
    val w = hm(m.altitudeTrip.waitMinutes)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        HorizontalDivider()
        Text("Method 1  Bühlmann ZH-L16C", fontWeight = FontWeight.Bold)
        if (r.method1Available) {
            Line("Limit", String.format(Locale.US, "GF %.0f%%", r.method1LimitGF * 100))
            Line("GF needed at $w",
                pct(r.method1GFAtWait) + if (r.method1OK) "  within limit" else "  EXCEEDS limit")
            Line("Earliest departure", hm(r.method1Earliest))
            Line("Earliest, air only", hm(r.method1EarliestAir))
            Line("O2 to leave at $w", o2(r.method1OxygenFromSurfacing, r.method1OxygenBeforeLeaving))
            if (m.altitudeTrip.oxygenMinutes > 0) {
                Line("Mask O2", String.format(Locale.US, "%.0f%%", m.altitudeSettings.maskO2Percent))
                Line("Oxygen cost", String.format(Locale.US, "CNS %.0f%%, %.0f OTU", r.oxygenCNS, r.oxygenOTU))
            }
        } else {
            Text("Not available for VVAL-79 dives.", style = MaterialTheme.typography.bodySmall)
        }
        HorizontalDivider()
        Text("Method 2  ${r.method2Name}", fontWeight = FontWeight.Bold)
        if (r.method2Available) {
            Line("Limit", String.format(Locale.US, "P(DCS) %s %s%%",
                if (m.altitudeSettings.method2Total) "total" else "added",
                fmt(m.altitudeSettings.method2LimitPercent)))
            Line("P(DCS) of the dive", p2(r.method2PDive))
            Line("Leaving at $w", "total ${p2(r.method2PAtWait)}, added ${p2(r.method2AddedAtWait)}" +
                if (r.method2OK) "  within limit" else "  EXCEEDS limit")
            Line("Earliest departure", hm(r.method2Earliest))
            Line("Earliest, air only", hm(r.method2EarliestAir))
            Line("O2 to leave at $w", o2(r.method2OxygenFromSurfacing, r.method2OxygenBeforeLeaving))
        } else {
            Text("Not available: helium, closed circuit, or a dive in the series planned without it.",
                style = MaterialTheme.typography.bodySmall)
        }
        HorizontalDivider()
        CurveRow("leave after", "GF needed", "P(DCS) added")
        r.curve.forEach {
            CurveRow(String.format(Locale.US, "%.0f h", it.hours), it.gf?.let(::pct) ?: "–",
                it.added?.let(::p2) ?: "–")
        }
        Text("Model output. Neither model was fitted to altitude exposures.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun Line(k: String, v: String) {
    Row {
        Text(k, style = MonoTextSmall, modifier = Modifier.width(150.dp))
        Text(v, style = MonoTextSmall)
    }
}

@Composable
private fun CurveRow(a: String, b: String, c: String) {
    Row {
        Text(a, style = MonoTextSmall, textAlign = TextAlign.End, modifier = Modifier.width(90.dp))
        Text(b, style = MonoTextSmall, textAlign = TextAlign.End, modifier = Modifier.width(90.dp))
        Text(c, style = MonoTextSmall, textAlign = TextAlign.End, modifier = Modifier.width(110.dp))
    }
}

private fun num(s: String): Double = s.replace(',', '.').trim().toDoubleOrNull() ?: 0.0

private fun minutes(s: String): Double {
    val f = s.split(":").map { num(it) }
    return if (f.size == 2) f[0] * 60 + f[1] else (f.firstOrNull() ?: 0.0) * 60
}

private fun hm(v: Double?, short: Boolean = false): String {
    if (v == null) return "not within 72 h"
    val t = Math.round(v).toInt()
    return if (short) String.format(Locale.US, "%d:%02d", t / 60, t % 60)
    else String.format(Locale.US, "%d h %02d min", t / 60, t % 60)
}

private fun o2(a: Double?, b: Double?): String =
    "${a?.let { hm(it) } ?: "not possible"} from surfacing, ${b?.let { hm(it) } ?: "not possible"} before leaving"

private fun pct(g: Double): String = if (g < 0) "<0" else String.format(Locale.US, "%.0f%%", g * 100)
private fun p2(p: Double): String = String.format(Locale.US, "%.2f%%", p * 100)
private fun fmt(v: Double): String =
    if (v == Math.rint(v)) v.toLong().toString() else String.format(Locale.US, "%.1f", v)
