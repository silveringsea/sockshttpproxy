package org.siltools.sockshttp.proxy;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.*;
import org.siltools.sockshttp.SocksHttpProxyServer;
import org.siltools.sockshttp.def.ConnectionState;
import org.siltools.sockshttp.util.HttpResponseUtils;
import org.siltools.sockshttp.util.SocksHttpProxyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 */
public class DefaultClientToProxyConnection extends ProxyConnection<HttpRequest> implements IClientToProxyConnection {

    private AtomicInteger curConServerNum = new AtomicInteger(0);
    private static Logger logger = LoggerFactory.getLogger(DefaultClientToProxyConnection.class);
    private HttpRequest currentHttpRequest;

    public DefaultClientToProxyConnection(SocksHttpProxyServer socksHttpProxyServer, IConnectionPipeline connectionPipeline,
                                   ChannelPipeline pipeline) {
        super(connectionPipeline);
        connectionPipeline.clientConnection(this);
        connectionPipeline.fireInitChannelPipeline(pipeline, IConnectionHandlerContext.CONNECTION_TYPE_CLIENT);
    }

    public IProxyConnectionType getType() {
        return IProxyConnectionType.CLIENT;
    }

    void connectionFailedUnrecoverably(HttpRequest initialRequest, String serverHostPort) {
        // the connection to the server failed, so disconnect the server and remove the ProxyToServerConnection from the
        // map of open server connections
        boolean keepAlive = writeBadGateway(initialRequest);
        if (keepAlive) {
            becomeState(ConnectionState.AWAITING_INITIAL);
        } else {
            becomeState(ConnectionState.DISCONNECT_REQUESTED);
        }
    }

    public void serverConnectionSucceeded(boolean shouldForwardInitialRequest) {
        resumeReadingIfNecessary();
        becomeState(shouldForwardInitialRequest ? getCurrentState() : ConnectionState.AWAITING_INITIAL);
    }

    public boolean serverConnectionFlowStarted(IProxyToServerConnection serverConnection) {
        stopAutoRead();
        this.curConServerNum.incrementAndGet();
        return true;
    }

    public boolean writeBadGateway(HttpRequest httpRequest) {
        String body = "Bad Gateway: " + httpRequest.uri();
        DefaultFullHttpResponse response = HttpResponseUtils.responseFor(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY, body);

        if (SocksHttpProxyUtils.isHead(httpRequest)) {
            // don't allow any body content in response to a HEAD request
            response.content().clear();
        }

        return respondWithShortCircuitResponse(response);
    }

    public HttpRequest getCurHttpRequest() {
        return null;
    }

    /**
      * Responds to the client with the specified "short-circuit" response. The response will be sent through the
      * {@link   # proxyToClientResponse(HttpObject)} filter method before writing it to the client. The client
      * will not be disconnected, unless the response includes a "Connection: close" header, or the filter returns
      * a null HttpResponse (in which case no response will be written to the client and the connection will be
      * disconnected immediately). If the response is not a Bad Gateway or Gateway Timeout response, the response's headers
      * will be modified to reflect proxying, including adding a Via header, Date header, etc.
      *
      * @param httpResponse the response to return to the client
      * @return true if the connection will be kept open, or false if it will be disconnected.
      */
    private boolean respondWithShortCircuitResponse(HttpResponse httpResponse) {
        writeDataToChannel(httpResponse);

        if (SocksHttpProxyUtils.isLastChunk(httpResponse)) {
            writeDataToChannel(Unpooled.EMPTY_BUFFER);
        }

        if (!HttpHeaderUtil.isKeepAlive(httpResponse)) {
            disconnect();
            return false;
        }

        return true;
    }

    private void resumeReadingIfNecessary() {
        if (this.curConServerNum.decrementAndGet() == 0) {
            logger.debug("All servers have finished attempting to connect, resuming reading from client.");
            resumeAutoRead();
        }
    }

    /***************************************************************************
     * Other Lifecycle
     **************************************************************************/

    /**
     * On disconnect of the server, track that we have one fewer connected
     * servers and then disconnect the client if necessary.
     *
     * @param serverConnection
     */
    protected void serverDisconnected(DefaultProxyToServerConnection serverConnection) {
        curConServerNum.decrementAndGet();

        // for non-SSL connections, do not disconnect the client from the proxy, even if this was the last server connection.
        // this allows clients to continue to use the open connection to the proxy to make future requests. for SSL
        // connections, whether we are tunneling or MITMing, we need to disconnect the client because there is always
        // exactly one ClientToProxyConnection per ProxyToServerConnection, and vice versa.
//        if (isTunneling() || isMitming()) {
//            disconnect();
//        }
    }

    protected void exceptionCaught(Throwable cause) {
        try {
            if (cause instanceof IOException) {
                // IOExceptions are expected errors, for example when a browser is killed and aborts a connection.
                // rather than flood the logs with stack traces for these expected exceptions, we log the message at the
                // INFO level and the stack trace at the DEBUG level.
                logger.info("An IOException occurred on ClientToProxyConnection: " + cause.getMessage());
                logger.debug("An IOException occurred on ClientToProxyConnection", cause);
            } else if (cause instanceof RejectedExecutionException) {
                logger.info("An executor rejected a read or write operation on the ClientToProxyConnection (this is normal if the proxy is shutting down). Message: " + cause.getMessage());
                logger.debug("A RejectedExecutionException occurred on ClientToProxyConnection", cause);
            } else {
                logger.error("Caught an exception on ClientToProxyConnection", cause);
            }
        } finally {
            // always disconnect the client when an exception occurs on the channel
            disconnect();
        }
    }

}
