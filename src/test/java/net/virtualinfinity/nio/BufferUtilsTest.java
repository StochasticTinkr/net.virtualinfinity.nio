package net.virtualinfinity.nio;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

/**
 * @author <a href='mailto:Daniel@coloraura.com'>Daniel Pitts</a>
 */
public class BufferUtilsTest {

    @Test
    public void putWhatFitsByteBufferOverflow() {
        final ByteBuffer src = ByteBuffer.wrap(new byte[] {1, 2, 3});
        final ByteBuffer dest = ByteBuffer.allocate(2);
        BufferUtils.putWhatFits(dest, src);
        assertEquals("3 should be the remainder of src", 3, src.get());
        dest.flip();
        assertEquals(1, dest.get());
        assertEquals(2, dest.get());
    }

    @Test
    public void putWhatFitsByteBufferUnderflow() {
        final ByteBuffer src = ByteBuffer.wrap(new byte[] {1, 2});
        final ByteBuffer dest = ByteBuffer.allocate(3);
        BufferUtils.putWhatFits(dest, src);
        assertFalse(src.hasRemaining());
        assertEquals("Should have space still in dest.", 1, dest.remaining());
        dest.flip();
        assertEquals(1, dest.get());
        assertEquals(2, dest.get());
    }
}