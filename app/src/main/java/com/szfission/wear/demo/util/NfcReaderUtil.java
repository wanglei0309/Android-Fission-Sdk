package com.szfission.wear.demo.util;

/**
 * describe:
 * author: wl
 * createTime: 2025/3/14
 */
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.NfcA;
import android.util.Log;

import com.fission.wear.sdk.v2.bean.NfcCardInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NfcReaderUtil {
    private static final String TAG = "NfcReaderUtil";

    public static List<NdefMessage> parseNfcIntent(Tag tag, byte[] id) {
        List<NdefMessage> messages = new ArrayList<>();
        if (tag == null) {
            return messages;
        }

        String payload = dumpTagData(tag);
        NdefRecord record = new NdefRecord(
                NdefRecord.TNF_UNKNOWN,
                new byte[0],
                id,
                payload.getBytes()
        );
        messages.add(new NdefMessage(new NdefRecord[]{record}));
        return messages;
    }

    public static String dumpTagData(Tag tag) {
        StringBuilder sb = new StringBuilder();
        byte[] id = tag.getId();
        sb.append("ID (hex): ").append(toHex(id)).append('\n');
        sb.append("ID (reversed hex): ").append(toReversedHex(id)).append('\n');
        sb.append("ID (dec): ").append(toDec(id)).append('\n');
        sb.append("ID (reversed dec): ").append(toReversedDec(id)).append('\n');

        sb.append("Technologies: ");
        for (String tech : tag.getTechList()) {
            sb.append(tech.replace("android.nfc.tech.", "")).append(", ");
        }
        sb.setLength(sb.length() - 2);

        for (String tech : tag.getTechList()) {
            if (tech.equals(MifareClassic.class.getName())) {
                try {
                    MifareClassic mifareTag = MifareClassic.get(tag);
                    NfcA nfcA = NfcA.get(tag);
                    sb.append("\nMifare Classic type: ").append(getMifareType(mifareTag.getType()));
                    sb.append("\nMifare size: ").append(mifareTag.getSize()).append(" bytes");
                    sb.append("\nMifare sectors: ").append(mifareTag.getSectorCount());
                    sb.append("\nMifare blocks: ").append(mifareTag.getBlockCount());
                    sb.append("\nMifare atqa: ").append(toHex(nfcA.getAtqa()));
                    sb.append("\nMifare sak: ").append(toHex(new byte[]{(byte) nfcA.getSak()}));
                } catch (Exception e) {
                    sb.append("\nMifare classic error: ").append(e.getMessage());
                }
            }
        }
        return sb.toString();
    }

    public static NfcCardInfo dumpTagDataToNfcCardInfo(Tag tag) {
        byte[] id = tag.getId();
        NfcCardInfo nfcCardInfo = new NfcCardInfo();
        String uid = toReversedHex(id);
        String atqa = "";
        String sak = "";
        for (String tech : tag.getTechList()) {
            if (tech.equals(MifareClassic.class.getName())) {
                try {
                    MifareClassic mifareTag = MifareClassic.get(tag);
                    NfcA nfcA = NfcA.get(tag);
                    atqa = toReversedHex(nfcA.getAtqa());
                    sak = toHex(new byte[]{(byte) nfcA.getSak()});
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        nfcCardInfo.setUid(uid);
        nfcCardInfo.setAtqa(atqa);
        nfcCardInfo.setSak(sak);
        return nfcCardInfo;
    }


    private static String getMifareType(int type) {
        switch (type) {
            case MifareClassic.TYPE_CLASSIC:
                return "Classic";
            case MifareClassic.TYPE_PLUS:
                return "Plus";
            case MifareClassic.TYPE_PRO:
                return "Pro";
            default:
                return "Unknown";
        }
    }

    public static byte[] readMifareClassic(Tag tag) {
        MifareClassic mifare = MifareClassic.get(tag);
        if (mifare == null) return null;

        List<byte[]> dataList = new ArrayList<>();

        try {
            mifare.connect();
            int sectorCount = mifare.getSectorCount();
            boolean is4KCard = sectorCount > 16;  // 4K 卡片有 40 个扇区，1K 卡片有 16 个扇区

            for (int i = 0; i < sectorCount; i++) {
                byte[] sectorData = new byte[0]; // 用于存储扇区数据

                if (mifare.authenticateSectorWithKeyA(i, MifareClassic.KEY_DEFAULT)) {
                    int firstBlock = mifare.sectorToBlock(i);
                    // 读取扇区中的所有块
                    for (int j = 0; j < 4; j++) {
                        int blockIndex = firstBlock + j;
                        byte[] blockData = null;
                        try {
                            blockData = mifare.readBlock(blockIndex);
                        } catch (IOException e) {
                            Log.d(TAG, "Failed to read Block " + blockIndex + " in Sector " + i);
                        }

                        if (blockData != null) {
                            // 将当前块的数据合并到扇区数据中
                            byte[] newSectorData = new byte[sectorData.length + blockData.length];
                            System.arraycopy(sectorData, 0, newSectorData, 0, sectorData.length);
                            System.arraycopy(blockData, 0, newSectorData, sectorData.length, blockData.length);
                            sectorData = newSectorData;
                            Log.d(TAG, "Sector " + i + " Block " + blockIndex + " Data: " + toHex(blockData));
                        } else {
                            Log.d(TAG, "Sector " + i + " Block " + blockIndex + " Data: NULL (Read Failed)");
                        }
                    }
                    Log.d(TAG, "Sector " + i + " Complete Data: " + toHex(sectorData));
                } else {
                    // 认证失败时补 0
                    Log.e(TAG, "Sector " + i + " Authentication Failed, Filling with 0s");
                    byte[] zeroData = new byte[64];  // 1K 卡片每个扇区 4 个块，每块 16 字节
                    Arrays.fill(zeroData, (byte) 0);
                    dataList.add(zeroData);  // 将填充 0 的数据添加到列表中
                    Log.d(TAG, "Sector " + i + " Complete Data (Filled with 0s): " + toHex(zeroData));
                }
                dataList.add(sectorData);  // 将扇区完整数据添加到列表中
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                mifare.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 将所有数据合并成一个 byte 数组
        int totalLength = 0;
        for (byte[] data : dataList) {
            totalLength += data.length;
        }

        byte[] result = new byte[totalLength];
        int offset = 0;
        for (byte[] data : dataList) {
            System.arraycopy(data, 0, result, offset, data.length);
            offset += data.length;
        }

        return result;
    }



    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = bytes.length - 1; i >= 0; i--) {
            int b = bytes[i] & 0xff;
            if (b < 0x10) sb.append('0');
            sb.append(Integer.toHexString(b)).append(" ");
        }
        return sb.toString().trim();
    }

    private static String toReversedHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte aByte : bytes) {
            int b = aByte & 0xff;
            if (b < 0x10) sb.append('0');
            sb.append(Integer.toHexString(b)).append(" ");
        }
        return sb.toString().trim();
    }

    private static long toDec(byte[] bytes) {
        long result = 0;
        long factor = 1;
        for (byte aByte : bytes) {
            result += (aByte & 0xffL) * factor;
            factor *= 256L;
        }
        return result;
    }

    private static long toReversedDec(byte[] bytes) {
        long result = 0;
        long factor = 1;
        for (int i = bytes.length - 1; i >= 0; i--) {
            result += (bytes[i] & 0xffL) * factor;
            factor *= 256L;
        }
        return result;
    }
}

