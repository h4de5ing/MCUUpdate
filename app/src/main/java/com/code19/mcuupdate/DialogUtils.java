package com.code19.mcuupdate;

import android.content.Context;
import android.os.Environment;

import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;

import java.io.File;

/**
 * Created by Gh0st on 2017/6/30.
 * 文件选择对话框
 */

public class DialogUtils {
    public interface DialogSelection {
        void onSelectedFilePaths(String[] files);
    }

    public static void select_file(Context context, final DialogSelection dialogSelection) {
        String default_dir= Environment.getExternalStorageDirectory().getAbsolutePath();
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = new File(default_dir);
        properties.error_dir = new File(default_dir);
        properties.offset = new File(default_dir);
        properties.extensions = null;
        FilePickerDialog dialog = new FilePickerDialog(context, properties);
        dialog.setTitle(context.getString(R.string.select_file_title));
        dialog.show();
        dialog.setDialogSelectionListener(new DialogSelectionListener() {
            @Override
            public void onSelectedFilePaths(String[] files) {
                dialogSelection.onSelectedFilePaths(files);
            }
        });
    }
}
