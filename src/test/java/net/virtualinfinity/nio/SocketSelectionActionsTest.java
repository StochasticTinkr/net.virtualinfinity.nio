package net.virtualinfinity.nio;

import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Sequence;
import org.jmock.States;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.auto.Auto;
import org.jmock.auto.Mock;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.internal.ExpectationBuilder;
import org.jmock.lib.action.ReturnValueAction;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href='mailto:Daniel@coloraura.com'>Daniel Pitts</a>
 */
public class SocketSelectionActionsTest {
    public static final int INPUT_BUFFER_SIZE = 8;
    public static final String CONNECTION_PENDING = "connectionPending";
    public static final String CONNECTED = "connected";
    public static final String WRITABLE = "writable";
    private static final String READABLE = "readable";
    public static final String NOT_WRITABLE = "notWritable";
    public static final String NOT_READABLE = "notReadable";

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();

    @Mock
    private SocketChannelInterface channel;

    @Mock
    private ConnectionListener connectionListener;

    @Mock
    private ByteBufferConsumer receiver;

    @Mock
    private SelectionKeyInterface selectionKey;

    private final OutputBuffer outputBuffer = new OutputBuffer();

    private final States channelState = context.states("channel").startsAs(CONNECTION_PENDING);
    private final States outputState = context.states("channel").startsAs(NOT_WRITABLE);
    private final States inputState = context.states("channel").startsAs(NOT_READABLE);

    @Auto
    private Sequence sequence;

    @Test
    public void connectionCompletion() throws IOException {
        checkPendingConnection();

        context.checking(new Expectations() {{
            oneOf(channel).finishConnect(); will(returnValue(true)); then(channelState.is(CONNECTED));
            oneOf(connectionListener).connected();
        }});

        final SocketSelectionActions actions = createActions(INPUT_BUFFER_SIZE, true);
        assertEquals(SelectionKey.OP_CONNECT, actions.interestOps());
        actions.selected();
        assertEquals(SelectionKey.OP_READ, actions.interestOps());
    }

    @Test
    public void connectionFailure() throws IOException {
        checkPendingConnection();

        context.checking(new Expectations() {{
            final IOException failure = new IOException();
            oneOf(channel).finishConnect();
            will(throwException(failure));
            oneOf(connectionListener).connectionFailed(failure);
            oneOf(selectionKey).cancel();
        }});
        final SocketSelectionActions actions = createActions(INPUT_BUFFER_SIZE, true);
        assertEquals(SelectionKey.OP_CONNECT, actions.interestOps());
        actions.selected();
    }

    @Test
    public void readAndWrite() throws IOException {
        channelState.startsAs(CONNECTED);
        context.checking(expectations());
        context.checking(new Expectations() {{
            oneOf(channel).read((ByteBuffer)with.is(Expectations.anything()));
            will(simulateChannelRead(INPUT_BUFFER_SIZE, (byte) 0));

            oneOf(channel).read((ByteBuffer)with.is(Expectations.anything()));
            will(simulateChannelRead(2, (byte)1));

            exactly(2).of(receiver).accept((ByteBuffer)with.is(Expectations.anything()));
            will(consumeByteBuffer());

            oneOf(channel).write((ByteBuffer)with.is(Expectations.anything()));
            will(consumeByteBufferAndReturn(0));
        }});

        final SocketSelectionActions actions = createActions(INPUT_BUFFER_SIZE, false);
        assertEquals(SelectionKey.OP_READ, actions.interestOps());
        outputBuffer.append(ByteBuffer.allocate(10));
        assertEquals(SelectionKey.OP_READ | SelectionKey.OP_WRITE, actions.interestOps());
        outputState.become(WRITABLE);
        inputState.become(READABLE);
        actions.selected();
        actions.selected();

    }

