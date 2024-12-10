package com.falsepattern.sasqpatch.payload;


public interface IPayload {
    IPayloadReader createReader();
    IPayloadWriter createWriter();
    int type();
    String extraNameInfo();
}
