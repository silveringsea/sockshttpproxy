package org.siltools.sockshttp.cli.impl;

import org.siltools.sockshttp.FlowSocksHttpProxyServer;
import org.siltools.sockshttp.cli.AbstractCliCommand;
import org.siltools.sockshttp.proxy.ConnectionInitializer;
import org.siltools.sockshttp.proxy.FlowClientToProxyConnectionHandler;
import org.siltools.sockshttp.proxy.FlowProxyToServerConnectionHandler;
import org.siltools.sockshttp.proxy.IConnectionPipeline;

import java.io.IOException;
import java.text.ParseException;

/**
 * start/stop proxy command
 * usage: start -p 8787
 */
public class StartProxyCommand extends AbstractCliCommand {

    public StartProxyCommand(String cmdStr, String optionStr) {
        super(cmdStr, optionStr);
    }

    @Override
    public AbstractCliCommand parse(String[] cmdArgs) throws ParseException {
        return this;
    }

    @Override
    public boolean exec() throws Exception, IOException, InterruptedException {
        if (socksHttpProxyServer != null) {
            System.out.println("socksHttpProxyServer already exists");
            return false;
        }
        FlowSocksHttpProxyServer.FlowSocksHttpProxyServerBootstrap bootstrap = new FlowSocksHttpProxyServer.FlowSocksHttpProxyServerBootstrap();
        bootstrap.withConnectTimeout(100000000);
        bootstrap.withName("myProxy")
                .withHost("127.0.0.1")
                .withPort(8787);
        bootstrap.withConnInitializer(new ConnectionInitializer() {
            @Override
            public IConnectionPipeline initPipline(IConnectionPipeline cp) throws Exception {
                cp.addLast(new FlowClientToProxyConnectionHandler());
                cp.addLast(new FlowProxyToServerConnectionHandler());
                return cp;
            }
        });

        socksHttpProxyServer = bootstrap.start();
        return false;
    }
}
