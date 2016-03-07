package org.siltools.sockshttp.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCounted;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Set;

/**
 * Created by Administrator on 1/12/2016.
 */
public class HttpResponseUtils {

    /**
     * Used for case-insensitive comparisons when parsing Connection header values.
     */
    private static final String LOWERCASE_TRANSFER_ENCODING_HEADER = HttpHeaderNames.TRANSFER_ENCODING.toString()
            .toLowerCase(Locale.US);

    private static final Logger logger = LoggerFactory.getLogger(HttpResponseUtils.class);
    /**
     * Tells the client that something went wrong trying to proxy its request. If the Bad Gateway is a response to
     * an HTTP HEAD request, the response will contain no body, but the Content-Length header will be set to the
     * value it would have been if this 502 Bad Gateway were in response to a GET.
     *
     * @param httpRequest the HttpRequest that is resulting in the Bad Gateway response
     * @return true if the connection will be kept open, or false if it will be disconnected
     */
    public static HttpResponse getBadGateway(HttpRequest httpRequest) {
        String body = "Bad Gateway: " + httpRequest.uri();
        DefaultFullHttpResponse response = HttpResponseUtils.responseFor(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY, body);

        if (SocksHttpProxyUtils.isHead(httpRequest)) {
            // don't allow any body content in response to a HEAD request
            response.content().clear();
        }

        return respondWithShortCircuitResponse(response);
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
    public static HttpResponse respondWithShortCircuitResponse(HttpResponse httpResponse) {

        // allow short-circuit messages to close the connection. normally the Connection header would be stripped when modifying
        // the message for proxying, so save the keep-alive status before the modifications are made.
        boolean isKeepAlive = HttpHeaderUtil.isKeepAlive(httpResponse);

        // if the response is not a Bad Gateway or Gateway Timeout, modify the headers "as if" the short-circuit response were proxied
        int statusCode = httpResponse.status().code();
        if (statusCode != HttpResponseStatus.BAD_GATEWAY.code() && statusCode != HttpResponseStatus.GATEWAY_TIMEOUT.code()) {
            modifyResponseHeadersToReflectProxying(httpResponse);
        }

        // restore the keep alive status, if it was overwritten when modifying headers for proxying
        HttpHeaderUtil.setKeepAlive(httpResponse, isKeepAlive);

        return httpResponse;
    }

    public static void writeDataToChannel(final io.netty.channel.Channel channel, Object msg) {
        if (msg instanceof ReferenceCounted) {
            //LOG.debug("Retaining reference counted message");
            ((ReferenceCounted) msg).retain();
        }

        //LOG.debug("Writing: {}", msg);

        try {
            if (msg instanceof HttpObject) {
                writeHttp(channel, (HttpObject) msg);
            } else {
                writeToChannel(channel, (ByteBuf) msg);
            }
        } finally {
            //LOG.debug("Wrote: {}", msg);
        }
    }

    public static void writeHttp(final io.netty.channel.Channel channel, HttpObject httpObject) {
        if (SocksHttpProxyUtils.isLastChunk(httpObject)) {
            if (channel != null && channel.isActive())
                channel.write(httpObject);
//            LOG.debug("Writing an empty buffer to signal the end of our chunked transfer");
            writeToChannel(channel, Unpooled.EMPTY_BUFFER);
        } else {
            writeToChannel(channel, httpObject);
        }
    }

    public static ChannelFuture writeToChannel(final io.netty.channel.Channel channel, final Object msg) {
        return channel.writeAndFlush(msg);
    }

    /**
     * Tells the client that the connection to the server, or possibly to some intermediary service (such as DNS), timed out.
     * If the Gateway Timeout is a response to an HTTP HEAD request, the response will contain no body, but the
     * Content-Length header will be set to the value it would have been if this 504 Gateway Timeout were in response to a GET.
     *
     * @param httpRequest the HttpRequest that is resulting in the Gateway Timeout response
     * @return true if the connection will be kept open, or false if it will be disconnected
     */
    private static HttpResponse writeGatewayTimeout(HttpRequest httpRequest) {
        String body = "Gateway Timeout";
        DefaultFullHttpResponse response = HttpResponseUtils.responseFor(HttpVersion.HTTP_1_1,
                HttpResponseStatus.GATEWAY_TIMEOUT, body);

        if (httpRequest != null && SocksHttpProxyUtils.isHead(httpRequest)) {
            // don't allow any body content in response to a HEAD request
            response.content().clear();
        }

        return respondWithShortCircuitResponse(response);
    }
//
//    boolean respondWithShortCircuitResponse(HttpResponse httpResponse) {
//        // we are sending a response to the client, so we are done handling this request
//        this.currentRequest = null;
//
//        HttpResponse filteredResponse = (HttpResponse) currentFilters.proxyToClientResponse(httpResponse);
//        if (filteredResponse == null) {
//            disconnect();
//            return false;
//        }
//
//        // allow short-circuit messages to close the connection. normally the Connection header would be stripped when modifying
//        // the message for proxying, so save the keep-alive status before the modifications are made.
//        boolean isKeepAlive = HttpHeaders.isKeepAlive(httpResponse);
//
//        // if the response is not a Bad Gateway or Gateway Timeout, modify the headers "as if" the short-circuit response were proxied
//        int statusCode = httpResponse.getStatus().code();
//        if (statusCode != HttpResponseStatus.BAD_GATEWAY.code() && statusCode != HttpResponseStatus.GATEWAY_TIMEOUT.code()) {
//            modifyResponseHeadersToReflectProxying(httpResponse);
//        }
//
//        // restore the keep alive status, if it was overwritten when modifying headers for proxying
//        HttpHeaders.setKeepAlive(httpResponse, isKeepAlive);
//
//        write(httpResponse);
//
//        if (ProxyUtils.isLastChunk(httpResponse)) {
//            writeEmptyBuffer();
//        }
//
//        if (!HttpHeaders.isKeepAlive(httpResponse)) {
//            disconnect();
//            return false;
//        }
//
//        return true;
//    }
//
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

    /**
     * If and only if our proxy is not running in transparent mode, modify the
     * response headers to reflect that it was proxied.
     *
     * @param httpResponse
     * @return
     */
    private static void modifyResponseHeadersToReflectProxying(
        HttpResponse httpResponse) {
//        if (!proxyServer.isTransparent()) {
//        }
        HttpHeaders headers = httpResponse.headers();

        stripConnectionTokens(headers);
        stripHopByHopHeaders(headers);

        /*
         * RFC2616 Section 14.18
         *
         * A received message that does not have a Date header field MUST be
         * assigned one by the recipient if the message will be cached by
         * that recipient or gatewayed via a protocol which requires a Date.
         */
        if (!headers.contains(HttpHeaderNames.DATE)) {
//            httpResponse.headers().setObject(new Date());
        }
    }

    /**
     * Remove sdch from encodings we accept since we can't decode it.
     *
     * @param headers
     *            The headers to modify
     */
    private void removeSDCHEncoding(HttpHeaders headers) {
        String ae = headers.get(HttpHeaderNames.ACCEPT_ENCODING).toString();
        if (StringUtils.isNotBlank(ae)) {
            //
            String noSdch = ae.replace(",sdch", "").replace("sdch", "");
            headers.set(HttpHeaderNames.ACCEPT_ENCODING, noSdch);
            //LOG.debug("Removed sdch and inserted: {}", noSdch);
        }
    }

    /**
     * Switch the de-facto standard "Proxy-Connection" header to "Connection"
     * when we pass it along to the remote host. This is largely undocumented
     * but seems to be what most browsers and servers expect.
     *
     * @param headers
     *            The headers to modify
     */
    private void switchProxyConnectionHeader(HttpHeaders headers) {
        String proxyConnectionKey = "Proxy-Connection";
        if (headers.contains(proxyConnectionKey)) {
            String header = headers.get(proxyConnectionKey).toString();
            headers.remove(proxyConnectionKey);
            headers.set(HttpHeaderNames.CONNECTION, header);
        }
    }

    /**
     * RFC2616 Section 14.10
     *
     * HTTP/1.1 proxies MUST parse the Connection header field before a message
     * is forwarded and, for each connection-token in this field, remove any
     * header field(s) from the message with the same name as the
     * connection-token.
     *
     * @param headers
     *            The headers to modify
     */
    private static void stripConnectionTokens(HttpHeaders headers) {
        if (headers.contains(HttpHeaderNames.CONNECTION)) {
            for (CharSequence headerValue : headers.getAll(HttpHeaderNames.CONNECTION)) {
                for (String connectionToken : SocksHttpProxyUtils
                        .splitCommaSeparatedHeaderValues(headerValue.toString())) {
                    // do not strip out the Transfer-Encoding header if it is specified in the Connection header, since LittleProxy does not
                    // normally modify the Transfer-Encoding of the message.
                    if (!LOWERCASE_TRANSFER_ENCODING_HEADER.equals(connectionToken.toLowerCase(Locale.US))) {
                        headers.remove(connectionToken);
                    }
                }
            }
        }
    }

    /**
     * Removes all headers that should not be forwarded. See RFC 2616 13.5.1
     * End-to-end and Hop-by-hop Headers.
     *
     * @param headers
     *            The headers to modify
     */
    public static void stripHopByHopHeaders(HttpHeaders headers) {
        Set<CharSequence> headerNames = headers.names();
        for (CharSequence headerName : headerNames) {
            if (SocksHttpProxyUtils.shouldRemoveHopByHopHeader(headerName.toString())) {
                headers.remove(headerName);
            }
        }
    }

    /**
     * Factory for {@link DefaultFullHttpResponse}s.
     *
     * @param httpVersion
     * @param status
     * @return
     */
    public static DefaultFullHttpResponse responseFor(HttpVersion httpVersion,
                                                HttpResponseStatus status) {
        return responseFor(httpVersion, status, (ByteBuf) null, 0);
    }

    /**
     * Factory for {@link DefaultFullHttpResponse}s.
     *
     * @param httpVersion
     * @param status
     * @param body
     * @return
     */
    public static DefaultFullHttpResponse responseFor(HttpVersion httpVersion,
                                                         HttpResponseStatus status, String body) {
        byte[] bytes = body.getBytes(Charset.forName("UTF-8"));
        ByteBuf content = Unpooled.copiedBuffer(bytes);
        return responseFor(httpVersion, status, content, bytes.length);
    }

    /**
     * Factory for {@link DefaultFullHttpResponse}s.
     *
     * @param httpVersion
     * @param status
     * @param body
     * @param contentLength
     * @return
     */
    public static DefaultFullHttpResponse responseFor(HttpVersion httpVersion,
                                                         HttpResponseStatus status, ByteBuf body, int contentLength) {
        DefaultFullHttpResponse response = body != null ? new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status, body)
                : new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
        if (body != null) {
            response.headers().setObject(HttpHeaderNames.CONTENT_LENGTH, contentLength);
            response.headers().set("Content-Type", "text/html; charset=UTF-8");
        }
        return response;
    }
}
