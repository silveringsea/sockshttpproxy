package org.siltools.sockshttp.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.udt.nio.NioUdtProvider;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.commons.lang3.StringUtils;
import org.siltools.sockshttp.base.ResultFuture;
import org.siltools.sockshttp.def.ConnectionState;
import org.siltools.sockshttp.def.TransportProtocol;
import org.siltools.sockshttp.def.exception.UnknownTransportProtocolException;
import org.siltools.sockshttp.util.SocksHttpProxyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * Created by Administrator on 3/3/2016.
 */
public class FlowProxyToServerConnectionHandler extends ProxyConnHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(FlowProxyToServerConnectionHandler.class);

    @Override
    public void initChannelPipeline(IConnectionHandlerContext ctx, ChannelPipeline channelPipeline) {
//            if (!isTunneling)
//                channelPipeline.addLast("decoder", new HttpResponseDecoder(8192,
//                        8192 * 2,
//                        8192 * 2));
        channelPipeline.addLast("encoder", new HttpRequestEncoder());
        // Set idle timeout
        channelPipeline.addLast(
                "idle",
                new IdleStateHandler(0, 0, ctx.pipeline().proxyServer()
                        .getIdleConnectionTimeout()));
        channelPipeline.addLast("proxy_handler", (ChannelHandler)ctx.proxyToServerConnection());
    }

    @Override
    public void messageReceive(IConnectionHandlerContext ctx, Object... objects) {
        if (objects == null || objects.length < 2) {
            return;
        }
        if (objects[1] instanceof HttpRequest) {
            HttpRequest httpRequest = (HttpRequest)objects[1];
            if (ctx.proxyToServerConnection().getCurrentState() == ConnectionState.DISCONNECTED) {
                SocksHttpProxyUtils.retainObject(httpRequest);
                connectAndWrite(ctx, httpRequest);
            } else {
                ctx.proxyToServerConnection().resentHttpRequest(httpRequest);
            }
        } else if (objects[1] instanceof HttpObject) {
            ctx.pipeline().fireReadHTTPChunk(objects[1]);
        } else if (objects[1] instanceof ByteBuf) {
            ctx.pipeline().fireReadRaw((ByteBuf)objects[1]);
        }
    }

    @Override
    public void proxyStateChange(IConnectionHandlerContext ctx, Object... objects) {
        super.proxyStateChange(ctx, objects);
    }

    @Override
    public Future<InetSocketAddress> remoteInetSocketAddress(IConnectionHandlerContext ctx, Object ...object) {
        String[] serverHostPortStrs = StringUtils.split(ctx.proxyToServerConnection().getServerHostAndPort(), ":");
        String host = serverHostPortStrs[0];
        int port = (serverHostPortStrs.length > 1 && serverHostPortStrs[1] != null) ? Integer.parseInt(serverHostPortStrs[1]): 80;
        InetSocketAddress remoteAddress = new InetSocketAddress(host, port);
        Future<InetSocketAddress> future = new ResultFuture<InetSocketAddress>(remoteAddress, true);
        return future;
    }

    /** */
    public void connectAndWrite(final IConnectionHandlerContext ctx, final HttpRequest initialHttpRequest) {
        initialHttpRequest.headers().set(HttpHeaderNames.USER_AGENT, "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:41.0) Gecko/20100101 Firefox/41.0");
        TransportProtocol transportProtocol = TransportProtocol.TCP;
        final Bootstrap cb = new Bootstrap().group(ctx.pipeline().proxyServer().getProxyToServerThreadPool(transportProtocol));
        switch (transportProtocol) {
            case TCP:
                logger.debug("Connecting to server with TCP");
                cb.channel(NioSocketChannel.class);
                break;
            case UDT:
                logger.debug("Connecting to server with UDT");
                cb.channelFactory(NioUdtProvider.BYTE_CONNECTOR)
                        .option(ChannelOption.SO_REUSEADDR, true);
                break;
            default:
                throw new UnknownTransportProtocolException(transportProtocol);
        }

        cb.handler(new LoggingHandler(LogLevel.INFO));
        cb.handler(new ChannelInitializer<Channel>() {
            protected void initChannel(Channel ch) throws Exception {
                ctx.pipeline().fireInitChannelPipeline(ch.pipeline(), IConnectionHandlerContext.CONNECTION_TYPE_PROXY);
            }
        });
        cb.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, ctx.pipeline().proxyServer().getConnectTimeout());
        Future<InetSocketAddress> f = ctx.pipeline().fireRemoteInetSocketAddress(null);

        ctx.proxyToServerConnection().becomeState(ConnectionState.CONNECTING);//        ctx.fireProxyStateChange(ctx, null);
        f.addListener(new GenericFutureListener<Future<InetSocketAddress>>() {
            public void operationComplete(Future future) throws Exception {
                if (future.isSuccess()) {
                    InetSocketAddress remoteAddress = (InetSocketAddress) future.get();
                    ChannelFuture cf = cb.connect(remoteAddress);
                    cf.addListener(new ChannelFutureListener() {
                        public void operationComplete(ChannelFuture cbFuture) throws Exception {
                            if (cbFuture.isSuccess()) {
                                ctx.pipeline().fireServerConnectedSucc(cbFuture.cause());
                            } else {
                                ctx.pipeline().fireServerConnectedFail(cbFuture.cause());
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public void readHTTPChunk(IConnectionHandlerContext ctx, Object ...chunk) {
        if (chunk == null) {
//            ctx.disconnectAll();
            return;
        }
        if (chunk[0] instanceof HttpResponse) {
            ctx.proxyToServerConnection().becomeState(ConnectionState.AWAITING_CHUNK);
        } else if (chunk[0] instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) chunk[0];
            if (httpContent instanceof LastHttpContent) {
                logger.debug("last httpContent receive");
            }
        }
        ctx.fireReadHTTPChunk(chunk);
    }

    @Override
    public void readRaw(IConnectionHandlerContext ctx, ByteBuf buf) {
        super.readRaw(ctx, buf);
    }

    @Override
    public void serverConnectedSucc(IConnectionHandlerContext ctx, Object... objects) {
        ctx.proxyToServerConnection().connectionSucceeded(true);
        super.serverConnectedSucc(ctx, objects);
    }

    @Override
    public void serverConnectedFail(IConnectionHandlerContext ctx, Object... objects) {
        super.serverConnectedFail(ctx, objects);
    }

    public boolean shouldExecuteOnEventLoop(int stepFlag) {
        return false;
    }
}
