package cube.run.game.track

/** Night route: predator encounters alternate with clear recovery beats and lamp-lit coin runs. */
object NightSections {
    private val dg = Step::dg
    private val ft = Step::ft
    private val sld = Step::sld
    private val pn = Step::pn
    private val st = Step::st
    private val pl = Step::pl
    private val pf = Step::pf
    private const val JP = Step.JP
    private const val DK = Step.DK
    private const val CF = Step.CF
    private const val EM = Step.EM
    private const val VD = Step.VD

    val pool = listOf(
        Sect(240, 0, 1.15f, "WOLF TRACK", intArrayOf(ft(0), dg(1), ft(2), dg(1), ft(0))),
        Sect(241, 0, 1.05f, "LANTERN LINE", intArrayOf(CF, CF, dg(1), CF, CF)),
        Sect(242, 0, 1.0f, "MOONLIT HOP", intArrayOf(JP, dg(1), ft(0), dg(2), JP)),
        Sect(243, 1, 1.05f, "TIGER POUNCE", intArrayOf(st(0), dg(1), st(2), dg(1), st(0))),
        Sect(244, 1, .95f, "HOWL UNDERPASS", intArrayOf(DK, ft(0), DK, ft(2), dg(1))),
        Sect(245, 1, .9f, "FOREST GAP", intArrayOf(VD, dg(0), JP, dg(2), VD)),
        Sect(246, 2, .9f, "PACK CROSSING", intArrayOf(pn(1), dg(1), ft(0), ft(2), pn(1))),
        Sect(247, 2, .85f, "MOON BRIDGE", intArrayOf(pl(1), pf(1), pf(1), pf(1), dg(1), st(0))),
        Sect(248, 3, .8f, "PREDATOR GAUNTLET", intArrayOf(st(0), DK, st(2), JP, pn(1), dg(1), st(0))),
        Sect(249, 0, .8f, "QUIET STRETCH", intArrayOf(EM, ft(2), EM, CF)),
    )
}

/** Existing Matrix Circuit: readable branches, code coins, firewalls and server hazards. */
object HackerSections {
    private val dg = Step::dg
    private val ft = Step::ft
    private val sld = Step::sld
    private val hw = Step::hw
    private val dw = Step::dw
    private val pl = Step::pl
    private val pf = Step::pf
    private val pn = Step::pn
    private val st = Step::st
    private val vw = Step::vw
    private const val JP = Step.JP
    private const val DK = Step.DK
    private const val CF = Step.CF
    private const val EM = Step.EM

    val pool = listOf(
        Sect(250, 0, 1.2f, "FORK LEFT / RIGHT", intArrayOf(ft(0), ft(2), ft(0), ft(2), dg(1))),
        Sect(251, 0, 1.05f, "DATA STREAM", intArrayOf(CF, dg(0), CF, dg(2), CF)),
        Sect(252, 0, 1.0f, "ROUTE SWITCH", intArrayOf(dg(0), dg(1), dg(2), dg(1), dg(0))),
        Sect(253, 1, 1.0f, "GLITCH GATES", intArrayOf(sld(0), ft(2), sld(2), ft(0), dg(1))),
        Sect(254, 1, .95f, "FIREWALL HOP", intArrayOf(hw(0), dg(1), JP, hw(2), dg(1))),
        Sect(255, 1, .9f, "INVERTED INPUT", intArrayOf(DK, dw(0), dg(1), DK, dw(2))),
        Sect(256, 2, .9f, "BRANCHING CODE", intArrayOf(ft(0), dg(2), ft(2), dg(0), ft(0), dg(2))),
        Sect(257, 2, .85f, "SERVER STACK", intArrayOf(pl(0), pf(0), pf(0), dg(1), pl(2), pf(2))),
        Sect(258, 3, .8f, "ROOT ACCESS", intArrayOf(hw(0), ft(2), JP, sld(0), hw(2), dg(1), DK, ft(0))),
        Sect(259, 0, .8f, "SAFE BUFFER", intArrayOf(EM, ft(0), EM, ft(2))),
        Sect(270, 2, .86f, "TRACE ROUTE", intArrayOf(pn(0), dg(1), vw(2), ft(1), hw(0), dg(2))),
        Sect(271, 3, .72f, "OVERRIDE LOOP", intArrayOf(st(0), DK, sld(2), JP, pn(1), vw(0), dg(2))),
    )
}

