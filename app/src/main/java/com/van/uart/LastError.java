package com.van.uart;

import android.support.annotation.Keep;

@Keep
public class LastError extends Exception {
	
	private static final long serialVersionUID = -5059096406499912488L;
	private int errno;
	
	public LastError(int errno, String msg) {
		super(msg);
		
		this.errno = errno;
	}
	
	public int getNumber() {
		return errno;
	}

	@Override
	public String toString() {
		return "errno = " + errno + ", " + getMessage();
	}
}
