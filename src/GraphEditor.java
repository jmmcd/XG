import java.awt.Color;
import java.io.File;
import java.util.Set;

import org.jgrapht.graph.DefaultEdge;

import processing.core.PApplet;
import processing.core.PFont;

public class GraphEditor extends PApplet {
	NotePanel notePanel;
	Integer side;
	MusicGraph graph;
	Integer notePanelIndex;
	Boolean isGraphNull = true;
	GAThread gaThread;
	PFont f;
	Integer nodeDiameter;

	// used in draw(), drawNew(), detectNodeClick(), etc.
	Boolean graphAltered;
	Integer selectedNode;

	// global variables that govern stroke/fill properties
	Integer defaultStroke;
	Integer defaultFill; // note: edge stroke/fill = default, for simplicity.
	Integer nodeStroke;
	Integer nodeFill;
	Integer textStroke;
	// Integer textFill;
	Color textFill;
	Integer textSize;
	String fontName;
	Integer locusStroke;
	Integer locusFill;

	Integer selectStroke; // replaced with #0000ff in draw()

	File current;
	String absPath;
	
	public GraphEditor(NotePanel _notepanel) {
		super();
		notePanel = _notepanel;
		side = 530;
		graph = null;
		nodeDiameter = 10;
		selectedNode = null;
		graphAltered = true;
		defaultStroke = 0;
		defaultFill = 0;
		nodeStroke = 200;
		nodeFill = 155;
		textStroke = 0;
		textFill = new Color(12,31,74);
		textFill = new Color(36,50,88);
		textFill = new Color(0,155,0);
		textFill = new Color(200);
		textSize = 12;
		// fontName = "CharterBT-Roman-10.vlw";
		// fontName = "CourierNewPSMT-12.vlw";
		fontName = "Arial-BoldMT-13.vlw";
		locusStroke = 180;
		locusFill = 58;

		selectStroke = 255;

		// file path stuff
		current = new File(".");
		absPath = current.getAbsolutePath();
		absPath = absPath.substring(0, absPath.length()-1);
		System.out.println(absPath);
		MusicGraph.absPath = absPath;
	
	}

	/**
	 * Initialize processing
	 */
	public void setup() {
		size(side, side);
		stroke(defaultStroke);
		fill(defaultFill);
		background(192, 40, 0);
		f = loadFont(fontName);
		textFont(f, textSize);
		// trying higher frame rates for click detection
		// int fps = 4;
		int fps = 15;
		frameRate(fps);
		selectedNode = -1;
	}

	/**
	 * Draw one frame
	 */
	public void draw() {
		if (graphAltered) {
			if (!(graph == null)) {
				// System.out.println("Drawing Graph:");
				drawGraph(this.graphAltered);
			}
			graphAltered = false;
		}
		// selection in the applet
	}

	// called by GUI
	public Integer mousePressedAt(Integer x, Integer y) {
		Integer newSelectedNode = this.detectNodeClick(x,y);
		System.out.println("Click on node " + newSelectedNode.intValue());
		if (newSelectedNode != this.selectedNode) { // only if new value has
			// been detected (may
			// eliminate duplicates?)
			// draw selection

			// new node -- draw highlighting
			if (newSelectedNode.intValue() != -1) {
				MusicNode newSelection = this.graph.idsToVertices.get(newSelectedNode);
				stroke(0, 0, 255);
				fill(0, 0, 255);
				ellipse(newSelection.getX().intValue(), newSelection.getY().intValue(), 10, 10);
			}
			// old node -- revert highlighting
			if ((selectedNode != null) && (selectedNode != -1)) {
				MusicNode oldSelection = this.graph.idsToVertices
						.get(selectedNode);
				stroke(nodeStroke);
				fill(nodeFill);
				ellipse(oldSelection.getX().intValue(), oldSelection.getY()
						.intValue(), 10, 10);

			}
			selectedNode = newSelectedNode; // changed selection
		}
		return selectedNode;
	}


	/**
	 * Set this.graph to a new value
	 * 
	 * @param g
	 *            -- the new MusicGraph
	 * @param ind
	 *            -- the index of that graph in the notePanel
	 */
	public void setGraph(MusicGraph g, Integer ind) {
		this.graph = g.copy();
		this.notePanelIndex = ind;
		// TODO this is a bad place to set this.
		// this.graphAltered = true;
		this.drawGraph(true);
	}

	// TODO move this method to MusicGraph
	/**
	 * Calculates which node has been clicked on the current MusicGraph SHOULD
	 * BE MOVED TO MUSICGRAPH
	 * @param y 
	 * @param x 
	 * 
	 * @return -1 if no node has been clicked, else the nodeID
	 */
	public Integer detectNodeClick(Integer x, Integer y) {
		MusicNode node;
		Integer[] gNodes = graph.getNodes(); // not ordered
		for (Integer nodeID : gNodes) {
			node = graph.idsToVertices.get(nodeID);
			if (graph.overCircle(x, y, node, nodeDiameter / 2)) {
				return node.id;
			}
		}
		return -1;
	}

