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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.vm.LocVars;
import nl.weeaboo.lua2.vm.Lua;
import nl.weeaboo.lua2.vm.LuaInteger;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Prototype;

final class LexState {

    private static final LuaString STR_ARG = LuaString.valueOf("arg");
    private static final LuaString STR_SELF = LuaString.valueOf("self");
    private static final LuaString STR_FOR_CONTROL = LuaString.valueOf("(for control)");
    private static final LuaString STR_FOR_STATE = LuaString.valueOf("(for state)");
    private static final LuaString STR_FOR_GENERATOR = LuaString.valueOf("(for generator)");
    private static final LuaString STR_FOR_STEP = LuaString.valueOf("(for step)");
    private static final LuaString STR_FOR_LIMIT = LuaString.valueOf("(for limit)");
    private static final LuaString STR_FOR_INDEX = LuaString.valueOf("(for index)");

    private static final int EOZ = (-1);
    private static final int MAXSRC = 80;
    private static final int MAX_INT = Integer.MAX_VALUE - 2;

    // TODO convert to unicode
    // CHAR_MAX?
    private static final int UCHAR_MAX = 255;

    private static final int LUAI_MAXCCALLS = 200;

    private static final String luaQS(String s) {
        return "'" + s + "'";
    }

    private static final String luaQL(Object o) {
        return luaQS(String.valueOf(o));
    }

    /** 1 for compatibility, 2 for old behavior. */
    private static int LUA_COMPAT_LSTR = 1;
    private static final boolean LUA_COMPAT_VARARG = true;

    /*
     * * Marks the end of a patch list. It is an invalid value both as an
     * absolute* address, and as a list link (would link an element to itself).
     */
    static final int NO_JUMP = (-1);

    /*
     * Binary operators
     *
     * grep "ORDER OPR" if you change these enums
     */
    static final int OPR_ADD = 0;
    static final int OPR_SUB = 1;
    static final int OPR_MUL = 2;
    static final int OPR_DIV = 3;
    static final int OPR_MOD = 4;
    static final int OPR_POW = 5;
    static final int OPR_CONCAT = 6;
    static final int OPR_NE = 7;
    static final int OPR_EQ = 8;
    static final int OPR_LT = 9;
    static final int OPR_LE = 10;
    static final int OPR_GT = 11;
    static final int OPR_GE = 12;
    static final int OPR_AND = 13;
    static final int OPR_OR = 14;
    static final int OPR_NOBINOPR = 15;

    // Unary operators
    static final int OPR_MINUS = 0;
    static final int OPR_NOT = 1;
    static final int OPR_LEN = 2;
    static final int OPR_NOUNOPR = 3;

    // Exp kind
    /** no value. */
    static final int VVOID = 0;
    static final int VNIL = 1;
    static final int VTRUE = 2;
    static final int VFALSE = 3;
    /** info = index of constant in 'k'. */
    static final int VK = 4;
    /** nval = numerical value. */
    static final int VKNUM = 5;
    /** info = local register. */
    static final int VLOCAL = 6;
    /** info = index of upvalue in 'upvalues'. */
    static final int VUPVAL = 7;
    /** info = index of table, aux = index of global name in 'k'. */
    static final int VGLOBAL = 8;
    /** info = table register, aux = index register (or 'k'). */
    static final int VINDEXED = 9;
    /** info = instruction pc. */
    static final int VJMP = 10;
    /** info = instruction pc. */
    static final int VRELOCABLE = 11;
    /** info = result register. */
    static final int VNONRELOC = 12;
    /** info = instruction pc. */
    static final int VCALL = 13;
    /** info = instruction pc. */
    static final int VVARARG = 14;

    /* semantics information */
    private static class SemInfo {
        LuaValue r;
        LuaString ts;
    }

    private static class Token {
        int token;
        final SemInfo seminfo = new SemInfo();

        public void set(Token other) {
            this.token = other.token;
            this.seminfo.r = other.seminfo.r;
            this.seminfo.ts = other.seminfo.ts;
        }
    }

    int current; /* current character (charint) */
    int linenumber; /* input line counter */
    int lastline; /* line of last token `consumed' */
    final Token t = new Token(); /* current token */
    final Token lookahead = new Token(); /* look ahead token */
    @Nullable FuncState fs; /* `FuncState' is private to the parser */
    LuaC luaC;
    InputStream z; /* input stream */
    byte[] buff; /* buffer for tokens */
    int nbuff; /* length of buffer */
    LuaString source; /* current source name */
    byte decpoint; /* locale decimal point */

    /* ORDER RESERVED */
    static final String[] luaX_tokens = { "and", "break", "do", "else", "elseif", "end", "false", "for",
            "function", "if", "in", "local", "nil", "not", "or", "repeat", "return", "then", "true", "until",
            "while", "..", "...", "==", ">=", "<=", "~=", "<number>", "<name>", "<string>", "<eof>", };

    /* terminal symbols denoted by reserved words */
    static final int TK_AND = 257;
    static final int TK_BREAK = 258;
    static final int TK_DO = 259;
    static final int TK_ELSE = 260;
    static final int TK_ELSEIF = 261;
    static final int TK_END = 262;
    static final int TK_FALSE = 263;
    static final int TK_FOR = 264;
    static final int TK_FUNCTION = 265;
    static final int TK_IF = 266;
    static final int TK_IN = 267;
    static final int TK_LOCAL = 268;
    static final int TK_NIL = 269;
    static final int TK_NOT = 270;
    static final int TK_OR = 271;
    static final int TK_REPEAT = 272;
    static final int TK_RETURN = 273;
    static final int TK_THEN = 274;
    static final int TK_TRUE = 275;
    static final int TK_UNTIL = 276;
    static final int TK_WHILE = 277;
    // other terminal symbols
    static final int TK_CONCAT = 278;
    static final int TK_DOTS = 279;
    static final int TK_EQ = 280;
    static final int TK_GE = 281;
    static final int TK_LE = 282;
    static final int TK_NE = 283;
    static final int TK_NUMBER = 284;
    static final int TK_NAME = 285;
    static final int TK_STRING = 286;
    static final int TK_EOS = 287;

    static final int FIRST_RESERVED = TK_AND;
    static final int NUM_RESERVED = TK_WHILE + 1 - FIRST_RESERVED;

    static final Map<LuaString, Integer> RESERVED = new HashMap<>();

    static {
        for (int i = 0; i < NUM_RESERVED; i++) {
            LuaString ts = LuaValue.valueOf(luaX_tokens[i]);
            RESERVED.put(ts, Integer.valueOf(FIRST_RESERVED + i));
        }
    }

    public LexState(LuaC state, InputStream stream) {
        this.z = stream;
        this.buff = new byte[32];
        this.luaC = state;
    }

