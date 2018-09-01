package nl.weeaboo.lua2.luajava;

import static nl.weeaboo.lua2.vm.LuaConstants.META_INDEX;
import static nl.weeaboo.lua2.vm.LuaConstants.META_LEN;
import static nl.weeaboo.lua2.vm.LuaConstants.META_NEWINDEX;
import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import java.io.ObjectStreamException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;

import javax.annotation.Nullable;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.io.IReadResolveSerializable;
import nl.weeaboo.lua2.io.IWriteReplaceSerializable;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.lib.OneArgFunction;
import nl.weeaboo.lua2.lib.VarArgFunction;
import nl.weeaboo.lua2.vm.LuaFunction;
import nl.weeaboo.lua2.vm.LuaNil;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Varargs;

@LuaSerializable
final class ClassMetaTable extends LuaTable implements IWriteReplaceSerializable {

    private static final LuaString LENGTH = valueOf("length");
    private static final LuaValue ARRAY_LENGTH_FUNCTION = new ArrayLengthFunction();

    //--- Uses manual serialization, don't add variables ---
    private JavaClass classInfo;
    private transient ArrayList<LuaMethod> cachedMethods;
    //--- Uses manual serialization, don't add variables ---

    ClassMetaTable(JavaClass ci) {
        classInfo = ci;
        cachedMethods = new ArrayList<>();

        super.hashset(META_INDEX, newMetaFunction(classInfo, this, true));
        super.hashset(META_NEWINDEX, newMetaFunction(classInfo, this, false));
        if (ci.isArray()) {
            super.hashset(META_LEN, ARRAY_LENGTH_FUNCTION);
        }

    }

    @Override
    public Object writeReplace() throws ObjectStreamException {
        return new ClassMetaTableRef(classInfo);
    }

    private static LuaFunction newMetaFunction(JavaClass ci, ClassMetaTable mt, boolean isGet) {
        if (ci.isArray()) {
            if (isGet) {
                return new ArrayMetaGetFunction(ci, mt);
            } else {
                return new ArrayMetaSetFunction(ci);
            }
        } else {
            if (isGet) {
                return new MetaGetFunction(ci, mt);
            } else {
                return new MetaSetFunction(ci);
            }
        }
    }

    @Override
    public void hashset(LuaValue key, LuaValue value) {
        checkSeal();
        super.hashset(key, value);
    }

    @Override
    public void rawset(int key, LuaValue value) {
        checkSeal();
        super.rawset(key, value);
    }

    @Override
    public void rawset(LuaValue key, LuaValue value) {
        checkSeal();
        super.rawset(key, value);
    }

    @Override
    public void sort(LuaValue comparator) {
        checkSeal();
        super.sort(comparator);
    }

    protected void checkSeal() {
        throw new LuaException("Can't write to a shared Java class metatable");
    }

    @Override
    public String tojstring() {
        return "ClassMetaTable(" + classInfo.getWrappedClass().getSimpleName() + ")@" + hashCode();
    }

    @Nullable LuaMethod getMethod(LuaValue name) {
        final int cachedMethodsL = cachedMethods.size();
        for (int n = 0; n < cachedMethodsL; n++) {
            LuaMethod method = cachedMethods.get(n);
            if (name.equals(method.methodName)) {
                return method;
            }
        }

        if (classInfo.hasMethod(name)) {
            LuaMethod method = new LuaMethod(classInfo, name);
            cachedMethods.add(method);
            return method;
        }

        return null;
    }

    @LuaSerializable
    private static final class ClassMetaTableRef implements IReadResolveSerializable {

        private static final long serialVersionUID = 1L;

        private final JavaClass classInfo;

        public ClassMetaTableRef(JavaClass classInfo) {
            this.classInfo = classInfo;
        }

        @Override
        public Object readResolve() throws ObjectStreamException {
            return classInfo.getMetatable();
        }
    }

    @LuaSerializable
    private static class MetaGetFunction extends VarArgFunction {

        private static final long serialVersionUID = 1L;

        protected final JavaClass classInfo;
        protected final ClassMetaTable meta;

