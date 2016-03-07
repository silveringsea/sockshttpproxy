package org.siltools.sockshttp.proxy.connection;

import io.netty.handler.codec.http.HttpObject;

/**
 * Created by Administrator on 1/13/2016.
 */
public abstract class AbstractClientToProxyConnection<I extends HttpObject>  {
//    private static final HttpResponseStatus CONNECTION_ESTABLISHED = new HttpResponseStatus(
//            200, "HTTP/1.1 200 Connection established");
//    /**
//     * Tells the Client that its HTTP CONNECT request was successful.
//     */
////    protected ConnectionFlowStep getRespondCONNECTSuccessful() {
////        return new ConnectionFlowStep(this, ConnectionState.NEGOTIATING_CONNECT) {
////            protected Future<?> execute() {
////                HttpResponse response = HttpResponseUtils.responseFor(HttpVersion.HTTP_1_1,
////                        CONNECTION_ESTABLISHED);
////                return writeToChannel(response);
////            }
////        };
////    };
//
//    protected AbstractClientToProxyConnection(ConnectionState initialState, SocksHttpProxyServer proxyServer, boolean runsAsSslClient) {
//        super(initialState, proxyServer, runsAsSslClient);
//    }

//    public abstract void respond(IProxyToServerConnection serverConnection, HttpRequest currHttpRequest, HttpResponse curHttpResponse,
//                 HttpObject httpObject);

}
