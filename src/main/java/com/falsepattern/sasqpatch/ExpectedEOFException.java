package com.falsepattern.sasqpatch;

import java.io.EOFException;
import java.io.IOException;

public class ExpectedEOFException extends IOException {
    public ExpectedEOFException(EOFException cause) {
        super(cause);
    }
}
