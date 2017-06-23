import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

import org.jgrapht.graph.DefaultEdge;
import org.junit.Ignore;
import org.junit.Test;

public class MusicGraphTest {

	/**
	 * Simple test to confirm how to access nodes
	 */
	@Test
	@Ignore
	public void printGraphNodes() {
		Integer num = 15;
		MusicGraph m = MusicGraph.makeTestGraph(); // make graph
		Set v = m.vertexSet(); // get vertices
		Class vclass = v.getClass();
		Boolean isempty = v.isEmpty();
		Integer s = v.size();
		Iterator i = v.iterator(); // get an iterator
		Integer nd = m.orderedNodes.get(num);
		String[] ndtypes = m.nodeTypes;
		MusicNode a = m.idsToVertices.get(num); // use idsToVertices map
		Integer aID = a.id;
		String aF = a.function;
		System.out.println("*******************");
		System.out.println(v.toString());
		System.out.println(vclass.toString());
		System.out.println(isempty.toString());
		System.out.println(s);
		System.out.println(i.next().toString());
		System.out.println(nd.toString());
		System.out.println(ndtypes.toString());
		System.out.println("------------");
		System.out.println(a.toString());
		System.out.println(aID.toString());
		System.out.println(aF.toString());
		System.out.println("*******************");
	}

	@Test
	@Ignore
	public void printGraphNodes2() {
		Integer num = 15;
		MusicGraph m = MusicGraph.makeTestGraph(); // make graph
		Iterator i = m.idsToVertices.keySet().iterator();
		MusicNode a = m.idsToVertices.get(num); // use idsToVertices map to get
		// nodes
		Integer aID = a.id;
		String aF = a.function;
		System.out.println("*******************");
		System.out.println(a.toString());
		System.out.println(aID.toString());
		System.out.println(aF.toString());
		System.out.println("*******************");
	}

