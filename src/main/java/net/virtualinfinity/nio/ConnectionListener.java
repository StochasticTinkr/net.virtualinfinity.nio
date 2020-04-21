package net.virtualinfinity.nio;

import java.io.IOException;

/**
 * Listener of the lifecycle events of a connection.
*/
public interface ConnectionListener {
    /**
     * Called when the connection is initiating.
     */
    void connecting();

    /**
     * Called when the connection has been established.
     */
    void connected();

    /**
     * Called when the connection has failed.
     * @param e the exception which caused the failure
     */
    void connectionFailed(IOException e);

    /**
     * Called when the connection has been disconnected.
     */
    void disconnected();
}
