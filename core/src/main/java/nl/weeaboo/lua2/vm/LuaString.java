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
import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import java.io.ByteArrayInputStream;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import nl.weeaboo.lua2.internal.SharedByteAlloc;
import nl.weeaboo.lua2.io.LuaSerializable;

/**
 * Subclass of {@link LuaValue} for representing lua strings.
 * <p>
 * Because lua string values are more nearly sequences of bytes than
 * sequences of characters or unicode code points, the {@link LuaString}
 * implementation holds the string value in an internal byte array.
 * <p>
 * {@link LuaString} values are generally not mutable once constructed,
 * so multiple {@link LuaString} values can share a single byte array.
 * <p>
 * Currently {@link LuaString}s are pooled via a centrally managed weak table.
 * To ensure that as many string values as possible take advantage of this,
 * Constructors are not exposed directly.  As with number, booleans, and nil,
 * instance construction should be via {@link LuaValue#valueOf(byte[])} or similar API.
 * <p>
 * When Java Strings are used to initialize {@link LuaString} data, the UTF8 encoding is assumed.
 * The functions
 * {@link LuaString#lengthAsUtf8(char[])},
 * {@link LuaString#encodeToUtf8(char[], byte[], int)}, and
 * {@link LuaString#decodeAsUtf8(byte[], int, int)}
 * are used to convert back and forth between UTF8 byte arrays and character arrays.
 *
 * @see LuaValue
 * @see LuaValue#valueOf(String)
 * @see LuaValue#valueOf(byte[])
 */
@LuaSerializable
public final class LuaString extends LuaValue implements Externalizable {

    private static final CharsetDecoder UTF8_DECODER = Charset.forName("UTF-8").newDecoder();

    private static final int MAX_STRING_LENGTH = 64 << 20; // 64 MiB
    private static final int MAX_TO_STRING_LENGTH = 1000;

    public static LuaValue s_metatable;

    //--- Uses manual serialization, don't add variables ---
    private /*final*/ byte[] strBytes;
    private /*final*/ int strOffset;
    private /*final*/ int strLength;
    private transient /*final*/ int hashCode;
    //--- Uses manual serialization, don't add variables ---

    /**
     * Get a {@link LuaString} instance whose bytes match the supplied Java
     * String using the UTF8 encoding.
     *
     * @param string Java String containing characters to encode as UTF8
     * @return {@link LuaString} with UTF8 bytes corresponding to the supplied
     *         String
     */
    public static LuaString valueOf(String string) {
        //Calculate length
        final int charLength = string.length();
        int length = charLength;
        for (int n = 0; n < charLength; n++) {
            char c = string.charAt(n);
            if (c >= 0x80) {
                length += (c >= 0x800 ? 2 : 1);
            }
        }

        //Encode to UTF-8
        SharedByteAlloc sba = SharedByteAlloc.getInstance();
        int offset = sba.reserve(length);
        byte[] b = sba.getReserved();
        for (int s = 0, d = offset; s < charLength; s++) {
            char c = string.charAt(s);
            if (c < 0x80) {
                b[d++] = (byte) c;
            } else if (c < 0x800) {
                b[d++] = (byte) (0xC0 | ((c >> 6) & 0x1f));
                b[d++] = (byte) (0x80 | (c & 0x3f));
            } else {
                b[d++] = (byte) (0xE0 | ((c >> 12) & 0x0f));
                b[d++] = (byte) (0x80 | ((c >> 6) & 0x3f));
                b[d++] = (byte) (0x80 | (c & 0x3f));
            }
        }

        return valueOf(b, offset, length);
    }

    /**
     * Construct a {@link LuaString} around a byte array without copying the
     * contents.
     * <p>
     * The array is used directly after this is called, so clients must not
     * change contents.
     * <p>
     *
     * @param bytes byte buffer
     * @param off offset into the byte buffer
     * @param len length of the byte buffer
     * @return {@link LuaString} wrapping the byte buffer
     */
    public static LuaString valueOf(byte[] bytes, int off, int len) {
        return new LuaString(bytes, off, len);
    }

    /**
     * Construct a {@link LuaString} around a byte array without copying the
     * contents.
     * <p>
     * The array is used directly after this is called, so clients must not
     * change contents.
     * <p>
     *
     * @param bytes byte buffer
     * @return {@link LuaString} wrapping the byte buffer
     */
    public static LuaString valueOf(byte[] bytes) {
        return valueOf(bytes, 0, bytes.length);
    }

