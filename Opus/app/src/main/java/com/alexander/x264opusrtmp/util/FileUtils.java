package com.alexander.x264opusrtmp.util;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

public class FileUtils {
    private static final String TAG = "FileUtils";

    public static void writeBytes(byte[] array) {
        writeBytes(array, "/data/data/com.alexander.x264opusrtmp/files/codec.h264");
    }

    public static void writeAudioBytes(byte[] array) {
        writeBytes(array, "/data/data/com.alexander.x264opusrtmp/files/audio.pcm");
//        copyDataToFile(array, "/data/data/com.alexander.x264opusrtmp/files/audio.pcm");
    }

    public static void writeBytes(byte[] array, String fileName) {
        FileOutputStream writer = null;
        try {
            // 打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件
            File file = new File(fileName);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdir();
            }
            writer = new FileOutputStream(file, true);
            writer.write(array);
            writer.write('\n');
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void copyDataToFile(byte[] array, String fileName) {
        File outFile = new File(fileName);
        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outFile, true)));
            for (byte b : array) {
                dos.writeShort(Short.reverseBytes(b));
            }
            dos.flush();
            dos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
