package src.particlefilter;

public class GaussianMotion2D {
    private final double maxSpeed; // m/s
    private final double minSpeed; // m/s

    public GaussianMotion2D(double maxSpeed, double minSpeed) {
        this.maxSpeed = maxSpeed;
        this.minSpeed = minSpeed;
    }

    /**
     * Predicts and updates the new position of a particle based on the motion model.
     * Destructive - updates the particle's latitude and longitude.
     * @param p The particle to update.
     */
    public void predict(Particle p) {
        double[] pos = p.getPos();
        double[] velocity = p.getVelocity();
        double newX = pos[0] + velocity[0];
        double newY = pos[1] + velocity[1];
        p.setPos(newX, newY);
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }

    public double getMinSpeed() {
        return minSpeed;
    }
}
