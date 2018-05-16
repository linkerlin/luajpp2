package nl.weeaboo.lua2.lib;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.vm.LuaConstants;
import nl.weeaboo.lua2.vm.LuaNil;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.Varargs;

/** Base class for making libraries of Java functions available to Lua. */
public abstract class LuaLib implements ILuaLib {

    private static final long serialVersionUID = 1L;

    @Override
    public abstract void register() throws LuaException;

    protected final void registerFunctions(LuaTable globals, LuaTable libTable) throws LuaException {
        for (Method method : getClass().getMethods()) {
            LuaBoundFunction functionAnnot = method.getAnnotation(LuaBoundFunction.class);
            if (functionAnnot == null) {
                continue;
            }

            LuaTable targetTable = libTable;
            if (functionAnnot.global()) {
                targetTable = globals;
            }

            // Get name from annotation. If missing, default to using the Java function name.
            String luaMethodName = functionAnnot.luaName();
            if (luaMethodName.equals("")) {
                luaMethodName = method.getName();
            }

            // Check that we don't accidentally overwrite an existing table entry.
            if (targetTable.rawget(luaMethodName) != LuaNil.NIL) {
                throw new LuaException("There's already a table entry named: "
                        + luaMethodName + " :: " + targetTable.rawget(luaMethodName));
            }

            targetTable.rawset(luaMethodName, wrapFunction(method, luaMethodName));
        }
    }

    private VarArgFunction wrapFunction(Method method, String luaMethodName) throws LuaException {
        Class<?> returnType = method.getReturnType();
        if (!returnType.equals(Varargs.class) && !returnType.equals(Void.TYPE)) {
            throw new LuaException("Return type must be Varargs or void");
        }

        Class<?>[] parameterTypes = method.getParameterTypes();
        if (!Arrays.equals(parameterTypes, new Class<?>[] { Varargs.class })) {
            throw new LuaException("Method must have a single parameter of type Varargs");
        }

        FunctionWrapper functionWrapper = new FunctionWrapper(this, luaMethodName, method.getName(), parameterTypes);
        functionWrapper.setfenv(LuaRunState.getCurrent().getGlobalEnvironment());
        return functionWrapper;
    }

    @LuaSerializable
    private static class FunctionWrapper extends VarArgFunction {

        private static final long serialVersionUID = 1L;

        private final LuaLib object;
        private final String javaMethodName;
        private final Class<?>[] parameterTypes;

        private transient Method method;

        public FunctionWrapper(LuaLib object, String luaMethodName, String javaMethodName, Class<?>[] parameterTypes) {
            name = luaMethodName;

            this.object = object;
            this.javaMethodName = javaMethodName;
            this.parameterTypes = parameterTypes.clone();
        }

        @Override
        public Varargs invoke(Varargs args) {
            try {
                if (method == null) {
                    method = object.getClass().getMethod(javaMethodName, parameterTypes);
                }
                Object result = method.invoke(object, args);
                if (result instanceof Varargs) {
                    return (Varargs)result;
                } else if (result == null && method.getReturnType() == Void.TYPE) {
                    return LuaConstants.NONE;
                } else {
                    // This may happen if the methods return type changed (can happen due to serialization)
                    throw new LuaException("Java method (" + method + ") returned non-varargs: "
                            + (result != null ? result.getClass().getName() : "null"));
                }
            } catch (InvocationTargetException ite) {
                throw LuaException.wrap(createErrorMessage(args, ite.getCause()), ite.getCause());
            } catch (Exception e) {
                throw LuaException.wrap(createErrorMessage(args, e), e);
            }
        }

        private String createErrorMessage(Varargs args, Throwable cause) {
            String error = "Error invoking Java method: " + javaMethodName;
            if (args.narg() > 1) {
                error += args;
            } else {
                error += "(" + args + ")";
            }
            if (cause != null) {
                error += "\n\t " + cause;
            }
            return error;
        }
    }

}
