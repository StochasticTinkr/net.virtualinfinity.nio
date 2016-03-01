package net.virtualinfinity.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.util.function.Consumer;

/**
 * Provides an asynchronous way to initiate a new connection.
 *
 * @author <a href='mailto:Daniel@coloraura.com'>Daniel Pitts</a>
 */
public final class ConnectionInitiator {
    private final AsynchronousAddressResolver addressResolver;
    private final ChannelProvider<SocketChannelInterface> socketChannelProvider;
    private final ChannelProvider<ServerSocketChannelInterface> serverSocketChannelProvider;

    public ConnectionInitiator() {
        this(new AsynchronousAddressResolver());
    }

    public ConnectionInitiator(AsynchronousAddressResolver asynchronousAddressResolver) {
        this(asynchronousAddressResolver, SocketChannelWrapper.PROVIDER, ServerSocketChannelWrapper.PROVIDER);
    }

    public ConnectionInitiator(AsynchronousAddressResolver asynchronousAddressResolver, ChannelProvider<SocketChannelInterface> socketChannelProvider, ChannelProvider<ServerSocketChannelInterface> serverSocketChannelProvider) {
        this.addressResolver = asynchronousAddressResolver;
        this.socketChannelProvider = socketChannelProvider;
        this.serverSocketChannelProvider = serverSocketChannelProvider;
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
    public void connect(final EventLoop eventLoop, final String hostname, final int port, final ConnectionListener connectionListener, final ByteBufferConsumer receiver, final OutputBuffer outputBuffer, final boolean sendAllBeforeReading, final int inputBufferSize) {
        connect(eventLoop, hostname, port, connectionListener, new Consumer<SocketChannelInterface>() {
            @Override
            public void accept(SocketChannelInterface socketChannel) {
                connectToSocketSelectionActions(eventLoop, socketChannel, connectionListener, receiver, outputBuffer, sendAllBeforeReading, inputBufferSize);
            }
        });
    }

    /**
     * Non-Blocking open socket and connect to the given host/port.
     * This method will look up the hostname using the given {@link AsynchronousAddressResolver}, and once that resolution
     * is complete, send the socket to the connectionInitiated object.  Note, it is likely that the socket is not
     * fully pending (eg. {@link SocketChannelInterface#isConnectionPending()} is true). You will need to wait for the connection
     * process to complete, and then call finishConnecting.
     *
     * @param eventLoop The event loop that will manage the connection.
     * @param hostname The host to connect to.
     * @param port The port to connect to.
     * @param connectionListener The connection listener to be notified about connections.
     * @param connectionInitiated The object to be notified after the connection has been initiated.
     */
    public void connect(final EventLoop eventLoop, final String hostname, final int port, final ConnectionListener connectionListener, final Consumer<SocketChannelInterface> connectionInitiated) {
        addressResolver.lookupInetSocketAddress(eventLoop, hostname, port, new Consumer<InetSocketAddress>() {
            @Override
            public void accept(InetSocketAddress address) {
                try {
                    checkAddress(address);
                    final SocketChannelInterface channel = socketChannelProvider.open();
                    channel.configureBlocking(false);
                    channel.connect(address);
                    connectionListener.connecting();
                    connectionInitiated.accept(channel);
                } catch (IOException e) {
                    connectionListener.connectionFailed(e);
                }
            }
        });
    }

    private void connectToSocketSelectionActions(final EventLoop eventLoop, final SocketChannelInterface socketChannel, final ConnectionListener connectionListener, final ByteBufferConsumer receiver, final OutputBuffer outputBuffer, final boolean sendAllBeforeReading, final int inputBufferSize) {
        try {
            SocketSelectionActions socketSelectionActions = new SocketSelectionActions(socketChannel, connectionListener, receiver, outputBuffer, inputBufferSize, sendAllBeforeReading);
            socketSelectionActions.register(eventLoop);
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
     * @param incomingConnection the handler of incoming connections.
     * @param exceptionHandler The handler of exceptions.
     */
    public void bind(final EventLoop eventLoop, final String hostname, final int port, final int backlog, final Consumer<SocketChannelInterface> incomingConnection, final ExceptionHandler<IOException> exceptionHandler) {
        addressResolver.lookupInetSocketAddress(eventLoop, hostname, port, new Consumer<InetSocketAddress>() {
            @Override
            public void accept(InetSocketAddress address) {
                doBind(eventLoop, backlog, incomingConnection, exceptionHandler, address);
            }
        });
    }

    private void doBind(final EventLoop eventLoop, final int backlog, final Consumer<SocketChannelInterface> incomingConnection, final ExceptionHandler<IOException> exceptionHandler, final InetSocketAddress address) {
        try {
            final ServerSocketChannelInterface channel = serverSocketChannelProvider.open();
            channel.bind(address, backlog);
            eventLoop.registerHandler(channel.selectableChannel(), SelectionKey.OP_ACCEPT, new SelectionKeyHandler() {
                @Override
                public void selected() throws IOException {
                    final SocketChannelInterface accept = channel.accept();
                    if (accept != null) {
                        accept.configureBlocking(false);
                        incomingConnection.accept(accept);
                    }
                }
            });
        } catch (final IOException e) {
            try {
                exceptionHandler.handleException(null, e);
            } catch (final IOException e1) {
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
