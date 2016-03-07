package org.siltools.sockshttp.cli;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2/27/2016.
 */
public enum UsageCommandFactory {
    INSTANCE;

    Map<String, String> mapCommand = new HashMap<String, String>();

    public static UsageCommandFactory getInstance() {
        return INSTANCE;
    }

    private UsageCommandFactory() {

    }

    public String getCommand(String cmdStr) {
        return mapCommand.get(cmdStr);
    }
}
