package com.withintent.samplebleapp;


import android.util.Log;

import com.polidea.multiplatformbleadapter.utils.ByteUtils;

import java.util.Arrays;

import okio.Buffer;

/**
 * 指令格式校验工具类
 *
 * @author FS
 * @time 2024/1/25 15:26
 */
public class CmdUtils {

    private static long msgDataLen = 0L;
    private static int packetSize = 0;

    private static Buffer buffer = new Buffer();

    private CmdUtils() {
    }

    /**
     * 验证是否首包数据
     * 格式：?##<msg type><data len><data>
     *
     * @param chunk
     * @return
     */
    public static boolean isHeaderChunk(byte[] chunk) {
        if (chunk.length < 9) {
            return false;
        }

        byte magicQuestionMark = chunk[0];
        byte sharp1 = chunk[1];
        byte sharp2 = chunk[2];

        return (Character.toChars(magicQuestionMark & 0xFF)[0] == Character.toChars(ProtocolConstants.MESSAGE_TOP_CHAR)[0] &&
                Character.toChars(sharp1 & 0xFF)[0] == Character.toChars(ProtocolConstants.MESSAGE_HEADER_BYTE)[0] &&
                Character.toChars(sharp2 & 0xFF)[0] == Character.toChars(ProtocolConstants.MESSAGE_HEADER_BYTE)[0]);
    }

    /**
     * @param src
     * @param offset
     * @return
     */
    public static int decode16BE(byte[] src, int offset) {
        return (int) Byte.toUnsignedInt(src[offset + 1])
                | (int) Byte.toUnsignedInt(src[offset + 0]) << 8;
    }

    /**
     * 计算有效数据长度
     *
     * @param src
     * @param offset 开始计算的偏移位置
     * @return
     */
    public static long decode32BE(byte[] src, int offset) {
        return (long) Byte.toUnsignedInt(src[offset + 3])
                | (long) Byte.toUnsignedInt(src[offset + 2]) << 8
                | (long) Byte.toUnsignedInt(src[offset + 1]) << 16
                | (long) Byte.toUnsignedInt(src[offset + 0]) << 24;
    }

    public static void parseCmd(byte[] value) {
        packetSize = value.length;
        if (CmdUtils.isHeaderChunk(value)) {
            // ?##<msg type><data len><data>
            msgDataLen = CmdUtils.decode32BE(value, 5);
            // ?##<msg type><data len>
            msgDataLen += 1 + 2 + 2 + 4;
            // 不保留 ?##
            buffer.write(Arrays.copyOfRange(value, 3, packetSize));
        } else {
            buffer.write(Arrays.copyOfRange(value, 0, packetSize));
        }
        msgDataLen -= packetSize;

        if (msgDataLen <= 0) {
            byte[] byteArray = buffer.readByteArray();
            Log.d("FS", ">>> 完整的指令数据：" + ByteUtils.bytesToHex(byteArray));
        }
    }
}