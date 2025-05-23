package com.szfission.wear.demo.util;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * describe:
 * author: wl
 * createTime: 2025/3/25
 */
public class CRC32Calculator {
    private static final int[] CRC_TABLE = new int[256];

    static {
        final int POLYNOMIAL = 0xEDB88320;
        for (int i = 0; i < 256; i++) {
            int crc = i;
            for (int j = 0; j < 8; j++) {
                if ((crc & 1) != 0) {
                    crc = (crc >>> 1) ^ POLYNOMIAL;
                } else {
                    crc >>>= 1;
                }
            }
            CRC_TABLE[i] = crc;
        }
    }

    public static int crc32CalWithInitial(int initialCrc, byte[] data, int size) {
        int crc = initialCrc;
        for (int i = 0; i < size; i++) {
            crc = CRC_TABLE[(crc ^ (data[i] & 0xFF)) & 0xFF] ^ (crc >>> 8);
        }
        return crc;
    }

    public static int calculateFileCRC(String filePath) {
        int crc = 0;
        try (FileInputStream fis = new FileInputStream(filePath)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) > 0) {
                crc = crc32CalWithInitial(crc, buffer, bytesRead);
            }
        } catch (IOException e) {
            System.err.println("Failed to open file: " + e.getMessage());
            return 0;
        }
        return crc;
    }
}
