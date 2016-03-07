package org.siltools.sockshttp.proxy;

import io.netty.handler.codec.http.HttpRequest;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * Created by Administrator on 1/13/2016.
 */
public interface IProxyToServerConnection extends IProxyConnection {
    void connectionSucceeded(boolean shouldForwardInitalRequest);

    boolean connectionFailed(Throwable cause) throws UnknownHostException;

    HttpRequest getInitialRequest();

    String getServerHostAndPort();

    InetSocketAddress getRemoteAddress();

//    void serverConnectionFlowStarted();

//    boolean serverConnectionFailed(ConnectionState connectionState, Throwable cause);

    void resentHttpRequest(HttpRequest httpRequest);

//    Future startConnect(HttpRequest httpRequest);
}
