package org.siltools.sockshttp.proxy;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;

/**
 * Created by Administrator on 2/29/2016.
 */
public interface IConnectionHandlerContext {

    public static final int MASK_IN_EXCEPTION_CAUGHT = 1 << 2;
    public static final int MASK_CONN_MESSAGERECEIVE = 1 << 3;
    public static final int MASK_CONN_INIT_CHANNEL_PIPELINE = 1 << 4;
    public static final int MASK_CONN_PROXYSTATE_CHG = 1 << 5;
    public static final int MASK_CONN_CLIENT_CONNECT_TIMEOUT = 1 << 6;
    public static final int MASK_CONN_SERVER_CONNSUC = 1 << 7;
    public static final int MASK_CONN_SERVER_CONNFAIL = 1 << 8;
    public static final int MASK_CONN_REMOTE_INTNETADDRESS = 1 << 9;

    public static final int MASK_CONN_READ_HTTPCHUNK = 1 << 11;
    public static final int MASK_CONN_READ_RAW = 1 << 12;
//    public static final int MASK_CONN_PROXY_AUTHENTICATION = 1 << 13;
    public static final int MASK_OUT_EXCEPTION_CAUGHT = 1 << 14;

    public static final int CONNECTION_TYPE_CLIENT = 1;
    public static final int CONNECTION_TYPE_PROXY = 2;
    /**
     * Return the {@link IProxyConnection} which is bound to the {@link IConnectionHandlerContext}.
     */
    IProxyConnection connection();

    IProxyConnection connectionSibling();

    void connection(IProxyConnection connection);

    IProxyToServerConnection proxyToServerConnection();

    IClientToProxyConnection clientToProxyConnection();

//    boolean disconnectAll();
    /**
     * Return the {@link IConnectionHandler} which is bound to the {@link IConnectionHandlerContext}.
     */
    IConnectionHandler handler();

    /**
     * Return the assigned {@link IConnectionPipeline}
     */
    IConnectionPipeline pipeline();

    /**
     * Return {@code true} if the {@link IConnectionHandler} which belongs to this {@link IConnectionHandler} was removed
     * from the {@link IConnectionPipeline}.
     */
    boolean isRemoved();

    IConnectionHandlerContext fireInitChannelPipeline(ChannelPipeline channelPipeline, int connectionType);
    /**
     * A {@link IConnectionHandlerContext}
     *
     * This will result in having the {@link IConnectionHandler#serverConnectedSucc(IConnectionHandlerContext, Object...)}
     * method called of the next {@link IConnectionHandler} contained in the {@link IConnectionPipeline} of the
     * {@link IProxyConnection}.
     */
    IConnectionHandlerContext fireMessageReceive(Object ...objects);

    /**
     * A {@link IConnectionHandlerContext}
     *
     * This will result in having the {@link IConnectionHandler#serverConnectedSucc(IConnectionHandlerContext, Object...)}
     * method called of the next {@link IConnectionHandler} contained in the {@link IConnectionPipeline} of the
     * {@link IProxyConnection}.
     */
    IConnectionHandlerContext fireProxyStateChange(Object ...objects);
    /**
     * A {@link IConnectionHandlerContext}
     *
     * This will result in having the {@link IConnectionHandler#readHTTPInitial(IConnectionHandlerContext, Object...)} method
     * called of the next {@link IConnectionHandler} contained in the {@link IConnectionPipeline} of the
     * {@link IProxyConnection}.
     */
//    IConnectionHandlerContext fireReadHTTPInitial(Object ...objects);

    /**
     * A {@link IConnectionHandlerContext}
     *
     * This will result in having the {@link IConnectionHandler#readHTTPChunk(IConnectionHandlerContext, Object...)} method
     * called of the next {@link IConnectionHandler} contained in the {@link IConnectionPipeline} of the
     * {@link IProxyConnection}.
     */
    IConnectionHandlerContext fireReadHTTPChunk(Object ...objects);

    /**
     * A {@link IConnectionHandlerContext}
     *
     * This will result in having the {@link IConnectionHandler#readRaw(IConnectionHandlerContext, ByteBuf)} method
     * called of the next {@link IConnectionHandler} contained in the {@link IConnectionPipeline} of the
     * {@link IProxyConnection}.
     */
    IConnectionHandlerContext fireReadRaw(Object object);

    /**
     * A {@link IConnectionHandlerContext}
     *
     * This will result in having the {@link IConnectionHandler#negotiatingConnect(IConnectionHandlerContext, Object)} method
     * called of the next {@link IConnectionHandler} contained in the {@link IConnectionPipeline} of the
     * {@link IProxyConnection}.
     */
//    IConnectionHandlerContext fireNegotiatingConnect(Object object);

    /**
     * A {@link IConnectionHandlerContext}
     *
     * This will result in having the {@link IConnectionHandler#proxyAuthentication(IConnectionHandlerContext, Object)} method
     * called of the next {@link IConnectionHandler} contained in the {@link IConnectionPipeline} of the
     * {@link IProxyConnection}.
     */
//    IConnectionHandlerContext fireProxyAuthentication(Object object);

    /**
//     * A {@link IConnectionHandlerContext}
//     *
//     * This will result in having the {@link IConnectionHandler#connecting(IConnectionHandlerContext, Object)} method
//     * called of the next {@link IConnectionHandler} contained in the {@link IConnectionPipeline} of the
//     * {@link IProxyConnection}.
//     */
//    IConnectionHandlerContext fireConnecting(Object object);
//
//    /**
//     * A {@link IConnectionHandlerContext}
//     *
//     * This will result in having the {@link IConnectionHandler#disconnected(IConnectionHandlerContext, Object)} method
//     * called of the next {@link IConnectionHandler} contained in the {@link IConnectionPipeline} of the
//     * {@link IProxyConnection}.
//     */
//    IConnectionHandlerContext fireDisconnected(Object object);

    /**
     * A {@link IConnectionHandlerContext}
     *
     * This will result in having the {@link IConnectionHandler#serverConnectedSucc(IConnectionHandlerContext, Object...)}
     * method called of the next {@link IConnectionHandler} contained in the {@link IConnectionPipeline} of the
     * {@link IProxyConnection}.
     */
    IConnectionHandlerContext fireServerConnectedSucc(Object ...objects);


    /**
     * A {@link IConnectionHandlerContext}
     *
     * This will result in having the {@link IConnectionHandler#serverConnectedFail(IConnectionHandlerContext, Object...)}
     * method called of the next {@link IConnectionHandler} contained in the {@link IConnectionPipeline} of the
     * {@link IProxyConnection}.
     */
    IConnectionHandlerContext fireServerConnectedFail(Object ...object);
    /**
     * A {@link IProxyConnection} received an {@link Throwable} in one of its inbound operations.
     *
     * This will result in having the {@link IConnectionHandler#inboundExceptionCaught(IConnectionHandlerContext, Throwable)}}
     * method called of the next {@link IConnectionHandler} contained in the {@link IConnectionPipeline} of the
     * {@link IProxyConnection}.
     */
    IConnectionHandlerContext fireInboundExceptionCaught(Throwable cause, Object object) throws Exception;

    IConnectionHandlerContext fireOutboundExceptionCaught(Throwable cause, Object object) throws Exception;

    IConnectionHandlerContext fireClientConnectTimeout(Object objects);

    Future<InetSocketAddress> fireRemoteInetSocketAddress(Object ...objects);
}
