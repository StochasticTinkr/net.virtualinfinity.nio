package net.virtualinfinity.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * @author <a href='mailto:Daniel@coloraura.com'>Daniel Pitts</a>
 */
public class ServerSocketChannelWrapper implements ServerSocketChannelInterface {
    public static final ChannelProvider<ServerSocketChannelInterface> PROVIDER = () -> new ServerSocketChannelWrapper(ServerSocketChannel.open());
    private final ServerSocketChannel channel;

    public ServerSocketChannelWrapper(ServerSocketChannel channel) {
        this.channel = channel;
    }

    @Override
    public void bind(InetSocketAddress address, int backlog) throws IOException {
        channel.bind(address, backlog);
    }

    @Override
    public SocketChannelInterface accept() throws IOException {
        final SocketChannel accept = channel.accept();
        return accept != null ? new SocketChannelWrapper(accept) : null;
    }

    @Override
    public SelectableChannel selectableChannel() {
        return channel;
    }
}
