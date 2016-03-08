package org.siltools.sockshttp.cli.impl;

import org.siltools.sockshttp.proxy.log.LogFactory;
import py4j.GatewayServer;

/**
 * Created by Administrator on 3/8/2016.
 */
public enum StartXUtils {
    INSTANCE;

    GatewayServer gatewayServer;

    public static StartXUtils getInstance() {
        return INSTANCE;
    }

    private StartXUtils() {

    }

    public synchronized void startX(Integer port) {
        if (gatewayServer != null)
            return;

        gatewayServer = new GatewayServer(LogFactory.getInstance());
        gatewayServer.start();
    }

    public synchronized void stop() {
        if (gatewayServer == null)
            return;

        gatewayServer.shutdown();
    }
}
