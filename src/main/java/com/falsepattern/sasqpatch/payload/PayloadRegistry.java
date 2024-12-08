package com.falsepattern.sasqpatch.payload;

import com.falsepattern.sasqpatch.Bits;
import com.falsepattern.sasqpatch.payloads.Script;
import com.falsepattern.sasqpatch.payloads.ScriptManifest;
import com.falsepattern.sasqpatch.payloads.Unknown;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class PayloadRegistry {
    private final Map<Integer, Supplier<IPayload>> GENERATORS = new HashMap<>();

    public PayloadRegistry(Bits bits) {
        GENERATORS.put(ScriptManifest.TYPE, ScriptManifest::new);
        GENERATORS.put(Script.TYPE, () -> new Script(bits));
    }

    public IPayload create(int key) {
        return GENERATORS.getOrDefault(key, Unknown::new).get();
    }
}
