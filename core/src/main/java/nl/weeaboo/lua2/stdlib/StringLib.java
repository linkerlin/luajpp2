package nl.weeaboo.lua2.stdlib;

import static nl.weeaboo.lua2.vm.LuaConstants.EMPTYSTRING;
import static nl.weeaboo.lua2.vm.LuaConstants.NONE;
import static nl.weeaboo.lua2.vm.LuaNil.NIL;
import static nl.weeaboo.lua2.vm.LuaValue.tableOf;
import static nl.weeaboo.lua2.vm.LuaValue.valueOf;
import static nl.weeaboo.lua2.vm.LuaValue.varargsOf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.compiler.DumpState;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.lib2.LuaBoundFunction;
import nl.weeaboo.lua2.lib2.LuaLib;
import nl.weeaboo.lua2.vm.Buffer;
import nl.weeaboo.lua2.vm.LuaClosure;
import nl.weeaboo.lua2.vm.LuaConstants;
import nl.weeaboo.lua2.vm.LuaError;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Varargs;

@LuaSerializable
public final class StringLib extends LuaLib {

    static final int L_ESC = '%';

    private static final long serialVersionUID = 1L;
    private static final LuaString SPECIALS = valueOf("^$*+?.([%-");

    public StringLib() {
        super("string");
    }

    @Override
    protected void initTable(LuaTable table) throws LuaException {
        super.initTable(table);

        LuaString.s_metatable = tableOf(new LuaValue[] { LuaConstants.INDEX, table });
        LuaRunState.getCurrent().setIsLoaded("string", table);
    }



