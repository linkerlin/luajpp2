package nl.weeaboo.lua2.luajava;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

final class JavaConstructor {

    private final Constructor<?> constr;
    private List<Class<?>> params;

    public JavaConstructor(Constructor<?> c) {
        constr = c;
    }

    public Object newInstance(Object... args)
            throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
        return constr.newInstance(args);
    }

    public List<Class<?>> getParamTypes() {
        if (params == null) {
            params = Arrays.asList(constr.getParameterTypes());
        }
        return params;
    }

}