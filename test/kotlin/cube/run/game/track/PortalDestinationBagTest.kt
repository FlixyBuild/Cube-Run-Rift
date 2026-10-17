package cube.run.game.track

import cube.run.data.Bonus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PortalDestinationBagTest {
    @Test fun eachShuffledCycleUsesEveryDestinationBeforeRepeating() {
        val pool = listOf(Bonus.WIDE, Bonus.HILLS, Bonus.FLOAT, Bonus.KALEIDO, Bonus.SPACE, Bonus.NEXUS)
        val bag = PortalDestinationBag(Random(817))

        val first = List(pool.size) { bag.next(pool)!! }
        assertEquals(pool.size, first.toSet().size)
        assertEquals(pool.toSet(), first.toSet())
        val second = List(pool.size) { bag.next(pool)!! }
        assertEquals(pool.size, second.toSet().size)
        assertEquals(pool.toSet(), second.toSet())
        assertNotEquals("The cycle boundary should also avoid an immediate repeat", first.last(), second.first())
    }

    @Test fun duplicatePoolIdsAreCollapsedAndAnEmptyPoolIsSafe() {
        val bag = PortalDestinationBag(Random(19))
        val pool = listOf(Bonus.SPACE, Bonus.WIDE, Bonus.SPACE, Bonus.WIDE)
        val first = bag.next(pool)!!
        val second = bag.next(pool)!!
        assertTrue(first in setOf(Bonus.SPACE, Bonus.WIDE))
        assertTrue(second in setOf(Bonus.SPACE, Bonus.WIDE))
        assertNotEquals(first, second)
        assertNull(bag.next(emptyList()))
    }

    @Test fun changingTheUnlockedPoolRebuildsTheBagWithOnlyNewlyAvailableOptions() {
        val bag = PortalDestinationBag(Random(41))
        bag.next(listOf(Bonus.SPACE, Bonus.WIDE))
        val expanded = listOf(Bonus.SPACE, Bonus.WIDE, Bonus.HILLS)
        val cycle = List(expanded.size) { bag.next(expanded)!! }
        assertEquals(expanded.toSet(), cycle.toSet())
        assertFalse(cycle.any { it !in expanded })
    }
}
