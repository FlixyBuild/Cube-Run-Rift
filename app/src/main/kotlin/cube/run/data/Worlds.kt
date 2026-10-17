package cube.run.data

/**
 * The worlds (biomes) the track runs through. Each is a palette + a kind of
 * roadside decoration; the game cross-fades between them every so often
 * (see game.world.WorldRunner). Obstacles keep their fixed hue offsets from
 * [hue], so every world reads the same way: pillars, walls and bars stay
 * recognisable while everything around them changes. Palettes are loud on
 * purpose: saturated skies, two-tone candy floors (kept deeper than any
 * cube skin so the cube always stands out), bright roadside.
 */
object Worlds {
    // roadside decoration styles (drawn by Scenery)
    const val LOLLIPOPS = 0   // candy: gumdrops, canes, cupcakes
    const val TOWERS = 1      // city: tall blocks with lit windows
    const val CRYSTALS = 2    // caves: shard clusters
    const val SPIKES = 3      // frost: tapering white-capped stacks
    const val CACTI = 4       // dunes: blocks with arms
    const val RINGS = 5       // space: floating spun frames
    const val RAILWAY = 6     // signal posts, sleepers and rail gantries
    const val JUNGLE = 7      // blocky trunks, layered canopy and vines
    const val HIGHWAY = 8     // signposts, lamp poles and night asphalt
    const val BEACH = 9       // palm trees, boardwalk sand, surf and open water
    const val NIGHT = 10      // moonlit road, lamps and moving predators
    const val HACKER = 11     // Matrix-inspired neon code circuit and forked routes
    const val AIR = 12        // high-altitude cloud road and swooping bird hazards
    const val NEXUS = 13      // fractured neon-code architecture and glitching hazards

    const val NEON_CITY_ID = 1
    const val LAVA_ID = 2
    const val CITY_ID = NEON_CITY_ID
    const val WINTER_ID = 3
    const val RAILWAY_ID = 6
    const val JUNGLE_ID = 7
    const val GALAXY_ID = 5
    /** Internal compatibility alias: the former Space route now presents as Galaxy. */
    const val SPACE_ID = GALAXY_ID
    const val HIGHWAY_ID = 8
    const val BEACH_ID = 9
    const val NIGHT_ID = 10
    const val HACKER_ID = 11
    const val AIR_ID = 12
    const val NEXUS_ID = 13

    /** Full campaign: legacy Lava is now selectable, Galaxy replaces Space, and Glitch Nexus is new. */
    val campaignIds: List<Int> = listOf(JUNGLE_ID, NEON_CITY_ID, LAVA_ID, WINTER_ID, RAILWAY_ID, GALAXY_ID, BEACH_ID, NIGHT_ID, HACKER_ID, AIR_ID, NEXUS_ID)
    val campaign: List<World> get() = campaignIds.map(::get)
    fun isCampaignWorld(id: Int): Boolean = id in campaignIds

    class World(
        val id: Int,
        val name: String,
        /** The obstacle/base hue; obstacle kinds add their offsets to it. */
        val hue: Float,
        // sky gradient, HSV
        val skyTopH: Float, val skyTopS: Float, val skyTopV: Float,
        val skyBotH: Float, val skyBotS: Float, val skyBotV: Float,
        // floor checker
        val floorH: Float, val floorS: Float, val floorV: Float, val floorAltV: Float,
        // roadside
        val postH: Float, val postS: Float, val postV: Float,
        val deco: Int,
        /** The far haze mixes this much sky-top into the fog colour (0 = horizon, 1 = zenith). */
        val fogMix: Float = 0.62f,
    )