	// /**
	// * Loads a new MusicGraph
	// * @param g
	// */
	// public void loadGraph(MusicGraph g) {
	// // has xy been computed?
	// Boolean xy_available = g.xy_available;
	// if (!xy_available) {
	// g.computeXY();
	// }
	// // set the scaling factor
	// computeScalingFactor(g);
	//		
	// }

	/**
	 * If no graph is specified, draw this instance's graph.
	 */
	public void drawGraph(Boolean getXY) {
		drawGraph(this.graph, getXY);
	}

	/**
	 * Draw a graph -- called from the draw() method
	 */
	public void drawGraph(MusicGraph g, Boolean getXY) {
		// has xy been computed?
		// Boolean xy_available = g.xy_available;
		background(192, 40, 0);
		if (!g.xy_available || getXY) {
			g.computeXY();
			g.scaleXY(this.side, this.side);
		}
		selectedNode = -1;
		// set the scaling factor -- moved to g.scaleXY()
		// computeScalingFactor(g);
		MusicNode node;
		DoublePair nodeXY;
		// Double xScaled;
		// Double yScaled;
		String function;

		Double nodex;
		Double nodey;

		// get edges
		Set<DefaultEdge> rawEdges = g.edgeSet();
		Object[] unformattedE = rawEdges.toArray();
		DefaultEdge[] formattedE = new DefaultEdge[rawEdges.size()];
		Integer i = 0;
		for (Object edge : unformattedE) {
			formattedE[i] = (DefaultEdge) edge;
			i++;
		}
		// draw each edge
		i = 0;
		MusicNode source;
		MusicNode target;
		stroke(defaultFill);
		fill(defaultFill);
		Integer sx;
		Integer sy;
		Integer dx;
		Integer dy;

		for (DefaultEdge edge : formattedE) {
			// TODO draw edgeLocus things for edge selection/editing. Have in
			// list somewhere
			// stroke(locusStroke);
			// fill(locusFill);
			// stroke(defaultStroke);
			// fill(defaultFill);
			source = g.getEdgeSource(edge);
			target = g.getEdgeTarget(edge);
			// this.line(100, 200, 300, 400);
			sx = source.getX().intValue();
			sy = source.getY().intValue();
			dx = target.getX().intValue();
			dy = target.getY().intValue();
			// this.line(sx, sy, dx, dy);
			this.arrow(sx, sy, dx, dy, 30);
			i++;
		}

		// get nodes
		for (Integer nodeID : g.idsToVertices.keySet()) {
			node = g.idsToVertices.get(nodeID);
			nodex = node.getX();
			nodey = node.getY();
			function = node.function;
			// draw
			stroke(nodeStroke);
			fill(nodeFill);
			ellipse(nodex.intValue(), nodey.intValue(), nodeDiameter,
					nodeDiameter);
		}
		// draw the node text
		stroke(textStroke);
		fill(textFill.getRed(),textFill.getGreen(), textFill.getBlue());
		for (Integer nodeID : g.idsToVertices.keySet()) {
			node = g.idsToVertices.get(nodeID);
			nodex = node.getX();
			nodey = node.getY();
			function = node.function;
			text(nodeID.toString() + ":" + function, nodex.intValue()-20,
					nodey.intValue() - 10);
		}
		stroke(defaultStroke);
		fill(defaultFill);
		this.repaint();
	}

	/**
	 * Converts Double to Integer -- added due to primitive issues in drawGraph
	 * line drawing. Why: can't call Double.intValue() on a double primitive,
	 * and casting doesn't work in context.
	 * 
	 * @param d
	 *            A Double
	 * @return An Integer (rounded)
	 */
	public Integer toInt(Double d) {
		return d.intValue();
	}

	/**
	 * Tentative arrow function
	 */
	public void arrow(int x1, int y1, int x2, int y2, int headOffset) {
		float a;
		line(x1, y1, x2, y2);
		// strokeWeight(3);
		pushMatrix();
		translate(x2, y2);
		a = atan2(x1 - x2, y2 - y1);
		rotate(a);
		// draw the arrowhead
		// triangle( -5, -10, 0, 0, +5,-10);
		triangle(-5, -10 - headOffset, 0, -headOffset, +5, -10 - headOffset);
		popMatrix();
		this.resetMatrix();
		// strokeWeight(1);
	}

	/**
	 * Give the GraphEditor a new GAThread
	 * 
	 * @param gat
	 */
	public void setGAThread(GAThread gat) {
		this.gaThread = gat;
	}

	public GAThread getGAThread() {
		return this.gaThread;
	}

	public static void main(String args[]) {
		PApplet.main(new String[] { "--present", "GraphEditor" });
	}
}