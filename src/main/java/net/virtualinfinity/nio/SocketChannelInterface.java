package net.virtualinfinity.nio;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;

/**
 * @author <a href='mailto:Daniel@coloraura.com'>Daniel Pitts</a>
 */
public interface SocketChannelInterface extends Closeable{
    SelectableChannel selectableChannel();
    boolean isOpen();
    boolean isConnectionPending();
    boolean finishConnect() throws IOException;
    boolean isConnected();
    int write(ByteBuffer byteBuffer) throws IOException;
    int read(ByteBuffer inputBuffer) throws IOException;
    void close() throws IOException;
    void configureBlocking(boolean blocking) throws IOException;
    boolean connect(SocketAddress address) throws IOException;
}
