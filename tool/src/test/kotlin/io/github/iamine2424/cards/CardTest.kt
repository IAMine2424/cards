package io.github.iamine2424.cards

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.oned.Code128Reader
import com.google.zxing.LuminanceSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CardTest {

    // Same shapes as real Cineville (prefix + digits) and Museumkaart (19 digits) barcodes.
    @Test
    fun cardValuesRoundTripThroughCode128() {
        for (value in listOf("CP$123456789", "1234567890123456789")) {
            val matrix = encode(value, BarcodeFormat.CODE_128)
            assertEquals(1, matrix.height)
            val row = Code128Reader().decode(BinaryBitmap(HybridBinarizer(RowSource(matrix))))
            assertEquals(value, row.text)
        }
    }

    @Test
    fun invalidValueForFormatFails() {
        assertNull(runCatching { encode("CP$123456789", BarcodeFormat.EAN_13) }.getOrNull())
    }

    @Test
    fun formatCycleWraps() {
        assertEquals(FORMATS.first(), FORMATS.last().next())
        assertTrue(FORMATS.all { it.next() in FORMATS })
    }

    /** Renders a 1-row BitMatrix as a 3px-per-module, 10px-tall grayscale image. */
    private class RowSource(private val m: BitMatrix) : LuminanceSource(m.width * 3, 10) {
        private val row = ByteArray(width) { if (m[it / 3, 0]) 0 else -1 }
        override fun getRow(y: Int, out: ByteArray?) = row.copyOf()
        override fun getMatrix() = ByteArray(width * height) { row[it % width] }
    }
}
