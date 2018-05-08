package nl.weeaboo.lua2.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.weeaboo.lua2.io.LuaSerializable;

/**
 * List implementation that automatically removes destroyed elements.
 */
@LuaSerializable
public final class DestructibleElemList<T extends IDestructible> implements Iterable<T>, Serializable {

    private static final long serialVersionUID = 2L;
    private static final Logger LOG = LoggerFactory.getLogger(DestructibleElemList.class);

    private List<T> elements = new ArrayList<T>();

    /** Adds an element to the list. */
    public void add(T elem) {
        elements = copyElements();
        elements.add(elem);
    }

    /** Removes an element from the list. */
    public void remove(Object elem) {
        elements = copyElements();
        elements.remove(elem);
    }

    /** Removes all elements from the list. */
    public void clear() {
        elements = copyElements();
        elements.clear();
    }

    /** Destroys all elements in the list and removes them. */
    public void destroyAll() {
        for (T elem : getSnapshot()) {
            elem.destroy();
        }

        // We can't use clear, because destroying elements may result in new elements being added to this list
        removeDestroyedElements();
    }

    /** Returns {@code true} if the specified element is contained within the list. */
    public boolean contains(Object elem) {
        removeDestroyedElements();

        return elements.contains(elem);
    }

    /** Returns the number of elements in the list. */
    public int size() {
        removeDestroyedElements();

        return elements.size();
    }

    @Override
    public Iterator<T> iterator() {
        return getSnapshot().iterator();
    }

    /** Returns an immutable snapshot of the list. */
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
            LOG.trace("Removing destroyed elements: {}", removed);
            elements = copyElements();
            elements.removeAll(removed);
        }
    }

    private List<T> copyElements() {
        return new ArrayList<T>(elements);
    }

}
