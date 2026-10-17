package cube.run.game.track

import cube.run.data.Worlds

/** Visual archetypes use the same authored collision boxes; they only change what the runner sees. */
enum class ObstacleArt {
    DEFAULT,
    JUNGLE_THORN_BUSH, JUNGLE_FALLEN_LOG, JUNGLE_VINE_GATE, JUNGLE_VINE_SWEEP,
    JUNGLE_SNAP_PLANT, JUNGLE_FALLING_POD, JUNGLE_VINE_PENDULUM, JUNGLE_ROOT_BRIDGE,
    JUNGLE_MUSHROOM, JUNGLE_SWAMP, JUNGLE_DINO,
    LAVA_BOULDER, LAVA_VENT, LAVA_CRUST, LAVA_BRIDGE, LAVA_PAD,
    NEXUS_GLITCH_BLOCK, NEXUS_FIREWALL, NEXUS_DRONE, NEXUS_VOID, NEXUS_PLATFORM, NEXUS_PAD,
    SPACE_ASTEROID, SPACE_METEOR, SPACE_GRAVITY_MINE, SPACE_CRYSTAL_WALL, SPACE_ENERGY_WALL, SPACE_LASER,
    SPACE_SOLAR_ARRAY, SPACE_PLASMA_SPIRE, SPACE_DROP_POD, SPACE_ORBITAL_DRONE,
    SPACE_STATION_HULL, SPACE_STATION_PANEL, SPACE_GRAVITY_RING, SPACE_RIFT,
    CITY_CAR, CITY_BUS, CITY_BIKE, CITY_BARRIER, CITY_SIGN, CITY_BOLLARD, CITY_PIT, CITY_ROOFTOP, CITY_PAD,
    WINTER_SNOWBALL, WINTER_SNOWBANK, WINTER_ICICLE, WINTER_ICE_SPIKE, WINTER_ICE_HOLE, WINTER_ICE_BRIDGE, WINTER_SPRING,
    NIGHT_WOLF, NIGHT_TIGER, NIGHT_SHADOW_GAP,
    HACKER_GLITCH_BLOCK, HACKER_FIREWALL, HACKER_DRONE, HACKER_DATA_GATE, HACKER_VOID, HACKER_PLATFORM, HACKER_PAD,
    AIR_EAGLE, AIR_HAWK, AIR_GULL, AIR_FLOCK, AIR_CLOUD_GAP, AIR_CLOUD_BRIDGE, AIR_THERMAL_PAD,
    BEACH_CRAB, BEACH_SURF, BEACH_GEYSER, BEACH_DRIFTWOOD, BEACH_TIDAL_GAP, BEACH_BOARDWALK, BEACH_SHELL_PAD, BEACH_SHELL,
}

