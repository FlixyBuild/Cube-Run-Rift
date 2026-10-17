package cube.run.game

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import cube.run.core.Gdx3DGame
import cube.run.data.Skins
import kotlin.math.sin

/** Procedural, GL-owned cosmetics attached to the same unit-cube transform as the player. */
internal class PlayerSkinArt(game: Gdx3DGame) {
    private val eyeModel = game.sphere(.46f, EYE, 18).apply {
        materials.first().set(ColorAttribute.createDiffuse(EYE), ColorAttribute.createEmissive(1f, .12f, .015f, 1f))
    }
    private val eyes = Array(2) { ModelInstance(eyeModel) }
    private val dangerEyeModel = game.sphere(.47f, DANGER_EYE, 18).apply {
        materials.first().set(ColorAttribute.createDiffuse(DANGER_EYE), ColorAttribute.createEmissive(1f, .025f, .006f, 1f))
    }
    private val dangerEyes = Array(2) { ModelInstance(dangerEyeModel) }

    // Classic face details are restrained etched marks; raised cube rails remain the bevels.
    private val scratchModel = game.box(.20f, .018f, .014f, Color(.25f, .28f, .31f, 1f))
    private val scratches = Array(2) { ModelInstance(scratchModel) }

    // Split the Danger face into an ink-black half and a deep-red half without changing its hitbox.
    private val dangerPanelModel = game.box(.49f, .98f, .055f, Color(.012f, .014f, .021f, 1f))
    private val dangerPanel = ModelInstance(dangerPanelModel)
    private val dangerScarModel = game.box(1f, 1f, 1f, Color(.78f, .025f, .018f, 1f)).apply {
        materials.first().set(ColorAttribute.createEmissive(.28f, .012f, .006f, 1f))
    }
    private val dangerScars = Array(2) { ModelInstance(dangerScarModel) }
    private val dangerBrowModel = game.box(.27f, .055f, .035f, Color(.018f, .008f, .012f, 1f))
    private val dangerBrows = Array(2) { ModelInstance(dangerBrowModel) }

    private val dinoSpineModel = game.cone(.18f, .24f, Color(.55f, .92f, .27f, 1f), 8)
    private val dinoSpines = Array(3) { ModelInstance(dinoSpineModel) }
    private val dinoScaleModel = game.box(.10f, .085f, .025f, Color(.73f, .98f, .31f, 1f))
    private val dinoScales = Array(4) { ModelInstance(dinoScaleModel) }

    // Tesseract: a cyan voxel frame with an inner cross and luminous corner nodes.
    private val tesseractBarModel = game.box(.72f, .052f, .032f, Color(.12f, .95f, 1f, 1f)).apply {
        materials.first().set(ColorAttribute.createDiffuse(Color(.12f, .95f, 1f, 1f)), ColorAttribute.createEmissive(.05f, .56f, .82f, 1f))
    }
    private val tesseractBars = Array(8) { ModelInstance(tesseractBarModel) }
    private val tesseractNodeModel = game.box(.12f, .12f, .055f, Color(.8f, .98f, 1f, 1f)).apply {
        materials.first().set(ColorAttribute.createDiffuse(Color(.8f, .98f, 1f, 1f)), ColorAttribute.createEmissive(.42f, .8f, 1f, 1f))
    }
    private val tesseractNodes = Array(4) { ModelInstance(tesseractNodeModel) }

    // Aurora: three original flowing light bands, held by a second pair of cool-glow eyes.
    private val auroraBandModels = arrayOf(
        game.box(.68f, .075f, .035f, Color(.08f, .95f, .56f, 1f)).apply { materials.first().set(ColorAttribute.createEmissive(.02f, .35f, .16f, 1f)) },
        game.box(.75f, .075f, .035f, Color(.04f, .72f, 1f, 1f)).apply { materials.first().set(ColorAttribute.createEmissive(.01f, .25f, .48f, 1f)) },
        game.box(.62f, .075f, .035f, Color(.63f, .24f, 1f, 1f)).apply { materials.first().set(ColorAttribute.createEmissive(.22f, .04f, .40f, 1f)) },
    )
    private val auroraBands = Array(3) { ModelInstance(auroraBandModels[it]) }
    private val auroraEyeModel = game.sphere(.34f, Color(.82f, 1f, 1f, 1f), 18).apply {
        materials.first().set(ColorAttribute.createDiffuse(Color(.82f, 1f, 1f, 1f)), ColorAttribute.createEmissive(.15f, .65f, 1f, 1f))
    }
    private val auroraEyes = Array(2) { ModelInstance(auroraEyeModel) }

