package org.siltools.sockshttp.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleStateHandler;
import org.siltools.sockshttp.def.ConnectionState;
import org.siltools.sockshttp.def.ServerParams;
import org.siltools.sockshttp.proxy.connection.ServerConnectPool;
import org.siltools.sockshttp.util.HttpResponseUtils;
import org.siltools.sockshttp.util.SocksHttpProxyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class FlowClientToProxyConnectionHandler extends ClientConnHandlerAdapter {

    private static Logger logger = LoggerFactory.getLogger(FlowClientToProxyConnectionHandler.class);

    @Override
    public void initChannelPipeline(IConnectionHandlerContext ctx, ChannelPipeline channelPipeline) {
        logger.debug("Begin configuration pipeline");

        channelPipeline.addLast("clientDecoder", new HttpRequestDecoder(ServerParams.HTTP_DECODER_MAX_INITIAL_LINE_LENGTH,
                ServerParams.HTTP_DECODER_MAX_CHUNK_SIZE,
                ServerParams.HTTP_DECODER_MAX_HEADER_SIZE,
                ServerParams.HTTP_DECODER_VALIDATE_HEADER));

        channelPipeline.addLast(
                "idle",
                new IdleStateHandler(0, 0, ctx.pipeline().proxyServer()
                        .getIdleConnectionTimeout()));

        channelPipeline.addLast("handler", (ChannelHandler) ctx.clientToProxyConnection());
    }

    @Override
    public void messageReceive(IConnectionHandlerContext ctx, Object... objects) {
        HttpRequest httpRequest = (HttpRequest)objects[0];
        doReadHttpInitial(ctx, httpRequest);
        super.messageReceive(ctx, objects);
    }

    protected ConnectionState doReadHttpInitial(IConnectionHandlerContext ctx, HttpRequest httpRequest) {
        String serverHostAndPort = SocksHttpProxyUtils.identifyHostAndPort(httpRequest);
        IProxyConnection currentServerConnection = ServerConnectPool.getInstance().get(serverHostAndPort);
        boolean newConnectionRequired = false;
        if (SocksHttpProxyUtils.isConnect(httpRequest) || currentServerConnection == null) {
            logger.debug(currentServerConnection == null? "Didn't find existing ProxyToServerConnection for: {}":
                            "Not reusing existing ProxyToServerConnection because request is a CONNECT for: {}",
                    serverHostAndPort);
            newConnectionRequired = true;
        }

        if (newConnectionRequired) {
            currentServerConnection = ProxyConnectionFactory.createServer(ctx, httpRequest, serverHostAndPort);
            if (currentServerConnection == null) {
                HttpResponse httpResponse = HttpResponseUtils.getBadGateway(httpRequest);
                ctx.connection().writeDataToChannel(httpResponse);
                boolean keepAlive = ctx.clientToProxyConnection().writeBadGateway(httpRequest);
                ctx.connection().resumeAutoRead();
                return keepAlive? ConnectionState.AWAITING_INITIAL: ConnectionState.DISCONNECT_REQUESTED;
            }
            ServerConnectPool.getInstance().put(serverHostAndPort, currentServerConnection);
            ctx.pipeline().proxyConnection(currentServerConnection);
        } else {
            logger.debug("no need new Connection ");
        }

        if (SocksHttpProxyUtils.isConnect(httpRequest)) {
            return ConnectionState.NEGOTIATING_CONNECT;
        } else if (SocksHttpProxyUtils.isChunked(httpRequest)) {
            return ConnectionState.AWAITING_CHUNK;
        } else {
            return ConnectionState.AWAITING_INITIAL;
        }
    }

    @Override
    public void readHTTPChunk(IConnectionHandlerContext ctx, Object ...objects) {
        if (objects == null) {
            return;
        }

        HttpObject httpObject = (HttpObject)objects[0];
        if (httpObject instanceof HttpResponse && ctx.clientToProxyConnection().getCurHttpRequest() != null) {
            HttpResponse httpResponse = (HttpResponse) httpObject;
            if (!SocksHttpProxyUtils.isHead(ctx.clientToProxyConnection().getCurHttpRequest()) &&
                    !SocksHttpProxyUtils.isResponseSelfTerminating(httpResponse)) {
                if (!(httpResponse instanceof FullHttpResponse)) {
                    HttpResponse duplicateResponse = SocksHttpProxyUtils.copy(httpResponse);
                    httpObject = httpResponse = duplicateResponse;
                }

                HttpHeaderUtil.setTransferEncodingChunked(httpResponse, true);
            }

        }

        ctx.connection().writeDataToChannel(httpObject);

        if (SocksHttpProxyUtils.isLastChunk(httpObject)) {
            ctx.connection().writeDataToChannel(Unpooled.EMPTY_BUFFER);
        }
    }

    @Override
    public void readRaw(IConnectionHandlerContext ctx, ByteBuf buf) {
        super.readRaw(ctx, buf);
    }

    @Override
    public void serverConnectedSucc(IConnectionHandlerContext ctx, Object ...object) {
        if (ctx.connection() instanceof DefaultClientToProxyConnection) {
            ((DefaultClientToProxyConnection)ctx.connection()).serverConnectionSucceeded((Boolean) object[0]);
        }
        super.serverConnectedSucc(ctx, object);
    }

    @Override
    public void serverConnectedFail(IConnectionHandlerContext ctx, Object ...objects) {
        if (ctx.connection() instanceof DefaultClientToProxyConnection &&
                objects != null &&
                objects.length == 2 &&
                objects[0] instanceof HttpRequest) {
            ServerConnectPool.getInstance().remove((String)objects[1]);
                    ((DefaultClientToProxyConnection) ctx.connection())
                    .connectionFailedUnrecoverably((HttpRequest) objects[0], (String) objects[1]);
        }
    }

}
