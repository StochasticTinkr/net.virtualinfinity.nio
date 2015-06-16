package net.virtualinfinity.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;

/**
 * Provides an asynchronous way to initiate a new connection.
 *
 * @author <a href='mailto:Daniel@coloraura.com'>Daniel Pitts</a>
 */
public class ConnectionInitiator {
    private final AsynchronousAddressResolver addressResolver;

    public ConnectionInitiator() {
        this(new AsynchronousAddressResolver());
    }

    public ConnectionInitiator(AsynchronousAddressResolver asynchronousAddressResolver) {
        this.addressResolver = asynchronousAddressResolver;
    }

    /**
     * Non-Blocking open socket and connect to the given host/port.
     * This method will look up the hostname using the given {@link AsynchronousAddressResolver}, and once that resolution
     * is complete, it will register a {@link SocketSelectionActions} instance configured with the newly opened channel.
     *
     * @param eventLoop The event loop that will manage the connection.
     * @param hostname The host name to connect to.
     * @param port The port to connect to.
     * @param connectionListener The connection listener to be notified about connections.
     * @param receiver The object that will receive data read from the socket
     * @param outputBuffer The output buffer that will be sent over the socket.
     * @param sendAllBeforeReading Whether or not the outputBuffer should be fully flushed before new input is processed.
     * @param inputBufferSize The input buffer size.
     */
    public void connect(EventLoop eventLoop, String hostname, int port, ConnectionListener connectionListener, ByteBufferConsumer receiver, OutputBuffer outputBuffer, boolean sendAllBeforeReading, int inputBufferSize) {
        connect(eventLoop, hostname, port, connectionListener, receiver, outputBuffer, sendAllBeforeReading, inputBufferSize,
            socketChannel -> {
            });
    }

    /**
     * Non-Blocking open socket and connect to the given host/port.
     * This method will look up the hostname using the given {@link AsynchronousAddressResolver}, and once that resolution
     * is complete, it will register a {@link SocketSelectionActions} instance configured with the newly opened channel.
     *
     * @param eventLoop The event loop that will manage the connection.
     * @param hostname The host name to connect to.
     * @param port The port to connect to.
     * @param connectionListener The connection listener to be notified about connections.
     * @param receiver The object that will receive data read from the socket
     * @param outputBuffer The output buffer that will be sent over the socket.
     * @param sendAllBeforeReading Whether or not the outputBuffer should be fully flushed before new input is processed.
     * @param inputBufferSize The input buffer size.
     * @param socketConfigurer Configures the socket before it is connected.
     */
    public void connect(EventLoop eventLoop, String hostname, int port, ConnectionListener connectionListener, ByteBufferConsumer receiver, OutputBuffer outputBuffer, boolean sendAllBeforeReading, int inputBufferSize, ChannelConfigurer<SocketChannel> socketConfigurer) {
        connect(eventLoop, hostname, port, connectionListener, socketConfigurer, socketChannel -> connectToSocketSelectionActions(eventLoop, socketChannel, connectionListener, receiver, outputBuffer, sendAllBeforeReading, inputBufferSize));
    }

    /**
     * Non-Blocking open socket and connect to the given host/port.
     * This method will look up the hostname using the given {@link AsynchronousAddressResolver}, and once that resolution
     * is complete, send the socket to the connectionInitiated object.  Note, it is likely that the socket is not
     * fully pending (eg. {@link SocketChannel#isConnectionPending()} is true). You will need to wait for the connection
     * process to complete, and then call finishConnecting.
     *
     * @param eventLoop The event loop that will manage the connection.
     * @param hostname The host to connect to.
     * @param port The port to connect to.
     * @param connectionListener The connection listener to be notified about connections.
     * @param socketConfigurer Configures the socket before it is connected.
     * @param connectionInitiated The object to be notified after the connection has been initiated.
     */
    public void connect(EventLoop eventLoop, String hostname, int port, ConnectionListener connectionListener, ChannelConfigurer<SocketChannel> socketConfigurer, Consumer<SocketChannel> connectionInitiated) {
        addressResolver.lookupInetSocketAddress(eventLoop, hostname, port, address -> {
            try {
                checkAddress(address);
                final SocketChannel channel = SocketChannel.open();
                socketConfigurer.configure(channel);
                channel.configureBlocking(false);
                channel.connect(address);
                connectionListener.connecting();
                connectionInitiated.accept(channel);
            } catch (IOException e) {
                connectionListener.connectionFailed(e);
            }
        });
    }

    private void connectToSocketSelectionActions(EventLoop eventLoop, SocketChannel socketChannel, ConnectionListener connectionListener, ByteBufferConsumer receiver, OutputBuffer outputBuffer, boolean sendAllBeforeReading, int inputBufferSize) {
        try {
            new SocketSelectionActions(socketChannel,
                connectionListener,
                receiver,
                outputBuffer, inputBufferSize, sendAllBeforeReading
            ).register(eventLoop);
        } catch (final ClosedChannelException e) {
            connectionListener.connectionFailed(e);
        }
    }

    /**
     * Begins listening for incoming connections, binding to the given port
     * @param eventLoop The event loop that will manage the connections.
     * @param hostname The address to bind to. May be null, which means any local.
     * @param port The port to bind on.
     * @param backlog The backlog to pass to {@link ServerSocketChannel#bind(SocketAddress, int)}
     * @param configurer The configurer of the server socket.
     * @param incomingConnection the handler of incoming connections.
     * @param exceptionHandler The handler of exceptions.
     */
    public void bind(EventLoop eventLoop, String hostname, int port, int backlog, ChannelConfigurer<ServerSocketChannel> configurer, Consumer<SocketChannel> incomingConnection, ExceptionHandler<IOException> exceptionHandler) {
        addressResolver.lookupInetSocketAddress(eventLoop, hostname, port, address -> {
            doBind(eventLoop, backlog, configurer, incomingConnection, exceptionHandler, address);
        });
    }

    private static void doBind(EventLoop eventLoop, int backlog, ChannelConfigurer<ServerSocketChannel> configurer, Consumer<SocketChannel> incomingConnection, ExceptionHandler<IOException> exceptionHandler, InetSocketAddress address) {
        try {
            final ServerSocketChannel channel = ServerSocketChannel.open();
            configurer.configure(channel);
            channel.bind(address, backlog);
            eventLoop.registerHandler(channel, SelectionKey.OP_ACCEPT, () -> {
                final SocketChannel accept = channel.accept();
                accept.configureBlocking(false);
                incomingConnection.accept(accept);
            });

        } catch (IOException e) {
            try {
                exceptionHandler.handleException(null, e);
            } catch (IOException e1) {
                Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e1);
            }
        }
    }

    private static void checkAddress(InetSocketAddress address) throws UnknownHostException {
        if (address.isUnresolved()) {
            throw new UnknownHostException(address.getHostString());
        }
    }
}