    // Original comic-style mask, white eye patches, cape and split-comet mark; no borrowed logo or art.
    private val renegadeBlackHalfModel = game.box(.49f, .98f, .055f, Color(.012f, .014f, .022f, 1f))
    private val renegadeBlackHalf = ModelInstance(renegadeBlackHalfModel)
    private val maskModel = game.box(.72f, .30f, .055f, Color(.012f, .014f, .022f, 1f))
    private val mask = ModelInstance(maskModel)
    private val whitePatchModel = game.sphere(.44f, Color(.98f, .96f, .88f, 1f), 18)
    private val whitePatches = Array(2) { ModelInstance(whitePatchModel) }
    private val renegadeEyeModel = game.sphere(.29f, Color(1f, .20f, .055f, 1f), 18).apply {
        materials.first().set(
            ColorAttribute.createDiffuse(Color(1f, .20f, .055f, 1f)),
            ColorAttribute.createEmissive(1f, .075f, .012f, 1f),
        )
    }
    private val renegadeEyes = Array(2) { ModelInstance(renegadeEyeModel) }
    private val capeModel = game.box(.58f, .68f, .06f, Color(.48f, .025f, .095f, 1f))
    private val cape = ModelInstance(capeModel)
    private val emblemModel = game.box(1f, 1f, 1f, Color(1f, .34f, .055f, 1f)).apply {
        materials.first().set(ColorAttribute.createEmissive(.36f, .055f, .008f, 1f))
    }
    private val emblemA = ModelInstance(emblemModel)
    private val emblemB = ModelInstance(emblemModel)

    // Deadpool-inspired crimson mask, ivory eye patches, utility belt and crossed straps.
    private val deadpoolMaskModel = game.box(.96f, .48f, .06f, Color(.016f, .018f, .026f, 1f))
    private val deadpoolMask = ModelInstance(deadpoolMaskModel)
    private val deadpoolPatches = Array(2) { ModelInstance(whitePatchModel) }
    private val deadpoolStrapModel = game.box(.78f, .065f, .035f, Color(.075f, .045f, .055f, 1f))
    private val deadpoolStraps = Array(2) { ModelInstance(deadpoolStrapModel) }
    private val deadpoolBeltModel = game.box(.90f, .11f, .06f, Color(.09f, .055f, .06f, 1f))
    private val deadpoolBelt = ModelInstance(deadpoolBeltModel)
    private val deadpoolBuckleModel = game.box(.15f, .13f, .04f, Color(.72f, .70f, .62f, 1f))
    private val deadpoolBuckle = ModelInstance(deadpoolBuckleModel)

    // Spider-Man-inspired red mask, cool-blue suit panels, bright eyes and an original web grid.
    private val spiderBluePanelModel = game.box(.22f, .96f, .05f, Color(.025f, .12f, .43f, 1f))
    private val spiderBluePanels = Array(2) { ModelInstance(spiderBluePanelModel) }
    private val spiderBlueBandModel = game.box(.92f, .17f, .055f, Color(.025f, .12f, .43f, 1f))
    private val spiderBlueBand = ModelInstance(spiderBlueBandModel)
    private val spiderRimModel = game.sphere(.45f, Color(.018f, .025f, .045f, 1f), 18)
    private val spiderRims = Array(2) { ModelInstance(spiderRimModel) }
    private val spiderEyeModel = game.sphere(.39f, Color(.96f, .98f, 1f, 1f), 18)
    private val spiderEyes = Array(2) { ModelInstance(spiderEyeModel) }
    private val spiderWebModel = game.box(1f, .018f, .025f, Color(.22f, .018f, .035f, 1f))
    private val spiderWebBars = Array(9) { ModelInstance(spiderWebModel) }

