package com.szfission.wear.demo.activity;

import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.StringUtils;
import com.szfission.wear.demo.R;
import com.szfission.wear.sdk.util.StringUtil;

public class TestActivity extends BaseActivity {
    Button sub4;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_test);
        sub4 = findViewById(R.id.sub4);
        int rand_key = 698121;
        String mac = "05:a0:94:8f:a5:db";
        mac = mac.replace(":", "");
//        String strMac = str2HexStr(mac).trim();
        String finalMac = mac;

        byte ss [] = new byte[2];
        byte sb = (byte) 0;
        byte sb1 = (byte) 0xF8;

        ss[0]= sb;
        ss[1] = sb1;
        byte ssss [] = new byte[2];

        for (int i = 0;i<ss.length;i++){
            ssss[i] = (byte) (ss[i]>>8);
        }

        LogUtils.d("我就是隔壁"+ StringUtil.bytesToHexStr(ssss));

    }

    public static byte[] toByteArray(String hexString) {
        if (StringUtils.isEmpty(hexString))
            throw new IllegalArgumentException("this hexString must not be empty");

        hexString = hexString.toLowerCase();
        final byte[] byteArray = new byte[hexString.length() / 2];
        int k = 0;
        for (int i = 0; i < byteArray.length; i++) {//因为是16进制，最多只会占用4位，转换成字节需要两个16进制的字符，高位在先
            byte high = (byte) ((byte) 0xFF &Character.digit(hexString.charAt(k), 16)  );
            byte low = (byte) ((byte) 0xFF &( Character.digit(hexString.charAt(k + 1), 16)  ));
            byteArray[i] = (byte) (high << 4 | low);
            k += 2;
        }
        return byteArray;
    }

    /**
     * hex字符串转byte数组
     * @param inHex 待转换的Hex字符串
     * @return  转换后的byte数组结果
     */
    public static byte[] hexToByteArray(String inHex){
        int hexlen = inHex.length();
        byte[] result;
        if (hexlen % 2 == 1){
            //奇数
            hexlen++;
            result = new byte[(hexlen/2)];
            inHex="0"+inHex;
        }else {
            //偶数
            result = new byte[(hexlen/2)];
        }
        int j=0;
        for (int i = 0; i < hexlen; i+=2){
            result[j]=hexToByte(inHex.substring(i,i+2) );
            LogUtils.d("啊哈较大环境"+result[j]);
            j++;
        }
        return result ;
    }
    /**
     * Hex字符串转byte
     * @param inHex 待转换的Hex字符串
     * @return  转换后的byte
     */
    public static byte hexToByte(String inHex){
        LogUtils.d("健康杀手计划就",(byte)0xff & ( Integer.parseInt(inHex,16)));
        return (byte) (0xff & ( Integer.parseInt(inHex,16)));
    }

    }
