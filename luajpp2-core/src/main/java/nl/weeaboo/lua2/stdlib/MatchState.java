package nl.weeaboo.lua2.stdlib;

import static nl.weeaboo.lua2.stdlib.StringLib.L_ESC;
import static nl.weeaboo.lua2.vm.LuaConstants.NONE;
import static nl.weeaboo.lua2.vm.LuaConstants.TFUNCTION;
import static nl.weeaboo.lua2.vm.LuaConstants.TNUMBER;
import static nl.weeaboo.lua2.vm.LuaConstants.TSTRING;
import static nl.weeaboo.lua2.vm.LuaConstants.TTABLE;
import static nl.weeaboo.lua2.vm.LuaValue.error;
import static nl.weeaboo.lua2.vm.LuaValue.valueOf;
import static nl.weeaboo.lua2.vm.LuaValue.varargsOf;

import java.io.Serializable;

import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.vm.Buffer;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Varargs;

@LuaSerializable
final class MatchState implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final int MAX_CAPTURES = 32;

    private static final int CAP_UNFINISHED = -1;
    private static final int CAP_POSITION = -2;

    private static final byte MASK_ALPHA = 0x01;
    private static final byte MASK_LOWERCASE = 0x02;
    private static final byte MASK_UPPERCASE = 0x04;
    private static final byte MASK_DIGIT = 0x08;
    private static final byte MASK_PUNCT = 0x10;
    private static final byte MASK_SPACE = 0x20;
    private static final byte MASK_CONTROL = 0x40;
    private static final byte MASK_HEXDIGIT = (byte)0x80;

    private static final byte[] CHAR_TABLE;

    static {
        CHAR_TABLE = new byte[256];

        for (int i = 0; i < 256; ++i) {
            final char c = (char)i;
            CHAR_TABLE[i] = (byte)((Character.isDigit(c) ? MASK_DIGIT : 0)
                    | (Character.isLowerCase(c) ? MASK_LOWERCASE : 0)
                    | (Character.isUpperCase(c) ? MASK_UPPERCASE : 0)
                    | ((c < ' ' || c == 0x7F) ? MASK_CONTROL : 0));
            if ((c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F') || (c >= '0' && c <= '9')) {
                CHAR_TABLE[i] |= MASK_HEXDIGIT;
            }
            if ((c >= '!' && c <= '/') || (c >= ':' && c <= '@')) {
                CHAR_TABLE[i] |= MASK_PUNCT;
            }
            if ((CHAR_TABLE[i] & (MASK_LOWERCASE | MASK_UPPERCASE)) != 0) {
                CHAR_TABLE[i] |= MASK_ALPHA;
            }
        }

        CHAR_TABLE[' '] = MASK_SPACE;
        CHAR_TABLE['\r'] |= MASK_SPACE;
        CHAR_TABLE['\n'] |= MASK_SPACE;
        CHAR_TABLE['\t'] |= MASK_SPACE;
        CHAR_TABLE[0x0C /* '\v' */] |= MASK_SPACE;
        CHAR_TABLE['\f'] |= MASK_SPACE;
    }

    final LuaString s;
    final LuaString p;
    final Varargs args;
    int level;
    int[] cinit;
    int[] clen;

    MatchState(Varargs args, LuaString s, LuaString pattern) {
        this.s = s;
        this.p = pattern;
        this.args = args;
        this.level = 0;
        this.cinit = new int[MAX_CAPTURES];
        this.clen = new int[MAX_CAPTURES];
    }

    void reset() {
        level = 0;
    }

    private void add_s(Buffer lbuf, LuaString news, int soff, int e) {
        int l = news.length();
        for (int i = 0; i < l; ++i) {
            byte b = (byte)news.luaByte(i);
            if (b != L_ESC) {
                lbuf.append(b);
            } else {
                ++i; // skip ESC
                b = (byte)news.luaByte(i);
                if (!Character.isDigit((char)b)) {
                    lbuf.append(b);
                } else if (b == '0') {
                    lbuf.append(s.substring(soff, e));
                } else {
                    lbuf.append(push_onecapture(b - '1', soff, e).strvalue());
                }
            }
        }
    }

    public void add_value(Buffer lbuf, int soffset, int end, LuaValue repl) {
        switch (repl.type()) {
        case TSTRING:
        case TNUMBER:
            add_s(lbuf, repl.strvalue(), soffset, end);
            return;

        case TFUNCTION:
            repl = repl.invoke(push_captures(true, soffset, end)).arg1();
            break;

        case TTABLE:
            // Need to call push_onecapture here for the error checking
            repl = repl.get(push_onecapture(0, soffset, end));
            break;

        default:
            error("bad argument: string/function/table expected");
            return;
        }

        if (!repl.toboolean()) {
            repl = s.substring(soffset, end);
        } else if (!repl.isstring()) {
            error("invalid replacement value (a " + repl.typename() + ")");
        }
        lbuf.append(repl.strvalue());
    }

    Varargs push_captures(boolean wholeMatch, int soff, int end) {
        int nlevels = (this.level == 0 && wholeMatch) ? 1 : this.level;
        switch (nlevels) {
        case 0:
            return NONE;
        case 1:
            return push_onecapture(0, soff, end);
        }
        LuaValue[] v = new LuaValue[nlevels];
        for (int i = 0; i < nlevels; ++i) {
            v[i] = push_onecapture(i, soff, end);
        }
        return varargsOf(v);
    }

    private LuaValue push_onecapture(int i, int soff, int end) {
        if (i >= this.level) {
            if (i == 0) {
                return s.substring(soff, end);
            } else {
                throw error("invalid capture index");
            }
        } else {
            int l = clen[i];
            if (l == CAP_UNFINISHED) {
                throw error("unfinished capture");
            }
            if (l == CAP_POSITION) {
                return valueOf(cinit[i] + 1);
            } else {
                int begin = cinit[i];
                return s.substring(begin, begin + l);
            }
        }
    }

    private int check_capture(int l) {
        l -= '1';
        if (l < 0 || l >= level || this.clen[l] == CAP_UNFINISHED) {
            error("invalid capture index");
        }
        return l;
    }

    private int capture_to_close() {
        int level = this.level;
        for (level--; level >= 0; level--) {
            if (clen[level] == CAP_UNFINISHED) {
                return level;
            }
        }
        error("invalid pattern capture");
        return 0;
    }

    int classend(int poffset) {
        switch (p.luaByte(poffset++)) {
        case L_ESC:
            if (poffset == p.length()) {
                error("malformed pattern (ends with %)");
            }
            return poffset + 1;

        case '[':
            if (p.luaByte(poffset) == '^') {
                poffset++;
            }
            do {
                if (poffset == p.length()) {
                    error("malformed pattern (missing ])");
                }
                if (p.luaByte(poffset++) == L_ESC && poffset != p.length()) {
                    poffset++;
                }
            } while (p.luaByte(poffset) != ']');
            return poffset + 1;
        default:
            return poffset;
        }
    }

    static boolean match_class(int c, int cl) {
        final char lcl = Character.toLowerCase((char)cl);
        int cdata = CHAR_TABLE[c];

        boolean res;
        switch (lcl) {
        case 'a':
            res = (cdata & MASK_ALPHA) != 0;
            break;
        case 'd':
            res = (cdata & MASK_DIGIT) != 0;
            break;
        case 'l':
            res = (cdata & MASK_LOWERCASE) != 0;
            break;
        case 'u':
            res = (cdata & MASK_UPPERCASE) != 0;
            break;
        case 'c':
            res = (cdata & MASK_CONTROL) != 0;
            break;
        case 'p':
            res = (cdata & MASK_PUNCT) != 0;
            break;
        case 's':
            res = (cdata & MASK_SPACE) != 0;
            break;
        case 'w':
            res = (cdata & (MASK_ALPHA | MASK_DIGIT)) != 0;
            break;
        case 'x':
            res = (cdata & MASK_HEXDIGIT) != 0;
            break;
        case 'z':
            res = (c == 0);
            break;
        default:
            return cl == c;
        }
        return (lcl == cl) ? res : !res;
    }

    boolean matchbracketclass(int c, int poff, int ec) {
        boolean sig = true;
        if (p.luaByte(poff + 1) == '^') {
            sig = false;
            poff++;
        }
        while (++poff < ec) {
            if (p.luaByte(poff) == L_ESC) {
                poff++;
                if (match_class(c, p.luaByte(poff))) {
                    return sig;
                }
            } else if ((p.luaByte(poff + 1) == '-') && (poff + 2 < ec)) {
                poff += 2;
                if (p.luaByte(poff - 2) <= c && c <= p.luaByte(poff)) {
                    return sig;
                }
            } else if (p.luaByte(poff) == c) {
                return sig;
            }
        }
        return !sig;
    }

    boolean singlematch(int c, int poff, int ep) {
        switch (p.luaByte(poff)) {
        case '.':
            return true;
        case L_ESC:
            return match_class(c, p.luaByte(poff + 1));
        case '[':
            return matchbracketclass(c, poff, ep - 1);
        default:
            return p.luaByte(poff) == c;
        }
    }

    /**
     * Perform pattern matching. If there is a match, returns offset into s where match ends, otherwise
     * returns -1.
     */
    int match(int soffset, int poffset) {
        while (true) {
            // Check if we are at the end of the pattern -
            // equivalent to the '\0' case in the C version, but our pattern
            // string is not NUL-terminated.
            if (poffset == p.length()) {
                return soffset;
            }
            switch (p.luaByte(poffset)) {
            case '(':
                if (++poffset < p.length() && p.luaByte(poffset) == ')') {
                    return start_capture(soffset, poffset + 1, CAP_POSITION);
                } else {
                    return start_capture(soffset, poffset, CAP_UNFINISHED);
                }
            case ')':
                return end_capture(soffset, poffset + 1);
            case L_ESC:
                if (poffset + 1 == p.length()) {
                    error("malformed pattern (ends with '%')");
                }
                switch (p.luaByte(poffset + 1)) {
                case 'b':
                    soffset = matchbalance(soffset, poffset + 2);
                    if (soffset == -1) {
                        return -1;
                    }
                    poffset += 4;
                    continue;
                case 'f': {
                    poffset += 2;
                    if (p.luaByte(poffset) != '[') {
                        error("Missing [ after %f in pattern");
                    }
                    int ep = classend(poffset);
                    int previous = (soffset == 0) ? '\0' : s.luaByte(soffset - 1);
                    int current = (soffset == s.length()) ? '\0' : s.luaByte(soffset);
                    if (matchbracketclass(previous, poffset, ep - 1)
                            || !matchbracketclass(current, poffset, ep - 1)) {

                        return -1;
                    }
                    poffset = ep;
                    continue;
                }
                default: {
                    int c = p.luaByte(poffset + 1);
                    if (Character.isDigit((char)c)) {
                        soffset = match_capture(soffset, c);
                        if (soffset == -1) {
                            return -1;
                        }
                        return match(soffset, poffset + 2);
                    }
                }
                }
                break;
            case '$':
                if (poffset + 1 == p.length()) {
                    return (soffset == s.length()) ? soffset : -1;
                }
            }
            int ep = classend(poffset);
            boolean m = soffset < s.length() && singlematch(s.luaByte(soffset), poffset, ep);
            int pc = (ep < p.length()) ? p.luaByte(ep) : '\0';

            switch (pc) {
            case '?':
                int res;
                if (m && ((res = match(soffset + 1, ep + 1)) != -1)) {
                    return res;
                }
                poffset = ep + 1;
                continue;
            case '*':
                return max_expand(soffset, poffset, ep);
            case '+':
                return (m ? max_expand(soffset + 1, poffset, ep) : -1);
            case '-':
                return min_expand(soffset, poffset, ep);
            default:
                if (!m) {
                    return -1;
                }
                soffset++;
                poffset = ep;
                continue;
            }
        }
    }

    int max_expand(int soff, int poff, int ep) {
        int i = 0;
        while (soff + i < s.length() && singlematch(s.luaByte(soff + i), poff, ep)) {
            i++;
        }
        while (i >= 0) {
            int res = match(soff + i, ep + 1);
            if (res != -1) {
                return res;
            }
            i--;
        }
        return -1;
    }

    int min_expand(int soff, int poff, int ep) {
        for (;;) {
            int res = match(soff, ep + 1);
            if (res != -1) {
                return res;
            } else if (soff < s.length() && singlematch(s.luaByte(soff), poff, ep)) {
                soff++;
            } else {
                return -1;
            }
        }
    }

    int start_capture(int soff, int poff, int what) {
        int level = this.level;
        if (level >= MAX_CAPTURES) {
            error("too many captures");
        }
        cinit[level] = soff;
        clen[level] = what;
        this.level = level + 1;
        int res = match(soff, poff);
        if (res == -1) {
            this.level--;
        }
        return res;
    }

    int end_capture(int soff, int poff) {
        int l = capture_to_close();
        int res;
        clen[l] = soff - cinit[l];
        if ((res = match(soff, poff)) == -1) {
            clen[l] = CAP_UNFINISHED;
        }
        return res;
    }

    int match_capture(int soff, int l) {
        l = check_capture(l);
        int len = clen[l];
        if ((s.length() - soff) >= len && LuaString.equals(s, cinit[l], s, soff, len)) {
            return soff + len;
        } else {
            return -1;
        }
    }

    int matchbalance(int soff, int poff) {
        final int plen = p.length();
        if (poff == plen || poff + 1 == plen) {
            error("unbalanced pattern");
        }
        if (soff >= s.length() || s.luaByte(soff) != p.luaByte(poff)) {
            return -1;
        } else {
            int b = p.luaByte(poff);
            int e = p.luaByte(poff + 1);
            int cont = 1;
            while (++soff < s.length()) {
                if (s.luaByte(soff) == e) {
                    if (--cont == 0) {
                        return soff + 1;
                    }
                } else if (s.luaByte(soff) == b) {
                    cont++;
                }
            }
        }
        return -1;
    }
}