    /**
     * Do not use. Required for efficient serialization.
     */
    @Deprecated
    public LuaString() {
    }

    /**
     * Construct a {@link LuaString} around a byte array without copying the
     * contents.
     * <p>
     * The array is used directly after this is called, so clients must not
     * change contents.
     * <p>
     *
     * @param bytes byte buffer
     * @param offset offset into the byte buffer
     * @param length length of the byte buffer
     */
    private LuaString(byte[] bytes, int offset, int length) {
        this.strBytes = bytes;
        this.strOffset = offset;
        this.strLength = length;

        if (strOffset < 0) {
            throw new IllegalArgumentException("Invalid offset: " + strOffset);
        }
        if (strLength < 0 || strOffset + strLength > bytes.length) {
            throw new IllegalArgumentException("Invalid length: " + strLength + " (offset=" + strOffset
                    + ", array.length=" + strBytes.length + ")");
        }
    }

    @Override
    public boolean isstring() {
        return true;
    }

    @Override
    public LuaValue getmetatable() {
        return s_metatable;
    }

    @Override
    public int type() {
        return LuaConstants.TSTRING;
    }

    @Override
    public String typename() {
        return "string";
    }

    @Override
    public String tojstring() {
        return decodeAsUtf8(strBytes, strOffset, strLength);
    }

    @Override
    public String toString() {
        /*
         * toString() is generally used for logging and debugging -- we don't want to cause perf. problems by
         * printing multi-megabyte strings.
         */
        if (strLength > MAX_TO_STRING_LENGTH) {
            return decodeAsUtf8(strBytes, strOffset, MAX_TO_STRING_LENGTH) + "[...]";
        } else {
            return decodeAsUtf8(strBytes, strOffset, strLength);
        }
    }

    // get is delegated to the string library
    @Override
    public LuaValue get(LuaValue key) {
        if (s_metatable != null) {
            return gettable(this, key);
        }
        return getfenv().get("string").get(key);
    }

    // unary operators
    @Override
    public LuaValue neg() {
        LuaValue numVal = tonumber();
        if (numVal.isnil()) {
            return super.neg();
        } else {
            return numVal.neg();
        }
    }

    // basic binary arithmetic
    @Override
    public LuaValue add(LuaValue rhs) {
        double val = scannumber(10);
        if (Double.isNaN(val) || !rhs.isnumber()) {
            return super.add(rhs);
        } else {
            // Special case: both operands are convertible to numbers
            return valueOf(val + rhs.checkdouble());
        }
    }

    @Override
    public LuaValue add(double rhs) {
        return valueOf(checkarith() + rhs);
    }

    @Override
    public LuaValue add(int rhs) {
        return valueOf(checkarith() + rhs);
    }

    @Override
    public LuaValue sub(LuaValue rhs) {
        double val = scannumber(10);
        if (Double.isNaN(val) || !rhs.isnumber()) {
            return super.sub(rhs);
        } else {
            // Special case: both operands are convertible to numbers
            return valueOf(val - rhs.checkdouble());
        }
    }

    @Override
    public LuaValue sub(double rhs) {
        return valueOf(checkarith() - rhs);
    }

    @Override
    public LuaValue sub(int rhs) {
        return valueOf(checkarith() - rhs);
    }

    @Override
    public LuaValue subFrom(double lhs) {
        return valueOf(lhs - checkarith());
    }

    @Override
    public LuaValue mul(LuaValue rhs) {
        double val = scannumber(10);
        if (Double.isNaN(val) || !rhs.isnumber()) {
            return super.mul(rhs);
        } else {
            // Special case: both operands are convertible to numbers
            return valueOf(val * rhs.checkdouble());
        }
    }

    @Override
    public LuaValue mul(double rhs) {
        return valueOf(checkarith() * rhs);
    }

    @Override
    public LuaValue mul(int rhs) {
        return valueOf(checkarith() * rhs);
    }

    @Override
    public LuaValue pow(LuaValue rhs) {
        double val = scannumber(10);
        if (Double.isNaN(val) || !rhs.isnumber()) {
            return super.pow(rhs);
        } else {
            // Special case: both operands are convertible to numbers
            return valueOf(LuaDouble.dpow(val, rhs.checkdouble()));
        }
    }

