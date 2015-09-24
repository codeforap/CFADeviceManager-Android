package com.cfap.cfadevicemanager.models;

/**
 * Created by Shreya Jagarlamudi on 21/09/15.
 */

public class AndroidAgentException extends Exception {

    private static final long serialVersionUID = 1L;

    public AndroidAgentException(String msg, Exception nestedEx) {
        super(msg, nestedEx);
    }

    public AndroidAgentException(String message, Throwable cause) {
        super(message, cause);
    }

    public AndroidAgentException(String msg) {
        super(msg);
    }

    public AndroidAgentException() {
        super();
    }

    public AndroidAgentException(Throwable cause) {
        super(cause);
    }

}