package org.siltools.sockshttp.proxy.connection;//package org.siltools.sockshttp.proxy;
//
//import io.netty.buffer.ByteBuf;
//import io.netty.buffer.Unpooled;
//import io.netty.channel.*;
//import io.netty.handler.codec.http.*;
//import io.netty.util.ReferenceCounted;
//import io.netty.util.concurrent.Future;
//import io.netty.util.concurrent.GenericFutureListener;
//import io.netty.util.concurrent.Promise;
//import org.siltools.sockshttp.SocksHttpProxyServer;
//import org.siltools.sockshttp.def.ConnectionState;
//import org.siltools.sockshttp.util.SocksHttpProxyUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// * Created by Administrator on 2015/12/29.
// */
//public abstract class ProxyConnection<I extends HttpObject> extends SimpleChannelInboundHandler<Object>
//        implements IProxyConnection, IConnection {
//
//    protected static final HttpResponseStatus CONNECTION_ESTABLISHED = new HttpResponseStatus(
//            200, "HTTP/1.1 200 Connection established");
//
//    private static transient Logger LOG = LoggerFactory.getLogger(ProxyConnection.class);
//
//    /** */
//    protected volatile ChannelHandlerContext channelHandlerContext;
//    protected volatile Channel channel;
//
//    protected ConnectionState curConnectionState;
//
//    /** */
//    protected volatile boolean isTunneling = false;
//
//    protected SocksHttpProxyServer proxyServer;
//
//    protected boolean runsAsSslClient;
//
//    public SocksHttpProxyServer socksHttpProxyServer() {
//        return proxyServer;
//    }
//
//    protected ProxyConnection(ConnectionState initialState,
//                              SocksHttpProxyServer proxyServer,
//                              boolean runsAsSslClient) {
//        becomeState(initialState);
//        this.proxyServer = proxyServer;
//        this.runsAsSslClient = runsAsSslClient;
//    }
//
//    protected boolean getIsTunneling() {
//        return isTunneling;
//    }
//
//    @Override
//    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
//        this.channel =  ctx.channel();
//        this.channelHandlerContext = ctx;
//        this.proxyServer.registerChannel(this.channel);
//    }
//
//    @Override
//    protected void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
//        if (isTunneling) {
//            // In tunneling mode, this connection is simply shoveling bytes
//            readRaw((ByteBuf) msg);
//        } else {
//            // If not tunneling, then we are always dealing with HttpObjects.
//            readHTTP((HttpObject) msg);
//        }
//    }
//
//    public ConnectionState getCurrentState() {
//        return this.curConnectionState;
//    }
//
//    public void becomeState(ConnectionState connectionState) {
//        this.curConnectionState = connectionState;
//    }
//
//    protected void readHTTP(HttpObject httpObject) {
//        ConnectionState nextConnectionState = getCurrentState();
//        switch (getCurrentState()) {
//            case AWAITING_INITIAL:
//                if (httpObject instanceof HttpMessage) {
//                    nextConnectionState = readHTTPInitial((I)httpObject);
//                } else {
//                    LOG.debug("Dropping message because HTTP object was not an HttpMessage. HTTP object may be orphaned content from a short-circuited response. Message: {}", httpObject);
//                }
//                break;
//            case AWAITING_CHUNK:
//                HttpContent chunk = (HttpContent)httpObject;
//                readHTTPChunk(chunk);
//                nextConnectionState = SocksHttpProxyUtils.isLastChunk(chunk) ? ConnectionState.AWAITING_INITIAL: ConnectionState.AWAITING_CHUNK;
//                break;
//            case AWAITING_PROXY_AUTHENTICATION:
//                LOG.debug(" AWAITING_PROXY_AUTHENTICATION ");
//                break;
//            case CONNECTING:
//                break;
//            case NEGOTIATING_CONNECT:
//                break;
//            default:
//                break;
//        }
//
//        becomeState(nextConnectionState);
//    }
//
//    /**
//     * If this connection is currently in the process of going through a ConnectionFlow}, this will return true.
//     *
//     * @return
//     */
//    protected boolean isConnecting() {
//        return curConnectionState.isPartOfConnectionFlow();
//    }
//
//    protected ChannelFuture writeToChannel(final Object msg) {
//        return channel.writeAndFlush(msg);
//    }
//
//    protected void writeHttp(HttpObject httpObject) {
//        if (SocksHttpProxyUtils.isLastChunk(httpObject)) {
//            if (channel != null && channel.isActive())
//                channel.write(httpObject);
//            LOG.debug("Writing an empty buffer to signal the end of our chunked transfer");
//            writeToChannel(Unpooled.EMPTY_BUFFER);
//        } else {
//            writeToChannel(httpObject);
//        }
//    }
//
//    public void writeDataToChannel(Object msg) {
//        if (msg instanceof ReferenceCounted) {
//            LOG.debug("Retaining reference counted message");
//            ((ReferenceCounted) msg).retain();
//        }
//
//        LOG.debug("Writing: {}", msg);
//
//        try {
//            if (msg instanceof HttpObject) {
//                writeHttp((HttpObject) msg);
//            } else {
//                writeToChannel((ByteBuf) msg);
//            }
//        } finally {
//            LOG.debug("Wrote: {}", msg);
//        }
//    }
//
//    /** 处理出错的时候 */
//    protected ConnectionState exceptionCauseReturn() {
//        return this.curConnectionState;
//    }
//
//    /** */
//    protected abstract ConnectionState readHTTPInitial(I httpObject);
//
//    protected abstract void readHTTPChunk(HttpContent chunk);
//
//    protected abstract void readRaw(ByteBuf buf);
//
//    /** 处理外部过来的http请求 */
//    abstract void handleHttpRequest(final HttpRequest object);
//
//    protected boolean is(ConnectionState state) {
//        return curConnectionState == state;
//    }
//
//    public void resumeAutoRead() {
//        LOG.debug("Resumed reading");
//        this.channel.config().setAutoRead(true);
//    }
//
//    public void stopAutoRead() {
//        LOG.debug("Resumed reading");
//        this.channel.config().setAutoRead(false);
//    }
//
//    public Future<Void> disconnect() {
//        if (channel == null) {
//            return null;
//        } else {
//            final Promise<Void> promise = channel.newPromise();
//            writeToChannel(Unpooled.EMPTY_BUFFER).addListener(
//                    new GenericFutureListener<Future<? super Void>>() {
//                        public void operationComplete(
//                                Future<? super Void> future)
//                                throws Exception {
//                            closeChannel(promise);
//                        }
//                    });
//            return promise;
//        }
//    }
//
//    void closeChannel(final Promise<Void> promise) {
//        channel.close().addListener(
//                new GenericFutureListener<Future<? super Void>>() {
//                    public void operationComplete(
//                            Future<? super Void> future)
//                            throws Exception {
//                        if (future
//                                .isSuccess()) {
//                            promise.setSuccess(null);
//                        } else {
//                            promise.setFailure(future
//                                    .cause());
//                        }
//                    }
//
//                    ;
//                });
//    }
//
//    public void submitChannelTask(Runnable r) {
//        channelHandlerContext.executor().submit(r);
//    }
//
//    /**
//     * <p>
//     * Enables tunneling on this connection by dropping the HTTP related
//     * encoders and decoders, as well as idle timers.
//     * </p>
//     *
//     * <p>
//     * Note - the work is done on the {@link ChannelHandlerContext}'s executor
//     * because {@link ChannelPipeline#remove(String)} can deadlock if called
//     * directly.
//     * </p>
//     */
//    protected ConnectionFlowStep StartTunneling = new ConnectionFlowStep(
//            this, ConnectionState.NEGOTIATING_CONNECT) {
//
//        protected Future execute() {
//            try {
//                ChannelPipeline pipeline = channelHandlerContext.pipeline();
//                if (pipeline.get("encoder") != null) {
//                    pipeline.remove("encoder");
//                }
//                if (pipeline.get("responseWrittenMonitor") != null) {
//                    pipeline.remove("responseWrittenMonitor");
//                }
//                if (pipeline.get("decoder") != null) {
//                    pipeline.remove("decoder");
//                }
//                if (pipeline.get("requestReadMonitor") != null) {
//                    pipeline.remove("requestReadMonitor");
//                }
//                isTunneling = true;
//                return channel.newSucceededFuture();
//            } catch (Throwable t) {
//                return channel.newFailedFuture(t);
//            }
//        }
//    };
//
//    public ConnectionFlowStep getStartTunnelingFlowStep() {
//        return this.StartTunneling;
//    }
//}