        public MetaGetFunction(JavaClass ci, ClassMetaTable mt) {
            classInfo = ci;
            meta = mt;
        }

        @Override
        public Varargs invoke(Varargs args) {
            return invokeMethod(args.arg1().checkuserdata(), args.arg(2));
        }

        protected LuaValue invokeMethod(Object instance, LuaValue key) {
            if (instance == null) {
                return LuaNil.NIL.call();
            }

            LuaMethod method = meta.getMethod(key);
            if (method != null) {
                return method;
            }

            Field field = classInfo.getField(key);
            if (field != null) {
                try {
                    /*
                     * Only allow access to the declared type. This prevents Lua from accessing non-public
                     * implementation details.
                     */
                    Object javaValue = field.get(instance);
                    return CoerceJavaToLua.coerce(javaValue, field.getType());
                } catch (Exception e) {
                    throw LuaException.wrap("Error coercing field: " + key, e);
                }
            }

            return NIL; // Invalid get returns nil
        }
    }

    @LuaSerializable
    private static class MetaSetFunction extends VarArgFunction {

        private static final long serialVersionUID = 1L;

        protected final JavaClass classInfo;

        public MetaSetFunction(JavaClass ci) {
            classInfo = ci;
        }

        @Override
        public Varargs invoke(Varargs args) {
            return invokeMethod(args.arg1().checkuserdata(), args.arg(2), args.arg(3));
        }

        protected LuaValue invokeMethod(Object instance, LuaValue key, LuaValue val) {
            Field field = classInfo.getField(key);
            if (field != null) {
                Object v = CoerceLuaToJava.coerceArg(val, field.getType());
                try {
                    field.set(instance, v);
                } catch (Exception e) {
                    throw LuaException.wrap("Error setting field: " + classInfo.getWrappedClass() + "." + key, e);
                }
                return NIL;
            } else {
                throw new LuaException("Invalid assignment, field does not exist in Java class: " + key);
            }
        }

    }

    @LuaSerializable
    private static final class ArrayMetaGetFunction extends MetaGetFunction {

        private static final long serialVersionUID = 1L;

        public ArrayMetaGetFunction(JavaClass ci, ClassMetaTable mt) {
            super(ci, mt);
        }

        @Override
        protected LuaValue invokeMethod(Object instance, LuaValue key) {
            int arrayLength = Array.getLength(instance);
            if (key.isinttype()) {
                int index = key.checkint() - 1;
                if (index < 0 || index >= arrayLength) {
                    throw new LuaException("Array index out of bounds: index=" + index
                            + ", length=" + arrayLength);
                }

                Object javaValue = Array.get(instance, index);

                /*
                 * Only allow access to the declared component type. This prevents Lua from accessing
                 * non-public implementation details.
                 */
                Class<?> wrappedClass = classInfo.getWrappedClass();
                return CoerceJavaToLua.coerce(javaValue, wrappedClass.getComponentType());
            } else if (key.equals(LENGTH)) {
                return valueOf(arrayLength);
            }

            return super.invokeMethod(instance, key);
        }
    }

    @LuaSerializable
    private static final class ArrayMetaSetFunction extends MetaSetFunction {

        private static final long serialVersionUID = 1L;

        public ArrayMetaSetFunction(JavaClass ci) {
            super(ci);
        }

        @Override
        protected LuaValue invokeMethod(Object instance, LuaValue key, LuaValue val) {
            if (key.isinttype()) {
                Class<?> wrappedClass = classInfo.getWrappedClass();
                Object v = CoerceLuaToJava.coerceArg(val, wrappedClass.getComponentType());
                Array.set(instance, key.checkint() - 1, v);
                return NIL;
            }

            return super.invokeMethod(instance, key, val);
        }
    }

    @LuaSerializable
    private static final class ArrayLengthFunction extends OneArgFunction {

        private static final long serialVersionUID = 1L;

        @Override
        public LuaValue call(LuaValue arg) {
            Object instance = arg.checkuserdata();
            return valueOf(Array.getLength(instance));
        }

    }

}