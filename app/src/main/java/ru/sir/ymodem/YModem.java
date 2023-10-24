package ru.sir.ymodem;

import com.code19.mcuupdate.OnChangeListener;
import com.van.uart.UartManager;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

public class YModem {
    private final Modem modem;

    public YModem(UartManager manager) {
        this.modem = new Modem(manager);
    }

    public void send(File file, OnChangeListener listener) throws IOException {
        //check filename
        if (!file.getName().matches("\\w{1,11}\\.\\w{1,3}")) {
            listener.post("Filename must be in DOS style (no spaces, max 8.3)");
            throw new IOException("Filename must be in DOS style (no spaces, max 8.3)");
        }
        //open file
        try (DataInputStream dataStream = new DataInputStream(new FileInputStream(file))) {
            Timer timer = new Timer(Modem.WAIT_FOR_RECEIVER_TIMEOUT).start();
            boolean useCRC16 = modem.waitReceiverRequest(timer, listener);
            CRC crc;
            if (useCRC16)
                crc = new CRC16();
            else
                crc = new CRC8();

            //send block 0
            String fileNameString = file.getName() + (char) 0 + /*getFileSizes(file)*/ file.length() + ' ';
            byte[] fileNameBytes = Arrays.copyOf(fileNameString.getBytes(), 128);
            modem.sendBlock(0, Arrays.copyOf(fileNameBytes, 128), 128, crc, listener);

            modem.waitReceiverRequest(timer, listener);
            //send data
            byte[] block = new byte[1024];
            modem.sendDataBlocks(dataStream, 1, crc, block, listener);
            modem.sendEOT(listener);
        }
    }
}
