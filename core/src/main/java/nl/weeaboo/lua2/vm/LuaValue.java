/*******************************************************************************
 * Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/

package nl.weeaboo.lua2.vm;

import static nl.weeaboo.lua2.vm.LuaBoolean.FALSE;
import static nl.weeaboo.lua2.vm.LuaBoolean.TRUE;
import static nl.weeaboo.lua2.vm.LuaConstants.META_ADD;
import static nl.weeaboo.lua2.vm.LuaConstants.META_CALL;
import static nl.weeaboo.lua2.vm.LuaConstants.META_CONCAT;
import static nl.weeaboo.lua2.vm.LuaConstants.META_DIV;
import static nl.weeaboo.lua2.vm.LuaConstants.META_EQ;
import static nl.weeaboo.lua2.vm.LuaConstants.META_INDEX;
import static nl.weeaboo.lua2.vm.LuaConstants.META_LE;
import static nl.weeaboo.lua2.vm.LuaConstants.META_LEN;
import static nl.weeaboo.lua2.vm.LuaConstants.META_LT;
import static nl.weeaboo.lua2.vm.LuaConstants.META_MOD;
import static nl.weeaboo.lua2.vm.LuaConstants.META_MODE;
import static nl.weeaboo.lua2.vm.LuaConstants.META_MUL;
import static nl.weeaboo.lua2.vm.LuaConstants.META_NEWINDEX;
import static nl.weeaboo.lua2.vm.LuaConstants.META_POW;
import static nl.weeaboo.lua2.vm.LuaConstants.META_SUB;
import static nl.weeaboo.lua2.vm.LuaConstants.META_UNM;
import static nl.weeaboo.lua2.vm.LuaConstants.NONE;
import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.compiler.LoadState;
import nl.weeaboo.lua2.stdlib.DebugTrace;

/**
 * Base class for all concrete lua type values.
 * <p>
 * Establishes base implementations for all the operations on lua types. This allows Java clients to deal
 * essentially with one type for all Java values, namely {@link LuaValue}.
 * <p>
 * Constructors are provided as static methods for common Java types, such as {@link LuaValue#valueOf(int)} or
 * {@link LuaValue#valueOf(String)} to allow for instance pooling.
 * <p>
 * Constants are defined for the lua values {@link #NIL}, {@link #TRUE}, and {@link #FALSE}. A constant
 * {@link #NONE} is defined which is a {@link Varargs} list having no values.
 * <p>
 * Operations are performed on values directly via their Java methods. For example, the following code divides
 * two numbers:
 *
 * <pre>
 * {
 *     &#064;code
 *     LuaValue a = LuaValue.valueOf(5);
 *     LuaValue b = LuaValue.valueOf(4);
 *     LuaValue c = a.div(b);
 * }
 * </pre>
 *
 * Note that in this example, c will be a {@link LuaDouble}, but would be a {@link LuaInteger} if the value of
 * a were changed to 8, say. In general the value of c in practice will vary depending on both the types and
 * values of a and b as well as any metatable/metatag processing that occurs.
 * <p>
 * Field access and function calls are similar, with common overloads to simplify Java usage:
 *
 * <pre>
 * {
 *     &#064;code
 *     LuaValue globals = JsePlatform.standardGlobals();
 *     LuaValue sqrt = globals.get(&quot;math&quot;).get(&quot;sqrt&quot;);
 *     LuaValue print = globals.get(&quot;print&quot;);
 *     LuaValue d = sqrt.call(a);
 *     print.call(LuaValue.valueOf(&quot;sqrt(5):&quot;), a);
 * }
 * </pre>
 * <p>
 * To supply variable arguments or get multiple return values, use {@link #invoke(Varargs)}:
 *
 * <pre>
 * {
 *     &#064;code
 *     LuaValue modf = globals.get(&quot;math&quot;).get(&quot;modf&quot;);
 *     Varargs r = modf.invoke(d);
 *     print.call(r.arg(1), r.arg(2));
 * }
 * </pre>
 * <p>
 * To load and run a script, {@link LoadState} is used:
 *
 * <pre>
 * {@code
 * LoadState.load( new FileInputStream("main.lua"), "main.lua", globals ).call();
 * }
 * </pre>
 * <p>
 * although {@code require} could also be used:
 *
 * <pre>
 * {@code
 * globals.get("require").call(LuaValue.valueOf("main"));
 * }
 * </pre>
 *
 * For this to work the file must be in the current directory, or in the class path, depending on the
 * platform.
 * <p>
 * In general a {@link LuaError} may be thrown on any operation when the types supplied to any operation are
 * illegal from a lua perspective. Examples could be attempting to concatenate a NIL value, or attempting
 * arithmetic on values that are not number.
 * <p>
 * There are several methods for preinitializing tables, such as:
 * <ul>
 * <li>{@link #listOf(LuaValue[])} for unnamed elements</li>
 * <li>{@link #tableOf(LuaValue[])} for named elements</li>
 * <li>{@link #tableOf(LuaValue[], LuaValue[], Varargs)} for mixtures</li>
 * </ul>
 * <p>
 * Predefined constants exist for the standard lua type constants {@link LuaConstants#TNIL},
 * {@link LuaConstants#TBOOLEAN}, {@link LuaConstants#TLIGHTUSERDATA}, {@link LuaConstants#TNUMBER},
 * {@link LuaConstants#TSTRING}, {@link LuaConstants#TTABLE}, {@link LuaConstants#TFUNCTION},
 * {@link LuaConstants#TUSERDATA}, {@link LuaConstants#TTHREAD}, and extended lua type constants
 * {@link LuaConstants#TINT}, {@link LuaConstants#TNONE}, {@link LuaConstants#TVALUE}
 * <p>
 * Predefined constants exist for all strings used as metatags: {@link #META_INDEX}, {@link #META_NEWINDEX},
 * {@link #META_CALL}, {@link #META_MODE}, {@link LuaConstants#META_METATABLE}, {@link #META_ADD}, {@link #META_SUB}, {@link #META_DIV},
 * {@link #META_MUL}, {@link #META_POW}, {@link #META_MOD}, {@link #META_UNM}, {@link #META_LEN}, {@link #META_EQ}, {@link #META_LT}, {@link #META_LE}
 * , {@link LuaConstants#META_TOSTRING}, and {@link #META_CONCAT}.
 *
 * @see LoadState
 * @see Varargs
 */
public abstract class LuaValue extends Varargs implements IArith, IComparable {

    /**
     * Get the enumeration value for the type of this value.
     *
     * @return value for this type, one of {@link LuaConstants#TNIL}, {@link LuaConstants#TBOOLEAN},
     *         {@link LuaConstants#TNUMBER}, {@link LuaConstants#TSTRING}, {@link LuaConstants#TTABLE},
     *         {@link LuaConstants#TFUNCTION}, {@link LuaConstants#TUSERDATA}, {@link LuaConstants#TTHREAD}
     * @see #typename()
     */
    public abstract int type();

    /**
     * Get the String name of the type of this value.
     *
     * @return Human-readable type name of this value.
     * @see #type()
     */
    public abstract String typename();

    /**
     * Return true if this is a valid key in a table index operation.
     *
     * @return true if valid as a table key, otherwise false
     * @see #isnil()
     * @see #isinttype()
     */
    public boolean isvalidkey() {
        return true;
    }

    /**
     * Check if {@code this} is a {@code boolean}.
     *
     * @return true if this is a {@code boolean}, otherwise false.
     */
    public boolean isboolean() {
        return false;
    }

    /**
     * Check if {@code this} is a {@code function} that is a closure, meaning interprets lua bytecode for its
     * execution.
     *
     * @return true if this is a {@code closure}, otherwise false
     */
    public boolean isclosure() {
        return false;
    }

    /**
     * Check if {@code this} is a {@code function}.
     *
     * @return true if this is a {@code function}, otherwise false
     */
    public boolean isfunction() {
        return false;
    }

    /**
     * Check if {@code this} is a {@code number} and is representable by java int without rounding or truncation.
     *
     * @return true if this is a {@code number} meaning derives from {@link LuaNumber} or derives from
     *         {@link LuaString} and is convertible to a number, and can be represented by int, otherwise
     *         false
     * @see #isinttype()
     * @see #islong()
     */
    public boolean isint() {
        return false;
    }

    /**
     * Check if {@code this} is a {@link LuaInteger}
     * <p>
     * No attempt to convert from string will be made by this call.
     *
     * @return true if this is a {@code LuaInteger}, otherwise false
     * @see #isint()
     */
    public boolean isinttype() {
        return false;
    }

    /**
     * Check if {@code this} is a {@code number} and is representable by java long without rounding or truncation.
     *
     * @return true if this is a {@code number} meaning derives from {@link LuaNumber} or derives from {@link LuaString}
     *         and is convertible to a number, and can be represented by long, otherwise false
     */
    public boolean islong() {
        return false;
    }

    /**
     * Check if {@code this} is {@code nil}.
     *
     * @return true if this is {@code nil}, otherwise false
     */
    public boolean isnil() {
        return false;
    }

    /**
     * Check if {@code this} is a {@code number}.
     *
     * @return true if this is a {@code number}, meaning derives from {@link LuaNumber} or derives from
     *         {@link LuaString} and is convertible to a number, otherwise false
     */
    public boolean isnumber() {
        return false;
    } // may convert from string

    /**
     * Check if {@code this} is a {@code string}.
     *
     * @return true if this is a {@code string}, meaning derives from {@link LuaString} or {@link LuaNumber},
     *         otherwise false
     */
    public boolean isstring() {
        return false;
    }

    /**
     * Check if {@code this} is a {@code thread}.
     *
     * @return true if this is a {@code thread}, otherwise false
     */
    public boolean isthread() {
        return false;
    }

    /**
     * Check if {@code this} is a {@code table}.
     *
     * @return true if this is a {@code table}, otherwise false
     */
    public boolean istable() {
        return false;
    }

    /**
     * Check if {@code this} is a {@code userdata}.
     *
     * @return true if this is a {@code userdata}, otherwise false
     */
    public boolean isuserdata() {
        return false;
    }

    /**
     * Check if {@code this} is a {@code userdata} of type {@code c}.
     *
     * @param c Class to test instance against
     * @return true if this is a {@code userdata} and the instance is assignable to {@code c}, otherwise false
     */
    public boolean isuserdata(Class<?> c) {
        return false;
    }

    /**
     * Convert to boolean false if {@link #NIL} or {@link #FALSE}, true if anything else.
     *
     * @return Value cast to byte if number or string convertible to number, otherwise 0
     */
    public boolean toboolean() {
        return true;
    }

    /**
     * Convert to byte if numeric, or 0 if not.
     *
     * @return Value cast to byte if number or string convertible to number, otherwise 0
     */
    public byte tobyte() {
        return 0;
    }

    /**
     * Convert to char if numeric, or 0 if not.
     *
     * @return Value cast to char if number or string convertible to number, otherwise 0
     */
    public char tochar() {
        return 0;
    }

    /**
     * Convert to double if numeric, or 0 if not.
     *
     * @return Value cast to double if number or string convertible to number, otherwise 0
     */
    public double todouble() {
        return 0;
    }

    /**
     * Convert to float if numeric, or 0 if not.
     *
     * @return Value cast to float if number or string convertible to number, otherwise 0
     */
    public float tofloat() {
        return 0;
    }

    /**
     * Convert to int if numeric, or 0 if not.
     *
     * @return Value cast to int if number or string convertible to number, otherwise 0
     */
    public int toint() {
        return 0;
    }

    /**
     * Convert to long if numeric, or 0 if not.
     *
     * @return Value cast to long if number or string convertible to number, otherwise 0
     */
    public long tolong() {
        return 0;
    }

    /**
     * Convert to short if numeric, or 0 if not.
     *
     * @return Value cast to short if number or string convertible to number, otherwise 0
     */
    public short toshort() {
        return 0;
    }

    /**
     * Convert to human readable String for any type.
     *
     * @return String for use by human readers based on type.
     */
    @Override
    public String tojstring() {
        return typename() + ": " + Integer.toHexString(hashCode());
    }

    /**
     * Convert to userdata instance, or null.
     *
     * @return userdata instance if userdata, or null if not {@link LuaUserdata}
     */
    public Object touserdata() {
        return null;
    }

    /**
     * Convert to userdata instance if specific type, or null.
     *
     * @param c The Java class to cast the userdata value to.
     *
     * @return userdata instance if is a userdata whose instance derives from {@code c}, or null if not
     *         {@link LuaUserdata}
     */
    public <T> T touserdata(Class<T> c) {
        return null;
    }

    /**
     * Convert the value to a human readable string using {@link #tojstring()}
     *
     * @return String value intended to be human readible.
     * @see #tojstring()
     */
    @Override
    public String toString() {
        return tojstring();
    }

