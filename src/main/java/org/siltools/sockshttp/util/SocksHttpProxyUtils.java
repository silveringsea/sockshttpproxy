package org.siltools.sockshttp.util;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.udt.nio.NioUdtProvider;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCounted;
import org.apache.commons.lang3.StringUtils;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 工具类
 */
public class SocksHttpProxyUtils {
    /**
     * Hop-by-hop headers that should be removed when proxying, as defined by the HTTP 1.1 spec, section 13.5.1
     * (http://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html#sec13.5.1). Transfer-Encoding is NOT included in this list, since LittleProxy
     * does not typically modify the transfer encoding. See also {@link #shouldRemoveHopByHopHeader(String)}.
     *
     * Header names are stored as lowercase to make case-insensitive comparisons easier.
     */
    private static final Set<String> SHOULD_NOT_PROXY_HOP_BY_HOP_HEADERS = ImmutableSet.of(
            HttpHeaderNames.CONNECTION.toString().toLowerCase(Locale.US),
            HttpHeaderNames.PROXY_AUTHENTICATE.toString().toLowerCase(Locale.US),
            HttpHeaderNames.PROXY_AUTHORIZATION.toString().toLowerCase(Locale.US),
            HttpHeaderNames.TE.toString().toLowerCase(Locale.US),
            HttpHeaderNames.TRAILER.toString().toLowerCase(Locale.US),
            /*  Note: Not removing Transfer-Encoding since LittleProxy does not normally re-chunk content.
                HttpHeaders.Names.TRANSFER_ENCODING.toLowerCase(Locale.US), */
            HttpHeaderNames.UPGRADE.toString().toLowerCase(Locale.US),
            "Keep-Alive".toLowerCase(Locale.US)
    );
    private static Pattern HTTP_PREFIX = Pattern.compile("^https?://.*",
            Pattern.CASE_INSENSITIVE);
    /**
     * Splits comma-separated header values (such as Connection) into their individual tokens.
     */
    private static final Splitter COMMA_SEPARATED_HEADER_VALUE_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

    /**
     * 复制HttpRequest
     * @param original
     * @return
     */
    public static HttpRequest copy(HttpRequest original) {
        if (original instanceof DefaultFullHttpRequest) {
            ByteBuf content = ((DefaultFullHttpRequest)original).content();
            return new DefaultFullHttpRequest(original.protocolVersion(), original.method(), original.uri(),
                    content);
        } else {
            return new DefaultHttpRequest(original.protocolVersion(), original.method(), original.uri());
        }
    }

    /**
     * Duplicates the status line and headers of an HttpResponse object. Does not duplicate any content associated with that response.
     *
     * @param originalResponse HttpResponse to be duplicated
     * @return a new HttpResponse with the same status line and headers
     */
    public static HttpResponse copy(HttpResponse originalResponse) {
        DefaultHttpResponse newResponse = new DefaultHttpResponse(originalResponse.protocolVersion(),
                originalResponse.status());
        newResponse.headers().add(originalResponse.headers());

        return newResponse;
    }

    /**
     * 是否已经连接
     * @param httpObject
     * @return
     */
    public static boolean isConnect(HttpObject httpObject) {
        return httpObject instanceof HttpRequest
                && HttpMethod.CONNECT.equals(((HttpRequest)httpObject).method());
    }

    /**
     *
     * @param httpRequest
     * @return
     */
    public static boolean isHead(HttpRequest httpRequest) {
        return HttpMethod.HEAD.equals(httpRequest.method());
    }

    /**
     * Parses the host and port an HTTP request is being sent to.
     *
     * @param httpRequest
     *            The request.
     * @return The host and port string.
     */
    public static String parseHostAndPort(final HttpRequest httpRequest) {
        final String uriHostAndPort = parseHostAndPort(httpRequest.uri());
        return uriHostAndPort;
    }

    /**
     * Parses the host and port an HTTP request is being sent to.
     *
     * @param uri
     *            The URI.
     * @return The host and port string.
     */
    public static String parseHostAndPort(final String uri) {
        final String tempUri;
        if (!HTTP_PREFIX.matcher(uri).matches()) {
            // Browsers particularly seem to send requests in this form when
            // they use CONNECT.
            tempUri = uri;
        } else {
            // We can't just take a substring from a hard-coded index because it
            // could be either http or https.
            tempUri = StringUtils.substringAfter(uri, "://");
        }
        final String hostAndPort;
        if (tempUri.contains("/")) {
            hostAndPort = tempUri.substring(0, tempUri.indexOf("/"));
        } else {
            hostAndPort = tempUri;
        }
        return hostAndPort;
    }

