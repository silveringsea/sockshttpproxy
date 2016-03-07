package org.siltools.sockshttp.proxy;

import io.netty.handler.codec.http.HttpRequest;

/**
 * Created by Administrator on 1/13/2016.
 */
public interface IClientToProxyConnection extends IProxyConnection {
//    boolean serverConnectionFailed(IProxyToServerConnection proxyToServerConnection, ConnectionState lastStateBeforeFailure, Throwable cause);
//
//    void serverConnectionSucceeded(IProxyToServerConnection proxyToServerConnection, boolean shouldForwardInitialRequest);
//
//    boolean serverConnectionFlowStarted(IProxyToServerConnection serverConnection);
//
//    void respond(IProxyToServerConnection serverConnection, HttpRequest currHttpRequest, HttpResponse curHttpResponse,
//                 HttpObject httpObject);
//
//    ConnectionFlowStep getRespondCONNECTSuccessful();

    boolean writeBadGateway(HttpRequest httpRequest);

    HttpRequest getCurHttpRequest();
}
