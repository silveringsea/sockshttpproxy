package org.siltools.sockshttp;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.siltools.sockshttp.def.ServerParams;
import org.siltools.sockshttp.def.TransportProtocol;
import org.siltools.sockshttp.proxy.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 */
public class FlowSocksHttpProxyServer implements SocksHttpProxyServer {

    private static transient Logger logger = LoggerFactory.getLogger(FlowSocksHttpProxyServer.class);

    protected ServerGroup serverGroup;
    protected int idleConnectionTimeout;
    protected int connectionTimeout;
    private final ChannelGroup allChannels = new DefaultChannelGroup("HttpProxyServers", GlobalEventExecutor.INSTANCE);
    private InetSocketAddress bindSocketAddress;
    private TransportProtocol transportProtocol = TransportProtocol.TCP;

    ArrayBlockingQueue<FlowConnectionInitializer> queue = new ArrayBlockingQueue<FlowConnectionInitializer>(1);
    InetSocketAddress boundAddress;
    AtomicBoolean stopped = new AtomicBoolean(false);

    /**
     * JVM shutdown hook to shutdown this proxy server.
     */
    private final Thread jvmShutdownHook = new Thread(new Runnable() {
        public void run() {
            shutdown();
        }
    }, "SocksHttp-JVM-shutdown-hook");


    public FlowSocksHttpProxyServer(ServerGroup serverGroup, InetSocketAddress bindSocketAddress,
                                    int idleConnectionTimeout, int connectionTimeout) {
        this.serverGroup = serverGroup;
        this.bindSocketAddress = bindSocketAddress;
        this.idleConnectionTimeout = idleConnectionTimeout;
        this.connectionTimeout = connectionTimeout;
        queue.add(new FlowConnectionInitializer());
    }


    public int getIdleConnectionTimeout() {
        return idleConnectionTimeout;
    }

    public int getConnectTimeout() {
        return connectionTimeout;
    }

    public EventLoopGroup getProxyToServerThreadPool(TransportProtocol transportProtocol) {
        return serverGroup.getProxyToServerWorkerPoolForTransport(transportProtocol);
    }


    public InetSocketAddress getLocalAddress() {
        return null;
    }

    public void registerChannel(Channel channel) {
        allChannels.add(channel);
    }

    public void unRegisterChannel(Channel channel) {

    }

    public void closeAllChannels(boolean gracefully) {
        allChannels.close();
    }

    public SocksHttpProxyServer start() {
        doStart();
        return this;
    }

    private void addConnectionInitializer(ConnectionInitializer connectionInitializer) throws Exception {
        FlowConnectionInitializer flowInitializer = queue.take();
        flowInitializer.add(connectionInitializer);
        queue.put(flowInitializer);
    }

    protected void doStart() {
        ChannelInitializer<Channel> initializer = new ChannelInitializer<Channel>() {
            protected void initChannel(Channel ch) throws Exception {
                FlowConnectionInitializer connectionInitializer = queue.poll();
                IConnectionPipeline defaultConnectionPipeline = connectionInitializer.getPipeline();
                queue.offer(connectionInitializer);

                new DefaultClientToProxyConnection(
                        FlowSocksHttpProxyServer.this,
                        defaultConnectionPipeline,
                        ch.pipeline()
                );
            };
        };
        ServerBootstrap serverBootstrap = new ServerBootstrap()
                .group(serverGroup.getClientToProxyAcceptorPoolForTransport(transportProtocol),
                        serverGroup.getClientToProxyWorkerPoolForTransport(transportProtocol));
        serverBootstrap.channel(NioServerSocketChannel.class);
        serverBootstrap.handler(new LoggingHandler());
        serverBootstrap.childHandler(initializer);


        ChannelFuture future = serverBootstrap.bind(bindSocketAddress)
                .addListener(new ChannelFutureListener() {
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            registerChannel(future.channel());
                        }
                    }
                });

        future.awaitUninterruptibly();

        Throwable cause = future.cause();
        if (cause != null) {
            throw new RuntimeException(cause);
        }

        this.boundAddress = ((InetSocketAddress) future.channel().localAddress());
        logger.info("Proxy started at address: " + this.boundAddress);

        Runtime.getRuntime().addShutdownHook(jvmShutdownHook);
        future.channel().closeFuture().syncUninterruptibly();
    }

    public void stop() {
        if (stopped.compareAndSet(false, true)) {
            closeAllChannels(true);

            try {
                Runtime.getRuntime().removeShutdownHook(jvmShutdownHook);
            } catch (IllegalStateException e) {
                throw new RuntimeException("shutdown error", e);
            }
            logger.info(" Done shutting down proxy server ");
        }
    }

    public void shutdown() {
        stop();
    }

    public final class FlowConnectionInitializer {

        public ArrayBlockingQueue<ConnectionInitializer> queue = new ArrayBlockingQueue<ConnectionInitializer>(10);

        public void add(ConnectionInitializer ch) throws Exception {
            queue.add(ch);
        }

        public IConnectionPipeline getPipeline() throws Exception {

            IConnectionPipeline pipeline = new DefaultConnectionPipeline(FlowSocksHttpProxyServer.this);
            for (Iterator<ConnectionInitializer> iterator = queue.iterator(); iterator.hasNext();) {
                pipeline = iterator.next().initPipline(pipeline);
            }
            return pipeline;
        }
    }

    public static class FlowSocksHttpProxyServerBootstrap implements SocksHttpProxyServerBootstrap {
        ServerGroup serverGroup;
        int idleConnectionTimeout;
        int connectionTimeout;
        String name;
        String host;
        int port;
        ConnectionInitializer connectionInitializer;

        public SocksHttpProxyServerBootstrap withHost(String host) {
            this.host = host;
            return this;
        }

        public SocksHttpProxyServerBootstrap withName(String name) {
            this.name = name;
            return this;
        }

        public SocksHttpProxyServerBootstrap withPort(int port) {
            this.port = port;
            return this;
        }

        public SocksHttpProxyServerBootstrap withTransparent(boolean transparent) {
            return this;
        }

        public SocksHttpProxyServerBootstrap withIdleConnectionTimeout(int idleConnectionTimeout) {
            this.idleConnectionTimeout = idleConnectionTimeout;
            return this;
        }

        public SocksHttpProxyServerBootstrap withConnectTimeout(int connectTimeout) {
            this.connectionTimeout = connectTimeout;
            return this;
        }

        public SocksHttpProxyServerBootstrap withConnInitializer(ConnectionInitializer connectionInitializer) {
            this.connectionInitializer = connectionInitializer;
            return this;
        }

        private FlowSocksHttpProxyServer build() {
            final ServerGroup serverGroup;
            if (this.serverGroup != null) {
                serverGroup = this.serverGroup;
            } else {
                serverGroup = new ServerGroup(name, ServerParams.DEFAULT_INBOUND_ACCEPTOR_THREADS,
                        ServerParams.DEFAULT_OUTBOUND_WORKER_THREADS,
                        ServerParams.DEFAULT_OUTBOUND_WORKER_THREADS);
            }

            FlowSocksHttpProxyServer socksHttpProxyServer =
                    new FlowSocksHttpProxyServer(serverGroup, new InetSocketAddress(host, port),
                    idleConnectionTimeout, connectionTimeout);

            try {
                socksHttpProxyServer.addConnectionInitializer(connectionInitializer);
            } catch (Exception e) {
                return null;
            }
            return socksHttpProxyServer;
        }

        public SocksHttpProxyServer start() {
            return build().start();
        }
    }

}
