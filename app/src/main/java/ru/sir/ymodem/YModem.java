package ru.sir.ymodem;

import android.util.Log;

import com.code19.mcuupdate.Constants;
import com.code19.mcuupdate.eventbus.PostEventBus;
import com.van.uart.UartManager;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * YModem.<br/>
 * Block 0 contain minimal file information (only filename)<br/>
 * <p>
 * Created by Anton Sirotinkin (aesirot@mail.ru), Moscow 2014<br/>
 * I hope you will find this program useful.<br/>
 * You are free to use/modify the code for any purpose, but please leave a reference to me.<br/>
 */
public class YModem {
    private Modem modem;

    public YModem(UartManager manager) {
        this.modem = new Modem(manager);
    }

    /**
     * Send a file.<br/>
     * <p>
     * This method support correct thread interruption, when thread is interrupted "cancel of transmission" will be send.
     * So you can move long transmission to other thread and interrupt it according to your algorithm.
     *
     * @param file
     * @throws IOException
     */
    public void send(File file) throws IOException {
        //check filename
        if (!file.getName().matches("\\w{1,8}\\.\\w{1,3}")) {
            PostEventBus.post("Filename must be in DOS style (no spaces, max 8.3)");
            throw new IOException("Filename must be in DOS style (no spaces, max 8.3)");
        }
        //open file
        try (DataInputStream dataStream = new DataInputStream(new FileInputStream(file))) {
            Timer timer = new Timer(Modem.WAIT_FOR_RECEIVER_TIMEOUT).start();
            boolean useCRC16 = modem.waitReceiverRequest(timer);
            CRC crc;
            if (useCRC16)
                crc = new CRC16();
            else
                crc = new CRC8();

            //send block 0
            String fileNameString = file.getName() + (char) 0 + /*getFileSizes(file)*/ file.length() + ' ';
            int packCount = (int) (Math.ceil((double) file.length() / 1024));
            Log.i("gh0st", "fileNameString:" + fileNameString + "packCount:" + (packCount + 1));
            Constants.sCountPro = packCount + 1;
            byte[] fileNameBytes = Arrays.copyOf(fileNameString.getBytes(), 128);
            modem.sendBlock(0, Arrays.copyOf(fileNameBytes, 128), 128, crc);

            modem.waitReceiverRequest(timer);
            //send data
            byte[] block = new byte[1024];
            modem.sendDataBlocks(dataStream, 1, crc, block);

            modem.sendEOT();
        }
    }

    /**
     * Send files in batch mode.<br/>
     * <p>
     * This method support correct thread interruption, when thread is interrupted "cancel of transmission" will be send.
     * So you can move long transmission to other thread and interrupt it according to your algorithm.
     *
     * @param files
     * @throws IOException
     */
    public void batchSend(File... files) throws IOException {
        for (File file : files) {
            send(file);
        }
        sendBatchStop();
    }

    private void sendBatchStop() throws IOException {
        Timer timer = new Timer(Modem.WAIT_FOR_RECEIVER_TIMEOUT).start();
        boolean useCRC16 = modem.waitReceiverRequest(timer);
        CRC crc;
        if (useCRC16)
            crc = new CRC16();
        else
            crc = new CRC8();

        //send block 0
        byte[] bytes = new byte[128];
        modem.sendBlock(0, bytes, bytes.length, crc);
    }
}
