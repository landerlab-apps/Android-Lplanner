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

    var algorithms: String =
        "A. A. Buhlmann's algorithm, the VVAL-18 algorithm, or the VPM-B algorithm"

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
Click Yes and list the mixes, e.g. 50, 100. The planner picks the richest one allowed by Max PO2 and Max END. The new mix appears in the gas column of the stop where you change on to it. If the switch depth is not a stop, a GasSw row marks it instead. Config can also hold you there for a few extra minutes — see Extended stops.

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
Both become available once a plan has been calculated.

WARNINGS
To keep the schedule readable on a phone, warnings are not printed under the table. Read them here and apply them yourself — the planner will not stop you.

Gas density. Above 5.2 g/L a bottom mix is denser than ideal; above 6.2 g/L it exceeds the limit given by Anthony & Mitchell, where work of breathing and CO2 retention rise steeply. CO2 retention is itself a risk factor for oxygen toxicity and narcosis. Add helium. For reference, 18/45 at 70 m is 6.4 g/L and 18/50 brings it to 5.9.

Isobaric counterdiffusion. Switching from a high-helium mix to a high-nitrogen one can raise the total inert load even as you ascend. Keep the nitrogen increase modest on a switch, and do not jump from trimix straight to air or nitrox at depth.

VVAL-18 on trimix. The U.S. Navy publishes no helium parameters for this model; the helium handling here is this project's own unvalidated extrapolation, and it begins decompression far shallower on helium than a bubble model does. Use VPM-B, or ZHL16-C with gradient factors, for trimix.

Oxygen exposure. CNS % and OTUs are printed with every plan. They are not warnings and nothing enforces them — 100 % CNS is a limit, not a target.

None of this replaces the disclaimer above. Validate every schedule against independent tables or software before diving it."""
}
