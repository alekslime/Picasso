package app.picasso.tracer.camera

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer

class YuvConverterTest {

    @Test
    fun `packs contiguous planes to nv21`() {
        val w = 4
        val h = 4
        val y = ByteBuffer.wrap(ByteArray(w * h) { (it + 1).toByte() })
        val u = ByteBuffer.wrap(ByteArray(4) { 0x50 })
        val v = ByteBuffer.wrap(ByteArray(4) { 0xA0.toByte() })
        val nv21 = YuvConverter.toNv21(y, u, v, w, h, w, 2, 1)
        assertEquals(w * h * 3 / 2, nv21.size)
        // Y first, untouched.
        assertArrayEquals(ByteArray(w * h) { (it + 1).toByte() }, nv21.copyOf(w * h))
        // Then V,U interleaved.
        val chroma = nv21.copyOfRange(w * h, nv21.size)
        assertArrayEquals(byteArrayOf(0xA0.toByte(), 0x50, 0xA0.toByte(), 0x50, 0xA0.toByte(), 0x50, 0xA0.toByte(), 0x50), chroma)
    }

    @Test
    fun `honours row padding and pixel stride`() {
        val w = 4
        val h = 2
        // Y rows padded to 6 bytes; chroma rows padded, pixel stride 2.
        val yRaw = ByteArray(2 * 6) { if (it % 6 < 4) 0x10 else 0x00 }
        val uRaw = ByteArray(1 * 4) { if (it % 2 == 0) 0x20 else 0x00 }
        val vRaw = ByteArray(1 * 4) { if (it % 2 == 0) 0x30 else 0x00 }
        val nv21 = YuvConverter.toNv21(
            ByteBuffer.wrap(yRaw), ByteBuffer.wrap(uRaw), ByteBuffer.wrap(vRaw),
            w, h, 6, 4, 2,
        )
        assertEquals(w * h * 3 / 2, nv21.size)
        assertArrayEquals(ByteArray(8) { 0x10 }, nv21.copyOf(8))
        assertArrayEquals(byteArrayOf(0x30, 0x20, 0x30, 0x20), nv21.copyOfRange(8, 12))
    }
}
