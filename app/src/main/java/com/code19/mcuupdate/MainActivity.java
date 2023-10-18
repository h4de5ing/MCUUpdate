package com.code19.mcuupdate;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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
    private File mFirmwareFile;
    private String mName = "ttyS5";
    private String mBaud = "115200";
    private ProgressBar mPBProgressBar;
    private Button mBtnStatDownload;
    private SharedPreferences mSp;

    private Handler mHandler = new Handler();

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        String message = event.getMessage();
        if (mTvResult != null && mPBProgressBar != null) {
            if (message.startsWith("pro_")) {
                int oneItem = (int) (Math.ceil((double) 100 / Constants.sCountPro));
                int Count = Constants.sCurrentPro * oneItem;
                if (Count >= 100) {
                    Count = 100;
                    mHandler.postDelayed(() -> {
                        try {
                            Runtime.getRuntime().exec("reboot");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
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
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            setTitle(getString(R.string.app_name) + " V " + packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        initView();
        EventBus.getDefault().register(this);
        mSp = PreferenceManager.getDefaultSharedPreferences(this);
        String name = mSp.getString("devices_name_devices", "");
        String baud = mSp.getString("devices_baudrate", "");
        if (!TextUtils.isEmpty(name)) {
            mName = name;
        }
        if (!TextUtils.isEmpty(baud)) {
            mBaud = baud;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }
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
        mBtnStatDownload.setOnClickListener(v -> {
            writeData();
            mTvResult.setText("");
            Constants.sCurrentPro = 0;
            Constants.sCountPro = 0;
            if (mFirmwareFile != null && mFirmwareFile.exists()) {
                new Thread(() -> {
                    if (mUartManager != null) {
                        try {
                            if (mUartManager.isOpen()) {
                                runOnUiThread(() -> mPBProgressBar.setVisibility(View.VISIBLE));
                                new YModem(mUartManager).send(mFirmwareFile);
                            }
                        } catch (IOException e) {
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

    /**
     * 发送开始升级指令,与单片机约定
     * 校验位需要重新计算55 02 30 00 01 04 35 C6 55 03
     */
    private void writeData() {
        try {
            byte[] data = {0x55, 0x02, 0x30, 0x00, 0x01, 0x04, 0x35, (byte) 0xC6, 0x55, 0x03};
            mUartManager.write(data, data.length);
        } catch (LastError lastError) {
            lastError.printStackTrace();
        }
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

    public static UartManager.BaudRate getBaudRate(int baudrate) {
        return switch (baudrate) {
            case 9600 -> UartManager.BaudRate.B9600;
            case 19200 -> UartManager.BaudRate.B19200;
            case 57600 -> UartManager.BaudRate.B57600;
            case 230400 -> UartManager.BaudRate.B230400;
            default -> UartManager.BaudRate.B115200;
        };
    }
}
