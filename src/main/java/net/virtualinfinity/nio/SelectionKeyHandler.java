package net.virtualinfinity.nio;

import java.io.IOException;

/**
 * @author <a href='mailto:Daniel@coloraura.com'>Daniel Pitts</a>
 */
@FunctionalInterface
public interface SelectionKeyHandler {
    void selected() throws IOException;
}
