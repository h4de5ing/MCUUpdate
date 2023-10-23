package com.code19.mcuupdate;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.code19.mcuupdate.eventbus.MessageEvent;
import com.van.uart.LastError;
import com.van.uart.UartManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.IOException;

import ru.sir.ymodem.YModem;

public class MainActivity extends AppCompatActivity {

    private TextView mTvResult, mTvProgress;
    private UartManager mUartManager;
    private EditText mEt_file;
    private File mFirmwareFile = new File("/sdcard/Download/21.bin");
    private String mName = "ttyS3";
    private String mBaud = "115200";
    private ProgressBar mPBProgressBar;
    private Button mBtnStatDownload;
    //    private final byte[] data = {0x20, 0x20, 0x20, 0x78, 0x78, 0x78};
    private final byte[] data = {' ', ' ', ' ', 'x', 'x', 'x', '1'};
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean isDownloading = false;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        String message = event.getMessage();
        if (mTvResult != null && mPBProgressBar != null) {
            if (message.startsWith("pro_")) {
                int oneItem = (int) (Math.ceil((double) 100 / Constants.sCountPro));
                int Count = Constants.sCurrentPro * oneItem;
                if (Count >= 100) {
                    Count = 100;
                    isDownloading = false;
                    mHandler.postDelayed(() -> {
//                        try {
//                            Runtime.getRuntime().exec("reboot");
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
                    }, 2000);
                }
                mPBProgressBar.setProgress(Count);
                mTvProgress.setText(Count + "%");
            } else {
                if (message.equals("...")) {
                    mTvResult.append(message);
                } else {
                    mTvResult.append(message + "\n");
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        EventBus.getDefault().register(this);
        SharedPreferences mSp = PreferenceManager.getDefaultSharedPreferences(this);
        String name = mSp.getString("devices_name_devices", "");
        String baud = mSp.getString("devices_baudrate", "");
        if (!TextUtils.isEmpty(name)) mName = name;
        if (!TextUtils.isEmpty(baud)) mBaud = baud;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }
//        new Thread(() -> {
//            while (true) {
//                if (mUartManager != null && mUartManager.isOpen()) {
//                    if (!isDownloading) {
//                        byte[] buf = new byte[1024];
//                        try {
//                            int size = mUartManager.read(buf, buf.length, 50, 1);
//                            if (size > 0) {
//                                Log.i("gh0st", "收到单片机信息：" + new String(buf, 0, size) + " " + DataUtils.bytes2HexString(buf));
//                                updateTv(new String(buf, 0, size));
//                            }
//                        } catch (LastError e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//            }
//        }).start();
    }

    private void initView() {
        mEt_file = findViewById(R.id.et_file_show);
        findViewById(R.id.btn_file_select).setOnClickListener(v -> DialogUtils.select_file(MainActivity.this, files -> {
            if (files.length == 1) {
                mEt_file.setText(files[0]);
                mFirmwareFile = new File(files[0]);
            }
        }));

        mTvResult = findViewById(R.id.tv_result);
        mTvResult.setMovementMethod(ScrollingMovementMethod.getInstance());
        mTvProgress = findViewById(R.id.tv_progress);
        mPBProgressBar = findViewById(R.id.pb_progress);
        mBtnStatDownload = findViewById(R.id.btn_stat_download);
        CheckBox cb = findViewById(R.id.cb_21bin);
        cb.setOnCheckedChangeListener((compoundButton, b) -> mFirmwareFile = new File(b ? "/sdcard/Download/21.bin" : "/sdcard/Download/20.bin"));
        findViewById(R.id.btn_send0).setOnClickListener(view -> {
            try {
                new Thread(() -> {
                    if (mUartManager.isOpen()) {
                        isDownloading = false;
                        try {
                            for (byte datum : data) {
                                byte[] sendData = {datum};
                                mUartManager.write(sendData, sendData.length);
                                SystemClock.sleep(170);
                            }
                            SystemClock.sleep(100);
                            isDownloading = true;
                            runOnUiThread(() -> mPBProgressBar.setVisibility(View.VISIBLE));
                            updateTv(mFirmwareFile.getAbsolutePath() + "\n");
                            new YModem(mUartManager).send(mFirmwareFile, message -> updateTv(message + "\n"));
                        } catch (LastError | IOException e) {
                            updateTv("发生异常; " + e.getMessage());
                            e.printStackTrace();
                        }
                    } else {
                        updateTv("串口没有打开");
                    }
                }).start();
                byte[] data = {'1'};
                mUartManager.write(data, data.length);
            } catch (LastError lastError) {
                lastError.printStackTrace();
            }
        });
        mBtnStatDownload.setOnClickListener(v -> {
            mTvResult.setText("");
            Constants.sCurrentPro = 0;
            Constants.sCountPro = 0;
            if (mFirmwareFile != null && mFirmwareFile.exists()) {
                new Thread(() -> {
                    if (mUartManager != null) {
                        try {
                            if (mUartManager.isOpen()) {
                                byte[] data = {'1'};
                                mUartManager.write(data, data.length);
                                isDownloading = true;
                                runOnUiThread(() -> mPBProgressBar.setVisibility(View.VISIBLE));
                                new YModem(mUartManager).send(mFirmwareFile, message -> updateTv(message + "\n"));
                            }
                        } catch (Exception e) {
                            Log.e("gh0st", e.toString());
                        }
                    }
                }).start();
            } else {
                showToast(getString(R.string.valid_file));
            }
        });
    }

    public void showToast(String str) {
        Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
    }

    public void updateTv(String message) {
        runOnUiThread(() -> {
            mTvResult.append(message);
            int offset = mTvResult.getLineCount() * mTvResult.getLineHeight() - mTvResult.getHeight();
            if (offset >= 6000) {
                mTvResult.setText("");
            }
            mTvResult.scrollTo(0, Math.max(0, offset));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mUartManager == null) {
            if (mName.isEmpty()) {
                showToast("Devices name is Empty");
                mBtnStatDownload.setEnabled(false);
            } else {
                if (mBaud.isEmpty()) {
                    showToast("Baud rate is Empty");
                    mBtnStatDownload.setEnabled(false);
                } else {
                    mBtnStatDownload.setEnabled(true);
                    try {
                        mUartManager = new UartManager();
                        mUartManager.open(mName, getBaudRate(Integer.parseInt(mBaud)));
                    } catch (LastError lastError) {
                        Toast.makeText(this, lastError.toString(), Toast.LENGTH_SHORT).show();
                        lastError.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mUartManager != null) mUartManager.close();
        EventBus.getDefault().unregister(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_setting) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    public static UartManager.BaudRate getBaudRate(int baudRate) {
        return switch (baudRate) {
            case 9600 -> UartManager.BaudRate.B9600;
            case 19200 -> UartManager.BaudRate.B19200;
            case 57600 -> UartManager.BaudRate.B57600;
            case 230400 -> UartManager.BaudRate.B230400;
            default -> UartManager.BaudRate.B115200;
        };
    }
}
