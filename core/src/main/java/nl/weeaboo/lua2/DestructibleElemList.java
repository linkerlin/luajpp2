package nl.weeaboo.lua2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import nl.weeaboo.lua2.io.LuaSerializable;

/** List implementation that automatically removes destroyed elements */
@LuaSerializable
final class DestructibleElemList<T extends IDestructible> implements Iterable<T>, Serializable {

    private static final long serialVersionUID = 1L;

    private final List<T> elements = new CopyOnWriteArrayList<T>();

    public void add(T elem) {
        elements.add(elem);
    }

    public void remove(Object elem) {
        elements.remove(elem);
    }

    public void clear() {
        elements.clear();
    }

    public void destroyAll() {
        for (T elem : getSnapshot()) {
            elem.destroy();
        }

        // We can't use clear, because destroying elements may result in new elements being added to this list
        removeDestroyedElements();
    }

    public boolean contains(Object elem) {
        removeDestroyedElements();

        return elements.contains(elem);
    }

    public int size() {
        removeDestroyedElements();

        return elements.size();
    }

    @Override
    public Iterator<T> iterator() {
        return getSnapshot().iterator();
    }

    public Collection<T> getSnapshot() {
        removeDestroyedElements();

        return elements;
    }

    private void removeDestroyedElements() {
        // Determine which elements are destroyed
        List<T> removed = null;
        for (T elem : elements) {
            if (elem.isDestroyed()) {
                if (removed == null) {
                    removed = new ArrayList<T>();
                }
                removed.add(elem);
            }
        }

        // Remove destroyed elements
        if (removed != null) {
            elements.removeAll(removed);
        }
    }

}
