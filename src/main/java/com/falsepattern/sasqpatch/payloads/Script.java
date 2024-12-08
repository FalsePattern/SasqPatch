package com.falsepattern.sasqpatch.payloads;

import com.falsepattern.sasqpatch.Bits;
import com.falsepattern.sasqpatch.BlockHeader;
import com.falsepattern.sasqpatch.DataUtil;
import com.falsepattern.sasqpatch.payload.IPayload;
import com.falsepattern.sasqpatch.payload.IPayloadReader;
import com.falsepattern.sasqpatch.payload.IPayloadWriter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.val;
import org.apache.commons.io.EndianUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

@RequiredArgsConstructor
public class Script implements IPayload {
    public static final int TYPE = 0x53435249;
    private final Bits bits;
    @Getter
    private long id;
    @Getter
    private String path;
    @Getter
    @Setter
    private byte[] bytecode;
    private byte[] compressedUnmodified;
    private boolean modified = false;

    @Override
    public IPayloadReader createReader() {
        return new IPayloadReader() {
            private final Compressed comp = new Compressed() {
                @Override
                public void processUncompressed(byte[] data) throws IOException {
                    int start, end;
                    switch (bits) {
                        case X32 -> {
                            id = DataUtil.readIntLE(data, 0);
                            start = DataUtil.readIntLE(data, 4);
                            end = DataUtil.readIntLE(data, 8);
                        }
                        case X64 -> {
                            id = DataUtil.readLongLE(data, 0);
                            start = Math.toIntExact(DataUtil.readIntLE(data, 8));
                            end = Math.toIntExact(DataUtil.readIntLE(data, 16));
                        }
                        case null -> throw new IOException();
                    }
                    bytecode = Arrays.copyOfRange(data, start, end);
                    path = switch (bits) {
                        case X32 -> {
                            int length = DataUtil.readIntLE(bytecode, 12);
                            yield new String(bytecode, 16, length - 1);
                        }
                        case X64 -> {
                            int length = Math.toIntExact(DataUtil.readLongLE(bytecode, 12));
                            yield new String(bytecode, 20, length - 1);
                        }
                    };
                }

                @Override
                public void snapshotCompressed(byte[] data) {
                    compressedUnmodified = data;
                }
            };

            @Override
            public int read(InputStream input, int maxSize, BlockHeader header) throws IOException {
                int read = 16;
                if (maxSize < 16)
                    throw new IOException();
                if (EndianUtils.readSwappedInteger(input) != 0) {
                    throw new IOException();
                }
                switch (bits) {
                    case X32 -> {
                        if (EndianUtils.readSwappedInteger(input) != 0) {
                            throw new IOException();
                        }
                        if (EndianUtils.readSwappedInteger(input) != 4) {
                            throw new IOException();
                        }
                        if (EndianUtils.readSwappedInteger(input) != 8) {
                            throw new IOException();
                        }
                    }
                    case X64 -> {
                        if (EndianUtils.readSwappedInteger(input) != 0) {
                            throw new IOException();
                        }
                        if (EndianUtils.readSwappedInteger(input) != 8) {
                            throw new IOException();
                        }
                        if (EndianUtils.readSwappedInteger(input) != 16) {
                            throw new IOException();
                        }
                    }
                }
                read += comp.read(input, maxSize - read, header);
                return read;
            };
        };
    }

    @Override
    public IPayloadWriter createWriter() {
        return new IPayloadWriter() {
            private final Compressed comp = new Compressed();
            @Override
            public void beforeHeaderWrite(BlockHeader header) {
                if (modified) {
                    int headerSize = switch (bits) {
                        case X32 -> 12;
                        case X64 -> 24;
                    };
                    int resultSize = bytecode.length + headerSize;
                    int start = headerSize;
                    int end = headerSize + bytecode.length;
                    val buf = new byte[resultSize];
                    switch (bits) {
                        case X32 -> {
                            DataUtil.writeIntLE(Math.toIntExact(id), buf, 0);
                            DataUtil.writeIntLE(start, buf, 4);
                            DataUtil.writeIntLE(end, buf, 8);
                        }
                        case X64 -> {
                            DataUtil.writeLongLE(id, buf, 0);
                            DataUtil.writeLongLE(start, buf, 8);
                            DataUtil.writeLongLE(end, buf, 16);
                        }
                    }
                    System.arraycopy(bytecode, 0, buf, start, bytecode.length);
                    comp.beforeHeaderWrite(header, buf);
                } else {
                    header.compressedSize = compressedUnmodified.length;
                    header.uncompressedSize = bytecode.length + switch (bits) {
                        case X32 -> 12;
                        case X64 -> 24;
                    };
                }
            }

            @Override
            public int outputSizeBytes() {
                int size = 16;
                if (modified) {
                    size += comp.outputSizeBytes();
                } else {
                    size += compressedUnmodified.length;
                }
                return size;
            }

            @Override
            public void write(OutputStream output) throws IOException {
                EndianUtils.writeSwappedInteger(output, 0);
                switch (bits) {
                    case X32 -> {
                        EndianUtils.writeSwappedInteger(output, 0);
                        EndianUtils.writeSwappedInteger(output, 4);
                        EndianUtils.writeSwappedInteger(output, 8);
                    }
                    case X64 -> {
                        EndianUtils.writeSwappedInteger(output, 0);
                        EndianUtils.writeSwappedInteger(output, 8);
                        EndianUtils.writeSwappedInteger(output, 16);
                    }
                }
                if (modified) {
                    comp.write(output);
                } else {
                    output.write(compressedUnmodified);
                }
            }
        };
    }

    @Override
    public int type() {
        return TYPE;
    }
}
