package org.siltools.sockshttp.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCounted;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import org.siltools.sockshttp.def.ConnectionState;
import org.siltools.sockshttp.util.HttpResponseUtils;
import org.siltools.sockshttp.util.SocksHttpProxyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Administrator on 2015/12/29.
 */
public abstract class ProxyConnection<I extends HttpObject> extends SimpleChannelInboundHandler<Object>
        implements IProxyConnection {


    private static transient Logger logger = LoggerFactory.getLogger(ProxyConnection.class);

    /** */
    protected volatile ChannelHandlerContext channelHandlerContext;
    protected volatile Channel channel;

    protected ConnectionState curConnectionState;
    protected IConnectionPipeline connectionPipeline;

    protected ProxyConnection(IConnectionPipeline connectionPipeline) {
        this.connectionPipeline = connectionPipeline;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        try {
            this.channel = ctx.channel();
            this.channelHandlerContext = ctx;
//            this.proxyServer.registerChannel(this.channel);
        } catch (Exception e) {
            logger.error("channelRegisterError", e);
        } finally {
            super.channelRegistered(ctx);
        }
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
        connectionPipeline.fireMessageReceive(ctx, msg);
//        ConnectionState state = connectionPipeline.connection().getCurrentState();
//////        IProxyConnectionType type = connectionPipeline.connection().getType();
////
//        if (state == ConnectionState.AWAITING_INITIAL) {
//            connectionPipeline.fireReadHTTPInitial(msg);
//        }
//
//        if (state == ConnectionState.AWAITING_CHUNK) {
//            connectionPipeline.fireReadHTTPChunk(msg);
//        }
//
//        if (state == ConnectionState.AWAITING_CONNECT_OK) {
////            connectionPipeline.fireConnect()
//        }
    }

    public void resumeAutoRead() {
        logger.debug("Resumed reading");
        this.channel.config().setAutoRead(true);
    }

    public void stopAutoRead() {
        logger.debug("Resumed reading");
        this.channel.config().setAutoRead(false);
    }

    public Future<Void> disconnect() {
        if (channel == null) {
            return null;
        } else {
            final Promise<Void> promise = channel.newPromise();
            HttpResponseUtils.writeToChannel(channel, Unpooled.EMPTY_BUFFER).addListener(
                    new GenericFutureListener<Future<? super Void>>() {
                        public void operationComplete(
                                Future<? super Void> future)
                                throws Exception {
                            closeChannel(promise);
                        }
                    });
            return promise;
        }
    }

    void closeChannel(final Promise<Void> promise) {
        channel.close().addListener(
                new GenericFutureListener<Future<? super Void>>() {
                    public void operationComplete(
                            Future<? super Void> future)
                            throws Exception {
                        if (future
                                .isSuccess()) {
                            promise.setSuccess(null);
                        } else {
                            promise.setFailure(future
                                    .cause());
                        }
                    }

                    ;
                });
    }

    public void submitChannelTask(Runnable r) {
        channelHandlerContext.executor().submit(r);
    }

    public void becomeState(ConnectionState connectionState) {
        curConnectionState = connectionState;
    }

    public ConnectionState getCurrentState() {
        return curConnectionState;
    }

    public void writeDataToChannel(Object msg) {
        if (msg instanceof ReferenceCounted) {
            logger.debug("Retaining reference counted message");
            ((ReferenceCounted) msg).retain();
        }

        logger.debug("Writing: {}", msg);

        try {
            if (msg instanceof HttpObject) {
                writeHttp((HttpObject) msg);
            } else {
                channel.writeAndFlush((ByteBuf) msg);
            }
        } finally {
            logger.debug("Wrote: {}", msg);
        }
    }

    protected void writeHttp(HttpObject httpObject) {
        if (SocksHttpProxyUtils.isLastChunk(httpObject)) {
            if (channel != null && channel.isActive())
                channel.write(httpObject);
            logger.debug("Writing an empty buffer to signal the end of our chunked transfer");
            channel.writeAndFlush(Unpooled.EMPTY_BUFFER);
        } else {
            channel.writeAndFlush(httpObject);
        }
    }

    /**
     * <p>
     * We're looking for {@link IdleStateEvent}s to see if we need to
     * disconnect.
     * </p>
     *
     * <p>
     * Note - we don't care what kind of IdleState we got. Thanks to <a
     * href="https://github.com/qbast">qbast</a> for pointing this out.
     * </p>
     */
    @Override
    public final void userEventTriggered(ChannelHandlerContext ctx, Object evt)
            throws Exception {
        try {
            if (evt instanceof IdleStateEvent) {
                connectionPipeline.fireClientConnectTimeout(ctx, evt);
            }
        } finally {
            super.userEventTriggered(ctx, evt);
        }
    }

    /**
     * Only once the Netty Channel is active to we recognize the ProxyConnection
     * as connected.
     */
    @Override
    public final void channelActive(ChannelHandlerContext ctx) throws Exception {
        try {
//            connectionPipeline.fireConnect(ctx);
        } finally {
            super.channelActive(ctx);
        }
    }

    /**
     * As soon as the Netty Channel is inactive, we recognize the
     * ProxyConnection as disconnected.
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        try {
//            connectionPipeline.fireDisconnect(ctx, null);
        } finally {
            super.channelInactive(ctx);
        }
    }

    public boolean isSaturated() {
        return !this.channel.isWritable();
    }

    /**
     * Callback that's invoked if this connection becomes saturated.
     */
    protected void becameSaturated() {
        logger.debug("Became saturated");
    }

    /**
     * Callback that's invoked when this connection becomes writeable again.
     */
    protected void becameWritable() {
        logger.debug("Became writeable");
    }
}
