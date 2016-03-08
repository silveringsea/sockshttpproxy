package org.siltools.sockshttp.proxy;

import java.net.InetSocketAddress;

/**
 * Created by Administrator on 3/5/2016.
 */
public class ProxyConnHandlerAdapter extends ConnectionHandlerAdapter {
    public int connectionType() {
        return IConnectionHandlerContext.CONNECTION_TYPE_PROXY;
    }

    public InetSocketAddress setupConnectionAddress() {
        return null;
    }
}
