package net.virtualinfinity.nio;

import java.nio.channels.SelectionKey;

/**
 * A selection key handler that can specify what its interested in.
 *
 * @author <a href='mailto:Daniel@coloraura.com'>Daniel Pitts</a>
 */
public interface SelectionKeyActions extends SelectionKeyHandler {
    /**
     * The interested ops for this listener.
     *
     * @return the interestOps bitset.
     *
     * @see SelectionKey#interestOps(int)
     */
    int interestOps();

    /**
     * Called to inform the handler of the the selection key.
     *
     * @param selectionKey the selection key.
     */
    default void setSelectionKey(SelectionKey selectionKey) {
    }
}
