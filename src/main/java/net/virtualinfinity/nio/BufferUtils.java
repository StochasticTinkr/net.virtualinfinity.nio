package net.virtualinfinity.nio;

import java.nio.ByteBuffer;

/**
 * Contains some methods for buffers.
 *
 * @author <a href='mailto:Daniel@coloraura.com'>Daniel Pitts</a>
 */
public class BufferUtils {
    /**
     * Puts as much of <code>src</code> as will fit in <code>dst</code>.
     *
     * The minimum of (<code>dst.remaining()</code> and <code>src.remaining()</code>) is copied from src
     * to dst, and their respective positions moved by that much.
     *
     * @param dst the destination buffer.
     * @param src the source buffer.
     */
    public static void putWhatFits(ByteBuffer dst, ByteBuffer src) {
        final int toPut = Math.min(dst.remaining(), src.remaining());
        if (toPut == src.remaining()) {
            dst.put(src);
        } else {
            if (toPut != 0) {
                final ByteBuffer slice = src.slice();
                slice.limit(toPut);
                src.position(src.position() + toPut);
                dst.put(slice);
            }
        }
    }
}
