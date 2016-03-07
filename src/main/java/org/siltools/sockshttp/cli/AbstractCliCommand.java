package org.siltools.sockshttp.cli;

import org.siltools.sockshttp.SocksHttpProxyServer;

import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.util.Map;

/**
 */
public abstract class AbstractCliCommand implements Command {

    protected SocksHttpProxyServer socksHttpProxyServer;
    private String cmdStr;
    private String optionStr;
    protected PrintStream outStream;
    protected PrintStream errStream;

    public AbstractCliCommand(String cmdStr, String optionStr) {
        this.outStream = System.out;
        this.errStream = System.err;

        this.cmdStr = cmdStr;
        this.optionStr = optionStr;
    }

    /**
     * parse Command
     * @param cmdArgs
     * @return
     * @throws ParseException
     */
    abstract public AbstractCliCommand parse(String[] cmdArgs) throws ParseException;

    /**
     * run exec command
     * @return true or false
     * @throws Exception
     * @throws IOException
     * @throws InterruptedException
     */
    abstract public boolean exec() throws Exception ;

    public void addToMap(Map<String, AbstractCliCommand> mapCommandCli) {
        mapCommandCli.put(cmdStr, this);
    }
}
