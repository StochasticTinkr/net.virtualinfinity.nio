package net.virtualinfinity.nio;

import java.io.IOException;
import java.nio.channels.Channel;

/**
 * @author <a href='mailto:Daniel@coloraura.com'>Daniel Pitts</a>
 */
public interface ChannelConfigurer<T extends Channel> {

    void configure(T channel) throws IOException;
}
