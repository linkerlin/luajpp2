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

import static nl.weeaboo.lua2.vm.LuaConstants.NONE;

import nl.weeaboo.lua2.compiler.ILuaCompiler;
import nl.weeaboo.lua2.compiler.LuaC;
import nl.weeaboo.lua2.io.LuaSerializable;

/**
 * Extension of {@link LuaFunction} which executes lua bytecode.
 * <p>
 * A {@link LuaClosure} is a combination of a {@link Prototype} and a {@link LuaValue} to use as an
 * environment for execution.
 * <p>
 * There are three main ways {@link LuaClosure} instances are created:
 * <ul>
 * <li>Construct an instance using {@link #LuaClosure(Prototype, LuaValue)}</li>
 * <li>Construct it indirectly by loading a chunk via
 * {@link ILuaCompiler#load(java.io.InputStream, String, LuaValue)}
 * <li>Execute the lua bytecode {@link Lua#OP_CLOSURE} as part of bytecode processing
 * </ul>
 * <p>
 * To construct it directly, the {@link Prototype} is typically created via a compiler such as {@link LuaC}:
 *
 * <pre>
 * {@code
 * InputStream is = new ByteArrayInputStream("print('hello,world').getBytes());
 * Prototype p = LuaC.instance.compile(is, "script");
 * LuaValue _G = JsePlatform.standardGlobals()
 * LuaClosure f = new LuaClosure(p, _G);
 * }
 * </pre>
 * <p>
 * To construct it indirectly, the {@link LuaC} compiler may be used, which implements the {@link ILuaCompiler}
 * interface:
 *
 * <pre>
 * {
 *     &#064;code
 *     LuaFunction f = LuaC.instance.load(is, &quot;script&quot;, _G);
 * }
 * </pre>
 * <p>
 * Typically, a closure that has just been loaded needs to be initialized by executing it, and its return
 * value can be saved if needed:
 *
 * <pre>
 * {@code
 * LuaValue r = f.call();
 * _G.set( "mypkg", r )
 * }
 * </pre>
 * <p>
 * In the preceding, the loaded value is typed as {@link LuaFunction} to allow for the possibility of other
 * compilers such as LuaJC producing {@link LuaFunction} directly without creating a {@link Prototype} or
 * {@link LuaClosure}.
 * <p>
 * Since a {@link LuaClosure} is a {@link LuaFunction} which is a {@link LuaValue}, all the value operations
 * can be used directly such as:
 * <ul>
 * <li>{@link LuaValue#setfenv(LuaValue)}</li>
 * <li>{@link LuaValue#call()}</li>
 * <li>{@link LuaValue#call(LuaValue)}</li>
 * <li>{@link LuaValue#invoke()}</li>
 * <li>{@link LuaValue#invoke(Varargs)}</li>
 * <li>...</li>
 * </ul>
 */
@LuaSerializable
public final class LuaClosure extends LuaFunction {

    private static final long serialVersionUID = 1L;

    private final Prototype p;
    private final UpValue[] upValues;

    /** Supply the initial environment. */
    public LuaClosure(Prototype p, LuaValue env) {
        super(env);

        this.p = p;
        if (p.nups == 0) {
            upValues = UpValue.NOUPVALUES;
        } else {
            upValues = new UpValue[p.nups];
            for (int n = 0; n < p.nups; n++) {
                upValues[n] = UpValue.newSealedInstance();
            }
        }
    }

    @Override
    public boolean isclosure() {
        return true;
    }

    @Override
    public LuaClosure optclosure(LuaClosure defval) {
        return this;
    }

    @Override
    public LuaClosure checkclosure() {
        return this;
    }

    @Override
    public final LuaValue call() {
        return invoke(NONE).arg1();
    }

    @Override
    public final LuaValue call(LuaValue arg) {
        return invoke(arg).arg1();
    }

    @Override
    public final LuaValue call(LuaValue arg1, LuaValue arg2) {
        return invoke(varargsOf(arg1, arg2)).arg1();
    }

    @Override
    public final LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
        return invoke(varargsOf(arg1, arg2, arg3)).arg1();
    }

    @Override
    public final Varargs invoke(Varargs varargs) {
        LuaThread thread = LuaThread.getRunning();
        return thread.callFunctionInThread(this, varargs);
    }

    /** Returns the prototype for this closure. */
    public Prototype getPrototype() {
        return p;
    }

    /** Returns the upvalues for this closure. */
    public UpValue[] getUpValues() {
        return upValues;
    }

    /** Returns the upvalue with the given index (0-based) */
    public UpValue getUpValue(int index) {
        return upValues[index];
    }

    /** Returns the number of upvalues for this closure. */
    public int getUpValueCount() {
        return upValues.length;
    }

    @Override
    public String tojstring() {
        return typename() + ": " + p.source + ":" + p.linedefined + "-" + p.lastlinedefined;
    }

}
