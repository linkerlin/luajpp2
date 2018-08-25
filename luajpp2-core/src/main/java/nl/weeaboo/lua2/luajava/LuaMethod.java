package nl.weeaboo.lua2.luajava;

import java.io.ObjectStreamException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.io.IReadResolveSerializable;
import nl.weeaboo.lua2.io.IWriteReplaceSerializable;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.lib.VarArgFunction;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Varargs;

@LuaSerializable
final class LuaMethod extends VarArgFunction implements IWriteReplaceSerializable {

    private final JavaClass classInfo;
    private final LuaValue methodName;

    private transient JavaMethod[] javaMethods;

    public LuaMethod(JavaClass c, LuaValue nm) {
        classInfo = c;
        methodName = nm;
    }

    @Override
    public Object writeReplace() throws ObjectStreamException {
        return new LuaMethodRef(classInfo, methodName);
    }

    @Override
    public String toString() {
        return classInfo.getWrappedClass().getName() + "." + methodName + "()";
    }

    @Override
    public LuaValue call() {
        throw error("Method cannot be called without instance");
    }

    @Override
    public Varargs invoke(Varargs args) {
        return invokeMethod(args.checkuserdata(1), args.subargs(2));
    }

    private LuaValue invokeMethod(Object instance, Varargs args) {
        try {
            JavaMethod method = findMethod(args);
            if (method == null) {
                throw new NoSuchMethodException(String.format(
                        "Method %s with the specified parameter types doesn't exist",
                        methodName));
            }

            return invokeMethod(method, instance, args);
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            String msg = "Error in invoked Java method: " + methodName + "(" + args + ")";
            throw LuaException.wrap(msg, cause);
        } catch (Exception e) {
            throw LuaException.wrap("Error invoking Java method: " + methodName + "(" + args + ")", e);
        }
    }

    private LuaValue invokeMethod(JavaMethod method, Object instance, Varargs args)
        throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {

        List<Class<?>> paramTypes = method.getParamTypes();
        Object[] javaArgs = new Object[paramTypes.size()];
        CoerceLuaToJava.coerceArgs(javaArgs, args, paramTypes);

        Object javaResult = method.invoke(instance, javaArgs);

        // Only allow access to declared type (prevents accidental use of nondeclared interfaces)
        return CoerceJavaToLua.coerce(javaResult, method.getReturnType());
    }

    protected JavaMethod findMethod(Varargs args) {
        JavaMethod[] methods = getMatchingJavaMethods();
        if (methods.length == 1) {
            return methods[0];
        }

        JavaMethod bestMatch = null;
        int bestScore = Integer.MAX_VALUE;
        for (JavaMethod curMethod : methods) {
            List<Class<?>> params = curMethod.getParamTypes();
            int score = CoerceLuaToJava.scoreParamTypes(args, params);
            if (score <= bestScore) {
                if (bestMatch != null && score == bestScore) {
                    // Parameter score is equal, select the method with the more specific return type
                    if (bestMatch.getReturnType().isAssignableFrom(curMethod.getReturnType())) {
                        bestMatch = curMethod;
                    }
                } else {
                    bestMatch = curMethod;
                }
                bestScore = score;
            }
        }

        return bestMatch;
    }

    private JavaMethod[] getMatchingJavaMethods() {
        if (javaMethods == null) {
            javaMethods = classInfo.getMethods(methodName);
        }
        return javaMethods;
    }

    @LuaSerializable
    private static class LuaMethodRef implements IReadResolveSerializable {

        private static final long serialVersionUID = 1L;

        private final JavaClass classInfo;
        private final LuaValue name;

        public LuaMethodRef(JavaClass classInfo, LuaValue name) {
            this.classInfo = classInfo;
            this.name = name;
        }

        @Override
        public Object readResolve() throws ObjectStreamException {
            return classInfo.getMetatable().getMethod(name);
        }
    }

}