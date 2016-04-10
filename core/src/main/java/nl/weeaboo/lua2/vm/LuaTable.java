/*******************************************************************************
 * Copyright (c) 2009 Luaj.org. All rights reserved.
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
import static nl.weeaboo.lua2.vm.LuaConstants.LEN;
import static nl.weeaboo.lua2.vm.LuaConstants.NEWINDEX;
import static nl.weeaboo.lua2.vm.LuaConstants.NONE;
import static nl.weeaboo.lua2.vm.LuaConstants.NOVALS;
import static nl.weeaboo.lua2.vm.LuaConstants.TBOOLEAN;
import static nl.weeaboo.lua2.vm.LuaConstants.TLIGHTUSERDATA;
import static nl.weeaboo.lua2.vm.LuaConstants.TNUMBER;
import static nl.weeaboo.lua2.vm.LuaConstants.TSTRING;
import static nl.weeaboo.lua2.vm.LuaConstants.TTABLE;
import static nl.weeaboo.lua2.vm.LuaConstants.TTHREAD;
import static nl.weeaboo.lua2.vm.LuaConstants.TUSERDATA;
import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

import nl.weeaboo.lua2.io.DelayedReader;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.io.LuaSerializer;

/**
 * Subclass of {@link LuaValue} for representing lua tables.
 * <p>
 * Almost all API's implemented in {@link LuaTable} are defined and documented in {@link LuaValue}.
 * <p>
 * If a table is needed, the one of the type-checking functions can be used such as {@link #istable()},
 * {@link #checktable()}, or {@link #opttable(LuaTable)}
 * <p>
 * The main table operations are defined on {@link LuaValue} for getting and setting values with and without
 * metatag processing:
 * <ul>
 * <li>{@link #get(LuaValue)}</li>
 * <li>{@link #set(LuaValue,LuaValue)}</li>
 * <li>{@link #rawget(LuaValue)}</li>
 * <li>{@link #rawset(LuaValue,LuaValue)}</li>
 * <li>plus overloads such as {@link #get(String)}, {@link #get(int)}, and so on</li>
 * </ul>
 * <p>
 * To iterate over key-value pairs from Java, use
 *
 * <pre>
 * LuaValue k = LuaValue.NIL;
 * while ( true ) {
 *    Varargs n = table.next(k);
 *    if ( (k = n.arg1()).isnil() )
 *       break;
 *    LuaValue v = n.arg(2)
 *    process( k, v )
 * }
 * </pre>
 *
 * <p>
 * As with other types, {@link LuaTable} instances should be constructed via one of the table constructor
 * methods on {@link LuaValue}:
 * <ul>
 * <li>{@link LuaValue#tableOf()} empty table</li>
 * <li>{@link LuaValue#tableOf(int, int)} table with capacity</li>
 * <li>{@link LuaValue#listOf(LuaValue[])} initialize array part</li>
 * <li>{@link LuaValue#listOf(LuaValue[], Varargs)} initialize array part</li>
 * <li>{@link LuaValue#tableOf(LuaValue[])} initialize named hash part</li>
 * <li>{@link LuaValue#tableOf(Varargs, int)} initialize named hash part</li>
 * <li>{@link LuaValue#tableOf(LuaValue[], LuaValue[])} initialize array and named parts</li>
 * <li>{@link LuaValue#tableOf(LuaValue[], LuaValue[], Varargs)} initialize array and named parts</li>
 * </ul>
 *
 * @see LuaValue
 */
@LuaSerializable
public class LuaTable extends LuaValue implements Metatable, Externalizable {

    private static final int MIN_HASH_CAPACITY = 2;
    private static final LuaString N = valueOf("n");

    private static final Slot[] NOBUCKETS = {};

    /** the array values */
    protected LuaValue[] array;

    /** the hash part */
    protected Slot[] hash;

    /** the number of hash entries */
    protected int hashEntries;

    /** metatable for this table, or null */
    protected Metatable m_metatable;

