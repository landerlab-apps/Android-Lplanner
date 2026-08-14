package com.landerlab.lplanner

/**
 * Disclaimer.kt — Lplanner Android v1.1.0
 *
 * Shown by the Info button and reproduced in the documentation of every build.
 * [algorithms] names the models the build actually ships, so Lplanner79 states
 * VVAL-79 rather than VVAL-18. Kept identical in wording to the Swift
 * `Disclaimer` in ZPlannerView.swift — the two must not drift.
 */
object Disclaimer {

    var algorithms: String = "A. A. Buhlmann's algorithm or VVAL-18 algorithm"

    val text: String
        get() = "This generated dive schedule could indirectly kill you and probably has " +
            "bugs. The author does not warrant that it accurately reflects " +
            algorithms + ". This dive schedule is experimental, and you use it at " +
            "your own risk."
}

/**
 * Short how-to shown in the Info dialog, under the disclaimer.
 * Kept word-for-word identical to `Manual` in ZPlannerView.swift.
 */
object Manual {
    const val text: String = """ENTERING A DIVE
Type Depth, Time and O2 % — plus He % for trimix — then press Add >>. Repeat for each level. Tap a level to edit it, use the arrows to reorder, × to remove. Click a level's box to leave it out without deleting it.

CLOSED CIRCUIT
Switch to Closed to show Set (setpoint) and Sld (Scamahorn slide).

DECO GASES
Click Yes and list the mixes, e.g. 50, 100. The planner picks the richest one allowed by Max PO2 and Max END. Config can also hold you at the switch for a few extra minutes — see Extended stops.

CONFIG
Units, water, altitude, model, gradient factors, deep stops, ascent and descent rates, RMVs. Each section carries its own explanation.

SURFACE INTERVAL AND RESIDUAL GAS
After a dive press "Dive done" to carry your inert gas loading forward. It is kept when the app is closed and ages with real time. While gas is carried you must state a surface interval — 48 hr, 24 hr or Actual — before Calculate will work.

Always use your exact surface interval time or a shorter duration if you're uncertain about how long to wait between dives.

Press Clear to declare yourself clean again.

LOG
Every successful Calculate is recorded automatically, with the dive and settings that produced it. Swipe an entry to delete it, or press Clear.

SHARE AND PRINT
Both become available once a plan has been calculated."""
}