/** Air route: eagle dives, gull flocks, cloud gaps, underpasses and thermal lift. */
object AirSections {
    private val dg = Step::dg
    private val ft = Step::ft
    private val hw = Step::hw
    private val dw = Step::dw
    private val pn = Step::pn
    private val st = Step::st
    private val vw = Step::vw
    private val pl = Step::pl
    private val pf = Step::pf
    private const val JP = Step.JP
    private const val DK = Step.DK
    private const val VD = Step.VD
    private const val PS = Step.PS
    private const val SW = Step.SW
    private const val EM = Step.EM
    private const val CF = Step.CF

    val pool = listOf(
        Sect(260, 0, 1.15f, "UPDRAFT", intArrayOf(ft(0), dg(1), ft(2), dg(1), EM)),
        Sect(261, 0, 1.05f, "EAGLE DIVE", intArrayOf(SW, dg(1), SW, dg(2), EM)),
        Sect(262, 0, .95f, "CLOUD GAPS", intArrayOf(vw(1), dg(0), JP, dg(2), vw(0))),
        Sect(263, 1, 1.0f, "FLOCK CROSSING", intArrayOf(pn(1), dg(1), ft(0), ft(2), pn(1))),
        Sect(264, 1, .95f, "WING UNDERPASS", intArrayOf(DK, ft(0), DK, dg(1), ft(2))),
        Sect(265, 1, .9f, "THERMAL PISTONS", intArrayOf(PS, dg(1), PS, dg(0), PS)),
        Sect(266, 2, .9f, "BALLOON GATES", intArrayOf(hw(1), ft(2), dg(1), hw(0), ft(2))),
        Sect(267, 2, .85f, "CLOUD BRIDGES", intArrayOf(pl(1), pf(1), pf(1), dg(2), pl(0), pf(0), dg(1))),
        Sect(268, 3, .78f, "STORM FRONT", intArrayOf(st(0), DK, vw(1), SW, pn(2), dg(1), EM)),
        Sect(269, 0, .82f, "CLEAR SKY", intArrayOf(CF, ft(0), CF, ft(2), dg(1), CF)),
    )
}

/** Lava Caves: crust breaks, timed vents, falling rock and safe-lane paths around molten gaps. */
object LavaSections {
    private val dg = Step::dg
    private val ft = Step::ft
    private val hw = Step::hw
    private val dw = Step::dw
    private val pn = Step::pn
    private val st = Step::st
    private val vw = Step::vw
    private val pl = Step::pl
    private val pf = Step::pf
    private val pd = Step::pd
    private const val JP = Step.JP
    private const val DK = Step.DK
    private const val VD = Step.VD
    private const val PS = Step.PS
    private const val SW = Step.SW
    private const val PM = Step.PM
    private const val EM = Step.EM
    private const val CF = Step.CF

    val pool = listOf(
        Sect(280, 0, 1.12f, "EMBER CROSSING", intArrayOf(ft(0), dg(1), ft(2), EM, dg(1))),
        Sect(281, 0, 1.04f, "VENT TIMING", intArrayOf(PS, dg(1), PS, dg(0), EM)),
        Sect(282, 1, 1.0f, "CRACKED CRUST", intArrayOf(vw(1), dg(0), VD, dg(2), vw(1))),
        Sect(283, 1, .94f, "BOULDER ROLL", intArrayOf(PM, dg(1), st(0), dg(2), PM)),
        Sect(284, 2, .88f, "LAVA BRIDGE", intArrayOf(pl(1), pf(1), pf(1), dg(2), pl(0), pf(0))),
        Sect(285, 2, .83f, "GEYSER LEAP", intArrayOf(pd(1), JP, dg(1), SW, dg(2), pd(0))),
        Sect(286, 3, .72f, "CALDERA RUN", intArrayOf(pn(1), PS, dg(0), VD, dg(2), hw(1), dg(0))),
        Sect(287, 1, .88f, "MAGMA SHOWER", intArrayOf(Step.mt(0), dg(1), Step.mt(2), CF, Step.mt(1), dg(0), Step.mt(2))),
        Sect(288, 2, .78f, "BASALT FALL", intArrayOf(Step.mt(1), PM, dg(0), Step.mt(2), JP, dg(1), Step.mt(0))),
    )
}