    /** Construct empty table */
    public LuaTable() {
        array = NOVALS;
        hash = NOBUCKETS;
    }

    /**
     * Construct table with preset capacity.
     *
     * @param narray capacity of array part
     * @param nhash capacity of hash part
     */
    public LuaTable(int narray, int nhash) {
        presize(narray, nhash);
    }

    /**
     * Construct table with named and unnamed parts.
     *
     * @param named Named elements in order {@code key-a, value-a, key-b, value-b, ... }
     * @param unnamed Unnamed elements in order {@code value-1, value-2, ... }
     * @param lastarg Additional unnamed values beyond {@code unnamed.length}
     */
    public LuaTable(LuaValue[] named, LuaValue[] unnamed, Varargs lastarg) {
        int nn = (named != null ? named.length : 0);
        int nu = (unnamed != null ? unnamed.length : 0);
        int nl = (lastarg != null ? lastarg.narg() : 0);
        presize(nu + nl, nn >> 1);
        for (int i = 0; i < nu; i++){
            rawset(i + 1, unnamed[i]);
        }
        if (lastarg != null) {
            for (int i = 1, n = lastarg.narg(); i <= n; ++i) {
                rawset(nu + i, lastarg.arg(i));
            }
        }
        for (int i = 0; i < nn; i += 2) {
            if (!named[i + 1].isnil()) {
                rawset(named[i], named[i + 1]);
            }
        }
    }

    /**
     * Construct table of unnamed elements.
     *
     * @param varargs Unnamed elements in order {@code value-1, value-2, ... }
     */
    public LuaTable(Varargs varargs) {
        this(varargs, 1);
    }

    /**
     * Construct table of unnamed elements.
     *
     * @param varargs Unnamed elements in order {@code value-1, value-2, ... }
     * @param firstarg the index in varargs of the first argument to include in the table
     */
    public LuaTable(Varargs varargs, int firstarg) {
        int nskip = firstarg - 1;
        int n = Math.max(varargs.narg() - nskip, 0);
        presize(n, 1);
        set(N, valueOf(n));
        for (int i = 1; i <= n; i++) {
            set(i, varargs.arg(i + nskip));
        }
    }