    /**
     * string.dump (function)
     *
     * Returns a string containing a binary representation of the given function, so that a later loadstring
     * on this string returns a copy of the function. function must be a Lua function without upvalues.
     */
    @LuaBoundFunction
    public Varargs dump(Varargs args) {
        LuaClosure f = args.checkclosure(1);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            DumpState.dump(f.getPrototype(), baos, true);
            return LuaString.valueOf(baos.toByteArray());
        } catch (IOException e) {
            // This should never happen -- ByteArrayOutputStream doesn't throw these.
            throw new LuaError(e);
        }
    }

    /**
     * string.len (s)
     *
     * Receives a string and returns its length. The empty string "" has length 0. Embedded zeros are counted,
     * so "a\000bc\000" has length 5.
     */
    @LuaBoundFunction
    public Varargs len(Varargs args) {
        return args.checkstring(1).len();
    }

    /**
     * string.lower (s)
     *
     * Receives a string and returns a copy of this string with all uppercase letters changed to lowercase.
     * All other characters are left unchanged. The definition of what an uppercase letter is depends on the
     * current locale.
     */
    @LuaBoundFunction
    public Varargs lower(Varargs args) {
        return valueOf(args.checkjstring(1).toLowerCase());
    }

    /**
     * string.upper (s)
     *
     * Receives a string and returns a copy of this string with all lowercase letters changed to uppercase.
     * All other characters are left unchanged. The definition of what a lowercase letter is depends on the
     * current locale.
     */
    @LuaBoundFunction
    public Varargs upper(Varargs args) {
        return valueOf(args.checkjstring(1).toUpperCase());
    }

    /**
     * string.reverse (s)
     *
     * Returns a string that is the string s reversed.
     */
    @LuaBoundFunction
    public Varargs reverse(Varargs args) {
        LuaString s = args.checkstring(1);
        int n = s.length();
        byte[] b = new byte[n];
        for (int i = 0, j = n - 1; i < n; i++, j--) {
            b[j] = (byte)s.luaByte(i);
        }
        return LuaString.valueOf(b);
    }

    /**
     * string.byte (s [, i [, j]])
     *
     * Returns the internal numerical codes of the characters s[i], s[i+1], ..., s[j]. The default value for i
     * is 1; the default value for j is i.
     *
     * Note that numerical codes are not necessarily portable across platforms.
     *
     * @param args the calling args
     */
    @LuaBoundFunction(luaName = "byte")
    public Varargs byte_(Varargs args) {
        LuaString s = args.checkstring(1);
        int l = s.length();
        int posi = posrelat(args.optint(2, 1), l);
        int pose = posrelat(args.optint(3, posi), l);
        if (posi <= 0) {
            posi = 1;
        }
        if (pose > l) {
            pose = l;
        }
        if (posi > pose) {
            return NONE; /* empty interval; return no values */
        }
        int n = (pose - posi + 1);
        if (posi + n <= pose) {
            throw new LuaError("string slice too long");
        }
        LuaValue[] v = new LuaValue[n];
        for (int i = 0; i < n; i++) {
            v[i] = valueOf(s.luaByte(posi + i - 1));
        }
        return varargsOf(v);
    }

    /**
     * string.char (...)
     *
     * Receives zero or more integers. Returns a string with length equal to the number of arguments, in which
     * each character has the internal numerical code equal to its corresponding argument.
     *
     * Note that numerical codes are not necessarily portable across platforms.
     *
     * @param args the calling VM
     */
    @LuaBoundFunction(luaName = "char")
    public Varargs char_(Varargs args) {
        int n = args.narg();
        byte[] bytes = new byte[n];
        for (int i = 0, a = 1; i < n; i++, a++) {
            int c = args.checkint(a);
            if (c < 0 || c >= 256) {
                LuaValue.argerror(a, "invalid value");
            }
            bytes[i] = (byte)c;
        }
        return LuaString.valueOf(bytes);
    }

    /**
     * string.find (s, pattern [, init [, plain]])
     *
     * Looks for the first match of pattern in the string s. If it finds a match, then find returns the
     * indices of s where this occurrence starts and ends; otherwise, it returns nil. A third, optional
     * numerical argument init specifies where to start the search; its default value is 1 and may be
     * negative. A value of true as a fourth, optional argument plain turns off the pattern matching
     * facilities, so the function does a plain "find substring" operation, with no characters in pattern
     * being considered "magic". Note that if plain is given, then init must be given as well.
     *
     * If the pattern has captures, then in a successful match the captured values are also returned, after
     * the two indices.
     */
    @LuaBoundFunction
    public Varargs find(Varargs args) {
        return strFindAux(args, true);
    }

    /**
     * string.format (formatstring, ...)
     *
     * Returns a formatted version of its variable number of arguments following the description given in its
     * first argument (which must be a string). The format string follows the same rules as the printf family
     * of standard C functions. The only differences are that the options/modifiers *, l, L, n, p, and h are
     * not supported and that there is an extra option, q. The q option formats a string in a form suitable to
     * be safely read back by the Lua interpreter: the string is written between double quotes, and all double
     * quotes, newlines, embedded zeros, and backslashes in the string are correctly escaped when written. For
     * instance, the call string.format('%q', 'a string with "quotes" and \n new line')
     *
     * will produce the string: "a string with \"quotes\" and \ new line"
     *
     * The options c, d, E, e, f, g, G, i, o, u, X, and x all expect a number as argument, whereas q and s
     * expect a string.
     *
     * This function does not accept string values containing embedded zeros, except as arguments to the q
     * option.
     */
    @LuaBoundFunction
    public Varargs format(Varargs args) {
        LuaString fmt = args.checkstring(1);
        final int n = fmt.length();
        Buffer result = new Buffer(n);
        int arg = 1;
        int c;

        for (int i = 0; i < n;) {
            switch (c = fmt.luaByte(i++)) {
            case '\n':
                result.append("\n");
                break;
            default:
                result.append((byte)c);
                break;
            case L_ESC:
                if (i < n) {
                    if ((c = fmt.luaByte(i)) == L_ESC) {
                        ++i;
                        result.append((byte)L_ESC);
                    } else {
                        arg++;
                        FormatDesc fdsc = new FormatDesc(fmt, i);
                        i += fdsc.length;
                        switch (fdsc.conversion) {
                        case 'c':
                            fdsc.format(result, (byte)args.checkint(arg));
                            break;
                        case 'i':
                        case 'd':
                            fdsc.format(result, args.checkint(arg));
                            break;
                        case 'o':
                        case 'u':
                        case 'x':
                        case 'X':
                            fdsc.format(result, args.checklong(arg));
                            break;
                        case 'e':
                        case 'E':
                        case 'f':
                        case 'g':
                        case 'G':
                            fdsc.format(result, args.checkdouble(arg));
                            break;
                        case 'q':
                            addquoted(result, args.checkstring(arg));
                            break;
                        case 's': {
                            LuaString s = args.checkstring(arg);
                            if (fdsc.getPrecision() == -1 && s.length() >= 100) {
                                // No precision specified and the string is too long to be formatted
                                result.append(s);
                            } else {
                                fdsc.format(result, s);
                            }
                        }
                            break;
                        default:
                            throw new LuaError("invalid option '%" + (char)fdsc.conversion + "' to 'format'");
                        }
                    }
                }
            }
        }

        return result.tostring();
    }

    /**
     * string.gmatch (s, pattern)
     *
     * Returns an iterator function that, each time it is called, returns the next captures from pattern over
     * string s. If pattern specifies no captures, then the whole match is produced in each call.
     *
     * As an example, the following loop s = "hello world from Lua" for w in string.gmatch(s, "%a+") do
     * print(w) end
     *
     * will iterate over all the words from string s, printing one per line. The next example collects all
     * pairs key=value from the given string into a table: t = {} s = "from=world, to=Lua" for k, v in
     * string.gmatch(s, "(%w+)=(%w+)") do t[k] = v end
     *
     * For this function, a '^' at the start of a pattern does not work as an anchor, as this would prevent
     * the iteration.
     */
    @LuaBoundFunction
    public Varargs gmatch(Varargs args) {
        LuaString src = args.checkstring(1);
        LuaString pat = args.checkstring(2);
        return new GMatchAux(args, src, pat);
    }

    /**
     * string.gsub (s, pattern, repl [, n]) Returns a copy of s in which all (or the first n, if given)
     * occurrences of the pattern have been replaced by a replacement string specified by repl, which may be a
     * string, a table, or a function. gsub also returns, as its second value, the total number of matches
     * that occurred.
     *
     * If repl is a string, then its value is used for replacement. The character % works as an escape
     * character: any sequence in repl of the form %n, with n between 1 and 9, stands for the value of the
     * n-th captured substring (see below). The sequence %0 stands for the whole match. The sequence %% stands
     * for a single %.
     *
     * If repl is a table, then the table is queried for every match, using the first capture as the key; if
     * the pattern specifies no captures, then the whole match is used as the key.
     *
     * If repl is a function, then this function is called every time a match occurs, with all captured
     * substrings passed as arguments, in order; if the pattern specifies no captures, then the whole match is
     * passed as a sole argument.
     *
     * If the value returned by the table query or by the function call is a string or a number, then it is
     * used as the replacement string; otherwise, if it is false or nil, then there is no replacement (that
     * is, the original match is kept in the string).
     *
     * Here are some examples: x = string.gsub("hello world", "(%w+)", "%1 %1") --> x=
     * "hello hello world world"
     *
     * x = string.gsub("hello world", "%w+", "%0 %0", 1) --> x="hello hello world"
     *
     * x = string.gsub("hello world from Lua", "(%w+)%s*(%w+)", "%2 %1") --> x="world hello Lua from"
     *
     * x = string.gsub("home = $HOME, user = $USER", "%$(%w+)", os.getenv) --> x=
     * "home = /home/roberto, user = roberto"
     *
     * x = string.gsub("4+5 = $return 4+5$", "%$(.-)%$", function (s) return loadstring(s)() end) --> x=
     * "4+5 = 9"
     *
     * local t = {name="lua", version="5.1"} x = string.gsub("$name-$version.tar.gz", "%$(%w+)", t) -->
     * x="lua-5.1.tar.gz"
     */
    @LuaBoundFunction
    public Varargs gsub(Varargs args) {
        LuaString src = args.checkstring(1);
        final int srclen = src.length();
        LuaString p = args.checkstring(2);
        LuaValue repl = args.arg(3);
        int maxS = args.optint(4, srclen + 1);
        final boolean anchor = p.length() > 0 && p.charAt(0) == '^';

        Buffer lbuf = new Buffer(srclen);
        MatchState ms = new MatchState(args, src, p);

        int soffset = 0;
        int n = 0;
        while (n < maxS) {
            ms.reset();
            int res = ms.match(soffset, anchor ? 1 : 0);
            if (res != -1) {
                n++;
                ms.add_value(lbuf, soffset, res, repl);
            }
            if (res != -1 && res > soffset) {
                soffset = res;
            } else if (soffset < srclen) {
                lbuf.append((byte)src.luaByte(soffset++));
            } else {
                break;
            }
            if (anchor) {
                break;
            }
        }
        lbuf.append(src.substring(soffset, srclen));
        return varargsOf(lbuf.tostring(), valueOf(n));
    }

    /**
     * string.match (s, pattern [, init])
     *
     * Looks for the first match of pattern in the string s. If it finds one, then match returns the captures
     * from the pattern; otherwise it returns nil. If pattern specifies no captures, then the whole match is
     * returned. A third, optional numerical argument init specifies where to start the search; its default
     * value is 1 and may be negative.
     */
    @LuaBoundFunction
    public Varargs match(Varargs args) {
        return strFindAux(args, false);
    }

    /**
     * string.rep (s, n)
     *
     * Returns a string that is the concatenation of n copies of the string s.
     */
    @LuaBoundFunction
    public Varargs rep(Varargs args) {
        LuaString s = args.checkstring(1);
        int n = args.checkint(2);
        final byte[] bytes = new byte[s.length() * n];
        int len = s.length();
        for (int offset = 0; offset < bytes.length; offset += len) {
            s.copyInto(0, bytes, offset, len);
        }
        return LuaString.valueOf(bytes);
    }

    /**
     * string.sub (s, i [, j])
     *
     * Returns the substring of s that starts at i and continues until j; i and j may be negative. If j is
     * absent, then it is assumed to be equal to -1 (which is the same as the string length). In particular,
     * the call string.sub(s,1,j) returns a prefix of s with length j, and string.sub(s, -i) returns a suffix
     * of s with length i.
     */
    @LuaBoundFunction
    public Varargs sub(Varargs args) {
        final LuaString s = args.checkstring(1);
        final int l = s.length();

        int start = posrelat(args.checkint(2), l);
        int end = posrelat(args.optint(3, -1), l);

        if (start < 1) {
            start = 1;
        }
        if (end > l) {
            end = l;
        }

        if (start <= end) {
            return s.substring(start - 1, end);
        } else {
            return EMPTYSTRING;
        }
    }

    private static int posrelat(int pos, int len) {
        return (pos >= 0) ? pos : len + pos + 1;
    }

    private static void addquoted(Buffer buf, LuaString s) {
        int c;
        buf.append((byte)'"');
        for (int i = 0, n = s.length(); i < n; i++) {
            switch (c = s.luaByte(i)) {
            case '"':
            case '\\':
            case '\n':
                buf.append((byte)'\\');
                buf.append((byte)c);
                break;
            case '\r':
                buf.append("\\r");
                break;
            case '\0':
                buf.append("\\000");
                break;
            default:
                buf.append((byte)c);
                break;
            }
        }
        buf.append((byte)'"');
    }

    /**
     * This utility method implements both string.find and string.match.
     */
    private static Varargs strFindAux(Varargs args, boolean find) {
        LuaString s = args.checkstring(1);
        LuaString pat = args.checkstring(2);
        int init = args.optint(3, 1);

        if (init > 0) {
            init = Math.min(init - 1, s.length());
        } else if (init < 0) {
            init = Math.max(0, s.length() + init);
        }

        boolean fastMatch = find && (args.arg(4).toboolean() || pat.indexOfAny(SPECIALS) == -1);

        if (fastMatch) {
            int result = s.indexOf(pat, init);
            if (result != -1) {
                return varargsOf(valueOf(result + 1), valueOf(result + pat.length()));
            }
        } else {
            MatchState ms = new MatchState(args, s, pat);

            boolean anchor = false;
            int poff = 0;
            if (pat.luaByte(0) == '^') {
                anchor = true;
                poff = 1;
            }

            int soff = init;
            do {
                int res;
                ms.reset();
                if ((res = ms.match(soff, poff)) != -1) {
                    if (find) {
                        return varargsOf(valueOf(soff + 1), valueOf(res), ms.push_captures(false, soff, res));
                    } else {
                        return ms.push_captures(true, soff, res);
                    }
                }
            } while (soff++ < s.length() && !anchor);
        }
        return NIL;
    }

}
