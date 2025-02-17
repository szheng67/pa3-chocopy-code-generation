package chocopy.pa3;

import chocopy.common.astnodes.Node;
import chocopy.common.astnodes.Program;
import chocopy.common.codegen.CodeGenBase;
import chocopy.common.codegen.RiscVBackend;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Interface to code generator.
 */
public class StudentCodeGen {


    public static void main(String[] args) throws IOException {
//        String path = "src/test/data/pa3/benchmarks/exp.py.ast.typed";
        String path = "src/test/data/pa3/subtests/id_local.py.ast.typed";


        String content = Files.readString(Path.of(path), StandardCharsets.US_ASCII);

        Program p = (Program) Node.fromJSON(content);

//        System.out.println(p);

        String x= process(p, true);
        System.out.println(x);
    }

    /**
     * Perform code generation from PROGRAM, assumed to be well-typed,
     * to RISC-V, returning the assembly code.  DEBUG iff --debug was on the
     * command line.
     */
    public static String process(Program program, boolean debug) {
        /* Emit code into a ByteOutputStream, and convert to a string.
         * If you need instructions not provided by RiscVBackend, simply
         * use an extension of it. */
        try {
            RiscVBackend backend = new RiscVBackend();
            CodeGenBase cgen = new CodeGenImpl(backend);
            cgen.generate(program);

            return backend.toString();
        } catch (IllegalStateException | IllegalArgumentException e) {
            System.err.println("Error performing code generation. "
                    + "Re-run with --debug to see stack trace.");
            if (debug) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
