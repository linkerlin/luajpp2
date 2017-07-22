/*******************************************************************************
 * Copyright (c) 2014 Luaj.org. All rights reserved.
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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.LuaUtil;
import nl.weeaboo.lua2.link.LuaLink;

public class UTF8StreamTest {

    private LuaRunState lrs;

    @Before
    public void before() throws LuaException {
        lrs = LuaRunState.newInstance();
    }

    @Test
    public void testUtf8CharsInStream() throws LuaException {
        String value = "98\u00b0: today's temp!"; // \u00b0 is the degree sign
        String script = "x = \"" + value + "\"\n"
                + "print('x = ', x)\n"
                + "return x";

        LuaLink link = new LuaLink(lrs);
        String str = LuaUtil.eval(link, script).tojstring(1);
        Assert.assertEquals(value, str);
    }

}
