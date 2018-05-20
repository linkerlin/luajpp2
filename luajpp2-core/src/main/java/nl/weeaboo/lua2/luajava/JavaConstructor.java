package nl.weeaboo.lua2.luajava;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;

final class JavaConstructor {

    private final Constructor<?> constr;
    private List<Class<?>> params;

    public JavaConstructor(Constructor<?> c) {
        constr = c;
    }

    public Constructor<?> getConstructor() {
        return constr;
    }

    public List<Class<?>> getParamTypes() {
        if (params == null) {
            params = Arrays.asList(constr.getParameterTypes());
        }
        return params;
    }

}