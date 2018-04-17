package nl.weeaboo.lua2.vm.old;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.junit.Assert;
import org.junit.Test;

import nl.weeaboo.lua2.vm.LuaString;

public class StringTest {

    @Test
    public void testToInputStream() throws IOException {
        LuaString str = LuaString.valueOf("Hello");

        InputStream is = str.toInputStream();

        Assert.assertEquals('H', is.read());
        Assert.assertEquals('e', is.read());
        Assert.assertEquals(2, is.skip(2));
        Assert.assertEquals('o', is.read());
        Assert.assertEquals(-1, is.read());

        Assert.assertTrue(is.markSupported());

        is.reset();

        Assert.assertEquals('H', is.read());
        is.mark(4);

        Assert.assertEquals('e', is.read());
        is.reset();
        Assert.assertEquals('e', is.read());

        LuaString substr = str.substring(1, 4);
        Assert.assertEquals(3, substr.length());

        is.close();
        is = substr.toInputStream();

        Assert.assertEquals('e', is.read());
        Assert.assertEquals('l', is.read());
        Assert.assertEquals('l', is.read());
        Assert.assertEquals(-1, is.read());

        is.close();

        is = substr.toInputStream();
        is.reset();
        Assert.assertEquals('e', is.read());
        is.close();
    }