    /**
     * Conditionally convert to lua number without throwing errors.
     * <p>
     * In lua all numbers are strings, but not all strings are numbers. This function will return the
     * {@link LuaValue} {@code this} if it is a number or a string convertible to a number, and {@link #NIL}
     * for all other cases.
     * <p>
     * This allows values to be tested for their "numeric-ness" without the penalty of throwing exceptions,
     * nor the cost of converting the type and creating storage for it.
     *
     * @return {@code this} if it is a {@link LuaNumber} or {@link LuaString} that can be converted to a
     *         number, otherwise {@link #NIL}
     */
    public LuaValue tonumber() {
        return NIL;
    }

    /**
     * Conditionally convert to lua string without throwing errors.
     * <p>
     * In lua all numbers are strings, so this function will return the {@link LuaValue} {@code this} if it is
     * a string or number, and {@link #NIL} for all other cases.
     * <p>
     * This allows values to be tested for their "string-ness" without the penalty of throwing exceptions.
     *
     * @return {@code this} if it is a {@link LuaString} or {@link LuaNumber}, otherwise {@link #NIL}
     * @see #tojstring()
     */
    public LuaValue tostring() {
        return NIL;
    }

    /**
     * Check that optional argument is a boolean and return its boolean value
     *
     * @param defval boolean value to return if {@code this} is nil or none
     * @return {@code this} cast to boolean if a {@LuaBoolean}, {@code defval} if nil or none, throws
     *         {@link LuaError} otherwise
     * @throws LuaError if was not a boolean or nil or none.
     */
    public boolean optboolean(boolean defval) {
        argerror("boolean");
        return false;
    }

    /**
     * Check that optional argument is a closure and return as {@link LuaClosure}
     * <p>
     * A {@link LuaClosure} is a {@LuaFunction} that executes lua byteccode.
     *
     * @param defval {@link LuaClosure} to return if {@code this} is nil or none
     * @return {@code this} cast to {@link LuaClosure} if a function, {@code defval} if nil or none, throws
     *         {@link LuaError} otherwise
     * @throws LuaError if was not a closure or nil or none.
     */
    public LuaClosure optclosure(LuaClosure defval) {
        argerror("closure");
        return null;
    }

    /**
     * Check that optional argument is a number or string convertible to number and return as double
     *
     * @param defval double to return if {@code this} is nil or none
     * @return {@code this} cast to double if numeric, {@code defval} if nil or none, throws {@link LuaError}
     *         otherwise
     * @throws LuaError if was not numeric or nil or none.
     * @see #optint(int)
     */
    public double optdouble(double defval) {
        argerror("double");
        return 0;
    }

    /**
     * Check that optional argument is a function and return as {@link LuaFunction}
     * <p>
     * A {@link LuaFunction} may either be a Java function that implements functionality directly in Java, or
     * a {@link LuaClosure} which is a {@link LuaFunction} that executes lua bytecode.
     *
     * @param defval {@link LuaFunction} to return if {@code this} is nil or none
     * @return {@code this} cast to {@link LuaFunction} if a function, {@code defval} if nil or none, throws
     *         {@link LuaError} otherwise
     * @throws LuaError if was not a function or nil or none.
     */
    public LuaFunction optfunction(LuaFunction defval) {
        argerror("function");
        return null;
    }

    /**
     * Check that optional argument is a number or string convertible to number and return as int
     *
     * @param defval int to return if {@code this} is nil or none
     * @return {@code this} cast to int if numeric, {@code defval} if nil or none, throws {@link LuaError}
     *         otherwise
     * @throws LuaError if was not numeric or nil or none.
     * @see #optdouble(double)
     * @see #optlong(long)
     */
    public int optint(int defval) {
        argerror("int");
        return 0;
    }

    /**
     * Check that optional argument is a number or string convertible to number and return as
     * {@link LuaInteger}
     *
     * @param defval {@link LuaInteger} to return if {@code this} is nil or none
     * @return {@code this} converted and wrapped in {@link LuaInteger} if numeric, {@code defval} if nil or
     *         none, throws {@link LuaError} otherwise
     * @throws LuaError if was not numeric or nil or none.
     * @see #optdouble(double)
     * @see #optint(int)
     */
    public LuaInteger optinteger(LuaInteger defval) {
        argerror("integer");
        return null;
    }

    /**
     * Check that optional argument is a number or string convertible to number and return as long
     *
     * @param defval long to return if {@code this} is nil or none
     * @return {@code this} cast to long if numeric, {@code defval} if nil or none, throws {@link LuaError}
     *         otherwise
     * @throws LuaError if was not numeric or nil or none.
     * @see #optdouble(double)
     * @see #optint(int)
     */
    public long optlong(long defval) {
        argerror("long");
        return 0;
    }

    /**
     * Check that optional argument is a number or string convertible to number and return as
     * {@link LuaNumber}
     *
     * @param defval {@link LuaNumber} to return if {@code this} is nil or none
     * @return {@code this} cast to {@link LuaNumber} if numeric, {@code defval} if nil or none, throws
     *         {@link LuaError} otherwise
     * @throws LuaError if was not numeric or nil or none.
     * @see #optdouble(double)
     * @see #optlong(long)
     * @see #optint(int)
     */
    public LuaNumber optnumber(LuaNumber defval) {
        argerror("number");
        return null;
    }

    /**
     * Check that optional argument is a string or number and return as Java String
     *
     * @param defval {@link LuaString} to return if {@code this} is nil or none
     * @return {@code this} converted to String if a string or number, {@code defval} if nil or none, throws
     *         {@link LuaError} if some other type
     * @throws LuaError if was not a string or number or nil or none.
     */
    public String optjstring(String defval) {
        argerror("String");
        return null;
    }

    /**
     * Check that optional argument is a string or number and return as {@link LuaString}
     *
     * @param defval {@link LuaString} to return if {@code this} is nil or none
     * @return {@code this} converted to {@link LuaString} if a string or number, {@code defval} if nil or
     *         none, throws {@link LuaError} if some other type
     * @throws LuaError if was not a string or number or nil or none.
     */
    public LuaString optstring(LuaString defval) {
        argerror("string");
        return null;
    }

    /**
     * Check that optional argument is a table and return as {@link LuaTable}
     *
     * @param defval {@link LuaTable} to return if {@code this} is nil or none
     * @return {@code this} cast to {@link LuaTable} if a table, {@code defval} if nil or none, throws
     *         {@link LuaError} if some other type
     * @throws LuaError if was not a table or nil or none.
     */
    public LuaTable opttable(LuaTable defval) {
        argerror("table");
        return null;
    }

    /**
     * Check that optional argument is a thread and return as {@link LuaThread}
     *
     * @param defval {@link LuaThread} to return if {@code this} is nil or none
     * @return {@code this} cast to {@link LuaTable} if a thread, {@code defval} if nil or none, throws
     *         {@link LuaError} if some other type
     * @throws LuaError if was not a thread or nil or none.
     */
    public LuaThread optthread(LuaThread defval) {
        argerror("thread");
        return null;
    }

    /**
     * Check that optional argument is a userdata and return the Object instance
     *
     * @param defval Object to return if {@code this} is nil or none
     * @return Object instance of the userdata if a {@link LuaUserdata}, {@code defval} if nil or none, throws
     *         {@link LuaError} if some other type
     * @throws LuaError if was not a userdata or nil or none.
     */
    public Object optuserdata(Object defval) {
        argerror("object");
        return null;
    }

    /**
     * Check that optional argument is a userdata whose instance is of a type and return the Object instance
     *
     * @param c Class to test userdata instance against
     * @param defval Object to return if {@code this} is nil or none
     * @return Object instance of the userdata if a {@link LuaUserdata} and instance is assignable to
     *         {@code c}, {@code defval} if nil or none, throws {@link LuaError} if some other type
     * @throws LuaError if was not a userdata whose instance is assignable to {@code c} or nil or none.
     */
    public <T> T optuserdata(Class<T> c, T defval) {
        argerror(c.getName());
        return null;
    }

    /**
     * Perform argument check that this is not nil or none.
     *
     * @param defval {@link LuaValue} to return if {@code this} is nil or none
     * @return {@code this} if not nil or none, else {@code defval}
     */
    public LuaValue optvalue(LuaValue defval) {
        return this;
    }

    /**
     * Check that the value is a {@link LuaBoolean}, or throw {@link LuaError} if not.
     *
     * @return boolean value for {@code this} if it is a {@link LuaBoolean}
     * @throws LuaError if not a {@link LuaBoolean}
     */
    public boolean checkboolean() {
        argerror("boolean");
        return false;
    }

    /**
     * Check that the value is a {@link LuaClosure} , or throw {@link LuaError} if not
     * <p>
     * {@link LuaClosure} is a subclass of {@LuaFunction} that interprets lua bytecode.
     *
     * @return {@code this} cast as {@link LuaClosure}
     * @throws LuaError if not a {@link LuaClosure}
     */
    public LuaClosure checkclosure() {
        argerror("closure");
        return null;
    }

    /**
     * Check that the value is numeric and return the value as a double, or throw {@link LuaError} if not
     * numeric
     * <p>
     * Values that are {@link LuaNumber} and values that are {@link LuaString} that can be converted to a
     * number will be converted to double.
     *
     * @return value cast to a double if numeric
     * @throws LuaError if not a {@link LuaNumber} or is a {@link LuaString} that can't be converted to number
     * @see #checkint()
     * @see #checklong()
     */
    public double checkdouble() {
        argerror("double");
        return 0;
    }

    /**
     * Check that the value is a function , or throw {@link LuaError} if not
     * <p>
     * A function is considered anything whose {@link #type()} returns {@link LuaConstants#TFUNCTION}. In
     * practice it will be either a built-in Java function, typically deriving from {@link LuaFunction} or a
     * {@link LuaClosure} which represents lua source compiled into lua bytecode.
     *
     * @return {@code this} if if a lua function or closure
     * @throws LuaError if not a function
     * @see #checkclosure()
     */
    public LuaFunction checkfunction() {
        argerror("function");
        return null;
    }

    /**
     * Check that the value is numeric, and convert and cast value to int, or throw {@link LuaError} if not
     * numeric
     * <p>
     * Values that are {@link LuaNumber} will be cast to int and may lose precision. Values that are
     * {@link LuaString} that can be converted to a number will be converted, then cast to int, so may also
     * lose precision.
     *
     * @return value cast to a int if numeric
     * @throws LuaError if not a {@link LuaNumber} or is a {@link LuaString} that can't be converted to number
     */
    public int checkint() {
        argerror("int");
        return 0;
    }

    /**
     * Check that the value is numeric, and convert and cast value to int, or throw {@link LuaError} if not
     * numeric
     * <p>
     * Values that are {@link LuaNumber} will be cast to int and may lose precision. Values that are
     * {@link LuaString} that can be converted to a number will be converted, then cast to int, so may also
     * lose precision.
     *
     * @return value cast to a int and wrapped in {@link LuaInteger} if numeric
     * @throws LuaError if not a {@link LuaNumber} or is a {@link LuaString} that can't be converted to number
     */
    public LuaInteger checkinteger() {
        argerror("integer");
        return null;
    }

    /**
     * Check that the value is numeric, and convert and cast value to long, or throw {@link LuaError} if not
     * numeric
     * <p>
     * Values that are {@link LuaNumber} will be cast to long and may lose precision. Values that are
     * {@link LuaString} that can be converted to a number will be converted, then cast to long, so may also
     * lose precision.
     *
     * @return value cast to a long if numeric
     * @throws LuaError if not a {@link LuaNumber} or is a {@link LuaString} that can't be converted to number
     */
    public long checklong() {
        argerror("long");
        return 0;
    }

    /**
     * Check that the value is numeric, and return as a LuaNumber if so, or throw {@link LuaError}
     * <p>
     * Values that are {@link LuaString} that can be converted to a number will be converted and returned.
     *
     * @return value as a {@link LuaNumber} if numeric
     * @throws LuaError if not a {@link LuaNumber} or is a {@link LuaString} that can't be converted to number
     */
    public LuaNumber checknumber() {
        argerror("number");
        return null;
    }

    /**
     * Check that the value is numeric, and return as a LuaNumber if so, or throw {@link LuaError}
     * <p>
     * Values that are {@link LuaString} that can be converted to a number will be converted and returned.
     *
     * @param msg String message to supply if conversion fails
     * @return value as a {@link LuaNumber} if numeric
     * @throws LuaError if not a {@link LuaNumber} or is a {@link LuaString} that can't be converted to number
     */
    public LuaNumber checknumber(String msg) {
        throw new LuaError(msg);
    }

    /**
     * Convert this value to a Java String.
     * <p>
     * The string representations here will roughly match what is produced by the C lua distribution, however
     * hash codes have no relationship, and there may be differences in number formatting.
     *
     * @return String representation of the value
     */
    public String checkjstring() {
        argerror("string");
        return null;
    }

    /**
     * Check that this is a lua string, or throw {@link LuaError} if it is not.
     * <p>
     * In lua all numbers are strings, so this will succeed for anything that derives from {@link LuaString}
     * or {@link LuaNumber}. Numbers will be converted to {@link LuaString}.
     *
     * @return {@link LuaString} representation of the value if it is a {@link LuaString} or {@link LuaNumber}
     * @throws LuaError if {@code this} is not a {@link LuaTable}
     */
    public LuaString checkstring() {
        argerror("string");
        return null;
    }

