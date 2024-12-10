package com.falsepattern.sasqpatch;

import com.falsepattern.sasqpatch.payload.IPayload;
import com.falsepattern.sasqpatch.payload.PayloadRegistry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@RequiredArgsConstructor
public class Block {
    private final boolean headerOnly;
    @Getter
    private final BlockHeader header = new BlockHeader();
    @Getter
    private IPayload payload;

    public void read(InputStream input, PayloadRegistry registry) throws IOException {
        header.read(input);
        int totalLength = header.length * Constants.SECTOR_SIZE;
        int dataLength = totalLength - BlockHeader.LENGTH_BYTES;
        if (headerOnly) {
            input.skipNBytes(dataLength);
        } else {
            if (payload == null || payload.type() != header.type) {
                payload = registry.create(header.type);
            }
            val reader = payload.createReader();
            int read = reader.read(input, dataLength, header);
            input.skipNBytes(dataLength - read);
        }
    }

    public void write(OutputStream output) throws IOException {
        val writer = payload == null ? null : payload.createWriter();
        if (writer != null)
            writer.beforeHeaderWrite(header);
        if (headerOnly || writer == null) {
            int totalLength = header.length * Constants.SECTOR_SIZE;
            int dataLength = totalLength - BlockHeader.LENGTH_BYTES;
            header.write(output);
            for (int i = 0; i < dataLength; i++) {
                output.write(0);
            }
        } else {
            int dataLength = writer.outputSizeBytes();
            int combinedLength = dataLength + BlockHeader.LENGTH_BYTES;
            int newSectorCount = ((combinedLength + Constants.SECTOR_SIZE - 1) / Constants.SECTOR_SIZE);
            if (newSectorCount > header.length)
                throw new IllegalStateException("Header resizal! Payload: " + payload.extraNameInfo());
            newSectorCount = header.length;
            int totalLength = newSectorCount * Constants.SECTOR_SIZE;
            int padding = totalLength - combinedLength;
            header.write(output);
            writer.write(output);
            for (int i = 0; i < padding; i++) {
                output.write((byte) 0);
            }
        }
    }
}
