package nl.weeaboo.lua2.vm;

/**
 * Lua stack trace element.
 */
public final class LuaStackTraceElement {

    private final String fileName;
    private final int line;

    private final String functionName;

    public LuaStackTraceElement(String fileName, int line, String functionName) {
        this.fileName = fileName;
        this.line = line;
        this.functionName = functionName;
    }

    /**
     * The file name of the source file.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * The line number in the source file.
     */
    public int getLineNumber() {
        return line;
    }

    /**
     * The function or
     */
    public String getFunctionName() {
        return functionName;
    }

    @Override
    public String toString() {
        return functionName + " (" + fileName + ":" + line + ")";
    }

    /**
     * Returns an equivalent Java {@link StackTraceElement}.
     */
    public StackTraceElement toJavaStackTraceElement() {
        return new StackTraceElement("Lua", functionName, fileName, line);
    }

}
