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
package nl.weeaboo.lua2.lib;

import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.luajava.LuajavaLib;
import nl.weeaboo.lua2.vm.LuaTable;

public final class J2sePlatform {

	/**
     * Registers Lua standard libraries.
     */
    public static LuaTable registerStandardLibs(LuaRunState lrs) {
        PackageLib packageLib = new PackageLib();
        lrs.setPackageLib(packageLib);

        LuaTable _G = new LuaTable();
		_G.load(new BaseLib());
        _G.load(packageLib);
		_G.load(new TableLib());
		_G.load(new StringLib());
		_G.load(new CoroutineLib());
		_G.load(MathLib.getInstance());
		_G.load(new SerializableIoLib());
		_G.load(new OsLib());
		_G.load(new LuajavaLib());
		_G.load(new ThreadLib());
		_G.rawset("yield", _G.rawget("Thread").rawget("yield")); //Make a global yield() function
        _G.load(new DebugLib());
		return _G;
	}

}