    @Test
    public void pendingWritePreventsRead() throws IOException {
        channelState.startsAs(CONNECTED);
        outputState.startsAs(WRITABLE);
        inputState.startsAs(READABLE);
        context.checking(expectations());
        context.checking(new Expectations() {{
            exactly(2).of(channel).write((ByteBuffer)with.is(Expectations.anything()));
            will(consumeByteBufferAndReturn(0)); inSequence(sequence);

            oneOf(channel).read((ByteBuffer)with.is(Expectations.anything()));
            will(simulateChannelRead(INPUT_BUFFER_SIZE, (byte) 0)); inSequence(sequence);

            oneOf(channel).read((ByteBuffer)with.is(Expectations.anything()));
            will(simulateChannelRead(2, (byte) 1)); inSequence(sequence);

            exactly(2).of(receiver).accept((ByteBuffer)with.is(Expectations.anything()));
            will(consumeByteBuffer());
        }});
        final SocketSelectionActions actions = createActions(INPUT_BUFFER_SIZE, true);
        assertEquals(SelectionKey.OP_READ, actions.interestOps());
        outputBuffer.append(ByteBuffer.allocate(512));
        outputBuffer.append(ByteBuffer.allocate(512));
        assertEquals(SelectionKey.OP_WRITE, actions.interestOps());
        actions.selected();
        actions.selected();

    }

    private Action simulateChannelRead(int count, byte value) {
        return new SimulateChannelRead(count, value);
    }

    private Action consumeByteBuffer() {
        return new ConsumeByteBuffer(null);
    }

    private Action consumeByteBufferAndReturn(Object value) {
        return new ConsumeByteBuffer(value);
    }

    private void checkPendingConnection() {
        channelState.startsAs(CONNECTION_PENDING);
        context.checking(expectations());
    }

    private ExpectationBuilder expectations() {
        return new Expectations() {{
            allowing(channel).isOpen();
            will(returnValue(true));

            allowing(channel).isConnectionPending();
            will(returnValue(true));
            when(channelState.is(CONNECTION_PENDING));

            allowing(channel).isConnectionPending();
            will(returnValue(false));
            when(channelState.isNot(CONNECTION_PENDING));

            allowing(selectionKey).isConnectable();
            will(returnValue(true));
            when(channelState.isNot(CONNECTED));

            allowing(channel).isConnected();
            will(returnValue(true));
            when(channelState.is(CONNECTED));

            allowing(channel).isConnected();
            will(returnValue(false));
            when(channelState.isNot(CONNECTED));

            allowing(selectionKey).isWritable();
            will(returnValue(true));
            when(outputState.is(WRITABLE));

            allowing(selectionKey).isReadable();
            will(returnValue(true));
            when(inputState.is(READABLE));

            allowing(selectionKey).isWritable();
            will(returnValue(false));
            when(outputState.isNot(WRITABLE));

            allowing(selectionKey).isReadable();
            will(returnValue(false));
            when(inputState.isNot(READABLE));

            allowing(selectionKey).isValid();
            will(returnValue(true));

            allowing(selectionKey).interestOps(with.intIs(anything()));
        }};
    }

    private SocketSelectionActions createActions(int inputBufferSize, boolean sendAllBeforeReading) {
        final SocketSelectionActions socketSelectionActions = new SocketSelectionActions(channel, connectionListener, receiver, outputBuffer, inputBufferSize, sendAllBeforeReading);
        socketSelectionActions.setSelectionKey(selectionKey);
        return socketSelectionActions;
    }

    private static class ConsumeByteBuffer extends ReturnValueAction{
        public ConsumeByteBuffer(Object result) {
            super(result);
        }

        @Override
        public Object invoke(Invocation invocation) throws Throwable {
            final ByteBuffer buffer = (ByteBuffer) invocation.getParameter(0);
            buffer.position(buffer.limit());
            return super.invoke(invocation);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("Moves the ByteBuffer's position to its limit, and ");
            super.describeTo(description);
        }
    }

    private static class SimulateChannelRead implements Action {
        private final int count;
        private final byte value;

        public SimulateChannelRead(int count, byte value) {
            this.count = count;
            this.value = value;
        }

        @Override
        public Object invoke(Invocation invocation) throws Throwable {
            if (count < 0) {
                return count;
            }
            for (int c = 0; c < count; ++c) {
                ((ByteBuffer)invocation.getParameter(0)).put(value);
            }
            return count;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("Read ").appendValue(value).appendText(" " + count + " times");
        }
    }
}