package org.siltools.sockshttp.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.concurrent.Future;

import java.lang.annotation.*;
import java.net.InetSocketAddress;

/**
 */
public interface IConnectionHandler {

    void initChannelPipeline(IConnectionHandlerContext ctx, ChannelPipeline channelPipeline, int connectionType);

    void messageReceive(IConnectionHandlerContext ctx, Object... objects);

    void proxyStateChange(IConnectionHandlerContext ctx, Object... objects);

//    void readHTTPInitial(IConnectionHandlerContext ctx, Object ...objects);

    void readHTTPChunk(IConnectionHandlerContext ctx, Object... chunk);

    void readRaw(IConnectionHandlerContext ctx, ByteBuf buf);

//    void proxyAuthentication(IConnectionHandlerContext ctx, Object httpObject);

//    void negotiatingConnect(IConnectionHandlerContext ctx, Object object);

//    void connecting(IConnectionHandlerContext ctx, Object ...object);
//
//    void connectAndWrite(IConnectionHandlerContext ctx, Object ...object);
//
//    void disconnecting(IConnectionHandlerContext ctx, Object ...object);
//
//    void disconnected(IConnectionHandlerContext ctx, Object object);

    void serverConnectedSucc(IConnectionHandlerContext ctx, Object... object);

    void serverConnectedFail(IConnectionHandlerContext ctx, Object... objects);

    void inboundExceptionCaught(IConnectionHandlerContext ctx, Throwable cause) throws Exception;

    void outboundExceptionCaught(IConnectionHandlerContext ctx, Throwable cause) throws Exception;

    void clientConnectTimeout(IConnectionHandlerContext ctx, Throwable cause);

    Future<InetSocketAddress> remoteInetSocketAddress(IConnectionHandlerContext ctx, HttpRequest httpRequest);

    boolean shouldExecuteOnEventLoop(int stepFlag);

    int connectionType();

    String getName();

    @Inherited
    @Documented
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Sharable {
        // no value
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Skip {
        // no value
    }
}
