package net.virtualinfinity.nio;

import java.nio.channels.SelectionKey;

/**
 * @author <a href='mailto:Daniel@coloraura.com'>Daniel Pitts</a>
 */
public interface ExceptionHandler<T extends Throwable> {
    void handleException(SelectionKey key, T e) throws T;
}
