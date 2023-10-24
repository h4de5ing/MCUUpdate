package com.code19.mcuupdate;

public class DataUtils {
    public static String bytes2HexString(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        final char[] HEX = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        for (byte datum : data) {
            int value = datum & 0xff;
            sb.append("0x").append(HEX[value / 16]).append(HEX[value % 16]).append(" ");
        }
        return sb.toString();
    }
}
