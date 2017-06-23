import java.util.*;
import jm.music.data.*;
import jm.music.tools.*;
import jm.util.Read;
import static java.lang.Math.*;

public class FeatureVector {
    public HashMap<String, Double>  results;
    public static String[] names = jm.music.tools.PhraseAnalysis.featureNames;

    public FeatureVector() {
    }

    public FeatureVector(Phrase p) throws NoteListException, QuantisationException{

        // FIXME this uses 1/12 for duration, and F minor for key.
        // that's what Glass "Opening" gives us.
        Hashtable tmpResults = PhraseAnalysis.
            getAllStatistics(p, (1.0 / 3.0) * (1.0 / 4.0), 5,
                             PhraseAnalysis.MINOR_SCALE);
        results = new HashMap<String, Double>();
        for (Object key: tmpResults.keySet()) {
            // System.out.println("key = " + key);
            String value = (String) tmpResults.get(key);
            if (value.equals(jm.music.tools.PhraseAnalysis.
                             NOTELIST_EXCEPTION_STRING)) {
                throw new NoteListException();
            }
            if (value.equals(jm.music.tools.PhraseAnalysis.
                             QUANTISATION_EXCEPTION_STRING)) {
                throw new QuantisationException();
            }
            results.put((String) key, Double.parseDouble((String) (tmpResults.get(key))));
        }

    }

    public String toString() {
        String retval = "";
        for (int i = 0; i < names.length; i++) {
            retval += (names[i] + ": " + results.get(names[i]) + "\n");
        }
        return retval;
    }

    // A measure of the difference between two phrases, calculated as
    // the Euclidean distance between the two feature vectors. There
    // is no weighting or scaling applied to the various features, so
    // it is possible that some features, if they tend to take on
    // larger values, will have a larger influence on the outcome.
    public double delta(Phrase p) throws NoteListException, QuantisationException {
        return delta(new FeatureVector(p));
    }

    // Alternative version where the other phrase's FeatureVector has
    // already been calculated.
    public double delta(FeatureVector other) {
        double retval = 0.0;
        for (int i = 0; i < names.length; i++) {
            retval += abs(results.get(names[i]) - other.results.get(names[i]));
        }
        retval /= (float) names.length;
        return retval;
    }

    // Fitness Function which targets specific values of jMusic
    // melodic features.
    public static class MIDIFeatureTarget extends VariGA.FitnessFunction {
        float[][] data;
        // FIXME set targets
        FeatureVector[] targetFVs;

        // Constructor for when we have specific target values,
        // presumably. Filename gives structual (xyz) values.
        public MIDIFeatureTarget(String filename) {
            data = MusicGraph.getDataFromFile(filename);
        }

        // Constructor for when the target values are calculated from
        // a MIDI file. First filename gives structural (xyz) values.
        public MIDIFeatureTarget(String filename, String midiFilename) {

            // Read structural data.
            data = MusicGraph.getDataFromFile(filename);

            // Read score and save a feature vector characterising
            // each phrase: count number of phrases.
            Score s = new Score();
            Read.midi(s, midiFilename);
            int i = 0;
            for (Object opart: s.getPartList()) {
                Part part = (Part) opart;
                for (Object ophrase: part.getPhraseList()) {
                    Phrase phrase = (Phrase) ophrase;
                    i++;
                }
            }

            // Allocate targetFVs and populate it.
            targetFVs = new FeatureVector[i];
            i = 0;
            for (Object opart: s.getPartList()) {
                Part part = (Part) opart;
                for (Object ophrase: part.getPhraseList()) {
                    Phrase phrase = (Phrase) ophrase;
                    try {
                        targetFVs[i++] = new FeatureVector(phrase);
                        System.out.println("Glass track " + (i-1));
                        System.out.println(targetFVs[i-1]);
                    } catch (NoteListException e) {
                        System.out.println("Error. Bad input track: " + midiFilename + " gave NoteListException. Exiting.");
                        System.exit(1);
                    } catch (QuantisationException e) {
                        System.out.println("Error. Bad input track: " + midiFilename + " gave QuantisationException. Exiting.");
                        System.exit(1);
                    }
                }
            }
        }

        public float call(VariGA.Individual ind) {

            // Run the individual and save the phrases it makes.
            MusicGraph mg = MusicGraph.constructFromIntArray(ind);
            mg.runFromData(data);
            Phrase[] phrases = mg.myPhrases;

            // sum the deltas between corresponding phrases
            float result = 0.0f;
            try {
                for (int i = 0; i < phrases.length; i++) {
                    // FIXME attempt to make all phrases copy target 1
                    // (triplets)
                    result += targetFVs[i].delta(phrases[i]);
                }
            } catch (NoteListException e) {
                return 0.0f;
            } catch (QuantisationException e) {
                return 0.0f;
            }

            // normalise the result, and take 1-x since VariGA maximises.
            result /= (float) phrases.length;
            return 1.0f - result;
        }
    }

