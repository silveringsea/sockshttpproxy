package org.siltools.sockshttp;

/**
 */
public interface SocksHttpProxyServerBootstrap {

    SocksHttpProxyServerBootstrap withHost(String name);

    SocksHttpProxyServerBootstrap withName(String name);

    SocksHttpProxyServerBootstrap withPort(int port);

    SocksHttpProxyServerBootstrap withTransparent(
            boolean transparent);

    SocksHttpProxyServerBootstrap withIdleConnectionTimeout(
            int idleConnectionTimeout);

    SocksHttpProxyServerBootstrap withConnectTimeout(
            int connectTimeout);

    SocksHttpProxyServer start();

}
