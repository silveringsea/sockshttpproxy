package org.siltools.sockshttp.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;

/**
 * Created by Administrator on 3/2/2016.
 */
public abstract class ConnectionHandlerAdapter implements IConnectionHandler {

//    @Skip
//    public void readHTTPInitial(IConnectionHandlerContext ctx, Object httpObject) {
//        ctx.fireReadHTTPInitial(httpObject);
//    }


    @Skip
    public void initChannelPipeline(IConnectionHandlerContext ctx, ChannelPipeline channelPipeline, int connectionType) {
        if (connectionType == connectionType()) {
            initChannelPipeline(ctx, channelPipeline);
            ctx.fireInitChannelPipeline(channelPipeline, connectionType);
        }
    }

    public void initChannelPipeline(IConnectionHandlerContext ctx, ChannelPipeline channelPipeline) {

    }

    @Skip
    public void readRaw(IConnectionHandlerContext ctx, ByteBuf buf) {
        ctx.fireReadRaw(buf);
    }
    @Skip
    public void inboundExceptionCaught(IConnectionHandlerContext ctx, Throwable cause) throws Exception {
        ctx.fireInboundExceptionCaught(cause, ctx);
    }

    @Skip
    public void outboundExceptionCaught(IConnectionHandlerContext ctx, Throwable cause) throws Exception {
        ctx.fireOutboundExceptionCaught(cause, ctx);
    }

    @Skip
    public boolean shouldExecuteOnEventLoop(int stepFlag) {
        return false;
    }

    public int connectionType() {
        return 0;
    }

    @Skip
    public void messageReceive(IConnectionHandlerContext ctx, Object... objects) {
        ctx.fireMessageReceive(objects);
    }

    @Skip
    public void proxyStateChange(IConnectionHandlerContext ctx, Object... objects) {
        ctx.fireProxyStateChange(objects);
    }

    @Skip
    public void readHTTPChunk(IConnectionHandlerContext ctx, Object... chunk) {
        ctx.fireReadHTTPChunk(chunk);
    }

    @Skip
    public void serverConnectedSucc(IConnectionHandlerContext ctx, Object... objects) {
        ctx.fireServerConnectedSucc(objects);
    }


    @Skip
    public void serverConnectedFail(IConnectionHandlerContext ctx, Object ...objects) {
        ctx.fireServerConnectedFail(objects);
    }

    @Skip
    public void clientConnectTimeout(IConnectionHandlerContext ctx, Throwable cause) {
        ctx.fireClientConnectTimeout(cause);
    }

    @Skip
    public Future<InetSocketAddress> remoteInetSocketAddress(IConnectionHandlerContext ctx, HttpRequest httpRequest) {
        return ctx.fireRemoteInetSocketAddress(httpRequest);
    }

    public String getName() {
        return this.getClass().getName();
    }
}
