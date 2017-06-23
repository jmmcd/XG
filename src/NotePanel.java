/**
 *
 *
 */

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiUnavailableException;

import org.jgrapht.graph.*;
import org.jgrapht.generate.*;
import org.jgrapht.ext.*;
import org.jgrapht.event.*;
import org.jgrapht.util.*;
import org.jgrapht.experimental.alg.*;
import org.jgrapht.experimental.permutation.*;
import org.jgrapht.experimental.isomorphism.*;
import org.jgrapht.alg.util.*;
import org.jgrapht.experimental.*;
import org.jgrapht.traverse.*;
import org.jgrapht.experimental.equivalence.*;
import org.jgrapht.experimental.alg.color.*;
import org.jgrapht.experimental.touchgraph.*;
import org.jgrapht.alg.*;
import org.jgrapht.*;
import org.jgrapht.demo.*;

import java.util.*;

import processing.core.PApplet;
import processing.core.PFont;

public class NotePanel extends PApplet {

    int fps = 30;
    int num = 30;
    float mx[] = new float[num];
    float my[] = new float[num];
    //      *	(Index zero means MIDI channel 1)
    int nChannelNumber = 0;
    int nNoteNumber = 60;
    int nVelocity = 100;
    Synthesizer	synth;

    MidiChannel[]	channels;
    MidiChannel	channel;

    private MusicGraph mg;
    ArrayList<MusicGraph> musicGraphs;
    GraphIDGenerator g;
    int nInds;
    int currentInd;

    int[] xvals;
    int[] yvals;
    int[] zvals;
    int[] xvolvals;
    int[] yvolvals;
    int[] zvolvals;
    int[] invals0;
    int[] invals1;
    int[] invals2;


    int[][] notes;

    int[] prevNotes;

    GAThread gaThread;

    GUI parent;

    boolean mouseMode;
    float[][] data;
    int t;

    String xyzFilename;

    public NotePanel(GUI _parent, String _xyzFilename) {
        super();
        System.out.println("NotePanel constructor.");
        parent = _parent;
        xyzFilename = _xyzFilename;
        if (xyzFilename == null || xyzFilename.equals("")) {
            mouseMode = true;
            System.out.println("Mouse mode on.");
        } else {
            mouseMode = false;
            t = 0;
            data = MusicGraph.getDataFromFile(xyzFilename);
            System.out.println("Mouse mode off. Reading " + xyzFilename);
        }
        this.g = new GraphIDGenerator();
    }


    public void setup() {

        int side = 530;

        fps = 30;
        size(side, side);
        smooth();
        noStroke();
        fill(255, 153);
        frameRate(fps);

        System.out.println("width = " + width);

        notes = new int[2][3];
        notes[0][0] = 0;
        notes[1][0] = 0;
        notes[0][1] = 0;
        notes[1][1] = 0;
        notes[0][2] = 0;
        notes[1][2] = 0;
        prevNotes = new int[3];
        prevNotes[0] = 0;
        prevNotes[1] = 0;
        prevNotes[2] = 0;

        xvals = new int[width];
        yvals = new int[width];
        zvals = new int[width];
        xvolvals = new int[width];
        yvolvals = new int[width];
        zvolvals = new int[width];
        invals0 = new int[width];
        invals1 = new int[width];
        invals2 = new int[width];


        /*
         *	Here, we simply request the default synthesizer and open it.
         */
        synth = null;
        try {
            synth = MidiSystem.getSynthesizer();
            synth.open();
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
            System.exit(1);
        }

        /*
         */
        channels = synth.getChannels();
        channel = channels[nChannelNumber];

        musicGraphs = new ArrayList<MusicGraph>();
        nInds = 10;

        gaThread = new GAThread(this);
        gaThread.start();
        mapGraphs(gaThread.ga.pop);
        System.out.println("in init, there are " + musicGraphs.size() + " graphs");
        auditionIndividual(0);

        currentInd = 0;
    }

