package net.virtualinfinity.nio;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Provides "asynchronous" lookup for host names.  Java unfortunately doesn't provide a direct way to do this through NIO,
 * so we have to resort to multiple threads *or* implementing our own DNS client.
 *
 * This version of this class uses an Executor to offload the work to another thread, future versions *may* use implement
 * the DNS lookup protocol directly.
 *
 * @author <a href='mailto:Daniel@coloraura.com'>Daniel Pitts</a>
 */
public class AsynchronousAddressResolver {
    private final Executor service;

    /**
     * Constructs a resolver that uses a maximum of 15 threads.
     * @see #AsynchronousAddressResolver(int)
     */
    public AsynchronousAddressResolver() {
        this(15);
    }

    /**
     * Constructs a resolver that uses a maximum number of threads as given.
     * Some day, this constructor may be deprecated, and a non-threaded implementation may be created.
     *
     * @param maximumConcurrency the maximum number of threads.
     */
    public AsynchronousAddressResolver(int maximumConcurrency) {
        service = new ThreadPoolExecutor(1, maximumConcurrency, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    /**
     * Asynchronously look up the given hostname, and create a InetSockAddress for it (with the given port).
     *
     * The resolved address will be sent to the consumer from within the {@link EventLoop#run} method.
     *
     * This implementation offloads the work to a thread-pool, but future version may utilize the EventLoop for
     * an NIO based BIND protocol implementation.
     *
     * @param eventLoop The event loop
     * @param hostname The hostname to look up.
     * @param port The port number to pass to the InetSocketAddress.
     * @param completed The function to call when the lookup is complete.
     */
    public void lookupInetSocketAddress(EventLoop eventLoop, String hostname, int port, Consumer<InetSocketAddress> completed) {
        service.execute(() -> {
            final InetSocketAddress resolved = hostname == null ?
                new InetSocketAddress(port) : new InetSocketAddress(hostname, port);
            eventLoop.invokeLater(() -> completed.accept(resolved));
        });
    }
    /**
     * Asynchronously look up InetAddress[] objects for the given hostname.
     *
     * The resolved address will be sent to the consumer from within the {@link EventLoop#run} method.
     *
     * This implementation offloads the work to a thread-pool, but future version may utilize the EventLoop for
     * an NIO based BIND protocol implementation.
     *
     * @param eventLoop The event loop
     * @param hostname The hostname to look up.
     * @param completed The function to call when the lookup is complete.
     * @param onUnknown The function to call when lookup fails.
     */
    public void lookupInetAddress(EventLoop eventLoop, String hostname, Consumer<InetAddress[]> completed, Runnable onUnknown) {
        service.execute(() -> {
            try {
                final InetAddress[] resolved = InetAddress.getAllByName(hostname);
                eventLoop.invokeLater(() -> completed.accept(resolved));
            } catch (UnknownHostException e) {
                eventLoop.invokeLater(onUnknown);
            }
        });
    }

}
