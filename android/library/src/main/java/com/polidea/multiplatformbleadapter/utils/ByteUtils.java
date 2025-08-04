package com.polidea.multiplatformbleadapter.utils;

public class ByteUtils {
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }


    /**
     * 将十六进制字符串转换为字节数组。
     *
     * @param hex 要转换的十六进制字符串，该字符串长度必须为偶数。
     * @return 转换后的字节数组，如果输入字符串长度为奇数或包含非十六进制字符则返回空数组。
     */
    public static byte[] hexToBytes(String hex) {
        if (hex == null || hex.length() == 0) {
            return new byte[0];
        }
        // 若十六进制字符串长度为奇数，返回空数组
        if (hex.length() % 2 != 0) {
            return new byte[0];
        }
        byte[] result = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            // 将每两个十六进制字符转换为一个字节
            result[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return result;
    }
}
