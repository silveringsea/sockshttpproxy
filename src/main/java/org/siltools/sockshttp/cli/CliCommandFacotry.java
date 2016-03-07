package org.siltools.sockshttp.cli;

import java.util.HashMap;
import java.util.Map;

/**
 */
public class CliCommandFacotry {

    private static volatile CliCommandFacotry instance;

    private CliCommandFacotry() {
        initAllCommand();
    }

    public static CliCommandFacotry getFactory() {
        if (instance == null) {
            synchronized (CliCommandFacotry.class) {
                if (instance == null) {
                    instance = new CliCommandFacotry();
                }
            }
        }
        return instance;
    }

    protected  Map<String,AbstractCliCommand> commandMapCli =
            new HashMap<String, AbstractCliCommand>( );

    protected void initAllCommand() {
        initAllCommand(commandMapCli);
    }

    protected static void initAllCommand(Map<String, AbstractCliCommand> commandMapCli) {

    }

    public AbstractCliCommand getCommand(String cmdStr) {
        return commandMapCli.get(cmdStr);
    }
}
