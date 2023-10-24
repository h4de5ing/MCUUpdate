package com.van.uart;


public class LastError extends Exception {

    private static final long serialVersionUID = -5059096406499912488L;
    private final int errno;

    public LastError(int errno, String msg) {
        super(msg);
        this.errno = errno;
    }


    @Override
    public String toString() {
        return "errno = " + errno + ", " + getMessage();
    }
}
