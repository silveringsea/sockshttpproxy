package org.siltools.sockshttp.proxy;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * Created by Administrator on 3/3/2016.
 */
public class DefaultConnHandlerContext extends AbstractConnHandlerContext {
    private final IConnectionHandler handler;
    private final int connectionType;

    protected DefaultConnHandlerContext(DefaultConnectionPipeline pipeline,
                                        String name, IConnectionHandler handler) {
        super(pipeline, name, skipFlags(Objects.requireNonNull(handler, "connection handler couldnot be null")));
        this.handler = handler;
        this.connectionType = handler.connectionType();
    }


    public boolean disconnectAll() {
        return false;
    }


    public IConnectionHandler handler() {
        return handler;
    }


    public IProxyConnection connection() {
        return ((connectionType & CONNECTION_TYPE_CLIENT) == CONNECTION_TYPE_CLIENT)?
                pipeline().clientConnection(): pipeline().proxyConnection();
    }

    public IProxyConnection connectionSibling() {
        return ((connectionType & CONNECTION_TYPE_CLIENT) == CONNECTION_TYPE_CLIENT)?
                pipeline().proxyConnection(): pipeline().clientConnection();
    }

    public void connection(IProxyConnection connection) {
        if ((connectionType & CONNECTION_TYPE_CLIENT) == CONNECTION_TYPE_CLIENT) {
            pipeline().clientConnection(connection);
        } else {
            pipeline().proxyConnection(connection);
        }
    }

    public IConnectionHandlerContext fireServerConnectedSucc(Object object) {
        return null;
    }

//    public ConnectionState state() {
//        return proxyConnection.getCurrentState();
//    }
//
//    public void state(ConnectionState connectionState) {
//        proxyConnection.becomeState(connectionState);
//    }
}
