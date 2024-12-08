package com.falsepattern.sasqpatch;

import lombok.Getter;
import lombok.Setter;
import lombok.val;
import org.apache.commons.io.EndianUtils;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class BlockHeader {
    public static final int LENGTH_BYTES = Integer.BYTES * 8;
    public int offset;
    public int length;
    public int type;
    public int compressedSize;
    public int uncompressedSize;
    public int magicA;
    public int magicB;

    public void read(InputStream input) throws IOException {
        int off1;
        try {
            off1 = EndianUtils.readSwappedInteger(input);
        } catch (EOFException e) {
            throw new ExpectedEOFException(e);
        }
        int sizeFile = EndianUtils.readSwappedInteger(input);
        int sizeMemory = EndianUtils.readSwappedInteger(input);
        int length = EndianUtils.readSwappedInteger(input);
        int off2 = EndianUtils.readSwappedInteger(input);
        int type = EndianUtils.readSwappedInteger(input);
        int magicA = EndianUtils.readSwappedInteger(input);
        int magicB = EndianUtils.readSwappedInteger(input);
        if (off1 != off2)
            throw new IllegalArgumentException("input[0h:4h] != input[10h:14h]");
        this.offset = off1;
        this.length = length;
        this.type = type;
        this.compressedSize = sizeFile;
        this.uncompressedSize = sizeMemory;
        this.magicA = magicA;
        this.magicB = magicB;
    }

    public void write(OutputStream output) throws IOException {
        EndianUtils.writeSwappedInteger(output, offset);
        EndianUtils.writeSwappedInteger(output, compressedSize);
        EndianUtils.writeSwappedInteger(output, uncompressedSize);
        EndianUtils.writeSwappedInteger(output, length);
        EndianUtils.writeSwappedInteger(output, offset);
        EndianUtils.writeSwappedInteger(output, type);
        EndianUtils.writeSwappedInteger(output, magicA);
        EndianUtils.writeSwappedInteger(output, magicB);
    }

    public String getTypeString() {
        val res = new byte[4];
        res[0] = (byte) ((type >> 24) & 0xFF);
        res[1] = (byte) ((type >> 16) & 0xFF);
        res[2] = (byte) ((type >> 8) & 0xFF);
        res[3] = (byte) (type & 0xFF);
        return new String(res, StandardCharsets.UTF_8);
    }
}
