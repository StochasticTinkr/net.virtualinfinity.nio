package net.virtualinfinity.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
/**
 * A consumer of byte buffers.  The consume method is intentionally mimics the semantics of
 * {@link java.nio.channels.WritableByteChannel#write(ByteBuffer)}.
 *
 * @author <a href='mailto:Daniel@coloraura.com'>Daniel Pitts</a>
 */
@FunctionalInterface
public interface ByteBufferConsumer {
    /**
     * This method should implement the same contract as {@link java.nio.channels.WritableByteChannel#write(ByteBuffer)}.
     * Though it doesn't need to be tied to a "Channel" implementation, and it doesn't need to return a value.
     *
     * @param  src
     *         The buffer from which bytes are to be retrieved
     **
     * @throws  IOException
     *          If some I/O error occurs
     */
    void accept(ByteBuffer src) throws IOException;
}