    /**
     * Check that this is a {@link LuaTable}, or throw {@link LuaError} if it is not.
     *
     * @return {@code this} if it is a {@link LuaTable}
     * @throws LuaError if {@code this} is not a {@link LuaTable}
     */
    public LuaTable checktable() {
        argerror("table");
        return null;
    }

    /**
     * Check that this is a {@link LuaThread}, or throw {@link LuaError} if it is not.
     *
     * @return {@code this} if it is a {@link LuaThread}
     * @throws LuaError if {@code this} is not a {@link LuaThread}
     */
    public LuaThread checkthread() {
        argerror("thread");
        return null;
    }

    /**
     * Check that this is a {@link LuaUserdata}, or throw {@link LuaError} if it is not.
     *
     * @return {@code this} if it is a {@link LuaUserdata}
     * @throws LuaError if {@code this} is not a {@link LuaUserdata}
     */
    public Object checkuserdata() {
        argerror("userdata");
        return null;
    }

    /**
     * Check that this is a {@link LuaUserdata}, or throw {@link LuaError} if it is not
     *
     * @param c The Java class to cast the userdata value to.
     *
     * @return {@code this} if it is a {@link LuaUserdata}
     * @throws LuaError if {@code this} is not a {@link LuaUserdata}
     */
    public <T> T checkuserdata(Class<T> c) {
        argerror("userdata");
        return null;
    }

    /**
     * Check that this is not the value {@link #NIL}, or throw {@link LuaError} if it is.
     *
     * @return {@code this} if it is not {@link #NIL}
     * @throws LuaError if {@code this} is {@link #NIL}
     */
    public LuaValue checknotnil() {
        return this;
    }

    /**
     * Throw a {@link LuaError} with a particular message.
     *
     * @param message String providing message details
     * @throws LuaError in all cases
     */
    public static LuaValue error(String message) {
        throw new LuaError(message);
    }

    /**
     * Throw a {@link LuaError} indicating an invalid argument was supplied to a function.
     *
     * @param expected String naming the type that was expected
     * @throws LuaError in all cases
     */
    protected LuaValue argerror(String expected) {
        throw new LuaError("bad argument: " + expected + " expected, got " + debugName() + " (a "
                + typename() + ")");
    }

    /**
     * Throw a {@link LuaError} indicating an invalid argument was supplied to a function.
     *
     * @param iarg index of the argument that was invalid, first index is 1
     * @param msg String providing information about the invalid argument
     * @throws LuaError in all cases
     */
    public static LuaValue argerror(int iarg, String msg) {
        throw new LuaError("bad argument #" + iarg + ": " + msg);
    }

    /**
     * Throw a {@link LuaError} indicating an invalid type was supplied to a function.
     *
     * @param expected String naming the type that was expected
     * @throws LuaError in all cases
     */
    protected LuaValue typerror(String expected) {
        throw new LuaError(expected + " expected, got " + debugName());
    }

    /**
     * Throw a {@link LuaError} indicating an operation is not implemented.
     *
     * @throws LuaError in all cases
     */
    protected LuaValue unimplemented(String fun) {
        throw new LuaError("'" + fun + "' not implemented for " + debugName());
    }

    /**
     * Throw a {@link LuaError} indicating an illegal operation occurred, typically involved in managing weak
     * references.
     *
     * @throws LuaError in all cases
     */
    protected LuaValue illegal(String op, String typename) {
        throw new LuaError("illegal operation '" + op + "' for " + typename);
    }

    /**
     * Throw a {@link LuaError} based on the len operator, typically due to an invalid operand type.
     *
     * @throws LuaError in all cases
     */
    protected LuaValue lenerror() {
        throw new LuaError("attempt to get length of " + debugName());
    }

    /**
     * Throw a {@link LuaError} based on an arithmetic error such as add, or pow, typically due to an invalid
     * operand type.
     *
     * @throws LuaError in all cases
     */
    protected LuaValue aritherror() {
        throw new LuaError("attempt to perform arithmetic on " + debugName());
    }

    /**
     * Throw a {@link LuaError} based on an arithmetic error such as add, or pow, typically due to an invalid
     * operand type.
     *
     * @param fun String description of the function that was attempted
     * @throws LuaError in all cases
     */
    protected LuaValue aritherror(String fun) {
        throw new LuaError("attempt to perform arithmetic '" + fun + "' on " + debugName());
    }

    /**
     * Throw a {@link LuaError} based on a comparison error such as greater-than or less-than, typically due
     * to an invalid operand type
     *
     * @param rhs String description of what was on the right-hand-side of the comparison that resulted in the
     *        error.
     * @throws LuaError in all cases
     */
    protected LuaValue compareerror(String rhs) {
        throw new LuaError("attempt to compare " + debugName() + " with " + rhs);
    }

    /**
     * Throw a {@link LuaError} based on a comparison error such as greater-than or less-than, typically due
     * to an invalid operand type
     *
     * @param rhs Right-hand-side of the comparison that resulted in the error.
     * @throws LuaError in all cases
     */
    protected LuaValue compareerror(LuaValue rhs) {
        throw new LuaError("attempt to compare " + debugName() + " with " + rhs.debugName());
    }

    /**
     * Get a value in a table including metatag processing using {@link #META_INDEX}.
     *
     * @param key the key to look up, must not be {@link #NIL} or null
     * @return {@link LuaValue} for that key, or {@link #NIL} if not found and no metatag
     * @throws LuaError if {@code this} is not a table, or there is no {@link #META_INDEX} metatag, or key is
     *         {@link #NIL}
     * @see #get(int)
     * @see #get(String)
     * @see #rawget(LuaValue)
     */
    public LuaValue get(LuaValue key) {
        return gettable(this, key);
    }

    /**
     * Get a value in a table including metatag processing using {@link #META_INDEX}.
     *
     * @param key the key to look up
     * @return {@link LuaValue} for that key, or {@link #NIL} if not found
     * @throws LuaError if {@code this} is not a table, or there is no {@link #META_INDEX} metatag
     * @see #get(LuaValue)
     * @see #rawget(int)
     */
    public LuaValue get(int key) {
        return get(LuaInteger.valueOf(key));
    }

    /**
     * Get a value in a table including metatag processing using {@link #META_INDEX}.
     *
     * @param key the key to look up, must not be null
     * @return {@link LuaValue} for that key, or {@link #NIL} if not found
     * @throws LuaError if {@code this} is not a table, or there is no {@link #META_INDEX} metatag
     * @see #get(LuaValue)
     * @see #rawget(String)
     */
    public LuaValue get(String key) {
        return get(valueOf(key));
    }

    /**
     * Set a value in a table without metatag processing using {@link #META_NEWINDEX}.
     *
     * @param key the key to use, must not be {@link #NIL} or null
     * @param value the value to use, can be {@link #NIL}, must not be null
     * @throws LuaError if {@code this} is not a table, or key is {@link #NIL}, or there is no
     *         {@link #META_NEWINDEX} metatag
     */
    public void set(LuaValue key, LuaValue value) {
        settable(this, key, value);
    }

    /**
     * Set a value in a table without metatag processing using {@link #META_NEWINDEX}.
     *
     * @param key the key to use
     * @param value the value to use, can be {@link #NIL}, must not be null
     * @throws LuaError if {@code this} is not a table, or there is no {@link #META_NEWINDEX} metatag
     */
    public void set(int key, LuaValue value) {
        set(LuaInteger.valueOf(key), value);
    }

    /**
     * Set a value in a table without metatag processing using {@link #META_NEWINDEX}.
     *
     * @param key the key to use
     * @param value the value to use, must not be null
     * @throws LuaError if {@code this} is not a table, or there is no {@link #META_NEWINDEX} metatag
     */
    public void set(int key, String value) {
        set(key, valueOf(value));
    }

    /**
     * Set a value in a table without metatag processing using {@link #META_NEWINDEX}.
     *
     * @param key the key to use, must not be {@link #NIL} or null
     * @param value the value to use, can be {@link #NIL}, must not be null
     * @throws LuaError if {@code this} is not a table, or there is no {@link #META_NEWINDEX} metatag
     */
    public void set(String key, LuaValue value) {
        set(valueOf(key), value);
    }

    /**
     * Set a value in a table without metatag processing using {@link #META_NEWINDEX}.
     *
     * @param key the key to use, must not be null
     * @param value the value to use
     * @throws LuaError if {@code this} is not a table, or there is no {@link #META_NEWINDEX} metatag
     */
    public void set(String key, double value) {
        set(valueOf(key), valueOf(value));
    }

    /**
     * Set a value in a table without metatag processing using {@link #META_NEWINDEX}.
     *
     * @param key the key to use, must not be null
     * @param value the value to use
     * @throws LuaError if {@code this} is not a table, or there is no {@link #META_NEWINDEX} metatag
     */
    public void set(String key, int value) {
        set(valueOf(key), valueOf(value));
    }

    /**
     * Set a value in a table without metatag processing using {@link #META_NEWINDEX}.
     *
     * @param key the key to use, must not be null
     * @param value the value to use, must not be null
     * @throws LuaError if {@code this} is not a table, or there is no {@link #META_NEWINDEX} metatag
     */
    public void set(String key, String value) {
        set(valueOf(key), valueOf(value));
    }

    /**
     * Get a value in a table without metatag processing.
     *
     * @param key the key to look up, must not be {@link #NIL} or null
     * @return {@link LuaValue} for that key, or {@link #NIL} if not found
     * @throws LuaError if {@code this} is not a table, or key is {@link #NIL}
     */
    public LuaValue rawget(LuaValue key) {
        return unimplemented("rawget");
    }

    /**
     * Get a value in a table without metatag processing.
     *
     * @param key the key to look up
     * @return {@link LuaValue} for that key, or {@link #NIL} if not found
     * @throws LuaError if {@code this} is not a table
     */
    public LuaValue rawget(int key) {
        return rawget(valueOf(key));
    }

    /**
     * Get a value in a table without metatag processing.
     *
     * @param key the key to look up, must not be null
     * @return {@link LuaValue} for that key, or {@link #NIL} if not found
     * @throws LuaError if {@code this} is not a table
     */
    public LuaValue rawget(String key) {
        return rawget(valueOf(key));
    }

    /**
     * Set a value in a table without metatag processing.
     *
     * @param key the key to use, must not be {@link #NIL} or null
     * @param value the value to use, can be {@link #NIL}, must not be null
     * @throws LuaError if {@code this} is not a table, or key is {@link #NIL}
     */
    public void rawset(LuaValue key, LuaValue value) {
        unimplemented("rawset");
    }

    /**
     * Set a value in a table without metatag processing.
     *
     * @param key the key to use
     * @param value the value to use, can be {@link #NIL}, must not be null
     * @throws LuaError if {@code this} is not a table
     */
    public void rawset(int key, LuaValue value) {
        rawset(valueOf(key), value);
    }

    /**
     * Set a value in a table without metatag processing.
     *
     * @param key the key to use
     * @param value the value to use, can be {@link #NIL}, must not be null
     * @throws LuaError if {@code this} is not a table
     */
    public void rawset(int key, String value) {
        rawset(key, valueOf(value));
    }

    /**
     * Set a value in a table without metatag processing.
     *
     * @param key the key to use, must not be null
     * @param value the value to use, can be {@link #NIL}, must not be null
     * @throws LuaError if {@code this} is not a table
     */
    public void rawset(String key, LuaValue value) {
        rawset(valueOf(key), value);
    }

    /**
     * Set a value in a table without metatag processing.
     *
     * @param key the key to use, must not be null
     * @param value the value to use
     * @throws LuaError if {@code this} is not a table
     */
    public void rawset(String key, double value) {
        rawset(valueOf(key), valueOf(value));
    }

    /**
     * Set a value in a table without metatag processing.
     *
     * @param key the key to use, must not be null
     * @param value the value to use
     * @throws LuaError if {@code this} is not a table
     */
    public void rawset(String key, int value) {
        rawset(valueOf(key), valueOf(value));
    }

    /**
     * Set a value in a table without metatag processing.
     *
     * @param key the key to use, must not be null
     * @param value the value to use, must not be null
     * @throws LuaError if {@code this} is not a table
     */
    public void rawset(String key, String value) {
        rawset(valueOf(key), valueOf(value));
    }

    /**
     * Set list values in a table without invoking metatag processing
     * <p>
     * Primarily used internally in response to a SETLIST bytecode.
     *
     * @param key0 the first key to set in the table
     * @param values the list of values to set
     * @throws LuaError if this is not a table.
     */
    public void rawsetlist(int key0, Varargs values) {
        for (int i = 0, n = values.narg(); i < n; i++) {
            rawset(key0 + i, values.arg(i + 1));
        }
    }

