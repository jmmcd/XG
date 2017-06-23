import static java.lang.Math.abs;
import static java.lang.Math.cos;
import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.tanh;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

public class MusicNode implements Serializable{
    int id;
    String function;
    public float output;
    
    // used in GUI
    //TODO modify to use DoublePair?? No.
    private Double x;
    private Double y;
    
    // used only for accumulator nodes
    float accum;

    // used only by output nodes
    float activity;

    // used only for delay nodes
    float [][] history;
    int historyIdx;

    // used only for output nodes.
    int note;
    int velocity;

    static int maxArgCount = 3;
    private static final double nullMarkerDouble = 999999999.0;
    private static final Integer nullMarkerInt = 999999999;

    public MusicNode(int _id, String _function) {
        id = _id;
        function = _function;
        accum = 0.0f;
        activity = 0.0f;
        history = new float[100][maxArgCount];
        historyIdx = 0;
    }

    /** Custom serialization */
    private void writeObject(ObjectOutputStream out) throws IOException {    	
    	out.writeInt(id);
    	out.writeUTF(function);
    	out.writeFloat(output);
    	if (x == null) {
    		x = nullMarkerDouble;
    	}
    	out.writeDouble(x);
    	if (y == null) {
    		y = nullMarkerDouble;
    	}
    	out.writeDouble(y);
    	out.writeFloat(accum);
    	out.writeFloat(activity);
    	out.writeInt(historyIdx);
    	out.writeInt(note);
    	out.writeInt(velocity);
    	for (int outer = 0; outer < 100; outer++) {
    		for (int inner = 0; inner < maxArgCount; inner++) {
    			out.writeFloat(history[outer][inner]);
    		}
    	}
    }

