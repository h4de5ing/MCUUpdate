package com.code19.mcuupdate;

public interface OnChangeListener {
    void post(String message);
    void progress(int progress);
}
