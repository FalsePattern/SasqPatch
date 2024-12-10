package com.falsepattern.sasqpatch;

import com.falsepattern.sasqpatch.payloads.Script;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.luaj.vm2.Prototype;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.jse.JsePlatform;
import unluac.Configuration;
import unluac.decompile.Decompiler;
import unluac.decompile.Output;
import unluac.decompile.OutputProvider;
import unluac.parse.BHeader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    // TODO un-hardcode
    private record Game(String name, Bits bits, String... scriptPrefix) {
    }
    private static final Game NEX_MACHINA = new Game(
            "nexmachina",
            Bits.X64,
            "nexmachina",
            "@D:\\jenkins\\workspace\\nex_machina_master\\resotron_master\\data\\resources\\nonscene\\"
    );
    // TODO recompiling not yet implemented for Outland
//    private static final Game OUTLAND = new Game(
//            "outland",
//            Bits.X32,
//            "@c:\\external_projects\\humblebundle\\Kingdom_14_Jan_2014\\Kingdom\\data\\resources\\nonscene\\",
//            "@C:\\dev\\humblebundle\\outland\\Kingdom_14_Jan_2014\\Kingdom\\data\\resources\\nonscene\\",
//            "@C:\\external_projects\\humblebundle\\outland\\Kingdom_14_Jan_2014\\Kingdom\\data\\resources\\nonscene\\"
//
//    );
    private static final String recPrefix = "--SP_recompile";
    private static final byte[] recPrefixBytes = recPrefix.getBytes(StandardCharsets.UTF_8);


    public static void main(String[] args) throws Exception {
        boolean recompile = Arrays.asList(args).contains("recompile");
        JsePlatform.standardGlobals();
        val game = NEX_MACHINA;
        @Cleanup val exec = Executors.newVirtualThreadPerTaskExecutor();
        val p = Path.of("games", game.name, "input", "stream.dat");
        val pOut = Path.of("games", game.name, "output", "stream.dat");
        Files.createDirectories(p.getParent());
        Files.createDirectories(pOut.getParent());
        if (!Files.exists(p)) {
            System.err.println("Copy stream.dat into " + p.toAbsolutePath().getParent() + "/");
            return;
        }
        val blocks = new ArrayList<Block>();
        final OutputStream cOut;
        if (recompile) {
            cOut = new BufferedOutputStream(Files.newOutputStream(pOut, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE, StandardOpenOption.WRITE), 2 * 1024 * 1024);
        } else {
            cOut = null;
        }
        try (val sFile = new StreamFile(p, game.bits)) {
            val prefixDir = Path.of("games", game.name, "dev");
            Files.createDirectories(prefixDir);
            sFile.blocks(false)
                 .map(block -> switch (block.getHeader().type) {
                     case Script.TYPE -> CompletableFuture.supplyAsync(() -> transformScript(block, game, prefixDir), exec);
                     default -> CompletableFuture.completedFuture(block);
                 })
                 .toList()
                 .forEach(future -> {
                     final Block block;
                     try {
                         block = future.get();
                     } catch (InterruptedException | ExecutionException e) {
                         throw new RuntimeException(e);
                     }
                     if (recompile) {
                         try {
                             block.write(cOut);
                         } catch (IOException e) {
                             throw new RuntimeException(e);
                         }
                     }
                 });
        } finally {
            exec.shutdown();
            exec.awaitTermination(10, TimeUnit.MINUTES);
            if (cOut != null) {
                cOut.close();
            }
        }
    }

    @SneakyThrows
    private static Block transformScript(Block block, Game game, Path prefixDir) {
        val script = (Script) block.getPayload();
        var path = script.getPath();
        String prefix = null;
        for (val strip: game.scriptPrefix) {
            if (path.startsWith(strip)) {
                path = path.substring(strip.length());
                prefix = strip;
                break;
            }
        }
        if (prefix == null) {
            System.err.println("Unknown script prefix: " + script.getPath());
            return block;
        }
        path = path.replace('\\', '/');
        val decompiledFilePath = prefixDir.resolve(path);
        val parentDir = decompiledFilePath.getParent();
        val bc = script.getBytecode();
        Files.createDirectories(parentDir);
        if (!Files.exists(decompiledFilePath)) {
            decompile(bc, decompiledFilePath);
            return block;
        }
        @Cleanup val input = new BufferedInputStream(Files.newInputStream(decompiledFilePath));
        input.mark(recPrefixBytes.length);
        val buf = input.readNBytes(recPrefixBytes.length);
        if (!Arrays.equals(buf, recPrefixBytes)) {
            return block;
        }
        System.out.println("Recompiling " + path);
        input.reset();
        script.setBytecode(compile(input, script.getPath()));
        return block;
    }

    private static byte[] compile(InputStream input, String path) throws IOException {
        final Prototype compiledProto = LuaC.compile(input, path);
        byte[] compiled;
        {
            val recompiledOut = new ByteArrayOutputStream();
            DumpState.dump(compiledProto, recompiledOut, true, 0, true);
            compiled = recompiledOut.toByteArray();
        }
        return compiled;
    }

    private static void decompile(byte[] bytecode, Path decompiledFilePath) throws IOException {
        val decompiledOut = new ByteArrayOutputStream();
        val print = new PrintStream(decompiledOut);
        doDecompile(bytecode, print);
        print.close();
        Files.write(decompiledFilePath, decompiledOut.toByteArray());
    }

    private static synchronized void doDecompile(byte[] bytecode, PrintStream output) {
        val config = new Configuration();
        val header = new BHeader(ByteBuffer.wrap(bytecode), config);
        val lmain = header.main;
        val d = new Decompiler(lmain);
        d.decompile();
        d.print(new Output(new PrintStreamOutputProvider(output)));
    }

    @RequiredArgsConstructor
    private static class PrintStreamOutputProvider implements OutputProvider {
        private final PrintStream print;
        @Override
        public void print(String s) {
            print.print(s);
        }

        @Override
        public void print(byte b) {
            print.print(b);
        }

        @Override
        public void println() {
            print.println();
        }
    }
}
