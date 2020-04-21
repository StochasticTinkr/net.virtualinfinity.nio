package net.virtualinfinity.nio;

import java.nio.channels.SelectionKey;

/**
 * A selection key handler that can specify what its interested in.
*/
public interface SelectionKeyActions extends SelectionKeyHandler {
    /**
     * The operations this listener is interested in being notified of.
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
