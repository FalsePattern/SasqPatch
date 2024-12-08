package com.falsepattern.sasqpatch;

public class DataUtil {
    public static int readIntLE(byte[] buf, int offset) {
        return
                (buf[offset] & 0xFF) |
                ((buf[offset + 1] & 0xFF) << 8) |
                ((buf[offset + 2] & 0xFF) << 16) |
                ((buf[offset + 3] & 0xFF) << 24);
    }
    public static long readLongLE(byte[] buf, int offset) {
        return
                (buf[offset] & 0xFFL) |
                ((buf[offset + 1] & 0xFFL) << 8) |
                ((buf[offset + 2] & 0xFFL) << 16) |
                ((buf[offset + 3] & 0xFFL) << 24) |
                ((buf[offset + 4] & 0xFFL) << 32) |
                ((buf[offset + 5] & 0xFFL) << 40) |
                ((buf[offset + 6] & 0xFFL) << 48) |
                ((buf[offset + 7] & 0xFFL) << 56);
    }

    public static void writeIntLE(int value, byte[] buf, int offset) {
        buf[offset] = (byte) (value & 0xFF);
        buf[offset + 1] = (byte) ((value >> 8) & 0xFF);
        buf[offset + 2] = (byte) ((value >> 16) & 0xFF);
        buf[offset + 3] = (byte) ((value >> 24) & 0xFF);
    }

    public static void writeLongLE(long value, byte[] buf, int offset) {
        buf[offset] = (byte) (value & 0xFFL);
        buf[offset + 1] = (byte) ((value >> 8) & 0xFFL);
        buf[offset + 2] = (byte) ((value >> 16) & 0xFFL);
        buf[offset + 3] = (byte) ((value >> 24) & 0xFFL);
        buf[offset + 4] = (byte) ((value >> 32) & 0xFFL);
        buf[offset + 5] = (byte) ((value >> 40) & 0xFFL);
        buf[offset + 6] = (byte) ((value >> 48) & 0xFFL);
        buf[offset + 7] = (byte) ((value >> 56) & 0xFFL);
    }
}
