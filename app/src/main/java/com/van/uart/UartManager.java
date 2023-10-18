package com.van.uart;

public class UartManager {

    static {
        System.loadLibrary("VanUart");
    }

    public static enum BaudRate {
        B1200, B2400, B4800, B9600, B19200, B38400, B57600, B115200, B230400;
    }

    private int id;
    private String name;
    private BaudRate baudRate;

    public UartManager() {
        id = -1;
        name = "";
        baudRate = BaudRate.B115200;
    }

    public String getName() {
        return name;
    }

    public BaudRate getBaudRate() {
        return baudRate;
    }

    public void open(String name, BaudRate baudRate) throws LastError {
        id = open(name, baudRate.ordinal());
        this.name = name;
        this.baudRate = baudRate;
    }

    public void close() {
        if (-1 != id) close(id);
    }

    public boolean isOpen() {
        if (-1 != id) return isOpen(id);
        return false;
    }

    public int write(final byte[] data, int size) throws LastError {
        if (-1 != id) return write(id, data, size);
        return -1;
    }

    public int read(byte[] buf, int size, int wait, int interval) throws LastError {
        if (-1 != id) return read(id, buf, size, wait, interval);
        return -1;
    }

    public void stopRead() {
        if (-1 != id) stopRead(id);
    }

    public static native String[] devices();

    private native int open(String name, int baudRate) throws LastError;

    private native void close(int id);

    private native boolean isOpen(int id);

    private native int write(int id, final byte[] data, int size) throws LastError;

    private native int read(int id, byte[] buf, int size, int wait, int interval) throws LastError;

    private native void stopRead(int id);
}
