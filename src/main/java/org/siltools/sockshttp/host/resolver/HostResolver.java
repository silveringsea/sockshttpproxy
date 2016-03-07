package org.siltools.sockshttp.host.resolver;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * Created by Administrator on 1/13/2016.
 */
public interface HostResolver {
    public InetSocketAddress resolve(String host, int port)
            throws UnknownHostException;
}
