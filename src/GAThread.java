public class GAThread extends Thread {
    VariGA ga;
    NotePanel parent;
    boolean running;
    VariGA.FitnessFunction fitnessFunction;

    public GAThread (NotePanel _parent) {
        parent = _parent;
        fitnessFunction = new VariGA.MusicGraphVarietyMaximiser();
    }

    public void start() {
        ga = new VariGA(fitnessFunction);
        running = false;
        super.start();
    }

    public void run() {
        while (true) {
            // System.out.println("in run()");
            if (running) {
                System.out.println("running = true");
                // Perform step 5 of the procedure linking GA and GUI
                // (see GUI.java)
                ga.step(false);

                // Perform step 1 of the procedure linking GA and GUI.
                ga.pop.evaluate(fitnessFunction);

                // ask parent (NotePanel.java) to perform step 2
                parent.getNewPopulation();
                running = false;
            } else {
                try {
                    sleep((long) 500);
                } catch (Exception e) {
                }
            }
        }
    }

    public void step() {
        running = true;
    }

    public void quit() {
        running = false;
        interrupt();
    }
}

