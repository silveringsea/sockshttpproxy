package org.siltools.sockshttp.proxy;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaderUtil;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import org.apache.commons.lang3.StringUtils;
import org.siltools.sockshttp.def.ConnectionState;
import org.siltools.sockshttp.util.HttpResponseUtils;
import org.siltools.sockshttp.util.SocksHttpProxyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class ClientToProxyVerifyHandler extends ClientConnHandlerAdapter {

    private static Logger logger = LoggerFactory.getLogger(ClientToProxyVerifyHandler.class);

    @Override
    public void messageReceive(IConnectionHandlerContext ctx, Object... objects) {
        HttpRequest httpRequest = (HttpRequest)objects[0];
        SocksHttpProxyUtils.retainObject(httpRequest);
        ConnectionState connectionState = doReadHttpInitial(ctx, httpRequest);
        if (connectionState != ConnectionState.DISCONNECT_REQUESTED) {
            super.messageReceive(ctx, objects);
        }
    }

    protected ConnectionState doReadHttpInitial(IConnectionHandlerContext ctx, HttpRequest httpRequest) {
        if (!httpRequest.uri().startsWith("http://picture.youth.cn/")) {
            return ConnectionState.DISCONNECT_REQUESTED;
        }

        ConnectionState nextState = ConnectionState.AWAITING_INITIAL;
        HttpRequest currentRequest = SocksHttpProxyUtils.copy(httpRequest);
        String serverHostAndPort = SocksHttpProxyUtils.identifyHostAndPort(httpRequest);
        if (serverHostAndPort == null || StringUtils.isBlank(serverHostAndPort)) {
            logger.warn("No host and port found in {}", httpRequest.uri());
            boolean keepAlive = true;
            HttpResponse httpResponse = HttpResponseUtils.getBadGateway(httpRequest);
            ctx.connection().writeDataToChannel(httpResponse);

            if (SocksHttpProxyUtils.isLastChunk(httpResponse)) {
                ctx.connection().writeDataToChannel(Unpooled.EMPTY_BUFFER);
            }

            if (!HttpHeaderUtil.isKeepAlive(httpResponse)) {
                ctx.pipeline().fireReadHTTPChunk(httpResponse);
//                disconnect();
                keepAlive = false;
            }

            if (!keepAlive) {
                nextState = ConnectionState.DISCONNECT_REQUESTED;
            }
        }

        return nextState;
    }


}
