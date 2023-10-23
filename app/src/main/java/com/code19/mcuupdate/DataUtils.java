package com.code19.mcuupdate;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class DataUtils {
    public static byte[] int2bytes2(String s) {
        byte[] data;
        try {
            s = s.replace(" ", "");
            if (s.length() % 2 != 0) {
                s = s.substring(0, s.length() - 1) + "0" + s.substring(s.length() - 1, s.length());
            }
            data = new byte[s.length() / 2];
            for (int j = 0; j < data.length; j++) {
                data[j] = (byte) (Integer.valueOf(s.substring(j * 2, j * 2 + 2), 16) & 0xff);
            }
        } catch (Exception e) {
            e.printStackTrace();//NumberFormatException
            data = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04};
        }
        return data;
    }
    public static String bytes2HexString(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        final char[] HEX = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        for (byte datum : data) {
            int value = datum & 0xff;
            sb.append("0x").append(HEX[value / 16]).append(HEX[value % 16]).append(" ");
        }
        return sb.toString();
    }
    public static byte[] addPreifxAndTail(byte[] data) {
        byte[] preifx = new byte[]{0x7F, 0x01, 0x30, 0x30, 0x30, 0x30};//头
        byte[] suffix = new byte[]{0x3B, 0x03}; //尾
        byte[] resultData = new byte[preifx.length + data.length + suffix.length];
        System.arraycopy(preifx, 0, resultData, 0, preifx.length);
        System.arraycopy(data, 0, resultData, preifx.length, data.length);
        System.arraycopy(suffix, 0, resultData, preifx.length + data.length, suffix.length);
        return resultData;
    }
}
