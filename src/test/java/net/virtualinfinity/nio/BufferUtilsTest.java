package net.virtualinfinity.nio;


import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
*/
public class BufferUtilsTest {

    /**
     * Validates that putWhatFits will only copy as much as will fit into the destination buffer,
     * leaving the rest in the source buffer.
     */
    @Test
    public void putWhatFits_byteBufferOverflow() {
        final ByteBuffer src = ByteBuffer.wrap(new byte[]{1, 2, 3});
        final ByteBuffer dest = ByteBuffer.allocate(2);
        BufferUtils.putWhatFits(dest, src);
        assertEquals(3, src.get(), "3 should be the remainder of src");
        dest.flip();
        assertEquals(1, dest.get());
        assertEquals(2, dest.get());
    }

    /**
     * Validates that putWhatFits will only copy as much as is in the src buffer, and leaves the rest of the space
     * in the destination.
     */
    @Test
    public void putWhatFits_byteBufferUnderflow() {
        final ByteBuffer src = ByteBuffer.wrap(new byte[]{1, 2});
        final ByteBuffer dest = ByteBuffer.allocate(3);
        BufferUtils.putWhatFits(dest, src);
        assertFalse(src.hasRemaining());
        assertEquals(1, dest.remaining(), "Should have space still in dest.");
        dest.flip();
        assertEquals(1, dest.get());
        assertEquals(2, dest.get());
    }
}