    public void draw() {
        background(255);

        if (mouseMode) {
            // draw the mouse-path thing

            // Cycle through the array, using a different entry on each frame.
            // Using modulo (%) like this is faster than moving all the values over.
            int which = frameCount % num;
            mx[which] = mouseX;
            my[which] = mouseY;

            for (int i = 0; i < num; i++) {
                // which+1 is the smallest (the oldest in the array)
                int index = (which+1 + i) % num;
                ellipse(mx[index], my[index], i/2, i/2);
            }
        }

        int whichNote = frameCount % width;

        int framesPerQuantum = 5;
        if (frameCount % framesPerQuantum == 0) {
            // System.out.println("in draw, frameCount % 10 == 0");

            // FIXME deal with section/bar length
            float time;
            float beat;
            float x, y, z;

            if (mouseMode) {
                time = (frameCount / (float) fps) % 64;
                beat = time % 4;
                x = 1.0f - mouseX / (float) width;
                y = 1.0f - mouseY / (float) height;
                z = 0.0f;
                if (mousePressed) {
                    z = 1.0f;
                }
            } else {
                t++;
                t %= data.length;
                float[] line = data[t];
                time = line[0];
                beat = line[1];
                x = line[2];
                y = line[3];
                z = line[4];
            }

            float[] inputs = {x, y, z};
            float[] times = {time, beat};
            notes = mg.run(times, inputs);
            for (int i = 0; i < 3; i++) {
                nNoteNumber = notes[0][i];
                nVelocity = notes[1][i];
                //  && prevNotes[i] != nNoteNumber
                if (nVelocity > 0) {
                    channel.noteOn(nNoteNumber, nVelocity);
                    prevNotes[i] = nNoteNumber;
                } else if (nVelocity == 0) {
                    channel.noteOn(prevNotes[i], 0);
                }
            }

            for (int w = 0; w < framesPerQuantum; w++) {
                invals0[(whichNote + w) % width] = (int) (inputs[0] * height);
                invals1[(whichNote + w) % width] = (int) (inputs[1] * height);
                invals2[(whichNote + w) % width] = (int) (inputs[2] * height);
            }
        }

        xvals[whichNote] = notes[0][0];
        yvals[whichNote] = notes[0][1];
        zvals[whichNote] = notes[0][2];
        xvolvals[whichNote] = notes[1][0];
        yvolvals[whichNote] = notes[1][1];
        zvolvals[whichNote] = notes[1][2];


        for (int i = 1; i < width; i++) {
            int index = (whichNote + 1 + i) % width;
            int[] c;

            // draw inputs
            fill(256, 0, 0);
            rect(i, invals0[index], 10, 10);
            //stroke(256, 0, 0);
            rect(i, invals1[index], 10, 10);
            //stroke(256, 0, 0);
            rect(i, invals2[index], 10, 10);


            // draw notes

			// check that notes are not "hold" or "note-off"
			if (xvolvals[index] > 0) {
				c = getColorFromVol(xvolvals[index]);
				fill(c[0], c[1], c[2]);
				rect(i, getYFromNote(xvals[index]), 10, 10);
			}
			if (yvolvals[index] > 0) {
				c = getColorFromVol(yvolvals[index]);
				fill(c[0], c[1], c[2]);
				rect(i, getYFromNote(yvals[index]), 10, 10);
			}
			if (zvolvals[index] > 0) {
				c = getColorFromVol(zvolvals[index]);
				fill(c[0], c[1], c[2]);
				rect(i, getYFromNote(zvals[index]), 10, 10);
			}
        }
    }

    int getYFromNote(int note) {
        int maxNote = 90;
        return height - (int) (height * (float) (note / (float) maxNote));
    }

    int[] getColorFromVol(int vol) {
        int[] c = new int[3];
        // make a shade of green
        c[0] = 0; c[1] = min(256, (int) (vol * 1.5)); c[2] = 0;
        return c;
    }



    void dispose() {
        /* Close the synthesizer.
         */
        synth.close();

    }

    public void keyPressed() {
        System.out.println("in applet, got a key!");
        if (key == ' ') {
            System.out.println("triggering next from applet");
            parent.nextBtn.doClick();
        } else if (key == 's') {
            System.out.println("triggering select from applet");
            parent.selectBtns.get(currentInd).doClick();
        } else if (key >= '0' && key <= '9') {
            int i = key - '0';
            parent.auditionBtns.get(i).doClick();
        } else if (key == 'y') {
            parent.selectBtns.get(currentInd).setSelected(true);
            parent.auditionBtns.get((currentInd + 1) % nInds).doClick();
        } else if (key == 'n') {
            parent.selectBtns.get(currentInd).setSelected(false);
            parent.auditionBtns.get((currentInd + 1) % nInds).doClick();
            System.out.println("NEXT");
        } else if (key == 'p') {
            parent.selectBtns.get(currentInd).setSelected(false);
            parent.auditionBtns.get((currentInd - 1 + nInds) % nInds).doClick();
        } else if (key == 'w') {
            ArrayList<Integer> ind = gaThread.ga.pop.get(currentInd);
            System.out.println("writing individual " + currentInd + " to screen:" + ind);
        }
    }

    public void auditionIndividual(int ind) {
        System.out.println("trying to audition ind " + ind);
        if (ind < musicGraphs.size()) {
            currentInd = ind;
            mg = musicGraphs.get(ind);
        }
    }

    public void getNewPopulation() {
        System.out.println("new graphs!");
        mapGraphs(gaThread.ga.pop);
        mg = musicGraphs.get(0);
        for (int i = 0; i < nInds; i++) {
            parent.selectBtns.get(i).setSelected(false);
        }
        parent.auditionBtns.get(0).setSelected(true);
    }


    public void mapGraphs(VariGA.Population pop) {
        musicGraphs.clear();
        for (int i = 0; i < nInds; i++) {
            musicGraphs.add(MusicGraph.constructFromIntArray(pop.get(i), i, this.g.fetchID()));
        }
    }


    public static void main(String args[]) {
        PApplet.main(new String[] {"--present", "NotePanel"});
    }
}