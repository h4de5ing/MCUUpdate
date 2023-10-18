package com.code19.mcuupdate;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.van.uart.UartManager;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


/**
 * Created by gh0st on 2017/9/26.
 * 设置
 */

public class SettingsPreferenceFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

    private ListPreference mLpName, mLpBaudRate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        String[] devices = removal(UartManager.devices());//过滤重复
        mLpName = (ListPreference) findPreference("devices_name_devices");
        mLpBaudRate = (ListPreference) findPreference("devices_baudrate");
        Preference mTips = findPreference("tips");
        try {
            PackageInfo packageInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            mTips.setTitle(getString(R.string.pref_update_title, packageInfo.versionName));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        mLpName.setEntries(devices);
        mLpName.setEntryValues(devices);
        mLpName.setSummary(getString(R.string.pref_name_summary) + " : " + mLpName.getValue());

        mLpBaudRate.setSummary(getString(R.string.pref_baud_summary) + " : " + mLpBaudRate.getValue());
        mLpName.setOnPreferenceChangeListener(this);
        mLpBaudRate.setOnPreferenceChangeListener(this);
    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if ("devices_name_devices".equals(key)) {
            mLpName.setSummary(getString(R.string.pref_name_summary) + " : " + newValue);
            mLpName.setValue(newValue.toString());
        } else if ("devices_baudrate".equals(key)) {
            mLpBaudRate.setSummary(getString(R.string.pref_baud_summary) + " : " + newValue);
            mLpBaudRate.setValue(newValue.toString());
        }
        return false;
    }

    public String[] removal(Object[] devices) {
        Set set = new HashSet<String>();
        Collections.addAll(set, devices);
        Object[] objects = set.toArray();
        String[] strArray = new String[objects.length];
        for (int i = 0; i < objects.length; i++) {
            strArray[i] = objects[i].toString();
        }
        return strArray;
    }
}
