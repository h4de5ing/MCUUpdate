package ru.sir.ymodem;


import android.util.Log;

import com.code19.mcuupdate.DataUtils;
import com.code19.mcuupdate.OnChangeListener;
import com.van.uart.LastError;
import com.van.uart.UartManager;

import java.io.DataInputStream;
import java.io.IOException;


/**
 * This is core Modem class supporting XModem (and some extensions XModem-1K, XModem-CRC), and YModem.<br/>
 * YModem support is limited (currently block 0 is ignored).<br/>
 * <br/>
 * Created by Anton Sirotinkin (aesirot@mail.ru), Moscow 2014 <br/>
 * I hope you will find this program useful.<br/>
 * You are free to use/modify the code for any purpose, but please leave a reference to me.<br/>
 */
class Modem {
    private final UartManager mUartManager;
    /* Protocol characters used */
    protected static final byte SOH = 0x01; /* Start Of Header */
    protected static final byte STX = 0x02; /* Start Of Text (used like SOH but means 1024 block size) */
    protected static final byte EOT = 0x04; /* End Of Transmission */
    protected static final byte ACK = 0x06; /* ACKnowlege */
    protected static final byte NAK = 0x15; /* Negative AcKnowlege */
    protected static final byte CAN = 0x18; /* CANcel character */

    protected static final byte CPMEOF = 0x1A;
    protected static final byte ST_C = 'C';

    protected static final int MAXERRORS = 10;

    protected static final int BLOCK_TIMEOUT = 1000;
    protected static final int REQUEST_TIMEOUT = 3000;
    protected static final int WAIT_FOR_RECEIVER_TIMEOUT = 60_000;
    protected static final int SEND_BLOCK_TIMEOUT = 10_000;

    protected Modem(UartManager manager) {
        mUartManager = manager;
    }


    /**
     * Wait for receiver request for transmission
     *
     * @return TRUE if receiver requested CRC-16 checksum, FALSE if 8bit checksum
     */
    protected boolean waitReceiverRequest(Timer timer, OnChangeListener listener) throws IOException {
        listener.post("Connecting ...");
        int character;
        while (true) {
            try {
                character = readByte(timer);
                if (character == NAK) return false;
                if (character == ST_C) return true;
            } catch (TimeoutException e) {
                listener.post("TimeOut,Please try again!!");
                throw new IOException("Timeout waiting for receiver");
            }
        }
    }

    protected void sendDataBlocks(DataInputStream dataStream, int blockNumber, CRC crc, byte[] block, OnChangeListener listener) throws IOException {
        int allFileLength = dataStream.available();
        listener.post("write file start ...");
        int dataLength;
        float sendLength = 0f;
        while ((dataLength = dataStream.read(block)) != -1) {
            sendLength += dataLength;
            listener.progress((int) (sendLength / allFileLength * 100.0));
            sendBlock(blockNumber++, block, dataLength, crc, listener);
        }
        listener.post("\nwrite file finish ...");
    }

    protected void sendEOT(OnChangeListener listener) throws IOException {
        listener.post("application start! ...");
        int errorCount = 0;
        Timer timer = new Timer(BLOCK_TIMEOUT);
        int character;
        while (errorCount < 10) {
            sendByte(EOT);
            try {
                character = readByte(timer.start());
                if (character == ACK) {
                    return;
                } else if (character == CAN) {
                    listener.post("Transmission terminated");
                    throw new IOException("Transmission terminated");
                }
            } catch (TimeoutException ignored) {
            }
            errorCount++;
        }
    }

    protected void sendBlock(int blockNumber, byte[] block, int dataLength, CRC crc, OnChangeListener listener) throws IOException {
        int errorCount;
        int character;
        Timer timer = new Timer(SEND_BLOCK_TIMEOUT);
        if (dataLength < block.length) block[dataLength] = CPMEOF;
        errorCount = 0;
        while (errorCount < MAXERRORS) {
            timer.start();

            if (block.length == 1024) write(new byte[]{STX});
            else //128
                write(new byte[]{SOH});

            write(new byte[]{(byte) blockNumber});
            write(new byte[]{((byte) ~blockNumber)});
            write(block);
            writeCRC(block, crc);
            while (true) {
                try {
                    character = readByte(timer);
                    if (character == ACK) {
                        return;
                    } else if (character == NAK) {
                        errorCount++;
                        break;
                    } else if (character == CAN) {
                        listener.post("Transmission terminated");
                        throw new IOException("Transmission terminated");
                    }
                } catch (TimeoutException e) {
                    errorCount++;
                    break;
                }
            }

        }
        listener.post("Too many errors caught, abandoning transfer");
        throw new IOException("Too many errors caught, abandoning transfer");
    }

    private void writeCRC(byte[] block, CRC crc) {
        byte[] crcBytes = new byte[crc.getCRCLength()];
        long crcValue = crc.calcCRC(block);
        for (int i = 0; i < crc.getCRCLength(); i++) {
            crcBytes[crc.getCRCLength() - i - 1] = (byte) ((crcValue >> (8 * i)) & 0xFF);
        }
        write(crcBytes);
    }


    protected void sendByte(byte b) {
        write(new byte[]{b});
    }

    private void shortSleep() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            try {
                interruptTransmission();
            } catch (IOException ignore) {
            }
            throw new RuntimeException("Transmission was interrupted", e);
        }
    }

    protected void interruptTransmission() throws IOException {
        sendByte(CAN);
        sendByte(CAN);
    }


    private byte readByte(Timer timer) throws TimeoutException {
        while (true) {
            byte[] buf = new byte[1];
            try {
                int read = mUartManager.read(buf, 1, 100, 20);
                if (read > 0) {
                    Log.i("gh0st", "YMode：" + DataUtils.bytes2HexString(buf));
                    return buf[0];
                }
            } catch (LastError lastError) {
                Log.e("gh0st", lastError.toString());
                lastError.printStackTrace();
            }
            if (timer.isExpired()) {
                throw new TimeoutException();
            }
            shortSleep();
        }
    }

    public void write(byte[] b) {
        try {
            mUartManager.write(b, b.length);
        } catch (LastError lastError) {
            lastError.printStackTrace();
        }
    }
}