    @Override
    public LuaValue pow(double rhs) {
        return valueOf(LuaDouble.dpow(checkarith(), rhs));
    }

    @Override
    public LuaValue pow(int rhs) {
        return valueOf(LuaDouble.dpow(checkarith(), rhs));
    }

    @Override
    public LuaValue powWith(double lhs) {
        return valueOf(LuaDouble.dpow(lhs, checkarith()));
    }

    @Override
    public LuaValue powWith(int lhs) {
        return valueOf(LuaDouble.dpow(lhs, checkarith()));
    }

    @Override
    public LuaValue div(LuaValue rhs) {
        double val = scannumber(10);
        if (Double.isNaN(val) || !rhs.isnumber()) {
            return super.div(rhs);
        } else {
            // Special case: both operands are convertible to numbers
            return valueOf(val / rhs.checkdouble());
        }
    }

    @Override
    public LuaValue div(double rhs) {
        return LuaDouble.ddiv(checkarith(), rhs);
    }

    @Override
    public LuaValue div(int rhs) {
        return LuaDouble.ddiv(checkarith(), rhs);
    }

    @Override
    public LuaValue divInto(double lhs) {
        return LuaDouble.ddiv(lhs, checkarith());
    }

    @Override
    public LuaValue mod(LuaValue rhs) {
        double val = scannumber(10);
        if (Double.isNaN(val) || !rhs.isnumber()) {
            return super.mod(rhs);
        } else {
            // Special case: both operands are convertible to numbers
            return LuaDouble.dmod(val, rhs.checkdouble());
        }
    }

    @Override
    public LuaValue mod(double rhs) {
        return LuaDouble.dmod(checkarith(), rhs);
    }

    @Override
    public LuaValue mod(int rhs) {
        return LuaDouble.dmod(checkarith(), rhs);
    }

    @Override
    public LuaValue modFrom(double lhs) {
        return LuaDouble.dmod(lhs, checkarith());
    }

    // relational operators, these only work with other strings
    @Override
    public LuaValue lt(LuaValue rhs) {
        return rhs.strcmp(this) > 0 ? TRUE : FALSE;
    }

    @Override
    public boolean lt_b(LuaValue rhs) {
        return rhs.strcmp(this) > 0;
    }

    @Override
    public boolean lt_b(int rhs) {
        typerror("attempt to compare string with number");
        return false;
    }

    @Override
    public boolean lt_b(double rhs) {
        typerror("attempt to compare string with number");
        return false;
    }

    @Override
    public LuaValue lteq(LuaValue rhs) {
        return rhs.strcmp(this) >= 0 ? TRUE : FALSE;
    }

    @Override
    public boolean lteq_b(LuaValue rhs) {
        return rhs.strcmp(this) >= 0;
    }

    @Override
    public boolean lteq_b(int rhs) {
        typerror("attempt to compare string with number");
        return false;
    }

    @Override
    public boolean lteq_b(double rhs) {
        typerror("attempt to compare string with number");
        return false;
    }

    @Override
    public LuaValue gt(LuaValue rhs) {
        return rhs.strcmp(this) < 0 ? TRUE : FALSE;
    }

    @Override
    public boolean gt_b(LuaValue rhs) {
        return rhs.strcmp(this) < 0;
    }

    @Override
    public boolean gt_b(int rhs) {
        typerror("attempt to compare string with number");
        return false;
    }

    @Override
    public boolean gt_b(double rhs) {
        typerror("attempt to compare string with number");
        return false;
    }

    @Override
    public LuaValue gteq(LuaValue rhs) {
        return rhs.strcmp(this) <= 0 ? TRUE : FALSE;
    }

    @Override
    public boolean gteq_b(LuaValue rhs) {
        return rhs.strcmp(this) <= 0;
    }

    @Override
    public boolean gteq_b(int rhs) {
        typerror("attempt to compare string with number");
        return false;
    }

    @Override
    public boolean gteq_b(double rhs) {
        typerror("attempt to compare string with number");
        return false;
    }

    // concatenation
    @Override
    public LuaValue concat(LuaValue rhs) {
        return rhs.concatTo(this);
    }

    @Override
    public Buffer concat(Buffer rhs) {
        return rhs.concatTo(this);
    }

    @Override
    public LuaValue concatTo(LuaNumber lhs) {
        return concatTo(lhs.strvalue());
    }

