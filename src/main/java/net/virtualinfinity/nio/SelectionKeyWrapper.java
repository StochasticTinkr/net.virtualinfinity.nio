package net.virtualinfinity.nio;

import java.nio.channels.SelectionKey;

/**
 * @author <a href='mailto:Daniel@coloraura.com'>Daniel Pitts</a>
 */
public class SelectionKeyWrapper implements SelectionKeyInterface {
    private final SelectionKey selectionKey;

    public SelectionKeyWrapper(SelectionKey selectionKey) {
        this.selectionKey = selectionKey;
    }

    @Override
    public boolean isConnectable() {
        return selectionKey.isConnectable();
    }

    @Override
    public boolean isWritable() {
        return selectionKey.isWritable();
    }

    @Override
    public boolean isAcceptable() {
        return selectionKey.isAcceptable();
    }

    @Override
    public boolean isReadable() {
        return selectionKey.isReadable();
    }

    @Override
    public boolean isValid() {
        return selectionKey.isValid();
    }

    @Override
    public void interestOps(int ops) {
        selectionKey.interestOps(ops);
    }

    @Override
    public void cancel() {
        selectionKey.cancel();
    }
}