    // Iron Sentinel: a dark glass visor, twin cyan optics and a small reactor jewel.
    private val sentinelVisorModel = game.box(.84f, .32f, .07f, Color(.025f, .075f, .12f, 1f))
    private val sentinelVisor = ModelInstance(sentinelVisorModel)
    private val sentinelEyeModel = game.sphere(.28f, Color(.24f, .94f, 1f, 1f), 18).apply {
        materials.first().set(ColorAttribute.createDiffuse(Color(.24f, .94f, 1f, 1f)), ColorAttribute.createEmissive(.08f, .64f, 1f, 1f))
    }
    private val sentinelEyes = Array(2) { ModelInstance(sentinelEyeModel) }
    private val sentinelCoreModel = game.box(.20f, .20f, .06f, Color(.9f, .98f, 1f, 1f)).apply {
        materials.first().set(ColorAttribute.createDiffuse(Color(.9f, .98f, 1f, 1f)), ColorAttribute.createEmissive(.24f, .72f, 1f, 1f))
    }
    private val sentinelCore = ModelInstance(sentinelCoreModel)
    private val sentinelTrimModel = game.box(.92f, .055f, .035f, Color(.98f, .58f, .08f, 1f))
    private val sentinelTrim = Array(2) { ModelInstance(sentinelTrimModel) }

    // Frost Byte: a crisp snowflake insignia and three small ice crystals on the upper edge.
    private val frostCrystalModel = game.cone(.16f, .30f, Color(.72f, .98f, 1f, 1f), 7).apply {
        materials.first().set(ColorAttribute.createDiffuse(Color(.72f, .98f, 1f, 1f)), ColorAttribute.createEmissive(.12f, .4f, .58f, 1f))
    }
    private val frostCrystals = Array(3) { ModelInstance(frostCrystalModel) }
    private val frostBarModel = game.box(.54f, .045f, .04f, Color(.79f, .99f, 1f, 1f)).apply {
        materials.first().set(ColorAttribute.createDiffuse(Color(.79f, .99f, 1f, 1f)), ColorAttribute.createEmissive(.14f, .52f, .72f, 1f))
    }
    private val frostBars = Array(3) { ModelInstance(frostBarModel) }
    private val frostNodeModel = game.box(.11f, .11f, .05f, Color(.98f, 1f, 1f, 1f)).apply {
        materials.first().set(ColorAttribute.createDiffuse(Color(.98f, 1f, 1f, 1f)), ColorAttribute.createEmissive(.34f, .74f, 1f, 1f))
    }
    private val frostNodes = Array(4) { ModelInstance(frostNodeModel) }

    // Codebreaker hacker cube: a black visor, luminous status pixels and a small antenna.
    private val hackerDark = Color(.012f, .035f, .025f, 1f)
    private val hackerGreen = Color(.12f, 1f, .38f, 1f)
    private val hackerMint = Color(.62f, 1f, .78f, 1f)
    private val hackerPink = Color(.96f, .12f, .68f, 1f)
    private val hackerVisorModel = game.box(.90f, .25f, .065f, hackerDark)
    private val hackerVisor = ModelInstance(hackerVisorModel)
    private val hackerEyeModel = game.box(.18f, .055f, .035f, hackerMint).apply {
        materials.first().set(ColorAttribute.createDiffuse(hackerMint), ColorAttribute.createEmissive(.25f, 1f, .48f, 1f))
    }
    private val hackerEyes = Array(2) { ModelInstance(hackerEyeModel) }
    private val hackerPixelModel = game.box(.075f, .10f, .035f, hackerGreen).apply {
        materials.first().set(ColorAttribute.createDiffuse(hackerGreen), ColorAttribute.createEmissive(.04f, .52f, .14f, 1f))
    }
    private val hackerPixels = Array(6) { ModelInstance(hackerPixelModel) }
    private val hackerAntennaModel = game.box(.065f, .22f, .065f, hackerGreen).apply {
        materials.first().set(ColorAttribute.createDiffuse(hackerGreen), ColorAttribute.createEmissive(.04f, .55f, .12f, 1f))
    }
    private val hackerAntenna = ModelInstance(hackerAntennaModel)
    private val hackerAntennaTipModel = game.box(.13f, .10f, .13f, hackerPink).apply {
        materials.first().set(ColorAttribute.createDiffuse(hackerPink), ColorAttribute.createEmissive(.40f, .02f, .24f, 1f))
    }
    private val hackerAntennaTip = ModelInstance(hackerAntennaTipModel)