    /**
     * Preallocate the array part of a table to be a certain size,
     * <p>
     * Primarily used internally in response to a SETLIST bytecode.
     *
     * @param i the number of array slots to preallocate in the table.
     * @throws LuaError if this is not a table.
     */
    public void presize(int i) {
        typerror("table");
    }

    /**
     * Find the next key,value pair if {@code this} is a table, return {@link #NIL} if there are no more, or
     * throw a {@link LuaError} if not a table.
     * <p>
     * To iterate over all key-value pairs in a table you can use
     *
     * <pre>
     * <code>
     * LuaValue k = LuaValue.NIL;
     * while ( true ) {
     *    Varargs n = table.next(k);
     *    if ( (k = n.arg1()).isnil() )
     *       break;
     *    LuaValue v = n.arg(2)
     *    process( k, v )
     * }
     * </code>
     * </pre>
     *
     * @param index {@link LuaInteger} value identifying a key to start from, or {@link #NIL} to start at the
     *        beginning
     * @return {@link Varargs} containing {key,value} for the next entry, or {@link #NIL} if there are no
     *         more.
     * @throws LuaError if {@code this} is not a table, or the supplied key is invalid.
     * @see LuaTable
     * @see #inext(LuaValue)
     * @see #valueOf(int)
     * @see Varargs#arg1()
     * @see Varargs#arg(int)
     * @see #isnil()
     */
    public Varargs next(LuaValue index) {
        return typerror("table");
    }

    /**
     * Find the next integer-key,value pair if {@code this} is a table, return {@link #NIL} if there are no
     * more, or throw a {@link LuaError} if not a table.
     * <p>
     * To iterate over integer keys in a table you can use
     *
     * <pre>
     * <code>
     *   LuaValue k = LuaValue.NIL;
     *   while ( true ) {
     *      Varargs n = table.inext(k);
     *      if ( (k = n.arg1()).isnil() )
     *         break;
     *      LuaValue v = n.arg(2)
     *      process( k, v )
     *   }
     * </code>
     * </pre>
     *
     * @param index {@link LuaInteger} value identifying a key to start from, or {@link #NIL} to start at the
     *        beginning
     * @return {@link Varargs} containing {@code (key,value)} for the next entry, or {@link #NONE} if there
     *         are no more.
     * @throws LuaError if {@code this} is not a table, or the supplied key is invalid.
     * @see LuaTable
     * @see #next(LuaValue)
     * @see #valueOf(int)
     * @see Varargs#arg1()
     * @see Varargs#arg(int)
     * @see #isnil()
     */
    public Varargs inext(LuaValue index) {
        return typerror("table");
    }

    /**
     * Load a library instance by setting its environment to {@code this} and calling it, which should
     * iniitalize the library instance and install itself into this instance.
     *
     * @param library The callable {@link LuaValue} to load into {@code this}
     * @return {@link LuaValue} containing the result of the initialization call.
     */
    @Deprecated
    public LuaValue load(LuaValue library) {
        library.setfenv(this);
        return library.call();
    }

    // varargs references
    @Override
    public LuaValue arg(int index) {
        return index == 1 ? this : NIL;
    }

    @Override
    public int narg() {
        return 1;
    }

    @Override
    public LuaValue arg1() {
        return this;
    }

    /**
     * Get the metatable for this {@link LuaValue}
     * <p>
     * For {@link LuaTable} and {@link LuaUserdata} instances, the metatable returned is this instance
     * metatable. For all other types, the class metatable value will be returned.
     *
     * @return metatable, or null if it there is none
     * @see LuaBoolean#s_metatable
     * @see LuaNumber#s_metatable
     * @see LuaNil#s_metatable
     * @see LuaFunction#s_metatable
     * @see LuaThread#s_metatable
     */
    public LuaValue getmetatable() {
        return null;
    }

    /**
     * Set the metatable for this {@link LuaValue}
     * <p>
     * For {@link LuaTable} and {@link LuaUserdata} instances, the metatable is per instance. For all other
     * types, there is one metatable per type that can be set directly from java
     *
     * @param metatable {@link LuaValue} instance to serve as the metatable, or null to reset it.
     * @return {@code this} to allow chaining of Java function calls
     * @see LuaBoolean#s_metatable
     * @see LuaNumber#s_metatable
     * @see LuaNil#s_metatable
     * @see LuaFunction#s_metatable
     * @see LuaThread#s_metatable
     */
    public LuaValue setmetatable(LuaValue metatable) {
        return argerror("table");
    }

    /**
     * Get the environemnt for an instance.
     *
     * @return {@link LuaValue} currently set as the instances environent.
     */
    public LuaValue getfenv() {
        typerror("function or thread");
        return null;
    }

    /**
     * Set the environment on an object.
     * <p>
     * Typically the environment is created once per application via a platform helper method such as
     * {@link LuaRunState}. However, any object can serve as an environment if it contains suitable metatag
     * values to implement {@link #get(LuaValue)} to provide the environment values.
     *
     * @param env {@link LuaValue} (typically a {@link LuaTable}) containing the environment.
     */
    public void setfenv(LuaValue env) {
        typerror("function or thread");
    }

    /**
     * Call this with 0 arguments, including metatag processing, and return only the first return value.
     * <p>
     * If {@code this} is a {@link LuaFunction}, call it, and return only its first return value, dropping any
     * others. Otherwise, look for the {@link #META_CALL} metatag and call that.
     * <p>
     * If the return value is a {@link Varargs}, only the 1st value will be returned. To get multiple values,
     * use {@link #invoke()} instead.
     *
     * @return First return value {@code (this())}, or {@link #NIL} if there were none.
     * @throws LuaError if not a function and {@link #META_CALL} is not defined, or the invoked function throws a
     *         {@link LuaError} or the invoked closure throw a lua {@code error}
     * @see #call(LuaValue)
     * @see #call(LuaValue,LuaValue)
     * @see #call(LuaValue, LuaValue, LuaValue)
     * @see #invoke()
     */
    public LuaValue call() {
        return callmt().call(this);
    }

    /**
     * Call this with 1 argument, including metatag processing, and return only the first return value.
     * <p>
     * If {@code this} is a {@link LuaFunction}, call it, and return only its first return value, dropping any
     * others. Otherwise, look for the {@link #META_CALL} metatag and call that.
     * <p>
     * If the return value is a {@link Varargs}, only the 1st value will be returned. To get multiple values,
     * use {@link #invoke()} instead.
     *
     * @param arg First argument to supply to the called function
     * @return First return value {@code (this(arg))}, or {@link #NIL} if there were none.
     * @throws LuaError if not a function and {@link #META_CALL} is not defined, or the invoked function throws a
     *         {@link LuaError} or the invoked closure throw a lua {@code error}
     * @see #call()
     * @see #call(LuaValue,LuaValue)
     * @see #call(LuaValue, LuaValue, LuaValue)
     * @see #invoke(LuaValue, Varargs)
     */
    public LuaValue call(LuaValue arg) {
        return callmt().call(this, arg);
    }

    /**
     * Call this with 2 arguments, including metatag processing, and return only the first return value.
     * <p>
     * If {@code this} is a {@link LuaFunction}, call it, and return only its first return value, dropping any
     * others. Otherwise, look for the {@link #META_CALL} metatag and call that.
     * <p>
     * If the return value is a {@link Varargs}, only the 1st value will be returned. To get multiple values,
     * use {@link #invoke()} instead.
     *
     * @param arg1 First argument to supply to the called function
     * @param arg2 Second argument to supply to the called function
     * @return First return value {@code (this(arg1,arg2))}, or {@link #NIL} if there were none.
     * @throws LuaError if not a function and {@link #META_CALL} is not defined, or the invoked function throws a
     *         {@link LuaError} or the invoked closure throw a lua {@code error}
     * @see #call()
     * @see #call(LuaValue)
     * @see #call(LuaValue, LuaValue, LuaValue)
     * @see #invoke(LuaValue, LuaValue, Varargs)
     */
    public LuaValue call(LuaValue arg1, LuaValue arg2) {
        return callmt().call(this, arg1, arg2);
    }

