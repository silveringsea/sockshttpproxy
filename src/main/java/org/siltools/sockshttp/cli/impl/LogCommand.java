package org.siltools.sockshttp.cli.impl;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.siltools.sockshttp.cli.AbstractCliCommand;

import java.io.IOException;
import java.text.ParseException;

/**
 */
public class LogCommand extends AbstractCliCommand {

    private Options options = new Options();
    private String[] args;
    private CommandLine cl;

    public LogCommand(String cmdStr, String optionStr) {
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