    private static final String userFriendly(String s) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0, n = s.length(); i < n; i++) {
            int c = s.charAt(i);
            if (c < ' ' || c >= 0x80) {
                sb.append("\\u" + Integer.toHexString(0x10000 + c).substring(1));
            } else {
                sb.append((char)c);
            }
        }
        return sb.toString();
    }

    @Test
    public void testUtf820482051() {
        int i = 2048;
        char[] c = { (char)(i + 0), (char)(i + 1), (char)(i + 2), (char)(i + 3) };
        String before = new String(c) + " " + i + "-" + (i + 4);
        LuaString ls = LuaString.valueOf(before);
        String after = ls.tojstring();
        Assert.assertEquals(userFriendly(before), userFriendly(after));
    }

    @Test
    public void testUtf8() {
        for (int i = 4; i < 0xffff; i += 4) {
            char[] c = { (char)(i + 0), (char)(i + 1), (char)(i + 2), (char)(i + 3) };
            String before = new String(c) + " " + i + "-" + (i + 4);
            LuaString ls = LuaString.valueOf(before);
            String after = ls.tojstring();
            Assert.assertEquals(userFriendly(before), userFriendly(after));
        }
        char[] c = { (char)(1), (char)(2), (char)(3) };
        String before = new String(c) + " 1-3";
        LuaString ls = LuaString.valueOf(before);
        String after = ls.tojstring();
        Assert.assertEquals(userFriendly(before), userFriendly(after));
    }

    @Test
    public void testSpotCheckUtf8() throws UnsupportedEncodingException {
        byte[] bytes = { (byte)194, (byte)160, (byte)194, (byte)161, (byte)194, (byte)162, (byte)194,
                (byte)163, (byte)194, (byte)164 };
        String expected = new String(bytes, "UTF8");
        String actual = LuaString.valueOf(bytes).tojstring();
        char[] d = actual.toCharArray();
        Assert.assertEquals(160, d[0]);
        Assert.assertEquals(161, d[1]);
        Assert.assertEquals(162, d[2]);
        Assert.assertEquals(163, d[3]);
        Assert.assertEquals(164, d[4]);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testNullTerminated() {
        char[] c = { 'a', 'b', 'c', '\0', 'd', 'e', 'f' };
        String before = new String(c);
        LuaString ls = LuaString.valueOf(before);
        String after = ls.tojstring();
        Assert.assertEquals(userFriendly("abc\0def"), userFriendly(after));
    }

    @Test
    public void testIndexOfByteInSubstring() {
        LuaString str = LuaString.valueOf("abcdef:ghi");
        LuaString sub = str.substring(2, 10);

        Assert.assertEquals(6, str.indexOf((byte)':', 0));
        Assert.assertEquals(6, str.indexOf((byte)':', 2));
        Assert.assertEquals(6, str.indexOf((byte)':', 6));
        Assert.assertEquals(-1, str.indexOf((byte)':', 7));
        Assert.assertEquals(-1, str.indexOf((byte)':', 9));
        Assert.assertEquals(9, str.indexOf((byte)'i', 0));
        Assert.assertEquals(9, str.indexOf((byte)'i', 2));
        Assert.assertEquals(9, str.indexOf((byte)'i', 9));
        Assert.assertEquals(-1, str.indexOf((byte)'z', 0));
        Assert.assertEquals(-1, str.indexOf((byte)'z', 2));
        Assert.assertEquals(-1, str.indexOf((byte)'z', 9));

        Assert.assertEquals(4, sub.indexOf((byte)':', 0));
        Assert.assertEquals(4, sub.indexOf((byte)':', 2));
        Assert.assertEquals(4, sub.indexOf((byte)':', 4));
        Assert.assertEquals(-1, sub.indexOf((byte)':', 5));
        Assert.assertEquals(-1, sub.indexOf((byte)':', 7));
        Assert.assertEquals(7, sub.indexOf((byte)'i', 0));
        Assert.assertEquals(7, sub.indexOf((byte)'i', 2));
        Assert.assertEquals(7, sub.indexOf((byte)'i', 7));
        Assert.assertEquals(-1, sub.indexOf((byte)'z', 0));
        Assert.assertEquals(-1, sub.indexOf((byte)'z', 2));
        Assert.assertEquals(-1, sub.indexOf((byte)'z', 7));
    }

    @Test
    public void testIndexOfPatternInSubstring() {
        LuaString str = LuaString.valueOf("abcdef:ghi");
        LuaString sub = str.substring(2, 10);

        LuaString pat = LuaString.valueOf(":");
        LuaString i = LuaString.valueOf("i");
        LuaString xyz = LuaString.valueOf("xyz");

        Assert.assertEquals(6, str.indexOf(pat, 0));
        Assert.assertEquals(6, str.indexOf(pat, 2));
        Assert.assertEquals(6, str.indexOf(pat, 6));
        Assert.assertEquals(-1, str.indexOf(pat, 7));
        Assert.assertEquals(-1, str.indexOf(pat, 9));
        Assert.assertEquals(9, str.indexOf(i, 0));
        Assert.assertEquals(9, str.indexOf(i, 2));
        Assert.assertEquals(9, str.indexOf(i, 9));
        Assert.assertEquals(-1, str.indexOf(xyz, 0));
        Assert.assertEquals(-1, str.indexOf(xyz, 2));
        Assert.assertEquals(-1, str.indexOf(xyz, 9));

        Assert.assertEquals(4, sub.indexOf(pat, 0));
        Assert.assertEquals(4, sub.indexOf(pat, 2));
        Assert.assertEquals(4, sub.indexOf(pat, 4));
        Assert.assertEquals(-1, sub.indexOf(pat, 5));
        Assert.assertEquals(-1, sub.indexOf(pat, 7));
        Assert.assertEquals(7, sub.indexOf(i, 0));
        Assert.assertEquals(7, sub.indexOf(i, 2));
        Assert.assertEquals(7, sub.indexOf(i, 7));
        Assert.assertEquals(-1, sub.indexOf(xyz, 0));
        Assert.assertEquals(-1, sub.indexOf(xyz, 2));
        Assert.assertEquals(-1, sub.indexOf(xyz, 7));
    }

    @Test
    public void testLastIndexOfPatternInSubstring() {
        LuaString str = LuaString.valueOf("abcdef:ghi");
        LuaString sub = str.substring(2, 10);

        LuaString pat = LuaString.valueOf(":");
        LuaString i = LuaString.valueOf("i");
        LuaString xyz = LuaString.valueOf("xyz");

        Assert.assertEquals(6, str.lastIndexOf(pat));
        Assert.assertEquals(9, str.lastIndexOf(i));
        Assert.assertEquals(-1, str.lastIndexOf(xyz));

        Assert.assertEquals(4, sub.lastIndexOf(pat));
        Assert.assertEquals(7, sub.lastIndexOf(i));
        Assert.assertEquals(-1, sub.lastIndexOf(xyz));
    }

    @Test
    public void testIndexOfAnyInSubstring() {
        LuaString str = LuaString.valueOf("abcdef:ghi");
        LuaString sub = str.substring(2, 10);

        LuaString ghi = LuaString.valueOf("ghi");
        LuaString ihg = LuaString.valueOf("ihg");
        LuaString ijk = LuaString.valueOf("ijk");
        LuaString kji = LuaString.valueOf("kji");
        LuaString xyz = LuaString.valueOf("xyz");
        final LuaString ABCdEFGHIJKL = LuaString.valueOf("ABCdEFGHIJKL");
        final LuaString EFGHIJKL = ABCdEFGHIJKL.substring(4, 12);
        final LuaString CdEFGHIJ = ABCdEFGHIJKL.substring(2, 10);

        Assert.assertEquals(7, str.indexOfAny(ghi));
        Assert.assertEquals(7, str.indexOfAny(ihg));
        Assert.assertEquals(9, str.indexOfAny(ijk));
        Assert.assertEquals(9, str.indexOfAny(kji));
        Assert.assertEquals(-1, str.indexOfAny(xyz));
        Assert.assertEquals(3, str.indexOfAny(CdEFGHIJ));
        Assert.assertEquals(-1, str.indexOfAny(EFGHIJKL));

        Assert.assertEquals(5, sub.indexOfAny(ghi));
        Assert.assertEquals(5, sub.indexOfAny(ihg));
        Assert.assertEquals(7, sub.indexOfAny(ijk));
        Assert.assertEquals(7, sub.indexOfAny(kji));
        Assert.assertEquals(-1, sub.indexOfAny(xyz));
        Assert.assertEquals(1, sub.indexOfAny(CdEFGHIJ));
        Assert.assertEquals(-1, sub.indexOfAny(EFGHIJKL));
    }

}
