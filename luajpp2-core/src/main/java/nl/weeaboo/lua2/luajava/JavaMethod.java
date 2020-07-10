package nl.weeaboo.lua2.luajava;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Varargs;

final class JavaMethod {

    private static final Object[] EMPTY_ARGS = {};

    private final Method method;
    private final List<Class<?>> paramTypes;
    private final int paramCount;

    public JavaMethod(Method m) {
        method = m;
        paramTypes = Arrays.asList(m.getParameterTypes());
        paramCount = paramTypes.size();
    }

    public LuaValue luaInvoke(Object instance, Varargs args)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {

        Object[] javaArgs = EMPTY_ARGS;
        if (paramCount > 0) {
            javaArgs = new Object[paramCount];
            CoerceLuaToJava.coerceArgs(javaArgs, args, paramTypes);
        }

        Object javaResult = method.invoke(instance, javaArgs);

        return CoerceJavaToLua.coerce(javaResult, method.getReturnType());
    }

    public List<Class<?>> getParamTypes() {
        return paramTypes;
    }

    public Class<?> getReturnType() {
        return method.getReturnType();
    }

}