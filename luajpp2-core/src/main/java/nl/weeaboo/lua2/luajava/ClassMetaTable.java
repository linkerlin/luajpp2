package nl.weeaboo.lua2.luajava;

import static nl.weeaboo.lua2.vm.LuaConstants.META_INDEX;
import static nl.weeaboo.lua2.vm.LuaConstants.META_LEN;
import static nl.weeaboo.lua2.vm.LuaConstants.META_NEWINDEX;
import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import java.io.ObjectStreamException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.io.IReadResolveSerializable;
import nl.weeaboo.lua2.io.IWriteReplaceSerializable;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.lib.OneArgFunction;
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
    private ClassInfo classInfo;
    private boolean seal;
    private transient Map<LuaValue, LuaMethod> methods;
    //--- Uses manual serialization, don't add variables ---

    ClassMetaTable(ClassInfo ci) {
        classInfo = ci;

        rawset(META_INDEX, newMetaFunction(classInfo, this, true));
        rawset(META_NEWINDEX, newMetaFunction(classInfo, this, false));
        if (ci.isArray()) {
            rawset(META_LEN, ARRAY_LENGTH_FUNCTION);
        }

        seal = true;

        methods = new HashMap<LuaValue, LuaMethod>();
    }

    @Override
    public Object writeReplace() throws ObjectStreamException {
        return new ClassMetaTableRef(classInfo);
    }

    private static MetaFunction newMetaFunction(ClassInfo ci, ClassMetaTable mt, boolean isGet) {
        if (ci.isArray()) {
            return new ArrayMetaFunction(ci, mt, isGet);
        }
        return new MetaFunction(ci, mt, isGet);
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
        if (seal) {
            throw new LuaException("Can't write to a shared Java class metatable");
        }
    }

    @Override
    public String tojstring() {
        return "ClassMetaTable(" + classInfo.getWrappedClass().getSimpleName() + ")@" + hashCode();
    }

    LuaMethod getMethod(LuaValue name) {
        LuaMethod method = methods.get(name);
        if (method != null) {
            return method;
        } else {
            MethodInfo[] ms = classInfo.getMethods(name);
            if (ms != null && ms.length > 0) {
                method = new LuaMethod(classInfo, name, ms);
                methods.put(name, method);
                return method;
            }
        }
        return null;
    }

    //Inner Classes
    @LuaSerializable
    private static class ClassMetaTableRef implements IReadResolveSerializable {

        private static final long serialVersionUID = 1L;

        private final ClassInfo classInfo;

        public ClassMetaTableRef(ClassInfo classInfo) {
            this.classInfo = classInfo;
        }

        @Override
        public Object readResolve() throws ObjectStreamException {
            return classInfo.getMetatable();
        }
    }

    @LuaSerializable
    private static class MetaFunction extends LuaFunction {

        private static final long serialVersionUID = 1L;

        protected final ClassInfo classInfo;
        protected final ClassMetaTable meta;
        protected final boolean isGet;

        public MetaFunction(ClassInfo ci, ClassMetaTable mt, boolean get) {
            classInfo = ci;
            meta = mt;
            isGet = get;
        }

        @Override
        public Varargs invoke(Varargs args) {
            return invokeMethod(args.arg1().checkuserdata(), args.arg(2), args.arg(3));
        }

        protected LuaValue invokeMethod(Object instance, LuaValue key, LuaValue val) {
            if (instance == null) {
                return LuaNil.NIL.call();
            }

            //Fields & Methods
            if (isGet) {
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
                        Object o = field.get(instance);
                        return CoerceJavaToLua.coerce(o, field.getType());
                    } catch (Exception e) {
                        throw LuaException.wrap("Error coercing field: " + key, e);
                    }
                }

                return NIL; //Invalid get returns nil
            } else {
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

    }

    @LuaSerializable
    private static class ArrayMetaFunction extends MetaFunction {

        private static final long serialVersionUID = 1L;

        public ArrayMetaFunction(ClassInfo ci, ClassMetaTable mt, boolean get) {
            super(ci, mt, get);
        }

        @Override
        protected LuaValue invokeMethod(Object instance, LuaValue key, LuaValue val) {
            int arrayLength = Array.getLength(instance);
            if (key.isinttype()) {
                int index = key.checkint() - 1;
                if (index < 0 || index >= arrayLength) {
                    throw new LuaException("Array index out of bounds: index=" + index
                            + ", length=" + arrayLength);
                }

                Class<?> clazz = classInfo.getWrappedClass();
                if (isGet) {
                    /*
                     * Only allow access to the declared component type. This prevents Lua from accessing
                     * non-public implementation details.
                     */
                    Object javaValue = Array.get(instance, index);
                    return CoerceJavaToLua.coerce(javaValue, clazz.getComponentType());
                } else {
                    Object v = CoerceLuaToJava.coerceArg(val, clazz.getComponentType());
                    Array.set(instance, key.checkint() - 1, v);
                    return NIL;
                }
            } else if (key.equals(LENGTH)) {
                if (isGet) {
                    return valueOf(arrayLength);
                }
            }

            return super.invokeMethod(instance, key, val);
        }

    }

    @LuaSerializable
    private static class ArrayLengthFunction extends OneArgFunction {

        private static final long serialVersionUID = 1L;

        @Override
        public LuaValue call(LuaValue arg) {
            Object instance = arg.checkuserdata();
            return valueOf(Array.getLength(instance));
        }

    }

}