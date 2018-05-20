package nl.weeaboo.lua2.luajava;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

final class JavaMethod {

    private final Method method;
    private List<Class<?>> paramTypes;

    public JavaMethod(Method m) {
        method = m;
    }

    public Method getMethod() {
        return method;
    }

    public List<Class<?>> getParamTypes() {
        if (paramTypes == null) {
            paramTypes = Arrays.asList(method.getParameterTypes());
        }
        return paramTypes;
    }

}