    val all: List<World> = listOf(
        World(0, "Candy Fields", 320f, 318f, 0.55f, 1f, 275f, 0.6f, 0.75f, 318f, 0.72f, 0.7f, 0.54f, 160f, 0.7f, 1f, LOLLIPOPS, 0.5f),
        // Night asphalt keeps pickups and striped hazards bright; cyan lane paint is drawn by Scenery.
        World(1, "Neon City", 250f, 258f, 0.8f, 0.42f, 265f, 0.8f, 0.10f, 222f, 0.42f, 0.13f, 0.085f, 310f, 0.95f, 1f, TOWERS, 0.7f),
        World(2, "Lava Caves", 10f, 6f, 0.9f, 0.5f, 350f, 0.95f, 0.08f, 12f, 0.9f, 0.42f, 0.26f, 28f, 1f, 1f, CRYSTALS, 0.7f),
        World(3, "Frost Peaks", 205f, 200f, 0.5f, 1f, 225f, 0.6f, 0.75f, 208f, 0.66f, 0.7f, 0.54f, 195f, 0.25f, 1f, SPIKES, 0.45f),
        World(4, "Sunset Dunes", 30f, 26f, 0.9f, 1f, 300f, 0.7f, 0.5f, 34f, 0.8f, 0.72f, 0.55f, 305f, 0.8f, 1f, CACTI, 0.55f),
        // Galaxy replaces the old Space map: a lifted nebula sky and transparent orbital road.
        World(5, "Galaxy", 230f, 238f, 0.9f, 0.34f, 255f, 0.95f, 0.14f, 232f, 0.85f, 0.34f, 0.20f, 185f, 1f, 1f, RINGS, 0.75f),
        World(6, "Railway Yard", 195f, 220f, 0.55f, 0.25f, 236f, 0.72f, 0.08f, 212f, 0.22f, 0.20f, 0.14f, 185f, 0.9f, 1f, RAILWAY, 0.7f),
        World(7, "Jungle Canopy", 106f, 136f, 0.72f, 0.42f, 154f, 0.78f, 0.12f, 111f, 0.55f, 0.24f, 0.15f, 98f, 0.82f, 0.95f, JUNGLE, 0.65f),
        World(8, "Highway Rush", 8f, 224f, 0.62f, 0.28f, 272f, 0.75f, 0.10f, 220f, 0.34f, 0.12f, 0.08f, 16f, 0.95f, 1f, HIGHWAY, 0.72f),
        World(9, "Tropical Beach", 187f, 203f, 0.62f, 1f, 190f, 0.72f, 0.92f, 39f, 0.57f, 0.72f, 0.62f, 37f, 0.84f, 0.96f, BEACH, 0.48f),
        // Near-black road and sky; warm lamp pools, reflective lane paint and animal eyes carry visibility.
        World(10, "Moonlit Wilds", 216f, 232f, 0.74f, 0.018f, 239f, 0.82f, 0.035f, 230f, 0.48f, 0.025f, 0.04f, 44f, 0.60f, 0.88f, NIGHT, 0.82f),
        // Matrix-inspired black-green sky and a bright code-green grid, without changing movement rules.
        World(11, "Matrix Circuit", 128f, 135f, 0.92f, 0.025f, 152f, 0.88f, 0.07f, 132f, 0.88f, 0.035f, 0.055f, 124f, 0.96f, 0.94f, HACKER, 0.78f),
        // Bright upper sky, cool cloud-road tiles, and white cloudbanks for a clear high-altitude route.
        World(12, "Air", 196f, 204f, 0.56f, 0.96f, 215f, 0.68f, 0.82f, 204f, 0.48f, 0.54f, 0.45f, 195f, 0.46f, 0.98f, AIR, 0.42f),
        // A colder, glitched Matrix branch: teal light, fractured magenta seams, and a near-black road.
        World(13, "Glitch Nexus", 184f, 196f, 0.86f, 0.028f, 268f, 0.82f, 0.085f, 188f, 0.88f, 0.032f, 0.052f, 172f, 0.96f, 0.98f, NEXUS, 0.80f),
    )

    fun get(id: Int): World = all.getOrElse(id) { all[0] }
}
