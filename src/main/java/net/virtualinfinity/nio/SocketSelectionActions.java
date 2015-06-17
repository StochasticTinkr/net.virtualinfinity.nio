package net.virtualinfinity.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * @author <a href='mailto:Daniel@coloraura.com'>Daniel Pitts</a>
 */
public class SocketSelectionActions implements SelectionKeyActions {
    private final SocketChannel channel;
    private final ConnectionListener connectionListener;
    private final ByteBufferConsumer receiver;
    private final boolean sendAllBeforeReading;
    private final ByteBuffer inputBuffer;
    private final OutputBuffer outputBuffer;
    private SelectionKey selectionKey;

    /**
     * Construct a new SocketSelectionActions, which will be able to manage finishing connections, and transferring data
     * to and from the socket.
     *
     * @param channel the channel.
     * @param connectionListener The connection listener to be notified about connections.
     * @param receiver The object that will receive data read from the socket
     * @param outputBuffer The output buffer that will be sent over the socket.
     * @param sendAllBeforeReading Whether or not the outputBuffer should be fully flushed before new input is processed.
     * @param inputBufferSize The input buffer size.
     */
    public SocketSelectionActions(SocketChannel channel, ConnectionListener connectionListener, ByteBufferConsumer receiver, OutputBuffer outputBuffer, int inputBufferSize, boolean sendAllBeforeReading) {
        this.channel = channel;
        this.connectionListener = connectionListener;
        this.receiver = receiver;
        this.outputBuffer = outputBuffer;
        this.sendAllBeforeReading = sendAllBeforeReading;
        this.inputBuffer = ByteBuffer.allocateDirect(inputBufferSize);
    }


    public void register(EventLoop loop) throws ClosedChannelException {
        loop.registerHandler(channel(), this);
    }

    @Override
    public int interestOps() {
        if (interestedInConnect()) {
            return SelectionKey.OP_CONNECT;
        }
        int interest = interestedInWrite() ? SelectionKey.OP_WRITE : 0;
        if (interestedInRead()) {
            interest |= SelectionKey.OP_READ;
        }
        return interest;
    }

    @Override
    public void setSelectionKey(SelectionKey selectionKey) {
        if (this.selectionKey != null && selectionKey == null) {
            outputBuffer.removeNewDataListener(this::updateInterests);
        }
        if (this.selectionKey == null && selectionKey != null) {
            outputBuffer.addNewDataListener(this::updateInterests);
        }
        this.selectionKey = selectionKey;
    }

    @Override
    public void selected() throws IOException {
        try {
            doSelectedActions();
        } finally {
            updateInterests();
        }
    }

    protected void doSelectedActions() throws IOException {
        if (!channel.isOpen()) {
            throw new ClosedChannelException();
        }
        if (channel.isConnectionPending()) {
            if (selectionKey.isConnectable()) {
                try {
                    if (channel.finishConnect()) {
                        connectionListener.connected();
                    } else {
                        return;
                    }
                } catch (final IOException exception) {
                    connectionListener.connectionFailed(exception);
                }
            } else {
                return;
            }
        }
        if (!channel.isConnected()) {
            connectionListener.disconnected();
            return;
        }
        doReadWriteActions();
    }

    private void doReadWriteActions() throws IOException {
        if (isWritable()) {
            outputBuffer.send(channel()::write);
        }
        if (isReadable()) {
            if (channel().read(inputBuffer) < 0) {
                connectionListener.disconnected();
                channel.close();
            }
            inputBuffer.flip();
            receiver.accept(inputBuffer);
        }
    }

    protected SocketChannel channel() {
        return channel;
    }

    private void updateInterests() {
        if (selectionKey != null && selectionKey.isValid()) {
            //noinspection MagicConstant
            selectionKey.interestOps(interestOps());
        }
    }

    private boolean isWritable() {
        return selectionKey.isWritable();
    }

    private boolean isReadable() {
        return interestedInRead() && selectionKey.isReadable();
    }

    protected boolean interestedInConnect() {
        return channel().isConnectionPending();
    }

    private boolean interestedInRead() {
        return !sendAllBeforeReading || !interestedInWrite();
    }

    private boolean interestedInWrite() {
        return outputBuffer.hasRemaining();
    }
}
