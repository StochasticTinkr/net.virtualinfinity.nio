package net.virtualinfinity.nio;

import java.io.IOException;

/**
 * @author <a href='mailto:Daniel@coloraura.com'>Daniel Pitts</a>
 */
public interface ChannelProvider<T> {
    T open() throws IOException;
}
