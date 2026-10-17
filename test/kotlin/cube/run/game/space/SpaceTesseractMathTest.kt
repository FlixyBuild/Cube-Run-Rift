package cube.run.game.space

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Test

class SpaceTesseractMathTest {
    @Test fun localZAxisAlignsWithEveryProjectedEdgeDirection() {
        val samples = arrayOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f),
            floatArrayOf(0f, 0f, -1f),
            floatArrayOf(1f, -2f, 3f),
            floatArrayOf(-.4f, .8f, -.2f),
        )
        val pose = FloatArray(3)
        val degToRad = (Math.PI / 180.0).toFloat()
        for (edge in samples) {
            SpaceTesseractMath.segmentPose(edge[0], edge[1], edge[2], pose)
            val yaw = pose[0] * degToRad
            val pitch = pose[1] * degToRad
            val axisX = sin(yaw) * cos(pitch)
            val axisY = -sin(pitch)
            val axisZ = cos(yaw) * cos(pitch)
            val length = sqrt(edge[0] * edge[0] + edge[1] * edge[1] + edge[2] * edge[2])
            assertEquals(edge[0] / length, axisX, .0001f)
            assertEquals(edge[1] / length, axisY, .0001f)
            assertEquals(edge[2] / length, axisZ, .0001f)
            assertEquals(length * .5f, pose[2], .0001f)
        }
    }
}
