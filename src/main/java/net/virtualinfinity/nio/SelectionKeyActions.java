package net.virtualinfinity.nio;

import java.nio.channels.SelectionKey;

/**
 * A selection key handler that can specify what its interested in.
 *
 * @author <a href='mailto:Daniel@coloraura.com'>Daniel Pitts</a>
 */
public interface SelectionKeyActions extends SelectionKeyHandler {
    int interestOps();
    default void setSelectionKey(SelectionKey selectionKey) {
    }
}