    /** Custom serialization */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    	id = in.readInt();
    	function = in.readUTF();
    	output = in.readFloat();
    	x = in.readDouble();
    	if (x == nullMarkerDouble) {
    		x = null;
    	}
    	y = in.readDouble();
    	if (y == nullMarkerDouble) {
    		y = null;
    	}
    	accum = in.readFloat();
    	activity = in.readFloat();
    	historyIdx = in.readInt();
    	note = in.readInt();
    	velocity = in.readInt();
    	// convert back to a float[][]
    	history = new float[100][maxArgCount];
    	for (int outer = 0; outer < 100; outer++) {
    		for (int inner = 0; inner < maxArgCount; inner++) {
    			history[outer][inner] = in.readFloat();
    		}
    	}
    }
    
    /**
     * Calculate scaled x value
     * @param scaleFactor self-explanatory
     * @return
     */
    public Double getX() {
    	return this.x;
    }
    
    /**
     * Calculate scaled y value
     * @param scaleFactor self-explanatory
     * @return
     */
    public Double getY() {
    	return this.y;
    }
    
    /**
     * Set the node's x value
     * @param newX A double representing the new x value
     */
    public void setX(Double newX) {
    	this.x = newX;
    }

    /**
     * Set the node's y value
     * @param newY A double representing the new y value
     */
    public void setY(Double newY) {
    	this.y = newY;
    }
    
    void run(float[] args, float[] times, float[] curves) {
        // System.out.println("now running node " + this);
        // for (float arg: args) {
        //     System.out.println("input " + arg);
        // }
        for (int i = 0; i < maxArgCount; i++) {
            history[historyIdx++][i] = curves[i];
            if (historyIdx >= 100) {
                historyIdx = 0;
            };

        }
        output = runInternal(args, times, curves);
        // System.out.println("output = " + output);
    }

    float runInternal(float[] args, float[] times, float[] curves) {
        if (function.equals("sin")) {
            return (float) sin(mean(args));
        } else if (function.equals("cos")) {
            return (float) cos(mean(args));
        } else if (function.equals("sin2")) {
            return (float) (args[0] * sin(times[0] * args[1]));
        } else if (function.equals("log")) {
            return (float) log(0.00001f + abs(mean(args)));
        } else if (function.equals("exp")) {
            return (float) exp(mean(args));
        } else if (function.equals("+")) {
            return (float) sum(args);
        } else if (function.equals("*")) {
            return (float) product(args);
        } else if (function.equals("-")) {
            return (float) args[0] - args[1];
        } else if (function.equals("/")) {
            return (float) protectedDivide(args);
        } else if (function.equals("unary-")) {
            return (float) -args[0];
        } else if (function.equals("mod")) {
            return (float) protectedMod(args);
        } else if (function.equals("delta")) {
            return (float) delta(args);
        } else if (function.equals("max")) {
            return (float) max(args);
        } else if (function.equals("output")) {
            return (float) output(args);
        } else if (function.equals("bar")) {
            return (float) times[0];
        } else if (function.equals("beat")) {
            return (float) times[1];
        } else if (function.equals("edge")) {
            return (float) edge(args);
        } else if (function.equals("x")) {
            return (float) curves[0];
        } else if (function.equals("y")) {
            return (float) curves[1];
        } else if (function.equals("z")) {
            return (float) curves[2];
        } else if (function.equals("branch")) {
            return (float) branch(args);
        } else {
            // a constant
            return (float) Float.parseFloat(function);
        }
    }

    public float output(float[] args) {
        float activityIn = abs(args[0]);
        activity *= 0.25f;
        activity += activityIn;
        float threshold = 1.5f;
        float value = args[1];
        // System.out.println("output: activity = " + activity);
        if (activity < 0.05f * threshold) {
            // System.out.println("note-off");
            // note-off
            velocity = 0;
        } else if (activity < threshold) {
            // System.out.println("hold");
            // hold, ie do nothing
            velocity = -1;
        } else {
            // translate to a midi value
            // System.out.println("note-on");
            velocity = (int) (50.0f + 50.0f * log(1.0f + activity - threshold));
            activity *= 0.05f;
            if (velocity > 127) {
                velocity = 127;
            }
            note = midiMap((int) (18 + 32 * 0.5f * (1.0 + tanh(value))));
        }
        return activity - 2 * threshold;
    }

    public float branch(float[] args) {
        float threshold = 0.5f;
        if (args[0] > threshold) {
            return args[1];
        } else {
            return -args[1];
        }
    }

    public float max(float[] args) {
        float retval = -99999.9f;
        for (float arg: args) {
            if (arg > retval) {
                retval = arg;
            }
        }
        return retval;
    }

    public int midiMap(int in) {
        int n = naturalMinor(in);
        // System.out.println("out: " + n);
        return n;
    }

    public int naturalMinor(int in) {
        int octave = in / 7;
        int chroma = in % 7;
        int retval = octave * 12;
        // System.out.println("in = " + in + "; octave = " + octave + " chroma = " + chroma);

        switch (chroma) {
        case 0: return retval + 0;
        case 1: return retval + 2;
        case 2: return retval + 3;
        case 3: return retval + 5;
        case 4: return retval + 7;
        case 5: return retval + 8;
        case 6: return retval + 10;
        default:
            System.out.println("Error, in = " + in + "; octave = " + octave + " chroma = " + chroma);
            return 0;
        }
    }

    public float protectedDivide(float[] args) {
        if (abs(args[1]) < 0.000001) {
            return args[0];
        } else {
            return args[0] / args[1];
        }
    }

    public float protectedMod(float[] args) {
        if (abs(args[1]) < 0.000001) {
            return args[0];
        } else {
            return args[0] % args[1];
        }
    }

    public float delta(float[] args) {
        float threshold = 0.1f;
        if (abs(args[0] - args[1]) < threshold) {
            return 1.0f;
        } else {
            return 0.0f;
        }
    }

    public float edge(float[] args) {
        float x = args[0];
        if (x < 0.0f || x > 10.0f) {
            return 0.0f;
        } else {
            return (float) pow(1.0f - x / 10.0f, 5.0f);
        }
    }

    public float mean(float[] args) {
        return sum(args) / (float) args.length;
    }

    public float sum(float[] args) {
        float retval = 0.0f;
        for (float arg: args) {
            retval += arg;
        }
        return retval;
    }

    public float product(float[] args) {
        float retval = 0.0f;
        for (float arg: args) {
            retval *= arg;
        }
        return retval;
    }

    public String toString() {
        return id + ":" + function;
    }
}