    /** Check that subclasses override writeExternal */
    private boolean checkOverridden() {
        Class<?> c = getClass();
        if (c == LuaTable.class) {
            return true;
        }

        try {
            if (c.getDeclaredMethod("writeExternal", ObjectOutput.class) != null) {
                return true;
            }
        } catch (NoSuchMethodException e) {
            // Ignore
        } catch (SecurityException e) {
            // Assume the best
            return true;
        }
        throw new RuntimeException("Subclass of LuaTable doesn't implement writeExternal()");
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        checkOverridden();

        LuaSerializer ls = LuaSerializer.getCurrent();

        out.writeObject(m_metatable);

        out.writeInt(array.length);
        int lastNonNil = array.length - 1;
        while (lastNonNil >= 0 && array[lastNonNil] == null) {
            lastNonNil--;
        }
        out.writeInt(lastNonNil + 1);
        for (int n = 0; n <= lastNonNil; n++) {
            out.writeObject(array[n]);
        }

        out.writeInt(hashEntries);
        // Use writeDelayed to reduce recursion depth
        if (ls != null) {
            ls.writeDelayed(hash);
        } else {
            out.writeObject(hash);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        LuaSerializer ls = LuaSerializer.getCurrent();

        m_metatable = (Metatable)in.readObject();

        int arrayLength = in.readInt();
        int arrayUsed = in.readInt();
        array = new LuaValue[arrayLength];
        for (int n = 0; n < arrayUsed; n++) {
            array[n] = (LuaValue)in.readObject();
        }

        hashEntries = in.readInt();

        if (ls != null) {
            ls.readDelayed(new DelayedReader() {
                @Override
                public void onRead(Object obj) {
                    hash = (Slot[])obj;
                }
            });
        } else {
            hash = (Slot[])in.readObject();
        }
    }

    @Override
    public int type() {
        return LuaConstants.TTABLE;
    }

    @Override
    public String typename() {
        return "table";
    }

    @Override
    public boolean istable() {
        return true;
    }

    @Override
    public LuaTable checktable() {
        return this;
    }

    @Override
    public LuaTable opttable(LuaTable defval) {
        return this;
    }

    @Override
    public void presize(int narray) {
        if (narray > array.length) {
            array = resize(array, 1 << log2(narray));
        }
    }

    public void presize(int narray, int nhash) {
        if (nhash > 0 && nhash < MIN_HASH_CAPACITY) nhash = MIN_HASH_CAPACITY;
        // Size of both parts must be a power of two.
        array = (narray > 0 ? new LuaValue[1 << log2(narray)] : NOVALS);
        hash = (nhash > 0 ? new Slot[1 << log2(nhash)] : NOBUCKETS);
        hashEntries = 0;
    }

    /** Resize the table */
    private static LuaValue[] resize(LuaValue[] old, int n) {
        LuaValue[] v = new LuaValue[n];
        System.arraycopy(old, 0, v, 0, old.length);
        return v;
    }

    /**
     * Get the length of the array part of the table.
     *
     * @return length of the array part, does not relate to count of objects in the table.
     */
    protected int getArrayLength() {
        return array.length;
    }

    /**
     * Get the length of the hash part of the table.
     *
     * @return length of the hash part, does not relate to count of objects in the table.
     */
    protected int getHashLength() {
        return hash.length;
    }

    @Override
    public LuaValue getmetatable() {
        return (m_metatable != null) ? m_metatable.toLuaValue() : null;
    }

    @Override
    public LuaValue setmetatable(LuaValue metatable) {
        boolean oldWeakKeys = m_metatable != null && m_metatable.useWeakKeys();
        boolean oldWeakValues = m_metatable != null && m_metatable.useWeakValues();

        m_metatable = metatableOf(metatable);

        boolean newWeakKeys = m_metatable != null && m_metatable.useWeakKeys();
        boolean newWeakValues = m_metatable != null && m_metatable.useWeakValues();

        if (oldWeakKeys != newWeakKeys || oldWeakValues != newWeakValues) {
            // force a rehash
            rehash(0);
        }
        return this;
    }

    @Override
    public LuaValue get(int key) {
        LuaValue v = rawget(key);
        return v.isnil() && m_metatable != null ? gettable(this, valueOf(key)) : v;
    }

    @Override
    public LuaValue get(LuaValue key) {
        LuaValue v = rawget(key);
        return v.isnil() && m_metatable != null ? gettable(this, key) : v;
    }

    @Override
    public LuaValue rawget(int key) {
        if (key > 0 && key <= array.length) {
            LuaValue v = m_metatable == null ? array[key - 1] : m_metatable.arrayget(array, key - 1);
            return v != null ? v : NIL;
        }
        return hashget(LuaInteger.valueOf(key));
    }

    @Override
    public LuaValue rawget(LuaValue key) {
        if (key.isinttype()) {
            int ikey = key.toint();
            if (ikey > 0 && ikey <= array.length) {
                LuaValue v = m_metatable == null ? array[ikey - 1] : m_metatable.arrayget(array, ikey - 1);
                return v != null ? v : NIL;
            }
        }
        return hashget(key);
    }

    protected LuaValue hashget(LuaValue key) {
        if (hashEntries > 0) {
            for (Slot slot = hash[hashSlot(key)]; slot != null; slot = slot.rest()) {
                StrongSlot foundSlot;
                if ((foundSlot = slot.find(key)) != null) {
                    return foundSlot.value();
                }
            }
        }
        return NIL;
    }

    @Override
    public void set(int key, LuaValue value) {
        if (m_metatable == null || !rawget(key).isnil() || !settable(this, LuaInteger.valueOf(key), value)) {
            rawset(key, value);
        }
    }

    /** caller must ensure key is not nil */
    @Override
    public void set(LuaValue key, LuaValue value) {
        if (!key.isvalidkey() && !metatag(NEWINDEX).isfunction()) {
            typerror("table index");
        }
        if (m_metatable == null || !rawget(key).isnil() || !settable(this, key, value)) {
            rawset(key, value);
        }
    }

    @Override
    public void rawset(int key, LuaValue value) {
        if (!arrayset(key, value)) hashset(LuaInteger.valueOf(key), value);
    }

    /** caller must ensure key is not nil */
    @Override
    public void rawset(LuaValue key, LuaValue value) {
        if (!key.isinttype() || !arrayset(key.toint(), value)) hashset(key, value);
    }

    /** Set an array element */
    private boolean arrayset(int key, LuaValue value) {
        if (key > 0 && key <= array.length) {
            array[key - 1] = value.isnil() ? null : (m_metatable != null ? m_metatable.wrap(value) : value);
            return true;
        }
        return false;
    }

    /**
     * Remove the element at a position in a list-table
     *
     * @param pos the position to remove
     * @return The removed item, or {@link #NONE} if not removed
     */
    public LuaValue remove(int pos) {
        int n = rawlen();
        if (pos == 0) pos = n;
        else if (pos > n) return NONE;
        LuaValue v = rawget(pos);
        for (LuaValue r = v; !r.isnil();) {
            r = rawget(pos + 1);
            rawset(pos++, r);
        }
        return v.isnil() ? NONE : v;
    }

    /**
     * Insert an element at a position in a list-table
     *
     * @param pos the position to remove
     * @param value The value to insert
     */
    public void insert(int pos, LuaValue value) {
        if (pos == 0) pos = rawlen() + 1;
        while (!value.isnil()) {
            LuaValue v = rawget(pos);
            rawset(pos++, value);
            value = v;
        }
    }

    /**
     * Concatenate the contents of a table efficiently, using {@link Buffer}
     *
     * @param sep {@link LuaString} separater to apply between elements
     * @param i the first element index
     * @param j the last element index, inclusive
     * @return {@link LuaString} value of the concatenation
     */
    public LuaValue concat(LuaString sep, int i, int j) {
        Buffer sb = new Buffer();
        if (i <= j) {
            sb.append(get(i).checkstring());
            while (++i <= j) {
                sb.append(sep);
                sb.append(get(i).checkstring());
            }
        }
        return sb.tostring();
    }

    @Override
    public int length() {
        return m_metatable != null ? len().toint() : rawlen();
    }

    @Override
    public LuaValue len() {
        final LuaValue h = metatag(LEN);
        if (h.toboolean()) return h.call(this);
        return LuaInteger.valueOf(rawlen());
    }

    @Override
    public int rawlen() {
        int a = getArrayLength();
        int n = a + 1, m = 0;
        while (!rawget(n).isnil()) {
            m = n;
            n += a + getHashLength() + 1;
        }
        while (n > m + 1) {
            int k = (n + m) / 2;
            if (!rawget(k).isnil()) {
                m = k;
            } else {
                n = k;
            }
        }
        return m;
    }

    /**
     * Get the next element after a particular key in the table
     *
     * @return key,value or nil
     */
    @Override
    public Varargs next(LuaValue key) {
        int i = 0;
        do {
            // find current key index
            if (!key.isnil()) {
                if (key.isinttype()) {
                    i = key.toint();
                    if (i > 0 && i <= array.length) {
                        break;
                    }
                }
                if (hash.length == 0) error("invalid key to 'next'");
                i = hashSlot(key);
                boolean found = false;
                for (Slot slot = hash[i]; slot != null; slot = slot.rest()) {
                    if (found) {
                        StrongSlot nextEntry = slot.first();
                        if (nextEntry != null) {
                            return nextEntry.toVarargs();
                        }
                    } else if (slot.keyeq(key)) {
                        found = true;
                    }
                }
                if (!found) {
                    error("invalid key to 'next'");
                }
                i += 1 + array.length;
            }
        } while (false);

        // check array part
        for (; i < array.length; ++i) {
            if (array[i] != null) {
                LuaValue value = m_metatable == null ? array[i] : m_metatable.arrayget(array, i);
                if (value != null) {
                    return varargsOf(LuaInteger.valueOf(i + 1), value);
                }
            }
        }

        // check hash part
        for (i -= array.length; i < hash.length; ++i) {
            Slot slot = hash[i];
            while (slot != null) {
                StrongSlot first = slot.first();
                if (first != null) return first.toVarargs();
                slot = slot.rest();
            }
        }

        // nothing found, push nil, return nil.
        return NIL;
    }

    /**
     * Get the next element after a particular key in the contiguous array part of a table
     *
     * @return key,value or none
     */
    @Override
    public Varargs inext(LuaValue key) {
        int k = key.checkint() + 1;
        LuaValue v = rawget(k);
        return v.isnil() ? NONE : varargsOf(LuaInteger.valueOf(k), v);
    }

    /**
     * Set a hashtable value
     *
     * @param key key to set
     * @param value value to set
     */
    public void hashset(LuaValue key, LuaValue value) {
        if (value.isnil()) {
            hashRemove(key);
        } else {
            int index = 0;
            if (hash.length > 0) {
                index = hashSlot(key);
                for (Slot slot = hash[index]; slot != null; slot = slot.rest()) {
                    StrongSlot foundSlot;
                    if ((foundSlot = slot.find(key)) != null) {
                        hash[index] = hash[index].set(foundSlot, value);
                        return;
                    }
                }
            }
            if (checkLoadFactor()) {
                if (key.isinttype() && key.toint() > 0) {
                    // a rehash might make room in the array portion for this key.
                    rehash(key.toint());
                    if (arrayset(key.toint(), value)) {
                        return;
                    }
                } else {
                    rehash(-1);
                }
                index = hashSlot(key);
            }
            Slot entry = (m_metatable != null) ? m_metatable.entry(key, value) : defaultEntry(key, value);
            hash[index] = (hash[index] != null) ? hash[index].add(entry) : entry;
            ++hashEntries;
        }
    }

    public static int hashpow2(int hashCode, int mask) {
        return hashCode & mask;
    }

    public static int hashmod(int hashCode, int mask) {
        return (hashCode & 0x7FFFFFFF) % mask;
    }

    /**
     * Find the hashtable slot index to use.
     *
     * @param key the key to look for
     * @param hashMask N-1 where N is the number of hash slots (must be power of 2)
     * @return the slot index
     */
    public static int hashSlot(LuaValue key, int hashMask) {
        switch (key.type()) {
        case TNUMBER:
        case TTABLE:
        case TTHREAD:
        case TLIGHTUSERDATA:
        case TUSERDATA:
            return hashmod(key.hashCode(), hashMask);
        default:
            return hashpow2(key.hashCode(), hashMask);
        }
    }

    /**
     * Find the hashtable slot to use
     *
     * @param key key to look for
     * @return slot to use
     */
    private int hashSlot(LuaValue key) {
        return hashSlot(key, hash.length - 1);
    }

    private void hashRemove(LuaValue key) {
        if (hash.length == 0) {
            return;
        }

        int index = hashSlot(key);
        for (Slot slot = hash[index]; slot != null; slot = slot.rest()) {
            StrongSlot foundSlot;
            if ((foundSlot = slot.find(key)) != null) {
                hash[index] = hash[index].remove(foundSlot);
                --hashEntries;
                return;
            }
        }
    }

    private boolean checkLoadFactor() {
        return hashEntries >= hash.length;
    }

    private int countHashKeys() {
        int keys = 0;
        for (int i = 0; i < hash.length; ++i) {
            for (Slot slot = hash[i]; slot != null; slot = slot.rest()) {
                if (slot.first() != null) keys++;
            }
        }
        return keys;
    }

    private void dropWeakArrayValues() {
        for (int i = 0; i < array.length; ++i) {
            m_metatable.arrayget(array, i);
        }
    }

    private int countIntKeys(int[] nums) {
        int total = 0;
        int i = 1;

        // Count integer keys in array part
        for (int bit = 0; bit < 31; ++bit) {
            if (i > array.length) break;
            int j = Math.min(array.length, 1 << bit);
            int c = 0;
            while (i <= j) {
                if (array[i++ - 1] != null) c++;
            }
            nums[bit] = c;
            total += c;
        }

        // Count integer keys in hash part
        for (i = 0; i < hash.length; ++i) {
            for (Slot s = hash[i]; s != null; s = s.rest()) {
                int k;
                if ((k = s.arraykey(Integer.MAX_VALUE)) > 0) {
                    nums[log2(k)]++;
                    total++;
                }
            }
        }

        return total;
    }

    // Compute ceil(log2(x))
    static int log2(int x) {
        int lg = 0;
        x -= 1;
        if (x < 0)
            // 2^(-(2^31)) is approximately 0
            return Integer.MIN_VALUE;
        if ((x & 0xFFFF0000) != 0) {
            lg = 16;
            x >>>= 16;
        }
        if ((x & 0xFF00) != 0) {
            lg += 8;
            x >>>= 8;
        }
        if ((x & 0xF0) != 0) {
            lg += 4;
            x >>>= 4;
        }
        switch (x) {
        case 0x0:
            return 0;
        case 0x1:
            lg += 1;
            break;
        case 0x2:
            lg += 2;
            break;
        case 0x3:
            lg += 2;
            break;
        case 0x4:
            lg += 3;
            break;
        case 0x5:
            lg += 3;
            break;
        case 0x6:
            lg += 3;
            break;
        case 0x7:
            lg += 3;
            break;
        case 0x8:
            lg += 4;
            break;
        case 0x9:
            lg += 4;
            break;
        case 0xA:
            lg += 4;
            break;
        case 0xB:
            lg += 4;
            break;
        case 0xC:
            lg += 4;
            break;
        case 0xD:
            lg += 4;
            break;
        case 0xE:
            lg += 4;
            break;
        case 0xF:
            lg += 4;
            break;
        }
        return lg;
    }

    /*
     * newKey > 0 is next key to insert newKey == 0 means number of keys not changing (__mode changed) newKey
     * < 0 next key will go in hash part
     */
    private void rehash(int newKey) {
        if (m_metatable != null && (m_metatable.useWeakKeys() || m_metatable.useWeakValues())) {
            // If this table has weak entries, hashEntries is just an upper bound.
            hashEntries = countHashKeys();
            if (m_metatable.useWeakValues()) {
                dropWeakArrayValues();
            }
        }
        int[] nums = new int[32];
        int total = countIntKeys(nums);
        if (newKey > 0) {
            total++;
            nums[log2(newKey)]++;
        }

        // Choose N such that N <= sum(nums[0..log(N)]) < 2N
        int keys = nums[0];
        int newArraySize = 0;
        for (int log = 1; log < 32; ++log) {
            keys += nums[log];
            if (total * 2 < 1 << log) {
                // Not enough integer keys.
                break;
            } else if (keys >= (1 << (log - 1))) {
                newArraySize = 1 << log;
            }
        }

        final LuaValue[] oldArray = array;
        final Slot[] oldHash = hash;
        final LuaValue[] newArray;
        final Slot[] newHash;

        // Copy existing array entries and compute number of moving entries.
        int movingToArray = 0;
        if (newKey > 0 && newKey <= newArraySize) {
            movingToArray--;
        }
        if (newArraySize != oldArray.length) {
            newArray = new LuaValue[newArraySize];
            if (newArraySize > oldArray.length) {
                for (int i = log2(oldArray.length + 1), j = log2(newArraySize) + 1; i < j; ++i) {
                    movingToArray += nums[i];
                }
            } else if (oldArray.length > newArraySize) {
                for (int i = log2(newArraySize + 1), j = log2(oldArray.length) + 1; i < j; ++i) {
                    movingToArray -= nums[i];
                }
            }
            System.arraycopy(oldArray, 0, newArray, 0, Math.min(oldArray.length, newArraySize));
        } else {
            newArray = array;
        }

        final int newHashSize = hashEntries - movingToArray + ((newKey < 0 || newKey > newArraySize) ? 1 : 0); // Make
                                                                                                               // room
                                                                                                               // for
                                                                                                               // the
                                                                                                               // new
                                                                                                               // entry
        final int oldCapacity = oldHash.length;
        final int newCapacity;
        final int newHashMask;

        if (newHashSize > 0) {
            // round up to next power of 2.
            newCapacity = (newHashSize < MIN_HASH_CAPACITY) ? MIN_HASH_CAPACITY : 1 << log2(newHashSize);
            newHashMask = newCapacity - 1;
            newHash = new Slot[newCapacity];
        } else {
            newCapacity = 0;
            newHashMask = 0;
            newHash = NOBUCKETS;
        }

        // Move hash buckets
        for (int i = 0; i < oldCapacity; ++i) {
            for (Slot slot = oldHash[i]; slot != null; slot = slot.rest()) {
                int k;
                if ((k = slot.arraykey(newArraySize)) > 0) {
                    StrongSlot entry = slot.first();
                    if (entry != null) newArray[k - 1] = entry.value();
                } else {
                    int j = slot.keyindex(newHashMask);
                    newHash[j] = slot.relink(newHash[j]);
                }
            }
        }

        // Move array values into hash portion
        for (int i = newArraySize; i < oldArray.length;) {
            LuaValue v;
            if ((v = oldArray[i++]) != null) {
                int slot = hashmod(LuaInteger.hashCode(i), newHashMask);
                Slot newEntry;
                if (m_metatable != null) {
                    newEntry = m_metatable.entry(valueOf(i), v);
                    if (newEntry == null) continue;
                } else {
                    newEntry = defaultEntry(valueOf(i), v);
                }
                newHash[slot] = (newHash[slot] != null) ? newHash[slot].add(newEntry) : newEntry;
            }
        }

        hash = newHash;
        array = newArray;
        hashEntries -= movingToArray;
    }

    @Override
    public Slot entry(LuaValue key, LuaValue value) {
        return defaultEntry(key, value);
    }

    static boolean isLargeKey(LuaValue key) {
        switch (key.type()) {
        case TSTRING:
            return key.rawlen() > 32;
        case TNUMBER:
        case TBOOLEAN:
            return false;
        default:
            return true;
        }
    }

    protected static Entry defaultEntry(LuaValue key, LuaValue value) {
        if (key.isinttype()) {
            return new IntKeyEntry(key.toint(), value);
        } else if (value.type() == TNUMBER) {
            return new NumberValueEntry(key, value.todouble());
        } else {
            return new NormalEntry(key, value);
        }
    }

    // ----------------- sort support -----------------------------
    //
    // implemented heap sort from wikipedia
    //
    // Only sorts the contiguous array part.
    //
    /**
     * Sort the table using a comparator.
     *
     * @param comparator {@link LuaValue} to be called to compare elements.
     */
    public void sort(LuaValue comparator) {
        if (m_metatable != null && m_metatable.useWeakValues()) {
            dropWeakArrayValues();
        }
        int n = array.length;
        while (n > 0 && array[n - 1] == null) {
            --n;
        }
        if (n > 1) {
            heapSort(n, comparator);
        }
    }

    private void heapSort(int count, LuaValue cmpfunc) {
        heapify(count, cmpfunc);
        for (int end = count - 1; end > 0;) {
            swap(end, 0);
            siftDown(0, --end, cmpfunc);
        }
    }

    private void heapify(int count, LuaValue cmpfunc) {
        for (int start = count / 2 - 1; start >= 0; --start)
            siftDown(start, count - 1, cmpfunc);
    }

    private void siftDown(int start, int end, LuaValue cmpfunc) {
        for (int root = start; root * 2 + 1 <= end;) {
            int child = root * 2 + 1;
            if (child < end && compare(child, child + 1, cmpfunc)) ++child;
            if (compare(root, child, cmpfunc)) {
                swap(root, child);
                root = child;
            } else {
                return;
            }
        }
    }

    private boolean compare(int i, int j, LuaValue cmpfunc) {
        LuaValue a, b;
        if (m_metatable == null) {
            a = array[i];
            b = array[j];
        } else {
            a = m_metatable.arrayget(array, i);
            b = m_metatable.arrayget(array, j);
        }
        if (a == null || b == null) return false;
        if (!cmpfunc.isnil()) {
            return cmpfunc.call(a, b).toboolean();
        } else {
            return a.lt_b(b);
        }
    }

    private void swap(int i, int j) {
        LuaValue a = array[i];
        array[i] = array[j];
        array[j] = a;
    }

    public int keyCount() {
        return keys().length;
    }

    /**
     * This may be deprecated in a future release. It is recommended to use next() instead
     *
     * @return array of keys in the table
     */
    public LuaValue[] keys() {
        List<LuaValue> result = new ArrayList<LuaValue>();

        LuaValue k = NIL;
        while (true) {
            Varargs n = next(k);
            k = n.arg1();
            if (k.isnil()) {
                break;
            }
            result.add(k);
        }
        return result.toArray(new LuaValue[result.size()]);
    }

    // equality w/ metatable processing
    @Override
    public LuaValue eq(LuaValue val) {
        return eq_b(val) ? TRUE : FALSE;
    }

    @Override
    public boolean eq_b(LuaValue val) {
        if (this == val) {
            return true;
        }
        if (m_metatable == null || !val.istable()) {
            return false;
        }
        LuaValue valmt = val.getmetatable();
        return valmt != null && LuaValue.eqmtcall(this, m_metatable.toLuaValue(), val, valmt);
    }

    /** Unpack all the elements of this table */
    public Varargs unpack() {
        return unpack(1, this.rawlen());
    }

    /** Unpack all the elements of this table from element i */
    public Varargs unpack(int i) {
        return unpack(i, this.rawlen());
    }

    /** Unpack the elements from i to j inclusive */
    public Varargs unpack(int i, int j) {
        int n = j + 1 - i;
        switch (n) {
        case 0:
            return NONE;
        case 1:
            return get(i);
        case 2:
            return varargsOf(get(i), get(i + 1));
        default:
            if (n < 0) return NONE;
            LuaValue[] v = new LuaValue[n];
            while (--n >= 0) {
                v[n] = get(i + n);
            }
            return varargsOf(v);
        }
    }

    // Metatable operations

    @Override
    public boolean useWeakKeys() {
        return false;
    }

    @Override
    public boolean useWeakValues() {
        return false;
    }

    @Override
    public LuaValue toLuaValue() {
        return this;
    }

    @Override
    public LuaValue wrap(LuaValue value) {
        return value;
    }

    @Override
    public LuaValue arrayget(LuaValue[] array, int index) {
        return array[index];
    }

    /**
     * Return table.maxn() as defined by lua 5.0.
     * <p>
     * Provided for compatibility, not a scalable operation.
     *
     * @return value for maxn
     */
    public int maxn() {
        int max = 0;
        for (LuaValue key : keys()) {
            if (key.isint()) {
                max = Math.max(max, key.toint());
            }
        }
        return max;
    }

    /**
     * Call the supplied function once for each key-value pair
     *
     * @param func function to call
     */
    public LuaValue foreach(LuaValue func) {
        Varargs n;
        LuaValue k = NIL;
        LuaValue v;
        while (!(k = ((n = next(k)).arg1())).isnil()) {
            if (!(v = func.call(k, n.arg(2))).isnil()) {
                return v;
            }
        }
        return NIL;
    }

    /**
     * Call the supplied function once for each key-value pair in the contiguous array part
     *
     * @param func function to call
     */
    public LuaValue foreachi(LuaValue func) {
        LuaValue v, r;
        for (int k = 0; !(v = rawget(++k)).isnil();) {
            if (!(r = func.call(valueOf(k), v)).isnil()) {
                return r;
            }
        }
        return NIL;
    }

}
