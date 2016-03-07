package org.siltools.sockshttp.proxy.connection;//package org.siltools.sockshttp.proxy;
//
//import io.netty.buffer.ByteBuf;
//import io.netty.buffer.Unpooled;
//import io.netty.channel.ChannelHandlerContext;
//import io.netty.channel.ChannelPipeline;
//import io.netty.handler.codec.http.*;
//import io.netty.handler.timeout.IdleStateHandler;
//import io.netty.util.concurrent.Future;
//import org.apache.commons.codec.binary.Base64;
//import org.apache.commons.lang3.StringUtils;
//import org.siltools.sockshttp.SocksHttpProxyServer;
//import org.siltools.sockshttp.auth.ProxyAuthenticator;
//import org.siltools.sockshttp.def.ConnectionState;
//import org.siltools.sockshttp.def.ServerParams;
//import org.siltools.sockshttp.def.exception.ProxyException;
//import org.siltools.sockshttp.util.HttpResponseUtils;
//import org.siltools.sockshttp.util.SocksHttpProxyUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.IOException;
//import java.io.UnsupportedEncodingException;
//import java.net.UnknownHostException;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.RejectedExecutionException;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import static org.siltools.sockshttp.def.ConnectionState.AWAITING_INITIAL;
//import static org.siltools.sockshttp.def.ConnectionState.DISCONNECT_REQUESTED;
//
///**
// */
//public class ClientToProxyConnection extends ProxyConnection<HttpRequest> implements IClientToProxyConnection {
//
//    private static transient Logger LOG = LoggerFactory.getLogger(ClientToProxyConnection.class);
//
//    private static final Map<String, ProxyConnection> serverConnectionsByHostAndPort = new ConcurrentHashMap<String, ProxyConnection>();
//
//    /** */
//    protected ProxyConnection currentServerConnection;
//
//    /** 是否认证 */
//    private AtomicBoolean authenticated = new AtomicBoolean();
//
//    private HttpRequest currentRequest;
//
//    private AtomicInteger curConServerNum = new AtomicInteger(0);
//
//    public ClientToProxyConnection(SocksHttpProxyServer socksHttpProxyServer, ChannelPipeline pipeline) {
//        super(ConnectionState.AWAITING_INITIAL, socksHttpProxyServer, true);
//        initChannelPipeline(pipeline);
//    }
//
//    public void initChannelPipeline(ChannelPipeline pipeline) {
//        LOG.debug("Begin configuration pipeline");
//
//        pipeline.addLast("clientDecoder", new HttpRequestDecoder(ServerParams.HTTP_DECODER_MAX_INITIAL_LINE_LENGTH,
//                ServerParams.HTTP_DECODER_MAX_CHUNK_SIZE,
//                ServerParams.HTTP_DECODER_MAX_HEADER_SIZE,
//                ServerParams.HTTP_DECODER_VALIDATE_HEADER));
//
////        pipeline.addLast("clientEncoder", new HttpResponseEncoder());
//
//        pipeline.addLast(
//                "idle",
//                new IdleStateHandler(0, 0, proxyServer
//                        .getIdleConnectionTimeout()));
//
//        pipeline.addLast("handler", this);
//    }
//
//    @Override
//    protected ConnectionState readHTTPInitial(HttpRequest httpRequest) {
//        return doReadHttpInitial(httpRequest);
//    }
//
//    @Override
//    protected void readHTTPChunk(HttpContent chunk) {
//        currentServerConnection.writeDataToChannel(chunk);
//    }
//
//    @Override
//    protected void readRaw(ByteBuf buf) {
//        currentServerConnection.writeDataToChannel(buf);
//    }
//
//    @Override
//    protected void handleHttpRequest(HttpRequest object) {
//
//    }
//
//
//    @Override
//    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//        try {
//            if (cause instanceof IOException) {
//                // IOExceptions are expected errors, for example when a browser is killed and aborts a connection.
//                // rather than flood the logs with stack traces for these expected exceptions, we log the message at the
//                // INFO level and the stack trace at the DEBUG level.
//                LOG.info("An IOException occurred on ClientToProxyConnection: " + cause.getMessage());
//                LOG.debug("An IOException occurred on ClientToProxyConnection", cause);
//            } else if (cause instanceof RejectedExecutionException) {
//                LOG.info("An executor rejected a read or write operation on the ClientToProxyConnection (this is normal if the proxy is shutting down). Message: " + cause.getMessage());
//                LOG.debug("A RejectedExecutionException occurred on ClientToProxyConnection", cause);
//            } else if (cause instanceof ProxyException) {
//
//            } else {
//                LOG.error("Caught an exception on ClientToProxyConnection", cause);
//            }
//        } finally {
//            // always disconnect the client when an exception occurs on the channel
//            disconnect();
//        }
//    }
//
//    protected ConnectionState doReadHttpInitial(HttpRequest httpRequest) {
//        if (!httpRequest.uri().startsWith("http://picture.youth.cn/")) {
//            return ConnectionState.DISCONNECT_REQUESTED;
//        }
//
//        currentRequest = SocksHttpProxyUtils.copy(httpRequest);
//
//        String serverHostAndPort = SocksHttpProxyUtils.identifyHostAndPort(httpRequest);
//        if (serverHostAndPort == null || StringUtils.isBlank(serverHostAndPort)) {
//            LOG.warn("No host and port found in {}", httpRequest.uri());
//            boolean keepAlive = writeBadGateway(httpRequest);
//            if (keepAlive) {
//                return AWAITING_INITIAL;
//            } else {
//                return ConnectionState.DISCONNECT_REQUESTED;
//            }
//        }
//
//        currentServerConnection = getIsTunneling() ?
//                this.currentServerConnection
//                : this.serverConnectionsByHostAndPort.get(serverHostAndPort);
//        boolean newConnectionRequired = false;
//        if (SocksHttpProxyUtils.isConnect(httpRequest) || currentServerConnection == null) {
//            LOG.debug(currentServerConnection == null? "Didn't find existing ProxyToServerConnection for: {}":
//                    "Not reusing existing ProxyToServerConnection because request is a CONNECT for: {}",
//                    serverHostAndPort);
//            newConnectionRequired = true;
//        }
//
//        if (newConnectionRequired) {
//            currentServerConnection = ProxyConnectionFactory.createServer(proxyServer, this, httpRequest, serverHostAndPort);
//            if (currentServerConnection == null) {
//                boolean keepAlive = writeBadGateway(httpRequest);
//                resumeAutoRead();
//                if (keepAlive) {
//                    return AWAITING_INITIAL;
//                } else {
//                    return DISCONNECT_REQUESTED;
//                }
//            }
//            serverConnectionsByHostAndPort.put(serverHostAndPort, currentServerConnection);
//        } else {
//            LOG.debug("no need new Connection ");
//        }
//
//        currentServerConnection.handleHttpRequest(httpRequest);
//
//        if (SocksHttpProxyUtils.isConnect(httpRequest)) {
//            return ConnectionState.NEGOTIATING_CONNECT;
//        } else if (SocksHttpProxyUtils.isChunked(httpRequest)) {
//            return ConnectionState.AWAITING_CHUNK;
//        } else {
//            return ConnectionState.AWAITING_INITIAL;
//        }
//    }
//
//    public void serverConnectionSucceeded(IProxyToServerConnection proxyToServerConnection,
//                                          boolean shouldForwardInitialRequest) {
//        resumeReadingIfNecessary();
//        becomeState(shouldForwardInitialRequest ? getCurrentState() : AWAITING_INITIAL);
//    }
//
//    /** */
//    public boolean serverConnectionFlowStarted(IProxyToServerConnection serverConnection) {
//        stopAutoRead();
//        this.curConServerNum.incrementAndGet();
//        return true;
//    }
//
//    public boolean serverConnectionFailed(
//            IProxyToServerConnection serverConnection,
//            ConnectionState lastStateBeforeFailure,
//            Throwable cause) {
//        resumeReadingIfNecessary();
//        HttpRequest initialRequest = serverConnection.getInitialRequest();
//        try {
//            if (serverConnection.connectionFailed(cause)) {
//                LOG.info(
//                        "Failed to connect via chained proxy, falling back to next chained proxy. Last state before failure: {}",
//                        lastStateBeforeFailure, cause);
//                return true;
//            } else {
//                LOG.debug(
//                        "Connection to server failed: {}.  Last state before failure: {}",
//                        serverConnection.getRemoteAddress(),
//                        lastStateBeforeFailure,
//                        cause);
//                connectionFailedUnrecoverably(initialRequest, serverConnection);
//                return false;
//            }
//        } catch (UnknownHostException uhe) {
//            connectionFailedUnrecoverably(initialRequest, serverConnection);
//            return false;
//        }
//    }
//
//    public void respond(IProxyToServerConnection serverConnection, HttpRequest resumeReadingIfNecessary, HttpResponse curHttpResponse,
//                HttpObject httpObject) {
//
//        if (httpObject == null) {
//            forceDisconnect(serverConnection);
//            return;
//        }
//
//        if (httpObject instanceof HttpResponse && currHttpRequest != null) {
//            HttpResponse httpResponse = (HttpResponse) httpObject;
//            // if this HttpResponse does not have any means of signaling the end of the message body other than closing
//            // the connection, convert the message to a "Transfer-Encoding: chunked" HTTP response. This avoids the need
//            // to close the client connection to indicate the end of the message. (Responses to HEAD requests "must be" empty.)
//            if (!SocksHttpProxyUtils.isHead(currHttpRequest) && !SocksHttpProxyUtils.isResponseSelfTerminating(httpResponse)) {
//                // if this is not a FullHttpResponse,  duplicate the HttpResponse from the server before sending it to
//                // the client. this allows us to set the Transfer-Encoding to chunked without interfering with netty's
//                // handling of the response from the server. if we modify the original HttpResponse from the server,
//                // netty will not generate the appropriate LastHttpContent when it detects the connection closure from
//                // the server (see HttpObjectDecoder#decodeLast). (This does not apply to FullHttpResponses, for which
//                // netty already generates the empty final chunk when Transfer-Encoding is chunked.)
//                if (!(httpResponse instanceof FullHttpResponse)) {
//                    HttpResponse duplicateResponse = SocksHttpProxyUtils.copy(httpResponse);
//
//                    // set the httpObject and httpResponse to the duplicated response, to allow all other standard processing
//                    // (filtering, header modification for proxying, etc.) to be applied.
//                    httpObject = httpResponse = duplicateResponse;
//                }
//
//                HttpHeaderUtil.setTransferEncodingChunked(httpResponse, true);
//            }
//
//            //fixHttpVersionHeaderIfNecessary(httpResponse);
//            //modifyResponseHeadersToReflectProxying(httpResponse);
//        }
//
//        writeDataToChannel(httpObject);
//
//        if (SocksHttpProxyUtils.isLastChunk(httpObject)) {
//            writeDataToChannel(Unpooled.EMPTY_BUFFER);
//        }
//
////        boolean closeServerConnection = SocksHttpProxyUtils.shouldCloseHttpConnection(
////                currHttpRequest, curHttpResponse, httpObject);
////        boolean closeClientConnection = SocksHttpProxyUtils.shouldCloseHttpConnection(
////                currHttpRequest, curHttpResponse, httpObject);
////
////        if (closeServerConnection) {
////            LOG.debug("Closing remote connection after writing to client");
////            serverConnection.disconnect();
////        }
////
////        if (closeClientConnection) {
////            LOG.debug("Closing connection to client after writes");
////            disconnect();
////        }
//    }
//
//    public ConnectionFlowStep getRespondCONNECTSuccessful() {
//        return new ConnectionFlowStep(this, ConnectionState.NEGOTIATING_CONNECT) {
//            protected Future<?> execute() {
//                HttpResponse response = HttpResponseUtils.responseFor(HttpVersion.HTTP_1_1,
//                        CONNECTION_ESTABLISHED);
//                return writeToChannel(response);
//            }
//        };
//    };
//
//    private void resumeReadingIfNecessary() {
//        if (this.curConServerNum.decrementAndGet() == 0) {
//            LOG.debug("All servers have finished attempting to connect, resuming reading from client.");
//            resumeAutoRead();
//        }
//    }
//
//    private void forceDisconnect(IProxyToServerConnection serverConnection) {
//        LOG.debug("Forcing disconnect");
//        serverConnection.disconnect();
//        disconnect();
//    }
//
//    private void connectionFailedUnrecoverably(HttpRequest initialRequest, IProxyToServerConnection serverConnection) {
//        // the connection to the server failed, so disconnect the server and remove the ProxyToServerConnection from the
//        // map of open server connections
//        serverConnection.disconnect();
//        this.serverConnectionsByHostAndPort.remove(serverConnection.getServerHostAndPort());
//
//        boolean keepAlive = writeBadGateway(initialRequest);
//        if (keepAlive) {
//            becomeState(AWAITING_INITIAL);
//        } else {
//            becomeState(DISCONNECT_REQUESTED);
//        }
//    }
//
//    private boolean authenticationRequired(HttpRequest request) {
//
//        if (authenticated.get()) {
//            return false;
//        }
//
//        final ProxyAuthenticator authenticator = proxyServer
//                .getProxyAuthenticator();
//
//        if (authenticator == null)
//            return false;
//
//        if (!request.headers().contains(HttpHeaderNames.PROXY_AUTHORIZATION)) {
//            writeAuthenticationRequired();
//            return true;
//        }
//
//        List<CharSequence> values = request.headers().getAll(
//                HttpHeaderNames.PROXY_AUTHORIZATION);
//        String fullValue = values.iterator().next().toString();
//        String value = StringUtils.substringAfter(fullValue, "Basic ")
//                .trim();
//        byte[] decodedValue = Base64.decodeBase64(value);
//        try {
//            String decodedString = new String(decodedValue, "UTF-8");
//            String userName = StringUtils.substringBefore(decodedString,
//                    ":");
//            String password = StringUtils.substringAfter(decodedString,
//                    ":");
//            if (!authenticator.authenticate(userName,
//                    password)) {
//                writeAuthenticationRequired();
//                return true;
//            }
//        } catch (UnsupportedEncodingException e) {
//            LOG.error("Could not decode?", e);
//        }
//
//        LOG.info("Got proxy authorization!");
//        // We need to remove the header before sending the request on.
//        String authentication = request.headers().get(
//                HttpHeaderNames.PROXY_AUTHORIZATION).toString();
//        LOG.info(authentication);
//        request.headers().remove(HttpHeaderNames.PROXY_AUTHORIZATION);
//        authenticated.set(true);
//        return false;
//    }
//
//    private void writeAuthenticationRequired() {
//        String body = "<!DOCTYPE HTML \"-//IETF//DTD HTML 2.0//EN\">\n"
//                + "<html><head>\n"
//                + "<title>407 Proxy Authentication Required</title>\n"
//                + "</head><body>\n"
//                + "<h1>Proxy Authentication Required</h1>\n"
//                + "<p>This server could not verify that you\n"
//                + "are authorized to access the document\n"
//                + "requested.  Either you supplied the wrong\n"
//                + "credentials (e.g., bad password), or your\n"
//                + "browser doesn't understand how to supply\n"
//                + "the credentials required.</p>\n" + "</body></html>\n";
//        DefaultFullHttpResponse response = SocksHttpProxyUtils.responseFor(HttpVersion.HTTP_1_1,
//                HttpResponseStatus.PROXY_AUTHENTICATION_REQUIRED, body);
//        //HttpHeaders.set(response, new Date().toString());
//        response.headers().set("Proxy-Authenticate",
//                "Basic realm=\"Restricted Files\"");
//        writeToChannel(response);
//    }
//
//    /**
//     * Tells the client that something went wrong trying to proxy its request. If the Bad Gateway is a response to
//     * an HTTP HEAD request, the response will contain no body, but the Content-Length header will be set to the
//     * value it would have been if this 502 Bad Gateway were in response to a GET.
//     *
//     * @param httpRequest the HttpRequest that is resulting in the Bad Gateway response
//     * @return true if the connection will be kept open, or false if it will be disconnected
//     */
//    public boolean writeBadGateway(HttpRequest httpRequest) {
//        String body = "Bad Gateway: " + httpRequest.uri();
//        DefaultFullHttpResponse response = HttpResponseUtils.responseFor(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY, body);
//
//        if (SocksHttpProxyUtils.isHead(httpRequest)) {
//            // don't allow any body content in response to a HEAD request
//            response.content().clear();
//        }
//
//        return respondWithShortCircuitResponse(response);
//    }
//
//    /**
//     * Tells the client that the request was malformed or erroneous. If the Bad Request is a response to
//     * an HTTP HEAD request, the response will contain no body, but the Content-Length header will be set to the
//     * value it would have been if this Bad Request were in response to a GET.
//     *
//     * @return true if the connection will be kept open, or false if it will be disconnected
//     */
//    private boolean writeBadRequest(HttpRequest httpRequest) {
//        String body = "Bad Request to URI: " + httpRequest.uri();
//        DefaultFullHttpResponse response = HttpResponseUtils.responseFor(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST, body);
//
//        if (SocksHttpProxyUtils.isHead(httpRequest)) {
//            // don't allow any body content in response to a HEAD request
//            response.content().clear();
//        }
//
//        return respondWithShortCircuitResponse(response);
//    }
//
//    /**
//     * Responds to the client with the specified "short-circuit" response. The response will be sent through the
//     * {@link   # proxyToClientResponse(HttpObject)} filter method before writing it to the client. The client
//     * will not be disconnected, unless the response includes a "Connection: close" header, or the filter returns
//     * a null HttpResponse (in which case no response will be written to the client and the connection will be
//     * disconnected immediately). If the response is not a Bad Gateway or Gateway Timeout response, the response's headers
//     * will be modified to reflect proxying, including adding a Via header, Date header, etc.
//     *
//     * @param httpResponse the response to return to the client
//     * @return true if the connection will be kept open, or false if it will be disconnected.
//     */
//    private boolean respondWithShortCircuitResponse(HttpResponse httpResponse) {
//        // we are sending a response to the client, so we are done handling this request
//        this.currentRequest = null;
//
//        // allow short-circuit messages to close the connection. normally the Connection header would be stripped when modifying
//        // the message for proxying, so save the keep-alive status before the modifications are made.
//        boolean isKeepAlive = HttpHeaderUtil.isKeepAlive(httpResponse);
//
//        // if the response is not a Bad Gateway or Gateway Timeout, modify the headers "as if" the short-circuit response were proxied
////        int statusCode = httpResponse.status().code();
////        if (statusCode != HttpResponseStatus.BAD_GATEWAY.code() && statusCode != HttpResponseStatus.GATEWAY_TIMEOUT.code()) {
////            modifyResponseHeadersToReflectProxying(httpResponse);
////        }
//
//        // restore the keep alive status, if it was overwritten when modifying headers for proxying
//        HttpHeaderUtil.setKeepAlive(httpResponse, isKeepAlive);
//
//        writeDataToChannel(httpResponse);
//
//        if (SocksHttpProxyUtils.isLastChunk(httpResponse)) {
//            writeDataToChannel(Unpooled.EMPTY_BUFFER);
//        }
//
//        if (!HttpHeaderUtil.isKeepAlive(httpResponse)) {
//            disconnect();
//            return false;
//        }
//
//        return true;
//    }
//
//    public IProxyConnectionType getType() {
//        return null;
//    }
//}
