package net.virtualinfinity.nio;

import java.io.IOException;

/**
 * A handler that will be called when the selected channel has readyOps.
*/
@FunctionalInterface
public interface SelectionKeyHandler {
    /**
     * Called when the channel is ready for a requested operation.
     * @throws IOException
     */
    void selected() throws IOException;
}
