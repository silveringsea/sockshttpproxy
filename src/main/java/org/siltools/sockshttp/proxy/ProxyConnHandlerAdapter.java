package org.siltools.sockshttp.proxy;

import io.netty.handler.codec.http.HttpRequest;

import java.net.InetSocketAddress;

/**
 * Created by Administrator on 3/5/2016.
 */
public class ProxyConnHandlerAdapter extends ConnectionHandlerAdapter {
    public int connectionType() {
        return IConnectionHandlerContext.CONNECTION_TYPE_PROXY;
    }

    public void connectAndWrite(final HttpRequest initialHttpRequest) {

    }

    public InetSocketAddress setupConnectionAddress() {
        return null;
    }
}
