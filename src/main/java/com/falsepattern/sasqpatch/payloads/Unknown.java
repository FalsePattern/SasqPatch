package com.falsepattern.sasqpatch.payloads;

import com.falsepattern.sasqpatch.payload.IPayload;
import com.falsepattern.sasqpatch.payload.IPayloadReader;
import com.falsepattern.sasqpatch.payload.IPayloadWriter;

import java.io.IOException;
import java.io.OutputStream;

public class Unknown implements IPayload {
    private byte[] data;

    @Override
    public IPayloadReader createReader() {
        return (input, maxSize, header) -> {
            data = input.readNBytes(maxSize);
            return data.length;
        };
    }

    @Override
    public IPayloadWriter createWriter() {
        return new IPayloadWriter() {
            @Override
            public int outputSizeBytes() {
                if (data == null)
                    return 0;
                return data.length;
            }

            @Override
            public void write(OutputStream output) throws IOException {
                if (data == null)
                    return;
                output.write(data);
            }
        };
    }

    @Override
    public int type() {
        return 0;
    }

    @Override
    public String extraNameInfo() {
        return "UNKNOWN ";
    }
}
