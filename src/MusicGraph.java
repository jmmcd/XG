import static java.lang.Math.min;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jm.music.data.Note;
import jm.music.data.Part;
import jm.music.data.Phrase;
import jm.music.data.Rest;
import jm.music.data.Score;

import org.jgrapht.Graphs;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.IntegerNameProvider;
import org.jgrapht.ext.VertexNameProvider;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedMultigraph;

/**
 * XG (aka MusicGraph)
 */
public class MusicGraph extends DirectedMultigraph<MusicNode, DefaultEdge> implements Cloneable{

	public Integer popIndex; // the index of this graph in the current
								// Population

	public GraphIDGenerator g; // Unique ID generator, common to all MusicGraphs
	public Boolean xy_available; // have xy coords been computed yet?

	public Integer graphID; // a unique ID for the graph instance
	public Boolean removeDOT = true; // a flag that specifies whether to remove
	// generated .dot files in computeXY()

	// the following 3 variables are used by MusicGraph.scaleXY()
	public DoublePair xy; // the graph's xy size without any scaling (calculated
	// in this.importGViz)
	// public IntPair displaySize; // the size (pixels) of the window the graph is
	// currently displayed on.
	// public DoublePair scaleFactor; // the scaling factor associated with that
	// size.

	// public HashMap<Integer[], MusicNode> xyToVertices; // used by GraphEditor

	public Boolean graphAltered;

	public HashMap<Integer, MusicNode> idsToVertices;
	ArrayList<Integer> orderedNodes;
	public static HashMap<String, Integer> arities;
	public static String[] nodeTypes = { "sin", "cos", "+", "-", "*", "/",
			"unary-", "mod", "edge", "max", "branch", "sin2", "delta", "0.1",
			"0.2", "0.5"
	// "0.1", "0.2", "0.5"
	};
	static int nOutputs;
	static int maxArity;
	Phrase[] myPhrases;

	static String absPath;

	org.jgrapht.alg.CycleDetector<MusicNode, DefaultEdge> cycleDetector;

	public MusicGraph(GraphIDGenerator g) {
		this();
		//TODO move this into VariGA to allow overwriting genomes?
		this.graphID = g.fetchID();
	}

