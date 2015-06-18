package net.virtualinfinity.nio;

/**
 * @author <a href='mailto:Daniel@coloraura.com'>Daniel Pitts</a>
 */
public interface SelectionKeyInterface {
    boolean isConnectable();
    boolean isWritable();
    boolean isAcceptable();
    boolean isReadable();
    boolean isValid();
    void interestOps(int ops);
    void cancel();
}
