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

package nl.weeaboo.lua2.luajava;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.lib.LuaBoundFunction;
import nl.weeaboo.lua2.stdlib.LuaModule;
import nl.weeaboo.lua2.vm.LuaFunction;
import nl.weeaboo.lua2.vm.LuaUserdata;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Varargs;

/**
 * Special library for accessing Java functionality from Lua.
 */
@LuaSerializable
public final class LuajavaLib extends LuaModule {

    private static final long serialVersionUID = 2L;

    private static final Map<Class<?>, JavaClass> classInfoMap = new HashMap<>();

    private boolean allowUnsafeClassLoading;

    public LuajavaLib() {
        super("luajava");
    }

    /**
     * Enables or disables class loading.
     */
    public void setAllowUnsafeClassLoading(boolean allow) {
        allowUnsafeClassLoading = allow;
    }

    /**
     * Loads a Java class.
     *
     * @param args
     *        <ol>
     *        <li>Fully qualified name of the Java class to load.
     *        </ol>
     * @throws LuaException If the class isn't allowed to be loaded.
     * @throws ClassNotFoundException If the class isn't found.
     */
    @LuaBoundFunction
    public Varargs bindClass(Varargs args) throws ClassNotFoundException {
        if (!allowUnsafeClassLoading) {
            throw new LuaException("Class loading is not allowed");
        }

        Class<?> clazz = Class.forName(args.checkjstring(1));
        return toUserdata(clazz, Class.class);
    }

    /**
     * Instantiates a new Java object. Any additionaly arguments are passed to the class constructor.
     *
     * @param args
     *        <ol>
     *        <li>class. This can be either a class object (see {@link #bindClass(Varargs)}), or a string with
     *        the full Java class name.
     *        <li>args...
     *        </ol>
     * @return A userdata object.
     * @throws Exception If class loading fails, or the constructor raised an exception.
     */
    @LuaBoundFunction
    public Varargs newInstance(Varargs args) throws Exception {
        final LuaValue c = args.checkvalue(1);
        final Class<?> clazz;
        if (c.isuserdata(Class.class)) {
            clazz = c.checkuserdata(Class.class);
        } else {
            if (!allowUnsafeClassLoading) {
                throw new LuaException("Class loading is not allowed");
            }
            clazz = Class.forName(c.tojstring());
        }

        JavaClass info = getClassInfo(clazz);
        return info.newInstance(args.subargs(2));
    }

    /**
     * Returns a Lua userdata object wrapping the given object, giving Lua access to the public methods
     * defined in {@code clazz}.
     */
    public static LuaUserdata toUserdata(Object object, Class<?> clazz) {
        JavaClass info = getClassInfo(clazz);
        return LuaUserdata.userdataOf(object, info.getMetatable());
    }

    static JavaClass getClassInfo(Class<?> clazz) {
        JavaClass info = classInfoMap.get(clazz);
        if (info == null) {
            info = new JavaClass(clazz);
            classInfoMap.put(clazz, info);
        }
        return info;
    }

    /**
     * Returns a Lua function that calls a constructor of the given Java class.
     */
    public static LuaFunction getConstructor(Class<?> javaClass) {
        return new ConstrFunction(javaClass);
    }

    @LuaSerializable
    private static final class ConstrFunction extends LuaFunction {

        private static final long serialVersionUID = 6459092255782515933L;

        private final JavaClass ci;

        public ConstrFunction(Class<?> c) {
            ci = getClassInfo(c);
        }

        @Override
        public Varargs invoke(Varargs args) {
            try {
                return ci.newInstance(args);
            } catch (InvocationTargetException ite) {
                throw LuaException.wrap("Error invoking constructor: " + ci.getWrappedClass(), ite.getCause());
            } catch (Exception e) {
                throw LuaException.wrap("Error invoking constructor: " + ci.getWrappedClass(), e);
            }
        }
    }

}