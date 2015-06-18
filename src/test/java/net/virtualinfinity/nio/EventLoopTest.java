package net.virtualinfinity.nio;

import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href='mailto:Daniel@coloraura.com'>Daniel Pitts</a>
 */
public class EventLoopTest {

    @Test()
    public void testInvokeLater() throws IOException {
        final EventLoop eventLoop = new EventLoop();
        final boolean[] ran = new boolean[1];
        eventLoop.invokeLater(() -> {
            ran[0] = true;
            close(eventLoop);
        });
        eventLoop.run();
        assertTrue(ran[0]);
    }

    @Test
    public void testInvokeAfterDelay() throws IOException {
        final EventLoop eventLoop = new EventLoop();
        final boolean[] ran = new boolean[1];
        final long[] time = new long[1];
        final long start = System.nanoTime();
        eventLoop.invokeAfter(() -> {
            ran[0] = true;
            time[0] = System.nanoTime();
            close(eventLoop);
        }, 50, TimeUnit.MILLISECONDS);
        eventLoop.run();
        assertTrue(ran[0]);
        final long timeInMillis = TimeUnit.MILLISECONDS.convert(time[0] - start, TimeUnit.NANOSECONDS);
        assertTrue("Should have taken around 50ms. Took " + timeInMillis, timeInMillis >= 50 && timeInMillis <= 65);
    }

    @Test
    public void testInvokeAfterAbsoluteTime() throws IOException {
        final EventLoop eventLoop = new EventLoop();
        final boolean[] ran = new boolean[1];
        final long[] time = new long[1];
        final long start = System.nanoTime();
        eventLoop.invokeAfter(() -> {
            ran[0] = true;
            time[0] = System.nanoTime();
            close(eventLoop);
        }, new Date(System.currentTimeMillis() + 50));
        eventLoop.run();
        assertTrue(ran[0]);
        final long timeInMillis = TimeUnit.MILLISECONDS.convert(time[0] - start, TimeUnit.NANOSECONDS);
        assertTrue("Should have taken around 50ms. Took " + timeInMillis, timeInMillis >= 50 && timeInMillis <= 65);
    }

    @Test
    public void testSelector() throws IOException {
        final EventLoop eventLoop = new EventLoop();
        final boolean[] ran = new boolean[2];
        final long[] time = new long[1];
        final long start = System.nanoTime();
        final Pipe pipe = Pipe.open();
        new Thread(() -> {
            try {
                Thread.sleep(50);
                final ByteBuffer buf = ByteBuffer.allocate(10);
                buf.put(new byte[] { 1, 2, 3, 5, 8 });
                pipe.sink().write(buf);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        final Pipe.SourceChannel source = pipe.source();
        source.configureBlocking(false);
        eventLoop.registerHandler(source, new SelectionKeyActions() {
            private SelectionKeyInterface selectionKey;

            @Override
            public void setSelectionKey(SelectionKeyInterface selectionKey) {
                this.selectionKey = selectionKey;
                ran[1] = selectionKey.isReadable();
            }

            @Override
            public int interestOps() {
                return SelectionKey.OP_READ;
            }

            @Override
            public void selected() {
                ran[0] = true;
                ran[1] = selectionKey.isReadable();
                time[0] = System.nanoTime();
                close(eventLoop);
            }
        });
        assertFalse("Should not be readable for this test to be valid.", ran[1]);
        eventLoop.run();
        assertTrue(ran[0]);
        assertTrue("Should be readable in selected() call", ran[1]);
        final long timeInMillis = TimeUnit.MILLISECONDS.convert(time[0] - start, TimeUnit.NANOSECONDS);
        assertTrue("Should have taken around 50ms. Took " + timeInMillis, timeInMillis >= 50 && timeInMillis <= 65);
    }


    private void close(EventLoop eventLoop) {
        try {
            eventLoop.close();
        } catch (IOException e) {
        }
    }

}