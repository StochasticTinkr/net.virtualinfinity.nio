package net.virtualinfinity.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * A byte buffer queue useful for non blocking output. This output buffer keeps a queue of
 * smaller buffers until they are able to be sent using the {@link #send(ByteBufferConsumer)} method.
 *
 * <p>
 * When a new buffer is needed, it will be allocated via {@link ByteBuffer#allocateDirect(int)}.
 * The size of the buffer will be the higher of <code>minimumBufferSize</code> and the number of
 * bytes currently being appended.
 *
 * <p>
 * This buffer can grow unbounded.
 *
 * <p>
 * <strong>Warning:</strong> This class is not thread-safe.
 *
 * @see #append(ByteBuffer)
 * @see #send(ByteBufferConsumer)
*/
public final class OutputBuffer {
    private final List<Runnable> newDataListeners = new ArrayList<>();
    private final int minimumBufferSize;
    private final Deque<ByteBuffer> buffers = new LinkedList<>();
    private long remaining;

    /**
     * Constructs an OutputBuffer with minimumBufferSize of 512.
     * @see #append(ByteBuffer)
     */
    public OutputBuffer() {
        this(512);
    }

    /**
     * Constructs an OutputBuffer with the given minimumBufferSize.
     * @param minimumBufferSize The minimum buffer size to be allocated when a new buffer is needed.
     */
    public OutputBuffer(int minimumBufferSize) {
        this.minimumBufferSize = minimumBufferSize;
    }

    /**
     * Enqueue the data in the buffer to be sent at a later time.
     * <p>
     * First, an attempt is made to fill the last buffer in the queue.  If there is still data remaining, a new
     * direct buffer is allocated that is large enough to fit the remaining data, and at least minimumBufferSize.
     *
     * @param data the data to append to the output buffer.
     *
     * @throws NullPointerException if data is null.
     *
     * @return The amount of data copied, which is always the full amount of <code>data.remaining()</code> when the
     *         method is invoked.
     */
    public int append(ByteBuffer data) {
        if (data == null) {
            throw new NullPointerException("data");
        }
        final int count = data.remaining();
        remaining += count;
        if (!buffers.isEmpty()) {
            BufferUtils.putWhatFits(buffers.getLast(), data);
        }
        appendRemaining(data);
        if (remaining == count) {
            newDataListeners.forEach(Runnable::run);
        }
        return count;
    }

    /**
     * Sends as much of the remaining data as is available to the recipient, until the recipient doesn't
     * accept a full buffer.
     * <p>
     * If <code>recipient.accept(buffer)</code> is a blocking method that blocks until all data is sent, this method
     * with also block until the queue is completely flushed.
     * If <code>recipient.accept(buffer)</code> is a non-blocking method, or doesn't consume the entire buffer,
     * then this method can return before all the data is flushed.  If that's the case, this method should be called again
     * when the recipient is ready to handle more data.
     *
     * @param recipient
     *        the consumer that will process the data.
     *
     * @throws IOException if the recipient method does.
     * @throws NullPointerException if recipient is null
     *
     * @return this
     *
     */
    public OutputBuffer send(ByteBufferConsumer recipient) throws IOException {
        if (recipient == null) {
            throw new NullPointerException("recipient");
        }
        for (final Iterator<ByteBuffer> iterator = buffers.iterator(); iterator.hasNext(); ) {
            final ByteBuffer buffer = iterator.next();
            buffer.flip();
            remaining -= buffer.remaining();
            recipient.accept(buffer);
            remaining += buffer.remaining();
            if (buffer.hasRemaining()) {
                buffer.compact();
                break;
            }
            iterator.remove();
        }

        return this;
    }

    /**
     * Test whether or not more data is available for the {@link #send(ByteBufferConsumer)} method.
     *
     * @return true if there is more data.
     */
    public boolean hasRemaining() {
        return remaining != 0;
    }

    /**
     * @return the number of bytes remaining to be sent.
     */
    public long remaining() {
        return remaining;
    }

    /**
     * Appends a new buffer that contains a copy of the data.
     * <p>
     * If the data has any remaining, a new buffer is allocated that is the maximum of
     * {@link #minimumBufferSize} and <code>data.remaining()</code>.
     * data is copied into that buffer.
     *
     * @param data the data
     */
    private void appendRemaining(ByteBuffer data) {
        if (!data.hasRemaining()) {
            return;
        }
        buffers.add(copyOf(data, Math.max(minimumBufferSize, data.remaining())));
    }

    /**
     * Creates a new byte buffer of the given size, and fills it with a copy of data.
     *
     * @param src the source buffer
     * @param size the newly allocated buffer size.
     *
     * @return the newly allocated buffer.
     */
    private ByteBuffer copyOf(ByteBuffer src, int size) {
        return doAllocate(size).put(src);
    }

    /**
     * Allocates a ByteBuffer of the given size.  Future versions of this class may
     * allow this behavior to be customized.
     * @param size the size, in bytes, of the new ByteBuffer.
     *
     * @return the newly allocated buffer.
     */
    private ByteBuffer doAllocate(int size) {
        return ByteBuffer.allocateDirect(size);
    }

    public void removeNewDataListener(Runnable listener) {
        newDataListeners.remove(listener);
    }
    public void addNewDataListener(Runnable listener) {
        newDataListeners.add(listener);

    }
}
