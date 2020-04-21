package net.virtualinfinity.nio;

import java.nio.channels.SelectionKey;

/**
 * An exception handler strategy for exceptions encountered in the EventLoop.
*/
public interface ExceptionHandler<T extends Throwable> {
    /**
     * Called when an exception happens in the EventLoop.
     *
     * @param key The SelectionKey being processed when the exception was thrown, or null if the exception occurred during the select() call.
     * @param exception The thrown exception.
     *
     * @throws T if that is the desired behavior.
     */
    void handleException(SelectionKey key, T exception) throws T;
}
