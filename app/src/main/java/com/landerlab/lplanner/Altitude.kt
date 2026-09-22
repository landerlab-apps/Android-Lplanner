package com.landerlab.lplanner

import org.json.JSONObject

enum class TripMode { CAR, AIRPLANE }

data class AltitudeTrip(
    val mode: TripMode = TripMode.CAR,
    val altitudeMeters: Double = 2000.0,
    val waitMinutes: Double = 360.0,
    val travelMinutes: Double = 60.0,
    val stayMinutes: Double = 1440.0,
    val oxygenMinutes: Double = 0.0,
    val oxygenBeforeLeaving: Boolean = false,
) {
    fun request(s: AltitudeSettings) = DoubleArray(Zpa.REQ_N).also {
        it[Zpa.REQ_FLIGHT] = if (mode == TripMode.AIRPLANE) 1.0 else 0.0
        it[Zpa.REQ_ALT_M] = altitudeMeters
        it[Zpa.REQ_WAIT_MIN] = waitMinutes
        it[Zpa.REQ_TRAVEL_MIN] = travelMinutes
        it[Zpa.REQ_STAY_MIN] = stayMinutes
        it[Zpa.REQ_O2_MIN] = oxygenMinutes
        it[Zpa.REQ_O2_LAST] = if (oxygenBeforeLeaving) 1.0 else 0.0
        it[Zpa.REQ_MASK_FO2] = s.maskO2Percent / 100
        it[Zpa.REQ_M1_DIVE_GF] = if (s.method1DiveGF) 1.0 else 0.0
        it[Zpa.REQ_M2_EE1] = if (s.method2EE1) 1.0 else 0.0
        it[Zpa.REQ_M2_LIMIT_PCT] = s.method2LimitPercent
        it[Zpa.REQ_M2_TOTAL] = if (s.method2Total) 1.0 else 0.0
    }

    fun toJson(): JSONObject = JSONObject()
        .put("mode", mode.name).put("altitudeMeters", altitudeMeters)
        .put("waitMinutes", waitMinutes).put("travelMinutes", travelMinutes)
        .put("stayMinutes", stayMinutes).put("oxygenMinutes", oxygenMinutes)
        .put("oxygenBeforeLeaving", oxygenBeforeLeaving)

    companion object {
        fun from(o: JSONObject?): AltitudeTrip {
            val d = AltitudeTrip()
            if (o == null) return d
            return AltitudeTrip(
                mode = runCatching { TripMode.valueOf(o.optString("mode", d.mode.name)) }.getOrDefault(d.mode),
                altitudeMeters = o.optDouble("altitudeMeters", d.altitudeMeters),
                waitMinutes = o.optDouble("waitMinutes", d.waitMinutes),
                travelMinutes = o.optDouble("travelMinutes", d.travelMinutes),
                stayMinutes = o.optDouble("stayMinutes", d.stayMinutes),
                oxygenMinutes = o.optDouble("oxygenMinutes", d.oxygenMinutes),
                oxygenBeforeLeaving = o.optBoolean("oxygenBeforeLeaving", d.oxygenBeforeLeaving),
            )
        }
    }
}

data class AltitudeSettings(
    val method1DiveGF: Boolean = false,
    val method2EE1: Boolean = false,
    val method2LimitPercent: Double = 1.0,
    val method2Total: Boolean = false,
    val maskO2Percent: Double = 100.0,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("method1DiveGF", method1DiveGF).put("method2EE1", method2EE1)
        .put("method2LimitPercent", method2LimitPercent).put("method2Total", method2Total)
        .put("maskO2Percent", maskO2Percent)

    companion object {
        fun from(o: JSONObject?): AltitudeSettings {
            val d = AltitudeSettings()
            if (o == null) return d
            return AltitudeSettings(
                method1DiveGF = o.optBoolean("method1DiveGF", d.method1DiveGF),
                method2EE1 = o.optBoolean("method2EE1", d.method2EE1),
                method2LimitPercent = o.optDouble("method2LimitPercent", d.method2LimitPercent),
                method2Total = o.optBoolean("method2Total", d.method2Total),
                maskO2Percent = o.optDouble("maskO2Percent", d.maskO2Percent),
            )
        }
    }
}

