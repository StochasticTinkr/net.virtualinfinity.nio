package net.virtualinfinity.nio;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.channels.Selector;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventLoopTest {
    @Mock
    Runnable mockRunnable;

    @Mock
    Selector mockSelector;

    @Mock
    ExceptionHandler<IOException> mockExceptionHandler;

    @Mock
    private LongSupplier mockNanoNowSupplier;

    EventLoop eventLoop;

    @BeforeEach
    void setUp() {
        eventLoop = new EventLoop(mockSelector, mockExceptionHandler, mockNanoNowSupplier);
    }

    @Test
    public void testInvokeLater() throws IOException {
        when(mockSelector.isOpen()).thenReturn(true, false);
        eventLoop.invokeLater(mockRunnable);
        eventLoop.run();
        verify(mockRunnable).run();
    }


    @Test
    public void testInvokeAfter_delay() throws IOException {
        when(mockSelector.isOpen()).thenReturn(true, true, false);
        when(mockNanoNowSupplier.getAsLong()).thenReturn(0L,
                TimeUnit.NANOSECONDS.convert(47, TimeUnit.MILLISECONDS),
                TimeUnit.NANOSECONDS.convert(47, TimeUnit.MILLISECONDS),
                TimeUnit.NANOSECONDS.convert(50, TimeUnit.MILLISECONDS));
        eventLoop.invokeAfter(mockRunnable, 50, TimeUnit.MILLISECONDS);
        eventLoop.run();
        verify(mockSelector).select(3);
        verify(mockRunnable).run();
    }

//
//    @Test
//    public void testSelector() throws IOException {
//        final EventLoop eventLoop = new EventLoop();
//        final boolean[] ran = new boolean[2];
//        final long[] time = new long[1];
//        final long start = System.nanoTime();
//        final Pipe pipe = Pipe.open();
//        new Thread(() -> {
//            try {
//                Thread.sleep(50);
//                final ByteBuffer buf = ByteBuffer.allocate(10);
//                buf.put(new byte[] { 1, 2, 3, 5, 8 });
//                pipe.sink().write(buf);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }).start();
//        final Pipe.SourceChannel source = pipe.source();
//        source.configureBlocking(false);
//        eventLoop.registerHandler(source, new SelectionKeyActions() {
//            private SelectionKey selectionKey;
//
//            @Override
//            public void setSelectionKey(SelectionKey selectionKey) {
//                this.selectionKey = selectionKey;
//                ran[1] = selectionKey.isReadable();
//            }
//
//            @Override
//            public int interestOps() {
//                return SelectionKey.OP_READ;
//            }
//
//            @Override
//            public void selected() {
//                ran[0] = true;
//                ran[1] = selectionKey.isReadable();
//                time[0] = System.nanoTime();
//                close(eventLoop);
//            }
//        });
//        assertFalse("Should not be readable for this test to be valid.", ran[1]);
//        eventLoop.run();
//        assertTrue(ran[0]);
//        assertTrue("Should be readable in selected() call", ran[1]);
//        final long timeInMillis = TimeUnit.MILLISECONDS.convert(time[0] - start, TimeUnit.NANOSECONDS);
//        assertTrue("Should have taken around 50ms. Took " + timeInMillis, timeInMillis >= 50 && timeInMillis <= 65);
//    }
//
//
    private Void closeEventLoop() {
        try {
            eventLoop.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

}