package com.example.mediaclip;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class TestUtil {
    public static void mixPcm(String pcm1Path, String pcm2Path, String toPath
            , int vol1, int vol2) throws IOException {

        float volume1 = vol1 / 100f * 1;
        float volume2 = vol2 / 100f * 1;
//待混音的两条数据流 还原   傅里叶  复杂
        FileInputStream is1 = new FileInputStream(pcm1Path);
        FileInputStream is2 = new FileInputStream(pcm2Path);
        boolean end1 = false, end2 = false;
//        输出的数据流
        FileOutputStream fileOutputStream = new FileOutputStream(toPath);
        byte[] buffer1 = new byte[2048];
        byte[] buffer2 = new byte[2048];
        byte[] buffer3 = new byte[2048];
        short temp2, temp1;
        while (!end1 || !end2) {

            if (!end2) {
                end2 = (is2.read(buffer2) == -1);
            }
            if (!end1) {
                end1 = (is1.read(buffer1) == -1);
            }
            int voice = 0;
//2个字节
            for (int i = 0; i < buffer2.length; i += 2) {
//前 低字节  1  后面低字节 2  声量值
//                32767         -32768
                temp1 = (short) ((buffer1[i] & 0xff) | (buffer1[i + 1] & 0xff) << 8);
                temp2 = (short) ((buffer2[i] & 0xff) | (buffer2[i + 1] & 0xff) << 8);

                voice = (int) (temp1 * volume1 + temp2 * volume2);
                if (voice > 32767) {
                    voice = 32767;
                } else if (voice < -32768) {
                    voice = -32768;
                }
//
                buffer3[i] = (byte) (voice & 0xFF);
                buffer3[i + 1] = (byte) ((voice >>> 8) & 0xFF);
            }
            fileOutputStream.write(buffer3);
        }
        is1.close();
        is2.close();
        fileOutputStream.close();
    }
}
