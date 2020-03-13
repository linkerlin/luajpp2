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

package nl.weeaboo.lua2.compiler;

import static nl.weeaboo.lua2.vm.LuaConstants.NUMBER_FORMAT_FLOATS_OR_DOUBLES;
import static nl.weeaboo.lua2.vm.LuaConstants.NUMBER_FORMAT_INTS_ONLY;
import static nl.weeaboo.lua2.vm.LuaConstants.NUMBER_FORMAT_NUM_PATCH_INT32;
import static nl.weeaboo.lua2.vm.LuaConstants.TBOOLEAN;
import static nl.weeaboo.lua2.vm.LuaConstants.TINT;
import static nl.weeaboo.lua2.vm.LuaConstants.TNIL;
import static nl.weeaboo.lua2.vm.LuaConstants.TNUMBER;
import static nl.weeaboo.lua2.vm.LuaConstants.TSTRING;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import nl.weeaboo.lua2.vm.LocVars;
import nl.weeaboo.lua2.vm.LuaConstants;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Prototype;

/**
 * Writes compiled Lua code to an output stream.
 */
public final class DumpState {

    /**
     * Mark for precompiled code ({@code '<esc>Lua'}).
     */
    public static final String LUA_SIGNATURE = "\033Lua";

    /** for header of binary files -- this is Lua 5.1 */
    public static final int LUAC_VERSION = 0x51;

    /** for header of binary files -- this is the official format. */
    public static final int LUAC_FORMAT = 0;

    /** size of header of binary files. */
    public static final int LUAC_HEADERSIZE = 12;

    /** expected lua header bytes. */
    private static final byte[] LUAC_HEADER_SIGNATURE = { '\033', 'L', 'u', 'a' };

    /** default number format. */
    public static final int NUMBER_FORMAT_DEFAULT = LuaConstants.NUMBER_FORMAT_FLOATS_OR_DOUBLES;

    /** set true to allow integer compilation. */
    private static boolean ALLOW_INTEGER_CASTING = false;

    private static final int SIZEOF_INT = 4;
    private static final int SIZEOF_SIZET = 4;
    private static final int SIZEOF_INSTRUCTION = 4;

    // header fields
    private boolean isLittleEndian = false;
    private int numberFormat = NUMBER_FORMAT_DEFAULT;
    private int sizeofLuaNumber = 8;

    private final DataOutputStream writer;
    private final boolean strip;

    private DumpState(OutputStream w, boolean strip) {
        this.writer = new DataOutputStream(w);
        this.strip = strip;
    }

    void dumpBlock(final byte[] b, int size) throws IOException {
        writer.write(b, 0, size);
    }

    void dumpChar(int b) throws IOException {
        writer.write(b);
    }

    void dumpInt(int x) throws IOException {
        if (isLittleEndian) {
            writer.writeByte(x & 0xff);
            writer.writeByte((x >> 8) & 0xff);
            writer.writeByte((x >> 16) & 0xff);
            writer.writeByte((x >> 24) & 0xff);
        } else {
            writer.writeInt(x);
        }
    }

    void dumpString(LuaString s) throws IOException {
        final int len = s.len().toint();
        dumpInt(len + 1);
        s.write((DataOutput)writer, 0, len);
        writer.write(0);
    }

    void dumpDouble(double d) throws IOException {
        long l = Double.doubleToLongBits(d);
        if (isLittleEndian) {
            dumpInt((int)l);
            dumpInt((int)(l >> 32));
        } else {
            writer.writeLong(l);
        }
    }

    void dumpCode(final Prototype f) throws IOException {
        final int[] code = f.code;
        int n = code.length;
        dumpInt(n);
        for (int i = 0; i < n; i++) {
            dumpInt(code[i]);
        }
    }

