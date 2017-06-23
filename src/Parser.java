import java.io.*;

/**
 * Given the filepath of a MusicGraph in dot format, convert and save to
 * graphviz
 * 
 * @author dylan
 * 
 */
public class Parser {
	Integer i = 0;
	String defaultFileName = "dotFile" + i.toString() + ".graphviz"; // default
																		// name
																		// for
																		// dot
	// export
	// used by toGraphViz()
	String dotFilePath; // filepath of the DOT file
	String gvizFilePath; // filepath of the Graphviz output
	String gvizCommandBase1 = "dot -Kdot -Tplain ";
	String gvizCommandBase2 = " > ";
	String gvizCommand;
	Process p;

	/**
	 * Constructor
	 * 
	 * @param _filepath
	 *            We'll assume the path being passed in is the path to the dot
	 *            file
	 */
	public Parser(String _filepath) {
		dotFilePath = _filepath;
		gvizFilePath = this.pathManage(dotFilePath);
	}

	/**
	 * Extract the base filepath
	 * 
	 * @param dotpath
	 *            The path to the .dot file ".../dotfile.dot"
	 * @return the graphviz output path
	 */
	public String pathManage(String dotpath) {
		if (!dotpath.endsWith(".dot")) {
			System.out.println("ERROR: File name should end with \".dot\": "
					+ dotpath);
			assert false;
		}
		return dotpath.substring(0, dotpath.length() - 3) + "graphviz";
	}

	/**
	 * Convert the dot file to graphviz format
	 * @return A string containing the graphviz formatted text
	 */
	public String toGraphViz() {
		String text = null;
		String nextline = "";
		// construct the gvizCommand
		gvizCommand = gvizCommandBase1 + dotFilePath;
		// run it
		try {
			p = Runtime.getRuntime().exec(gvizCommand);
			// result
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
			nextline = stdInput.readLine();
			if (nextline == null) {
				System.out.println("Error in Parser.toGraphViz: stdInput text is null");
				BufferedReader stdErr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				String stdErrText = "";
				String nextLineErr = stdErr.readLine();
				while (nextLineErr != null) {
					stdErrText += nextLineErr;
					nextLineErr = stdErr.readLine();
				}
				System.out.println(stdErrText);
				System.out.println(stdErrText);
			}
			while (nextline != null) {
				text += nextline + "\n";
				nextline = stdInput.readLine();
			}
			// System.out.println(text);
		} catch (IOException e) {
			System.out.println("IOException: filename error or similar:");
			e.printStackTrace();
		}
		System.out.println("Parser.toGraphViz complete.");
		// System.out.print(text);
		return text;
	}

}