class AltitudeAnswer(private val v: DoubleArray) {
    private fun d(i: Int): Double? = v[i].takeUnless { it.isNaN() }
    private fun t(i: Int): Double? = d(i)?.takeUnless { it < 0 }

    val method1Available get() = v[Zpa.M1_AVAILABLE] > 0.5
    val method1LimitGF get() = v[Zpa.M1_LIMIT_GF]
    val method1GFAtWait get() = v[Zpa.M1_GF_AT_WAIT]
    val method1OK get() = v[Zpa.M1_OK] > 0.5
    val method1Earliest get() = t(Zpa.M1_EARLIEST)
    val method1EarliestAir get() = t(Zpa.M1_EARLIEST_AIR)
    val method1OxygenFromSurfacing get() = t(Zpa.M1_O2_FIRST)
    val method1OxygenBeforeLeaving get() = t(Zpa.M1_O2_LAST)
    val oxygenCNS get() = d(Zpa.M1_O2_CNS) ?: 0.0
    val oxygenOTU get() = d(Zpa.M1_O2_OTU) ?: 0.0

    val method2Available get() = v[Zpa.M2_AVAILABLE] > 0.5
    val method2Name get() = if (v[Zpa.M2_EE1] > 0.5) "EE1 (Di Muro 2020)" else "UT (Di Muro 2020)"
    val method2PDive get() = v[Zpa.M2_P_DIVE]
    val method2PAtWait get() = v[Zpa.M2_P_AT_WAIT]
    val method2AddedAtWait get() = v[Zpa.M2_ADDED_AT_WAIT]
    val method2OK get() = v[Zpa.M2_OK] > 0.5
    val method2Earliest get() = t(Zpa.M2_EARLIEST)
    val method2EarliestAir get() = t(Zpa.M2_EARLIEST_AIR)
    val method2OxygenFromSurfacing get() = t(Zpa.M2_O2_FIRST)
    val method2OxygenBeforeLeaving get() = t(Zpa.M2_O2_LAST)

    data class Row(val hours: Double, val gf: Double?, val added: Double?)

    val curve: List<Row>
        get() = (0 until Zpa.CURVE_N).map {
            Row(v[Zpa.CURVE_HOURS + it], d(Zpa.CURVE_GF + it), d(Zpa.CURVE_ADDED + it))
        }
}

object Zpa {
    const val REQ_FLIGHT = 0
    const val REQ_ALT_M = 1
    const val REQ_WAIT_MIN = 2
    const val REQ_TRAVEL_MIN = 3
    const val REQ_STAY_MIN = 4
    const val REQ_O2_MIN = 5
    const val REQ_O2_LAST = 6
    const val REQ_MASK_FO2 = 7
    const val REQ_M1_DIVE_GF = 8
    const val REQ_M2_EE1 = 9
    const val REQ_M2_LIMIT_PCT = 10
    const val REQ_M2_TOTAL = 11
    const val REQ_N = 12

    const val M1_AVAILABLE = 0
    const val M1_LIMIT_GF = 1
    const val M1_GF_AT_WAIT = 2
    const val M1_OK = 3
    const val M1_EARLIEST = 4
    const val M1_EARLIEST_AIR = 5
    const val M1_O2_FIRST = 6
    const val M1_O2_LAST = 7
    const val M1_O2_CNS = 8
    const val M1_O2_OTU = 9
    const val M2_AVAILABLE = 10
    const val M2_EE1 = 11
    const val M2_P_DIVE = 12
    const val M2_P_TRIP_NO_DIVE = 13
    const val M2_P_AT_WAIT = 14
    const val M2_ADDED_AT_WAIT = 15
    const val M2_OK = 16
    const val M2_EARLIEST = 17
    const val M2_EARLIEST_AIR = 18
    const val M2_O2_FIRST = 19
    const val M2_O2_LAST = 20
    const val CURVE_N = 8
    const val CURVE_HOURS = 21
    const val CURVE_GF = CURVE_HOURS + CURVE_N
    const val CURVE_ADDED = CURVE_GF + CURVE_N
}