    // An original block hero. Separate shoulders, arms, hands, legs, knees and boots stay attached to the cube pose.
    private val rangerBlue = Color(.055f, .24f, .66f, 1f)
    private val rangerCyan = Color(.10f, .82f, .96f, 1f)
    private val rangerGlove = Color(.98f, .34f, .08f, 1f)
    private val rangerDark = Color(.035f, .065f, .15f, 1f)
    private val rangerGold = Color(1f, .76f, .16f, 1f)
    private val rangerArmModel = game.box(.40f, .17f, .20f, rangerBlue)
    private val rangerArms = Array(2) { ModelInstance(rangerArmModel) }
    private val rangerCuffModel = game.box(.13f, .21f, .22f, rangerCyan)
    private val rangerCuffs = Array(2) { ModelInstance(rangerCuffModel) }
    private val rangerHandModel = game.box(.25f, .24f, .25f, rangerGlove).apply {
        materials.first().set(ColorAttribute.createDiffuse(rangerGlove), ColorAttribute.createEmissive(.28f, .055f, .008f, 1f))
    }
    private val rangerHands = Array(2) { ModelInstance(rangerHandModel) }
    private val rangerLegModel = game.box(.20f, .34f, .23f, rangerDark)
    private val rangerLegs = Array(2) { ModelInstance(rangerLegModel) }
    private val rangerKneeModel = game.box(.22f, .12f, .25f, rangerCyan)
    private val rangerKnees = Array(2) { ModelInstance(rangerKneeModel) }
    private val rangerBootModel = game.box(.31f, .16f, .39f, rangerDark)
    private val rangerBoots = Array(2) { ModelInstance(rangerBootModel) }
    private val rangerBootStripeModel = game.box(.23f, .045f, .04f, rangerGold).apply {
        materials.first().set(ColorAttribute.createDiffuse(rangerGold), ColorAttribute.createEmissive(.3f, .18f, .025f, 1f))
    }
    private val rangerBootStripes = Array(2) { ModelInstance(rangerBootStripeModel) }
    private val rangerVisorModel = game.box(.70f, .16f, .055f, rangerDark)
    private val rangerVisor = ModelInstance(rangerVisorModel)
    private val rangerEyeModel = game.box(.17f, .065f, .035f, Color(.86f, 1f, 1f, 1f)).apply {
        materials.first().set(ColorAttribute.createDiffuse(Color(.86f, 1f, 1f, 1f)), ColorAttribute.createEmissive(.18f, .72f, 1f, 1f))
    }
    private val rangerEyes = Array(2) { ModelInstance(rangerEyeModel) }
    private val rangerEmblemModel = game.box(.19f, .19f, .045f, rangerGold).apply {
        materials.first().set(ColorAttribute.createDiffuse(rangerGold), ColorAttribute.createEmissive(.28f, .15f, .02f, 1f))
    }
    private val rangerEmblem = ModelInstance(rangerEmblemModel)

    // Copy the matching user photo over all six cube faces with real mesh UV coordinates.
    // A shallow front-only plate left the other five faces white and had no UVs on the base box.
    private val portraitTextures = arrayOf(
        game.texture("skins/portrait_cube_1.jpg"),
        game.texture("skins/portrait_cube_2.jpg"),
        game.texture("skins/portrait_cube_3.jpg"),
    )
    private val portraitCubes = Array(3) { i -> game.texturedBox(1f, 1f, 1f, portraitTextures[i]) }

    fun portraitCube(art: Int): Model? {
        val index = when (art) {
            Skins.ART_PORTRAIT_ONE -> 0
            Skins.ART_PORTRAIT_TWO -> 1
            Skins.ART_PORTRAIT_THREE -> 2
            else -> return null
        }
        return portraitCubes[index]
    }

