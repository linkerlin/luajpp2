package nl.weeaboo.lua2.stdlib;

import java.io.Serializable;
import java.util.Locale;

import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.vm.Buffer;
import nl.weeaboo.lua2.vm.LuaError;
import nl.weeaboo.lua2.vm.LuaString;

@LuaSerializable
final class FormatDesc implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int MAX_FLAGS = 5;

    private boolean leftAdjust;
    private boolean zeroPad;
    private boolean explicitPlus;
    private boolean space;

    private int width;
    private int precision;

    public final int conversion;
    public final int length;

    public FormatDesc(LuaString strfrmt, final int start) {
        int p = start;
        int n = strfrmt.length();
        int c = 0;

        boolean moreFlags = true;
        while (moreFlags) {
            switch (c = ((p < n) ? strfrmt.luaByte(p++) : 0)) {
            case '-':
                leftAdjust = true;
                break;
            case '+':
                explicitPlus = true;
                break;
            case ' ':
                space = true;
                break;
            case '0':
                zeroPad = true;
                break;
            default:
                moreFlags = false;
                break;
            }
        }
        if (p - start > MAX_FLAGS) {
            throw new LuaError("invalid format (repeated flags)");
        }

        width = -1;
        if (Character.isDigit((char)c)) {
            width = c - '0';
            c = ((p < n) ? strfrmt.luaByte(p++) : 0);
            if (Character.isDigit((char)c)) {
                width = width * 10 + (c - '0');
                c = ((p < n) ? strfrmt.luaByte(p++) : 0);
            }
        }

        precision = -1;
        if (c == '.') {
            c = ((p < n) ? strfrmt.luaByte(p++) : 0);
            if (Character.isDigit((char)c)) {
                precision = c - '0';
                c = ((p < n) ? strfrmt.luaByte(p++) : 0);
                if (Character.isDigit((char)c)) {
                    precision = precision * 10 + (c - '0');
                    c = ((p < n) ? strfrmt.luaByte(p++) : 0);
                }
            }
        }

        if (Character.isDigit((char)c)) {
            throw new LuaError("invalid format (width or precision too long)");
        }

        zeroPad &= !leftAdjust; // '-' overrides '0'
        conversion = c;
        length = p - start;
    }

    public void format(Buffer buf, byte c) {
        // TODO: not clear that any of width, precision, or flags apply here.
        buf.append(c);
    }

    public void format(Buffer buf, long number) {
        String digits;

        if (number == 0 && precision == 0) {
            digits = "";
        } else {
            int radix;
            switch (conversion) {
            case 'x':
            case 'X':
                radix = 16;
                break;
            case 'o':
                radix = 8;
                break;
            default:
                radix = 10;
                break;
            }
            digits = Long.toString(number, radix);
            if (conversion == 'X') {
                digits = digits.toUpperCase();
            }
        }

        int minwidth = digits.length();
        int ndigits = minwidth;
        int nzeros;

        if (number < 0) {
            ndigits--;
        } else if (explicitPlus || space) {
            minwidth++;
        }

        if (precision > ndigits) {
            nzeros = precision - ndigits;
        } else if (precision == -1 && zeroPad && width > minwidth) {
            nzeros = width - minwidth;
        } else {
            nzeros = 0;
        }

        minwidth += nzeros;
        int nspaces = width > minwidth ? width - minwidth : 0;

        if (!leftAdjust) {
            pad(buf, ' ', nspaces);
        }

        if (number < 0) {
            if (nzeros > 0) {
                buf.append((byte)'-');
                digits = digits.substring(1);
            }
        } else if (explicitPlus) {
            buf.append((byte)'+');
        } else if (space) {
            buf.append((byte)' ');
        }

        if (nzeros > 0) {
            pad(buf, '0', nzeros);
        }

        buf.append(digits);

        if (leftAdjust) {
            pad(buf, ' ', nspaces);
        }
    }

    public void format(Buffer buf, double x) {
        StringBuilder format = new StringBuilder("%");
        if (width >= 0) {
            format.append(width);
        }
        if (precision >= 0) {
            format.append('.');
            format.append(precision);
        }
        format.append('f');

        String str = String.format(Locale.ROOT, format.toString(), x);
        buf.append(str);
    }

    public void format(Buffer buf, LuaString str) {
        int nullindex = str.indexOf((byte)'\0', 0);
        if (nullindex >= 0) {
            str = str.substring(0, nullindex);
        }

        // Trim to precision
        str = trimToPrecision(str);

        int pad = Math.max(0, width - str.length());
        if (leftAdjust) {
            buf.append(str);
            pad(buf, ' ', pad);
        } else {
            pad(buf, ' ', pad);
            buf.append(str);
        }
    }

    private LuaString trimToPrecision(LuaString str) {
        if (precision >= 0) {
            return str.substring(0, Math.min(str.length(), precision));
        } else {
            return str;
        }
    }

    public int getPrecision() {
        return precision;
    }

    public static final void pad(Buffer buf, char c, int n) {
        byte b = (byte)c;
        while (n-- > 0) {
            buf.append(b);
        }
    }

}
