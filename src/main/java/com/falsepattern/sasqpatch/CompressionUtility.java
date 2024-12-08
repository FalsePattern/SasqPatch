package com.falsepattern.sasqpatch;

import lombok.val;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class CompressionUtility {
    public static byte[] compress(byte[] data) {
        val def = new Deflater(9);
        val out = new ByteArrayOutputStream(data.length);
        val buf = new byte[data.length];
        def.setInput(data);
        def.finish();
        while (!def.finished()) {
            int amount = def.deflate(buf);
            out.write(buf, 0, amount);
        }
        return out.toByteArray();
    }

    public static byte[] decompress(byte[] compressed, int uncompressedLength) throws IOException {
        val data = new byte[uncompressedLength];
        val inf = new Inflater();
        inf.setInput(compressed);
        try {
            inf.inflate(data);
        } catch (DataFormatException e) {
            throw new IOException(e);
        }
        return data;
    }
}