    @Override
    public LuaValue concatTo(LuaString lhs) {
        SharedByteAlloc sba = SharedByteAlloc.getInstance();
        int len = lhs.strLength + this.strLength;
        int offset = sba.reserve(len);
        byte[] b = sba.getReserved();
        System.arraycopy(lhs.strBytes, lhs.strOffset, b, offset, lhs.strLength);
        System.arraycopy(this.strBytes, this.strOffset, b, offset + lhs.strLength, this.strLength);
        return new LuaString(b, offset, len);
    }

    // string comparison
    @Override
    public int strcmp(LuaValue lhs) {
        return -lhs.strcmp(this);
    }

    @Override
    public int strcmp(LuaString rhs) {
        for (int i = 0, j = 0; i < strLength && j < rhs.strLength; ++i, ++j) {
            if (strBytes[strOffset + i] != rhs.strBytes[rhs.strOffset + j]) {
                return strBytes[strOffset + i] - rhs.strBytes[rhs.strOffset + j];
            }
        }
        return strLength - rhs.strLength;
    }

    /** Check for number in arithmetic, or throw aritherror. */
    private double checkarith() {
        double d = scannumber(10);
        if (Double.isNaN(d)) {
            aritherror();
        }
        return d;
    }

    @Override
    public int checkint() {
        return (int) (long) checkdouble();
    }

    @Override
    public LuaInteger checkinteger() {
        return valueOf(checkint());
    }

    @Override
    public long checklong() {
        return (long) checkdouble();
    }

    @Override
    public double checkdouble() {
        double d = scannumber(10);
        if (Double.isNaN(d)) {
            argerror("number");
        }
        return d;
    }

    @Override
    public LuaNumber checknumber() {
        return valueOf(checkdouble());
    }

    @Override
    public LuaNumber checknumber(String msg) {
        double d = scannumber(10);
        if (Double.isNaN(d)) {
            argerror("number");
        }
        return valueOf(d);
    }

    @Override
    public LuaValue tonumber() {
        return tonumber(10);
    }

    /**
     * Convert to a number using a supplied base, or NIL if it can't be converted.
     *
     * @param base the base to use, such as 10
     * @return IntValue, DoubleValue, or NIL depending on the content of the string.
     * @see LuaValue#tonumber()
     */
    public LuaValue tonumber(int base) {
        double d = scannumber(base);
        return Double.isNaN(d) ? NIL : valueOf(d);
    }

    @Override
    public boolean isnumber() {
        double d = scannumber(10);
        return !Double.isNaN(d);
    }

    @Override
    public boolean isint() {
        double d = scannumber(10);
        if (Double.isNaN(d)) {
            return false;
        }
        int i = (int) d;
        return i == d;
    }

    @Override
    public boolean islong() {
        double d = scannumber(10);
        if (Double.isNaN(d)) {
            return false;
        }
        long l = (long) d;
        return l == d;
    }

    @Override
    public byte tobyte() {
        return (byte) toint();
    }

    @Override
    public char tochar() {
        return (char) toint();
    }

    @Override
    public double todouble() {
        double d = scannumber(10);
        return Double.isNaN(d) ? 0 : d;
    }

    @Override
    public float tofloat() {
        return (float) todouble();
    }

    @Override
    public int toint() {
        return (int) tolong();
    }

    @Override
    public long tolong() {
        return (long) todouble();
    }

    @Override
    public short toshort() {
        return (short) toint();
    }

    @Override
    public double optdouble(double defval) {
        return checknumber().checkdouble();
    }

    @Override
    public int optint(int defval) {
        return checknumber().checkint();
    }

    @Override
    public LuaInteger optinteger(LuaInteger defval) {
        return checknumber().checkinteger();
    }

    @Override
    public long optlong(long defval) {
        return checknumber().checklong();
    }

    @Override
    public LuaNumber optnumber(LuaNumber defval) {
        return checknumber().checknumber();
    }

    @Override
    public LuaString optstring(LuaString defval) {
        return this;
    }

    @Override
    public LuaValue tostring() {
        return this;
    }

    @Override
    public String optjstring(String defval) {
        return tojstring();
    }

    @Override
    public LuaString strvalue() {
        return this;
    }

