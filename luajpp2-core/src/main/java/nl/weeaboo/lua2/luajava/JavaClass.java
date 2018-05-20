package nl.weeaboo.lua2.luajava;

import static nl.weeaboo.lua2.vm.LuaValue.valueOf;

import java.io.ObjectStreamException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.io.IReadResolveSerializable;
import nl.weeaboo.lua2.io.IWriteReplaceSerializable;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Varargs;

@LuaSerializable
final class JavaClass implements IWriteReplaceSerializable {

    private static final Comparator<Method> methodSorter = new MethodSorter();

    private final Class<?> clazz;
    private final boolean isArray;

    private transient ClassMetaTable metaTable;

    private transient JavaConstructor[] constrs;
    private transient Map<LuaString, Field> fields;
    private transient Map<LuaString, JavaMethod[]> methods;

    public JavaClass(Class<?> c) {
        clazz = c;
        isArray = c.isArray();
    }

    @Override
    public Object writeReplace() throws ObjectStreamException {
        return new JavaClassRef(clazz);
    }

    public Object newInstance(Varargs luaArgs) throws IllegalArgumentException, InstantiationException,
            IllegalAccessException, InvocationTargetException {

        JavaConstructor constr = findConstructor(luaArgs);
        if (constr == null) {
            throw new LuaException("No suitable constructor found for: " + clazz.getName());
        }

        List<Class<?>> paramTypes = constr.getParamTypes();
        Object[] javaArgs = new Object[paramTypes.size()];
        CoerceLuaToJava.coerceArgs(javaArgs, luaArgs, paramTypes);
        return constr.getConstructor().newInstance(javaArgs);
    }

    @Override
    public int hashCode() {
        return clazz.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof JavaClass) {
            JavaClass ci = (JavaClass)obj;
            return clazz.equals(ci.clazz);
        }
        return false;
    }

    public Class<?> getWrappedClass() {
        return clazz;
    }

    public boolean isArray() {
        return isArray;
    }

    protected JavaConstructor findConstructor(Varargs luaArgs) {
        JavaConstructor bestMatch = null;
        int bestScore = Integer.MAX_VALUE;

        for (JavaConstructor constr : getConstructors()) {
            int score = CoerceLuaToJava.scoreParamTypes(luaArgs, constr.getParamTypes());
            if (score == 0) {
                return constr; // Perfect match, return at once
            } else if (score < bestScore) {
                bestScore = score;
                bestMatch = constr;
            }
        }

        return bestMatch;
    }

    public JavaConstructor[] getConstructors() {
        if (constrs == null) {
            Constructor<?>[] cs = clazz.getConstructors();

            JavaConstructor[] result = new JavaConstructor[cs.length];
            for (int n = 0; n < cs.length; n++) {
                result[n] = new JavaConstructor(cs[n]);
            }
            constrs = result;
        }
        return constrs;
    }

    public ClassMetaTable getMetatable() {
        if (metaTable == null) {
            metaTable = new ClassMetaTable(this);
        }
        return metaTable;
    }

    public Field getField(LuaValue name) {
        if (fields == null) {
            fields = new HashMap<LuaString, Field>();
            for (Field f : clazz.getFields()) {
                fields.put(valueOf(f.getName()), f);
            }
        }
        return fields.get(name);
    }

    public JavaMethod[] getMethods(LuaValue name) {
        if (methods == null) {
            Method[] marr = clazz.getMethods();
            Arrays.sort(marr, methodSorter);

            methods = new HashMap<LuaString, JavaMethod[]>();

            String curName = null;
            List<JavaMethod> list = new ArrayList<JavaMethod>();
            for (Method m : marr) {
                // Workaround for https://bugs.openjdk.java.net/browse/JDK-4283544
                m.setAccessible(true);

                if (!m.getName().equals(curName)) {
                    if (curName != null) {
                        methods.put(valueOf(curName), list.toArray(new JavaMethod[list.size()]));
                    }
                    curName = m.getName();
                    list.clear();
                }
                list.add(new JavaMethod(m));
            }

            if (curName != null) {
                methods.put(LuaString.valueOf(curName), list.toArray(new JavaMethod[list.size()]));
            }
        }
        return methods.get(name);
    }

    public boolean hasMethod(LuaValue name) {
        return getMethods(name) != null;
    }

    @LuaSerializable
    private static class JavaClassRef implements IReadResolveSerializable {

        private static final long serialVersionUID = 1L;

        private final Class<?> clazz;

        public JavaClassRef(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public Object readResolve() throws ObjectStreamException {
            return LuajavaLib.getClassInfo(clazz);
        }
    }

    private static final class MethodSorter implements Comparator<Method> {

        @Override
        public int compare(Method m1, Method m2) {
            return m1.getName().compareTo(m2.getName());
        }
    }

}
