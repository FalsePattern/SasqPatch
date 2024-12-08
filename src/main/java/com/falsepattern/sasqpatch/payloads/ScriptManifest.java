package com.falsepattern.sasqpatch.payloads;

import com.falsepattern.sasqpatch.BlockHeader;
import com.falsepattern.sasqpatch.CompressionUtility;
import com.falsepattern.sasqpatch.DataUtil;
import com.falsepattern.sasqpatch.payload.IPayload;
import com.falsepattern.sasqpatch.payload.IPayloadReader;
import com.falsepattern.sasqpatch.payload.IPayloadWriter;
import lombok.val;
import org.apache.commons.io.EndianUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ScriptManifest implements IPayload {
    public static final int TYPE = 0x5343524d;
    private ScriptRef[] scripts;

    @Override
    public IPayloadReader createReader() {
        return new IPayloadReader() {
            private final Compressed comp = new Compressed() {
                @Override
                public void processUncompressed(byte[] data) {
                    for (final ScriptRef ref : scripts) {
                        ref.scriptID = DataUtil.readLongLE(data, Math.toIntExact(ref.scriptID) + 4);
                    }
                }
            };
            @Override
            public int read(InputStream input, int maxSize, BlockHeader header) throws IOException {
                int read = 4;
                if (maxSize < read)
                    throw new IOException();
                int count = EndianUtils.readSwappedInteger(input);
                read += count * 8 + 4;
                if (maxSize < read)
                    throw new IOException();
                scripts = new ScriptRef[count];
                for (int i = 0; i < count; i++) {
                    val ref = new ScriptRef();
                    scripts[i] = ref;
                    ref.scriptID = EndianUtils.readSwappedInteger(input);
                    ref.blockOffset = EndianUtils.readSwappedInteger(input);
                }
                input.skipNBytes(4);
                read += comp.read(input, maxSize - read, header);
                return read;
            }


        };
    }

    @Override
    public IPayloadWriter createWriter() {
        return new IPayloadWriter() {
            private final Compressed comp = new Compressed();
            private int dataSize;
            @Override
            public void beforeHeaderWrite(BlockHeader header) {
                val dataOut = new ByteArrayOutputStream();
                try {
                    int i;
                    long offset;
                    EndianUtils.writeSwappedInteger(dataOut, scripts.length);
                    for (i = 0, offset = 0; i < scripts.length; i++, offset += 8) {
                        val ref = scripts[i];
                        EndianUtils.writeSwappedLong(dataOut, ref.scriptID);
                        ref.scriptID = offset;
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                val data = dataOut.toByteArray();
                dataSize = data.length - 4;
                comp.beforeHeaderWrite(header, data);
            }

            @Override
            public int outputSizeBytes() {
                return 4 + scripts.length * 8 + 4 + comp.outputSizeBytes();
            }

            @Override
            public void write(OutputStream output) throws IOException {
                int count = scripts.length;
                EndianUtils.writeSwappedInteger(output, count);
                for (int i = 0; i < count; i++) {
                    val script = scripts[i];
                    EndianUtils.writeSwappedInteger(output, Math.toIntExact(script.scriptID));
                    EndianUtils.writeSwappedInteger(output, script.blockOffset);
                }
                EndianUtils.writeSwappedInteger(output, dataSize);
                comp.write(output);
            }
        };
    }

    @Override
    public int type() {
        return TYPE;
    }

    public static class ScriptRef {
        public int blockOffset;
        public long scriptID;
    }
}
