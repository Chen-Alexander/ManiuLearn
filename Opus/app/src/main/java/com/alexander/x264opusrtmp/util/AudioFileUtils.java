package com.alexander.x264opusrtmp.util;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AudioFileUtils {
    private static final String TAG = "AudioFileUtils";

    private static FileOutputStream writer = null;

    public static void initAudioFile() {
        String fileName = "/data/data/com.alexander.x264opusrtmp/files/audio.pcm";
        try {
            File file = new File(fileName);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdir();
            }
            writer = new FileOutputStream(file, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeAudioBytes(byte[] array) {
        try {
            writer.write(array);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void release() {
        try {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
