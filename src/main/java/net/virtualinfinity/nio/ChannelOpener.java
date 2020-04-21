package net.virtualinfinity.nio;

import java.io.IOException;
import java.nio.channels.Channel;

/**
 * Provides a method for opening a new channel.
*/
@FunctionalInterface
public interface ChannelOpener<T extends Channel> {
    /**
     * Open the channel if possible.
     * @return the opened channel
     * @throws IOException if an I/O error occurs.
     */
    T open() throws IOException;
}