/** Pure, deterministic mapping that keeps biome art choices testable without a GL context. */
object ObstacleArtResolver {
    fun resolve(
        worldStyle: Int,
        type: Int,
        animation: Int,
        cue: ObCue,
        pit: Boolean,
        width: Float,
        height: Float,
        animal: RoadAnimal = RoadAnimal.NONE,
        bird: AirBird = AirBird.NONE,
    ): ObstacleArt {
        if (worldStyle == Worlds.AIR) return when {
            bird == AirBird.EAGLE -> ObstacleArt.AIR_EAGLE
            bird == AirBird.HAWK -> ObstacleArt.AIR_HAWK
            bird == AirBird.GULL -> ObstacleArt.AIR_GULL
            bird == AirBird.FLOCK -> ObstacleArt.AIR_FLOCK
            pit -> ObstacleArt.AIR_CLOUD_GAP
            type == ObType.PLAT -> ObstacleArt.AIR_CLOUD_BRIDGE
            type == ObType.PAD -> ObstacleArt.AIR_THERMAL_PAD
            else -> ObstacleArt.DEFAULT
        }
        if (worldStyle == Worlds.NIGHT) return when {
            animal == RoadAnimal.WOLF -> ObstacleArt.NIGHT_WOLF
            animal == RoadAnimal.TIGER -> ObstacleArt.NIGHT_TIGER
            pit -> ObstacleArt.NIGHT_SHADOW_GAP
            else -> ObstacleArt.NIGHT_WOLF
        }
        if (worldStyle == Worlds.NEXUS) return when {
            pit -> ObstacleArt.NEXUS_VOID
            type == ObType.PLAT -> ObstacleArt.NEXUS_PLATFORM
            type == ObType.PAD -> ObstacleArt.NEXUS_PAD
            animation == ObAnim.PENDULUM -> ObstacleArt.NEXUS_DRONE
            animation == ObAnim.SWEEP || cue == ObCue.DUCK -> ObstacleArt.NEXUS_FIREWALL
            else -> ObstacleArt.NEXUS_GLITCH_BLOCK
        }
        if (worldStyle == Worlds.HACKER) return when {
            pit -> ObstacleArt.HACKER_VOID
            type == ObType.PLAT -> ObstacleArt.HACKER_PLATFORM
            type == ObType.PAD -> ObstacleArt.HACKER_PAD
            animation == ObAnim.PENDULUM -> ObstacleArt.HACKER_DRONE
            animation == ObAnim.SWEEP || cue == ObCue.DUCK -> ObstacleArt.HACKER_DATA_GATE
            animation == ObAnim.STOMP || cue == ObCue.JUMP && width > 3f -> ObstacleArt.HACKER_FIREWALL
            else -> ObstacleArt.HACKER_GLITCH_BLOCK
        }
        if (worldStyle == Worlds.JUNGLE) return when {
            animal == RoadAnimal.DINOSAUR -> ObstacleArt.JUNGLE_DINO
            pit -> ObstacleArt.JUNGLE_SWAMP
            type == ObType.PLAT -> ObstacleArt.JUNGLE_ROOT_BRIDGE
            type == ObType.PAD -> ObstacleArt.JUNGLE_MUSHROOM
            animation == ObAnim.PENDULUM -> ObstacleArt.JUNGLE_VINE_PENDULUM
            animation == ObAnim.PISTON -> ObstacleArt.JUNGLE_SNAP_PLANT
            animation == ObAnim.SWEEP -> ObstacleArt.JUNGLE_VINE_SWEEP
            animation == ObAnim.STOMP -> ObstacleArt.JUNGLE_FALLING_POD
            cue == ObCue.JUMP && width > 3f -> ObstacleArt.JUNGLE_FALLEN_LOG
            cue == ObCue.DUCK -> ObstacleArt.JUNGLE_VINE_GATE
            else -> ObstacleArt.JUNGLE_THORN_BUSH
        }
        if (worldStyle == Worlds.CRYSTALS) return when {
            pit -> ObstacleArt.LAVA_CRUST
            type == ObType.PLAT -> ObstacleArt.LAVA_BRIDGE
            type == ObType.PAD -> ObstacleArt.LAVA_PAD
            animation == ObAnim.PISTON || animation == ObAnim.STOMP -> ObstacleArt.LAVA_VENT
            animation == ObAnim.PENDULUM || animation == ObAnim.SWEEP -> ObstacleArt.LAVA_BOULDER
            else -> ObstacleArt.LAVA_CRUST
        }
        if (worldStyle == Worlds.RINGS) return when {
            cue == ObCue.SPACE_MINE -> ObstacleArt.SPACE_GRAVITY_MINE
            pit -> ObstacleArt.SPACE_RIFT
            type == ObType.PAD -> ObstacleArt.SPACE_GRAVITY_RING
            type == ObType.PLAT -> ObstacleArt.SPACE_STATION_PANEL
            animation == ObAnim.PENDULUM -> ObstacleArt.SPACE_ORBITAL_DRONE
            animation == ObAnim.PISTON -> ObstacleArt.SPACE_PLASMA_SPIRE
            animation == ObAnim.SWEEP -> ObstacleArt.SPACE_SOLAR_ARRAY
            animation == ObAnim.STOMP -> ObstacleArt.SPACE_DROP_POD
            cue == ObCue.JUMP && width > 3f -> ObstacleArt.SPACE_ENERGY_WALL
            cue == ObCue.JUMP && height < 1.4f -> ObstacleArt.SPACE_METEOR
            cue == ObCue.JUMP -> ObstacleArt.SPACE_CRYSTAL_WALL
            cue == ObCue.DUCK -> ObstacleArt.SPACE_LASER
            height < 1.35f -> ObstacleArt.SPACE_METEOR
            width > 3f -> ObstacleArt.SPACE_STATION_HULL
            else -> ObstacleArt.SPACE_ASTEROID
        }
        if (worldStyle == Worlds.TOWERS) return when {
            pit -> ObstacleArt.CITY_PIT
            type == ObType.PLAT -> ObstacleArt.CITY_ROOFTOP
            type == ObType.PAD -> ObstacleArt.CITY_PAD
            animation == ObAnim.PENDULUM || animation == ObAnim.SWEEP -> ObstacleArt.CITY_BIKE
            animation == ObAnim.PISTON -> ObstacleArt.CITY_BOLLARD
            animation == ObAnim.STOMP -> ObstacleArt.CITY_BUS
            cue == ObCue.JUMP && width > 3f -> ObstacleArt.CITY_BARRIER
            cue == ObCue.DUCK -> ObstacleArt.CITY_SIGN
            width > 3f -> ObstacleArt.CITY_BUS
            else -> ObstacleArt.CITY_CAR
        }
        if (worldStyle == Worlds.BEACH) return when {
            pit -> ObstacleArt.BEACH_TIDAL_GAP
            type == ObType.PLAT -> ObstacleArt.BEACH_BOARDWALK
            type == ObType.PAD -> ObstacleArt.BEACH_SHELL_PAD
            animation == ObAnim.PENDULUM || animation == ObAnim.SWEEP -> ObstacleArt.BEACH_CRAB
            animation == ObAnim.PISTON -> ObstacleArt.BEACH_GEYSER
            animation == ObAnim.STOMP -> ObstacleArt.BEACH_SURF
            cue == ObCue.JUMP && width > 3f -> ObstacleArt.BEACH_DRIFTWOOD
            cue == ObCue.DUCK -> ObstacleArt.BEACH_SHELL
            else -> ObstacleArt.BEACH_SHELL
        }
        if (worldStyle == Worlds.SPIKES) return when {
            pit -> ObstacleArt.WINTER_ICE_HOLE
            type == ObType.PLAT -> ObstacleArt.WINTER_ICE_BRIDGE
            type == ObType.PAD -> ObstacleArt.WINTER_SPRING
            animation == ObAnim.PENDULUM || animation == ObAnim.SWEEP -> ObstacleArt.WINTER_SNOWBALL
            animation == ObAnim.PISTON -> ObstacleArt.WINTER_ICE_SPIKE
            animation == ObAnim.STOMP || cue == ObCue.DUCK -> ObstacleArt.WINTER_ICICLE
            cue == ObCue.JUMP && width > 3f -> ObstacleArt.WINTER_SNOWBANK
            width > 3f -> ObstacleArt.WINTER_SNOWBANK
            else -> ObstacleArt.WINTER_SNOWBALL
        }
        return ObstacleArt.DEFAULT
    }
}
