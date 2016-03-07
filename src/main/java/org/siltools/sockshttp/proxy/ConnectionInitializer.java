package org.siltools.sockshttp.proxy;

/**
 * Created by Administrator on 3/5/2016.
 */
public abstract class ConnectionInitializer<P extends IConnectionPipeline> {

    public static final String MAIN_INITIALIZER = "MAIN_INITIALIZER";


    public abstract P initPipline(P ch) throws Exception;
}
