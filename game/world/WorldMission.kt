package cube.run.game.world

import cube.run.data.Worlds
import cube.run.game.track.CoinType

/** Small, deterministic objectives for the campaign biomes and the Galaxy route. */
class WorldMission(
    private val cityCoinTarget: Int = 8,
    private val winterRowTarget: Int = 24,
    private val stardustCoinTarget: Int = 10,
    private val nightPredatorTarget: Int = 5,
    private val hackerDataTarget: Int = 8,
    private val dinosaurTarget: Int = 5,
    private val lavaCrystalTarget: Int = 10,
    private val nexusFragmentTarget: Int = 10,
    private val railwayRowTarget: Int = 22,
    private val beachRowTarget: Int = 20,
    private val airBirdTarget: Int = 6,
) {
    enum class Kind {
        NONE, CITY_BLUE_COINS, WINTER_ROWS, STARDUST_COINS, NIGHT_PREDATORS,
        HACKER_DATA, JUNGLE_DINOSAURS, LAVA_CRYSTALS, NEXUS_FRAGMENTS,
        RAILWAY_ROWS, BEACH_ROWS, AIR_BIRDS,
    }

    init {
        require(cityCoinTarget > 0 && winterRowTarget > 0 && stardustCoinTarget > 0 &&
            nightPredatorTarget > 0 && hackerDataTarget > 0 && dinosaurTarget > 0 &&
            lavaCrystalTarget > 0 && nexusFragmentTarget > 0 && railwayRowTarget > 0 &&
            beachRowTarget > 0 && airBirdTarget > 0)
    }

    var kind: Kind = Kind.NONE
        private set
    var collected: Int = 0
        private set
    var completed: Boolean = false
        private set

    val active: Boolean get() = kind != Kind.NONE
    val target: Int get() = when (kind) {
        Kind.CITY_BLUE_COINS -> cityCoinTarget
        Kind.WINTER_ROWS -> winterRowTarget
        Kind.STARDUST_COINS -> stardustCoinTarget
        Kind.NIGHT_PREDATORS -> nightPredatorTarget
        Kind.HACKER_DATA -> hackerDataTarget
        Kind.JUNGLE_DINOSAURS -> dinosaurTarget
        Kind.LAVA_CRYSTALS -> lavaCrystalTarget
        Kind.NEXUS_FRAGMENTS -> nexusFragmentTarget
        Kind.RAILWAY_ROWS -> railwayRowTarget
        Kind.BEACH_ROWS -> beachRowTarget
        Kind.AIR_BIRDS -> airBirdTarget
        Kind.NONE -> 0
    }
    val progress: Float get() = if (target == 0) 0f else (collected.toFloat() / target).coerceIn(0f, 1f)

    /** Start the optional objective belonging to a campaign biome. */
    fun enterWorld(worldId: Int) {
        kind = when (worldId) {
            Worlds.CITY_ID -> Kind.CITY_BLUE_COINS
            Worlds.WINTER_ID -> Kind.WINTER_ROWS
            Worlds.NIGHT_ID -> Kind.NIGHT_PREDATORS
            Worlds.HACKER_ID -> Kind.HACKER_DATA
            Worlds.JUNGLE_ID -> Kind.JUNGLE_DINOSAURS
            Worlds.LAVA_ID -> Kind.LAVA_CRYSTALS
            Worlds.NEXUS_ID -> Kind.NEXUS_FRAGMENTS
            Worlds.RAILWAY_ID -> Kind.RAILWAY_ROWS
            Worlds.BEACH_ID -> Kind.BEACH_ROWS
            Worlds.AIR_ID -> Kind.AIR_BIRDS
            else -> Kind.NONE
        }
        resetProgress()
    }

    /** Galaxy has its own coin objective so a portal visit does not erase a campaign mission. */
    fun enterSpace() {
        kind = Kind.STARDUST_COINS
        resetProgress()
    }

    fun clear() {
        kind = Kind.NONE
        resetProgress()
    }

    /** Count only the themed coin family belonging to the active objective. */
    fun collectCoin(type: CoinType): Boolean {
        val counts = when (kind) {
            Kind.CITY_BLUE_COINS -> type == CoinType.BLUE
            Kind.STARDUST_COINS -> true
            Kind.HACKER_DATA, Kind.NEXUS_FRAGMENTS -> type == CoinType.MIXED
            Kind.LAVA_CRYSTALS -> type == CoinType.PINK
            else -> false
        }
        return counts && advance()
    }

    /** Survival-route objectives advance only when a scored row is safely cleared. */
    fun scoreRow(): Boolean = when (kind) {
        Kind.WINTER_ROWS, Kind.RAILWAY_ROWS, Kind.BEACH_ROWS -> advance()
        else -> false
    }

    /** Reaching the end of a wolf/tiger row counts as one successfully evaded predator encounter. */
    fun evadePredator(): Boolean = kind == Kind.NIGHT_PREDATORS && advance()

    /** A cleared dinosaur crossing is a successful Jungle encounter. */
    fun evadeDinosaur(): Boolean = kind == Kind.JUNGLE_DINOSAURS && advance()

    /** A cleared aerial hazard row is one successful Air encounter. */
    fun evadeAirBird(): Boolean = kind == Kind.AIR_BIRDS && advance()

    private fun advance(): Boolean {
        if (!active || completed) return false
        collected = (collected + 1).coerceAtMost(target)
        if (collected < target) return false
        completed = true
        return true
    }

    private fun resetProgress() {
        collected = 0
        completed = false
    }
}
