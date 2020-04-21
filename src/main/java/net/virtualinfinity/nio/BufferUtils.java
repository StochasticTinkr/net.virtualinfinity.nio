package net.virtualinfinity.nio;

import java.nio.ByteBuffer;

/**
 * Contains some methods for buffers.
*/
public class BufferUtils {
    /**
     * Puts as much of <code>src</code> as will fit in <code>dest</code>.
     *
     * The minimum of (<code>dest.remaining()</code> and <code>src.remaining()</code>) is copied from src
     * to dest, and their respective positions moved by that much.
     *
     * @param dest the destination buffer.
     * @param src the source buffer.
     */
    public static void putWhatFits(ByteBuffer dest, ByteBuffer src) {
        final int toPut = Math.min(dest.remaining(), src.remaining());
        if (toPut == src.remaining()) {
            dest.put(src);
        } else {
            if (toPut != 0) {
                final ByteBuffer slice = src.slice();
                slice.limit(toPut);
                src.position(src.position() + toPut);
                dest.put(slice);
            }
        }
    }
}
