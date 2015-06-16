package net.virtualinfinity.nio;

import java.io.IOException;

/**
 * @author <a href='mailto:Daniel@coloraura.com'>Daniel Pitts</a>
 */
public interface ConnectionListener {
    void connecting();
    void connected();
    void connectionFailed(IOException e);
    void disconnected();
}