    public LuaString substring(int beginIndex, int endIndex) {
        return new LuaString(strBytes, strOffset + beginIndex, endIndex - beginIndex);
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) { //If the calculated hashCode by chance is also zero, caching doesn't work
            int h = strLength; //seed
            int step = (strLength >> 5) + 1; //If string is too long, don't hash all its chars
            for (int l1 = strLength; l1 >= step; l1 -= step) {
                h = h ^ ((h << 5) + (h >> 2) + (strBytes[strOffset + l1 - 1] & 0x0FF));
            }
            hashCode = h;
        }
        return hashCode;
    }

    // object comparison, used in key comparison
    @Override
    public boolean equals(Object o) {
        if (o instanceof LuaString) {
            return raweq((LuaString) o);
        }
        return false;
    }

    public static boolean equals(LuaString a, int i, LuaString b, int j, int n) {
        return equals(a.strBytes, a.strOffset + i, b.strBytes, b.strOffset + j, n);
    }

    public static boolean equals(byte[] a, int i, byte[] b, int j, int n) {
        if (a.length < i + n || b.length < j + n) {
            return false;
        }
        while (--n >= 0) {
            if (a[i++] != b[j++]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean eq_b(LuaValue val) {
        return val.raweq(this);
    }

    // equality w/o metatable processing
    @Override
    public boolean raweq(LuaValue val) {
        return val.raweq(this);
    }

    @Override
    public boolean raweq(LuaString s) {
        if (this == s) {
            return true;
        }
        if (s.strLength != strLength) {
            return false;
        }
        if (s.strBytes == strBytes && s.strOffset == strOffset) {
            return true;
        }
        if (s.hashCode() != hashCode()) {
            return false;
        }
        return equals(strBytes, strOffset, s.strBytes, s.strOffset, strLength);
    }

    public void write(OutputStream writer, int i, int len) throws IOException {
        writer.write(strBytes, strOffset + i, len);
    }

    public void write(DataOutput out, int i, int len) throws IOException {
        out.write(strBytes, strOffset + i, len);
    }

    @Override
    public LuaValue len() {
        return LuaInteger.valueOf(strLength);
    }

    @Override
    public int length() {
        return strLength;
    }

    @Override
    public int rawlen() {
        return strLength;
    }

    public int luaByte(int index) {
        if (index < 0 || index >= strLength) {
            throw new IndexOutOfBoundsException("index=" + index + ", length=" + strLength);
        }
        return strBytes[strOffset + index] & 0x0FF;
    }

    public int charAt(int index) {
        return luaByte(index);
    }

    @Override
    public String checkjstring() {
        return tojstring();
    }

    @Override
    public LuaString checkstring() {
        return this;
    }

    /**
     * Convert value to an input stream.
     *
     * @return {@link InputStream} whose data matches the bytes in this
     *         {@link LuaString}
     */
    public InputStream toInputStream() {
        return new ByteArrayInputStream(strBytes, strOffset, strLength);
    }

    /**
     * Copy the bytes of the string into the given byte array.
     *
     * @param offset offset from which to copy
     * @param bytes destination byte array
     * @param arrayOffset offset in destination
     * @param len number of bytes to copy
     */
    public void copyInto(int offset, byte[] bytes, int arrayOffset, int len) {
        System.arraycopy(strBytes, strOffset + offset, bytes, arrayOffset, len);
    }

    /**
     * Java version of strpbrk - find index of any byte that in an accept
     * string.
     *
     * @param accept {@link LuaString} containing characters to look for.
     * @return index of first match in the {@code accept} string, or -1 if not
     *         found.
     */
    public int indexOfAny(LuaString accept) {
        final int ilimit = strOffset + strLength;
        final int jlimit = accept.strOffset + accept.strLength;
        for (int i = strOffset; i < ilimit; ++i) {
            for (int j = accept.strOffset; j < jlimit; ++j) {
                if (strBytes[i] == accept.strBytes[j]) {
                    return i - strOffset;
                }
            }
        }
        return -1;
    }

    /**
     * Find the index of a byte starting at a point in this string
     *
     * @param b the byte to look for
     * @param start the first index in the string
     * @return index of first match found, or -1 if not found.
     */
    public int indexOf(byte b, int start) {
        for (int i = 0, j = strOffset + start; i < strLength; ++i) {
            if (strBytes[j++] == b) {
                return i + start;
            }
        }
        return -1;
    }

    /**
     * Find the index of a string starting at a point in this string
     *
     * @param s the string to search for
     * @param start the first index in the string
     * @return index of first match found, or -1 if not found.
     */
    public int indexOf(LuaString s, int start) {
        final int slen = s.length();
        final int limit = strOffset + strLength - slen;
        for (int i = strOffset + start; i <= limit; ++i) {
            if (equals(strBytes, i, s.strBytes, s.strOffset, slen)) {
                return i - strOffset;
            }
        }
        return -1;
    }

    /**
     * Find the last index of a string in this string
     *
     * @param s the string to search for
     * @return index of last match found, or -1 if not found.
     */
    public int lastIndexOf(LuaString s) {
        final int slen = s.length();
        final int limit = strOffset + strLength - slen;
        for (int i = limit; i >= strOffset; --i) {
            if (equals(strBytes, i, s.strBytes, s.strOffset, slen)) {
                return i - strOffset;
            }
        }
        return -1;
    }

    /**
     * Convert to Java String interpreting as utf8 characters.
     *
     * @param bytes byte array in UTF8 encoding to convert
     * @param offset starting index in byte array
     * @param length number of bytes to convert
     * @return Java String corresponding to the value of bytes interpreted using UTF8
     * @see #lengthAsUtf8(char[])
     * @see #encodeToUtf8(char[], byte[], int)
     * @see #isValidUtf8()
     */
    public static String decodeAsUtf8(byte[] bytes, int offset, int length) {
        int utf16L = 0;
        for (int n = 0; n < length; n++) {
            switch (0xE0 & bytes[offset + n]) {
            case 0xE0:
                n += 2;
                break;
            case 0xC0:
                n++;
                break;
            }
            utf16L++;
        }

        try {
            char[] chars = new char[utf16L];
            int i = offset;
            int j = offset + length;
            for (int n = 0; i < j; n++) {
                byte b = bytes[i++];
                if (b >= 0 || i >= j) {
                    chars[n] = (char)b;
                } else if (b < -32 || i + 1 >= j) {
                    chars[n] = (char)(((b & 0x3f) << 6) | (bytes[i++] & 0x3f));
                } else if (b < -16 || i + 2 >= j) {
                    chars[n] = (char)(((b & 0xf) << 12) | ((bytes[i++] & 0x3f) << 6) | (bytes[i++] & 0x3f));
                } else { //Requires more than one UTF-16 char
                    CharBuffer cbuf = UTF8_DECODER.decode(ByteBuffer.wrap(bytes, offset, length));
                    return cbuf.toString();
                }
            }

            return new String(chars);
        } catch (CharacterCodingException e) {
            return new String(bytes, offset, length);
        }
    }

    /**
     * Count the number of bytes required to encode the string as UTF-8.
     *
     * @param chars Array of unicode characters to be encoded as UTF-8
     * @return count of bytes needed to encode using UTF-8
     * @see #encodeToUtf8(char[], byte[], int)
     * @see #decodeAsUtf8(byte[], int, int)
     * @see #isValidUtf8()
     */
    public static int lengthAsUtf8(char[] chars) {
        int b = chars.length;
        for (int i = b; --i >= 0;) {
            char c = chars[i];
            if (c >= 0x80) {
                b += (c >= 0x800) ? 2 : 1;
            }
        }
        return b;
    }

    /**
     * Encode the given Java string as UTF-8 bytes, writing the result to bytes
     * starting at offset.
     * <p>
     * The string should be measured first with lengthAsUtf8 to make sure the
     * given byte array is large enough.
     *
     * @param chars Array of unicode characters to be encoded as UTF-8
     * @param bytes byte array to hold the result
     * @param off offset into the byte array to start writing
     * @see #lengthAsUtf8(char[])
     * @see #decodeAsUtf8(byte[], int, int)
     * @see #isValidUtf8()
     */
    public static void encodeToUtf8(char[] chars, byte[] bytes, int off) {
        final int n = chars.length;
        char c;
        for (int i = 0, j = off; i < n; i++) {
            if ((c = chars[i]) < 0x80) {
                bytes[j++] = (byte) c;
            } else if (c < 0x800) {
                bytes[j++] = (byte) (0xC0 | ((c >> 6) & 0x1f));
                bytes[j++] = (byte) (0x80 | (c & 0x3f));
            } else {
                bytes[j++] = (byte) (0xE0 | ((c >> 12) & 0x0f));
                bytes[j++] = (byte) (0x80 | ((c >> 6) & 0x3f));
                bytes[j++] = (byte) (0x80 | (c & 0x3f));
            }
        }
    }

    /**
     * Check that a byte sequence is valid UTF-8.
     *
     * @return true if it is valid UTF-8, otherwise false
     * @see #lengthAsUtf8(char[])
     * @see #encodeToUtf8(char[], byte[], int)
     * @see #decodeAsUtf8(byte[], int, int)
     */
    public boolean isValidUtf8() {
        int j = strOffset + strLength;
        for (int i = strOffset; i < j;) {
            int c = strBytes[i++];
            if (c >= 0) {
                continue;
            }
            if (((c & 0xE0) == 0xC0) && i < j && (strBytes[i++] & 0xC0) == 0x80) {
                continue;
            }
            if (((c & 0xF0) == 0xE0) && i + 1 < j && (strBytes[i++] & 0xC0) == 0x80
                    && (strBytes[i++] & 0xC0) == 0x80) {
                continue;
            }
            return false;
        }
        return true;
    }

    // --------------------- number conversion -----------------------

    /**
     * Convert to a number in a base, or return Double.NaN if not a number.
     *
     * @param base the base to use, such as 10
     * @return double value if conversion is valid, or Double.NaN if not
     */
    public double scannumber(int base) {
        if (base >= 2 && base <= 36) {
            int i = strOffset;
            int j = strOffset + strLength;
            while (i < j && Character.isWhitespace(strBytes[i])) {
                ++i;
            }
            while (i < j && Character.isWhitespace(strBytes[j - 1])) {
                --j;
            }
            if (i >= j) {
                return Double.NaN;
            }
            if ((base == 10 || base == 16)
                    && (strBytes[i] == '0' && i + 1 < j && (strBytes[i + 1] == 'x' || strBytes[i + 1] == 'X'))) {
                base = 16;
                i += 2;
            }
            double l = scanlong(base, i, j);
            return Double.isNaN(l) && base == 10 ? scandouble(i, j) : l;
        }

        return Double.NaN;
    }

    /**
     * Scan and convert a long value, or return Double.NaN if not found.
     *
     * @param base the base to use, such as 10
     * @param start the index to start searching from
     * @param end the first index beyond the search range
     * @return double value if conversion is valid, or Double.NaN if not
     */
    private double scanlong(int base, int start, int end) {
        long x = 0;
        boolean neg = (strBytes[start] == '-');
        for (int i = (neg ? start + 1 : start); i < end; i++) {
            int digit = strBytes[i]
                    - (base <= 10 || (strBytes[i] >= '0' && strBytes[i] <= '9') ? '0' : strBytes[i] >= 'A'
                            && strBytes[i] <= 'Z' ? ('A' - 10) : ('a' - 10));
            if (digit < 0 || digit >= base) {
                return Double.NaN;
            }
            x = x * base + digit;
        }
        return neg ? -x : x;
    }

    /**
     * Scan and convert a double value, or return Double.NaN if not a double.
     *
     * @param start the index to start searching from
     * @param end the first index beyond the search range
     * @return double value if conversion is valid, or Double.NaN if not
     */
    private double scandouble(int start, int end) {
        if (end > start + 64) {
            end = start + 64;
        }
        for (int i = start; i < end; i++) {
            switch (strBytes[i]) {
            case '-':
            case '+':
            case '.':
            case 'e':
            case 'E':
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                break;
            default:
                return Double.NaN;
            }
        }
        char[] c = new char[end - start];
        for (int i = start; i < end; i++) {
            c[i - start] = (char) strBytes[i];
        }
        try {
            return Double.parseDouble(new String(c));
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(strLength);
        out.write(strBytes, strOffset, strLength);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        strLength = in.readInt();
        SharedByteAlloc sba = SharedByteAlloc.getInstance();
        strOffset = sba.reserve(strLength);
        strBytes = sba.getReserved();
        in.readFully(strBytes, strOffset, strLength);
    }

    public static void assertValidStringLength(int len) {
        if (len < 0) {
            throw new LuaError("String length may not be negative: " + len);
        }

        if (len > MAX_STRING_LENGTH) {
            // Note: Lua test suite explicitly checks for the word 'overflow' in this error message
            throw new LuaError("String length overflow: " + len + " > " + MAX_STRING_LENGTH);
        }
    }

}
