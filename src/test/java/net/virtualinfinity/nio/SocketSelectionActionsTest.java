package net.virtualinfinity.nio;



import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 */
@ExtendWith(MockitoExtension.class)
public class SocketSelectionActionsTest {
    private static final int INPUT_BUFFER_SIZE = 8;
    private static final String CONNECTION_PENDING = "connectionPending";
    private static final String CONNECTED = "connected";
    private static final String WRITABLE = "writable";
    private static final String READABLE = "readable";
    private static final String NOT_WRITABLE = "notWritable";
    private static final String NOT_READABLE = "notReadable";
    @Mock
    private SocketChannel channel;
    @Mock
    private ConnectionListener connectionListener;
    @Mock
    private ByteBufferConsumer receiver;
    @Mock
    private SelectionKey selectionKey;

    private final OutputBuffer outputBuffer = new OutputBuffer();

    @Test
    public void connectionCompletion() throws IOException {
        when(channel.isOpen()).thenReturn(true);
        when(channel.isConnectionPending()).thenReturn(true);
        when(channel.isConnected()).thenReturn(true);
        when(selectionKey.isConnectable()).thenReturn(true);
        when(channel.finishConnect()).thenReturn(true);
        final SocketSelectionActions actions = createActions(INPUT_BUFFER_SIZE, true);
        assertEquals(SelectionKey.OP_CONNECT, actions.interestOps());
        actions.selected();
        when(channel.isConnectionPending()).thenReturn(false);
        assertEquals(SelectionKey.OP_READ, actions.interestOps());
        verify(connectionListener).connected();
    }

    @Test
    public void connectionFailure() throws IOException {
        when(channel.isOpen()).thenReturn(true);
        when(channel.isConnectionPending()).thenReturn(true);
        when(selectionKey.isConnectable()).thenReturn(true);
        final IOException failure = new IOException();
        when(channel.finishConnect()).thenThrow(failure);
        final SocketSelectionActions actions = createActions(INPUT_BUFFER_SIZE, true);
        assertEquals(SelectionKey.OP_CONNECT, actions.interestOps());
        actions.selected();
        verify(connectionListener).connectionFailed(failure);
    }
//
//    @Test
//    public void readAndWrite() throws IOException {
//        channelState.startsAs(CONNECTED);
//        context.checking(expectations());
//        context.checking(new Expectations() {{
//            oneOf(channel).read(with.is(Expectations.anything())); will(simulateChannelRead(INPUT_BUFFER_SIZE, (byte) 0));
//            oneOf(channel).read(with.is(Expectations.anything())); will(simulateChannelRead(2, (byte)1));
//            exactly(2).of(receiver).accept(with.is(Expectations.anything())); will(consumeByteBuffer());
//            oneOf(channel).write(with.is(Expectations.anything())); will(consumeByteBufferAndReturn(0));
//        }});
//        final SocketSelectionActions actions = createActions(INPUT_BUFFER_SIZE, false);
//        assertEquals(SelectionKey.OP_READ, actions.interestOps());
//        outputBuffer.append(ByteBuffer.allocate(10));
//        assertEquals(SelectionKey.OP_READ | SelectionKey.OP_WRITE, actions.interestOps());
//        outputState.become(WRITABLE);
//        inputState.become(READABLE);
//        actions.selected();
//        actions.selected();
//
//    }
//
//    @Test
//    public void pendingWritePreventsRead() throws IOException {
//        channelState.startsAs(CONNECTED);
//        outputState.startsAs(WRITABLE);
//        inputState.startsAs(READABLE);
//        context.checking(expectations());
//        context.checking(new Expectations() {{
//            exactly(2).of(channel).write(with.is(Expectations.anything())); will(consumeByteBufferAndReturn(0)); inSequence(sequence);
//            oneOf(channel).read(with.is(Expectations.anything())); will(simulateChannelRead(INPUT_BUFFER_SIZE, (byte) 0)); inSequence(sequence);
//            oneOf(channel).read(with.is(Expectations.anything())); will(simulateChannelRead(2, (byte) 1)); inSequence(sequence);
//            exactly(2).of(receiver).accept(with.is(Expectations.anything()));
//            will(consumeByteBuffer());
//        }});
//        final SocketSelectionActions actions = createActions(INPUT_BUFFER_SIZE, true);
//        assertEquals(SelectionKey.OP_READ, actions.interestOps());
//        outputBuffer.append(ByteBuffer.allocate(512));
//        outputBuffer.append(ByteBuffer.allocate(512));
//        assertEquals(SelectionKey.OP_WRITE, actions.interestOps());
//        actions.selected();
//        actions.selected();
//
//    }
//
//    private Action simulateChannelRead(int count, byte value) {
//        return new SimulateChannelRead(count, value);
//    }
//
//    private Action consumeByteBuffer() {
//        return new ConsumeByteBuffer(null);
//    }
//
//    private Action consumeByteBufferAndReturn(Object value) {
//        return new ConsumeByteBuffer(value);
//    }
//
        private SocketSelectionActions createActions(int inputBufferSize, boolean sendAllBeforeReading) {
            final SocketSelectionActions socketSelectionActions = new SocketSelectionActions(channel, connectionListener, receiver, outputBuffer, inputBufferSize, sendAllBeforeReading);
            socketSelectionActions.setSelectionKey(selectionKey);
            return socketSelectionActions;
        }
//
//    private static class ConsumeByteBuffer extends ReturnValueAction{
//        public ConsumeByteBuffer(Object result) {
//            super(result);
//        }
//
//        @Override
//        public Object invoke(Invocation invocation) throws Throwable {
//            final ByteBuffer buffer = (ByteBuffer) invocation.getParameter(0);
//            buffer.position(buffer.limit());
//            return super.invoke(invocation);
//        }
//
//        @Override
//        public void describeTo(Description description) {
//            description.appendText("Moves the ByteBuffer's position to its limit, and ");
//            super.describeTo(description);
//        }
//    }
//
//    private static class SimulateChannelRead implements Action {
//        private final int count;
//        private final byte value;
//
//        public SimulateChannelRead(int count, byte value) {
//            this.count = count;
//            this.value = value;
//        }
//
//        @Override
//        public Object invoke(Invocation invocation) throws Throwable {
//            if (count < 0) {
//                return count;
//            }
//            for (int c = 0; c < count; ++c) {
//                ((ByteBuffer)invocation.getParameter(0)).put(value);
//            }
//            return count;
//        }
//
//        @Override
//        public void describeTo(Description description) {
//            description.appendText("Read ").appendValue(value).appendText(" " + count + " times");
//        }
//    }
}