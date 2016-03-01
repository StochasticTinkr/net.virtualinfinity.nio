package net.virtualinfinity.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;

/**
 * @author <a href='mailto:Daniel@coloraura.com'>Daniel Pitts</a>
 */
public interface ServerSocketChannelInterface {
    void bind(InetSocketAddress address, int backlog) throws IOException;
    SocketChannelInterface accept() throws IOException;
    SelectableChannel selectableChannel();
}
