package com.withintent.samplebleapp;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Protocol {
    /**
     * trezor 协议编码
     *
     * 首包格式：?##<msg type><data len><data>
     * 其他包格式：<data len><data>
     *
     * @param data
     * @return
     */
//    public static byte[] encode(int messageType, byte[] data) {
//        Buffer buffer = new Buffer();
//        buffer.writeByte('?')
//                .writeByte('#')
//                .writeByte('#')
//                .writeShort(messageType)  // msg id (2 bytes)
//                .writeInt(data.length)  // msg data length (4 bytes)
//                .write(data);
//        return buffer.readByteArray();
//    }

    /**
     * 分割数据为指定大小的包
     *
     * @param data
     * @param sliceSize android: 192 bytes, ios: 128 bytes
     * @return
     */
    public static List<byte[]> slice(byte[] data, int sliceSize) {
        // 分片数
        int pieces = (int) Math.floor((data.length - 1) / sliceSize) + 1;
        List<byte[]> sliceLists = new ArrayList<>(pieces);
        int dataLength = data.length;
        int offset = 0;

        while (offset < dataLength) {
            int packetLength = Math.min(dataLength, sliceSize);
            byte[] buffer = new byte[packetLength];
            System.arraycopy(data, offset, buffer, 0, packetLength);
            sliceLists.add(buffer);
            offset += packetLength;
        }
        return sliceLists;
    }

    /**
     * 分割数据为 192 bytes 大小的包
     * @param data
     * @return
     */
    public static List<byte[]> slice(byte[] data) {
        return slice(data, ProtocolConstants.ANDROID_BLE_PACKET_SIZE);
    }


    public static byte[] decode(byte[] data) {
        return null;
    }
}
