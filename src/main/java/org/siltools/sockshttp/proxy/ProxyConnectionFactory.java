package org.siltools.sockshttp.proxy;

import io.netty.handler.codec.http.HttpRequest;

/**
 * Created by Administrator on 1/13/2016.
 */
public class ProxyConnectionFactory {

//    public static ProxyConnection createServer(SocksHttpProxyServer proxyServer, ClientToProxyConnection clientConnection,
//                                        HttpRequest initialHttpRequest, String serverHostAndPort) {
//
//        return new ProxyToServerConnection(proxyServer, clientConnection, initialHttpRequest, serverHostAndPort);
//    }

    public static IProxyToServerConnection createServer(IConnectionHandlerContext ctx,
                                                        HttpRequest initialHttpRequest,
                                                        String serverHostAndPort) {

        return new DefaultProxyToServerConnection(ctx.pipeline(), initialHttpRequest, serverHostAndPort);
    }
}
