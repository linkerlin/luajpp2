package nl.weeaboo.lua2.io;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 'External' environment used by {@link ObjectSerializer}. When serializing an object stored in the
 * environment, instead the object's id is stored. During deserialization, the id is used to retrieve the
 * object from the environment again.
 */
public final class Environment {

    private final Map<String, Object> idObjMap = new HashMap<String, Object>();
    private final IdentityHashMap<Object, String> objIdMap = new IdentityHashMap<Object, String>();

    public void add(String id, Object object) {
        if (id == null) {
            throw new IllegalArgumentException("Id may not be null");
        }
        if (object == null) {
            throw new IllegalArgumentException("Object may not be null");
        }

        remove(id, object);

        idObjMap.put(id, object);
        objIdMap.put(object, id);
	}

    public void remove(String id, Object obj) {
		idObjMap.remove(id);
		objIdMap.remove(obj);
	}

    public Object getObject(String id) {
		return idObjMap.get(id);
	}

    public String getId(Object obj) {
		return objIdMap.get(obj);
	}

	public int size() {
		return objIdMap.size();
	}

}
