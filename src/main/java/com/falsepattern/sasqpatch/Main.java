package com.falsepattern.sasqpatch;

import com.falsepattern.sasqpatch.payloads.Script;
import lombok.Cleanup;
import lombok.val;
import org.apache.commons.io.FileUtils;
import unluac.Configuration;
import unluac.decompile.Decompiler;
import unluac.decompile.Output;
import unluac.decompile.OutputProvider;
import unluac.parse.BHeader;
import unluac.parse.LFunction;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    // TODO un-hardcode
    private record Game(String path, Bits bits, String outputPrefix, String... scriptPrefix) {
    }
    private static final Game NEX_MACHINA = new Game(
            "/home/falsepattern/.local/share/Steam/steamapps/common/Nex Machina",
            Bits.X64,
            "nexmachina",
            "@D:\\jenkins\\workspace\\nex_machina_master\\resotron_master\\data\\resources\\nonscene\\"
    );
    private static final Game OUTLAND = new Game(
            "/home/falsepattern/.local/share/Steam/steamapps/common/Outland",
            Bits.X32,
            "outland",
            "@c:\\external_projects\\humblebundle\\Kingdom_14_Jan_2014\\Kingdom\\data\\resources\\nonscene\\",
            "@C:\\dev\\humblebundle\\outland\\Kingdom_14_Jan_2014\\Kingdom\\data\\resources\\nonscene\\",
            "@C:\\external_projects\\humblebundle\\outland\\Kingdom_14_Jan_2014\\Kingdom\\data\\resources\\nonscene\\"

    );
    public static void main(String[] args) throws Exception {
        val game = NEX_MACHINA;
        @Cleanup val exec = Executors.newVirtualThreadPerTaskExecutor();
        val p = Path.of(game.path + "/stream.dat");
        val pOut = Path.of("./stream.dat");
        try (val sFile = new StreamFile(p, game.bits); val cOut = new BufferedOutputStream(Files.newOutputStream(pOut), 2 * 1024 * 1024)) {

            sFile.blocks(false).forEach(block -> {
                try {
                    block.write(cOut);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                if ( block.getHeader().type == Script.TYPE) {
                    exec.submit(() -> {
                        val script = (Script) block.getPayload();
                        var path = script.getPath();
                        boolean hadPrefix = false;
                        for (val strip: game.scriptPrefix) {
                            if (path.startsWith(strip)) {
                                path = path.substring(strip.length());
                                hadPrefix = true;
                                break;
                            }
                        }
                        if (!hadPrefix) {
                            System.out.println("Unknown script prefix: " + script.getPath());
                            return;
                        }
                        path = path.replace('\\', '/');
                        try {
                            val filePath = Path.of(game.outputPrefix).resolve(path);
                            Files.createDirectories(filePath.getParent());
                            val writer = new BufferedOutputStream(Files.newOutputStream(filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE));
                            @Cleanup val print = new PrintStream(writer);
                            decompile(script.getBytecode(), print);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            });

        }
        exec.shutdown();
        exec.awaitTermination(10, TimeUnit.MINUTES);
        System.out.println(FileUtils.contentEquals(p.toFile(), pOut.toFile()));
    }

    private static LFunction bytesToFunction(byte[] bytecode) {
        val config = new Configuration();
        val header = new BHeader(ByteBuffer.wrap(bytecode), config);
        return header.main;
    }

    private static void decompile(byte[] bytecode, PrintStream output) {
        val lmain = bytesToFunction(bytecode);
        val d = new Decompiler(lmain);
        d.decompile();
        d.print(new Output(new OutputProvider() {
            @Override
            public void print(String s) {
                output.print(s);
            }

            @Override
            public void print(byte b) {
                output.print(b);
            }

            @Override
            public void println() {
                output.println();
            }
        }));
    }
}