    /**
     * Call this with 3 arguments, including metatag processing, and return only the first return value.
     * <p>
     * If {@code this} is a {@link LuaFunction}, call it, and return only its first return value, dropping any
     * others. Otherwise, look for the {@link #META_CALL} metatag and call that.
     * <p>
     * If the return value is a {@link Varargs}, only the 1st value will be returned. To get multiple values,
     * use {@link #invoke()} instead.
     *
     * @param arg1 First argument to supply to the called function
     * @param arg2 Second argument to supply to the called function
     * @param arg3 Second argument to supply to the called function
     * @return First return value {@code (this(arg1,arg2,arg3))}, or {@link #NIL} if there were none.
     * @throws LuaError if not a function and {@link #META_CALL} is not defined, or the invoked function throws a
     *         {@link LuaError} or the invoked closure throw a lua {@code error}
     * @see #call()
     * @see #call(LuaValue)
     * @see #call(LuaValue, LuaValue)
     * @see #invoke(Varargs)
     */
    public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
        return callmt().invoke(new LuaValue[] { this, arg1, arg2, arg3 }).arg1();
    }

    /**
     * Call named method on this with 0 arguments, including metatag processing, and return only the first
     * return value.
     * <p>
     * Look up {@code this[name]} and if it is a {@link LuaFunction}, call it inserting this as an additional
     * first argument. and return only its first return value, dropping any others. Otherwise, look for the
     * {@link #META_CALL} metatag and call that.
     * <p>
     * If the return value is a {@link Varargs}, only the 1st value will be returned. To get multiple values,
     * use {@link #invoke()} instead.
     * <p>
     * To call this as a plain call, use {@link #call()} instead.
     *
     * @param name Name of the method to look up for invocation
     * @return All values returned from {@code this:name()} as a {@link Varargs} instance
     * @throws LuaError if not a function and {@link #META_CALL} is not defined, or the invoked function throws a
     *         {@link LuaError} or the invoked closure throw a lua {@code error}
     * @see #call()
     * @see #invoke()
     * @see #method(LuaValue)
     * @see #method(String,LuaValue)
     * @see #method(String,LuaValue,LuaValue)
     */
    public LuaValue method(String name) {
        return this.get(name).call(this);
    }

    /**
     * Call named method on this with 0 arguments, including metatag processing, and return only the first
     * return value.
     * <p>
     * Look up {@code this[name]} and if it is a {@link LuaFunction}, call it inserting this as an additional
     * first argument, and return only its first return value, dropping any others. Otherwise, look for the
     * {@link #META_CALL} metatag and call that.
     * <p>
     * If the return value is a {@link Varargs}, only the 1st value will be returned. To get multiple values,
     * use {@link #invoke()} instead.
     * <p>
     * To call this as a plain call, use {@link #call()} instead.
     *
     * @param name Name of the method to look up for invocation
     * @return All values returned from {@code this:name()} as a {@link Varargs} instance
     * @throws LuaError if not a function and {@link #META_CALL} is not defined, or the invoked function throws a
     *         {@link LuaError} or the invoked closure throw a lua {@code error}
     * @see #call()
     * @see #invoke()
     * @see #method(String)
     * @see #method(LuaValue,LuaValue)
     * @see #method(LuaValue,LuaValue,LuaValue)
     */
    public LuaValue method(LuaValue name) {
        return this.get(name).call(this);
    }

    /**
     * Call named method on this with 1 argument, including metatag processing, and return only the first
     * return value.
     * <p>
     * Look up <code>this[name]</code> and if it is a {@link LuaFunction}, call it inserting this as an
     * additional first argument, and return only its first return value, dropping any others. Otherwise, look
     * for the {@link #META_CALL} metatag and call that.
     * <p>
     * If the return value is a {@link Varargs}, only the 1st value will be returned. To get multiple values,
     * use {@link #invoke()} instead.
     * <p>
     * To call this as a plain call, use {@link #call(LuaValue)} instead.
     *
     * @param name Name of the method to look up for invocation
     * @param arg Argument to supply to the method
     * @return All values returned from {@code this:name(arg)} as a {@link Varargs} instance
     * @throws LuaError if not a function and {@link #META_CALL} is not defined, or the invoked function throws a
     *         {@link LuaError} or the invoked closure throw a lua {@code error}
     * @see #call(LuaValue)
     * @see #invoke(LuaValue, Varargs)
     * @see #method(LuaValue,LuaValue)
     * @see #method(String)
     * @see #method(String,LuaValue,LuaValue)
     */
    public LuaValue method(String name, LuaValue arg) {
        return this.get(name).call(this, arg);
    }

    /**
     * Call named method on thiswith 1 argument, including metatag processing, and return only the first
     * return value.
     * <p>
     * Look up <code>this[name]</code> and if it is a {@link LuaFunction}, call it inserting this as an
     * additional first argument, and return only its first return value, dropping any others. Otherwise, look
     * for the {@link #META_CALL} metatag and call that.
     * <p>
     * If the return value is a {@link Varargs}, only the 1st value will be returned. To get multiple values,
     * use {@link #invoke()} instead.
     * <p>
     * To call this as a plain call, use {@link #call(LuaValue)} instead.
     *
     * @param name Name of the method to look up for invocation
     * @param arg Argument to supply to the method
     * @return All values returned from {@code this:name(arg)} as a {@link Varargs} instance
     * @throws LuaError if not a function and {@link #META_CALL} is not defined, or the invoked function throws a
     *         {@link LuaError} or the invoked closure throw a lua {@code error}
     * @see #call(LuaValue)
     * @see #invoke(LuaValue, Varargs)
     * @see #method(String,LuaValue)
     * @see #method(LuaValue)
     * @see #method(LuaValue,LuaValue,LuaValue)
     */
    public LuaValue method(LuaValue name, LuaValue arg) {
        return this.get(name).call(this, arg);
    }

    /**
     * Call named method on this with 2 arguments, including metatag processing, and return only the first
     * return value.
     * <p>
     * Look up <code>this[name]</codE> and if it is a {@link LuaFunction}, call it inserting this as an
     * additional first argument, and return only its first return value, dropping any others. Otherwise, look
     * for the {@link #META_CALL} metatag and call that.
     * <p>
     * If the return value is a {@link Varargs}, only the 1st value will be returned. To get multiple values,
     * use {@link #invoke()} instead.
     * <p>
     * To call this as a plain call, use {@link #call(LuaValue,LuaValue)} instead.
     *
     * @param name Name of the method to look up for invocation
     * @param arg1 First argument to supply to the method
     * @param arg2 Second argument to supply to the method
     * @return All values returned from {@code this:name(arg1,arg2)} as a {@link Varargs} instance
     * @throws LuaError if not a function and {@link #META_CALL} is not defined, or the invoked function throws a
     *         {@link LuaError} or the invoked closure throw a lua {@code error}
     * @see #call(LuaValue,LuaValue)
     * @see #invoke(LuaValue,Varargs)
     * @see #method(String,LuaValue)
     * @see #method(LuaValue,LuaValue,LuaValue)
     */
    public LuaValue method(String name, LuaValue arg1, LuaValue arg2) {
        return this.get(name).call(this, arg1, arg2);
    }

    /**
     * Call named method on this with 2 arguments, including metatag processing, and return only the first
     * return value.
     * <p>
     * Look up {@code this[name]} and if it is a {@link LuaFunction}, call it inserting this as an additional
     * first argument, and return only its first return value, dropping any others. Otherwise, look for the
     * {@link #META_CALL} metatag and call that.
     * <p>
     * If the return value is a {@link Varargs}, only the 1st value will be returned. To get multiple values,
     * use {@link #invoke()} instead.
     * <p>
     * To call this as a plain call, use {@link #call(LuaValue,LuaValue)} instead.
     *
     * @param name Name of the method to look up for invocation
     * @param arg1 First argument to supply to the method
     * @param arg2 Second argument to supply to the method
     * @return All values returned from {@code this:name(arg1,arg2)} as a {@link Varargs} instance
     * @throws LuaError if not a function and {@link #META_CALL} is not defined, or the invoked function throws a
     *         {@link LuaError} or the invoked closure throw a lua {@code error}
     * @see #call(LuaValue,LuaValue)
     * @see #invoke(LuaValue,Varargs)
     * @see #method(LuaValue,LuaValue)
     * @see #method(String,LuaValue,LuaValue)
     */
    public LuaValue method(LuaValue name, LuaValue arg1, LuaValue arg2) {
        return this.get(name).call(this, arg1, arg2);
    }

    /**
     * Call this with 0 arguments, including metatag processing, and retain all return values in a
     * {@link Varargs}.
     * <p>
     * If {@code this} is a {@link LuaFunction}, call it, and return all values. Otherwise, look for the
     * {@link #META_CALL} metatag and call that.
     * <p>
     * To get a particular return value, us {@link Varargs#arg(int)}
     *
     * @return All return values as a {@link Varargs} instance.
     * @throws LuaError if not a function and {@link #META_CALL} is not defined, or the invoked function throws a
     *         {@link LuaError} or the invoked closure throw a lua {@code error}
     * @see #call()
     * @see #invoke(Varargs)
     */
    public Varargs invoke() {
        return invoke(NONE);
    }

    /**
     * Call this with variable arguments, including metatag processing, and retain all return values in a
     * {@link Varargs}.
     * <p>
     * If {@code this} is a {@link LuaFunction}, call it, and return all values. Otherwise, look for the
     * {@link #META_CALL} metatag and call that.
     * <p>
     * To get a particular return value, us {@link Varargs#arg(int)}
     *
     * @param args Varargs containing the arguments to supply to the called function
     * @return All return values as a {@link Varargs} instance.
     * @throws LuaError if not a function and {@link #META_CALL} is not defined, or the invoked function throws a
     *         {@link LuaError} or the invoked closure throw a lua {@code error}
     * @see #varargsOf(LuaValue[])
     * @see #call(LuaValue)
     * @see #invoke()
     * @see #invoke(LuaValue,Varargs)
     */
    public Varargs invoke(Varargs args) {
        return callmt().invoke(this, args);
    }

    /**
     * Call this with variable arguments, including metatag processing, and retain all return values in a
     * {@link Varargs}.
     * <p>
     * If {@code this} is a {@link LuaFunction}, call it, and return all values. Otherwise, look for the
     * {@link #META_CALL} metatag and call that.
     * <p>
     * To get a particular return value, us {@link Varargs#arg(int)}
     *
     * @param arg The first argument to supply to the called function
     * @param varargs Varargs containing the remaining arguments to supply to the called function
     * @return All return values as a {@link Varargs} instance.
     * @throws LuaError if not a function and {@link #META_CALL} is not defined, or the invoked function throws a
     *         {@link LuaError} or the invoked closure throw a lua {@code error}
     * @see #varargsOf(LuaValue[])
     * @see #call(LuaValue,LuaValue)
     * @see #invoke(LuaValue,Varargs)
     */
    public Varargs invoke(LuaValue arg, Varargs varargs) {
        return invoke(varargsOf(arg, varargs));
    }

    /**
     * Call this with variable arguments, including metatag processing, and retain all return values in a
     * {@link Varargs}.
     * <p>
     * If {@code this} is a {@link LuaFunction}, call it, and return all values. Otherwise, look for the
     * {@link #META_CALL} metatag and call that.
     * <p>
     * To get a particular return value, us {@link Varargs#arg(int)}
     *
     * @param arg1 The first argument to supply to the called function
     * @param arg2 The second argument to supply to the called function
     * @param varargs Varargs containing the remaining arguments to supply to the called function
     * @return All return values as a {@link Varargs} instance.
     * @throws LuaError if not a function and {@link #META_CALL} is not defined, or the invoked function throws a
     *         {@link LuaError} or the invoked closure throw a lua {@code error}
     * @see #varargsOf(LuaValue[])
     * @see #call(LuaValue,LuaValue,LuaValue)
     * @see #invoke(LuaValue,LuaValue,Varargs)
     */
    public Varargs invoke(LuaValue arg1, LuaValue arg2, Varargs varargs) {
        return invoke(varargsOf(arg1, arg2, varargs));
    }

    /**
     * Call this with variable arguments, including metatag processing, and retain all return values in a
     * {@link Varargs}.
     * <p>
     * If {@code this} is a {@link LuaFunction}, call it, and return all values. Otherwise, look for the
     * {@link #META_CALL} metatag and call that.
     * <p>
     * To get a particular return value, us {@link Varargs#arg(int)}
     *
     * @param args Array of arguments to supply to the called function
     * @return All return values as a {@link Varargs} instance.
     * @throws LuaError if not a function and {@link #META_CALL} is not defined, or the invoked function throws a
     *         {@link LuaError} or the invoked closure throw a lua {@code error}
     * @see #varargsOf(LuaValue[])
     * @see #call(LuaValue,LuaValue,LuaValue)
     * @see #invoke(LuaValue,LuaValue,Varargs)
     */
    public Varargs invoke(LuaValue[] args) {
        return invoke(varargsOf(args));
    }

    /**
     * Call this with variable arguments, including metatag processing, and retain all return values in a
     * {@link Varargs}.
     * <p>
     * If {@code this} is a {@link LuaFunction}, call it, and return all values. Otherwise, look for the
     * {@link #META_CALL} metatag and call that.
     * <p>
     * To get a particular return value, us {@link Varargs#arg(int)}
     *
     * @param args Array of arguments to supply to the called function
     * @param varargs Varargs containing additional arguments to supply to the called function
     * @return All return values as a {@link Varargs} instance.
     * @throws LuaError if not a function and {@link #META_CALL} is not defined, or the invoked function throws a
     *         {@link LuaError} or the invoked closure throw a lua {@code error}
     * @see #varargsOf(LuaValue[])
     * @see #call(LuaValue,LuaValue,LuaValue)
     * @see #invoke(LuaValue,LuaValue,Varargs)
     */
    public Varargs invoke(LuaValue[] args, Varargs varargs) {
        return invoke(varargsOf(args, varargs));
    }

    /**
     * Call named method on this with 0 arguments, including metatag processing, and retain all return values
     * in a {@link Varargs}.
     * <p>
     * Look up {@code this[name]} and if it is a {@link LuaFunction}, call it inserting this as an additional
     * first argument, and return all return values as a {@link Varargs} instance. Otherwise, look for the
     * {@link #META_CALL} metatag and call that.
     * <p>
     * To get a particular return value, us {@link Varargs#arg(int)}
     * <p>
     * To call this as a plain call, use {@link #invoke()} instead.
     *
     * @param name Name of the method to look up for invocation
     * @return All values returned from {@code this:name()} as a {@link Varargs} instance
     * @throws LuaError if not a function and {@link #META_CALL} is not defined, or the invoked function throws a
     *         {@link LuaError} or the invoked closure throw a lua {@code error}
     * @see #call()
     * @see #invoke()
     * @see #method(String)
     * @see #invokemethod(LuaValue)
     * @see #invokemethod(String,Varargs)
     */
    public Varargs invokemethod(String name) {
        return get(name).invoke(this);
    }

    /**
     * Call named method on this with 0 arguments, including metatag processing, and retain all return values
     * in a {@link Varargs}.
     * <p>
     * Look up {@code this[name]} and if it is a {@link LuaFunction}, call it inserting this as an additional
     * first argument, and return all return values as a {@link Varargs} instance. Otherwise, look for the
     * {@link #META_CALL} metatag and call that.
     * <p>
     * To get a particular return value, us {@link Varargs#arg(int)}
     * <p>
     * To call this as a plain call, use {@link #invoke()} instead.
     *
     * @param name Name of the method to look up for invocation
     * @return All values returned from {@code this:name()} as a {@link Varargs} instance
     * @throws LuaError if not a function and {@link #META_CALL} is not defined, or the invoked function throws a
     *         {@link LuaError} or the invoked closure throw a lua {@code error}
     * @see #call()
     * @see #invoke()
     * @see #method(LuaValue)
     * @see #invokemethod(String)
     * @see #invokemethod(String, LuaValue[])
     */
    public Varargs invokemethod(LuaValue name) {
        return get(name).invoke(this);
    }

    /**
     * Call named method on this with 1 argument, including metatag processing, and retain all return values
     * in a {@link Varargs}.
     * <p>
     * Look up {@code this[name]} and if it is a {@link LuaFunction}, call it inserting this as an additional
     * first argument, and return all return values as a {@link Varargs} instance. Otherwise, look for the
     * {@link #META_CALL} metatag and call that.
     * <p>
     * To get a particular return value, us {@link Varargs#arg(int)}
     * <p>
     * To call this as a plain call, use {@link #invoke(Varargs)} instead.
     *
     * @param name Name of the method to look up for invocation
     * @param args {@link Varargs} containing arguments to supply to the called function after {@code this}
     * @return All values returned from {@code this:name(args)} as a {@link Varargs} instance
     * @throws LuaError if not a function and {@link #META_CALL} is not defined, or the invoked function throws a
     *         {@link LuaError} or the invoked closure throw a lua {@code error}
     * @see #call()
     * @see #invoke(Varargs)
     * @see #method(String)
     * @see #invokemethod(LuaValue,Varargs)
     * @see #invokemethod(String,LuaValue[])
     */
    public Varargs invokemethod(String name, Varargs args) {
        return get(name).invoke(varargsOf(this, args));
    }

    /**
     * Call named method on this with variable arguments, including metatag processing, and retain all return
     * values in a {@link Varargs}.
     * <p>
     * Look up {@code this[name]} and if it is a {@link LuaFunction}, call it inserting this as an additional
     * first argument, and return all return values as a {@link Varargs} instance. Otherwise, look for the
     * {@link #META_CALL} metatag and call that.
     * <p>
     * To get a particular return value, us {@link Varargs#arg(int)}
     * <p>
     * To call this as a plain call, use {@link #invoke(Varargs)} instead.
     *
     * @param name Name of the method to look up for invocation
     * @param args {@link Varargs} containing arguments to supply to the called function after {@code this}
     * @return All values returned from {@code this:name(args)} as a {@link Varargs} instance
     * @throws LuaError if not a function and {@link #META_CALL} is not defined, or the invoked function throws a
     *         {@link LuaError} or the invoked closure throw a lua {@code error}
     * @see #call()
     * @see #invoke(Varargs)
     * @see #method(String)
     * @see #invokemethod(String,Varargs)
     * @see #invokemethod(LuaValue,LuaValue[])
     */
    public Varargs invokemethod(LuaValue name, Varargs args) {
        return get(name).invoke(varargsOf(this, args));
    }

    /**
     * Call named method on this with 1 argument, including metatag processing, and retain all return values
     * in a {@link Varargs}.
     * <p>
     * Look up {@code this[name]} and if it is a {@link LuaFunction}, call it inserting this as an additional
     * first argument, and return all return values as a {@link Varargs} instance. Otherwise, look for the
     * {@link #META_CALL} metatag and call that.
     * <p>
     * To get a particular return value, us {@link Varargs#arg(int)}
     * <p>
     * To call this as a plain call, use {@link #invoke(Varargs)} instead.
     *
     * @param name Name of the method to look up for invocation
     * @param args Array of {@link LuaValue} containing arguments to supply to the called function after
     *        {@code this}
     * @return All values returned from {@code this:name(args)} as a {@link Varargs} instance
     * @throws LuaError if not a function and {@link #META_CALL} is not defined, or the invoked function throws a
     *         {@link LuaError} or the invoked closure throw a lua {@code error}
     * @see #call()
     * @see #invoke(Varargs)
     * @see #method(String)
     * @see #invokemethod(LuaValue,LuaValue[])
     * @see #invokemethod(String,Varargs)
     * @see LuaValue#varargsOf(LuaValue[])
     */
    public Varargs invokemethod(String name, LuaValue[] args) {
        return get(name).invoke(varargsOf(this, varargsOf(args)));
    }

    /**
     * Call named method on this with variable arguments, including metatag processing, and retain all return
     * values in a {@link Varargs}.
     * <p>
     * Look up {@code this[name]} and if it is a {@link LuaFunction}, call it inserting this as an additional
     * first argument, and return all return values as a {@link Varargs} instance. Otherwise, look for the
     * {@link #META_CALL} metatag and call that.
     * <p>
     * To get a particular return value, us {@link Varargs#arg(int)}
     * <p>
     * To call this as a plain call, use {@link #invoke(Varargs)} instead.
     *
     * @param name Name of the method to look up for invocation
     * @param args Array of {@link LuaValue} containing arguments to supply to the called function after
     *        {@code this}
     * @return All values returned from {@code this:name(args)} as a {@link Varargs} instance
     * @throws LuaError if not a function and {@link #META_CALL} is not defined, or the invoked function throws a
     *         {@link LuaError} or the invoked closure throw a lua {@code error}
     * @see #call()
     * @see #invoke(Varargs)
     * @see #method(String)
     * @see #invokemethod(String,LuaValue[])
     * @see #invokemethod(LuaValue,Varargs)
     * @see LuaValue#varargsOf(LuaValue[])
     */
    public Varargs invokemethod(LuaValue name, LuaValue[] args) {
        return get(name).invoke(varargsOf(this, varargsOf(args)));
    }

    /**
     * Get the metatag value for the {@link #META_CALL} metatag, if it exists.
     *
     * @return {@link LuaValue} value if metatag is defined
     * @throws LuaError if {@link #META_CALL} metatag is not defined.
     */
    protected LuaValue callmt() {
        return checkmetatag(META_CALL, "attempt to call ");
    }

    /**
     * Unary not: return inverse boolean value {@code (~this)} as defined by lua not operator.
     *
     * @return {@link #TRUE} if {@link #NIL} or {@link #FALSE}, otherwise {@link #FALSE}
     */
    public LuaValue not() {
        return FALSE;
    }

    @Override
    public LuaValue neg() {
        return checkmetatag(META_UNM, "attempt to perform arithmetic on ").call(this);
    }

    /**
     * Length operator: return lua length of object {@code (#this)} including metatag processing as java int.
     *
     * @return length as defined by the lua # operator or metatag processing result
     * @throws LuaError if {@code this} is not a table or string, and has no {@link #META_LEN} metatag
     */
    public LuaValue len() {
        return checkmetatag(META_LEN, "attempt to get length of ").call(this);
    }

    /**
     * Length operator: return lua length of object {@code (#this)} including metatag processing as java int.
     *
     * @return length as defined by the lua # operator or metatag processing result converted to java int
     *         using {@link #toint()}
     * @throws LuaError if {@code this} is not a table or string, and has no {@link #META_LEN} metatag
     */
    public int length() {
        return len().toint();
    }

    /**
     * Implementation of lua 5.0 getn() function.
     *
     * @return value of getn() as defined in lua 5.0 spec if {@code this} is a {@link LuaTable}
     * @throws LuaError if {@code this} is not a {@link LuaTable}
     */
    public LuaValue getn() {
        return typerror("getn");
    }

    @Override
    public final LuaValue eq(LuaValue val) {
        return eq_b(val) ? TRUE : FALSE;
    }

    @Override
    public boolean eq_b(LuaValue val) {
        return this == val;
    }

    @Override
    public LuaValue neq(LuaValue val) {
        return eq_b(val) ? FALSE : TRUE;
    }

    @Override
    public boolean neq_b(LuaValue val) {
        return !eq_b(val);
    }

    /**
     * Equals: Perform direct equality comparison with another value without metatag processing.
     *
     * @param val The value to compare with.
     * @return true if {@code (this == rhs)}, false otherwise
     * @see #eq(LuaValue)
     * @see #raweq(LuaUserdata)
     * @see #raweq(LuaString)
     * @see #raweq(double)
     * @see #raweq(int)
     * @see #META_EQ
     */
    public boolean raweq(LuaValue val) {
        return this == val;
    }

    /**
     * Equals: Perform direct equality comparison with a {@link LuaUserdata} value without metatag processing.
     *
     * @param val The {@link LuaUserdata} to compare with.
     * @return true if {@code this} is userdata and their metatables are the same using == and their instances
     *         are equal using {@link #equals(Object)}, otherwise false
     * @see #eq(LuaValue)
     * @see #raweq(LuaValue)
     */
    public boolean raweq(LuaUserdata val) {
        return false;
    }

    /**
     * Equals: Perform direct equality comparison with a {@link LuaString} value without metatag processing.
     *
     * @param val The {@link LuaString} to compare with.
     * @return true if {@code this} is a {@link LuaString} and their byte sequences match, otherwise false
     */
    public boolean raweq(LuaString val) {
        return false;
    }

    /**
     * Equals: Perform direct equality comparison with a double value without metatag processing.
     *
     * @param val The double value to compare with.
     * @return true if {@code this} is a {@link LuaNumber} whose value equals val, otherwise false
     */
    public boolean raweq(double val) {
        return false;
    }

    /**
     * Equals: Perform direct equality comparison with a int value without metatag processing.
     *
     * @param val The double value to compare with.
     * @return true if {@code this} is a {@link LuaNumber} whose value equals val, otherwise false
     */
    public boolean raweq(int val) {
        return false;
    }

    /**
     * Perform equality metatag processing.
     *
     * @param lhs left-hand-side of equality expression
     * @param lhsmt metatag value for left-hand-side
     * @param rhs right-hand-side of equality expression
     * @param rhsmt metatag value for right-hand-side
     * @return true if metatag processing result is not {@link #NIL} or {@link #FALSE}
     * @throws LuaError if metatag was not defined for either operand
     * @see #equals(Object)
     * @see #eq(LuaValue)
     * @see #raweq(LuaValue)
     */
    static final boolean eqmtcall(LuaValue lhs, LuaValue lhsmt, LuaValue rhs, LuaValue rhsmt) {
        LuaValue compareMethod = lhsmt.rawget(META_EQ);
        if (compareMethod.isnil() || !compareMethod.raweq(rhsmt.rawget(META_EQ))) {
            // Metamethod missing or different for the two operands
            return false;
        }
        return compareMethod.call(lhs, rhs).toboolean();
    }

    @Override
    public LuaValue add(LuaValue rhs) {
        return arithmt(META_ADD, rhs);
    }

    @Override
    public LuaValue add(double rhs) {
        return arithmtwith(META_ADD, rhs);
    }

    @Override
    public LuaValue add(int rhs) {
        return add((double)rhs);
    }

    @Override
    public LuaValue sub(LuaValue rhs) {
        return arithmt(META_SUB, rhs);
    }

    @Override
    public LuaValue sub(double rhs) {
        return aritherror("sub");
    }

    @Override
    public LuaValue sub(int rhs) {
        return sub((double)rhs);
    }

    @Override
    public LuaValue subFrom(double lhs) {
        return arithmtwith(META_SUB, lhs);
    }

    @Override
    public LuaValue subFrom(int lhs) {
        return subFrom((double)lhs);
    }

    @Override
    public LuaValue mul(LuaValue rhs) {
        return arithmt(META_MUL, rhs);
    }

    @Override
    public LuaValue mul(double rhs) {
        return arithmtwith(META_MUL, rhs);
    }

    @Override
    public LuaValue mul(int rhs) {
        return mul((double)rhs);
    }

    @Override
    public LuaValue pow(LuaValue rhs) {
        return arithmt(META_POW, rhs);
    }

    @Override
    public LuaValue pow(double rhs) {
        return aritherror("pow");
    }

    @Override
    public LuaValue pow(int rhs) {
        return pow((double)rhs);
    }

    @Override
    public LuaValue powWith(double lhs) {
        return arithmtwith(META_POW, lhs);
    }

    @Override
    public LuaValue powWith(int lhs) {
        return powWith((double)lhs);
    }

    @Override
    public LuaValue div(LuaValue rhs) {
        return arithmt(META_DIV, rhs);
    }

    @Override
    public LuaValue div(double rhs) {
        return aritherror("div");
    }

    @Override
    public LuaValue div(int rhs) {
        return div((double)rhs);
    }

    @Override
    public LuaValue divInto(double lhs) {
        return arithmtwith(META_DIV, lhs);
    }

    @Override
    public LuaValue mod(LuaValue rhs) {
        return arithmt(META_MOD, rhs);
    }

    @Override
    public LuaValue mod(double rhs) {
        return aritherror("mod");
    }

    @Override
    public LuaValue mod(int rhs) {
        return mod((double)rhs);
    }

    @Override
    public LuaValue modFrom(double lhs) {
        return arithmtwith(META_MOD, lhs);
    }

    /**
     * Perform metatag processing for arithmetic operations.
     * <p>
     * Finds the supplied metatag value for {@code this} or {@code op2} and invokes it, or throws
     * {@link LuaError} if neither is defined.
     *
     * @param tag The metatag to look up
     * @param op2 The other operand value to perform the operation with
     * @return {@link LuaValue} resulting from metatag processing
     * @throws LuaError if metatag was not defined for either operand
     * @see #add(LuaValue)
     * @see #sub(LuaValue)
     * @see #mul(LuaValue)
     * @see #pow(LuaValue)
     * @see #div(LuaValue)
     * @see #mod(LuaValue)
     */
    protected LuaValue arithmt(LuaValue tag, LuaValue op2) {
        LuaValue h = this.metatag(tag);
        if (h.isnil()) {
            h = op2.metatag(tag);
            if (h.isnil()) {
                error("attempt to perform arithmetic " + tag + " on " + debugName() + " and "
                        + op2.debugName());
            }
        }
        return h.call(this, op2);
    }

    /**
     * Perform metatag processing for arithmetic operations when the left-hand-side is a number.
     * <p>
     * Finds the supplied metatag value for {@code this} and invokes it, or throws {@link LuaError} if neither
     * is defined.
     *
     * @param tag The metatag to look up
     * @param op1 The value of the left-hand-side to perform the operation with
     * @return {@link LuaValue} resulting from metatag processing
     * @throws LuaError if metatag was not defined for either operand
     * @see #add(LuaValue)
     * @see #sub(LuaValue)
     * @see #mul(LuaValue)
     * @see #pow(LuaValue)
     * @see #div(LuaValue)
     * @see #mod(LuaValue)
     */
    protected LuaValue arithmtwith(LuaValue tag, double op1) {
        LuaValue h = metatag(tag);
        if (h.isnil()) {
            error("attempt to perform arithmetic " + tag + " on number and " + debugName());
        }
        return h.call(LuaValue.valueOf(op1), this);
    }

    @Override
    public LuaValue lt(LuaValue rhs) {
        return comparemt(META_LT, rhs);
    }

    @Override
    public LuaValue lt(double rhs) {
        return compareerror("number");
    }

    @Override
    public LuaValue lt(int rhs) {
        return compareerror("number");
    }

    @Override
    public boolean lt_b(LuaValue rhs) {
        return comparemt(META_LT, rhs).toboolean();
    }

    @Override
    public boolean lt_b(int rhs) {
        compareerror("number");
        return false;
    }

    @Override
    public boolean lt_b(double rhs) {
        compareerror("number");
        return false;
    }

    @Override
    public LuaValue lteq(LuaValue rhs) {
        return comparemt(META_LE, rhs);
    }

    @Override
    public LuaValue lteq(double rhs) {
        return compareerror("number");
    }

    @Override
    public LuaValue lteq(int rhs) {
        return compareerror("number");
    }

    @Override
    public boolean lteq_b(LuaValue rhs) {
        return comparemt(META_LE, rhs).toboolean();
    }

    @Override
    public boolean lteq_b(int rhs) {
        compareerror("number");
        return false;
    }

    @Override
    public boolean lteq_b(double rhs) {
        compareerror("number");
        return false;
    }

    @Override
    public LuaValue gt(LuaValue rhs) {
        return rhs.comparemt(META_LE, this);
    }

    @Override
    public LuaValue gt(double rhs) {
        return compareerror("number");
    }

    @Override
    public LuaValue gt(int rhs) {
        return compareerror("number");
    }

    @Override
    public boolean gt_b(LuaValue rhs) {
        return rhs.comparemt(META_LE, this).toboolean();
    }

    @Override
    public boolean gt_b(int rhs) {
        compareerror("number");
        return false;
    }

    @Override
    public boolean gt_b(double rhs) {
        compareerror("number");
        return false;
    }

    @Override
    public LuaValue gteq(LuaValue rhs) {
        return rhs.comparemt(META_LT, this);
    }

    @Override
    public LuaValue gteq(double rhs) {
        return compareerror("number");
    }

    @Override
    public LuaValue gteq(int rhs) {
        return valueOf(todouble() >= rhs);
    }

    @Override
    public boolean gteq_b(LuaValue rhs) {
        return rhs.comparemt(META_LT, this).toboolean();
    }

    @Override
    public boolean gteq_b(int rhs) {
        compareerror("number");
        return false;
    }

    @Override
    public boolean gteq_b(double rhs) {
        compareerror("number");
        return false;
    }

    /**
     * Perform metatag processing for comparison operations.
     * <p>
     * Finds the supplied metatag value and invokes it, or throws {@link LuaError} if none applies.
     *
     * @param tag The metatag to look up
     * @param op1 The right-hand-side value to perform the operation with
     * @return {@link LuaValue} resulting from metatag processing
     * @throws LuaError if metatag was not defined for either operand, or if the operands are not the same
     *         type, or the metatag values for the two operands are different.
     * @see #gt(LuaValue)
     * @see #gteq(LuaValue)
     * @see #lt(LuaValue)
     * @see #lteq(LuaValue)
     */
    private LuaValue comparemt(LuaValue tag, LuaValue op1) {
        // Try to get metatag from LHS of the comparison, and if nil, try RHS
        LuaValue compareMethod = metatag(tag);
        if (compareMethod.isnil()) {
            compareMethod = op1.metatag(tag);
        }

        if (!compareMethod.isnil()) {
            return compareMethod.call(this, op1);
        }

        // If the operator is LE, fall back to LT assuming: a <= b === !(b < a)
        // Try to get metatag from LHS of the comparison, and if nil, try RHS
        LuaString otherTag = (META_LE.raweq(tag) ? META_LT : META_LE);
        compareMethod = metatag(otherTag);
        if (compareMethod.isnil()) {
            compareMethod = op1.metatag(otherTag);
        }

        if (!compareMethod.isnil()) {
            return compareMethod.call(op1, this).not();
        }

        return error("attempt to compare " + tag + " on " + debugName() + " and " + op1.debugName());
    }

    @Override
    public int strcmp(LuaValue rhs) {
        error("attempt to compare " + debugName());
        return 0;
    }

    @Override
    public int strcmp(LuaString rhs) {
        error("attempt to compare " + debugName());
        return 0;
    }

    /**
     * Concatenate another value onto this value and return the result using rules of lua string concatenation
     * including metatag processing.
     * <p>
     * Only strings and numbers as represented can be concatenated, meaning each operand must derive from
     * {@link LuaString} or {@link LuaNumber}.
     *
     * @param rhs The right-hand-side value to perform the operation with
     * @returns {@link LuaValue} resulting from concatenation of {@code (this .. rhs)}
     * @throws LuaError if either operand is not of an appropriate type, such as nil or a table
     */
    public LuaValue concat(LuaValue rhs) {
        return this.concatmt(rhs);
    }


    /**
     * Concatenate a {@link Buffer} onto this value and return the result using rules of lua string
     * concatenation including metatag processing.
     * <p>
     * Only strings and numbers as represented can be concatenated, meaning each operand must derive from
     * {@link LuaString} or {@link LuaNumber}.
     *
     * @param rhs The right-hand-side {@link Buffer} to perform the operation with
     * @return LuaString resulting from concatenation of {@code (this .. rhs)}
     * @throws LuaError if either operand is not of an appropriate type, such as nil or a table
     */
    public Buffer concat(Buffer rhs) {
        return rhs.concatTo(this);
    }

    /**
     * Reverse-concatenation: concatenate this value onto another value whose type is unknwon and return the
     * result using rules of lua string concatenation including metatag processing.
     * <p>
     * Only strings and numbers as represented can be concatenated, meaning each operand must derive from
     * {@link LuaString} or {@link LuaNumber}.
     *
     * @param lhs The left-hand-side value onto which this will be concatenated
     * @returns {@link LuaValue} resulting from concatenation of {@code (lhs .. this)}
     * @throws LuaError if either operand is not of an appropriate type, such as nil or a table
     * @see #concat(LuaValue)
     */
    public LuaValue concatTo(LuaValue lhs) {
        return lhs.concatmt(this);
    }

    /**
     * Reverse-concatenation: concatenate this value onto another value known to be a {@link LuaNumber} and
     * return the result using rules of lua string concatenation including metatag processing.
     * <p>
     * Only strings and numbers as represented can be concatenated, meaning each operand must derive from
     * {@link LuaString} or {@link LuaNumber}.
     *
     * @param lhs The left-hand-side value onto which this will be concatenated
     * @returns {@link LuaValue} resulting from concatenation of {@code (lhs .. this)}
     * @throws LuaError if either operand is not of an appropriate type, such as nil or a table
     * @see #concat(LuaValue)
     */
    public LuaValue concatTo(LuaNumber lhs) {
        return lhs.concatmt(this);
    }

    /**
     * Reverse-concatenation: concatenate this value onto another value known to be a {@link LuaString} and
     * return the result using rules of lua string concatenation including metatag processing.
     * <p>
     * Only strings and numbers as represented can be concatenated, meaning each operand must derive from
     * {@link LuaString} or {@link LuaNumber}.
     *
     * @param lhs The left-hand-side value onto which this will be concatenated
     * @returns {@link LuaValue} resulting from concatenation of {@code (lhs .. this)}
     * @throws LuaError if either operand is not of an appropriate type, such as nil or a table
     * @see #concat(LuaValue)
     */
    public LuaValue concatTo(LuaString lhs) {
        return lhs.concatmt(this);
    }

    /**
     * Convert the value to a {@link Buffer} for more efficient concatenation of multiple strings.
     *
     * @return Buffer instance containing the string or number
     */
    public Buffer buffer() {
        return new Buffer(this);
    }

    /**
     * Perform metatag processing for concatenation operations.
     * <p>
     * Finds the {@link #META_CONCAT} metatag value and invokes it, or throws {@link LuaError} if it doesn't exist.
     *
     * @param rhs The right-hand-side value to perform the operation with
     * @return {@link LuaValue} resulting from metatag processing for {@link #META_CONCAT} metatag.
     * @throws LuaError if metatag was not defined for either operand
     */
    public LuaValue concatmt(LuaValue rhs) {
        LuaValue h = metatag(META_CONCAT);
        if (h.isnil() && (h = rhs.metatag(META_CONCAT)).isnil()) {
            error("attempt to concatenate " + debugName() + " and " + rhs.debugName());
        }
        return h.call(this, rhs);
    }

    /**
     * Perform boolean {@code and} with another operand, based on lua rules for boolean evaluation. This
     * returns either {@code this} or {@code rhs} depending on the boolean value for {@code this}.
     *
     * @param rhs The right-hand-side value to perform the operation with
     * @return {@code this} if {@code this.toboolean()} is false, {@code rhs} otherwise.
     */
    public LuaValue and(LuaValue rhs) {
        return this.toboolean() ? rhs : this;
    }

    /**
     * Perform boolean {@code or} with another operand, based on lua rules for boolean evaluation. This
     * returns either {@code this} or {@code rhs} depending on the boolean value for {@code this}.
     *
     * @param rhs The right-hand-side value to perform the operation with
     * @return {@code this} if {@code this.toboolean()} is true, {@code rhs} otherwise.
     */
    public LuaValue or(LuaValue rhs) {
        return this.toboolean() ? this : rhs;
    }

    /**
     * Convert this value to a string if it is a {@link LuaString} or {@link LuaNumber}, or throw a
     * {@link LuaError} if it is not.
     *
     * @return {@link LuaString} corresponding to the value if a string or number
     * @throws LuaError if not a string or number
     */
    public LuaString strvalue() {
        typerror("strValue");
        return null;
    }

    /**
     * Get raw length of table or string without metatag processing.
     *
     * @return the length of the table or string.
     * @throws LuaError if {@code this} is not a table or string.
     */
    public int rawlen() {
        typerror("table or string");
        return 0;
    }

    /**
     * Return the key part of this value if it is a weak table entry, or {@link #NIL} if it was weak and is no
     * longer referenced.
     *
     * @return {@link LuaValue} key, or {@link #NIL} if it was weak and is no longer referenced.
     * @see WeakTable
     */
    public LuaValue strongkey() {
        return strongvalue();
    }

    /**
     * Return this value as a strong reference, or {@link #NIL} if it was weak and is no longer referenced.
     *
     * @return {@link LuaValue} referred to, or {@link #NIL} if it was weak and is no longer referenced.
     * @see WeakTable
     */
    public LuaValue strongvalue() {
        return this;
    }

    /**
     * Test if this is a weak reference and its value no longer is referenced.
     *
     * @return true if this is a weak reference whose value no longer is referenced
     * @see WeakTable
     */
    public boolean isweaknil() {
        return false;
    }

    /**
     * Convert java boolean to a {@link LuaValue}.
     *
     * @param b boolean value to convert
     * @return {@link #TRUE} if not or {@link #FALSE} if false
     */
    public static LuaBoolean valueOf(boolean b) {
        return (b ? TRUE : FALSE);
    }

    /**
     * Convert java int to a {@link LuaValue}.
     *
     * @param i int value to convert
     * @return {@link LuaInteger} instance, possibly pooled, whose value is i
     */
    public static LuaInteger valueOf(int i) {
        return LuaInteger.valueOf(i);
    }

    /**
     * Convert java double to a {@link LuaValue}. This may return a {@link LuaInteger} or {@link LuaDouble}
     * depending on the value supplied.
     *
     * @param d double value to convert
     * @return {@link LuaNumber} instance, possibly pooled, whose value is d
     */
    public static LuaNumber valueOf(double d) {
        return LuaDouble.valueOf(d);
    }

    /**
     * Convert java string to a {@link LuaValue}.
     *
     * @param s String value to convert
     * @return {@link LuaString} instance, possibly pooled, whose value is s
     */
    public static LuaString valueOf(String s) {
        return LuaString.valueOf(s);
    }

    /**
     * Convert bytes in an array to a {@link LuaValue}.
     *
     * @param bytes byte array to convert
     * @return {@link LuaString} instance, possibly pooled, whose bytes are those in the supplied array
     */
    public static LuaString valueOf(byte[] bytes) {
        return LuaString.valueOf(bytes);
    }

    /**
     * Convert bytes in an array to a {@link LuaValue}.
     *
     * @param bytes byte array to convert
     * @param off offset into the byte array, starting at 0
     * @param len number of bytes to include in the {@link LuaString}
     * @return {@link LuaString} instance, possibly pooled, whose bytes are those in the supplied array
     */
    public static LuaString valueOf(byte[] bytes, int off, int len) {
        return LuaString.valueOf(bytes, off, len);
    }

    /**
     * Construct a {@link LuaTable} initialized with supplied array values.
     *
     * @param unnamedValues array of {@link LuaValue} containing the values to use in initialization
     * @return new {@link LuaTable} instance with sequential elements coming from the array.
     */
    public static LuaTable listOf(LuaValue[] unnamedValues) {
        return new LuaTable(null, unnamedValues, null);
    }

    /**
     * Construct a {@link LuaTable} initialized with supplied array values.
     *
     * @param unnamedValues array of {@link LuaValue} containing the first values to use in initialization
     * @param lastarg {@link Varargs} containing additional values to use in initialization to be put after
     *        the last unnamedValues element
     * @return new {@link LuaTable} instance with sequential elements coming from the array and varargs.
     */
    public static LuaTable listOf(LuaValue[] unnamedValues, Varargs lastarg) {
        return new LuaTable(null, unnamedValues, lastarg);
    }

    /**
     * Construct an empty {@link LuaTable}.
     *
     * @return new {@link LuaTable} instance with no values and no metatable.
     */
    public static LuaTable tableOf() {
        return new LuaTable();
    }

    /**
     * Construct a {@link LuaTable} initialized with supplied array values.
     *
     * @param varargs {@link Varargs} containing the values to use in initialization
     * @param firstarg the index of the first argument to use from the varargs, 1 being the first.
     * @return new {@link LuaTable} instance with sequential elements coming from the varargs.
     */
    public static LuaTable tableOf(Varargs varargs, int firstarg) {
        return new LuaTable(varargs, firstarg);
    }

    /**
     * Construct an empty {@link LuaTable} preallocated to hold array and hashed elements
     *
     * @param narray Number of array elements to preallocate
     * @param nhash Number of hash elements to preallocate
     * @return new {@link LuaTable} instance with no values and no metatable, but preallocated for array and
     *         hashed elements.
     */
    public static LuaTable tableOf(int narray, int nhash) {
        return new LuaTable(narray, nhash);
    }

    /**
     * Construct a {@link LuaTable} initialized with supplied named values.
     *
     * @param namedValues array of {@link LuaValue} containing the keys and values to use in initialization in order
     *        {@code key-a, value-a, key-b, value-b, ...}
     * @return new {@link LuaTable} instance with non-sequential keys coming from the supplied array.
     */
    public static LuaTable tableOf(LuaValue[] namedValues) {
        return new LuaTable(namedValues, null, null);
    }

    /**
     * Construct a {@link LuaTable} initialized with supplied named values and sequential elements. The named values
     * will be assigned first, and the sequential elements will be assigned later, possibly overwriting named values at
     * the same slot if there are conflicts.
     *
     * @param namedValues array of {@link LuaValue} containing the keys and values to use in initialization in order
     *        {@code key-a, value-a,
     *        key-b, value-b, ...}
     * @param unnamedValues array of {@link LuaValue} containing the sequenctial elements to use in initialization in
     *        order {@code value-1,
     *        value-2, ...} , or null if there are none
     * @return new {@link LuaTable} instance with named and sequential values supplied.
     */
    public static LuaTable tableOf(LuaValue[] namedValues, LuaValue[] unnamedValues) {
        return new LuaTable(namedValues, unnamedValues, null);
    }

    /**
     * Construct a {@link LuaTable} initialized with supplied named values and sequential elements in an array part and
     * as varargs. The named values will be assigned first, and the sequential elements will be assigned later, possibly
     * overwriting named values at the same slot if there are conflicts.
     *
     * @param namedValues array of {@link LuaValue} containing the keys and values to use in initialization in order
     *        {@code key-a, value-a,
     *        key-b, value-b, ...}
     * @param unnamedValues array of {@link LuaValue} containing the first sequenctial elements to use in initialization
     *        in order {@code value-1, value-2, ...} , or null if there are none
     * @param lastarg {@link Varargs} containing additional values to use in the sequential part of the initialization,
     *        to be put after the last unnamedValues element
     * @return new {@link LuaTable} instance with named and sequential values supplied.
     */
    public static LuaTable tableOf(LuaValue[] namedValues, LuaValue[] unnamedValues, Varargs lastarg) {
        return new LuaTable(namedValues, unnamedValues, lastarg);
    }

    /**
     * Construct a LuaUserdata for an object.
     *
     * @param o The java instance to be wrapped as userdata
     * @return {@link LuaUserdata} value wrapping the java instance.
     */
    public static LuaUserdata userdataOf(Object o) {
        return new LuaUserdata(o);
    }

    /**
     * Construct a LuaUserdata for an object with a user supplied metatable.
     *
     * @param o The java instance to be wrapped as userdata
     * @param metatable The metatble to associate with the userdata instance.
     * @return {@link LuaUserdata} value wrapping the java instance.
     */
    public static LuaUserdata userdataOf(Object o, LuaValue metatable) {
        return new LuaUserdata(o, metatable);
    }

    /** Constant limiting metatag loop processing. */
    private static final int MAXTAGLOOP = 100;

    /**
     * Return value for field reference including metatag processing, or {@link LuaNil#NIL} if it doesn't exist.
     *
     * @param t {@link LuaValue} on which field is being referenced, typically a table or something with the metatag
     *        {@link LuaConstants#META_INDEX} defined
     * @param key {@link LuaValue} naming the field to reference
     * @return {@link LuaValue} for the {@code key} if it exists, or {@link LuaNil#NIL}
     * @throws LuaError if there is a loop in metatag processing
     */
    protected static LuaValue gettable(LuaValue t, LuaValue key) {
        LuaValue tm;
        int loop = 0;
        do {
            if (t.istable()) {
                LuaValue res = t.rawget(key);
                if ((!res.isnil()) || (tm = t.metatag(META_INDEX)).isnil()) {
                    return res;
                }
            } else if ((tm = t.metatag(META_INDEX)).isnil()) {
                t.indexerror();
            }
            if (tm.isfunction()) {
                return tm.call(t, key);
            }
            t = tm;
        } while (++loop < MAXTAGLOOP);
        error("loop in gettable");
        return NIL;
    }

    /**
     * Perform field assignment including metatag processing.
     *
     * @param t {@link LuaValue} on which value is being set, typically a table or something with the metatag
     *        {@link LuaConstants#META_NEWINDEX} defined
     * @param key {@link LuaValue} naming the field to assign
     * @param value {@link LuaValue} the new value to assign to {@code key}
     * @return true if assignment or metatag processing succeeded, false otherwise
     * @throws LuaError if there is a loop in metatag processing
     */
    protected static boolean settable(LuaValue t, LuaValue key, LuaValue value) {
        LuaValue tm;
        int loop = 0;
        do {
            if (t.istable()) {
                if ((!t.rawget(key).isnil()) || (tm = t.metatag(META_NEWINDEX)).isnil()) {
                    t.rawset(key, value);
                    return true;
                }
            } else if ((tm = t.metatag(META_NEWINDEX)).isnil()) {
                t.typerror("index");
            }
            if (tm.isfunction()) {
                tm.call(t, key, value);
                return true;
            }
            t = tm;
        } while (++loop < MAXTAGLOOP);
        error("loop in settable");
        return false;
    }

    /**
     * Get particular metatag, or return {@link LuaNil#NIL} if it doesn't exist.
     *
     * @param tag Metatag name to look up, typically a string such as {@link LuaConstants#META_INDEX} or
     *        {@link LuaConstants#META_NEWINDEX}
     * @return {@link LuaValue} for tag {@code reason}, or {@link LuaNil#NIL}
     */
    public LuaValue metatag(LuaValue tag) {
        LuaValue mt = getmetatable();
        if (mt == null) {
            return NIL;
        }
        return mt.rawget(tag);
    }

    /**
     * Get particular metatag, or throw {@link LuaError} if it doesn't exist
     *
     * @param tag Metatag name to look up, typically a string such as {@link LuaConstants#META_INDEX} or
     *        {@link LuaConstants#META_NEWINDEX}
     * @param reason Description of error when tag lookup fails.
     * @return {@link LuaValue} that can be called
     * @throws LuaError when the lookup fails.
     */
    protected LuaValue checkmetatag(LuaValue tag, String reason) {
        LuaValue h = this.metatag(tag);
        if (h.isnil()) {
            throw new LuaError(reason + debugName());
        }
        return h;
    }

    /** Returns a string description of this object for use in error messages. */
    private String debugName() {
        LuaString[] name = DebugTrace.getobjname(this);

        if (name == null || name[0].raweq(valueOf("?"))) {
            return typename();
        } else {
            return name[1] + " '" + name[0] + "'";
        }
    }

    /**
     * Throw {@link LuaError} indicating index was attempted on illegal type
     *
     * @throws LuaError when called.
     */
    private void indexerror() {
        error("attempt to index " + debugName());
    }

    /** Construct a Metatable instance from the given LuaValue. */
    protected static IMetatable metatableOf(LuaValue mt) {
        if (mt != null && mt.istable()) {
            LuaValue mode = mt.rawget(META_MODE);
            if (mode.isstring()) {
                String m = mode.tojstring();
                boolean weakkeys = m.indexOf('k') >= 0;
                boolean weakvalues = m.indexOf('v') >= 0;
                if (weakkeys || weakvalues) {
                    return new WeakTable(weakkeys, weakvalues, mt);
                }
            }
            return (LuaTable)mt;
        } else if (mt != null) {
            return new NonTableMetatable(mt);
        } else {
            return null;
        }
    }

    /**
     * Construct a {@link Varargs} around an array of {@link LuaValue}s.
     *
     * @param v The array of {@link LuaValue}s
     * @return {@link Varargs} wrapping the supplied values.
     * @see LuaValue#varargsOf(LuaValue, Varargs)
     * @see LuaValue#varargsOf(LuaValue[], int, int)
     */
    public static Varargs varargsOf(final LuaValue[] v) {
        switch (v.length) {
        case 0:
            return NONE;
        case 1:
            return v[0];
        case 2:
            return new PairVarargs(v[0], v[1]);
        default:
            return new ArrayVarargs(v, NONE);
        }
    }

    /**
     * Construct a {@link Varargs} around an array of {@link LuaValue}s.
     *
     * @param v The array of {@link LuaValue}s
     * @param r {@link Varargs} contain values to include at the end
     * @return {@link Varargs} wrapping the supplied values.
     * @see LuaValue#varargsOf(LuaValue[])
     * @see LuaValue#varargsOf(LuaValue[], int, int, Varargs)
     */
    public static Varargs varargsOf(final LuaValue[] v, Varargs r) {
        switch (v.length) {
        case 0:
            return r;
        case 1:
            return new PairVarargs(v[0], r);
        default:
            return new ArrayVarargs(v, r);
        }
    }

    /**
     * Construct a {@link Varargs} around an array of {@link LuaValue}s.
     *
     * @param v The array of {@link LuaValue}s
     * @param offset number of initial values to skip in the array
     * @param length number of values to include from the array
     * @return {@link Varargs} wrapping the supplied values.
     * @see LuaValue#varargsOf(LuaValue[])
     * @see LuaValue#varargsOf(LuaValue[], int, int, Varargs)
     */
    public static Varargs varargsOf(final LuaValue[] v, final int offset, final int length) {
        switch (length) {
        case 0:
            return NONE;
        case 1:
            return v[offset];
        case 2:
            return new PairVarargs(v[offset + 0], v[offset + 1]);
        default:
            return new ArrayPartVarargs(v, offset, length);
        }
    }

    /**
     * Construct a {@link Varargs} around an array of {@link LuaValue}s.
     *
     * @param v The array of {@link LuaValue}s
     * @param offset number of initial values to skip in the array
     * @param length number of values to include from the array
     * @param more {@link Varargs} contain values to include at the end
     * @return {@link Varargs} wrapping the supplied values.
     * @see LuaValue#varargsOf(LuaValue[], Varargs)
     * @see LuaValue#varargsOf(LuaValue[], int, int)
     */
    public static Varargs varargsOf(final LuaValue[] v, final int offset, final int length, Varargs more) {
        switch (length) {
        case 0:
            return more;
        case 1:
            return new PairVarargs(v[offset], more);
        default:
            return new ArrayPartVarargs(v, offset, length, more);
        }
    }

    /**
     * Construct a {@link Varargs} around a set of 2 or more {@link LuaValue}s.
     * <p>
     * This can be used to wrap exactly 2 values, or a list consisting of 1 initial value followed by another
     * variable list of remaining values.
     *
     * @param v First {@link LuaValue} in the {@link Varargs}
     * @param r {@link LuaValue} supplying the 2rd value, or {@link Varargs}s supplying all values beyond the
     *        first
     * @return {@link Varargs} wrapping the supplied values.
     */
    public static Varargs varargsOf(LuaValue v, Varargs r) {
        switch (r.narg()) {
        case 0:
            return v;
        default:
            return new PairVarargs(v, r);
        }
    }

    /**
     * Construct a {@link Varargs} around a set of 3 or more {@link LuaValue}s.
     * <p>
     * This can be used to wrap exactly 3 values, or a list consisting of 2 initial values followed by another
     * variable list of remaining values.
     *
     * @param v1 First {@link LuaValue} in the {@link Varargs}
     * @param v2 Second {@link LuaValue} in the {@link Varargs}
     * @param v3 {@link LuaValue} supplying the 3rd value, or {@link Varargs}s supplying all values beyond the
     *        second
     * @return {@link Varargs} wrapping the supplied values.
     */
    public static Varargs varargsOf(LuaValue v1, LuaValue v2, Varargs v3) {
        switch (v3.narg()) {
        case 0:
            return new PairVarargs(v1, v2);
        default:
            return new ArrayVarargs(new LuaValue[] { v1, v2 }, v3);
        }
    }

}