    // The Air-map cube is an original sky pilot: articulated swept wings, blue goggles and a gold flight badge.
    private val soarerWhite = Color(.86f, .97f, 1f, 1f)
    private val soarerBlue = Color(.06f, .55f, .88f, 1f)
    private val soarerNavy = Color(.025f, .09f, .19f, 1f)
    private val soarerGlass = Color(.16f, .92f, 1f, 1f)
    private val soarerGold = Color(1f, .63f, .08f, 1f)
    private val soarerWingModel = game.box(.78f, .12f, .38f, soarerWhite)
    private val soarerWings = Array(2) { ModelInstance(soarerWingModel) }
    private val soarerFeatherModel = game.box(.23f, .07f, .16f, soarerBlue)
    private val soarerFeathers = Array(4) { ModelInstance(soarerFeatherModel) }
    private val soarerGoggleModel = game.box(.82f, .27f, .065f, soarerNavy)
    private val soarerGoggles = ModelInstance(soarerGoggleModel)
    private val soarerLensModel = game.box(.24f, .13f, .04f, soarerGlass).apply {
        materials.first().set(ColorAttribute.createDiffuse(soarerGlass), ColorAttribute.createEmissive(.04f, .38f, .55f, 1f))
    }
    private val soarerLenses = Array(2) { ModelInstance(soarerLensModel) }
    private val soarerScarfModel = game.box(.58f, .12f, .055f, soarerGold)
    private val soarerScarf = ModelInstance(soarerScarfModel)
    private val soarerBadgeModel = game.box(.16f, .16f, .05f, soarerGlass).apply {
        materials.first().set(ColorAttribute.createDiffuse(soarerGlass), ColorAttribute.createEmissive(.05f, .48f, .68f, 1f))
    }
    private val soarerBadge = ModelInstance(soarerBadgeModel)

    private val zAxis = Vector3.Z

