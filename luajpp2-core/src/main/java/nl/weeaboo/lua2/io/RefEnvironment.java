package nl.weeaboo.lua2.io;

import java.io.Serializable;

final class RefEnvironment implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;

    public RefEnvironment(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Id may not be null");
        }

        this.id = id;
    }

    public Object resolve(Environment env) {
        return env.getObject(id);
    }

}
