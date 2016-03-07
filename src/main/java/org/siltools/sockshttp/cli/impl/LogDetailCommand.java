package org.siltools.sockshttp.cli.impl;

import org.siltools.sockshttp.cli.AbstractCliCommand;

import java.text.ParseException;

/**
 * Created by Administrator on 2/27/2016.
 */
public class LogDetailCommand extends AbstractCliCommand {

    public LogDetailCommand(String cmdStr, String optionStr) {
        super(cmdStr, optionStr);
    }

    @Override
    public AbstractCliCommand parse(String[] cmdArgs) throws ParseException {
        return this;
    }

    @Override
    public boolean exec() throws Exception {
        return false;
    }
}