	/**
	 * Test of system execution from within java (sanity check)
	 */
	@Test
	@Ignore
	public void sysexec() {
		Boolean pass1 = true;
		Process p = null;
		try {
			p = Runtime
					.getRuntime()
					.exec(
							"dot -Kdot -Tplain data/test_graph2.dot > data/test_graph2.graphviz");
			// p = Runtime.getRuntime().exec("dot -Kdot -Tplain");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			pass1 = false;
		}
		assertTrue(pass1);
		// result -- don't need to read
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(p
				.getInputStream()));
		String text = "";
		String nextline = "";
		try {
			nextline = stdInput.readLine();
			while (nextline != null) {
				text += nextline + "\n";
				nextline = stdInput.readLine();
			}
			System.out.println(text);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Test of the method Parser.pathManage
	 */
	@Test
	public void pathManageTest() {
		String p1 = "data/pathOne.dot";
		String expected = "data/pathOne.graphviz";
		Parser p = new Parser(p1);
		String p2 = p.pathManage(p1);
		assertEquals(expected, p2);
		// System.out.println(p2);
	}

	/**
	 * A test of Parser.toGraphViz()
	 */
	@Test
	public void toGraphVizTest() {
		Boolean pass1 = true;
		// test_graph.dot located at data/test_graph.dot
		String dotPath = "data/test_graph_backup.dot";
		String gVizPath = "data/test_graph_backup.graphviz";
		String gVizCommand = "dot -Kdot -Tplain " + dotPath;
		// 1) create a Parser
		Parser p = new Parser(dotPath);
		assertEquals(gVizPath, p.gvizFilePath);
		String output = p.toGraphViz();
		assertEquals(gVizCommand, p.gvizCommand);

		BufferedReader r = null;
		// check that the dotfile exists
		try {
			r = new BufferedReader(new FileReader(dotPath));
		} catch (FileNotFoundException e) {
			System.out.println("Error: dotfile doesn't exist");
			e.printStackTrace();
			pass1 = false;
		}
		assertTrue(pass1);

		// check the first line of the graphviz output
		String line2 = "node ";
		String[] outlines = output.split("\\n");
		System.out.println("*******************************");
		System.out.println(outlines[1]);
		System.out.println("*******************************");
		System.out.println(outlines[0]);
		assertTrue(outlines[1].startsWith(line2));
	}

	/**
	 * A test of MusicGraph.addNodeXY() -- passes
	 */
	@Test
	public void addNodeXYtest() {
		MusicGraph mg = MusicGraph.makeTestGraph();
		assertNotNull(mg.graphID);
		System.out.println("MusicGraph graphID: " + mg.graphID.toString());
		try {
			mg.addNodeXY(3, 1.234, 3.435);
		} catch (MusicGraph.NoSuchNodeException e) {
			System.out.println("NO NODE 3??? Investigate");
			e.printStackTrace();
		}
		// assertEquals(Double.valueOf(1.234),
		// mg.idsToVertices.get(3).getX(mg.scaleFactor.getFirst()));
		// assertEquals(Double.valueOf(3.435),
		// mg.idsToVertices.get(3).getY(mg.scaleFactor.getSecond()));
		System.out.println(mg.idsToVertices.get(3).getX());
		System.out.println(mg.idsToVertices.get(3).getY());
	}

	/**
	 * A test of MusicGraph.computeXY()
	 */
	@Test
	public void simpleGraphParse() {
		// 1) create test graph
		MusicGraph mg = MusicGraph.makeTestGraph();
		assertNotNull(mg.graphID);
		System.out.println("MusicGraph graphID: " + mg.graphID.toString());
		// 2) try to get xy coords
		mg.computeXY();
		// 3) test if the nodes have xy values
		MusicNode n1 = mg.idsToVertices.get(3);
		System.out.println("MusicNode nodeID: " + n1.id);
		assertNotNull(n1.getX());
		System.out.println("X: " + n1.getX().toString() + "\nY: "
				+ n1.getY().toString() + "\n");
	}

	/**
	 * A test to establish whether the DOTExporter handles node ids correctly
	 * Conclusion: it doesnt
	 */
	@Test
	@Ignore
	// for now
	public void dotIDTest() {
		MusicGraph m = MusicGraph.makeTestGraph();
		// modify graph
		Integer i = 7;
		MusicNode node = m.idsToVertices.get(i);
		node.id = 30030;
		// run DOTExporter
		String testFileOutput = "data/test_graph_dotIDTest.dot";
		m.writeEPS(testFileOutput);
		// read output
		BufferedReader b = null;
		try {
			b = new BufferedReader(new FileReader(testFileOutput));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		assertNotNull(b);
		String nextline = "FIRST LINE";
		System.out.println("****************************************");
		try {
			nextline = b.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		}
		assertNotNull(nextline);
		while (!nextline.startsWith("}")) {
			System.out.println(nextline);
			assertNotNull(nextline);
			try {
				nextline = b.readLine();
			} catch (IOException e) {
				e.printStackTrace();
				assertTrue(false);
			}
		}
		System.out.println("}");
	}

	/**
	 * Brainstorming for how to write the MusicGraph.getEdges method
	 */
	@Test
	public void getEdgesTest() {
		MusicGraph g = MusicGraph.makeTestGraph();
		Set edges = g.edgeSet();
		Object[] e = edges.toArray();
		DefaultEdge[] d = new DefaultEdge[edges.size()];
		Integer i = 0;
		for (Object a : e) {
			// System.out.println(a.toString());
			d[i] = (DefaultEdge) e[i];
			i++;
		}
		System.out.println("*************************************************");
		for (DefaultEdge b : d) {
			System.out.println(b.toString());
		}
		// get edge ids.
		DefaultEdge edge0 = d[0];
		// edge0.toString() = (11:sin : 19:output)
	}

	/**
	 * Testing methods for GraphEditor.toGraphID()
	 */
	@Test
	public void edgeParseTest() {
		System.out.println("****************-------------*************");
		String text = "(11:sin : 19:output)";
		// String[] text1 = text.split("\\([0-9]+\\:");
		// for (String t : text1) {
		// System.out.println(t);
		// }
		Pattern p1 = Pattern.compile("\\([0-9]+:");
		java.util.regex.Matcher m1 = p1.matcher(text);
		m1.find();
		String match1 = m1.group();
		System.out.println(match1);
	}

	/**
	 * A simpler approach to edges. Didn't work
	 */
	@Ignore
	// Didn't work
	@Test
	public void getEdgesTest2() {
		MusicGraph g = MusicGraph.makeTestGraph();
		Set<DefaultEdge> edges = g.edgeSet();
		DefaultEdge[] d = (DefaultEdge[]) edges.toArray();
		System.out.println("*************************************************");
		for (DefaultEdge b : d) {
			System.out.println(b.toString());
		}
	}

	/**
	 * Another quicker alternative to getEdgesTest -- DOESNT WORK
	 */
	@Test
	@Ignore
	public void getEdgesTest3() {
		MusicGraph g = MusicGraph.makeTestGraph();
		Set<DefaultEdge> edges = g.edgeSet();
		DefaultEdge[] e = (DefaultEdge[]) edges.toArray();
		for (DefaultEdge b : e) {
			System.out.println(b.toString());
		}
	}

	/**
	 * Investigating how to access edges of a graph
	 */
	@Test
	public void edgeTest() {
		MusicGraph g = MusicGraph.makeTestGraph();
		Set<DefaultEdge> edges = g.edgeSet();
		Object[] e = edges.toArray();
		DefaultEdge[] d = new DefaultEdge[edges.size()];
		Integer i = 0;
		for (Object a : e) {
			// System.out.println(a.toString());
			d[i] = (DefaultEdge) e[i];
			i++;
		}

		// create a sample edge, get source and target
		DefaultEdge edge = g.getEdge(g.idsToVertices.get(4), g.idsToVertices
				.get(8));
		MusicNode source = g.getEdgeSource(edge);
		MusicNode target = g.getEdgeTarget(edge);

		// 

	}

	/**
	 * A test to determine if parsing properly loads graph xy dimensions
	 */
	@Test
	public void graphXYtest() {
		MusicGraph g = MusicGraph.makeTestGraph();
		g.computeXY();
		System.out.println("Graph xy values: \"" + g.xy.toString() + "\"");
	}

	/**
	 * Testing rounding via integer casting
	 */
	@Test
	public void intCastTest() {
		Double d1 = 1.234;
		Double d2 = 5.768;
		Double d3 = 9.9999;
		Integer i1 = d1.intValue();
		Integer i2 = d2.intValue();
		Integer i3 = d3.intValue();
		assertEquals((Integer) 1, i1);
		assertEquals((Integer) 5, i2);
		assertEquals((Integer) 9, i3);
	}

	/**
	 * Reverse mapping test: take a genome (where each gene < numGenes/3 =
	 * numNodes), forward-map, reverse-map, compare to original.
	 */
	@Test
	public void reverseMapTest1() {
		ArrayList<Integer> genes1 = new ArrayList<Integer>();
		ArrayList<Integer> genes2 = new ArrayList<Integer>();
		ArrayList<Integer> genes1_mod = new ArrayList<Integer>(); // where each
																	// gene is
																	// (gene mod
																	// numNodes)

		// construct genome
		String rawGenes = "85, 92, 0, 69, 112, 74, 80, 104, 74, 83, 15, 116, 13, 111, 44, 94, 84, 77, 96, 11, 24, 10, 38, 85, 74, 24, 43, 82, 53, 88, 2, 124, 46, 63, 49, 41, 71, 16, 106, 57, 113, 58, 57, 101, 23";
		String[] genesStrs = rawGenes.split(",");
		for (int i = 0; i < genesStrs.length; i++) {
			int g = Integer.parseInt(genesStrs[i].trim());
			System.out.println(g);
			genes1.add(g);
		}

		// construct genes1_mod
		Integer numEligibleNodes = genes1.size() / 3;
		for (Integer gene1 : genes1) {
			genes1_mod.add(gene1 % numEligibleNodes);
		}

		// forward-map
		MusicGraph mg = MusicGraph.constructFromIntArray(genes1);
		// reverse-map
		try {
			genes2 = mg.reverseMap();
		} catch (MusicGraph.NoSuchFunctionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertEquals(genes1.size(), genes2.size());
		assertEquals(genes1_mod.size(), genes1.size());
		assertEquals(genes1_mod.size(), genes2.size());
		assertEquals(genes1_mod, genes2);
		assertFalse(genes1.equals(genes2));
	}

	@Test
	public void reverseTest2() throws MusicGraph.NoSuchFunctionException {
		ArrayList<Integer> genes1 = new ArrayList<Integer>();
		ArrayList<Integer> genes2 = new ArrayList<Integer>();
		ArrayList<Integer> genes1_mod = new ArrayList<Integer>(); // where each
																	// gene is
																	// (gene mod
																	// numNodes)

		// construct genome
		String rawGenes = "85, 92, 0, 69, 112, 74, 80, 104, 74, 83, 15, 116, 13, 111, 44, 94, 84, 77, 96, 11, 24, 10, 38, 85, 74, 24, 43, 82, 53, 88, 2, 124, 46, 63, 49, 41, 71, 16, 106, 57, 113, 58, 57, 101, 23";
		String[] genesStrs = rawGenes.split(",");
		for (int i = 0; i < genesStrs.length; i++) {
			int g = Integer.parseInt(genesStrs[i].trim());
			System.out.println(g);
			genes1.add(g);
		}

		// construct genes1_mod
		Integer numEligibleNodes = genes1.size() / 3;
		for (Integer gene1 : genes1) {
			genes1_mod.add(gene1 % numEligibleNodes);
		}

		// forward-map
		MusicGraph mg = MusicGraph.constructFromIntArray(genes1);

		// first, consolidate holes -- work backwards
		// we assume an ordering of the form
		// [(5 inputs) node node node ... (3 outputs) node node node ... ]
		Integer numNodes = mg.nodeCount();
		Integer n = numNodes - 1;
		Boolean allHolesFound = false;
		Integer holeOffset = 0;
		MusicNode node;
		while (n >= 0) {
			node = mg.idsToVertices.get(n);
			if ((node.function == "output") && !allHolesFound) {
				allHolesFound = true;
				holeOffset = numNodes - n - 1;
				node.id = node.id + holeOffset;
				n -= 1;
				continue;
			} else if (!allHolesFound) { // nodes after "output" nodes
				node.id = node.id - holeOffset;
				n -= 1;
			} else { // must be nodes before 3 "output" nodes
				break;
			}
		}

		// reconstruct idsToVertices map
		mg.idsToVertices = new HashMap<Integer, MusicNode>();
		for (MusicNode nd : mg.vertexSet()) {
			mg.idsToVertices.put(nd.id, nd);
		}

		ArrayList<Integer> genes = new ArrayList<Integer>();
		Integer nodeType;
		Integer arity;
		Set<DefaultEdge> edges;
		ArrayList<Integer> idBuffer = new ArrayList<Integer>();

		// now that the nodes are ordered [(5 inputs) node node node node ... (3
		// outputs)]
		// the main bit:
		for (Integer nd : mg.orderedNodes) {
			// add the first of 3 genes
			node = mg.idsToVertices.get(nd);
			if ((node.function == "beat") || (node.function == "bar")
					|| (node.function == "x") || (node.function == "y")
					|| (node.function == "z")) {
				// skip input nodes
				continue;
			}
			if (node.function == "output") {
				// TODO handle output nodes
				continue;
			}
			nodeType = mg.getNodeTypeInt(node.function);
			if (nodeType == -1) {
				assertTrue(false);
			}
			genes.add(nodeType);
			// add the second/third genes
			edges = mg.edgesOf(node);
			arity = mg.arities.get(node.function);
			// idBuffer = new ArrayList<Integer>();
			Integer z = 0;
			for (DefaultEdge edge : edges) { //TODO fix this code
				if ((mg.getEdgeTarget(edge).id == node.id) && (z < arity)) {
					// must be a previously unseen input
					genes.add(mg.getEdgeSource(edge).id);
					// TODO check for null return from convertToNewID
					z++;
				}
			}
			// fill in holes if arity is less than maxArity
			if (arity < mg.maxArity) {
				for (int j = 0; j < mg.maxArity - arity; j++) {
					genes.add(0);
				}
			}
		}

		System.out.println("Nodes:");
		for (int i = 0; i < mg.idsToVertices.size(); i++) {
			System.out.println(mg.idsToVertices.get(i));
		}
		System.out.println(genes);
		System.out.println(genes.size());
		System.out.println(genes1.size());
	}
	
	@Test
	public void copyTest() {
		MusicGraph mg1 = MusicGraph.makeTestGraph();
		System.out.println("mg1 created");
		MusicGraph mg2 = mg1.copy();
		System.out.println("mg2 created");
		System.out.println(mg1.toString());
		System.out.println(mg2.toString());
		assertEquals(mg1.toString(), mg2.toString());
		assertEquals(mg1.graphID, mg2.graphID);
		for (int i = 0; i < mg1.idsToVertices.size(); i++) {
			MusicNode n1 = mg1.idsToVertices.get(i);
			MusicNode n2 = mg2.idsToVertices.get(i);
			assertEquals(n1.id, n2.id);
			assertEquals(n1.function, n2.function);
			// assertEquals( "Error: " + n1.toString() + " doesn't equal " + n2.toString(), n1, n2); // won't be equal, since MusicNode.equals doesn't exist
		}
		assertEquals(mg1.idsToVertices.keySet(), mg2.idsToVertices.keySet());
		assertEquals(mg1.orderedNodes, mg2.orderedNodes);
		// assertEquals(mg1.idsToVertices.values(), mg2.idsToVertices.values()); // won't be equal, since MusicNode.equals doesn't exist
	}
	
	@Test
	public void nodeFunctionNameTest() {
		MusicGraph mg = MusicGraph.makeTestGraph();
		for (MusicNode n : mg.vertexSet()) {
			System.out.println("Node " + n.id + ": \'" + n.function + "\'");
		}
	}
}
