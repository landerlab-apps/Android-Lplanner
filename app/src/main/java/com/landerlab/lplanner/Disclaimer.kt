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
Tap the OC chip so it reads CCR — on a tablet, switch Open to Closed. Set (setpoint) and Sld (Scamahorn slide) then appear beside the mix.

DECO GASES
Click Yes and list the mixes, e.g. 50, 100. The planner picks the richest one allowed by Max PO2 and Max END. A GasSw row marks where the switch happens. Config can also hold you there for a few extra minutes — see Extended stops.

SETTINGS STRIP
On a phone the settings sit in one strip of chips above the tabs. It folds to a single summary line on the Plan tab so the schedule gets the full screen; the chevron opens or closes it by hand. altGF is a plain on/off here — its two numbers are set in Config.

CONFIG
Units, water, altitude, model, gradient factors, deep stops, ascent and descent rates, RMVs. Each section carries its own explanation.

SURFACE INTERVAL AND RESIDUAL GAS
When you surface, press "Next dive" to carry your inert gas loading forward into the dive you plan next. It is kept when the app is closed and ages with real time. While gas is carried you must state a surface interval — 48 hr, 24 hr or Actual — before Calculate will work.

Always use your exact surface interval time or a shorter duration if you're uncertain about how long to wait between dives.

Press Clear to declare yourself clean again.

READING THE PLAN
Press "Full screen" above the schedule for the plan on its own. The type size is computed to fit the width exactly, so turning the phone sideways makes it bigger, not just wider. A− and A+ override it, Fit returns to the computed size, Sun goes to full brightness for reading in sunlight. The screen is held awake the whole time. Tap once to hide the controls; Back closes it.

LOG
Press Keep above the schedule to file a plan you want. It is stored with the dive and settings that produced it. Nothing is logged unless you ask — the log used to take every calculation and filled with throwaway runs. Keep has nothing to do with "Next dive": it records a schedule, it does not load your tissues. Swipe an entry to delete it, or press Clear.

SHARE AND PRINT
Both become available once a plan has been calculated."""
}
