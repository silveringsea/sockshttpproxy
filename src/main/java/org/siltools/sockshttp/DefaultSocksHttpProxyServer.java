//package org.siltools.sockshttp;
//
//import io.netty.bootstrap.ServerBootstrap;
//import io.netty.channel.*;
//import io.netty.channel.group.ChannelGroup;
//import io.netty.channel.group.DefaultChannelGroup;
//import io.netty.channel.socket.nio.NioServerSocketChannel;
//import io.netty.handler.logging.LoggingHandler;
//import io.netty.util.concurrent.GlobalEventExecutor;
//import org.siltools.sockshttp.auth.ProxyAuthenticator;
//import org.siltools.sockshttp.def.ServerParams;
//import org.siltools.sockshttp.def.TransportProtocol;
//import org.siltools.sockshttp.host.resolver.HostResolver;
//import org.siltools.sockshttp.proxy.DefaultClientToProxyConnection;
//import org.siltools.sockshttp.proxy.IConnectionHandler;
//import org.siltools.sockshttp.proxy.ServerGroup;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.net.InetSocketAddress;
//import java.util.concurrent.atomic.AtomicBoolean;
//
///**
// */
//public class DefaultSocksHttpProxyServer implements SocksHttpProxyServer {
//
//    private static transient Logger LOG = LoggerFactory.getLogger(DefaultSocksHttpProxyServer.class);
//
//    protected ServerGroup serverGroup;
//    protected int idleConnectionTimeout;
//    private final ChannelGroup allChannels = new DefaultChannelGroup("HttpProxyServers", GlobalEventExecutor.INSTANCE);
//    private InetSocketAddress bindSocketAddress;
//    private TransportProtocol transportProtocol = TransportProtocol.TCP;
//
//    InetSocketAddress boundAddress;
//    AtomicBoolean stopped = new AtomicBoolean(false);
//
//    /**
//     * JVM shutdown hook to shutdown this proxy server.
//     */
//    private final Thread jvmShutdownHook = new Thread(new Runnable() {
//        public void run() {
//            shutdown();
//        }
//    }, "SocksHttp-JVM-shutdown-hook");
//
//
//    public DefaultSocksHttpProxyServer(ServerGroup serverGroup, InetSocketAddress bindSocketAddress, int idleConnectionTimeout) {
//        this.serverGroup = serverGroup;
//        this.bindSocketAddress = bindSocketAddress;
//        this.idleConnectionTimeout = idleConnectionTimeout;
//    }
//
//    public int getIdleConnectionTimeout() {
//        return 0;
//    }
//
//    public int getConnectTimeout() {
//        return idleConnectionTimeout;
//    }
//
//    public ProxyAuthenticator getProxyAuthenticator() {
//        return null;
//    }
//
//    public EventLoopGroup getProxyToServerThreadPool(TransportProtocol transportProtocol) {
//        return serverGroup.getProxyToServerWorkerPoolForTransport(transportProtocol);
//    }
//
//    public HostResolver getHostResolver() {
//        return null;
//    }
//
//    public void setHostResolver() {
//
//    }
//
//    public InetSocketAddress getLocalAddress() {
//        return null;
//    }
//
//    public void registerChannel(Channel channel) {
//        allChannels.add(channel);
//    }
//
//    public void unRegisterChannel(Channel channel) {
//
//    }
//
//    public void closeAllChannels(boolean gracefully) {
//        allChannels.close();
//    }
//
//    public SocksHttpProxyServer start() {
//        doStart();
//        return this;
//    }
//
//    protected void doStart() {
//        ChannelInitializer<Channel> initializer = new ChannelInitializer<Channel>() {
//            protected void initChannel(Channel ch) throws Exception {
//                new DefaultClientToProxyConnection(
//                        DefaultSocksHttpProxyServer.this,
//                        ch.pipeline()
//                );
//            };
//        };
//
//        ServerBootstrap serverBootstrap = new ServerBootstrap()
//                .group(serverGroup.getClientToProxyAcceptorPoolForTransport(transportProtocol),
//                        serverGroup.getClientToProxyWorkerPoolForTransport(transportProtocol));
//        serverBootstrap.channel(NioServerSocketChannel.class);
//        serverBootstrap.handler(new LoggingHandler());
//        serverBootstrap.childHandler(initializer);
//
//        ChannelFuture future = serverBootstrap.bind(bindSocketAddress)
//                .addListener(new ChannelFutureListener() {
//                    public void operationComplete(ChannelFuture future) throws Exception {
//                        if (future.isSuccess()) {
//                            registerChannel(future.channel());
//                        }
//                    }
//                });
//
//        future.awaitUninterruptibly();
//
//        Throwable cause = future.cause();
//        if (cause != null) {
//            throw new RuntimeException(cause);
//        }
//
//        this.boundAddress = ((InetSocketAddress) future.channel().localAddress());
//        LOG.info("Proxy started at address: " + this.boundAddress);
//
//        Runtime.getRuntime().addShutdownHook(jvmShutdownHook);
//        future.channel().closeFuture().syncUninterruptibly();
//    }
//
//    public void stop() {
//        if (stopped.compareAndSet(false, true)) {
//            closeAllChannels(true);
//
//            try {
//                Runtime.getRuntime().removeShutdownHook(jvmShutdownHook);
//            } catch (IllegalStateException e) {
//
//            }
//            LOG.info(" Done shutting down proxy server ");
//        }
//    }
//
//    public void shutdown() {
//        stop();
//    }
//
//    public static class DefaultSocksHttpProxyServerBootstrap implements SocksHttpProxyServerBootstrap {
//
//        ServerGroup serverGroup;
//        int idleConnectionTimeout;
//        String name;
//        String host;
//        int port;
//
//        public SocksHttpProxyServerBootstrap withHost(String host) {
//            this.host = host;
//            return this;
//        }
//
//        public SocksHttpProxyServerBootstrap withName(String name) {
//            this.name = name;
//            return this;
//        }
//
//        public SocksHttpProxyServerBootstrap withPort(int port) {
//            this.port = port;
//            return this;
//        }
//
//        public SocksHttpProxyServerBootstrap withTransparent(boolean transparent) {
//            return null;
//        }
//
//        public SocksHttpProxyServerBootstrap withIdleConnectionTimeout(int idleConnectionTimeout) {
//            return null;
//        }
//
//        public SocksHttpProxyServerBootstrap withConnectTimeout(int connectTimeout) {
//            return null;
//        }
//
//        public SocksHttpProxyServerBootstrap handler(IConnectionHandler handler) {
//            return this;
//        }
//
//        private DefaultSocksHttpProxyServer build() {
//            final ServerGroup serverGroup;
//            if (this.serverGroup != null) {
//                serverGroup = this.serverGroup;
//            } else {
//                serverGroup = new ServerGroup(name, ServerParams.DEFAULT_INBOUND_ACCEPTOR_THREADS,
//                        ServerParams.DEFAULT_OUTBOUND_WORKER_THREADS,
//                        ServerParams.DEFAULT_OUTBOUND_WORKER_THREADS);
//            }
//
//            return new DefaultSocksHttpProxyServer(serverGroup, new InetSocketAddress(host, port),
//                    idleConnectionTimeout);
//        }
//
//        public SocksHttpProxyServer start() {
//            return build().start();
//        }
//    }
//}