	public MusicGraph copy() {
		ObjectCloner cloner = new ObjectCloner();
		// get a new MusicGraph
		MusicGraph mg = new MusicGraph();
		try {
			// vertices
			ArrayList<MusicNode> vertices = new ArrayList<MusicNode>();
			for (MusicNode n : this.vertexSet())
				vertices.add(n);
			ArrayList<MusicNode> vertices2 = (ArrayList<MusicNode>) cloner.deepCopy(vertices);
			for (MusicNode n : vertices2)
				mg.addVertex(n);

			// idsToVertices
			mg.idsToVertices = new HashMap<Integer, MusicNode>();
			for (MusicNode n : mg.vertexSet()) {
				mg.idsToVertices.put(n.id, n);
			}

			// edges
			ArrayList<DefaultEdge> edges = new ArrayList<DefaultEdge>();
			for (DefaultEdge e : this.edgeSet())
				edges.add(e);
			ArrayList<DefaultEdge> edges2 = (ArrayList<DefaultEdge>) cloner.deepCopy(edges);

			for (DefaultEdge e : edges2) {
				MusicNode source = this.getEdgeSource(e);
				MusicNode target = this.getEdgeTarget(e);
				// System.out.println(source.toString() + " --> " + target.toString());
				mg.addEdge(source.id, target.id);
			}
			// copy the rest
			mg.popIndex = (Integer) cloner.deepCopy(this.popIndex);
			mg.xy_available = (Boolean) cloner.deepCopy(xy_available);
			mg.graphID = (Integer) cloner.deepCopy(this.graphID);
			mg.removeDOT = (Boolean) cloner.deepCopy(removeDOT);
			mg.xy = (DoublePair) cloner.deepCopy(xy);
			mg.graphAltered = (Boolean) cloner.deepCopy(graphAltered);
			mg.orderedNodes = (ArrayList<Integer>) cloner.deepCopy(orderedNodes);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return mg;
	}

	public MusicGraph() {
		// construct an empty graph and setup variables
		super(DefaultEdge.class);
		idsToVertices = new HashMap<Integer, MusicNode>();
		orderedNodes = new ArrayList<Integer>();
		arities = new HashMap<String, Integer>();
		arities.put("bar", 0);
		arities.put("beat", 0);
		arities.put("x", 0);
		arities.put("y", 0);
		arities.put("z", 0);
		arities.put("sin", 1);
		arities.put("cos", 1);
		arities.put("log", 1);
		arities.put("exp", 1);
		arities.put("sin2", 2);
		arities.put("+", 2);
		arities.put("*", 2);
		arities.put("-", 2);
		arities.put("/", 2);
		arities.put("unary-", 1);
		arities.put("mod", 2);
		arities.put("delta", 2);
		arities.put("max", 2);
		arities.put("branch", 2);
		arities.put("output", 2);
		arities.put("edge", 1);
		arities.put("0.1", 0);
		arities.put("0.2", 0);
		arities.put("0.5", 0);

		maxArity = 0;
		for (int arity : arities.values()) {
			if (arity > maxArity) {
				maxArity = arity;
			}
		}
		nOutputs = 3;

		// construct orderedNodes
		sort();

		xy_available = false; // xy coords have not yet been computed
		xy = null;

		// make sure the absPath is set
		if (absPath == null) {
			File current = new File(".");
			absPath = current.getAbsolutePath();
			absPath = absPath.substring(0, absPath.length()-1);
			System.out.println(absPath);
		}
	}

	// does this graph contain any cycles?
	// other things can be added here if necessary
	// returns true if no cycles, else false
	public boolean verify() {
		this.cycleDetector = new org.jgrapht.alg.CycleDetector<MusicNode, DefaultEdge>(
				this);
		if (this.cycleDetector.detectCycles()) {
			System.out.println("Failed cycle detection");
			return false;
		}
		// check arities
		MusicNode mn;
		String function;
		Integer arity;
		Set<DefaultEdge> edgesOf;
		Integer inputs;
		Integer outputs;
		for (int i : idsToVertices.keySet()) {
			inputs = 0;
			outputs = 0;
			mn = this.idsToVertices.get(i);
			// System.out.println("Node found.");
			// System.out.println("Node " + mn.id);
			function = mn.function;
			// if (function == null) {
			//	 System.out.println("function is null");
			// }
			// System.out.println("Node " + mn.id + ": \'" + function + "\'");
			// System.out.println(MusicGraph.arities.size());
			arity = MusicGraph.arities.get(function);
			edgesOf = this.edgesOf(mn);
			for (DefaultEdge e : edgesOf) {
				if (this.getEdgeTarget(e).id == mn.id) {
					inputs++;
				} else if (this.getEdgeSource(e).id == mn.id) {
					outputs++;
				} else {
					System.out.println("ERROR in MusicGraph.verify: edge " + e
							+ " not connected to node " + mn.id);
					return false;
				}
			}
			if (!(arity == inputs)) {
				System.out.println("Node " + mn.id + " has invalid arity: expected " + arity
						+ " but was " + inputs + " ( for function "
						+ mn.function + ").");
				return false;
			}
			// don't need to check the number of outputs
		}
		return true;
	}

	/**
	 * Computes the x y coords of each MusicNode (used by GraphEditorGUI)
	 */
	public void computeXY() {
		BufferedReader r = null;
		Parser p;
		String filepathDOT = "";
		// String filepathGVIZ = "";

		// 1) compute the name for the .dot output file
		filepathDOT = this.absPath + "data/tmp/tmp-graph" + graphID.toString() + ".dot";
		// 2) get .dot output with writeEPS
		writeEPS(filepathDOT);
		// 3) use Parser to parse from .dot to .graphviz -- get path for that
		// file
		p = new Parser(filepathDOT);
		String gVizText = p.toGraphViz();
		if (gVizText == null) {
			System.out.println("Error in MusicGraph.computeXY: gVizText is null.");
		}
		// 3.1) clean up DOT temp file after use
		if (removeDOT) {
			removeFile(filepathDOT);
		}
		// 4) use importGViz to apply xy coords to nodes
		importGViz(gVizText);
		// finally, update xy_available and xy
		xy_available = true;
		// done
	}

	/**
	 * Updates scaleFactor and displaySize by the appropriate amount
	 *
	 * @param windowX
	 *            X size (in pixels) of the screen the graph will be displayed
	 *            on.
	 * @param windowY
	 *            Y size
	 */
	public void scaleXY(Integer windowX, Integer windowY) {
		Double xr = (windowX - 40) / this.xy.getFirst(); // x ratio (the +40 is
		// so that labels on
		// the right edge
		// aren't cut off)
		Double yr = windowY / this.xy.getSecond(); // y ratio
		// scaleFactor.setFirst(xr);
		// scaleFactor.setSecond(yr);

		// scale all nodes
		Integer[] gNodes = this.getNodes();
		for (Integer nodeID : gNodes) {
			// node = g.idsToVertices.get(nodeID);
			// nodex = node.getX(g.scaleFactor.getFirst());
			// nodey = node.getY(g.scaleFactor.getSecond());
			MusicNode node = this.idsToVertices.get(nodeID);
			Double scaledX = node.getX() * xr;
			Double scaledY = node.getY() * yr;
			// for debugging no-scaling bug
			if ((scaledX < 8.0) || (scaledY < 8.0)) {
				System.out.println("Error: node \"" + nodeID
						+ "\" has scaled x value of \"" + scaledX
						+ "\" and normal x value of \"" + node.getX() + "\"");
			}
			this.idsToVertices.get(nodeID).setX(scaledX);
			this.idsToVertices.get(nodeID).setY(scaledY);
		}
	}

	/**
	 * Remove a file
	 */
	public void removeFile(String filepathDOT) {
		File toRemove = new File(filepathDOT);
		toRemove.delete();
	}

	/**
	 * Convert a string to a float
	 *
	 * @param s
	 * @return
	 */
	public static Double toDouble(String s) {
		try {
			double d = Double.valueOf(s.trim()).doubleValue();
			// System.out.println("double d = " + d);
			return d;
		} catch (NumberFormatException nfe) {
			System.out.println("NumberFormatException: " + nfe.getMessage());
			nfe.printStackTrace();
			return null;
		}
	}

	/**
	 * Imports x/y node coords from graphviz output (generated by Parser)
	 *
	 * @param gVizText
	 *            Contains graphviz output
	 */
	public void importGViz(String text) {
		// FileReader f;
		// BufferedReader r;
		NewlineIterator gVizText = new NewlineIterator(text);
		String firstline = "!FIRST!\n!LINE!"; // random string (with newlines)
		String nextline = firstline;
		String origline;
		Boolean isfirstline = true;

		// line counter -- effectively, the node id
		// Integer i = 0;
		Integer nextid;
		Double nextx;
		Double nexty;
		nextline = gVizText.readLine();
		origline = nextline;
		String[] words;
		while (!nextline.equals(null)) {
			if (isfirstline) {
				// get graph size
				words = nextline.split("\\s+");
				this.xy = new DoublePair(toDouble(words[2]), toDouble(words[3]));
				// get second line
				nextline = gVizText.readLine();
				// do not increment i
				isfirstline = false;
				continue;
			} // otherwise, must be a valid line
			if (nextline.startsWith("node ")) {
				// advance past "node "
				origline = nextline;
				words = nextline.split("\\s+");
				// get id number
				// nextid = i;
				// FIXME: should we check against imported value
				nextid = Integer.parseInt(words[1]);
				// get x value
				nextx = toDouble(words[2]); // 3rd entry
				// get y value
				nexty = toDouble(words[3]); // 4th entry
				// done -- next line
				nextline = gVizText.readLine();
				// i += 1;
				// add the x/y vals to the correct node
				try {
					addNodeXY(nextid, nextx, nexty);
				} catch (NoSuchNodeException e) {
					e.printStackTrace();
				}
				// next line
				continue;
			}
			// otherwise, starts with "edge" -- ignore
			System.out.println("Ignoring \"" + nextline + "\".");
			break;
		}
	}

	/**
	 * Given a node id and associated x/y pair, add the x/y pair to the node
	 *
	 * @param nodeid
	 * @param nodex
	 * @param nodey
	 * @throws NoSuchNodeException
	 */
	public void addNodeXY(Integer nodeid, Double nodex, Double nodey)
			throws NoSuchNodeException {
		// 1) find the node in the MusicGraph's hashtbl
		MusicNode nd = idsToVertices.get(nodeid);
		// System.out.println(nodeid);
		// System.out.println(idsToVertices.containsKey(nodeid));
		// System.out.println(nd.id);
		if (nd == null) {
			throw new NoSuchNodeException(nodeid, nodex, nodey);
		}
		// 2) apply the changes
		idsToVertices.get(nodeid).setX(nodex);
		idsToVertices.get(nodeid).setY(nodey);
		// 3) update this.xyToVertices
		// this.xyToVertices.put(new DoublePair(nodex, nodey), null);
	}

	public static MusicGraph constructFromIntArray(ArrayList<Integer> _genes, Integer _popIndex, Integer _id){
		MusicGraph mg = constructFromIntArray(_genes, _popIndex);
		mg.graphID = _id;
		return mg;
	}


	public static MusicGraph constructFromIntArray(ArrayList<Integer> _genes,
			Integer _popIndex) {
		MusicGraph mg = constructFromIntArray(_genes);
		mg.popIndex = _popIndex;
		return mg;
	}

	/**
	 * Performs the translation from genome to MusicGraph
	 *
	 * @param _genes
	 * @return
	 */
	public static MusicGraph constructFromIntArray(ArrayList<Integer> _genes) {
		ArrayList<Integer> genes = _genes;
		MusicGraph mg = new MusicGraph();

		// The genome is effectively divided into nNodes sections,
		// each composed of maxArity + 1 genes. in addition to the
		// five hardcoded nodes (bar, beat, x, y, z), there will be
		// nNodes in total, where each node requires maxArity + 1
		// genes: 1 to choose nodetype, and maxArity to choose the
		// inputs. when a node (eg sin) has lower arity than maxArity,
		// we just discard the extra genes: this means each choice has
		// the same semantics after crossover/mutation.

		int nGenesPerNode = maxArity + 1;
		int nNodes = genes.size() / nGenesPerNode;
		int nGenesRequiredToComplete = nOutputs * nGenesPerNode;
		if (genes.size() < nGenesRequiredToComplete) {
			System.out.println("Error: insufficient number of genes received: "
					+ genes.size());
			System.exit(1);
		}

		// System.out.println("starting mapping...");

		// Add five hardcoded nodes.
		mg.addVertex("bar");
		mg.addVertex("beat");
		mg.addVertex("x");
		mg.addVertex("y");
		mg.addVertex("z");

		// use the input genes to add nodes and edges.
		for (int nodeidx = 0; nodeidx < nNodes; nodeidx++) {
			String nodeType;

			// add a node
			if (nodeidx >= nNodes - nOutputs) {
				nodeType = "output";
			} else {
				int geneidx = nodeidx * nGenesPerNode;
				int gene = genes.get(geneidx);
				nodeType = nodeTypes[gene % nodeTypes.length];
			}

			// We avoid connecting an output to an output. Therefore
			// eligible source-nodes for incident edges include all
			// existing non-output nodes. We calculate this number
			// *before* adding the new node.
			int eligibleNodes = min(mg.nodeCount(), nNodes - nOutputs);

			int id = mg.addVertex(nodeType);
			// System.out.println("added node " + nodeType);

			// Add some edges incident to that node.
			for (int i = 0; i < arities.get(nodeType); i++) {
				// offset by 1, since we used 0 to choose nodeType above.
				int geneidx = nodeidx * nGenesPerNode + i + 1;
				int gene = genes.get(geneidx);
				int inputId = gene % eligibleNodes;
				mg.addEdge(inputId, id);
				// System.out.println("added edge from " + inputId);
			}
		}

		mg.sort();
		return mg;
	}

	/**
	 * Performs the reverse translation from MusicGraph to genome.
	 *
	 * @return the genome
	 * @throws NoSuchFunctionException
	 */
	public ArrayList<Integer> reverseMap() throws NoSuchFunctionException {
		ArrayList<Integer> genes = new ArrayList<Integer>();
		MusicNode node;
		Integer nodeType;
		Integer arity;
		Set<DefaultEdge> edges;
		ArrayList<Integer> idBuffer = new ArrayList<Integer>();

		// first, consolidate holes -- work backwards
		// we assume an ordering of the form
		// [(5 inputs) node node node ... (3 outputs) node node node ... ]
		Integer numNodes = this.nodeCount();
		Integer n = numNodes - 1;
		Boolean allHolesFound = false;
		Integer holeOffset = 0;
		while (n >= 0) {
			node = idsToVertices.get(n);
			if (node == null) {
				// see my note on threading in MusicGraph.verify
				// this is a dirty hack, but it works
				n--;
				continue;
			}
			if ((node.function == "output") && !allHolesFound) {
				allHolesFound = true;
				holeOffset = numNodes - n - 1;
				node.id = node.id + holeOffset;
				n--;
				continue;
			} else if (!allHolesFound) { // nodes after "output" nodes
				node.id = node.id - holeOffset;
				n--;
			} else { // must be nodes before 3 "output" nodes
				break;
			}
		}
		// reconstruct idsToVertices map
		idsToVertices = new HashMap<Integer, MusicNode>();
		for (MusicNode nd : this.vertexSet()) {
			idsToVertices.put(nd.id, nd);
		}

		// now that the nodes are ordered [(5 inputs) node node node node ... (3
		// outputs)]
		// the main bit:
		for (Integer nd : this.idsToVertices.keySet()) {
			// add the first of 3 genes
			node = this.idsToVertices.get(nd);
			if ((node.function.equals("beat")) || (node.function.equals("bar"))
					|| (node.function.equals("x")) || (node.function.equals("y"))
					|| (node.function.equals("z"))) {
				// skip input nodes
				continue;
			}
			// skip the first 5 (backup)
			if (nd < 5) {
				continue;
			}
			if (node.function.equals("output")) {
				// TODO handle output nodes
				continue;
			}
			nodeType = getNodeTypeInt(node.function);
			if (nodeType == -1) {
				throw new NoSuchFunctionException(node.id, node.function);
			}
			genes.add(nodeType);
			// add the second/third genes
			edges = this.edgesOf(node);
			arity = arities.get(node.function);
			idBuffer = new ArrayList<Integer>();
			for (int i = 0; i < arity; i++) {
				for (DefaultEdge edge : edges) {
					if ((this.getEdgeTarget(edge).id == node.id)
							&& !(idBuffer.contains(this.getEdgeTarget(edge).id))) { // must
																					// be
																					// a
																					// previously
																					// unseen
																					// input
						genes.add(this
								.convertToNewID(this.getEdgeSource(edge).id)); // TODO
																				// check
																				// for
																				// null
																				// return
																				// from
																				// convertToNewID
						idBuffer.add(this.getEdgeTarget(edge).id);
					}
				}
			}
			// fill in holes if arity is less than maxArity
			if (arity < maxArity) {
				for (int j = 0; j + arity < maxArity; j++) {
					genes.add(0);
				}
			}
		}
		return genes;
	}

	/**
	 * Fills holes/jumps in node ID order so that no false references are made
	 * in reverse mapping.
	 */
	public Integer convertToNewID(Integer oldID) {
		return this.orderedNodes.indexOf(oldID);
	}

	/**
	 * A helper function for reverseMap (above)
	 *
	 * @param nodeType
	 *            e.g. "sin" or "+"
	 * @return the corresponding integer value, as defined by the ordering of
	 *         MusicGraph.nodeTypes. Will return -1 if invalid.
	 */
	public static int getNodeTypeInt(String nodeType) {
		for (Integer nt = 0; nt < nodeTypes.length; nt++) {
			if (nodeType.equals(nodeTypes[nt])) {
				return nt;
			}
		}
		return -1;
	}

	public int getUniqueID() {
		int i = 0;
		while (idsToVertices.get(i) != null) {
			i++;
		}
		return i;
	}

	public void sort() {
		orderedNodes = new ArrayList<Integer>();
		// we don't need a proper topological sort, which I seem to be
		// failing at anyway, because the node-adding process only
		// makes connections from existing nodes.
		for (int i = 0; i < nodeCount(); i++) {
			orderedNodes.add(i);
		}
	}

	// adds the new ID to the appropriate location
	public void addToOrderedNodes(Integer newNodeID) {
		Integer index = 0;
		while (index < orderedNodes.size()) {
			Integer nodeID = orderedNodes.get(index);
			if (newNodeID == nodeID) {
				System.out.println("Error in MusicGraph.addToOrderedNodes: attempting to make a duplicate entry: " + newNodeID);
				return;
			}
			if (newNodeID > nodeID) {
				orderedNodes.add(index, newNodeID);
				return;
			}
			index++;
		}
	}

	public int[][] run(float[] times, float[] curves) {
		// retval will be note-velocity pairs
		int[][] retval = new int[2][nOutputs];
		int o = 0;
		// System.out.println("starting run");
		for (int i : orderedNodes) {
			// System.out.println("MusicGraph.run:");
			// System.out.println("Node " + i + ":");
			MusicNode mn = idsToVertices.get(i);
			// System.out.println(mn.toString());
			List<MusicNode> predecessors = Graphs.predecessorListOf(this, mn);
			for (int p = 0; p < predecessors.size(); p++) {
				if (!(this.orderedNodes.contains(predecessors.get(p).id))) {
					System.out.println("Error in MusicGraph.run: no such node "
							+ predecessors.get(p));
				}
			}
			// FIXME sort by id?
			float[] predecessorValues = new float[predecessors.size()];
			for (int p = 0; p < predecessors.size(); p++) {
				MusicNode pred = predecessors.get(p);
				predecessorValues[p] = pred.output;
			}
			mn.run(predecessorValues, times, curves);
			if (mn.function.equals("output")) {
				retval[0][o] = mn.note;
				retval[1][o++] = mn.velocity;
			}
		}
		return retval;
	}

	public int addVertex(String _function) {
		int id = getUniqueID();
		MusicNode mn = new MusicNode(id, _function);
		addVertex(mn);
		idsToVertices.put(id, mn);
		addToOrderedNodes(id);
		return id;
	}

	/**
	 * Removes the specified node, if the node currently exists
	 *
	 * @param node
	 */
	public void removeNode(Integer node) {
		if ((node != null) && (node != -1)) {
			if (idsToVertices.containsKey(node)) {
				// remove edges
				Set<DefaultEdge> nodeEdges = this.edgesOf(idsToVertices
						.get(node));
				MusicNode source;
				MusicNode target;
				for (DefaultEdge edge : nodeEdges) {
					source = this.getEdgeSource(edge);
					target = this.getEdgeTarget(edge);
					this.removeEdge(source, target);
					// remove the node itself
					removeVertex(idsToVertices.get(node));
					// reminder for writing ADD
					// this.editor.selectedNode = null;
					System.out.println("Removed edge from " + source.id
							+ " to " + target.id + "internally.");
				}
				this.removeVertex(idsToVertices.get(node));
				idsToVertices.remove(node);
				orderedNodes.remove(node);
				System.out.println("Removed node " + node + "internally.");
			}
		}
	}

	/**
	 * Add an edge between two nodes via nodeIDs
	 *
	 * @param a
	 *            The id of node a
	 * @param b
	 *            The id of node b
	 * @return the edge that has been added.
	 */
	public DefaultEdge addEdge(int a, int b) {
		return addEdge(idsToVertices.get(a), idsToVertices.get(b));
	}

	/**
	 * Remove an edge between two nodes via nodeIDs
	 *
	 * @param a
	 *            The id of node a
	 * @param b
	 *            The id of node b
	 * @return the edge that has been removed, or null if no such edge existed.
	 */
	public DefaultEdge removeEdge(int a, int b) {
		return removeEdge(idsToVertices.get(a), idsToVertices.get(b));
	}

	public int nodeCount() {
		return vertexSet().size();
	}

	// Temporary stuff: refactor.
	public void writeMIDI(String xyzFilename) {
		String midiFilename = xyzFilename + ".midi";
		System.out.println("Writing MIDI to " + midiFilename + " from graph:");
		System.out.println(this);
		runFromData(getDataFromFile(xyzFilename));
		Score score = new Score();
		for (int i = 0; i < nOutputs; i++) {
			score.add(new Part(myPhrases[i]));
		}
		jm.util.Write.midi(score, midiFilename);
	}

	// This little class is used to provide the names for vertices in
	// DOT output. FIXME set this to make coloured inputs/outputs.
	public class LabelProvider implements VertexNameProvider<MusicNode> {
		public String getVertexName(MusicNode vertex) {
			return vertex.function;
		}
	}


	public void writeEPS(String filename) {
		// System.out.println("Writing to dot");
		DOTExporter<MusicNode, DefaultEdge> dot = new DOTExporter<MusicNode, DefaultEdge>(new MusicGraphIntegerNameProvider(),
				new LabelProvider(), null);
		try {
			PrintWriter out = new PrintWriter(filename);
			dot.export(out, this);
			System.out.print("");
		} catch (Exception e) {
			System.err
					.println("Error while trying to write graph to a dotfile."
							+ e + "\n");
			e.printStackTrace();
		}
	}

	// added by dylan, feb 11 -- pipes text to a BufferedReader instead of a
	// file
	// for now, ignore
	public BufferedReader writeEPS_no_file() {
		// System.out.println("Writing to dot");
		DOTExporter<MusicNode, DefaultEdge> dot = new DOTExporter<MusicNode, DefaultEdge>(new IntegerNameProvider<MusicNode>(),
				new LabelProvider(), null);
		PipedReader pr = null;
		try {
			PipedWriter pw = new PipedWriter();
			pr = new PipedReader();
			pw.connect(pr);
			dot.export(pw, this);
		} catch (Exception e) {
			System.err
					.println("Error while trying to write graph to a dotfile.\n"
							+ e);
		}
		return new BufferedReader(pr);
	}

	// Given an array of time and curve data, calculate an array of
	// jMusic Phrases (more or less MIDI format)
	public void runFromData(float[][] data) {

		myPhrases = new Phrase[nOutputs];
		// each run gives us nOutputs results, composed of a note and
		// a velocity, and we run data.length times.
		int[][][] results = new int[nOutputs][data.length][2];
		for (int i = 0; i < data.length; i++) {
			float t = data[i][0];
			float b = data[i][1];
			float x = data[i][2];
			float y = data[i][3];
			float z = data[i][4];
			float[] curves = { x, y, z };
			float[] times = { t, b };
			int[][] result = run(times, curves);

			for (int j = 0; j < nOutputs; j++) {
				for (int k = 0; k < 2; k++) {
					int r = result[k][j];
					results[j][i][k] = r;
				}
				// if (j == 0) {
				// System.out.println("1st pass: t " + t + " note " +
				// results[j][i][0]
				// + " vol " + results[j][i][1]);
				// }
			}
		}

		// go through data again and save it in a new format: for each
		// note-on, save the duration before next note-on or note-off.
		for (int i = 0; i < nOutputs; i++) {
			myPhrases[i] = new Phrase();

			int currentNote; // -1 for do-nothing
			int currentVolume;
			double quantum = 1.0 / 6.0;
			double currentDuration = 0.0;
			int currentNoteStartTime = 0;
			currentNote = results[i][0][0];
			currentVolume = results[i][0][1];

			// this is a state machine. initial state: no note is
			// playing.
			int state = 1;
			for (int j = 0; j <= data.length; j++) {

				int inputNote, inputVolume;

				if (j == data.length) {
					// hardcode a note-off at the very end.
					inputNote = 0;
					inputVolume = 0;
				} else {
					inputNote = results[i][j][0];
					inputVolume = results[i][j][1];
				}

				switch (state) {
				case 0:
					// a note is playing
					if (inputVolume < 0) {
						// hold: stay in same state; increment currentDuration
						currentDuration += quantum;
					} else if (inputVolume == 0) {
						// note-off: add note and go to state 1
						myPhrases[i].addNote(new Note(currentNote,
								currentDuration, currentVolume));
						currentDuration = quantum; // minimum value
						state = 1;
					} else if (inputNote > 0 && inputVolume > 0) {
						// note-on
						if (false && inputNote == currentNote) {
							// let's regard this as a "hold"
							// FIXME this is if-falsed out for now
							// what is the right solution?
							currentDuration += quantum;
						} else {
							// new note. add note and stay in this state.
							myPhrases[i].addNote(new Note(currentNote,
									currentDuration, currentVolume));
							currentNote = inputNote;
							currentVolume = inputVolume;
							currentDuration = quantum; // minimum value
						}
					}
					break;

				case 1:
					// no note is playing
					if (inputVolume < 0) {
						// hold: stay in same state; increment currentDuration
						currentDuration += quantum;
					} else if (inputVolume == 0) {
						// note-off: stay in same state; increment
						// currentDuration
						currentDuration += quantum;
					} else if (inputNote > 0 && inputVolume > 0) {
						// note-on; add rest and go to state 0
						myPhrases[i].addRest(new Rest(currentDuration));
						currentNote = inputNote;
						currentVolume = inputVolume;
						currentDuration = quantum; // minimum value
						state = 0;
					}

					break;
				}

				// System.out.println("inputNote = " + inputNote +
				// "; inputVolume = " + inputVolume + "; j = " + j +
				// "; length = " + myPhrases[i].getEndTime() / quantum);

			}
		}
	}

	// Given a filename, read an array of time and curve data
	public static float[][] getDataFromFile(String xyzFilename) {
		int nColumns = 0;
		int nRows = 0;

		// First, read the file to determine number of rows and columns
		try {
			FileInputStream fstream = new FileInputStream(xyzFilename);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			// Read file line by line
			while ((strLine = br.readLine()) != null) {
				nRows++;
				String[] values = strLine.split(" ");
				if (values.length > nColumns) {
					nColumns = values.length;
				}
			}
			in.close();
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}

		System.out.println("Data: rows, columns = " + nRows + "; " + nColumns);

		// allocate space for data and re-read file.
		float[][] retval = new float[nRows][nColumns];
		try {
			FileInputStream fstream = new FileInputStream(xyzFilename);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			// Read file line by line
			int i = 0;
			while ((strLine = br.readLine()) != null) {
				// Print the content on the console
				// System.out.println(strLine);
				String[] values = strLine.split(" ");
				if (values.length > 0) {
					for (int j = 0; j < values.length; j++) {
						retval[i][j] = Float.parseFloat(values[j]);
					}
				}
				i++;
			}
			in.close();
		} catch (Exception e) {
			System.err.println("Error when re-reading data: " + e);
		}

		return retval;
	}

	// Calculate just one simple statistic by hand... will probably
	// delete this and associated stuff when the jMusic feature vector
	// is implemented correctly.
	public float variety() {
		ArrayList<ArrayList<Integer>> results = new ArrayList<ArrayList<Integer>>();
		for (int j = 0; j < nOutputs; j++) {
			results.add(new ArrayList<Integer>());
		}

		int i = 0;
		int grid = 4;

		// Run the graph many times for different values of time and
		// control parameters and save the results.
		for (int bar = 0; bar < grid; bar++) {
			for (int beat = 0; beat < grid; beat++) {
				for (int xI = 0; xI < grid; xI++) {
					for (int yI = 0; yI < grid; yI++) {
						for (int zI = 0; zI < grid; zI++) {
							float[] curves = { xI / (float) grid,
									yI / (float) grid, zI / (float) grid };
							float[] times = { bar, beat };
							int[][] result = run(times, curves);
							for (int j = 0; j < nOutputs; j++) {
								// FIXME this only gets the note-variety, not
								// vol
								results.get(j).add(result[0][j]);
							}
						}
					}
				}
			}
		}

		float retval = 0.0f;
		for (int j = 0; j < nOutputs; j++) {
			retval += varietyStat(results.get(j));
		}
		retval /= nOutputs;
		return retval;
	}

	public float varietyStat(ArrayList<Integer> input) {
		// how many distinct values are there?
		HashSet<Integer> set = new HashSet<Integer>(input);
		return set.size() / (float) input.size();
	}

	// A topological sort allows a directed acyclic graph to be
	// executed by executing each node in the order returned by the
	// sort. For some reason the jGraphT topological sort is broken,
	// and I seem to fail at implementing it also, but it turns out
	// not to be needed since I add nodes with incoming edges only and
	// with incrementing ids.
	public ArrayList<Integer> topologicalSort() {
		ArrayList<Integer> done = new ArrayList<Integer>();
		ArrayList<Integer> remaining = new ArrayList<Integer>();
		for (int i = 0; i < nodeCount(); i++) {
			remaining.add(i);
		}
		for (int pass = 0; pass < nodeCount(); pass++) {
			for (int i : remaining) {
				MusicNode mn = idsToVertices.get(i);
				boolean readyToRun = true;
				for (MusicNode pred : Graphs.predecessorListOf(this, mn)) {
					if (done.indexOf(pred.id) == -1) {
						// System.out.println("pred " + pred + "not yet done!");
						readyToRun = false;
					}
				}
				if (readyToRun) {
					// System.out.println("adding " + i);
					done.add(i);
				} else {
					// System.out.println("failed to add " + i);
				}
			}
			remaining.clear();
			for (int i = 0; i < nodeCount(); i++) {
				if (done.indexOf(i) == -1) {
					remaining.add(i);
				}
			}
		}
		if (done.size() != nodeCount()) {
			System.out
					.println("Problem: could not sort topological. Cycle exists.");
			return null;
		}
		return done;
	}

	public static MusicGraph makeTestGraph() {
		String arg = "85, 92, 0, 69, 112, 74, 80, 104, 74, 83, 15, 116, 13, 111, 44, 94, 84, 77, 96, 11, 24, 10, 38, 85, 74, 24, 43, 82, 53, 88, 2, 124, 46, 63, 49, 41, 71, 16, 106, 57, 113, 58, 57, 101, 23";
		return makeTestGraph(arg);
	}

	public static MusicGraph makeTestGraph(String arg) {
		ArrayList<Integer> genes = new ArrayList<Integer>();
		String[] genesStrs = arg.split(",");
		for (int i = 0; i < genesStrs.length; i++) {
			int g = Integer.parseInt(genesStrs[i].trim());
			// System.out.println(g);
			genes.add(g);
		}
		return constructFromIntArray(genes);
	}

	/***
	 * Converts BufferedReader output to a string, line by line
	 *
	 * @param r
	 *            a BufferedReader
	 * @return a string containing all text
	 * */
	public static String readToString(BufferedReader r) {
		String txt = "";
		String line = null;
		try {
			line = r.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		while (line != null) {
			txt += line;
			if (!txt.endsWith("\n")) {
				txt += "\n";
			}
			// get next line
			try {
				line = r.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return txt;
	}

	/**
	 * Get all node ids of the graph. Used by GraphEditor.drawGraph()
	 *
	 * @return an Integer array of the node ids, used to index into
	 *         idsToVertices
	 */
	// TODO finish writing, as of 2011/3/14
	public Integer[] getNodes() {
		Integer[] ids = new Integer[idsToVertices.size()];
		// Integer[] ids2 = (Integer[]) idsToVertices.keySet().toArray();
		Integer i = 0;
		for (Integer nodeID : idsToVertices.keySet()) {
			ids[i] = nodeID;
			i++;
		}
		return ids;
	}

	/**
	 * Retrieves all edges in the graph.
	 *
	 * @return a list of DefaultEdge objects
	 */
	public DefaultEdge[] getEdges() {
		Set<DefaultEdge> edges = this.edgeSet();
		Object[] e = edges.toArray();
		DefaultEdge[] d = new DefaultEdge[edges.size()];
		Integer i = 0;
		for (Object a : e) {
			System.out.println(a.toString());
			d[i] = (DefaultEdge) e[i];
			i++;
		}
		return d;
	}

	/**
	 * Tells if given mouse xy coords are within d pixels of the given
	 * MusicNode's xy coords
	 *
	 * @param mousex
	 * @param mousey
	 * @param d
	 * @return
	 */
	boolean overCircle(Integer mousex, Integer mousey, MusicNode node, Integer r) {
		MusicNode node1 = node;
		Double xval = node1.getX();
		Double yval = node1.getY();
		Integer disX = Math.abs(node.getX().intValue() - mousex);
		Integer disY = Math.abs(node.getY().intValue() - mousey);
		Integer nodex = node.getX().intValue();
		Integer nodey = node.getY().intValue();

		if ((nodex - r <= mousex) && (mousex <= nodex + r)
				&& (nodey - r <= mousey) && (mousey <= nodey + r)) {
			return true;
		} else {
			return false;
		}
	}

	public void main(String[] args) {
		MusicGraph mg;
		if (args.length > 0) {
			mg = makeTestGraph(args[0]);
		} else {
			mg = makeTestGraph();
		}
		mg.writeMIDI("data/abab_2mins.txyz");
		mg.writeEPS("data/test_graph.dot");
		// BufferedReader mgDot = mg.writeEPS("data/test_graph.dot");
		// convert text to string
		// String mgText = readToString(mgDot);
	}

	// two semi-useful exceptions used in MusicGraph

	public class NoSuchNodeException extends Exception {
		public NoSuchNodeException(Integer nodeid, Double nodex, Double nodey) {
			System.out
					.println("Node id: " + nodeid.toString() + "\nNode X: "
							+ nodex.toString() + "\nNode Y: "
							+ nodey.toString() + "\n");
		}
	}

	public class NoSuchFunctionException extends Exception {
		public NoSuchFunctionException(Integer nodeID, String function) {
			System.out.println("Node id: \"" + nodeID.toString()
					+ "\" and function: \"" + function + "\".");
		}
	}

}
