/*
 * VariGA, a variable-length integer-array genome GA.
 *
 * See Readme.txt for copyright and licensing.
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

public class VariGA {

    // Dummy fitness function basically specifying call() as the interface
    public static class FitnessFunction {
        public float call(Individual ind) {
            return ind.get(0) / (float) ind.geneLimit;
        }
    }

    // Maximise the sum of the individual's genes.
    public static class SumMaximiser extends FitnessFunction {
        public float call(Individual ind) {
            // calculating fitness as the sum of genes
            float fitness = 0.0f;
            for (int i: ind) {
                fitness += i;
            }
            return fitness;
        }
    }

    // Maximise the variety of the notes in the MusicGraph's output
    public static class MusicGraphVarietyMaximiser extends FitnessFunction {
        public float call(Individual ind) {
            // Create a graph and take its variety as fitness
            MusicGraph mg = MusicGraph.constructFromIntArray(ind);
            float fitness = mg.variety();
            return fitness;
        }
    }

    // Minimise the variety of the notes in the MusicGraph's output:
    // for experimental purposes
    public static class MusicGraphVarietyMinimiser extends FitnessFunction {
        public float call(Individual ind) {
            // Create a graph and take 1 / (1+variety) as fitness
            MusicGraph mg = MusicGraph.constructFromIntArray(ind);
            float fitness = 1.0f / (mg.variety() + 1.0f);
            return fitness;
        }
    }


    // FIXME there should be a boolean for minimise/maximise fitness
    // As it is, we assume fitness is to be maximised, so we put
    // highest-fitness individuals *first* in the sort.
    public class IndividualFitnessComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            float f1 = (float) ((Individual) o1).fitness;
            float f2 = (float) ((Individual) o2).fitness;
            if (f1 > f2) {
                return -1;
            } else if (f1 < f2) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    // convert to an individual
	public Individual toIndividual(ArrayList<Integer> genes) {
		return new Individual(genes);
	}
    
    // Individual
    ////////////////////////////////////////////////////////////////

    public static class Individual extends ArrayList<Integer> {

        float fitness;
        int geneLimit;

        public Individual() {
            fitness = 0;
            geneLimit = 128;
        }

        public Individual(ArrayList<Integer> genes) {
            fitness = 0;
            geneLimit = 128;
            for (int x = 0; x < genes.size(); x++) {
        		this.add(genes.get(x));
        	}
        }
        
        public Individual(int minSize, int maxSize) {
            fitness = 0;
            geneLimit = 128;
            int size = minSize + rng.nextInt(maxSize - minSize);
            for (int i = 0; i < size; i++) {
                add(rng.nextInt(geneLimit));
            }
        }

        public void mutate(float prob) {
            for (int i = 0; i < size(); i++) {
                if (rng.nextDouble() < prob) {
                    set(i, rng.nextInt(geneLimit));
                }
            }
        }

        public Individual crossover(Individual other) {
            Individual child = new Individual();
            int xoverpt0 = 0;
            int xoverpt1 = other.size();

            // two issues where we hardcode behaviour suitable for
            // MusicGraph encoding. there is a genome "segment size"
            // (equal to 3 for now): the two xover pts should be the
            // same mod segmentSize, eg 10 (= 1) and 13 (= 1) would be
            // ok. This helps to keep genes semantics preserved after
            // crossover, avoiding the ripple effect of GE. also,
            // there is a minimum size for genomes.
            int segmentSize = 3;
            int minSize = 12;
            while (xoverpt0 + (other.size() - xoverpt1) < minSize) {
                xoverpt0 = rng.nextInt(size());
                int pt0modSegment = xoverpt0 % segmentSize;
                xoverpt1 = rng.nextInt(other.size());
                int pt1modSegment = xoverpt1 % segmentSize;

                // enforce the same-mod restriction:
                xoverpt1 += (pt0modSegment - pt1modSegment);
                if (xoverpt1 >= other.size()) {
                    xoverpt1 -= segmentSize;
                }
            }

            // System.out.println("original lengths: " + size() + "; " + other.size());
            // System.out.println("in hacked xover. pts are " + xoverpt0 + " and "
            //                    + xoverpt1 + "; new length is " +
            //                    (xoverpt0 + (other.size() - xoverpt1)));
            
            for (int i = 0; i < xoverpt0; i++) {
                child.add(get(i));
            }
            for (int i = xoverpt1; i < other.size(); i++) {
                child.add(other.get(i));
            }
            return child;
        }

        public void evaluate(FitnessFunction f) {
            fitness = f.call(this);
        }
    }

    // Population
    ////////////////////////////////////////////////////////////

    public class Population extends ArrayList<Individual> {
        float bestFitness, worstFitness, meanFitness;
        int bestIndSize;
        Individual best;

        public Population() {
        }

        public Population(int size, int minIndSize, int maxIndSize) {
            for (int i = 0; i < size; i++) {
                add(new Individual(minIndSize, maxIndSize));
            }
        }

        public void evaluate(FitnessFunction f) {
            bestFitness = -999999.9f;
            worstFitness = 999999.9f;
            meanFitness = 0.0f;
            bestIndSize = 0;

            for (Individual ind: this) {
                ind.evaluate(f);
                if (ind.fitness > bestFitness) {
                    best = ind;
                    bestFitness = ind.fitness;
                    bestIndSize = ind.size();
                }
                if (ind.fitness < worstFitness) {
                    worstFitness = ind.fitness;
                }
                meanFitness += ind.fitness;
            }
            meanFitness /= size();

            System.out.println(getStatistics());
            // Sort in ascending order, that is best-first.
            Collections.sort(pop, new IndividualFitnessComparator());
        }

        public String getStatistics() {
            // generation number, best, mean, and worst fitness, and
            // best individual's genome size.
            return ("Generation " + generation + " "
                    + bestFitness + " "
                    + meanFitness + " "
                    + worstFitness + " "
                    + bestIndSize);
        }

        // copy some best inds straight through from old to this.
        public void elitism(Population old) {
            int i = 0;
            while (size() < popsize * elitismProp) {
                add(old.get(i++));
            }
        }

        // crossover good individuals from old to add to this.
        public void crossover(Population old) {
            int tournamentSize = 7;

            while (size() < popsize) {
                Individual p0 = old.select(tournamentSize);
                Individual p1 = old.select(tournamentSize);
                add(p0.crossover(p1));
            }
        }

        // mutate some of our own individuals.
        public void mutate() {
            float pMut = 0.05f;
            // avoid mutating the elite individuals, which are at the start
            for (int i = (int) (popsize * elitismProp) + 1; i < size(); i++) {
                Individual ind = get(i);
                ind.mutate(pMut);
            }
        }

        public Individual select() {
            return select(size());
        }

        public Individual select(int tournamentSize) {
            Individual best = null;
            float bestFitness = -999999999.9f;
            for (int i = 0; i < tournamentSize; i++) {
                Individual test = get(rng.nextInt(size()));
                if (test.fitness > bestFitness) {
                    best = test;
                    bestFitness = test.fitness;
                }
            }
            if (best == null) {
                System.out.println("Error: failed to select individual.");
            }
            return best;
        }
    }

    static // VariGA proper
    ////////////////////////////////////////////////////////////////////////

    Random rng;
    int generation;
    int popsize;
    Population pop;
    int nGenerations;
    FitnessFunction fitnessFunction;
    float elitismProp;

    public VariGA(FitnessFunction f) {
        fitnessFunction = f;
        rng = new Random();
        generation = 0;
        popsize = 20;
        elitismProp = 0.05f;
        int minIndSize = 30;
        int maxIndSize = 60;
        pop = new Population(popsize, minIndSize, maxIndSize);
        nGenerations = 10;
    }

    public void step() {
        step(true);
    }

    // allow user to call step(false) to perform a step without
    // running evaluate(). this is useful for interactive settings
    // where user has set fitness values already.
    public void step(boolean performEval) {
        if (performEval) {
            pop.evaluate(fitnessFunction);
        }

        Population tmp = new Population();
        tmp.elitism(pop);
        tmp.crossover(pop);
        tmp.mutate();
        pop = tmp;
        generation++;
    }

    public void evolve() {
        for (int i = 0; i < nGenerations; i++) {
            step();
        }
        // final population has not yet been evaluated, so do it now
        pop.evaluate(fitnessFunction);
    }

    public static void main(String[] args) {

        String dataFilename = args[0];
        String midiFilename = "";
        if (args.length > 1) {
            midiFilename = args[1];
        } else {
            midiFilename = "data/opening.mid";
        }
        // VariGA ga = new VariGA(new FeatureVector.
        //                        MIDIFeatureTarget(dataFilename, midiFilename));
        VariGA ga = new VariGA(new MusicGraphVarietyMaximiser());
        // VariGA ga = new VariGA(new FitnessFunction());
        // VariGA ga = new VariGA(new SumMaximiser());
        // VariGA ga = new VariGA(new FeatureVector.
        //                        FeatureTarget(dataFilename));
        // VariGA ga = new VariGA(new FeatureVector.
        //                        NoteLengthMaximiser(dataFilename));
        // VariGA ga = new VariGA(new FeatureVector.
        //                        AntiRobotDevice(dataFilename));
        // VariGA ga = new VariGA(new FeatureVector.
        //                        MinimiseRepeatedRhythmAndPitchTarget
        //                        (dataFilename));

        System.out.println("Evolution start");
        ga.evolve();
        System.out.println("Evolution end");
        Individual best = ga.pop.best;
        System.out.println("best ind: " + best);

        // save the best individual's MIDI output now. FIXME this is a
        // hack: better solution would be to make each fitness
        // function capable of creating a phenotype, and maybe add a
        // fitnessFunction.save() method (different from toString()).

        // Run the individual and save the phrases it makes.
        MusicGraph mg = MusicGraph.constructFromIntArray(best);
        mg.writeMIDI(dataFilename);
        mg.writeEPS(dataFilename + ".dot");

        // save some extra interesting information.
        for (String node: MusicGraph.nodeTypes) {
            System.out.println(node);
        }
    }

}