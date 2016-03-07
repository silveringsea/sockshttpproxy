package org.siltools.sockshttp;

import org.siltools.sockshttp.def.exception.SocksHttpProxyException;

import java.io.IOException;
import java.util.List;

/**
 */
public class SocksHttpProxy {

    /**
     * show http logging
     * @param isTunneling
     */
    public void enableTunneling(boolean isTunneling) throws IOException, SocksHttpProxyException {

    }

    /**
     *
     * @param from
     * @param to
     * @return
     */
    public List getProxyLog(int from, int to) {
        return null;
    }

    /**
     * filter http
     * @param regexStr
     * @throws IOException
     * @throws SocksHttpProxyException
     */
    public void setFilter(String regexStr) throws IOException, SocksHttpProxyException {

    }


}
