package nl.weeaboo.lua2.stdlib;

import static nl.weeaboo.lua2.vm.LuaNil.NIL;
import static nl.weeaboo.lua2.vm.LuaValue.valueOf;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.lib2.LuaBoundFunction;
import nl.weeaboo.lua2.vm.LuaError;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.Varargs;

@LuaSerializable
public final class OsLib extends LuaModule {

    private static final long serialVersionUID = 1L;

    public OsLib() {
        super("os");
    }

    /**
     * This function is equivalent to the C function system. It passes command to be executed by an operating
     * system shell. It returns a status code, which is system-dependent. If command is absent, then it
     * returns nonzero if a shell is available and zero otherwise.
     *
     * @param args
     *        <ol>
     *        <li>command to pass to the system
     *        </ol>
     */
    @LuaBoundFunction
    public Varargs execute(Varargs args) throws IOException {
        throw new IOException("execute() is not supported");
    }

    /**
     * Calls the C function exit, with an optional code, to terminate the host program.
     *
     * @param args
     *        <ol>
     *        <li>(optional) exit code (int)
     *        </ol>
     */
    @LuaBoundFunction
    public Varargs exit(Varargs args) throws IOException {
        throw new IOException("exit() is not supported");
    }

    /**
     * Returns the value of the process environment variable varname, or null if the variable is not defined.
     *
     * @param args
     *        <ol>
     *        <li>varname
     *        </ol>
     * @return String value, or null if not defined
     */
    @LuaBoundFunction
    public Varargs getenv(Varargs args) {
        String varname = args.checkjstring(1);
        return valueOf(System.getProperty(varname));
    }

    /**
     * Deletes the file or directory with the given name. Directories must be empty to be removed. If this
     * function fails, it throws and IOException
     *
     * @param args
     *        <ol>
     *        <li>filename
     *        </ol>
     * @throws IOException if it fails
     */
    @LuaBoundFunction
    public Varargs remove(Varargs args) throws IOException {
        throw new IOException("remove() is not supported");
    }

    /**
     * Renames file or directory named oldname to newname. If this function fails,it throws and IOException
     *
     * @param args
     *        <ol>
     *        <li>old filename
     *        <li>new filename
     *        </ol>
     * @throws IOException if it fails
     */
    @LuaBoundFunction
    public Varargs rename(Varargs args) throws IOException {
        throw new IOException("rename() is not supported");
    }

    /**
     * Sets the current locale of the program. locale is a string specifying a locale; category is an optional
     * string describing which category to change: "all", "collate", "ctype", "monetary", "numeric", or
     * "time"; the default category is "all".
     *
     * If locale is the empty string, the current locale is set to an implementation- defined native locale.
     * If locale is the string "C", the current locale is set to the standard C locale.
     *
     * When called with null as the first argument, this function only returns the name of the current locale
     * for the given category.
     *
     * @param args
     *        <ol>
     *        <li>(optional) Locale to set
     *        <li>Category to set the locale for
     *        </ol>
     * @return the name of the new locale, or NIL if the request cannot be honored.
     */
    @LuaBoundFunction
    public Varargs setlocale(Varargs args) {
        if (args.isnil(1)) {
            return valueOf("C");
        }
        return NIL;
    }

    /**
     * Returns a string with a file name that can be used for a temporary file. The file must be explicitly
     * opened before its use and explicitly removed when no longer needed.
     *
     * On some systems (POSIX), this function also creates a file with that name, to avoid security risks.
     * (Someone else might create the file with wrong permissions in the time between getting the name and
     * creating the file.) You still have to open the file to use it and to remove it (even if you do not use
     * it).
     *
     * @param args Not used.
     * @return String filename to use
     */
    @LuaBoundFunction
    public Varargs tmpname(Varargs args) {
        String tempDir = System.getProperty("java.io.tmpdir");
        File tempFile = new File(tempDir, UUID.randomUUID() + ".tmp");
        return valueOf(tempFile.toString());
    }

    /**
     * @param args Not used.
     * @return an approximation of the amount in seconds of CPU time used by the program.
     */
    @LuaBoundFunction
    public Varargs clock(Varargs args) {
        throw new LuaError("clock() is not supported");
    }

    /**
     * If the time argument is present, this is the time to be formatted (see the os.time function for a
     * description of this value). Otherwise, date formats the current time.
     *
     * If format starts with '!', then the date is formatted in Coordinated Universal Time. After this
     * optional character, if format is the string "*t", then date returns a table with the following fields:
     * year (four digits), month (1--12), day (1--31), hour (0--23), min (0--59), sec (0--61), wday (weekday,
     * Sunday is 1), yday (day of the year), and isdst (daylight saving flag, a boolean).
     *
     * If format is not "*t", then date returns the date as a string, formatted according to the same rules as
     * the C function strftime.
     *
     * When called without arguments, date returns a reasonable date and time representation that depends on
     * the host system and on the current locale (that is, os.date() is equivalent to os.date("%c")).
     *
     * @return a LString or a LTable containing date and time, formatted according to the given string format.
     */
    @LuaBoundFunction
    public Varargs date(Varargs args) {
        String format = args.optjstring(1, null);

        Date date;
        if (args.isnil(2)) {
            date = new Date();
        } else {
            date = new Date(args.checklong(2));
        }

        if ("*t".equals(format)) {
            Calendar c = Calendar.getInstance();
            LuaTable t = new LuaTable();
            t.rawset("year", valueOf(String.format("%04d", c.get(Calendar.YEAR))));
            t.rawset("month", 1 + c.get(Calendar.MONTH));
            t.rawset("day", c.get(Calendar.DAY_OF_MONTH));
            t.rawset("hour", c.get(Calendar.HOUR_OF_DAY));
            t.rawset("min", c.get(Calendar.MINUTE));
            t.rawset("sec", c.get(Calendar.SECOND));
            t.rawset("wday", c.get(Calendar.DAY_OF_WEEK));
            t.rawset("yday", c.get(Calendar.DAY_OF_YEAR));
            t.rawset("isdst", valueOf(c.get(Calendar.DST_OFFSET) != 0));
            return t;
        } else {
            SimpleDateFormat df = new SimpleDateFormat();
            return valueOf(df.format(date));
        }
    }

    /**
     * Returns the current time when called without arguments, or a time representing the date and time
     * specified by the given table. This table must have fields year, month, and day, and may have fields
     * hour, min, sec, and isdst (for a description of these fields, see the os.date function).
     *
     * @param args <ol>
     *             <li>table
     *             </ol>
     * @return long value for the time
     */
    @LuaBoundFunction
    public Varargs time(Varargs args) {
        // TODO: Support table arg
        return valueOf(System.currentTimeMillis());
    }

    /**
     * Returns the number of seconds from time t1 to time t2. In POSIX, Windows, and some other systems, this
     * value is exactly t2-t1.
     *
     * @return time difference in seconds
     */
    @LuaBoundFunction
    public Varargs difftime(Varargs args) {
        long a = args.checklong(1);
        long b = args.optlong(2, 0);
        return valueOf(b - a);
    }

}
