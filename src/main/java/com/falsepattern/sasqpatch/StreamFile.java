package com.falsepattern.sasqpatch;

import com.falsepattern.sasqpatch.payload.PayloadRegistry;
import lombok.val;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StreamFile implements AutoCloseable {
    private final InputStream in;
    private final Bits bits;
    public StreamFile(Path filePath, Bits bits) throws IOException {
        in = new BufferedInputStream(Files.newInputStream(filePath), 2 * 1024 * 1024);
        this.bits = bits;
    }

    public Stream<Block> blocks(boolean headerOnly) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<>() {
            private Block nextBlock = null;
            private final PayloadRegistry registry = new PayloadRegistry(bits);
            @Override
            public boolean hasNext() {
                if (nextBlock != null)
                    return true;
                val block = new Block(headerOnly);
                try {
                    block.read(in, registry);
                } catch (IOException e) {
                    if (e instanceof ExpectedEOFException) {
                        return false;
                    } else {
                        throw new RuntimeException(e);
                    }
                }
                nextBlock = block;
                return true;
            }

            @Override
            public Block next() {
                if (nextBlock == null)
                    throw new IllegalStateException();
                val block = nextBlock;
                nextBlock = null;
                return block;
            }
        }, Spliterator.ORDERED | Spliterator.IMMUTABLE), false);
    }

    @Override
    public void close() throws Exception {
        in.close();
    }
}
