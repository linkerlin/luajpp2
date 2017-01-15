package nl.weeaboo.lua2.lib;

public final class FastMath {

    private FastMath() {
    }

    public static float PI = (float)Math.PI;
    public static float TWO_PI = PI + PI;
    public static float HALF_PI = PI * .5f;

    /**
     * Lookup table based drop-in replacement for {@link Math#sin(double)}.
     */
    public static float sin(float s) {
        return fastSin(s * SinLut.fastAngleScale);
    }

    /**
     * Lookup table based drop-in replacement for {@link Math#cos(double)}.
     */
    public static float cos(float s) {
        return fastCos(s * SinLut.fastAngleScale);
    }

    public static float acos(float s) {
        return fastArcCos(s) / SinLut.fastAngleScale;
    }

    public static float asin(float s) {
        return fastArcSin(s) / SinLut.fastAngleScale;
    }

    //-------------------------------------------------------------------------
    //--- LUT implementations of trig functions -------------------------------
    //-------------------------------------------------------------------------

    public static float getFastAngleScale() {
        return SinLut.fastAngleScale;
    }

    public static float fastSin(int angle) {
        return SinLut.LUT[angle & SinLut.MASK];
    }

    public static float fastCos(int angle) {
        return SinLut.LUT[(angle + SinLut.COS_OFFSET) & SinLut.MASK];
    }

    public static float fastSin(float angle) {
        int a = (angle >= 0 ? (int)(angle) : (int)(angle - 1));
        float prev = fastSin(a);
        float next = fastSin(a + 1);

        float result = prev + (next - prev) * Math.abs(angle - a);
        return result;
    }

    public static float fastCos(float angle) {
        return fastSin(angle + (SinLut.SIZE >> 2));
    }

    public static float fastArcSin(float a) {
        if (Float.isNaN(a) || a < -1 || a > 1) {
            return Float.NaN;
        }
        if (a == -0f || a == 0f) {
            return a;
        }

        int halfLutSize = ArcSinLut.MAX_INDEX >> 1;
        float floatIndex = halfLutSize + a * halfLutSize;
        int index = (int)floatIndex;
        float prev = ArcSinLut.LUT[index];
        float next = ArcSinLut.LUT[Math.min(ArcSinLut.MAX_INDEX, index + 1)];

        float result = prev + (next - prev) * Math.abs(floatIndex - index);
        return result;
    }

    public static float fastArcCos(float a) {
        return (SinLut.SIZE >> 2) - fastArcSin(a);
    }

    public static float fastArcTan2(float dy, float dx) {
        float c1 = SinLut.SIZE >> 3;
        float c2 = 3 * c1;

        float absDy = Math.abs(dy);
        if (absDy == 0.0) {
            absDy = 0.0001f;
        }

        float angle;
        if (dx >= 0) {
            float r = (dx - absDy) / (dx + absDy);
            angle = c1 - r * c1;
        } else {
            float r = (dx + absDy) / (absDy - dx);
            angle = c2 - r * c1;
        }

        if (dy < 0) {
            angle = -angle; // Negate if in quadrant 3 or 4
        }

        angle += (SinLut.SIZE >> 2);
        if (angle < 0) {
            angle += SinLut.SIZE;
        }
        return angle;
    }

    /** Inner class used to lazy-initialize the lookup table. */
    private static final class SinLut {

        static final int SIZE = 512;
        static final int MASK = 511;
        static final int COS_OFFSET = SIZE >> 2;
        static final float fastAngleScale = SIZE / (TWO_PI);

        static final float[] LUT;

        static {
            double s = Math.PI / (SIZE >> 1);

            LUT = new float[SIZE];
            for (int n = 0; n < SIZE; n++) {
                LUT[n] = (float)Math.sin(n * s);
            }
        }

    }

    /** Inner class used to lazy-initialize the lookup table. */
    private static final class ArcSinLut {

        private static int MAX_INDEX = 2048;

        static final float[] LUT;

        static {
            int halfLutSize = MAX_INDEX >> 1;

            LUT = new float[MAX_INDEX + 1];
            double f = 1.0 / halfLutSize;
            for (int n = 0; n <= MAX_INDEX; n++) {
                double d = Math.asin((n - halfLutSize) * f);
                LUT[n] = (float)(SinLut.fastAngleScale * d);
            }
        }

    }

}
