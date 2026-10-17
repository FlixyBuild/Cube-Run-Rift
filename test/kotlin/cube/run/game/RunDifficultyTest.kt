package cube.run.game

import cube.run.data.RunDifficulty
import cube.run.data.Worlds
import cube.run.game.space.SpaceSections
import cube.run.game.space.SpaceSpacing
import cube.run.game.track.ObAnim
import cube.run.game.track.ObCue
import cube.run.game.track.ObType
import cube.run.game.track.ObstacleArt
import cube.run.game.track.ObstacleArtResolver
import cube.run.game.track.Step
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RunDifficultyTest {
    @Test fun presetsGiveDistinctPaceAndRampCeilings() {
        val easy = Difficulty(RunDifficulty.EASY)
        val medium = Difficulty(RunDifficulty.MEDIUM)
        val hard = Difficulty(RunDifficulty.HARD)
        assertTrue(easy.speed() < medium.speed())
        assertTrue(medium.speed() < hard.speed())
        assertEquals(0, easy.tier())
        assertEquals(0, medium.tier())
        assertEquals(1, hard.tier())

        easy.ramp(RunDifficulty.EASY.rampSeconds)
        assertEquals(RunDifficulty.EASY.autoCeiling, easy.diff, .0001f)
        assertEquals(2, easy.tier())
        hard.ramp(RunDifficulty.HARD.rampSeconds)
        assertEquals(1f, hard.diff, .0001f)
        assertEquals(3, hard.tier())
    }

    @Test fun allThreeModesSelectDifferentSpaceSectionFloors() {
        val easy = SpaceSections.open(0, -1, RunDifficulty.EASY.spaceMinimumTier)
        val medium = SpaceSections.open(0, -1, RunDifficulty.MEDIUM.spaceMinimumTier)
        val hard = SpaceSections.open(1, -1, RunDifficulty.HARD.spaceMinimumTier)
        assertEquals(setOf(0), easy.map { it.tier }.toSet())
        assertEquals(setOf(0, 1), medium.map { it.tier }.toSet())
        assertEquals(setOf(0, 1, 2), hard.map { it.tier }.toSet())
    }

    @Test fun rowSpacingBreathesOnEasyAndTightensOnHardInSpaceToo() {
        fun gap(scale: Float) = SpaceSpacing.gap(
            Step.dg(0), Step.dg(1), speed = 12f, dodgeGap = 6.5f, breatherGap = 12f, spacingScale = scale,
        )
        assertTrue(gap(RunDifficulty.EASY.spacingScale) > gap(RunDifficulty.MEDIUM.spacingScale))
        assertTrue(gap(RunDifficulty.MEDIUM.spacingScale) > gap(RunDifficulty.HARD.spacingScale))
        // The full rift stays continuous and authored jump recovery is never shortened on Hard.
        assertEquals(SpaceSpacing.RIFT_SLICE, SpaceSpacing.gap(Step.RF, Step.RF, 12f, 6.5f, 12f, .92f), .0001f)
        val jumpEasy = SpaceSpacing.gap(Step.dg(0), Step.JP, 12f, 6.5f, 12f, 1.15f)
        val jumpHard = SpaceSpacing.gap(Step.dg(0), Step.JP, 12f, 6.5f, 12f, .92f)
        assertTrue(jumpEasy > jumpHard)
    }

    @Test fun campaignObstacleArtMapsToReadableBiomeSilhouettes() {
        assertEquals(ObstacleArt.JUNGLE_THORN_BUSH,
            ObstacleArtResolver.resolve(Worlds.JUNGLE, ObType.SOLID, ObAnim.NONE, ObCue.NONE, false, 1.5f, 2f))
        assertEquals(ObstacleArt.JUNGLE_FALLEN_LOG,
            ObstacleArtResolver.resolve(Worlds.JUNGLE, ObType.SOLID, ObAnim.NONE, ObCue.JUMP, false, 6.9f, .62f))
        assertEquals(ObstacleArt.JUNGLE_MUSHROOM,
            ObstacleArtResolver.resolve(Worlds.JUNGLE, ObType.PAD, ObAnim.NONE, ObCue.NONE, false, 1.4f, .14f))
        assertEquals(ObstacleArt.SPACE_METEOR,
            ObstacleArtResolver.resolve(Worlds.RINGS, ObType.SOLID, ObAnim.NONE, ObCue.JUMP, false, 1.4f, 1.05f))
        assertEquals(ObstacleArt.SPACE_GRAVITY_RING,
            ObstacleArtResolver.resolve(Worlds.RINGS, ObType.PAD, ObAnim.NONE, ObCue.NONE, false, 1.4f, .14f))
        assertEquals(ObstacleArt.SPACE_LASER,
            ObstacleArtResolver.resolve(Worlds.RINGS, ObType.SOLID, ObAnim.NONE, ObCue.DUCK, false, 6.9f, .72f))
    }
}
