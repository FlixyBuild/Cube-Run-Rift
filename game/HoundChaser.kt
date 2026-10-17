package cube.run.game

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import cube.run.core.Gdx3DGame
import cube.run.game.world.Fog
import kotlin.math.max
import kotlin.math.sin

/** Geometry only; [HoundEncounter] owns deterministic state, timing, and collision rules. */
internal class HoundChaser(private val game: Gdx3DGame) {
    val encounter = HoundEncounter()
    private val hide = Color(.012f, .016f, .024f, 1f)
    private val armor = Color(.075f, .085f, .11f, 1f)
    private val seam = Color(.78f, .025f, .035f, 1f)
    private val foot = Color(.035f, .04f, .055f, 1f)
    private val tooth = Color(.88f, .78f, .58f, 1f)
    private lateinit var eye: ModelInstance

    val alert: Int get() = encounter.alert
    val visible: Boolean get() = encounter.visible

    /** GL-thread construction; owned model resources are disposed by Gdx3DGame. */
    fun init() {
        if (::eye.isInitialized) return
        val eyeColor = Color(1f, .075f, .018f, 1f)
        val model = game.sphere(.34f, eyeColor, 16)
        model.materials.first().set(
            ColorAttribute.createDiffuse(eyeColor),
            ColorAttribute.createEmissive(1f, .035f, .006f, 1f),
        )
        eye = ModelInstance(model)
    }

    fun reset() = encounter.reset()

    /** Batched blockout body: four visible running legs and a single central facial eye. */
    fun renderBatched(time: Float) {
        if (!encounter.visible) return
        val x = encounter.x
        val z = encounter.z
        val fog = Fog.at(z)
        val lean = if (encounter.alert == 2) .11f else 0f
        val headY = 1.22f - lean
        game.worldBox(x, .78f, z, 1.02f, .48f, 1.34f, hide, fog)                     // long hound torso
        game.worldBox(x, .94f, z + .37f, .86f, .38f, .90f, armor, fog)              // shoulder mantle
        game.worldBox(x, headY, z + .63f, .72f, .62f, .62f, hide, fog)               // broad angular head
        val jawDrop = if (encounter.alert == 2) .075f + .018f * sin(time * 10f) else .02f
        game.worldBox(x, .97f - lean, z + .98f, .46f, .28f, .30f, foot, fog)         // single blunt muzzle
        game.worldBox(x, .79f - jawDrop - lean * .45f, z + 1.02f, .42f, .12f, .27f, armor, fog)
        game.worldBox(x - .23f, 1.49f - lean, z + .54f, .19f, .38f, .22f, armor, fog) // ears, not eye sockets
        game.worldBox(x + .23f, 1.49f - lean, z + .54f, .19f, .38f, .22f, armor, fog)
        // Two small teeth sit below the single central eye, inside the jaw line.
        game.worldBox(x - .105f, .865f - jawDrop - lean * .45f, z + 1.168f, .065f, .09f, .055f, tooth, fog)
        game.worldBox(x + .105f, .865f - jawDrop - lean * .45f, z + 1.168f, .065f, .09f, .055f, tooth, fog)
        game.worldBox(x, 1.005f, z + .30f, .73f, .075f, .86f, seam, fog)            // one chest/armor seam
        // Bright angular flank fissures read as energy cracks, not extra facial features.
        game.worldBoxSpin(x - .505f, .77f, z - .10f, .035f, .045f, .42f, -22f, seam, fog)
        game.worldBoxSpin(x + .505f, .82f, z + .05f, .035f, .045f, .40f, 19f, seam, fog)
        game.worldBoxSpin(x - .28f, 1.025f, z - .63f, .30f, .035f, .04f, -24f, seam, fog)

        // Four stride offsets; no per-frame objects or growing entity list.
        val phase = time * 12f
        for (leg in 0..3) {
            val side = if (leg % 2 == 0) -1f else 1f
            val front = leg < 2
            val stride = sin(phase + if (front) 0f else 3.14159f) * if (side < 0f) 1f else -1f
            val lift = max(0f, stride) * .14f
            val lx = x + side * .34f
            val lz = z + if (front) .43f else -.44f
            game.worldBox(lx, .28f + lift * .5f, lz + stride * .16f,
                .22f, .48f - lift, .25f, hide, fog)
            game.worldBox(lx, .075f + lift, lz + stride * .16f + .025f,
                .30f, .15f, .34f, foot, fog)
        }
        game.worldBoxSpin(x, .90f, z - .76f, .19f, .20f, .72f, 18f * sin(time * 5f), armor, fog)
    }

    /** The one emissive eye faces +Z, toward the chase camera. */
    fun renderEye(batch: ModelBatch, env: Environment) {
        if (!encounter.visible || !::eye.isInitialized) return
        eye.transform.setToTranslation(
            encounter.x,
            1.23f - if (encounter.alert == 2) .11f else 0f,
            encounter.z + 1.005f,
        ).scale(1.18f, 1.18f, .72f)
        batch.render(eye, env)
    }
}
