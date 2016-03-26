package nl.weeaboo.lua2.link;

abstract class AbstractLuaLink implements ILuaLink {

    private static final long serialVersionUID = 1L;

    private int wait;
    
    public int getWait() {
        return wait;
    }

    @Override
    public void setWait(int w) {
        wait = w;
    }

    @Override
    public boolean addWait(int dt) {
        if (dt < 0) {
            throw new IllegalArgumentException("Can't call addWait with a negative number: " + dt);
        }

        if (wait >= 0 && dt > 0) {
            setWait(wait + dt);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean decreaseWait(int dt) {
        if (dt < 0) {
            throw new IllegalArgumentException("Can't call decreaseWait with a negative number: " + dt);
        }

        if (wait > 0 && dt > 0) {
            setWait(Math.max(0, wait - dt));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void setMinimumWait(int w) {
        if (wait >= 0 && (w < 0 || w > wait)) {
            setWait(w);
        }
    }

}
