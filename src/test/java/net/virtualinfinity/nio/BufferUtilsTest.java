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
        final ByteBuffer dst = ByteBuffer.allocate(2);
        BufferUtils.putWhatFits(dst, src);
        assertEquals("3 should be the remainder of src", 3, src.get());
        dst.flip();
        assertEquals(1, dst.get());
        assertEquals(2, dst.get());
    }

    @Test
    public void putWhatFitsByteBufferUnderflow() {
        final ByteBuffer src = ByteBuffer.wrap(new byte[] {1, 2});
        final ByteBuffer dst = ByteBuffer.allocate(3);
        BufferUtils.putWhatFits(dst, src);
        assertFalse(src.hasRemaining());
        assertEquals("Should have space still in dst.", 1, dst.remaining());
        dst.flip();
        assertEquals(1, dst.get());
        assertEquals(2, dst.get());
    }
}