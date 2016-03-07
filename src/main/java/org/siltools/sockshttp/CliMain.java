package org.siltools.sockshttp;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

/**
 */
public class CliMain {

    String commandStr;
    List<String> cmdOptions;
    protected HashMap<Integer,String> history = new HashMap<Integer,String>( );

    public static void main(String[] args) throws InterruptedException {
//        try {
//
//            ConsoleReader consoleReader = new ConsoleReader();
////                consoleReader.addCompleter()
//            consoleReader.addTriggeredAction((char)72, new ActionListener() {
//                public void actionPerformed(ActionEvent e) {
//                    System.out.println("key up press");
//                }
//            });
//            String line;
//            while (true) {
////                    hisFlag = scanner.nextInt();
////                    System.out.println(hisFlag);
////                    reader.mark(0);
////                    hisFlag = reader.read();
////                    reader.reset();
////                    hisFlag = consoleReader.readCharacter();
//                if ((line = consoleReader.readLine("asdfsa")) != null) {
//                    System.out.println("line is" + line);
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        CliMain cliMain = new CliMain();
        cliMain.run();
    }

    public void run() throws InterruptedException {
        Thread cmdThread = new CommandReceiveCommand();
        cmdThread.start();
        cmdThread.join();
    }

    public boolean parseCommand2( String cmdstring ) {
        StringTokenizer cmdTokens = new StringTokenizer(cmdstring, " ");
        String[] args = new String[cmdTokens.countTokens()];
        int tokenIndex = 0;
        while (cmdTokens.hasMoreTokens()) {
            args[tokenIndex] = cmdTokens.nextToken();
            tokenIndex++;
        }
        if (args.length == 0){
            return false;
        }
        commandStr = args[0];
        cmdOptions = Arrays.asList(args);
        return true;
    }

    public boolean parseCommand(String cmdString) {
        String[] splitStrs = StringUtils.split(cmdString, " ");
        if (splitStrs == null || splitStrs.length == 0)
            return false;

        commandStr = splitStrs[0];

        cmdOptions = Arrays.asList(ArrayUtils.subarray(splitStrs, 1, splitStrs.length));
        return true;
    }

    protected void addToHistory(int i,String cmd) {
        history.put(i, cmd);
    }

    public void executeCommand(String line) {
        System.out.append(line);
//        Command command =
//        System.out.flush();
    }

    class CommandReceiveCommand extends Thread {
        @Override
        public void run() {
            InputStreamReader in = new InputStreamReader(System.in);
            BufferedReader reader = new BufferedReader(in);
            String line;
            try {

//                ConsoleReader consoleReader = new ConsoleReader();
////                consoleReader.addCompleter()
//                consoleReader.addTriggeredAction((char)72, new ActionListener() {
//                    public void actionPerformed(ActionEvent e) {
//                        System.out.println("key up press");
//                    }
//                });
                while (true) {
//                    hisFlag = scanner.nextInt();
//                    System.out.println(hisFlag);
//                    reader.mark(0);
//                    hisFlag = reader.read();
//                    reader.reset();
//                    hisFlag = consoleReader.readCharacter();
                    if ((line = reader.readLine()) != null) {
                        executeCommand(line);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
