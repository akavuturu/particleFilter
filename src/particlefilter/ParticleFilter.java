package src.particlefilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@SuppressWarnings("java:S1659")
public class ParticleFilter {
    private List<Particle> particles;
    private final Random rand = new Random();
    private final GaussianMotion2D motionModel;
    private static final double POSITION_PERTURBATION = 5;
    private static final double VELOCITY_PERTURBATION = 0.5;
    private double[] estimate;

    public ParticleFilter(int numParticles, double initX, double initY, double initStddev,
            GaussianMotion2D motionModel) {
        this.motionModel = motionModel;
        this.particles = new ArrayList<>();
        this.estimate = new double[]{initX, initY};

        for (int i = 0; i < numParticles; i++) {
            // normally distributed initial positions
            double x = initX + rand.nextGaussian() * initStddev;
            double y = initY + rand.nextGaussian() * initStddev;

            // uniformly distributed initial velocities around speed range and full circle
            double speed = motionModel.getMinSpeed() + ((motionModel.getMaxSpeed() - motionModel.getMinSpeed()) * rand.nextDouble());
            double course = 2 * Math.PI * rand.nextDouble();
            particles.add(new Particle(x, y, 1.0 / numParticles, speed * Math.cos(course), speed * Math.sin(course)));
        }
    }

    /** 
     * Performs a full Particle Filter step: motion update, information update, and resampling.
     * First uses motionModel.predict() to update particles based on motion.
     * Then applies the sensorModel to calculate the likelihood of each particle given the current measurement
     * for importance sampling (checking for weight degeneracy). Finally, it resamples the particles based on their weights
     * and updates new weights to be uniform.
     * @param currLat Current latitude measurement.
     * @param currLon Current longitude measurement.
     */
    public void motionUpdate() {
        // Motion update
        for (Particle p : this.particles) {
            motionModel.predict(p);
        }

        updateEstimate();
    }

    public void updateWeights(List<double[]> observations, List<Sensor> sensors) {
        // Measurement update with multiple observations
        for (Particle p : this.particles) {
            double particleWeight = 0.0; //log space

            // Multiply likelihoods from all observations
            for (double[] obs : observations) {
                int sensorId = (int) obs[0];
                if (sensorId < sensors.size()) {
                    Sensor sensor = sensors.get(sensorId);
                    double likelihood = sensor.likelihood(obs, p);

                    particleWeight += Math.log(likelihood + 1e-10);
                }
            }

            p.setWeight(Math.exp(particleWeight));
        }

        updateEstimate();
    }
    
    public void resample() {
        List<Particle> newParticles = new ArrayList<>();
        int numParticles = particles.size();

        double totalWeight = particles.stream().mapToDouble(p -> p.getWeight()).sum();
        if (totalWeight == 0.0)
            throw new ArithmeticException("Zero total weight for particles!");

        // build distribution
        double[] cdf = new double[numParticles];
        cdf[0] = particles.get(0).getWeight() / totalWeight;
        for (int i = 1; i < numParticles; i++) {
            cdf[i] = cdf[i - 1] + particles.get(i).getWeight() / totalWeight;
        }

        double step = 1.0 / numParticles, beta;
        double u = rand.nextDouble() * step;
        double[] pos, vel;
        double newX, newY;
        double newVx, newVy;

        int j = 0;
        for (int i = 0; i < numParticles; i++) {
            beta = u + i * step;
            while (j < numParticles - 1 && cdf[j] < beta)
                j++;

            pos = particles.get(j).getPos();
            vel = particles.get(j).getVelocity();
            newX = pos[0] + rand.nextGaussian() * POSITION_PERTURBATION;
            newY = pos[1] + rand.nextGaussian() * POSITION_PERTURBATION;
            newVx = vel[0] + rand.nextGaussian() * VELOCITY_PERTURBATION;
            newVy = vel[1] + rand.nextGaussian() * VELOCITY_PERTURBATION;
            Particle chosenPerturbed = new Particle(newX, newY, particles.get(j).getWeight(), newVx, newVy);
            newParticles.add(chosenPerturbed);
        }

        for (Particle p : newParticles) {
            p.setWeight(1.0 / numParticles);
        }

        particles.clear();
        particles.addAll(newParticles);
    }
    
    public void updateEstimate() {
        double predX = 0.0, predY = 0.0;
        double totalWeight = 0.0;

        for (Particle p : particles) {
            double[] pos = p.getPos();
            double weight = p.getWeight();
            predX += pos[0] * weight;
            predY += pos[1] * weight;
            totalWeight += weight;
        }

        if (totalWeight > 0) {
            estimate[0] = predX / totalWeight;
            estimate[1] = predY / totalWeight;
        }
    }

    public double[] getEstimate() {
        return estimate.clone();
    }

    public List<Particle> getParticles() {
        return this.particles;
    }
}
