package com.example.stepcounter;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class LogToFile {
    private static final String TAG = "LTF";

    public static void log(Context context, String tag, String message) {
        // Logcat log
        Log.d(tag, message);

        // Ghi log vào file
        File logFile = new File(context.getExternalFilesDir(null), "app.log");
        try {
            FileOutputStream fos = new FileOutputStream(logFile, true); // true để ghi tiếp vào cuối file
            fos.write((tag + ": " + message + "\n").getBytes());
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, "Không thể ghi log vào file", e);
        }
    }
}
