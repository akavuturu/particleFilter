package src.particlefilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * World simulator that manages the true state and multiple sensors
 */
public class Simulator {
    private double trueX;
    private double trueY;
    private double velocity;
    private double course; // degrees
    private List<Sensor> sensors;
    private Random random;
    
    public Simulator(double initialX, double initialY, double velocity, double course) {
        this.trueX = initialX;
        this.trueY = initialY;
        this.velocity = velocity;
        this.course = course;
        this.sensors = new ArrayList<>();
        this.random = new Random();
    }
    
    public void addSensor(Sensor sensor) {
        sensors.add(sensor);
    }
    
    public void step(double deltaTime) {
        trueX += velocity * Math.cos(Math.toRadians(course)) * deltaTime;
        trueY += velocity * Math.sin(Math.toRadians(course)) * deltaTime;
    }
    
    /**
     * Get all sensor readings as a simple array of doubles
     * Format: [sensorId, bearing, x, y]
     */
    public List<double[]> getObservations() {
        List<double[]> observations = new ArrayList<>();
        
        for (int i = 0; i < sensors.size(); i++) {
            Sensor sensor = sensors.get(i);
            if (sensor.canDetect(trueX, trueY)) {
                double[] obs = sensor.observe(trueX, trueY, random);
                if (obs.length > 0) {
                    // Prepend sensor ID to the observation
                    double[] fullObs = new double[obs.length + 1];
                    fullObs[0] = i; // sensor ID
                    System.arraycopy(obs, 0, fullObs, 1, obs.length);
                    observations.add(fullObs);
                }
            }
        }
        return observations;
    }
    
    public double[] getTruePosition() {
        return new double[]{trueX, trueY};
    }
    
    public List<Sensor> getSensors() {
        return sensors;
    }
}