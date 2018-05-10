package nl.weeaboo.lua2.lib;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

final class StdStreamsRedirect implements TestRule {

    private final PipedOutputStream inFeeder;
    private final ByteArrayOutputStream outBuffer;
    private final ByteArrayOutputStream errBuffer;

    public StdStreamsRedirect() {
        inFeeder = new PipedOutputStream();
        outBuffer = new ByteArrayOutputStream();
        errBuffer = new ByteArrayOutputStream();
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                InputStream oldIn = System.in;
                PrintStream oldOut = System.out;
                PrintStream oldErr = System.err;

                try {
                    System.setIn(new PipedInputStream(inFeeder));
                    System.setOut(new PrintStream(outBuffer));
                    System.setErr(new PrintStream(errBuffer));

                    base.evaluate();
                } finally {
                    System.setIn(oldIn);
                    System.setOut(oldOut);
                    System.setErr(oldErr);
                }
            }
        };
    }

    public void writeToStdIn(String string) throws IOException {
        inFeeder.write(string.getBytes("UTF-8"));
    }

    public String readStdOut() throws IOException {
        String string = outBuffer.toString("UTF-8");
        outBuffer.reset();
        return string;
    }

    public String readStdErr() throws IOException {
        String string = errBuffer.toString("UTF-8");
        errBuffer.reset();
        return string;
    }

}
