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
    private final IJavaToLua returnTypeCoercion;

    public JavaMethod(Method m) {
        method = m;
        paramTypes = Arrays.asList(m.getParameterTypes());
        paramCount = paramTypes.size();
        returnTypeCoercion = initReturnTypeCoercion(m);
    }

    private static IJavaToLua initReturnTypeCoercion(final Method method) {
        final Class<?> returnType = method.getReturnType();

        TypeCoercions typeCoercions = TypeCoercions.getInstance();
        IJavaToLua result = typeCoercions.findJavaToLua(returnType);

        if (result == null) {
            // Slow path for types for which no dedicated conversion exists
            result = new IJavaToLua() {
                @Override
                public LuaValue toLua(Object javaValue) {
                    return CoerceJavaToLua.coerce(javaValue, returnType);
                }
            };
        }
        return result;
    }

    public LuaValue luaInvoke(Object instance, Varargs args)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {

        Object[] javaArgs = EMPTY_ARGS;
        if (paramCount > 0) {
            javaArgs = new Object[paramCount];
            CoerceLuaToJava.coerceArgs(javaArgs, args, paramTypes);
        }

        Object javaResult = method.invoke(instance, javaArgs);

        return returnTypeCoercion.toLua(javaResult);
    }

    public List<Class<?>> getParamTypes() {
        return paramTypes;
    }

    public Class<?> getReturnType() {
        return method.getReturnType();
    }

}