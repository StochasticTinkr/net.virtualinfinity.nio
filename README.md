# net.virtualinfinity.nio
## Basic NIO and EventLoop.

This library provides a [EventLoop](src/main/java/net/virtualinfinity/nio/EventLoop.java) class,
which and aid in creating NIO based applications.

The basic idea is that you create an EventLoop object, which you can register handlers for selectable channels,
as well as events to fire off in the future.

The library makes no assumptions about the threading model you wish to use.  You will need to call "run()" on the
EventLoop object for it to do its job.

A typical "main" might look like this:


    public static void main(String[] args) throws IOException {
        final EventLoop eventLoop = new EventLoop();
        ServerSocketChannel serverChanel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(80));

        eventLoop.registerHandler(serverChannel, createServerListener());

        eventLoop.run();
    }