    public static class FeatureTarget extends MIDIFeatureTarget {

        // Constructor for when we have specific target values,
        // presumably. Filename gives structual (xyz) values.
        public FeatureTarget(String filename) {
            super(filename);
            targetFVs = new FeatureVector[3];
            for (int i = 0; i < 3; i++) {
                targetFVs[i] = sensibleTarget();
            }
        }

        public static FeatureVector sensibleTarget() {
            FeatureVector fv = new FeatureVector();
            fv.results = new HashMap<String, Double>();

            for (int i = 0; i < names.length; i++) {
                fv.results.put(names[i], 0.5);
            }
            return fv;
        }

        // call is the same as MIDIFeatureTarget

    }

    public static class NoteLengthMaximiser extends MIDIFeatureTarget {
        public NoteLengthMaximiser(String filename) {
            super(filename);
        }

        public float call(VariGA.Individual ind) {
            // Run the individual and save the phrases it makes.
            MusicGraph mg = MusicGraph.constructFromIntArray(ind);
            mg.runFromData(data);
            Phrase[] phrases = mg.myPhrases;

            float targetLength = 2.5f;
            int noteCount = 0;
            float totalLength = 0.0f;
            float error = 0.0f;
            for (int i = 0; i < phrases.length; i++) {
                for (Object on: phrases[i].getNoteList()) {
                    Note n = (Note) on;
                    if (!n.isRest()) {
                        error += abs(targetLength - n.getDuration());
                        totalLength += n.getDuration();
                        noteCount++;
                    }
                }
            }

            // normalise the result
            float meanLength = totalLength / (1.0f + noteCount);
            error /= (1.0f + noteCount);
            return 10.0f - error;
        }
    }

    public static class MinimiseRepeatedRhythmAndPitchTarget
        extends MIDIFeatureTarget {
        public MinimiseRepeatedRhythmAndPitchTarget(String filename) {
            super(filename);
        }

        public float call(VariGA.Individual ind) {
            // Run the individual and save the phrases it makes.
            MusicGraph mg = MusicGraph.constructFromIntArray(ind);
            mg.runFromData(data);
            Phrase[] phrases = mg.myPhrases;

            float retval = 12.0f;
            try {
                for (int i = 0; i < phrases.length; i++) {
                    Phrase p = phrases[i];
                    FeatureVector fv = new FeatureVector(p);
                    retval -= fv.results.get("20 - Repeated Pitch Patterns Of Three");
                    retval -= fv.results.get("21 - Repeated Pitch Patterns Of Four");
                    retval -= fv.results.get("22 - Repeated Rhythm Patterns Of Three");
                    retval -= fv.results.get("23 - Repeated Rhythm Patterns Of Four");

                    retval += 3.0 * fv.results.get("01 - Pitch Variety");
                    retval += 3.0 * fv.results.get("15 - Rhythmic Variety");

                    double noteDensity = fv.results.get("13 - Note Density");
                    if (noteDensity < 0.2 || noteDensity > 0.9) {
                        retval -= 1.0;
                    }
                }
            } catch (NoteListException e) {
                return 0.0f;
            } catch (QuantisationException e) {
                return 0.0f;
            }

            return retval;
        }
    }

    // This fitness function is intended to avoid obvious mistakes:
    // silent tracks, repeated notes, and many very short notes.
    public static class AntiRobotDevice extends MIDIFeatureTarget {
        public AntiRobotDevice(String filename) {
            super(filename);
        }

        public float call(VariGA.Individual ind) {
            // Run the individual and save the phrases it makes.
            MusicGraph mg = MusicGraph.constructFromIntArray(ind);
            mg.runFromData(data);
            Phrase[] phrases = mg.myPhrases;

            float targetLength = 2.5f;
            float totalLength = 0.0f;
            float error = 0.0f;
            for (int i = 0; i < phrases.length; i++) {
                double beatLength = phrases[i].getBeatLength();
                int noteCount = 0;
                int shortNoteCount = 0;
                int repeatedNoteCount = 0;
                int lastPitch = 60;
                for (Object on: phrases[i].getNoteList()) {
                    Note n = (Note) on;
                    if (!n.isRest()) {
                        error += abs(targetLength - n.getDuration());
                        totalLength += n.getDuration();
                        noteCount++;
                        if (n.getDuration() < 0.25 * 0.34) {
                            shortNoteCount++;
                        }
                        if (n.getPitch() == lastPitch) {
                            repeatedNoteCount++;
                        }
                        lastPitch = n.getPitch();
                    }
                }
                if (noteCount < 4) {
                    return 0;
                }
                if (shortNoteCount > beatLength) {
                    return 0;
                }
                if (repeatedNoteCount > beatLength) {
                    return 0;
                }
            }
            return 1.0f;
        }
    }
}