/** Galaxy campaign routes use ordinary gravity and familiar controls, not the Bonus Space physics library. */
object GalaxySections {
    private val dg = Step::dg
    private val ft = Step::ft
    private val hw = Step::hw
    private val dw = Step::dw
    private val pn = Step::pn
    private val st = Step::st
    private val vw = Step::vw
    private val pl = Step::pl
    private val pf = Step::pf
    private val pd = Step::pd
    private const val JP = Step.JP
    private const val DK = Step.DK
    private const val VD = Step.VD
    private const val PS = Step.PS
    private const val SW = Step.SW
    private const val PM = Step.PM
    private const val EM = Step.EM
    private const val CF = Step.CF

    val pool = listOf(
        Sect(320, 0, 1.16f, "ORBITAL DEBRIS", intArrayOf(ft(0), dg(1), ft(2), CF, ft(0))),
        Sect(321, 0, 1.05f, "STAR TRAIL", intArrayOf(CF, dg(0), CF, dg(2), EM, CF)),
        Sect(322, 1, 1.0f, "ASTEROID BELT", intArrayOf(vw(1), dg(0), JP, ft(2), vw(0))),
        Sect(323, 1, .94f, "DRONE ORBIT", intArrayOf(PM, dg(1), PM, ft(0), DK, ft(2))),
        Sect(324, 2, .88f, "SOLAR FLARE", intArrayOf(SW, ft(1), PS, dg(0), SW, dg(2))),
        Sect(325, 2, .84f, "STATION BRIDGE", intArrayOf(pl(1), pf(1), pf(1), dg(2), pl(0), pf(0), dg(1))),
        Sect(326, 3, .76f, "SUPERNOVA RUN", intArrayOf(st(1), JP, pn(0), PS, dg(2), vw(1), SW, dg(1))),
        Sect(327, 3, .70f, "NEBULA CIRCUIT", intArrayOf(pd(1), JP, dg(1), PM, hw(0), DK, ft(2), CF)),
    )
}

/** Separate, colder Matrix branch: visual glitches and branching routes without replacing Matrix Circuit. */
object NexusSections {
    private val dg = Step::dg
    private val ft = Step::ft
    private val sld = Step::sld
    private val hw = Step::hw
    private val dw = Step::dw
    private val pn = Step::pn
    private val st = Step::st
    private val vw = Step::vw
    private val pl = Step::pl
    private val pf = Step::pf
    private val pd = Step::pd
    private const val JP = Step.JP
    private const val DK = Step.DK
    private const val VD = Step.VD
    private const val PS = Step.PS
    private const val SW = Step.SW
    private const val PM = Step.PM
    private const val EM = Step.EM
    private const val CF = Step.CF

    val pool = listOf(
        Sect(290, 0, 1.15f, "FRACTURED INPUT", intArrayOf(ft(0), ft(2), dg(1), ft(0), ft(2))),
        Sect(291, 0, 1.06f, "SAFE BUFFER", intArrayOf(EM, CF, dg(0), EM, dg(2))),
        Sect(292, 1, 1.0f, "FORK BINARY", intArrayOf(dg(0), dg(2), dg(0), dg(2), dg(1))),
        Sect(293, 1, .95f, "PHASE FIREWALL", intArrayOf(hw(0), dg(1), JP, hw(2), dg(1))),
        Sect(294, 1, .91f, "TRACE PENDULUM", intArrayOf(PM, ft(0), DK, ft(2), PM)),
        Sect(295, 2, .87f, "CORRUPTED BRIDGE", intArrayOf(pl(0), pf(0), pf(0), dg(1), pl(2), pf(2))),
        Sect(296, 2, .82f, "VOID SWITCH", intArrayOf(vw(1), dg(0), VD, dg(2), sld(1), ft(0))),
        Sect(297, 3, .72f, "ROOT OVERRIDE", intArrayOf(st(0), PS, dg(1), SW, pn(2), vw(0), pd(1), JP)),
    )
}

