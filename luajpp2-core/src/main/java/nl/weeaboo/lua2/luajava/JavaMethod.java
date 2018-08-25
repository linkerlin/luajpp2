package nl.weeaboo.lua2.luajava;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

final class JavaMethod {

    private final Method method;
    private List<Class<?>> paramTypes;

    public JavaMethod(Method m) {
        method = m;
    }

    public Object invoke(Object instance, Object... args)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {

        return method.invoke(instance, args);
    }

    public List<Class<?>> getParamTypes() {
        if (paramTypes == null) {
            paramTypes = Arrays.asList(method.getParameterTypes());
        }
        return paramTypes;
    }

    public Class<?> getReturnType() {
        return method.getReturnType();
    }

}