package com.van.uart;

import android.util.Log;

import com.code19.mcuupdate.DataUtils;

public class UartManager {

    static {
        System.loadLibrary("VanUart");
    }

    public static enum BaudRate {
        B1200, B2400, B4800, B9600, B19200, B38400, B57600, B115200, B230400;
    }

    private int id;

    public UartManager() {
        id = -1;
    }

    public void open(String name, BaudRate baudRate) throws LastError {
        id = open(name, baudRate.ordinal());
        Log.i("gh0t", "打开串口:" + name + " " + baudRate);
    }

    public void close() {
        if (-1 != id) close(id);
    }

    public boolean isOpen() {
        if (-1 != id) return isOpen(id);
        return false;
    }

    public int write(final byte[] data, int size) throws LastError {
        if (-1 != id) {
            int result = write(id, data, size);
            Log.i("gh0st", "串口发送数据 ：" + DataUtils.bytes2HexString(data) + " 结果:" + result);
            return result;
        }
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