    void dumpConstants(final Prototype f) throws IOException {
        final LuaValue[] k = f.k;
        int n = k.length;
        dumpInt(n);
        for (int i = 0; i < n; i++) {
            final LuaValue o = k[i];
            switch (o.type()) {
            case TNIL:
                writer.write(TNIL);
                break;
            case TBOOLEAN:
                writer.write(TBOOLEAN);
                dumpChar(o.toboolean() ? 1 : 0);
                break;
            case TNUMBER:
                switch (numberFormat) {
                case NUMBER_FORMAT_FLOATS_OR_DOUBLES:
                    writer.write(TNUMBER);
                    dumpDouble(o.todouble());
                    break;
                case NUMBER_FORMAT_INTS_ONLY:
                    if (!ALLOW_INTEGER_CASTING && !o.isint()) {
                        throw new java.lang.IllegalArgumentException("not an integer: " + o);
                    }

                    writer.write(TNUMBER);
                    dumpInt(o.toint());
                    break;
                case NUMBER_FORMAT_NUM_PATCH_INT32:
                    if (o.isint()) {
                        writer.write(TINT);
                        dumpInt(o.toint());
                    } else {
                        writer.write(TNUMBER);
                        dumpDouble(o.todouble());
                    }
                    break;
                default:
                    throw new IllegalArgumentException("number format not supported: " + numberFormat);
                }
                break;
            case TSTRING:
                writer.write(TSTRING);
                dumpString((LuaString)o);
                break;
            default:
                throw new IllegalArgumentException("bad type for " + o);
            }
        }
        n = f.p.length;
        dumpInt(n);
        for (int i = 0; i < n; i++) {
            dumpFunction(f.p[i], f.source);
        }
    }

    void dumpDebug(final Prototype f) throws IOException {
        int n = (strip) ? 0 : f.lineinfo.length;
        dumpInt(n);
        for (int i = 0; i < n; i++) {
            dumpInt(f.lineinfo[i]);
        }
        n = (strip) ? 0 : f.locvars.length;
        dumpInt(n);
        for (int i = 0; i < n; i++) {
            LocVars lvi = f.locvars[i];
            dumpString(lvi.varname);
            dumpInt(lvi.startpc);
            dumpInt(lvi.endpc);
        }
        n = (strip) ? 0 : f.upvalues.length;
        dumpInt(n);
        for (int i = 0; i < n; i++) {
            dumpString(f.upvalues[i]);
        }
    }

    void dumpFunction(final Prototype f, final LuaString string) throws IOException {
        if (f.source == null || f.source.equals(string) || strip) {
            dumpInt(0);
        } else {
            dumpString(f.source);
        }
        dumpInt(f.linedefined);
        dumpInt(f.lastlinedefined);
        dumpChar(f.nups);
        dumpChar(f.numparams);
        dumpChar(f.isVararg);
        dumpChar(f.maxstacksize);
        dumpCode(f);
        dumpConstants(f);
        dumpDebug(f);
    }

    void dumpHeader() throws IOException {
        writer.write(LUAC_HEADER_SIGNATURE);
        writer.write(LUAC_VERSION);
        writer.write(LUAC_FORMAT);
        writer.write(isLittleEndian ? 1 : 0);
        writer.write(SIZEOF_INT);
        writer.write(SIZEOF_SIZET);
        writer.write(SIZEOF_INSTRUCTION);
        writer.write(sizeofLuaNumber);
        writer.write(numberFormat);
    }

    /**
     * Dump Lua function as precompiled chunk.
     *
     * @see #dump(Prototype, OutputStream, boolean, int, boolean)
     * @throws IOException If an I/O error occurs while trying to write the dump to the given output stream.
     */
    public static void dump(Prototype f, OutputStream w, boolean strip) throws IOException {
        DumpState d = new DumpState(w, strip);
        d.dumpHeader();
        d.dumpFunction(f, null);
    }

    /**
     * Dump Lua function as precompiled chunk.
     *
     * @param f the function to dump
     * @param w the output stream to dump to
     * @param stripDebug true to strip debugging info, false otherwise
     * @param numberFormat one of NUMBER_FORMAT_FLOATS_OR_DOUBLES, NUMBER_FORMAT_INTS_ONLY,
     *        NUMBER_FORMAT_NUM_PATCH_INT32
     * @param littleendian true to use little endian for numbers, false for big endian
     * @throws IllegalArgumentException if the number format it not supported
     * @throws IOException If an I/O error occurs while trying to write the dump to the given output stream.
     */
    public static void dump(Prototype f, OutputStream w, boolean stripDebug, int numberFormat,
            boolean littleendian) throws IOException {

        switch (numberFormat) {
        case NUMBER_FORMAT_FLOATS_OR_DOUBLES:
        case NUMBER_FORMAT_INTS_ONLY:
        case NUMBER_FORMAT_NUM_PATCH_INT32:
            break;
        default:
            throw new IllegalArgumentException("number format not supported: " + numberFormat);
        }
        DumpState d = new DumpState(w, stripDebug);
        d.isLittleEndian = littleendian;
        d.numberFormat = numberFormat;
        d.sizeofLuaNumber = (numberFormat == NUMBER_FORMAT_INTS_ONLY ? 4 : 8);
        d.dumpHeader();
        d.dumpFunction(f, null);
    }
}
