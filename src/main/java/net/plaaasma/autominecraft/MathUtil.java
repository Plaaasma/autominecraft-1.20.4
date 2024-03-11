package net.plaaasma.autominecraft;

public class MathUtil {
    private static final double FRAC_BIAS = Double.longBitsToDouble(4805340802404319232L);
    private static final double[] ASIN_TAB = new double[257];
    private static final double[] COS_TAB = new double[257];

    public static double fastInvSqrt(double pNumber) {
        double d0 = 0.5D * pNumber;
        long i = Double.doubleToRawLongBits(pNumber);
        i = 6910469410427058090L - (i >> 1);
        pNumber = Double.longBitsToDouble(i);
        return pNumber * (1.5D - d0 * pNumber * pNumber);
    }

    public static float wrapDegrees(float pValue) {
        float f = pValue % 360.0F;
        if (f >= 180.0F) {
            f -= 360.0F;
        }

        if (f < -180.0F) {
            f += 360.0F;
        }

        return f;
    }

    public static float rotlerp(float pAngle, float pTargetAngle, float pMaxIncrease) {
        float f = MathUtil.wrapDegrees(pTargetAngle - pAngle);
        if (f > pMaxIncrease) {
            f = pMaxIncrease;
        }

        if (f < -pMaxIncrease) {
            f = -pMaxIncrease;
        }

        return pAngle + f;
    }

    public static double atan2(double pY, double pX) {
        double d0 = pX * pX + pY * pY;
        if (Double.isNaN(d0)) {
            return Double.NaN;
        } else {
            boolean flag = pY < 0.0D;
            if (flag) {
                pY = -pY;
            }

            boolean flag1 = pX < 0.0D;
            if (flag1) {
                pX = -pX;
            }

            boolean flag2 = pY > pX;
            if (flag2) {
                double d1 = pX;
                pX = pY;
                pY = d1;
            }

            double d9 = fastInvSqrt(d0);
            pX *= d9;
            pY *= d9;
            double d2 = FRAC_BIAS + pY;
            int i = (int)Double.doubleToRawLongBits(d2);
            double d3 = ASIN_TAB[i];
            double d4 = COS_TAB[i];
            double d5 = d2 - FRAC_BIAS;
            double d6 = pY * d4 - pX * d5;
            double d7 = (6.0D + d6 * d6) * d6 * 0.16666666666666666D;
            double d8 = d3 + d7;
            if (flag2) {
                d8 = (Math.PI / 2D) - d8;
            }

            if (flag1) {
                d8 = Math.PI - d8;
            }

            if (flag) {
                d8 = -d8;
            }

            return d8;
        }
    }
}
