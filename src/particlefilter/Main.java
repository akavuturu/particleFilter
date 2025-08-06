package src.particlefilter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class Main {

    private static void exportParticleStates(FileWriter csvWriter, ParticleFilter filter, 
                                           int timeStep, double trueX, double trueY, double estX, double estY) throws IOException {
        List<Particle> particles = filter.getParticles();

        // Write each particle's state
        for (int i = 0; i < particles.size(); i++) {
            double[] pos = particles.get(i).getPos();
            double weight = particles.get(i).getWeight();

            csvWriter.append(String.format("%d,%d,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%n",
                    timeStep, // TimeStep
                    i, // ParticleID
                    pos[0], // Particle X
                    pos[1], // Particle Y
                    weight, // Particle Weight,
                    trueX, // True X position
                    trueY, // True Y position
                    estX, // Estimated X
                    estY  // Estimated Y
            ));
        }
        csvWriter.flush();
    }

    
    @SuppressWarnings("java:S106")
    public static void main(String[] args) {        
        double speed = 15; // target speed, m/s
        double courseDegrees = 45; // target course, degrees
        double err = 0.0;
        int timeSteps = 200;
        double[] pred;
        int dT = 1; // simulation time step, sec
        int observationStep = 10; // how often observations are recorded from sensors
        String csvName = "particle_states.csv";

        ParticleFilter filter = new ParticleFilter(500, 0.0, 0.0, 300.0, new GaussianMotion2D(20, 12));

        Simulator sim = new Simulator(0.0, 0.0, speed, courseDegrees);

        sim.addSensor(new Sensor(1500, 3000, 2500, 15, 100));
        sim.addSensor(new Sensor(0, 1500, 2500, 15, 100));

        try (FileWriter csvWriter = new FileWriter(csvName)) {
            // Write CSV header
            csvWriter.append("TimeStep,ParticleID,X,Y,Weight,TrueX,TrueY,EstimateX,EstimateY\n");
            csvWriter.flush();

            for (int t = 0; t < timeSteps; t += dT) {
                // Step the simulation
                sim.step(dT);
                double[] truePos = sim.getTruePosition();
                
                // Get observations every 10 steps, just predict otherwise
                if (t % observationStep == 0) {
                    List<double[]> observations = sim.getObservations();
                    List<Sensor> sensors = sim.getSensors();
                    
                    if (!observations.isEmpty()) {
                        filter.motionUpdate();
                        filter.updateWeights(observations, sensors);
                        pred = filter.getEstimate();
                        exportParticleStates(csvWriter, filter, t, truePos[0], truePos[1], pred[0], pred[1]);
                        filter.resample();
                        
                        System.out.printf("Time %d [OBS] | True: (%.2f, %.2f) | %d observations | ",
                                t, truePos[0], truePos[1], observations.size());
                    } else {
                        filter.motionUpdate();
                        pred = filter.getEstimate();

                        System.out.printf("Time %d [PRED] | True: (%.2f, %.2f) | No observations | ",
                                t, truePos[0], truePos[1]);
                        exportParticleStates(csvWriter, filter, t, truePos[0], truePos[1], pred[0], pred[1]);
                    }
                } else {
                    filter.motionUpdate();
                    pred = filter.getEstimate();
                    System.out.printf("Time %d [PRED] | True: (%.2f, %.2f) | No observations | ",
                            t, truePos[0], truePos[1]);
                    exportParticleStates(csvWriter, filter, t, truePos[0], truePos[1], pred[0], pred[1]);
                }
                
                System.out.printf("Estimate: (%.2f, %.2f)%n", pred[0], pred[1]);
                
                err = Math.sqrt(Math.pow(pred[0] - truePos[0], 2) + Math.pow(pred[1] - truePos[1], 2))
                        / (Math.sqrt(Math.pow(truePos[0], 2) + Math.pow(truePos[1], 2)) + 1e-6);
                System.out.printf("Error: %.4f%n", err);
            }
        } catch (IOException e) {
            System.out.println("Error writing states to CSV, " + e.getMessage());
        }
    }
}
