package net.virtualinfinity.nio;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Provides the functionality of an event loop that can listen to {@link SelectableChannel}, as well as execute events
 * at some point in the future.
 *
 * The { @link EventLoop#run() } method does the work.  This class is thread safe.
 *
 * @author <a href='mailto:Daniel@coloraura.com'>Daniel Pitts</a>
 */
public class EventLoop implements Closeable {
    private final Selector selector;
    private final ExceptionHandler<IOException> handler;
    private final Queue<Event> events = new PriorityQueue<>();
    private volatile boolean running;

    private EventLoop(Selector selector, ExceptionHandler<IOException> handler) {
        this.selector = selector;
        this.handler = handler == null ? (key, e) -> { throw e; } : handler;
    }

    /**
     * Creates an EventLoop with the given exception handler.  If the handler is null, the default exception handler is
     * used, which will re-through the exception.  This is generally not the best behavior, and a more suitable exception
     * handler should be installed that is specific to your use.
     *
     * @param handler the exception handler, or null to use the default handler.
     *
     * @throws IOException if there is an error opening a selector.
     */
    public EventLoop(ExceptionHandler<IOException> handler) throws IOException {
        this(Selector.open(), handler);
    }

    /**
     * Creates an EventLoop instance with the default exception handler.
     *
     * @throws IOException if there is an error opening a selector.
     *
     * @see #EventLoop(ExceptionHandler)
     */
    public EventLoop() throws IOException {
        this(Selector.open(), null);
    }

    /**
     * Runs the event loop, dispatching events and listening to {@link SelectableChannel}
     * @throws IOException
     */
    public void run() throws IOException {
        synchronized (this) {
            if (running) {
                throw new IllegalStateException("Event loop is already running, and is not thread safe");
            }
            running = true;
        }
        try {
            while (selector.isOpen()) {
                final Event nextEvent = executePendingEvents();
                if (nextEvent != null) {
                    select(nextEvent.timeRemaining(TimeUnit.MILLISECONDS));
                } else {
                    select(0);
                }
                executeSelected();
            }
        } finally {
            synchronized (this) {
                running = false;
            }
        }
    }

    /**
     * Calls select on the selector, delegating exception management to the exception handler.
     *
     * @param timeout the timeout parameter to the {@link Selector#select(long)} call.
     *
     * @throws IOException if there is an exception thrown by the exception handler.
     */
    private void select(long timeout) throws IOException {
        try {
            selector.select(timeout);
        } catch (IOException e) {
            handler.handleException(null, e);
        }
    }

    /**
     * Loops through all the selected keys, and executes there Runnable or selected methods.
     * This method delegates exception management to the exception handler.
     *
     * @throws IOException if there is an exception thrown by the exception handler.
     */
    private void executeSelected() throws IOException {
        for (final Iterator<SelectionKey> iterator = selector.selectedKeys().iterator(); iterator.hasNext(); ) {
            final SelectionKey key = iterator.next();
            try {
                final Object attachment = key.attachment();
                if (attachment instanceof SelectionKeyHandler) {
                    ((SelectionKeyHandler) attachment).selected();
                } else if (attachment instanceof Runnable) {
                    ((Runnable)attachment).run();
                }
                iterator.remove();
            } catch (IOException e) {
                handler.handleException(key, e);
            }
        }
    }

    /**
     * Dispatches an due events, and returns the timeout until the next event.
     *
     * @return the time in milliseconds until the next event, or 0 if there are no events.
     */
    private Event executePendingEvents() {
        final Collection<Runnable> toRun = new ArrayList<>();
        final Event nextEvent = getReadyToRun(toRun);
        toRun.forEach(Runnable::run);
        return nextEvent;
    }

    private Event getReadyToRun(Collection<Runnable> toRun) {
        synchronized (events) {
            while (events.peek() != null) {
                final Event nextEvent = events.peek();
                final long nextEventTime = nextEvent.timeRemaining(TimeUnit.MILLISECONDS);
                if (nextEventTime > 0) {
                    return nextEvent;
                }
                // It's ready to run, so run it outside of the synchronized block.
                toRun.add(events.poll());
            }
        }

        return null;
    }

    /**
     * Enqueue the given runnable at some time in the future. The Runnable will be executed from the
     * thread that called {@link #run()} on this object.
     *
     * @param runnable the runnable to execute on the event thread.
     */
    public void invokeLater(Runnable runnable) {
        invokeAfter(runnable, 0, TimeUnit.NANOSECONDS);
    }

    /**
     * Enqueue the given runnable to run after a specific point of time.  The Runnable will be executed from the
     * thread that called {@link #run()} on this object.
     *
     * @param runnable the command to run
     * @param absoluteTime the earliest time to run it.
     */
    public void invokeAfter(Runnable runnable, Date absoluteTime) {
        invokeAfter(runnable, absoluteTime.getTime() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Enqueue the given runnable to run after a specific point of time.  The Runnable will be executed from the
     * thread that called {@link #run()} on this object.
     *
     * @param runnable the command to run
     * @param timeInFuture the amount of time in the future
     * @param timeInFutureUnit the unit that the timeInFuture value is of.
     */
    public void invokeAfter(Runnable runnable, long timeInFuture, TimeUnit timeInFutureUnit) {
        final Event e = new Event(System.nanoTime() + TimeUnit.NANOSECONDS.convert(timeInFuture, timeInFutureUnit), runnable);
        synchronized (events) {
            events.add(e);
            selector.wakeup();
        }
    }

    /**
     * Registers handlers that will be invoked when the channel is selected.  The handler will be invoked from the event
     * loop.
     *
     * @param channel The channel.
     * @param handlers The handlers for the selected channel.
     *
     * @throws ClosedChannelException
     */
    public void registerHandler(SelectableChannel channel, SelectionKeyActions handlers) throws ClosedChannelException {
      handlers.setSelectionKey(doRegister(channel, handlers.interestOps(), handlers));
    }

    /**
     * Registers handlers that will be invoked when the channel is selected.  The handler will be invoked from the event
     * loop.
     *
     * @param channel The channel of interest
     * @param ops The valid operations. {@link SelectionKey}
     * @param handler The handler for the selected channel.
     *
     * @throws ClosedChannelException
     *
     * @see SelectableChannel#register(Selector, int)
     */
    public void registerHandler(SelectableChannel channel, int ops, SelectionKeyHandler handler) throws ClosedChannelException {
        doRegister(channel, ops, handler);
    }

    /**
     * Registers the channel with the selector.
     *
     * @param channel the channel to register
     * @param ops the interestOps.
     * @param handler the handler.
     *
     * @return The corresponding SelectionKey.
     *
     * @throws ClosedChannelException if the channel is closed.
     */
    private SelectionKey doRegister(SelectableChannel channel, int ops, SelectionKeyHandler handler) throws ClosedChannelException {
        return channel.register(selector, ops, handler);
    }

    /**
     * Closes the selector, causing the event loop to terminate.
     */
    @Override
    public void close() throws IOException {
        selector.close();
    }

    /**
     * Priority queue event item.
     */
    private static class Event implements Comparable<Event>, Runnable {
        private final long desiredTimeNanos;
        private final Runnable handler;

        public Event(long desiredTimeNanos, Runnable handler) {
            this.desiredTimeNanos = desiredTimeNanos;
            this.handler = handler;
        }

        @Override
        public int compareTo(Event event) {
            return Long.compare(timeRemainingNanos(), event.timeRemainingNanos());
        }

        @Override
        public void run() {
            handler.run();
        }

        public long timeRemaining(TimeUnit time) {
            return time.convert(timeRemainingNanos(), TimeUnit.NANOSECONDS);
        }

        public long timeRemainingNanos() {
            return desiredTimeNanos - System.nanoTime();
        }
    }
}
