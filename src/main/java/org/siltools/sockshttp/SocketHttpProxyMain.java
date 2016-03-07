package org.siltools.sockshttp;

import org.siltools.sockshttp.proxy.ConnectionInitializer;
import org.siltools.sockshttp.proxy.FlowClientToProxyConnectionHandler;
import org.siltools.sockshttp.proxy.FlowProxyToServerConnectionHandler;
import org.siltools.sockshttp.proxy.IConnectionPipeline;

/**
 * Created by Administrator on 1/11/2016.
 */
public class SocketHttpProxyMain {

    public static void main(String[] args) {
        FlowSocksHttpProxyServer.FlowSocksHttpProxyServerBootstrap bootstrap = new FlowSocksHttpProxyServer.FlowSocksHttpProxyServerBootstrap();
        bootstrap.withConnectTimeout(100000000);
        bootstrap.withName("myProxy")
                 .withHost("127.0.0.1")
                 .withPort(8787);
        bootstrap.withConnInitializer(new ConnectionInitializer() {
              @Override
              public IConnectionPipeline initPipline(IConnectionPipeline cp) throws Exception {
                  cp.addFirst(new FlowClientToProxyConnectionHandler());
                  cp.addFirst(new FlowProxyToServerConnectionHandler());
                  return cp;
              }
        });

        bootstrap.start();
    }
}
