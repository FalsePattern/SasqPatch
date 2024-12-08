package com.falsepattern.sasqpatch.payload;

import com.falsepattern.sasqpatch.BlockHeader;
import com.falsepattern.sasqpatch.CompressionUtility;
import lombok.val;

import java.io.IOException;
import java.io.InputStream;

public interface IPayloadReader {
    int read(InputStream input, int maxSize, BlockHeader header) throws IOException;

    abstract class Compressed {
        public int read(InputStream input, int maxSize, BlockHeader header) throws IOException {
            int compressedLength = header.compressedSize;
            int uncompressedLength = header.uncompressedSize;
            if (maxSize < compressedLength)
                throw new IOException();
            val comp = new byte[compressedLength];
            int len = input.readNBytes(comp, 0, compressedLength);
            if (len != compressedLength)
                throw new IOException();
            snapshotCompressed(comp);
            val data = CompressionUtility.decompress(comp, uncompressedLength);
            processUncompressed(data);
            return compressedLength;
        }

        public void snapshotCompressed(byte[] data) {

        }

        public abstract void processUncompressed(byte[] data) throws IOException;
    }
}