    fun render(batch: ModelBatch, env: Environment, body: Matrix4, art: Int, time: Float = 0f) {
        // +Z is the front face toward the chase camera. The spheres are actual emissive geometry.
        when (art) {
            Skins.ART_DANGER -> {
                place(dangerPanel, body, -.25f, 0f, .515f)
                place(dangerEyes[0], body, -.205f, .12f, .58f)
                place(dangerEyes[1], body, .205f, .12f, .58f)
                place(dangerBrows[0], body, -.205f, .36f, .565f, rotateZ = 12f)
                place(dangerBrows[1], body, .205f, .36f, .565f, rotateZ = -12f)
                place(dangerScars[0], body, -.30f, -.34f, .565f, .30f, .034f, .025f, -24f)
                place(dangerScars[1], body, .30f, -.34f, .565f, .26f, .032f, .025f, -24f)
                batch.render(dangerPanel, env)
                batch.render(dangerScars[0], env); batch.render(dangerScars[1], env)
                batch.render(dangerBrows[0], env); batch.render(dangerBrows[1], env)
                batch.render(dangerEyes[0], env); batch.render(dangerEyes[1], env)
            }
            Skins.ART_DINO -> {
                place(eyes[0], body, -.205f, .12f, .56f)
                place(eyes[1], body, .205f, .12f, .56f)
                batch.render(eyes[0], env); batch.render(eyes[1], env)
                place(dinoSpines[0], body, 0f, .49f, -.23f)
                place(dinoSpines[1], body, 0f, .49f, 0f)
                place(dinoSpines[2], body, 0f, .49f, .23f)
                for (spine in dinoSpines) batch.render(spine, env)
                place(dinoScales[0], body, -.34f, .29f, .515f)
                place(dinoScales[1], body, .34f, .29f, .515f)
                place(dinoScales[2], body, -.34f, -.29f, .515f)
                place(dinoScales[3], body, .34f, -.29f, .515f)
                for (scale in dinoScales) batch.render(scale, env)
            }
            Skins.ART_TESSERACT -> {
                place(eyes[0], body, -.17f, .12f, .58f, .78f, .8f, .7f)
                place(eyes[1], body, .17f, .12f, .58f, .78f, .8f, .7f)
                place(tesseractBars[0], body, 0f, .34f, .56f, .88f, 1f, 1f)
                place(tesseractBars[1], body, 0f, -.34f, .56f, .88f, 1f, 1f)
                place(tesseractBars[2], body, -.34f, 0f, .56f, .11f, 1f, 1f, 90f)
                place(tesseractBars[3], body, .34f, 0f, .56f, .11f, 1f, 1f, 90f)
                place(tesseractBars[4], body, 0f, 0f, .565f, .74f, .7f, 1f)
                place(tesseractBars[5], body, 0f, 0f, .57f, .09f, .72f, 1f, 90f)
                place(tesseractBars[6], body, 0f, 0f, .575f, .97f, .55f, 1f, 42f)
                place(tesseractBars[7], body, 0f, 0f, .58f, .97f, .55f, 1f, -42f)
                for (bar in tesseractBars) batch.render(bar, env)
                batch.render(eyes[0], env); batch.render(eyes[1], env)
                for (i in tesseractNodes.indices) {
                    val x = if (i % 2 == 0) -.34f else .34f
                    val y = if (i < 2) .34f else -.34f
                    place(tesseractNodes[i], body, x, y, .61f)
                    batch.render(tesseractNodes[i], env)
                }
            }
            Skins.ART_AURORA -> {
                place(auroraBands[0], body, -.02f, -.28f, .56f, .86f, .92f, 1f, -9f)
                place(auroraBands[1], body, .015f, 0f, .565f, 1f, 1f, 1f, 5f)
                place(auroraBands[2], body, .02f, .28f, .57f, .82f, .92f, 1f, -7f)
                for (band in auroraBands) batch.render(band, env)
                place(auroraEyes[0], body, -.20f, .06f, .60f)
                place(auroraEyes[1], body, .20f, .06f, .60f)
                batch.render(auroraEyes[0], env); batch.render(auroraEyes[1], env)
            }
            Skins.ART_RENEGADE -> {
                place(cape, body, 0f, -.10f, -.50f)
                place(renegadeBlackHalf, body, .25f, 0f, .515f)
                place(mask, body, 0f, .13f, .505f)
                for (i in 0..1) {
                    val x = if (i == 0) -.205f else .205f
                    place(whitePatches[i], body, x, .12f, .555f, 1.0f, .62f, .15f)
                    place(renegadeEyes[i], body, x, .12f, .585f)
                }
                place(emblemA, body, -.055f, -.23f, .565f, .19f, .045f, .025f, -25f)
                place(emblemB, body, .055f, -.23f, .565f, .19f, .045f, .025f, 25f)
                batch.render(cape, env)
                batch.render(renegadeBlackHalf, env)
                batch.render(mask, env)
                for (patch in whitePatches) batch.render(patch, env)
                for (eye in renegadeEyes) batch.render(eye, env)
                batch.render(emblemA, env); batch.render(emblemB, env)
            }
            Skins.ART_DEADPOOL -> {
                place(deadpoolMask, body, 0f, .13f, .515f)
                place(deadpoolPatches[0], body, -.215f, .13f, .565f, .82f, .52f, .12f, 12f)
                place(deadpoolPatches[1], body, .215f, .13f, .565f, .82f, .52f, .12f, -12f)
                place(deadpoolStraps[0], body, 0f, -.25f, .565f, .95f, 1f, 1f, 35f)
                place(deadpoolStraps[1], body, 0f, -.25f, .57f, .95f, 1f, 1f, -35f)
                place(deadpoolBelt, body, 0f, -.405f, .56f)
                place(deadpoolBuckle, body, 0f, -.405f, .60f)
                batch.render(deadpoolMask, env)
                for (patch in deadpoolPatches) batch.render(patch, env)
                for (strap in deadpoolStraps) batch.render(strap, env)
                batch.render(deadpoolBelt, env); batch.render(deadpoolBuckle, env)
            }
            Skins.ART_SPIDER_MAN -> {
                place(spiderBluePanels[0], body, -.385f, 0f, .515f)
                place(spiderBluePanels[1], body, .385f, 0f, .515f)
                place(spiderBlueBand, body, 0f, -.415f, .54f)
                batch.render(spiderBluePanels[0], env); batch.render(spiderBluePanels[1], env)
                batch.render(spiderBlueBand, env)
                val webY = floatArrayOf(.42f, .21f, 0f, -.21f, -.42f)
                val webWidth = floatArrayOf(.40f, .68f, .86f, .68f, .40f)
                for (i in 0..4) {
                    place(spiderWebBars[i], body, 0f, webY[i], .57f, webWidth[i], .8f, .7f)
                    batch.render(spiderWebBars[i], env)
                }
                for (i in 0..3) {
                    place(spiderWebBars[i + 5], body, 0f, 0f, .575f, .86f, .85f, .7f, i * 45f)
                    batch.render(spiderWebBars[i + 5], env)
                }
                for (i in 0..1) {
                    val x = if (i == 0) -.205f else .205f
                    val tilt = if (i == 0) 10f else -10f
                    place(spiderRims[i], body, x, .14f, .58f, .86f, .57f, .13f, tilt)
                    place(spiderEyes[i], body, x, .14f, .605f, .68f, .40f, .12f, tilt)
                    batch.render(spiderRims[i], env); batch.render(spiderEyes[i], env)
                }
            }
            Skins.ART_IRON_SENTINEL -> {
                place(sentinelTrim[0], body, 0f, .36f, .55f, 1f, .65f, 1f)
                place(sentinelTrim[1], body, 0f, -.38f, .55f, 1f, .65f, 1f)
                place(sentinelVisor, body, 0f, .13f, .515f)
                for (i in 0..1) {
                    val x = if (i == 0) -.22f else .22f
                    place(sentinelEyes[i], body, x, .13f, .565f, .78f, .56f, .30f)
                }
                place(sentinelCore, body, 0f, -.24f, .565f)
                for (trim in sentinelTrim) batch.render(trim, env)
                batch.render(sentinelVisor, env)
                for (eye in sentinelEyes) batch.render(eye, env)
                batch.render(sentinelCore, env)
            }
            Skins.ART_FROST_BYTE -> {
                for (i in 0..2) {
                    val x = (i - 1) * .30f
                    place(frostCrystals[i], body, x, .57f, -.05f, .75f, 1f, .75f)
                    batch.render(frostCrystals[i], env)
                }
                place(frostBars[0], body, 0f, 0f, .565f, 1f, 1f, 1f)
                place(frostBars[1], body, 0f, 0f, .57f, 1f, 1f, 1f, 60f)
                place(frostBars[2], body, 0f, 0f, .575f, 1f, 1f, 1f, -60f)
                for (bar in frostBars) batch.render(bar, env)
                val nodeX = floatArrayOf(0f, 0f, -.27f, .27f)
                val nodeY = floatArrayOf(.27f, -.27f, 0f, 0f)
                for (i in frostNodes.indices) {
                    place(frostNodes[i], body, nodeX[i], nodeY[i], .61f)
                    batch.render(frostNodes[i], env)
                }
                place(spiderEyes[0], body, -.20f, .15f, .60f, .54f, .34f, .10f)
                place(spiderEyes[1], body, .20f, .15f, .60f, .54f, .34f, .10f)
                batch.render(spiderEyes[0], env); batch.render(spiderEyes[1], env)
            }
            Skins.ART_HACKER -> {
                place(hackerAntenna, body, 0f, .56f, -.04f)
                place(hackerAntennaTip, body, 0f, .70f, -.04f)
                place(hackerVisor, body, 0f, .15f, .515f)
                place(hackerEyes[0], body, -.21f, .15f, .56f)
                place(hackerEyes[1], body, .21f, .15f, .56f)
                val pixelX = floatArrayOf(-.31f, -.10f, .10f, .31f, -.20f, .20f)
                val pixelY = floatArrayOf(-.23f, -.23f, -.23f, -.23f, -.36f, -.36f)
                for (i in hackerPixels.indices) {
                    place(hackerPixels[i], body, pixelX[i], pixelY[i], .555f,
                        if (i % 2 == 0) .9f else .62f, if (i < 4) .82f else .58f, .7f)
                    batch.render(hackerPixels[i], env)
                }
                batch.render(hackerAntenna, env); batch.render(hackerAntennaTip, env)
                batch.render(hackerVisor, env)
                batch.render(hackerEyes[0], env); batch.render(hackerEyes[1], env)
            }
            Skins.ART_RIFT_RANGER -> {
                for (i in 0..1) {
                    val side = if (i == 0) -1f else 1f
                    val armSwing = RiftRangerGait.armSwing(time, side)
                    val legSwing = RiftRangerGait.legSwing(time, side)
                    val footLift = RiftRangerGait.footLift(time, side)
                    place(rangerArms[i], body, side * .58f, .06f + armSwing * .0015f, 0f, rotateZ = armSwing)
                    place(rangerCuffs[i], body, side * .80f, .015f + armSwing * .0011f, .015f, rotateZ = armSwing)
                    place(rangerHands[i], body, side * .95f, -.025f + armSwing * .0009f, .045f, rotateZ = armSwing)
                    place(rangerLegs[i], body, side * .24f, -.62f + footLift * .65f, -.015f, rotateZ = legSwing)
                    place(rangerKnees[i], body, side * .24f, -.72f + footLift * .45f, .055f)
                    place(rangerBoots[i], body, side * .26f, -.92f + footLift, .08f, rotateZ = legSwing * .62f)
                    place(rangerBootStripes[i], body, side * .26f, -.90f + footLift, .285f)
                }
                place(rangerVisor, body, 0f, .17f, .515f)
                place(rangerEyes[0], body, -.18f, .17f, .56f)
                place(rangerEyes[1], body, .18f, .17f, .56f)
                place(rangerEmblem, body, 0f, -.24f, .56f, rotateZ = 45f)
                for (part in rangerArms) batch.render(part, env)
                for (part in rangerCuffs) batch.render(part, env)
                for (part in rangerHands) batch.render(part, env)
                for (part in rangerLegs) batch.render(part, env)
                for (part in rangerKnees) batch.render(part, env)
                for (part in rangerBoots) batch.render(part, env)
                for (part in rangerBootStripes) batch.render(part, env)
                batch.render(rangerVisor, env)
                batch.render(rangerEyes[0], env); batch.render(rangerEyes[1], env)
                batch.render(rangerEmblem, env)
            }
            Skins.ART_PORTRAIT_ONE, Skins.ART_PORTRAIT_TWO, Skins.ART_PORTRAIT_THREE -> Unit // already mapped over all six body faces
            Skins.ART_SKY_SOARER -> {
                val flap = sin(time * 8f)
                place(soarerWings[0], body, -.62f, .015f + flap * .035f, -.10f, rotateZ = -13f + flap * 9f)
                place(soarerWings[1], body, .62f, .015f + flap * .035f, -.10f, rotateZ = 13f - flap * 9f)
                batch.render(soarerWings[0], env); batch.render(soarerWings[1], env)
                place(soarerFeathers[0], body, -.83f, .055f + flap * .04f, -.01f, .88f, 1f, .8f, rotateZ = -18f)
                place(soarerFeathers[1], body, -.53f, .12f + flap * .04f, .01f, .8f, 1f, .8f, rotateZ = -10f)
                place(soarerFeathers[2], body, .53f, .12f + flap * .04f, .01f, .8f, 1f, .8f, rotateZ = 10f)
                place(soarerFeathers[3], body, .83f, .055f + flap * .04f, -.01f, .88f, 1f, .8f, rotateZ = 18f)
                for (feather in soarerFeathers) batch.render(feather, env)
                place(soarerGoggles, body, 0f, .14f, .515f)
                for (i in 0..1) place(soarerLenses[i], body, if (i == 0) -.20f else .20f, .14f, .56f)
                place(soarerScarf, body, 0f, -.19f, .535f)
                place(soarerBadge, body, 0f, -.20f, .57f, rotateZ = 45f)
                batch.render(soarerGoggles, env)
                for (lens in soarerLenses) batch.render(lens, env)
                batch.render(soarerScarf, env); batch.render(soarerBadge, env)
            }
            else -> {
                place(eyes[0], body, -.205f, .12f, .56f)
                place(eyes[1], body, .205f, .12f, .56f)
                place(scratches[0], body, -.31f, -.27f, .555f, rotateZ = -18f)
                place(scratches[1], body, .31f, -.34f, .555f, rotateZ = 25f)
                batch.render(eyes[0], env); batch.render(eyes[1], env)
                batch.render(scratches[0], env); batch.render(scratches[1], env)
            }
        }
    }

    private fun place(
        instance: ModelInstance,
        body: Matrix4,
        x: Float,
        y: Float,
        z: Float,
        sx: Float = 1f,
        sy: Float = 1f,
        sz: Float = 1f,
        rotateZ: Float = 0f,
    ) {
        instance.transform.set(body).translate(x, y, z)
        if (rotateZ != 0f) instance.transform.rotate(zAxis, rotateZ)
        if (sx != 1f || sy != 1f || sz != 1f) instance.transform.scale(sx, sy, sz)
    }

    private companion object {
        val EYE = Color(.98f, .16f, .035f, 1f)
        val DANGER_EYE = Color(1f, .10f, .018f, 1f)
    }
}
