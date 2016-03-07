package org.siltools.sockshttp.proxy.connection;//package org.siltools.sockshttp.proxy.connection;
//
//import io.netty.bootstrap.Bootstrap;
//import io.netty.buffer.ByteBuf;
//import io.netty.channel.Channel;
//import io.netty.channel.ChannelInitializer;
//import io.netty.channel.ChannelOption;
//import io.netty.channel.ChannelPipeline;
//import io.netty.channel.socket.nio.NioSocketChannel;
//import io.netty.channel.udt.nio.NioUdtByteConnectorChannel;
//import io.netty.handler.codec.http.*;
//import io.netty.handler.timeout.IdleStateHandler;
//import io.netty.util.concurrent.Future;
//import org.apache.commons.lang3.StringUtils;
//import org.siltools.sockshttp.SocksHttpProxyServer;
//import org.siltools.sockshttp.def.ConnectionState;
//import org.siltools.sockshttp.def.TransportProtocol;
//import org.siltools.sockshttp.def.exception.UnknownTransportProtocolException;
//import org.siltools.sockshttp.proxy.ConnectionFlow;
//import org.siltools.sockshttp.proxy.ConnectionFlowStep;
//import org.siltools.sockshttp.util.SocksHttpProxyUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.net.InetSocketAddress;
//import java.net.UnknownHostException;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.locks.Condition;
//import java.util.concurrent.locks.Lock;
//import java.util.concurrent.locks.ReentrantLock;
//
///**
// * Created by Administrator on 1/12/2016.
// */
//public class ProxyToServerConnection extends ProxyConnection<HttpResponse> implements IProxyToServerConnection  {
////    AbstractClientToProxyConnection
//    IClientToProxyConnection clientConnection;
//    protected ConnectionFlow connectionFlow;
//    HttpRequest initialHttpRequest;
//    protected String serverHostAndPort;
//    private final ProxyToServerConnection thisConnection = this;
//    private volatile InetSocketAddress remoteAddress;
//    private volatile InetSocketAddress localAddress;
//    private volatile TransportProtocol transportProtocol;
//    private static transient Logger LOG = LoggerFactory.getLogger(ProxyToServerConnection.class);
//
//    private Lock connectionLock = new ReentrantLock();
//    private Condition connectionLockCond = connectionLock.newCondition();
//
//    private ConnectionFlowStep CONNECTION_FLOW_CHANNEL = new ConnectionFlowStep(this, ConnectionState.CONNECTING) {
//        @Override
//        protected Future execute() {
//            Bootstrap bootstrap = new Bootstrap().group(proxyServer.getProxyToServerThreadPool(transportProtocol));
//            switch (transportProtocol) {
//                case TCP:
//                    bootstrap.channel(NioSocketChannel.class);
//                    break;
//                case UDT:
//                    bootstrap.channel(NioUdtByteConnectorChannel.class).option(ChannelOption.SO_REUSEADDR, true);
//                default:
//                    throw new UnknownTransportProtocolException(transportProtocol);
//            }
//            bootstrap.handler(new ChannelInitializer<Channel>() {
//                @Override
//                protected void initChannel(Channel ch) throws Exception {
//                    initChannelPipeline(ch.pipeline());
//                }
//            }).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, proxyServer.getConnectTimeout());
//            return bootstrap.connect(remoteAddress);
//        }
//    };
//
//    public void setTransportProtocol(TransportProtocol transportProtocol) {
//        this.transportProtocol = transportProtocol;
//    }
//
//    public ProxyToServerConnection(SocksHttpProxyServer proxyServer, IClientToProxyConnection clientConnection,
//                                   HttpRequest initialHttpRequest, String serverHostAndPort) {
//        this(proxyServer, clientConnection, initialHttpRequest, serverHostAndPort, TransportProtocol.TCP);
//    }
//
//    public ProxyToServerConnection(SocksHttpProxyServer proxyServer, IClientToProxyConnection clientConnection,
//                                   HttpRequest initialHttpRequest, String serverHostAndPort,
//                                   TransportProtocol transportProtocol) {
//        super(ConnectionState.DISCONNECTED, proxyServer, true);
//        this.clientConnection = clientConnection;
//        this.initialHttpRequest = initialHttpRequest;
//        initialHttpRequest.headers().set(HttpHeaderNames.USER_AGENT, "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:41.0) Gecko/20100101 Firefox/41.0");
//        this.serverHostAndPort = serverHostAndPort;
//        this.transportProtocol = transportProtocol;
//        this.isTunneling = true;
//        setupConnectionParameters();
//    }
//
//    protected void setupConnectionParameters() {
//        String[] serverHostPortStrs = StringUtils.split(serverHostAndPort, ":");
//        String host = serverHostPortStrs[0];
//        int port = (serverHostPortStrs.length > 1 && serverHostPortStrs[1] != null) ? Integer.parseInt(serverHostPortStrs[1]): 80;
//        remoteAddress = new InetSocketAddress(host, port);
//    }
//
//    /**
//     * Initialize our {@link ChannelPipeline}.
//     *
//     * @param pipeline
//     */
//    public void initChannelPipeline(ChannelPipeline pipeline) {
//        if (!isTunneling)
//            pipeline.addLast("decoder", new HttpResponseDecoder(8192,
//                    8192 * 2,
//                    8192 * 2));
//        pipeline.addLast("encoder", new HttpRequestEncoder());
//        // Set idle timeout
//        pipeline.addLast(
//                "idle",
//                new IdleStateHandler(0, 0, proxyServer
//                        .getIdleConnectionTimeout()));
//
//
//        pipeline.addLast("handler", this);
//    }
//
//    public String getServerHostAndPort() {
//        return serverHostAndPort;
//    }
//
//    public InetSocketAddress getRemoteAddress() {
//        return null;
//    }
//
//    public void serverConnectionFlowStarted() {
//        clientConnection.serverConnectionFlowStarted(this);
//    }
//
//    public boolean serverConnectionFailed(ConnectionState connectionState, Throwable cause) {
//        return clientConnection.serverConnectionFailed(this, connectionState, cause);
//    }
//
//    public void resentHttpRequest(HttpRequest httpRequest) {
//
//    }
//
//    public void startConnect(HttpRequest httpRequest) {
//
//    }
//
//    @Override
//    protected ConnectionState readHTTPInitial(HttpResponse httpResponse) {
//        LOG.debug("Received raw response: {}", httpResponse);
//        if (SocksHttpProxyUtils.isChunked(httpResponse)) {
//            return ConnectionState.AWAITING_CHUNK;
//        } else {
//            return ConnectionState.AWAITING_INITIAL;
//        }
//    }
//
//    @Override
//    protected void readHTTPChunk(HttpContent chunk) {
//        respondWith(chunk);
//    }
//
//    private void respondWith(HttpObject httpObject) {
//        clientConnection.respond(this, null, null, httpObject);
//    }
//
//    @Override
//    protected void readRaw(ByteBuf buf) {
//        clientConnection.writeDataToChannel(buf);
//    }
//
//    @Override
//    protected void handleHttpRequest(final HttpRequest httpRequest) {
//        SocksHttpProxyUtils.retainObject(httpRequest);
//
//        if (is(ConnectionState.DISCONNECTED) && httpRequest instanceof HttpRequest) {
//            connectAndWrite(httpRequest);
//        } else {
//            if (isConnecting()) {
//                connectionLock.lock();
//                if (isConnecting()) {
//                    clientConnection.stopAutoRead();
//                    try {
//                        connectionLockCond.await(1000000, TimeUnit.MINUTES);
//                    } catch (InterruptedException e) {
//                        LOG.error(" if is connecting fail ", e);
//                    }
//                }
//                connectionLock.unlock();
//            }
//
//            if (isConnecting() || getCurrentState().isDisconnectingOrDisconnected()) {
//                // connect fail
//                LOG.error("asdfasdfa");
//                return;
//            }
//
//            writeDataToChannel(httpRequest);
//        }
//    }
//
//    /** */
//    protected void connectAndWrite(final HttpRequest httpRequest) {
//        this.initialHttpRequest = httpRequest;
//        initialHttpRequest.headers().set(HttpHeaderNames.USER_AGENT, "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:41.0) Gecko/20100101 Firefox/41.0");
//        this.connectionFlow = new ConnectionFlow(this, connectionLock, connectionLockCond)
//                .then(CONNECTION_FLOW_CHANNEL);
//        //handle chain proxy  --begin
//        //handle chain proxy --end
//        if (SocksHttpProxyUtils.isConnect(initialHttpRequest)) {
////            handle isMitmEnabled
//
//            connectionFlow.then(thisConnection.StartTunneling)
//                    .then(clientConnection.getRespondCONNECTSuccessful())
//                    .then(clientConnection.getStartTunnelingFlowStep());
//        }
//        connectionFlow.start();
//    }
//
//
//    @Override
//    public void becomeState(ConnectionState newState) {
//        // Report connection status to HttpFilters
////        if (getCurrentState() == DISCONNECTED && newState == CONNECTING) {
////            currentFilters.proxyToServerConnectionStarted();
////        } else if (getCurrentState() == CONNECTING) {
////            if (newState == HANDSHAKING) {
////                currentFilters.proxyToServerConnectionSSLHandshakeStarted();
////            } else if (newState == AWAITING_INITIAL) {
////                currentFilters.proxyToServerConnectionSucceeded(ctx);
////            } else if (newState == DISCONNECTED) {
////                currentFilters.proxyToServerConnectionFailed();
////            }
////        } else if (getCurrentState() == HANDSHAKING) {
////            if (newState == AWAITING_INITIAL) {
////                currentFilters.proxyToServerConnectionSucceeded(ctx);
////            } else if (newState == DISCONNECTED) {
////                currentFilters.proxyToServerConnectionFailed();
////            }
////        } else if (getCurrentState() == AWAITING_CHUNK
////                && newState != AWAITING_CHUNK) {
////            currentFilters.serverToProxyResponseReceived();
////        }
//
//        super.becomeState(newState);
//    }
//
//    public IProxyConnectionType getType() {
//        return null;
//    }
//
//    public void connectionSucceeded(boolean shouldForwardInitialRequest) {
//        becomeState(ConnectionState.AWAITING_INITIAL);
//
//        clientConnection.serverConnectionSucceeded(this,
//                shouldForwardInitialRequest);
//        if (shouldForwardInitialRequest) {
//            //LOG.debug("Writing initial request: {}", int);
//            if (this.channel.isActive()) {
//                writeDataToChannel(this.initialHttpRequest);
//            } else {
//                LOG.error("channel is not active");
//            }
//        } else {
//            //LOG.debug("Dropping initial request: {}", initialRequest);
//        }
//
//    }
//
//    public boolean connectionFailed(Throwable cause) throws UnknownHostException {
//        return false;
//    }
//
//    public HttpRequest getInitialRequest() {
//        return this.initialHttpRequest;
//    }
//
//    @Override
//    public Future<Void> disconnect() {
//        return super.disconnect();
//    }
//
//}
