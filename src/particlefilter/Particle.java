package src.particlefilter;
public class Particle {
    private double x; // m
    private double y; // m
    private final double vX; // m/s
    private final double vY; // m/s
    private double weight;

    public Particle(double x, double y, double weight, double vX, double vY) {
        this.x = x;
        this.y = y;
        this.weight = weight;
        this.vX = vX;
        this.vY = vY;
    }

    public Particle copy() {
        return new Particle(this.x, this.y, this.weight, this.vX, this.vY);
    }

    public double[] getPos() {
        return new double[] {this.x, this.y};
    }

    public void setPos(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double[] getVelocity() {
        return new double[] { this.vX, this.vY };
    }

    public double getWeight() {
        return this.weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }
}