    private static boolean isalnum(int c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c == '_');
    }

    private static boolean isalpha(int c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isdigit(int c) {
        return (c >= '0' && c <= '9');
    }

    private static boolean isspace(int c) {
        return (c <= ' ');
    }

    void nextChar() {
        try {
            current = z.read();
        } catch (IOException e) {
            e.printStackTrace();
            current = EOZ;
        }
    }

    boolean currIsNewline() {
        return current == '\n' || current == '\r';
    }

    void save_and_next() {
        save(current);
        nextChar();
    }

    void save(int c) {
        if (buff == null || nbuff + 1 > buff.length) {
            buff = LuaC.realloc(buff, nbuff * 2 + 1);
        }
        buff[nbuff++] = (byte) c;
    }

    String token2str(int token) {
        if (token < FIRST_RESERVED) {
            if (iscntrl(token)) {
                return "char(" + token + ")";
            } else {
                return String.valueOf((char)token);
            }
        } else {
            return luaX_tokens[token - FIRST_RESERVED];
        }
    }

    private static boolean iscntrl(int token) {
        return token < ' ';
    }

    String txtToken(int token) {
        switch (token) {
        case TK_NAME:
        case TK_STRING:
        case TK_NUMBER:
            return LuaString.decodeAsUtf8(buff, 0, nbuff);
        default:
            return token2str(token);
        }
    }

    void lexerror(String msg, int token) {
        String cid = chunkid(source.tojstring());
        if (token != 0) {
            msg += " near '" + txtToken(token) + "'";
        }
        throw new LuaException(cid + ":" + linenumber + ": " + msg);
    }

    String chunkid(String source) {
        if (source.startsWith("=")) {
            return source.substring(1);
        }
        String end = "";
        if (source.startsWith("@")) {
            source = source.substring(1);
        } else {
            source = "[string \"" + source;
            end = "\"]";
        }
        int n = source.length() + end.length();
        if (n > MAXSRC) {
            source = source.substring(0, MAXSRC - end.length() - 3) + "...";
        }
        return source + end;
    }

    void syntaxerror(String msg) {
        lexerror(msg, t.token);
    }

    LuaString newstring(byte[] bytes, int offset, int len) {
        return luaC.newTString(bytes, offset, len);
    }

    void inclinenumber() {
        int old = current;
        LuaC.luaAssert(currIsNewline());

        /* skip '\n' or '\r' */
        nextChar();

        /*
         * skip '\n\r' or
         * '\r\n'
         */
        if (currIsNewline() && current != old) {
            nextChar();
        }

        if (++linenumber >= MAX_INT) {
            syntaxerror("chunk has too many lines");
        }
    }

    void setinput(LuaC luaC, int firstByte, InputStream z, LuaString source) {
        this.decpoint = '.';
        this.luaC = luaC;
        this.lookahead.token = TK_EOS; /* no look-ahead token */
        this.z = z;
        this.fs = null;
        this.linenumber = 1;
        this.lastline = 1;
        this.source = source;
        this.nbuff = 0; /* initialize buffer */
        this.current = firstByte; /* read first char */

        skipBOM();
        skipShebang();
    }

    private void skipBOM() {
        if (current == 0xEF) { //UTF-8 BOM
            nextChar();
            nextChar();
            nextChar();
        } else if (current == 0xFF || current == 0xFE) { //UTF-16 BOM
            nextChar();
            nextChar();
        }
    }

    private void skipShebang() {
        if (current == '#') {
            while (!currIsNewline() && current != EOZ) {
                nextChar();
            }
        }
    }

    /*
     * * =======================================================* LEXICAL
     * ANALYZER* =======================================================
     */

    boolean check_next(String set) {
        if (set.indexOf(current) < 0) {
            return false;
        }
        save_and_next();
        return true;
    }

    void buffreplace(byte from, byte to) {
        int n = nbuff;
        byte[] p = buff;
        while (--n >= 0) {
            if (p[n] == from) {
                p[n] = to;
            }
        }
    }

    boolean str2d(String str, SemInfo seminfo) {
        double d;
        str = str.trim(); // TODO: get rid of this
        if (str.startsWith("0x")) {
            d = Long.parseLong(str.substring(2), 16);
        } else {
            d = Double.parseDouble(str);
        }
        seminfo.r = LuaValue.valueOf(d);
        return true;
    }

    void read_numeral(SemInfo seminfo) {
        LuaC.luaAssert(isdigit(current));
        do {
            save_and_next();
        } while (isdigit(current) || current == '.');
        if (check_next("Ee")) {
            check_next("+-"); /* optional exponent sign */
        }
        while (isalnum(current) || current == '_') {
            save_and_next();
        }
        save('\0');
        buffreplace((byte) '.', decpoint); /* follow locale for decimal point */
        String str = LuaString.decodeAsUtf8(buff, 0, nbuff);
        // if (!str2d(str, seminfo)) /* format error? */
        // trydecpoint(str, seminfo); /* try to update decimal point separator
        // */
        str2d(str, seminfo);
    }

    int skip_sep() {
        int count = 0;
        int s = current;
        LuaC.luaAssert(s == '[' || s == ']');
        save_and_next();
        while (current == '=') {
            save_and_next();
            count++;
        }
        return current == s ? count : -count - 1;
    }

    void read_long_string(SemInfo seminfo, int sep) {
        int cont = 0;
        save_and_next(); /* skip 2nd `[' */
        if (currIsNewline()) {
            inclinenumber(); /* skip it */
        }
        for (boolean endloop = false; !endloop;) {
            switch (current) {
            case EOZ:
                lexerror((seminfo != null) ? "unfinished long string" : "unfinished long comment", TK_EOS);
                break; /* to avoid warnings */
            case '[': {
                if (skip_sep() == sep) {
                    save_and_next(); /* skip 2nd `[' */
                    cont++;
                    if (LUA_COMPAT_LSTR == 1) {
                        if (sep == 0) {
                            lexerror("nesting of [[...]] is deprecated", '[');
                        }
                    }
                }
                break;
            }
            case ']': {
                if (skip_sep() == sep) {
                    save_and_next(); /* skip 2nd `]' */
                    if (LUA_COMPAT_LSTR == 2) {
                        cont--;
                        if (sep == 0 && cont >= 0) {
                            break;
                        }
                    }
                    endloop = true;
                }
                break;
            }
            case '\n':
            case '\r': {
                save('\n');
                inclinenumber();
                if (seminfo == null) {
                    nbuff = 0; /* avoid wasting space */
                }
                break;
            }
            default: {
                if (seminfo != null) {
                    save_and_next();
                } else {
                    nextChar();
                }
            }
            }
        }
        if (seminfo != null) {
            seminfo.ts = newstring(buff, 2 + sep, nbuff - 2 * (2 + sep));
        }
    }

    void read_string(int del, SemInfo seminfo) {
        save_and_next();
        while (current != del) {
            switch (current) {
            case EOZ:
                lexerror("unfinished string", TK_EOS);
                continue; /* to avoid warnings */
            case '\n':
            case '\r':
                lexerror("unfinished string", TK_STRING);
                continue; /* to avoid warnings */
            case '\\': {
                int c;
                nextChar(); /* do not save the `\' */
                switch (current) {
                case 'a':
                    c = '\u0007'; /* bell */
                    break;
                case 'b': /* backspace */
                    c = '\b';
                    break;
                case 'f': /* form feed */
                    c = '\f';
                    break;
                case 'n': /* newline */
                    c = '\n';
                    break;
                case 'r': /* carriage return */
                    c = '\r';
                    break;
                case 't': /* tab */
                    c = '\t';
                    break;
                case 'v': /* vertical tab */
                    c = '\u000B';
                    break;
                case '\n': /* go through */
                case '\r':
                    save('\n');
                    inclinenumber();
                    continue;
                case EOZ:
                    continue; /* will raise an error next loop */
                default: {
                    if (!isdigit(current)) {
                        // handles \\, \",  \', and \?
                        save_and_next();
                    } else {
                        // \xxx
                        int i = 0;
                        c = 0;
                        do {
                            c = 10 * c + (current - '0');
                            nextChar();
                        } while (++i < 3 && isdigit(current));
                        if (c > UCHAR_MAX) {
                            lexerror("escape sequence too large", TK_STRING);
                        }
                        save(c);
                    }
                    continue;
                }
                }
                save(c);
                nextChar();
                continue;
            }
            default:
                save_and_next();
            }
        }
        save_and_next(); /* skip delimiter */
        seminfo.ts = newstring(buff, 1, nbuff - 2);
    }

    int llex(SemInfo seminfo) {
        nbuff = 0;
        while (true) {
            switch (current) {
            case '\n':
            case '\r': {
                inclinenumber();
                continue;
            }
            case '-': {
                nextChar();
                if (current != '-') {
                    return '-';
                }
                /* else is a comment */
                nextChar();
                if (current == '[') {
                    int sep = skip_sep();
                    nbuff = 0; /* `skip_sep' may dirty the buffer */
                    if (sep >= 0) {
                        read_long_string(null, sep); /* long comment */
                        nbuff = 0;
                        continue;
                    }
                }
                /* else short comment */
                while (!currIsNewline() && current != EOZ) {
                    nextChar();
                }
                continue;
            }
            case '[': {
                int sep = skip_sep();
                if (sep >= 0) {
                    read_long_string(seminfo, sep);
                    return TK_STRING;
                } else if (sep == -1) {
                    return '[';
                } else {
                    lexerror("invalid long string delimiter", TK_STRING);
                    break;
                }
            }
            case '=': {
                nextChar();
                if (current != '=') {
                    return '=';
                } else {
                    nextChar();
                    return TK_EQ;
                }
            }
            case '<': {
                nextChar();
                if (current != '=') {
                    return '<';
                } else {
                    nextChar();
                    return TK_LE;
                }
            }
            case '>': {
                nextChar();
                if (current != '=') {
                    return '>';
                } else {
                    nextChar();
                    return TK_GE;
                }
            }
            case '~': {
                nextChar();
                if (current != '=') {
                    return '~';
                } else {
                    nextChar();
                    return TK_NE;
                }
            }
            case '"':
            case '\'': {
                read_string(current, seminfo);
                return TK_STRING;
            }
            case '.': {
                save_and_next();
                if (check_next(".")) {
                    if (check_next(".")) {
                        return TK_DOTS; /* ... */
                    } else {
                        return TK_CONCAT; /* .. */
                    }
                } else if (!isdigit(current)) {
                    return '.';
                } else {
                    read_numeral(seminfo);
                    return TK_NUMBER;
                }
            }
            case EOZ: {
                return TK_EOS;
            }
            default: {
                if (isspace(current)) {
                    LuaC.luaAssert(!currIsNewline());
                    nextChar();
                    continue;
                } else if (isdigit(current)) {
                    read_numeral(seminfo);
                    return TK_NUMBER;
                } else if (isalpha(current) || current == '_') {
                    /* identifier or reserved word */
                    LuaString ts;
                    do {
                        save_and_next();
                    } while (isalnum(current) || current == '_');
                    ts = newstring(buff, 0, nbuff);
                    if (RESERVED.containsKey(ts)) {
                        return RESERVED.get(ts).intValue();
                    } else {
                        seminfo.ts = ts;
                        return TK_NAME;
                    }
                } else {
                    int c = current;
                    nextChar();
                    return c; /* single-char tokens (+ - / ...) */
                }
            }
            }
        }
    }

    void next() {
        lastline = linenumber;
        if (lookahead.token != TK_EOS) { /* is there a look-ahead token? */
            t.set(lookahead); /* use this one */
            lookahead.token = TK_EOS; /* and discharge it */
        } else {
            t.token = llex(t.seminfo); /* read next token */
        }
    }

    void lookahead() {
        LuaC.luaAssert(lookahead.token == TK_EOS);
        lookahead.token = llex(lookahead.seminfo);
    }

    // =============================================================
    // from lcode.h
    // =============================================================

    // =============================================================
    // from lparser.c
    // =============================================================

    static class ExpDesc {
        int k; // expkind, from enumerated list, above

        static class U { // originally a union
            static class S {
                int info;
                int aux;
            }

            final S s = new S();
            private LuaValue nval;

            public void setNval(LuaValue r) {
                nval = r;
            }

            public LuaValue nval() {
                return (nval == null ? LuaInteger.valueOf(s.info) : nval);
            }
        }

        final U u = new U();
        final IntPtr t = new IntPtr(); /* patch list of `exit when true' */
        final IntPtr f = new IntPtr(); /* patch list of `exit when false' */

        void init(int k, int i) {
            this.f.i = NO_JUMP;
            this.t.i = NO_JUMP;
            this.k = k;
            this.u.s.info = i;
        }

        boolean hasjumps() {
            return (t.i != f.i);
        }

        boolean isnumeral() {
            return (k == VKNUM && t.i == NO_JUMP && f.i == NO_JUMP);
        }

        public void setvalue(ExpDesc other) {
            this.k = other.k;
            this.u.nval = other.u.nval;
            this.u.s.info = other.u.s.info;
            this.u.s.aux = other.u.s.aux;
            this.t.i = other.t.i;
            this.f.i = other.f.i;
        }
    }

    boolean hasmultret(int k) {
        return (k == VCALL || k == VVARARG);
    }

    /*----------------------------------------------------------------------
    name        args    description
    ------------------------------------------------------------------------*/

    /*
     * * prototypes for recursive non-terminal functions
     */

    void error_expected(int token) {
        syntaxerror(luaQS(token2str(token)) + " expected");
    }

    boolean testnext(int c) {
        if (t.token == c) {
            next();
            return true;
        } else {
            return false;
        }
    }

    void check(int c) {
        if (t.token != c) {
            error_expected(c);
        }
    }

    void checknext(int c) {
        check(c);
        next();
    }

    void check_condition(boolean c, String msg) {
        if (!c) {
            syntaxerror(msg);
        }
    }

    void check_match(int what, int who, int where) {
        if (!testnext(what)) {
            if (where == linenumber) {
                error_expected(what);
            } else {
                syntaxerror(luaQS(token2str(what)) + " expected " + "(to close " + luaQS(token2str(who)) + " at line "
                        + where + ")");
            }
        }
    }

    LuaString str_checkname() {
        LuaString ts;
        check(TK_NAME);
        ts = t.seminfo.ts;
        next();
        return ts;
    }

    void codestring(ExpDesc e, LuaString s) {
        e.init(VK, getCurrentFuncState().stringK(s));
    }

    void checkname(ExpDesc e) {
        codestring(e, str_checkname());
    }

    int registerlocalvar(LuaString varname) {
        FuncState fs = getCurrentFuncState();
        Prototype f = fs.f;
        if (f.locvars == null || fs.nlocvars + 1 > f.locvars.length) {
            f.locvars = LuaC.realloc(f.locvars,
                    fs.nlocvars * 2 + 1);
        }
        f.locvars[fs.nlocvars] = new LocVars(varname, 0, 0);
        return fs.nlocvars++;
    }

    void new_localvar(LuaString name, int n) {
        FuncState fs = getCurrentFuncState();
        fs.checklimit(fs.nactvar + n + 1, LuaC.LUAI_MAXVARS, "local variables");
        fs.actvar[fs.nactvar + n] = (short) registerlocalvar(name);
    }

    void adjustlocalvars(int nvars) {
        FuncState fs = getCurrentFuncState();
        fs.nactvar = (short) (fs.nactvar + nvars);
        for (; nvars > 0; nvars--) {
            fs.getlocvar(fs.nactvar - nvars).startpc = fs.pc;
        }
    }

    void removevars(int tolevel) {
        FuncState fs = getCurrentFuncState();
        while (fs.nactvar > tolevel) {
            fs.getlocvar(--fs.nactvar).endpc = fs.pc;
        }
    }

    void singlevar(ExpDesc var) {
        LuaString varname = this.str_checkname();
        FuncState fs = getCurrentFuncState();
        if (fs.singlevaraux(varname, var, 1) == VGLOBAL) {
            // info points to global name
            var.u.s.info = fs.stringK(varname);
        }
    }

    void adjust_assign(int nvars, int nexps, ExpDesc e) {
        FuncState fs = getCurrentFuncState();
        int extra = nvars - nexps;
        if (hasmultret(e.k)) {
            /* includes call itself */
            extra++;
            if (extra < 0) {
                extra = 0;
            }
            /* last exp. provides the difference */
            fs.setreturns(e, extra);
            if (extra > 1) {
                fs.reserveregs(extra - 1);
            }
        } else {
            /* close last expression */
            if (e.k != VVOID) {
                fs.exp2nextreg(e);
            }
            if (extra > 0) {
                int reg = fs.freereg;
                fs.reserveregs(extra);
                fs.nil(reg, extra);
            }
        }
    }

    void enterlevel() {
        if (++luaC.nCcalls > LUAI_MAXCCALLS) {
            lexerror("chunk has too many syntax levels", 0);
        }
    }

    void leavelevel() {
        luaC.nCcalls--;
    }

    void pushclosure(FuncState func, ExpDesc v) {
        FuncState fs = getCurrentFuncState();
        Prototype f = fs.f;
        if (f.p == null || fs.np + 1 > f.p.length) {
            f.p = LuaC.realloc(f.p, fs.np * 2 + 1);
        }
        f.p[fs.np++] = func.f;
        v.init(VRELOCABLE, fs.codeABx(Lua.OP_CLOSURE, 0, fs.np - 1));
        for (int i = 0; i < func.f.nups; i++) {
            int o = (func.upvalues[i].k == VLOCAL) ? Lua.OP_MOVE : Lua.OP_GETUPVAL;
            fs.codeABC(o, 0, func.upvalues[i].info, 0);
        }
    }

    void open_func(FuncState fs) {
        final LuaC L = this.luaC;
        Prototype f = new Prototype();
        if (this.fs != null) {
            f.source = this.fs.f.source;
        }
        fs.f = f;
        fs.prev = this.fs; /* linked list of funcstates */
        fs.ls = this;
        fs.luaC = L;
        this.fs = fs;
        fs.pc = 0;
        fs.lasttarget = -1;
        fs.jpc = new IntPtr(NO_JUMP);
        fs.freereg = 0;
        fs.nk = 0;
        fs.np = 0;
        fs.nlocvars = 0;
        fs.nactvar = 0;
        fs.bl = null;
        f.maxstacksize = 2; /* registers 0/1 are always valid */
        // fs.h = new LTable();
        fs.htable = new HashMap<>();
    }

    void close_func() {
        FuncState fs = getCurrentFuncState();
        Prototype f = fs.f;
        this.removevars(0);
        fs.ret(0, 0); /* final return */
        f.code = LuaC.realloc(f.code, fs.pc);
        f.lineinfo = LuaC.realloc(f.lineinfo, fs.pc);
        // f.sizelineinfo = fs.pc;
        f.k = LuaC.realloc(f.k, fs.nk);
        f.p = LuaC.realloc(f.p, fs.np);
        f.locvars = LuaC.realloc(f.locvars, fs.nlocvars);
        // f.sizelocvars = fs.nlocvars;
        f.upvalues = LuaC.realloc(f.upvalues, f.nups);
        // LuaC._assert (CheckCode.checkcode(f));
        LuaC.luaAssert(fs.bl == null);
        this.fs = fs.prev;
    }

    /* ============================================================ */
    /* GRAMMAR RULES */
    /* ============================================================ */

    void field(ExpDesc v) {
        /* field -> ['.' | ':'] NAME */
        FuncState fs = getCurrentFuncState();
        ExpDesc key = new ExpDesc();
        fs.exp2anyreg(v);
        this.next(); /* skip the dot or colon */
        this.checkname(key);
        fs.indexed(v, key);
    }

    void yindex(ExpDesc v) {
        /* index -> '[' expr ']' */
        this.next(); /* skip the '[' */
        this.expr(v);
        getCurrentFuncState().exp2val(v);
        this.checknext(']');
    }

    private @Nonnull FuncState getCurrentFuncState() {
        FuncState fs = this.fs;
        if (fs == null) {
            throw new IllegalStateException("FuncState was unexpectedly null");
        }
        return fs;
    }

    /*
     * * {======================================================================
     * * Rules for Constructors*
     * =======================================================================
     */

    static class ConsControl {
        ExpDesc v = new ExpDesc(); /* last list item read */
        ExpDesc t; /* table descriptor */
        int nh; /* total number of `record' elements */
        int na; /* total number of array elements */
        int tostore; /* number of array elements pending to be stored */
    }

    void recfield(ConsControl cc) {
        /* recfield -> (NAME | `['exp1`]') = exp1 */
        FuncState fs = getCurrentFuncState();
        final int reg = fs.freereg;
        ExpDesc key = new ExpDesc();
        if (this.t.token == TK_NAME) {
            fs.checklimit(cc.nh, MAX_INT, "items in a constructor");
            checkname(key);
        } else {
            /* this.t.token == '[' */
            yindex(key);
        }
        cc.nh++;
        checknext('=');
        int rkkey = fs.exp2RK(key);

        ExpDesc val = new ExpDesc();
        expr(val);
        fs.codeABC(Lua.OP_SETTABLE, cc.t.u.s.info, rkkey, fs.exp2RK(val));
        fs.freereg = reg; /* free registers */
    }

    void listfield(ConsControl cc) {
        this.expr(cc.v);
        getCurrentFuncState().checklimit(cc.na, MAX_INT, "items in a constructor");
        cc.na++;
        cc.tostore++;
    }

    void constructor(ExpDesc t) {
        /* constructor -> ?? */
        FuncState fs = getCurrentFuncState();
        final int line = this.linenumber;
        int pc = fs.codeABC(Lua.OP_NEWTABLE, 0, 0, 0);
        ConsControl cc = new ConsControl();
        cc.na = cc.nh = cc.tostore = 0;
        cc.t = t;
        t.init(VRELOCABLE, pc);
        cc.v.init(VVOID, 0); /* no value (yet) */
        fs.exp2nextreg(t); /* fix it at stack top (for gc) */
        this.checknext('{');
        do {
            LuaC.luaAssert(cc.v.k == VVOID || cc.tostore > 0);
            if (this.t.token == '}') {
                break;
            }
            fs.closelistfield(cc);
            switch (this.t.token) {
            case TK_NAME: { /* may be listfields or recfields */
                this.lookahead();
                if (this.lookahead.token != '=') {
                    this.listfield(cc);
                } else {
                    this.recfield(cc);
                }
                break;
            }
            case '[': { /* constructor_item -> recfield */
                this.recfield(cc);
                break;
            }
            default: { /* constructor_part -> listfield */
                this.listfield(cc);
                break;
            }
            }
        } while (this.testnext(',') || this.testnext(';'));
        this.check_match('}', '{', line);
        fs.lastlistfield(cc);
        InstructionPtr i = new InstructionPtr(fs.f.code, pc);
        LuaC.setArgB(i, luaO_int2fb(cc.na)); /* set initial array size */
        LuaC.setArgC(i, luaO_int2fb(cc.nh)); /* set initial table size */
    }

    /*
     * * converts an integer to a "floating point byte", represented as*
     * (eeeeexxx), where the real value is (1xxx) * 2^(eeeee - 1) if* eeeee != 0
     * and (xxx) otherwise.
     */
    static int luaO_int2fb(int x) {
        int e = 0; /* expoent */
        while (x >= 16) {
            x = (x + 1) >> 1;
            e++;
        }
        if (x < 8) {
            return x;
        } else {
            return ((e + 1) << 3) | (x - 8);
        }
    }

    /* }====================================================================== */

    void parlist() {
        /* parlist -> [ param { `,' param } ] */
        FuncState fs = getCurrentFuncState();
        Prototype f = fs.f;
        int nparams = 0;
        f.isVararg = 0;
        if (this.t.token != ')') { /* is `parlist' not empty? */
            do {
                switch (this.t.token) {
                case TK_NAME: { /* param . NAME */
                    this.new_localvar(this.str_checkname(), nparams++);
                    break;
                }
                case TK_DOTS: { /* param . `...' */
                    this.next();
                    if (LUA_COMPAT_VARARG) {
                        /* use `arg' as default name */
                        this.new_localvar(STR_ARG, nparams++);
                        f.isVararg = Lua.VARARG_HASARG | Lua.VARARG_NEEDSARG;
                    }
                    f.isVararg |= Lua.VARARG_ISVARARG;
                    break;
                }
                default:
                    this.syntaxerror("<name> or " + luaQL("...") + " expected");
                }
            } while ((f.isVararg == 0) && this.testnext(','));
        }
        this.adjustlocalvars(nparams);
        f.numparams = (fs.nactvar - (f.isVararg & Lua.VARARG_HASARG));
        fs.reserveregs(fs.nactvar); /* reserve register for parameters */
    }

    /* body -> `(' parlist `)' chunk END */
    void body(ExpDesc e, boolean needself, int line) {
        /* body -> `(' parlist `)' chunk END */
        FuncState newFS = new FuncState();
        open_func(newFS);
        newFS.f.linedefined = line;
        this.checknext('(');
        if (needself) {
            new_localvar(STR_SELF, 0);
            adjustlocalvars(1);
        }
        this.parlist();
        this.checknext(')');
        this.chunk();
        newFS.f.lastlinedefined = this.linenumber;
        this.check_match(TK_END, TK_FUNCTION, line);
        this.close_func();
        this.pushclosure(newFS, e);
    }

    int explist1(ExpDesc v) {
        /* explist1 -> expr { `,' expr } */
        int n = 1; /* at least one expression */
        this.expr(v);
        while (this.testnext(',')) {
            getCurrentFuncState().exp2nextreg(v);
            this.expr(v);
            n++;
        }
        return n;
    }

    void funcargs(ExpDesc f) {
        FuncState fs = getCurrentFuncState();
        ExpDesc args = new ExpDesc();
        int line = this.linenumber;
        switch (this.t.token) {
        case '(': { /* funcargs -> `(' [ explist1 ] `)' */
            if (line != this.lastline) {
                this.syntaxerror("ambiguous syntax (function call x new statement)");
            }
            this.next();
            if (this.t.token == ')') {
                args.k = VVOID;
            } else {
                this.explist1(args);
                fs.setmultret(args);
            }
            this.check_match(')', '(', line);
            break;
        }
        case '{': { /* funcargs -> constructor */
            this.constructor(args);
            break;
        }
        case TK_STRING: { /* funcargs -> STRING */
            this.codestring(args, this.t.seminfo.ts);
            this.next(); /* must use `seminfo' before `next' */
            break;
        }
        default: {
            this.syntaxerror("function arguments expected");
            return;
        }
        }

        LuaC.luaAssert(f.k == VNONRELOC);
        int base = f.u.s.info; /* base register for call */
        int nparams;
        if (hasmultret(args.k)) {
            nparams = Lua.LUA_MULTRET; /* open call */
        } else {
            if (args.k != VVOID) {
                fs.exp2nextreg(args); /* close last argument */
            }
            nparams = fs.freereg - (base + 1);
        }
        f.init(VCALL, fs.codeABC(Lua.OP_CALL, base, nparams + 1, 2));
        fs.fixline(line);
        fs.freereg = base + 1; /*
                                 * call remove function and arguments and leaves
                                 * (unless changed) one result
                                 */
    }

    /*
     * * {======================================================================
     * * Expression parsing*
     * =======================================================================
     */

    void prefixexp(ExpDesc v) {
        /* prefixexp -> NAME | '(' expr ')' */
        switch (this.t.token) {
        case '(': {
            int line = this.linenumber;
            this.next();
            this.expr(v);
            this.check_match(')', '(', line);
            getCurrentFuncState().dischargevars(v);
            return;
        }
        case TK_NAME: {
            this.singlevar(v);
            return;
        }
        default: {
            this.syntaxerror("unexpected symbol (#" + this.t.token + ", '" + (char)t.token + "')");
            return;
        }
        }
    }

    void primaryexp(ExpDesc v) {
        /*
         * primaryexp -> prefixexp { `.' NAME | `[' exp `]' | `:' NAME funcargs
         * | funcargs }
         */
        FuncState fs = getCurrentFuncState();
        this.prefixexp(v);
        for (;;) {
            switch (this.t.token) {
            case '.': { /* field */
                this.field(v);
                break;
            }
            case '[': { /* `[' exp1 `]' */
                ExpDesc key = new ExpDesc();
                fs.exp2anyreg(v);
                this.yindex(key);
                fs.indexed(v, key);
                break;
            }
            case ':': { /* `:' NAME funcargs */
                ExpDesc key = new ExpDesc();
                this.next();
                this.checkname(key);
                fs.self(v, key);
                this.funcargs(v);
                break;
            }
            case '(':
            case TK_STRING:
            case '{': { /* funcargs */
                fs.exp2nextreg(v);
                this.funcargs(v);
                break;
            }
            default:
                return;
            }
        }
    }

    void simpleexp(ExpDesc v) {
        /*
         * simpleexp -> NUMBER | STRING | NIL | true | false | ... | constructor
         * | FUNCTION body | primaryexp
         */
        switch (this.t.token) {
        case TK_NUMBER: {
            v.init(VKNUM, 0);
            v.u.setNval(this.t.seminfo.r);
            break;
        }
        case TK_STRING: {
            this.codestring(v, this.t.seminfo.ts);
            break;
        }
        case TK_NIL: {
            v.init(VNIL, 0);
            break;
        }
        case TK_TRUE: {
            v.init(VTRUE, 0);
            break;
        }
        case TK_FALSE: {
            v.init(VFALSE, 0);
            break;
        }
        case TK_DOTS: { /* vararg */
            FuncState fs = getCurrentFuncState();
            this.check_condition(fs.f.isVararg != 0, "cannot use " + luaQL("...")
                    + " outside a vararg function");
            fs.f.isVararg &= ~Lua.VARARG_NEEDSARG; /* don't need 'arg' */
            v.init(VVARARG, fs.codeABC(Lua.OP_VARARG, 0, 1, 0));
            break;
        }
        case '{': { /* constructor */
            this.constructor(v);
            return;
        }
        case TK_FUNCTION: {
            this.next();
            this.body(v, false, this.linenumber);
            return;
        }
        default: {
            this.primaryexp(v);
            return;
        }
        }
        this.next();
    }

    int getunopr(int op) {
        switch (op) {
        case TK_NOT:
            return OPR_NOT;
        case '-':
            return OPR_MINUS;
        case '#':
            return OPR_LEN;
        default:
            return OPR_NOUNOPR;
        }
    }

    int getbinopr(int op) {
        switch (op) {
        case '+':
            return OPR_ADD;
        case '-':
            return OPR_SUB;
        case '*':
            return OPR_MUL;
        case '/':
            return OPR_DIV;
        case '%':
            return OPR_MOD;
        case '^':
            return OPR_POW;
        case TK_CONCAT:
            return OPR_CONCAT;
        case TK_NE:
            return OPR_NE;
        case TK_EQ:
            return OPR_EQ;
        case '<':
            return OPR_LT;
        case TK_LE:
            return OPR_LE;
        case '>':
            return OPR_GT;
        case TK_GE:
            return OPR_GE;
        case TK_AND:
            return OPR_AND;
        case TK_OR:
            return OPR_OR;
        default:
            return OPR_NOBINOPR;
        }
    }

    static class Priority {
        final byte left; /* left priority for each binary operator */

        final byte right; /* right priority */

        public Priority(int i, int j) {
            left = (byte) i;
            right = (byte) j;
        }
    }

    static Priority[] priority = { /* ORDER OPR */
            /*
             * ` + ' ` - ' ` / ' ` % '
             */
            new Priority(6, 6), new Priority(6, 6), new Priority(7, 7), new Priority(7, 7),
            new Priority(7, 7),

            new Priority(10, 9), new Priority(5, 4), /*
                                                      * power and concat (right associative)
                                                      */
            new Priority(3, 3), new Priority(3, 3), /* equality and inequality */
            new Priority(3, 3), new Priority(3, 3), new Priority(3, 3), new Priority(3, 3), /* order */
            new Priority(2, 2), new Priority(1, 1) /* logical (and/or) */
    };

    static final int UNARY_PRIORITY = 8; /* priority for unary operators */

    /*
     * * subexpr -> (simpleexp | unop subexpr) { binop subexpr }* where `binop'
     * is any binary operator with a priority higher than `limit'
     */
    int subexpr(ExpDesc v, int limit) {
        enterlevel();

        int uop = getunopr(this.t.token);
        if (uop != OPR_NOUNOPR) {
            next();
            subexpr(v, UNARY_PRIORITY);
            getCurrentFuncState().prefix(uop, v);
        } else {
            simpleexp(v);
        }
        /* expand while operators have priorities higher than `limit' */
        int op = getbinopr(this.t.token);
        while (op != OPR_NOBINOPR && priority[op].left > limit) {
            ExpDesc v2 = new ExpDesc();
            int nextop;
            next();
            getCurrentFuncState().infix(op, v);
            /* read sub-expression with higher priority */
            nextop = this.subexpr(v2, priority[op].right);
            getCurrentFuncState().posfix(op, v, v2);
            op = nextop;
        }
        leavelevel();
        return op; /* return first untreated operator */
    }

    void expr(ExpDesc v) {
        subexpr(v, 0);
    }

    /* }==================================================================== */

    /*
     * * {======================================================================
     * * Rules for Statements*
     * =======================================================================
     */

    boolean block_follow(int token) {
        switch (token) {
        case TK_ELSE:
        case TK_ELSEIF:
        case TK_END:
        case TK_UNTIL:
        case TK_EOS:
            return true;
        default:
            return false;
        }
    }

    void block() {
        /* block -> chunk */
        FuncState fs = getCurrentFuncState();
        BlockCnt bl = new BlockCnt();
        fs.enterblock(bl, false);
        this.chunk();
        LuaC.luaAssert(bl.breakList.i == NO_JUMP);
        fs.leaveblock();
    }

    /*
     * * structure to chain all variables in the left-hand side of an*
     * assignment
     */
    static class LhsAssign {
        @Nullable LhsAssign prev;
        /* variable (global, local, upvalue, or indexed) */
        ExpDesc v = new ExpDesc();
    }

    /*
     * * check whether, in an assignment to a local variable, the local variable
     * * is needed in a previous assignment (to a table). If so, save original*
     * local value in a safe place and use this safe copy in the previous*
     * assignment.
     */
    void check_conflict(LhsAssign lh, ExpDesc v) {
        FuncState fs = getCurrentFuncState();
        int extra = fs.freereg; /* eventual position to save local variable */
        boolean conflict = false;
        for (; lh != null; lh = lh.prev) {
            if (lh.v.k == VINDEXED) {
                if (lh.v.u.s.info == v.u.s.info) { /* conflict? */
                    conflict = true;
                    lh.v.u.s.info = extra; /*
                                             * previous assignment will use safe
                                             * copy
                                             */
                }
                if (lh.v.u.s.aux == v.u.s.info) { /* conflict? */
                    conflict = true;
                    lh.v.u.s.aux = extra; /*
                                         * previous assignment will use safe
                                         * copy
                                         */
                }
            }
        }
        if (conflict) {
            fs.codeABC(Lua.OP_MOVE, fs.freereg, v.u.s.info, 0); /* make copy */
            fs.reserveregs(1);
        }
    }

    void assignment(LhsAssign lh, int nvars) {
        ExpDesc e = new ExpDesc();
        this.check_condition(VLOCAL <= lh.v.k && lh.v.k <= VINDEXED, "syntax error");

        FuncState fs = getCurrentFuncState();
        if (this.testnext(',')) { /* assignment -> `,' primaryexp assignment */
            LhsAssign nv = new LhsAssign();
            nv.prev = lh;
            this.primaryexp(nv.v);
            if (nv.v.k == VLOCAL) {
                this.check_conflict(lh, nv.v);
            }
            this.assignment(nv, nvars + 1);
        } else { /* assignment . `=' explist1 */
            int nexps;
            this.checknext('=');
            nexps = this.explist1(e);
            if (nexps != nvars) {
                this.adjust_assign(nvars, nexps, e);
                if (nexps > nvars) {
                    fs.freereg -= nexps - nvars; /*
                                                                         * remove
                                                                         * extra
                                                                         * values
                                                                         */
                }
            } else {
                fs.setoneret(e); /* close last expression */
                fs.storevar(lh.v, e);
                return; /* avoid default */
            }
        }
        e.init(VNONRELOC, fs.freereg - 1); /* default assignment */
        fs.storevar(lh.v, e);
    }

    int cond() {
        /* cond -> exp */
        ExpDesc v = new ExpDesc();
        /* read condition */
        this.expr(v);
        /* `falses' are all equal here */
        if (v.k == VNIL) {
            v.k = VFALSE;
        }

        FuncState fs = getCurrentFuncState();
        fs.goiftrue(v);
        return v.f.i;
    }

    void breakstat() {
        FuncState fs = getCurrentFuncState();
        BlockCnt bl = fs.bl;
        boolean upval = false;
        while (bl != null && !bl.isBreakable) {
            upval |= bl.containsUpValue;
            bl = bl.previous;
        }

        if (bl == null) {
            this.syntaxerror("no loop to break");
        } else {
            if (upval) {
                fs.codeABC(Lua.OP_CLOSE, bl.activeLocalVarCount, 0, 0);
            }
            fs.concat(bl.breakList, fs.jump());
        }
    }

    void whilestat(int line) {
        /* whilestat -> WHILE cond DO block END */
        FuncState fs = getCurrentFuncState();
        int whileinit;
        int condexit;
        this.next(); /* skip WHILE */
        whileinit = fs.getlabel();
        condexit = this.cond();

        BlockCnt bl = new BlockCnt();
        fs.enterblock(bl, true);
        this.checknext(TK_DO);
        this.block();
        fs.patchlist(fs.jump(), whileinit);
        this.check_match(TK_END, TK_WHILE, line);
        fs.leaveblock();
        fs.patchtohere(condexit); /* false conditions finish the loop */
    }

    void repeatstat(int line) {
        /* repeatstat -> REPEAT block UNTIL cond */
        FuncState fs = getCurrentFuncState();
        final int repeat_init = fs.getlabel();
        BlockCnt bl1 = new BlockCnt();
        BlockCnt bl2 = new BlockCnt();
        fs.enterblock(bl1, true); /* loop block */
        fs.enterblock(bl2, false); /* scope block */
        this.next(); /* skip REPEAT */
        this.chunk();
        this.check_match(TK_UNTIL, TK_REPEAT, line);
        int condexit = this.cond(); /* read condition (inside scope block) */
        if (!bl2.containsUpValue) { /* no upvalues? */
            fs.leaveblock(); /* finish scope */
            fs.patchlist(condexit, repeat_init); /* close the loop */
        } else { /* complete semantics when there are upvalues */
            this.breakstat(); /* if condition then break */
            fs.patchtohere(condexit); /* else... */
            fs.leaveblock(); /* finish scope... */
            fs.patchlist(fs.jump(), repeat_init); /* and repeat */
        }
        fs.leaveblock(); /* finish loop */
    }

    int exp1() {
        ExpDesc e = new ExpDesc();
        int k;
        this.expr(e);
        k = e.k;
        getCurrentFuncState().exp2nextreg(e);
        return k;
    }

    void forbody(int base, int line, int nvars, boolean isnum) {
        /* forbody -> DO block */
        FuncState fs = getCurrentFuncState();
        this.adjustlocalvars(3); /* control variables */
        this.checknext(TK_DO);
        final int prep = isnum ? fs.codeAsBx(Lua.OP_FORPREP, base, NO_JUMP) : fs.jump();

        BlockCnt bl = new BlockCnt();
        fs.enterblock(bl, false); /* scope for declared variables */
        this.adjustlocalvars(nvars);
        fs.reserveregs(nvars);
        this.block();
        fs.leaveblock(); /* end of scope for declared variables */
        fs.patchtohere(prep);
        int endfor;
        if (isnum) {
            endfor = fs.codeAsBx(Lua.OP_FORLOOP, base, NO_JUMP);
        } else {
            endfor = fs.codeABC(Lua.OP_TFORLOOP, base, 0, nvars);
        }
        fs.fixline(line); /* pretend that `Lua.OP_FOR' starts the loop */
        fs.patchlist((isnum ? endfor : fs.jump()), prep + 1);
    }

    void fornum(LuaString varname, int line) {
        /* fornum -> NAME = exp1,exp1[,exp1] forbody */
        FuncState fs = getCurrentFuncState();
        final int base = fs.freereg;

        this.new_localvar(STR_FOR_INDEX, 0);
        this.new_localvar(STR_FOR_LIMIT, 1);
        this.new_localvar(STR_FOR_STEP, 2);
        this.new_localvar(varname, 3);
        this.checknext('=');
        this.exp1(); /* initial value */
        this.checknext(',');
        this.exp1(); /* limit */
        if (this.testnext(',')) {
            this.exp1(); /* optional step */
        } else { /* default step = 1 */
            fs.codeABx(Lua.OP_LOADK, fs.freereg, fs.numberK(LuaInteger.valueOf(1)));
            fs.reserveregs(1);
        }
        this.forbody(base, line, 1, true);
    }

    void forlist(LuaString indexname) {
        /* forlist -> NAME {,NAME} IN explist1 forbody */
        FuncState fs = getCurrentFuncState();
        int nvars = 0;
        final int base = fs.freereg;
        /* create control variables */
        this.new_localvar(STR_FOR_GENERATOR, nvars++);
        this.new_localvar(STR_FOR_STATE, nvars++);
        this.new_localvar(STR_FOR_CONTROL, nvars++);
        /* create declared variables */
        this.new_localvar(indexname, nvars++);
        while (this.testnext(',')) {
            this.new_localvar(this.str_checkname(), nvars++);
        }
        this.checknext(TK_IN);
        int line = this.linenumber;

        ExpDesc e = new ExpDesc();
        this.adjust_assign(3, this.explist1(e), e);
        fs.checkstack(3); /* extra space to call generator */
        this.forbody(base, line, nvars - 3, false);
    }

    void forstat(int line) {
        /* forstat -> FOR (fornum | forlist) END */
        FuncState fs = getCurrentFuncState();
        LuaString varname;
        BlockCnt bl = new BlockCnt();
        fs.enterblock(bl, true); /* scope for loop and control variables */
        this.next(); /* skip `for' */
        varname = this.str_checkname(); /* first variable name */
        switch (this.t.token) {
        case '=':
            this.fornum(varname, line);
            break;
        case ',':
        case TK_IN:
            this.forlist(varname);
            break;
        default:
            this.syntaxerror(luaQL("=") + " or " + luaQL("in") + " expected");
        }
        this.check_match(TK_END, TK_FOR, line);
        fs.leaveblock(); /* loop scope (`break' jumps to this point) */
    }

    int test_then_block() {
        /* test_then_block -> [IF | ELSEIF] cond THEN block */
        int condexit;
        this.next(); /* skip IF or ELSEIF */
        condexit = this.cond();
        this.checknext(TK_THEN);
        this.block(); /* `then' part */
        return condexit;
    }

    void ifstat(int line) {
        /*
         * ifstat -> IF cond THEN block {ELSEIF cond THEN block} [ELSE block]
         * END
         */
        FuncState fs = getCurrentFuncState();
        int flist;
        IntPtr escapelist = new IntPtr(NO_JUMP);
        flist = test_then_block(); /* IF cond THEN block */
        while (this.t.token == TK_ELSEIF) {
            fs.concat(escapelist, fs.jump());
            fs.patchtohere(flist);
            flist = test_then_block(); /* ELSEIF cond THEN block */
        }
        if (this.t.token == TK_ELSE) {
            fs.concat(escapelist, fs.jump());
            fs.patchtohere(flist);
            this.next(); /* skip ELSE (after patch, for correct line info) */
            this.block(); /* `else' part */
        } else {
            fs.concat(escapelist, flist);
        }
        fs.patchtohere(escapelist.i);
        this.check_match(TK_END, TK_IF, line);
    }

    void localfunc() {
        FuncState fs = getCurrentFuncState();
        this.new_localvar(this.str_checkname(), 0);

        ExpDesc v = new ExpDesc();
        v.init(VLOCAL, fs.freereg);
        fs.reserveregs(1);
        this.adjustlocalvars(1);

        ExpDesc b = new ExpDesc();
        this.body(b, false, this.linenumber);
        fs.storevar(v, b);
        /* debug information will only see the variable after this point! */
        fs.getlocvar(fs.nactvar - 1).startpc = fs.pc;
    }

    void localstat() {
        /* stat -> LOCAL NAME {`,' NAME} [`=' explist1] */
        int nvars = 0;
        int nexps;
        ExpDesc e = new ExpDesc();
        do {
            this.new_localvar(this.str_checkname(), nvars++);
        } while (this.testnext(','));
        if (this.testnext('=')) {
            nexps = this.explist1(e);
        } else {
            e.k = VVOID;
            nexps = 0;
        }
        this.adjust_assign(nvars, nexps, e);
        this.adjustlocalvars(nvars);
    }

    boolean funcname(ExpDesc v) {
        /* funcname -> NAME {field} [`:' NAME] */
        boolean needself = false;
        this.singlevar(v);
        while (this.t.token == '.') {
            this.field(v);
        }
        if (this.t.token == ':') {
            needself = true;
            this.field(v);
        }
        return needself;
    }

    void funcstat(int line) {
        /* funcstat -> FUNCTION funcname body */
        boolean needself;
        ExpDesc v = new ExpDesc();
        ExpDesc b = new ExpDesc();
        this.next(); /* skip FUNCTION */
        needself = this.funcname(v);
        this.body(b, needself, line);

        FuncState fs = getCurrentFuncState();
        fs.storevar(v, b);
        fs.fixline(line); /* definition `happens' in the first line */
    }

    void exprstat() {
        /* stat -> func | assignment */
        FuncState fs = getCurrentFuncState();
        LhsAssign v = new LhsAssign();
        this.primaryexp(v.v);
        if (v.v.k == VCALL) {
            LuaC.setArgC(fs.getcodePtr(v.v), 1); /* call statement uses no results */
        } else { /* stat -> assignment */
            v.prev = null;
            this.assignment(v, 1);
        }
    }

    void retstat() {
        /* stat -> RETURN explist */
        FuncState fs = getCurrentFuncState();
        ExpDesc e = new ExpDesc();
        this.next(); /* skip RETURN */

        /* registers with returned values */
        int first;
        int nret;
        if (block_follow(this.t.token) || this.t.token == ';') {
            first = nret = 0; /*
                               * return no values
                               */
        } else {
            nret = this.explist1(e); /* optional return values */
            if (hasmultret(e.k)) {
                fs.setmultret(e);
                if (e.k == VCALL && nret == 1) { /* tail call? */
                    LuaC.setOpcode(fs.getcodePtr(e), Lua.OP_TAILCALL);
                    LuaC.luaAssert(Lua.getArgA(fs.getcode(e)) == fs.nactvar);
                }
                first = fs.nactvar;
                nret = Lua.LUA_MULTRET; /* return all values */
            } else {
                if (nret == 1) {
                    first = fs.exp2anyreg(e);
                } else {
                    fs.exp2nextreg(e); /* values must go to the `stack' */
                    first = fs.nactvar; /* return all `active' values */
                    LuaC.luaAssert(nret == fs.freereg - first);
                }
            }
        }
        fs.ret(first, nret);
    }

    boolean statement() {
        int line = this.linenumber; /* may be needed for error messages */
        switch (this.t.token) {
        case TK_IF: { /* stat -> ifstat */
            this.ifstat(line);
            return false;
        }
        case TK_WHILE: { /* stat -> whilestat */
            this.whilestat(line);
            return false;
        }
        case TK_DO: { /* stat -> DO block END */
            this.next(); /* skip DO */
            this.block();
            this.check_match(TK_END, TK_DO, line);
            return false;
        }
        case TK_FOR: { /* stat -> forstat */
            this.forstat(line);
            return false;
        }
        case TK_REPEAT: { /* stat -> repeatstat */
            this.repeatstat(line);
            return false;
        }
        case TK_FUNCTION: {
            this.funcstat(line); /* stat -> funcstat */
            return false;
        }
        case TK_LOCAL: { /* stat -> localstat */
            this.next(); /* skip LOCAL */
            if (this.testnext(TK_FUNCTION)) {
                this.localfunc();
            } else {
                this.localstat();
            }
            return false;
        }
        case TK_RETURN: { /* stat -> retstat */
            this.retstat();
            return true; /* must be last statement */
        }
        case TK_BREAK: { /* stat -> breakstat */
            this.next(); /* skip BREAK */
            this.breakstat();
            return true; /* must be last statement */
        }
        default: {
            this.exprstat();
            return false; /* to avoid warnings */
        }
        }
    }

    void chunk() {
        /* chunk -> { stat [`;'] } */
        boolean islast = false;
        this.enterlevel();
        while (!islast && !block_follow(this.t.token)) {
            islast = this.statement();
            this.testnext(';');

            FuncState fs = getCurrentFuncState();
            LuaC.luaAssert(fs.f.maxstacksize >= fs.freereg && fs.freereg >= fs.nactvar);
            fs.freereg = fs.nactvar; /* free registers */
        }
        this.leavelevel();
    }

    /* }====================================================================== */

}
