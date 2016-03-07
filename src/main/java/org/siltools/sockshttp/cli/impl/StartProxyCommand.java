package org.siltools.sockshttp.cli.impl;

import org.siltools.sockshttp.cli.AbstractCliCommand;

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
        return false;
    }
}
