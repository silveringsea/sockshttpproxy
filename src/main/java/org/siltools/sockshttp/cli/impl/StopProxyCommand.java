package org.siltools.sockshttp.cli.impl;

import org.siltools.sockshttp.cli.AbstractCliCommand;

import java.text.ParseException;

/**
 */
public class StopProxyCommand extends AbstractCliCommand {
    public StopProxyCommand(String cmdStr, String optionStr) {
        super(cmdStr, optionStr);
    }

    @Override
    public AbstractCliCommand parse(String[] cmdArgs) throws ParseException {
        return null;
    }

    @Override
    public boolean exec() throws Exception {
        if (socksHttpProxyServer == null) {
            System.out.println("socksHttpProxyServer donot exists");
            return false;
        }
        socksHttpProxyServer.shutdown();
        return true;
    }
}