    /**
     * 从request中获取host和port
     * @param httpRequest
     * @return
     */
    public static String identifyHostAndPort(HttpRequest httpRequest) {
        String hostAndPort = parseHostAndPort(httpRequest);
        if (StringUtils.isBlank(hostAndPort)) {
            List<CharSequence> hosts = httpRequest.headers().getAll(
                    HttpHeaderNames.HOST);
            if (hosts != null && !hosts.isEmpty()) {
                hostAndPort = hosts.get(0).toString();
            }
        }

        return hostAndPort;
    }

    public static InetSocketAddress identifySockAddress(HttpRequest httpRequest) {
        String host = identifyHostAndPort(httpRequest);
        int port = 80;
        try {
            URI url = new URI(host);
            host =  url.getPath();
            if (url.getPort() != -1)
                port = url.getPort();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return new InetSocketAddress(host, port);
    }

    /**
     *
     * @param httpObject
     * @return
     */
    public static boolean isLastChunk(final HttpObject httpObject) {
        return httpObject instanceof LastHttpContent;
    }

    /**
     * httpObject非isLastChunk
     * @param httpObject
     * @return
     */
    public static boolean isChunked(final HttpObject httpObject) {
        return !isLastChunk(httpObject);
    }
    /**
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

    public static DefaultFullHttpResponse responseFor(HttpVersion httpVersion,
                                        HttpResponseStatus status, ByteBuf body, int contentLength) {
        DefaultFullHttpResponse response = body != null ? new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status, body)
                : new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
        if (body != null) {
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH,
                    String.valueOf(contentLength));
            response.headers().set("Content-Type", "text/html; charset=UTF-8");
        }
        return response;
    }

    public static void retainObject(Object msg) {
        if (msg instanceof ReferenceCounted) {
            //LOG.debug("Retaining reference counted message");
            ((ReferenceCounted) msg).retain();
        }
    }

    /**
     * Determines if the specified header should be removed from the proxied response because it is a hop-by-hop header, as defined by the
     * HTTP 1.1 spec in section 13.5.1. The comparison is case-insensitive, so "Connection" will be treated the same as "connection" or "CONNECTION".
     * From http://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html#sec13.5.1 :
     * <pre>
     The following HTTP/1.1 headers are hop-by-hop headers:
     - Connection
     - Keep-Alive
     - Proxy-Authenticate
     - Proxy-Authorization
     - TE
     - Trailers [LittleProxy note: actual header name is Trailer]
     - Transfer-Encoding [LittleProxy note: this header is not normally removed when proxying, since the proxy does not re-chunk
     responses. The exception is when an HttpObjectAggregator is enabled, which aggregates chunked content and removes
     the 'Transfer-Encoding: chunked' header itself.]
     - Upgrade

     All other headers defined by HTTP/1.1 are end-to-end headers.
     * </pre>
     *
     * @param headerName the header name
     * @return true if this header is a hop-by-hop header and should be removed when proxying, otherwise false
     */
    public static boolean shouldRemoveHopByHopHeader(String headerName) {
        return SHOULD_NOT_PROXY_HOP_BY_HOP_HEADERS.contains(headerName.toLowerCase(Locale.US));
    }
    /**
     * Returns true if the HTTP response from the server is expected to indicate its own message length/end-of-message. Returns false
     * if the server is expected to indicate the end of the HTTP entity by closing the connection.
     * <p/>
     * This method is based on the allowed message length indicators in the HTTP specification, section 4.4:
     * <pre>
     4.4 Message Length
     The transfer-length of a message is the length of the message-body as it appears in the message; that is, after any transfer-codings have been applied. When a message-body is included with a message, the transfer-length of that body is determined by one of the following (in order of precedence):

     1.Any response message which "MUST NOT" include a message-body (such as the 1xx, 204, and 304 responses and any response to a HEAD request) is always terminated by the first empty line after the header fields, regardless of the entity-header fields present in the message.
     2.If a Transfer-Encoding header field (section 14.41) is present and has any value other than "identity", then the transfer-length is defined by use of the "chunked" transfer-coding (section 3.6), unless the message is terminated by closing the connection.
     3.If a Content-Length header field (section 14.13) is present, its decimal value in OCTETs represents both the entity-length and the transfer-length. The Content-Length header field MUST NOT be sent if these two lengths are different (i.e., if a Transfer-Encoding
     header field is present). If a message is received with both a Transfer-Encoding header field and a Content-Length header field, the latter MUST be ignored.
     [LP note: multipart/byteranges support has been removed from the HTTP 1.1 spec by RFC 7230, section A.2. Since it is seldom used, LittleProxy does not check for it.]
     5.By the server closing the connection. (Closing the connection cannot be used to indicate the end of a request body, since that would leave no possibility for the server to send back a response.)
     * </pre>
     *
     * The rules for Transfer-Encoding are clarified in RFC 7230, section 3.3.1 and 3.3.3 (3):
     * <pre>
     If any transfer coding other than
     chunked is applied to a response payload body, the sender MUST either
     apply chunked as the final transfer coding or terminate the message
     by closing the connection.
     * </pre>
     *
     *
     * @param response the HTTP response object
     * @return true if the message will indicate its own message length, or false if the server is expected to indicate the message length by closing the connection
     */
    public static boolean isResponseSelfTerminating(HttpResponse response) {
        if (isContentAlwaysEmpty(response)) {
            return true;
        }

        // if there is a Transfer-Encoding value, determine whether the final encoding is "chunked", which makes the message self-terminating
        List<String> allTransferEncodingHeaders = getAllCommaSeparatedHeaderValues(HttpHeaderNames.TRANSFER_ENCODING, response);
        if (!allTransferEncodingHeaders.isEmpty()) {
            String finalEncoding = allTransferEncodingHeaders.get(allTransferEncodingHeaders.size() - 1);

            // per #3 above: "If a message is received with both a Transfer-Encoding header field and a Content-Length header field, the latter MUST be ignored."
            // since the Transfer-Encoding field is present, the message is self-terminating if and only if the final Transfer-Encoding value is "chunked"
            return HttpHeaderValues.CHUNKED.equals(finalEncoding);
        }

        String contentLengthHeader = response.headers().get(HttpHeaderNames.CONTENT_LENGTH).toString();
        if (contentLengthHeader != null && !contentLengthHeader.isEmpty()) {
            return true;
        }

        // not checking for multipart/byteranges, since it is seldom used and its use as a message length indicator was removed in RFC 7230

        // none of the other message length indicators are present, so the only way the server can indicate the end
        // of this message is to close the connection
        return false;
    }

    /**
     * Returns true if the HTTP message cannot contain an entity body, according to the HTTP spec. This code is taken directly
     * from {@link io.netty.handler.codec.http.HttpObjectDecoder#isContentAlwaysEmpty(HttpMessage)}.
     *
     * @param msg HTTP message
     * @return true if the HTTP message is always empty, false if the message <i>may</i> have entity content.
     */
    public static boolean isContentAlwaysEmpty(HttpMessage msg) {
        if (msg instanceof HttpResponse) {
            HttpResponse res = (HttpResponse) msg;
            int code = res.status().code();

            // Correctly handle return codes of 1xx.
            //
            // See:
            //     - http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html Section 4.4
            //     - https://github.com/netty/netty/issues/222
            if (code >= 100 && code < 200) {
                // One exception: Hixie 76 websocket handshake response
                return !(code == 101 && !res.headers().contains(HttpHeaderNames.SEC_WEBSOCKET_ACCEPT));
            }

            switch (code) {
                case 204: case 205: case 304:
                    return true;
            }
        }
        return false;
    }

    /**
     * Retrieves all comma-separated values for headers with the specified name on the HttpMessage. Any whitespace (spaces
     * or tabs) surrounding the values will be removed. Empty values (e.g. two consecutive commas, or a value followed
     * by a comma and no other value) will be removed; they will not appear as empty elements in the returned list.
     * If the message contains repeated headers, their values will be added to the returned list in the order in which
     * the headers appear. For example, if a message has headers like:
     * <pre>
     *     Transfer-Encoding: gzip,deflate
     *     Transfer-Encoding: chunked
     * </pre>
     * This method will return a list of three values: "gzip", "deflate", "chunked".
     * <p/>
     * Placing values on multiple header lines is allowed under certain circumstances
     * in RFC 2616 section 4.2, and in RFC 7230 section 3.2.2 quoted here:
     * <pre>
     A sender MUST NOT generate multiple header fields with the same field
     name in a message unless either the entire field value for that
     header field is defined as a comma-separated list [i.e., #(values)]
     or the header field is a well-known exception (as noted below).

     A recipient MAY combine multiple header fields with the same field
     name into one "field-name: field-value" pair, without changing the
     semantics of the message, by appending each subsequent field value to
     the combined field value in order, separated by a comma.  The order
     in which header fields with the same field name are received is
     therefore significant to the interpretation of the combined field
     value; a proxy MUST NOT change the order of these field values when
     forwarding a message.
     * </pre>
     * @param headerName the name of the header for which values will be retrieved
     * @param httpMessage the HTTP message whose header values will be retrieved
     * @return a list of single header values, or an empty list if the header was not present in the message or contained no values
     */
    public static List<String> getAllCommaSeparatedHeaderValues(CharSequence headerName, HttpMessage httpMessage) {
        List<CharSequence> allHeaders = httpMessage.headers().getAll(headerName);
        if (allHeaders.isEmpty()) {
            return Collections.emptyList();
        }

        ImmutableList.Builder<String> headerValues = ImmutableList.builder();
        for (CharSequence header : allHeaders) {
            List<String> commaSeparatedValues = splitCommaSeparatedHeaderValues(header.toString());
            headerValues.addAll(commaSeparatedValues);
        }

        return headerValues.build();
    }

    /**
     * Splits comma-separated header values into tokens. For example, if the value of the Connection header is "Transfer-Encoding, close",
     * this method will return "Transfer-Encoding" and "close". This method strips trims any optional whitespace from
     * the tokens. Unlike {@link # getAllCommaSeparatedHeaderValues(String, HttpMessage)}, this method only operates on
     * a single header value, rather than all instances of the header in a message.
     *
     * @param headerValue the un-tokenized header value (must not be null)
     * @return all tokens within the header value, or an empty list if there are no values
     */
    public static List<String> splitCommaSeparatedHeaderValues(String headerValue) {
        return ImmutableList.copyOf(COMMA_SEPARATED_HEADER_VALUE_SPLITTER.split(headerValue));
    }

    /**
     * Determines if UDT is available on the classpath.
     *
     * @return true if UDT is available
     */
    public static boolean isUdtAvailable() {
        try {
            return NioUdtProvider.BYTE_PROVIDER != null;
        } catch (NoClassDefFoundError e) {
            return false;
        }
    }

    /**
     * Determines if the remote connection should be closed based on the request
     * and response pair. If the request is HTTP 1.0 with no keep-alive header,
     * for example, the connection should be closed.
     *
     * This in part determines if we should close the connection. Here's the
     * relevant section of RFC 2616:
     *
     * "HTTP/1.1 defines the "close" connection option for the sender to signal
     * that the connection will be closed after completion of the response. For
     * example,
     *
     * Connection: close
     *
     * in either the request or the response header fields indicates that the
     * connection SHOULD NOT be considered `persistent' (section 8.1) after the
     * current request/response is complete."
     *
     * @param req
     *            The request.
     * @param res
     *            The response.
     * @param msg
     *            The message.
     * @return Returns true if the connection should close.
     */
    public static boolean shouldCloseHttpConnection(HttpRequest req,
                                                HttpResponse res, HttpObject msg) {
        if (SocksHttpProxyUtils.isChunked(res)) {
            // If the response is chunked, we want to return false unless it's
            // the last chunk. If it is the last chunk, then we want to pass
            // through to the same close semantics we'd otherwise use.
            if (msg != null) {
                if (!SocksHttpProxyUtils.isLastChunk(msg)) {
                    String uri = null;
                    if (req != null) {
                        uri = req.uri();
                    }
                    //LOG.debug("Not closing server connection on middle chunk for {}", uri);
                    return false;
                } else {
                    //LOG.debug("Handling last chunk. Using normal server connection closing rules.");
                }
            }
        }

        // ignore the request's keep-alive; we can keep this server connection open as long as the server allows it.

        if (!HttpHeaderUtil.isKeepAlive(res)) {
            //LOG.debug("Closing server connection since response is not keep alive: {}", res);
            // In this case, we want to honor the Connection: close header
            // from the remote server and close that connection. We don't
            // necessarily want to close the connection to the client, however
            // as it's possible it has other connections open.
            return true;
        }

        //LOG.debug("Not closing server connection for response: {}", res);
        return false;
    }
}