/** Tropical Beach: surf breaks, tidal gaps, driftwood hops and boardwalk climbs. */
object BeachSections {
    private val dg = Step::dg
    private val ft = Step::ft
    private val hw = Step::hw
    private val pn = Step::pn
    private val vw = Step::vw
    private val pl = Step::pl
    private val pf = Step::pf
    private val pd = Step::pd
    private val st = Step::st
    private const val SW = Step.SW
    private const val JP = Step.JP
    private const val DK = Step.DK
    private const val EM = Step.EM
    private const val CF = Step.CF

    val pool = listOf(
        Sect(300, 0, 1.15f, "TIDE POOLS", intArrayOf(vw(0), dg(1), vw(2), EM)),
        Sect(301, 0, 1.05f, "DRIFTWOOD HOP", intArrayOf(ft(0), JP, dg(1), ft(2), EM)),
        Sect(302, 1, 1.0f, "CRAB CROSSING", intArrayOf(pn(0), dg(1), pn(2), CF)),
        Sect(303, 1, .95f, "SURF BREAK", intArrayOf(SW, dg(1), SW, ft(0), EM)),
        Sect(304, 1, .92f, "BOARDWALK CLIMB", intArrayOf(pl(1), pf(1), pf(1), dg(2), pl(0), pf(0))),
        Sect(305, 2, .86f, "HIGH TIDE", intArrayOf(vw(1), dg(0), JP, vw(2), dg(1), CF)),
        Sect(306, 2, .82f, "UMBRELLA SLALOM", intArrayOf(hw(0), dg(1), hw(2), DK, ft(0))),
        Sect(307, 3, .72f, "KING TIDE", intArrayOf(st(1), vw(0), JP, vw(2), dg(1), hw(0), EM)),
    )
}

/** Railway Yard: signals, moving crossings, tunnel clearances and safe platform lanes. */
object RailwaySections {
    private val dg = Step::dg
    private val ft = Step::ft
    private val hw = Step::hw
    private val dw = Step::dw
    private val pn = Step::pn
    private val st = Step::st
    private const val SW = Step.SW
    private val pl = Step::pl
    private val pf = Step::pf
    private const val JP = Step.JP
    private const val DK = Step.DK
    private const val PM = Step.PM
    private const val EM = Step.EM
    private const val CF = Step.CF

    val pool = listOf(
        Sect(310, 0, 1.12f, "SIGNAL GREEN", intArrayOf(CF, ft(0), dg(1), ft(2), EM)),
        Sect(311, 0, 1.04f, "CROSSING GATE", intArrayOf(hw(1), ft(0), dg(1), ft(2), EM)),
        Sect(312, 1, 1.0f, "FREIGHT SLALOM", intArrayOf(pn(0), dg(1), pn(2), ft(1), CF)),
        Sect(313, 1, .96f, "TUNNEL DUCK", intArrayOf(DK, dw(0), dg(1), dw(2), EM)),
        Sect(314, 1, .91f, "PLATFORM RUN", intArrayOf(pl(1), pf(1), pf(1), dg(0), pl(2), pf(2))),
        Sect(315, 2, .86f, "SWITCHYARD", intArrayOf(ft(0), SW, dg(1), ft(2), pn(1))),
        Sect(316, 2, .82f, "CARGO CRANE", intArrayOf(PM, dg(1), JP, st(0), dg(2), PM)),
        Sect(317, 3, .72f, "NIGHT EXPRESS", intArrayOf(st(1), hw(0), JP, dg(1), PM, ft(2), CF)),
    )
}
