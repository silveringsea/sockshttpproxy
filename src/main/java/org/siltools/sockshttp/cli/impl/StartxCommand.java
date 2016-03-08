package org.siltools.sockshttp.cli.impl;

import org.siltools.sockshttp.cli.AbstractCliCommand;

import java.text.ParseException;

/**
 */
public class StartxCommand extends AbstractCliCommand {

    public StartxCommand(String cmdStr, String optionStr) {
        super(cmdStr, optionStr);
    }

    @Override
    public AbstractCliCommand parse(String[] cmdArgs) throws ParseException {
        return this;
    }

    @Override
    public boolean exec() throws Exception {
//        StartXUtils.getInstance().startX(null);
        final Process p = Runtime.getRuntime().exec("python socks-gui.py");
        ProcessBuilder builder = new ProcessBuilder("python", "socks-gui.py");
        builder.start();
//        System.out.println(v);
        return true;
    }

    public static void main(String[] args) throws Exception {
        (new StartxCommand("startx", null)).exec();
    }
}
