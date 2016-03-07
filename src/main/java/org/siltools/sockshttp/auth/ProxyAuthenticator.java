package org.siltools.sockshttp.auth;

/**
 */
public interface ProxyAuthenticator {
    /**
     * 用 userName and password 进行认证
     * @param userName user name.
     * @param password  password.
     * @return <code>true</code> if the credentials are acceptable, otherwise <code>false</code>.
     */
    boolean authenticate(String userName, String password);
}

