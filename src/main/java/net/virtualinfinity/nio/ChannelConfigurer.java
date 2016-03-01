package net.virtualinfinity.nio;

import java.io.IOException;

/**
 * @author <a href='mailto:Daniel@coloraura.com'>Daniel Pitts</a>
 */
public interface ChannelConfigurer<T> {
    void configure(T channel) throws IOException;
}
