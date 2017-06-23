import jm.music.data.*;
import jm.music.tools.*;
import jm.util.*;

/**
 * Analyse the midi file given as first command-line argument.
 */

public final class FileAnalysis {

    public static void main(String[] args){
		new FileAnalysis(args[0]);
	}

	public FileAnalysis(String midiFilename) {
        Score s = new Score();
        Read.midi(s, midiFilename);

        for (Object opart: s.getPartList()) {
            Part part = (Part) opart;
            for (Object ophrase: part.getPhraseList()) {
                Phrase phrase = (Phrase) ophrase;
                // System.out.println("phrase: " + phrase);

                // print all the pitches and durations
                for (int j = 0; j < phrase.getSize(); j++) {
                    Note n = phrase.getNote(j);
                    System.out.println("pitch = " + n.getPitch() + " " + n.getRhythmValue());
                }

                // print feature vector
                int i = 0;
                for (String stat: PhraseAnalysis.
                         getAllStatisticsAsStrings(phrase, (1.0 / 3.0) * (1.0 / 4.0), 5,
                                                   PhraseAnalysis.MINOR_SCALE)) {
                    // this is F minor, with (1/12) as rhythmic
                    // quantum. Suitable for
                    // http://www.glasspages.org/opening.mid
                    System.out.println(PhraseAnalysis.featureNames[i++] + ": " + stat);
                }
            }
        }
    }
}