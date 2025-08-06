package src.particlefilter;

import java.util.Random;

/**
 * Simple sensor that can do bearing, position, or both measurements
 */
public class Sensor {
    private double x;
    private double y;
    private double maxRange;
    private double bearingStdDev;
    private double positionStdDev;
    
    
    public Sensor(double x, double y, double maxRange, double bearingStdDev, double positionStdDev) {
        this.x = x;
        this.y = y;
        this.maxRange = maxRange;
        this.bearingStdDev = bearingStdDev;
        this.positionStdDev = positionStdDev;
    }
    
    public boolean canDetect(double targetX, double targetY) {
        double distance = Math.sqrt(Math.pow(targetX - x, 2) + Math.pow(targetY - y, 2));
        return distance <= maxRange;
    }
    
    /**
     * Generate observation
     * Returns: [bearing, x, y]
     */
    public double[] observe(double trueX, double trueY, Random random) {
        if (!canDetect(trueX, trueY)) {
            return new double[] {};
        }
        
        double trueBearing = Math.atan2(trueY - y, trueX - x);
        double noisyBearing = trueBearing + random.nextGaussian() * (bearingStdDev * Math.PI / 180);
        double noisyX = trueX + random.nextGaussian() * positionStdDev;
        double noisyY = trueY + random.nextGaussian() * positionStdDev;
        return new double[]{wrapToPi(noisyBearing), noisyX, noisyY};
    
    }
    
    /**
     * Calculate likelihood for a particle given an observation
     */
    public double likelihood(double[] observation, Particle particle) {
        if (observation.length < 4) {
            return 0.0;
        }

        double[] particlePos = particle.getPos();

        double measuredBearing = observation[1];

        // Calculate bearing likelihood (line of bearing)
        double predictedBearing = Math.atan2(particlePos[1] - y, particlePos[0] - x);
        double bearingDiff = wrapToPi(predictedBearing - measuredBearing);
        double bearingVariance = Math.pow(bearingStdDev * Math.PI / 180, 2);
        return Math.exp(-0.5 * (bearingDiff * bearingDiff) / bearingVariance) /
                Math.sqrt(2 * Math.PI * bearingVariance);
    }
    
    private static double wrapToPi(double angle) {
        while (angle < -Math.PI) angle += 2 * Math.PI;
        while (angle > Math.PI) angle -= 2 * Math.PI;
        return angle;
    }
    
    // Getters
    public double getX() { return x; }
    public double getY() { return y; }
    public double getMaxRange() { return maxRange; }
}