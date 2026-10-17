package cube.run.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SkinCatalogueTest {
    @Test fun sixOriginalStartersRemainFreeAndNewAffordableHomagesArePaidCosmetics() {
        val starters = Skins.all.take(Skins.STARTER_COUNT)
        assertEquals((0 until Skins.STARTER_COUNT).toList(), starters.map { it.id })
        assertTrue(starters.all { it.price == 0 })

        val added = listOf(Skins.DEADPOOL_ID, Skins.SPIDER_MAN_ID, Skins.IRON_SENTINEL_ID, Skins.FROST_BYTE_ID)
            .map(Skins::get)
        assertEquals(listOf("Deadpool", "Spider-Man", "Iron Sentinel", "Frost Byte"), added.map { it.name })
        assertEquals(listOf(150, 180, 350, 100), added.map { it.price })
        assertTrue(added.all { it.price > 0 && it.abilities.isEmpty() })
        assertEquals(listOf(Skins.ART_DEADPOOL, Skins.ART_SPIDER_MAN, Skins.ART_IRON_SENTINEL, Skins.ART_FROST_BYTE), added.map { it.art })
        assertTrue(added.all { it.id < 31 }) // owned cosmetics use a persisted Int bit mask
        assertFalse(added.any { it.id in listOf(Skins.MINT_ID, Skins.COAL_ID, Skins.VOID_ID, Skins.ZEN_ID) })

        val bassline = Skins.get(Skins.BASSLINE_ID)
        assertEquals("Bassline", bassline.name)
        assertEquals(320, bassline.price)
        assertTrue(bassline.abilities.isEmpty())
        assertTrue("Owned cosmetics use a persisted Int bit mask", bassline.id < 31)

        val newWorldSkins = listOf(Skins.get(Skins.CODEBREAKER_ID), Skins.get(Skins.RIFT_RANGER_ID))
        assertEquals(listOf("Codebreaker", "Rift Ranger"), newWorldSkins.map { it.name })
        assertEquals(listOf(Skins.ART_HACKER, Skins.ART_RIFT_RANGER), newWorldSkins.map { it.art })
        assertEquals(listOf(420, 650), newWorldSkins.map { it.price })
        assertTrue(newWorldSkins.all { it.abilities.isEmpty() && it.id < 31 })
        assertTrue(Wardrobe.shopItems(Wardrobe.CUBE).containsAll(newWorldSkins.map { it.id }))

        val newLaunchSkins = listOf(Skins.get(Skins.COMET_CORE_ID), Skins.get(Skins.SIGNAL_SHIFT_ID))
        assertEquals(listOf("Comet Core", "Signal Shift"), newLaunchSkins.map { it.name })
        assertEquals(listOf(240, 280), newLaunchSkins.map { it.price })
        assertTrue(newLaunchSkins.all { it.abilities.isEmpty() && it.id < 31 })
        assertTrue(Wardrobe.shopItems(Wardrobe.CUBE).containsAll(listOf(Skins.COMET_CORE_ID, Skins.SIGNAL_SHIFT_ID)))
    }

    @Test fun theThreePortraitSkinsStayCubesAndTheAirSkinIsFreeAndSelectable() {
        val portraits = listOf(
            Skins.get(Skins.PORTRAIT_ONE_ID), Skins.get(Skins.PORTRAIT_TWO_ID), Skins.get(Skins.PORTRAIT_THREE_ID),
        )
        assertEquals(listOf("Portrait One", "Portrait Two", "Portrait Three"), portraits.map { it.name })
        assertEquals(listOf(Skins.ART_PORTRAIT_ONE, Skins.ART_PORTRAIT_TWO, Skins.ART_PORTRAIT_THREE), portraits.map { it.art })
        assertTrue(portraits.all { it.price == 0 && it.abilities.isEmpty() })
        val air = Skins.get(Skins.SKY_SOARER_ID)
        assertEquals("Sky Soarer", air.name)
        assertEquals(Skins.ART_SKY_SOARER, air.art)
        assertEquals(0, air.price)
        assertTrue(air.abilities.isEmpty())
        assertTrue(Wardrobe.shopItems(Wardrobe.CUBE).containsAll(portraits.map { it.id } + air.id))
        assertTrue(listOf(Skins.PORTRAIT_ONE_ID, Skins.PORTRAIT_TWO_ID, Skins.PORTRAIT_THREE_ID, Skins.SKY_SOARER_ID)
            .all { (Skins.requestedFreeSkinMask and (1 shl it)) != 0 })
    }

    @Test fun legacyIdsDoNotAccidentallyResolveToNewPaidSkins() {
        assertEquals(Skins.EVENT_HORIZON_ID, Skins.get(Skins.MINT_ID).id)
        assertTrue(Wardrobe.shopItems(Wardrobe.CUBE).containsAll(listOf(
            Skins.DEADPOOL_ID, Skins.SPIDER_MAN_ID, Skins.IRON_SENTINEL_ID, Skins.FROST_BYTE_ID, Skins.BASSLINE_ID,
            Skins.CODEBREAKER_ID, Skins.RIFT_RANGER_ID,
            Skins.PORTRAIT_ONE_ID, Skins.PORTRAIT_TWO_ID, Skins.PORTRAIT_THREE_ID, Skins.SKY_SOARER_ID,
        )))
        assertEquals(Skins.all.size, Skins.all.map { it.id }.toSet().size)
        assertTrue(Skins.all.all { it.id in 0..30 })
        Skins.all.forEach { assertTrue(Skins.get(it.id) === it) }
    }
}
