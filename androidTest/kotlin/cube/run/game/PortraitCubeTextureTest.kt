package cube.run.game

import androidx.test.core.app.ActivityScenario
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import cube.run.GameActivity
import cube.run.bot.LiveBotDriver
import cube.run.bot.value
import cube.run.core.Stage
import cube.run.data.Skins
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The portraits must be bound to UV-mapped cube meshes, not a blank front-only overlay. */
class PortraitCubeTextureTest {
    @Test fun eachPortraitUsesItsOwnPhotoTextureOnAFullUvMappedCube() {
        val ids = listOf(Skins.PORTRAIT_ONE_ID, Skins.PORTRAIT_TWO_ID, Skins.PORTRAIT_THREE_ID)
        val handles = ArrayList<Int>()
        try {
            ActivityScenario.launch(GameActivity::class.java).use { scenario ->
                scenario.onActivity { it.setShowWhenLocked(true); it.setTurnScreenOn(true) }
                LiveBotDriver.gl { game ->
                    val player: Player = value(game, "player")
                    for (id in ids) {
                        Stage.previewSkin = id
                        player.showcase(game.time, 0f)
                        assertEquals(id, player.skin.id)
                        val instance: ModelInstance = value(player, "inst")
                        val texture = instance.materials.first().get(TextureAttribute.Diffuse) as? TextureAttribute
                        assertNotNull("Portrait $id must bind a real diffuse photo texture", texture)
                        val image = texture!!.textureDescription.texture
                        assertEquals(384, image.width)
                        val mesh = instance.model.meshes.first()
                        assertNotNull("All six cube faces need UV coordinates", mesh.getVertexAttribute(Usage.TextureCoordinates))
                        assertTrue("The textured model must contain all six faces", mesh.numVertices >= 24)
                        handles.add(image.textureObjectHandle)
                    }
                    Stage.previewSkin = -1
                }
            }
            assertEquals("Each portrait skin should use its own supplied photo", 3, handles.toSet().size)
        } finally {
            Stage.previewSkin = -1
            Stage.mode = Stage.NONE
        }
    }
}
