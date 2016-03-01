package net.virtualinfinity.nio;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author <a href='mailto:Daniel@coloraura.com'>Daniel Pitts</a>
 */
public class SocketChannelWrapper implements SocketChannelInterface {
    private static class DefaultChannelProvider implements ChannelProvider<SocketChannelInterface> {
        @Override
        public SocketChannelInterface open() throws IOException {
            return new SocketChannelWrapper(SocketChannel.open());
        }
    }

    public static final ChannelProvider<SocketChannelInterface> PROVIDER = new DefaultChannelProvider();

    private final SocketChannel channel;

    public SocketChannelWrapper(SocketChannel channel) {
        this.channel = channel;
    }

    @Override
    public SocketChannel selectableChannel() {
        return channel;
    }

    @Override
    public boolean isOpen() {
        return channel.isOpen();
    }

    @Override
    public boolean isConnectionPending() {
        return channel.isConnectionPending();
    }

    @Override
    public boolean finishConnect() throws IOException {
        return channel.finishConnect();
    }

    @Override
    public boolean isConnected() {
        return channel.isConnected();
    }

    @Override
    public int write(ByteBuffer byteBuffer) throws IOException {
        return channel.write(byteBuffer);
    }

    @Override
    public int read(ByteBuffer inputBuffer) throws IOException {
        return channel.read(inputBuffer);
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    @Override
    public void configureBlocking(boolean blocking) throws IOException {
        channel.configureBlocking(blocking);
    }

    public boolean connect(SocketAddress remote) throws IOException {
        return channel.connect(remote);
    }
}
