package com.falsepattern.sasqpatch.payload;

import com.falsepattern.sasqpatch.BlockHeader;
import com.falsepattern.sasqpatch.CompressionUtility;

import java.io.IOException;
import java.io.OutputStream;

public interface IPayloadWriter {
    default void beforeHeaderWrite(BlockHeader header) {}
    int outputSizeBytes();
    void write(OutputStream output) throws IOException;

    class Compressed {
        private byte[] compressedData;
        public void beforeHeaderWrite(BlockHeader header, byte[] data) {
            compressedData = CompressionUtility.compress(data);
            header.uncompressedSize = data.length;
            header.compressedSize = compressedData.length;
        }

        public int outputSizeBytes() {
            return compressedData.length;
        }

        public void write(OutputStream output) throws IOException {
            output.write(compressedData);
        }
    }
}
