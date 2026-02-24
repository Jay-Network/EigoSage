package com.jworks.eigolens.ui.capture

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Size
import androidx.compose.ui.geometry.Offset
import com.jworks.eigolens.domain.models.DetectedText
import com.jworks.eigolens.domain.models.OCRResult
import com.jworks.eigolens.domain.models.TextElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WordTapDetectorTest {

    private lateinit var bitmap: Bitmap
    private val containerSize = androidx.compose.ui.geometry.Size(1080f, 1920f)
    private val scale = 1.0f
    private val offset = Offset.Zero

    @Before
    fun setup() {
        bitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
    }

    private fun makeOcrResult(vararg elements: Pair<String, Rect>): OCRResult {
        val textElements = elements.map { (text, bounds) ->
            TextElement(text = text, bounds = bounds, isWord = true)
        }
        return OCRResult(
            texts = listOf(
                DetectedText(
                    text = elements.joinToString(" ") { it.first },
                    bounds = null,
                    confidence = 0.95f,
                    elements = textElements
                )
            ),
            timestamp = System.currentTimeMillis(),
            imageSize = Size(1080, 1920)
        )
    }

    @Test
    fun `transformScreenToImage is inverse of transformPointToScreen`() {
        val testScale = 2.0f
        val testOffset = Offset(50f, -30f)

        // Original image point
        val imgX = 200f
        val imgY = 300f

        // Forward: image → screen
        val screenPoint = transformPointToScreen(
            imgX, imgY, bitmap, testScale, testOffset, containerSize
        )

        // Inverse: screen → image
        val imagePoint = transformScreenToImage(
            screenPoint.x, screenPoint.y, bitmap, testScale, testOffset, containerSize
        )

        assertEquals(imgX, imagePoint.x, 0.01f)
        assertEquals(imgY, imagePoint.y, 0.01f)
    }

    @Test
    fun `transformScreenToImage roundtrip with various scales`() {
        for (s in listOf(0.5f, 1.0f, 1.5f, 3.0f)) {
            for (off in listOf(Offset.Zero, Offset(100f, 200f), Offset(-50f, -80f))) {
                val imgX = 540f
                val imgY = 960f

                val screen = transformPointToScreen(imgX, imgY, bitmap, s, off, containerSize)
                val img = transformScreenToImage(screen.x, screen.y, bitmap, s, off, containerSize)

                assertEquals("Scale=$s, Offset=$off: x mismatch", imgX, img.x, 0.01f)
                assertEquals("Scale=$s, Offset=$off: y mismatch", imgY, img.y, 0.01f)
            }
        }
    }

    @Test
    fun `findTappedWord returns correct word when tapping center of bounds`() {
        val ocrResult = makeOcrResult(
            "Hello" to Rect(100, 100, 200, 140),
            "World" to Rect(220, 100, 320, 140)
        )

        val result = findTappedWord(
            tapOffset = Offset(150f, 120f), // center of "Hello"
            ocrResult = ocrResult,
            bitmap = bitmap,
            scale = scale,
            offset = offset,
            containerSize = containerSize
        )

        assertNotNull(result)
        assertEquals("Hello", result!!.word)
    }

    @Test
    fun `findTappedWord returns null when tapping empty area`() {
        val ocrResult = makeOcrResult(
            "Hello" to Rect(100, 100, 200, 140)
        )

        val result = findTappedWord(
            tapOffset = Offset(500f, 500f), // far from any word
            ocrResult = ocrResult,
            bitmap = bitmap,
            scale = scale,
            offset = offset,
            containerSize = containerSize
        )

        assertNull(result)
    }

    @Test
    fun `findTappedWord uses tolerance for near-miss taps`() {
        val ocrResult = makeOcrResult(
            "test" to Rect(100, 100, 200, 140)
        )

        // Tap 15px outside the right edge — within 20px tolerance
        val result = findTappedWord(
            tapOffset = Offset(215f, 120f),
            ocrResult = ocrResult,
            bitmap = bitmap,
            scale = scale,
            offset = offset,
            containerSize = containerSize
        )

        assertNotNull(result)
        assertEquals("test", result!!.word)
    }

    @Test
    fun `findTappedWord picks closest word when overlapping tolerance zones`() {
        val ocrResult = makeOcrResult(
            "left" to Rect(100, 100, 200, 140),
            "right" to Rect(210, 100, 310, 140) // 10px gap
        )

        // Tap in the gap but closer to "right"
        val result = findTappedWord(
            tapOffset = Offset(207f, 120f),
            ocrResult = ocrResult,
            bitmap = bitmap,
            scale = scale,
            offset = offset,
            containerSize = containerSize
        )

        assertNotNull(result)
        assertEquals("right", result!!.word)
    }

    @Test
    fun `findTappedWord works with zoomed and panned image`() {
        val zoomScale = 2.0f
        val panOffset = Offset(100f, 50f)

        val ocrResult = makeOcrResult(
            "zoomed" to Rect(100, 100, 200, 140)
        )

        // Calculate where center of "zoomed" (150, 120) appears on screen
        val screenCenter = transformPointToScreen(
            150f, 120f, bitmap, zoomScale, panOffset, containerSize
        )

        val result = findTappedWord(
            tapOffset = screenCenter,
            ocrResult = ocrResult,
            bitmap = bitmap,
            scale = zoomScale,
            offset = panOffset,
            containerSize = containerSize
        )

        assertNotNull(result)
        assertEquals("zoomed", result!!.word)
    }
}
