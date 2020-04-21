package net.virtualinfinity.nio;

//import org.junit.Test;
//
//import java.io.IOException;
//import java.nio.ByteBuffer;
//
//import static org.junit.Assert.*;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
*/
public class OutputBufferTest {

    @Test
    public void testBuffer() throws IOException {
        final OutputBuffer buffer = new OutputBuffer(8);
        buffer.append(ByteBuffer.wrap(new byte[]{1, 2, 3, 4, 5, 6, 7}));
        buffer.append(ByteBuffer.wrap(new byte[] {8}));
        buffer.append(ByteBuffer.wrap(new byte[] {9, 10, 11, 12, 13, 14, 15, 16, 17, 18}));

        assertEquals(18, buffer.remaining());
        final ByteBuffer output = ByteBuffer.allocate(9);
        buffer.send(src -> BufferUtils.putWhatFits(output, src));
        assertEquals(9, buffer.remaining());
        assertTrue(buffer.hasRemaining());
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9}, output.array());
        output.clear();
        buffer.send(output::put);
        assertEquals(0, buffer.remaining());
        assertFalse(buffer.hasRemaining());
        assertArrayEquals(new byte[]{10, 11, 12, 13, 14, 15, 16, 17, 18}, output.array());

    }
}