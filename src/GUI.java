import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;

import org.jgrapht.graph.DefaultEdge;

public class GUI extends JFrame implements ActionListener, ItemListener, MouseListener {

	JButton nextBtn;
	NotePanel notepanel;
	GraphEditor editor;
	int nInds;
	ArrayList<JRadioButton> auditionBtns;
	ArrayList<JCheckBox> selectBtns;

	JButton addNode;
	JButton removeNode;
	JButton editNode; // popup Popup()
	JButton addEdge;
	JButton removeEdge;
	JButton commit;

	// for editNode
	String[] functionOptions;

	// for add edge and remove edge:
	Integer source; // first click (need2 --> need1)
	Integer target; // second click (need1 --> need0)
	Integer need2 = 0; // waiting for 2 more clicks
	Integer need1 = 1; // waiting for 1 more click
	Integer need0 = 2; // waiting for 0 more clicks
	Integer clickState; //  the current click state

	Integer addingEdge = 3; // in the process of adding an edge
	Integer removingEdge = 4; // in the process of removing an edge
	Integer operation; // the current operational state
	
	// If isNew == true, the new dual-panel GUI will be used.
	// Otherwise, the old GUI will be used
	// This may only work for another few commits, since it might become
	// tiresome to
	// support this once NotePanel is connected to GraphEditor.
	Boolean useNew = true;

	// Boolean useNew = false;

	public GUI(String filename) {
		super("NotePanel");

		clickState = need0;
		nInds = 10;
		int side = 530;
		int toolsHeight = 200;
		setSize(new Dimension(side * 2, side + toolsHeight));
		setResizable(false);
		setLayout(new FlowLayout());

		notepanel = new NotePanel(this, filename);
		// when embedding a PApplet. It ensures that the animation
		// thread is started and that other internal variables are
		// properly set.
		notepanel.init();
		editor = new GraphEditor(notepanel);
		editor.init();
		editor.addMouseListener(this);
		JPanel topPanel = new JPanel(new GridLayout(1, 2));

		topPanel.add(notepanel);
		topPanel.add(editor);
		add(topPanel);
		// sleep to allow Processing animation thread to finish its init
		// and set up the population
		try {
			Thread.sleep((long) 1000);
		} catch (Exception e) {
		}

		JPanel masterButtonPanel = new JPanel(new BorderLayout()); // all buttons below the NotePanel/GraphEditor

		JPanel individualselect = new JPanel(new GridLayout(2, 10)); // the audition buttons
		// add a row of radio buttons to audition individuals (use number to
		// select)
		auditionBtns = new ArrayList<JRadioButton>();
		ButtonGroup bg = new ButtonGroup();
		for (int i = 0; i < nInds; i++) {
			JRadioButton aBtn = new JRadioButton("a"
					+ (new Integer(i).toString()));
			aBtn.addItemListener(this);
			auditionBtns.add(aBtn);
			bg.add(aBtn);
			individualselect.add(aBtn);
		}
		// add a row of check buttons for selection (use return to toggle)
		selectBtns = new ArrayList<JCheckBox>();
		for (int i = 0; i < nInds; i++) {
			JCheckBox sBtn = new JCheckBox("s" + (new Integer(i).toString()));
			sBtn.addItemListener(this);
			selectBtns.add(sBtn);
			individualselect.add(sBtn);
		}

		masterButtonPanel.add(individualselect, BorderLayout.LINE_START);

		JPanel buttonPanel = new JPanel(new GridLayout(2, 3)); // the 6 add/edit/remove/commit buttons

		// button for iteration
		nextBtn = new JButton("Next Generation");
		// default size is 150x25
		nextBtn.setPreferredSize(new Dimension(150, 50));
		masterButtonPanel.add(nextBtn, BorderLayout.CENTER);
		nextBtn.addActionListener(this);

		// buttons for GraphEditor
		addNode = new JButton("Add Node");
		buttonPanel.add(addNode);
		addNode.addActionListener(this);

		removeNode = new JButton("Remove Node");
		buttonPanel.add(removeNode);
		removeNode.addActionListener(this);

		editNode = new JButton("Edit Node");
		buttonPanel.add(editNode);
		editNode.addActionListener(this);

		addEdge = new JButton("Add Edge");
		buttonPanel.add(addEdge);
		addEdge.addActionListener(this);

		removeEdge = new JButton("Remove Edge");
		buttonPanel.add(removeEdge);
		removeEdge.addActionListener(this);

		commit = new JButton("Commit Changes");
		buttonPanel.add(commit);
		commit.addActionListener(this);

		masterButtonPanel.add(buttonPanel, BorderLayout.LINE_END);
		add(masterButtonPanel);

		String keyInstructions = "Press 0-9 to audition an individual\n" +
		"Press s to select/deselect current individual\n" +
		"Press y/n to select/deselect and move to next\n" +
		"Press space or Next Generation for a new generation";

		String mouseInstructions = "Add Node -- adds a new node to the graph, choosing the new function from a popup\n" + 
		"Remove Node -- deletes the selected node from the graph\n" + 
		"Edit Node -- change the function of the selected node via a popup\n" + 
		"Add Edge -- after this button is clicked, select a source and target node to add a new edge\n" + 
		"Remove Edge -- after this button is clicked, select a source and target node to remove the corresponding edge\n" + 
		"Commit Changes -- any changes to a graph will not be applied until this button is selected";
		// instructions
		JPanel textPanel = new JPanel();
		textPanel.setLayout(new FlowLayout());
		JTextArea inst = new JTextArea(keyInstructions);
		textPanel.add(inst);

		JTextArea inst2 = new JTextArea(mouseInstructions);
		textPanel.add(inst2);
		add(textPanel);
		
		setVisible(true);
		// requestFocus();
		auditionBtns.get(0).setSelected(true);
		
		// add "CANCEL" to the options
		functionOptions = new String[MusicGraph.nodeTypes.length+1];
		for (int i = 0; i < MusicGraph.nodeTypes.length; i++) {
			functionOptions[i] = MusicGraph.nodeTypes[i];
		}
		functionOptions[functionOptions.length-1] = "CANCEL";
		
	}

	// enable a JButton
	public void disableButton(JButton button) {
		// button.setForeground(Color.GRAY);
		// button.setBackground(Color.WHITE);
		button.setEnabled(false);
	}

	// disable a JButton
	public void enableButton(JButton button) {
		// button.setForeground(new Color(51,51,51));
		// button.setBackground(new Color(238,238,238));
		button.setEnabled(true);
	}

	// Can't do this yet--need to request focus.
	public void keyPressed(KeyEvent e) {
		// displayInfo(e, "KEY PRESSED: ");
		System.out.println("GUI detected key \"" + e.getKeyChar() + "\"");
	}

	private void displayInfo(KeyEvent e, String keyStatus) {

		// You should only rely on the key char if the event
		// is a key typed event.
		int id = e.getID();
		String keyString;
		if (id == KeyEvent.KEY_TYPED) {
			char c = e.getKeyChar();
			keyString = "key character = '" + c + "'";
		} else {
			int keyCode = e.getKeyCode();
			keyString = "key code = " + keyCode + " ("
					+ KeyEvent.getKeyText(keyCode) + ")";
		}

		int modifiersEx = e.getModifiersEx();
		String modString = "extended modifiers = " + modifiersEx;
		String tmpString = KeyEvent.getModifiersExText(modifiersEx);
		if (tmpString.length() > 0) {
			modString += " (" + tmpString + ")";
		} else {
			modString += " (no extended modifiers)";
		}

		String actionString = "action key? ";
		if (e.isActionKey()) {
			actionString += "YES";
		} else {
			actionString += "NO";
		}

		String locationString = "key location: ";
		int location = e.getKeyLocation();
		if (location == KeyEvent.KEY_LOCATION_STANDARD) {
			locationString += "standard";
		} else if (location == KeyEvent.KEY_LOCATION_LEFT) {
			locationString += "left";
		} else if (location == KeyEvent.KEY_LOCATION_RIGHT) {
			locationString += "right";
		} else if (location == KeyEvent.KEY_LOCATION_NUMPAD) {
			locationString += "numpad";
		} else { // (location == KeyEvent.KEY_LOCATION_UNKNOWN)
			locationString += "unknown";
		}

		// Display information about the KeyEvent...
	}

	// handle activating/deactivating the "Commit Changes" button
	public void verifyGraph() {
		Boolean verified = editor.graph.verify();
		// System.out.println(verified);
		System.out.println(editor.graph.toString());
		System.out.println(editor.graph.idsToVertices.toString());
		if (verified) {
			System.out.println("Activating commit button");
			this.enableButton(commit);
		} else {
			System.out.println("Deactivating commit button");
			this.disableButton(commit);
		}
	}

	// unused MouseListener methods
	@Override
	public void mouseClicked(MouseEvent arg0) {}
	@Override
	public void mouseEntered(MouseEvent arg0) {}
	@Override
	public void mouseExited(MouseEvent arg0) {}
	@Override
	public void mouseReleased(MouseEvent arg0) {}

	@Override
	public void mousePressed(MouseEvent e) {
		Point a = e.getPoint();
		// System.out.println("GUI: click at " + a.toString());
		// this will get the new selectedNode
		Integer selectedNode = editor.mousePressedAt(a.x, a.y);		
		if (this.clickState.equals(this.need2)) {
			System.out.println("GUI clickState: need2, operation=" + this.operation);
			// need 1 more click after this
			if (selectedNode == -1) {
				// abort
				clickState = need0;
				verifyGraph();
				return;
			} // otherwise, the first node has been selected
			source = selectedNode;
			this.clickState = this.need1;
			verifyGraph();
		} else if (this.clickState.equals(this.need1)) {
			System.out.println("GUI clickState: need1, operation=" + this.operation);
			// need 0 more clicks after this
			target = selectedNode;
			if (selectedNode == -1) {
				// abort
				clickState = need0;
				verifyGraph();
				return;
			} // otherwise, we've found the second node
			if (this.operation.equals(this.addingEdge)) {
				this.editor.graph.addEdge(source, target);
			} else if (this.operation.equals(this.removingEdge)) {
				DefaultEdge edge = this.editor.graph.removeEdge(source, target);				
				if (edge == null) {
					System.out.println("Error in GUI.mousePressed: edge from node " + source + " to node " + target + " was not created.");
				}
			} else {
				System.out.println("Error in GUI.mousePressed: should never have reached operation code " + this.operation);
				this.clickState = this.need0;
				verifyGraph();
				return;
			}
			this.clickState = this.need0;
			verifyGraph();
			// this.editor.graphAltered = true;
			this.editor.drawGraph(true);
		} else if (this.clickState.equals(this.need0)) {
			System.out.println("GUI clickState: need0");
			// noop
			verifyGraph();
			return;
		} else { // an invalid clickState. Impossible.
			System.out.println("Error in GUI.mousePressed: should never have reached clickState " + clickState);
			verifyGraph();
		}
			
	}
	
	public void actionPerformed(ActionEvent e) {
		System.out.println("In actionPerformed:\n" + e + "\n");

		String a = e.getActionCommand();

		if (a.equals("Next Generation")) {
			System.out.println("nextBtn");
			System.out.println(e.getActionCommand());
			System.out.println("iterating...");
			step();
		} else if ((a.equals("Add Node"))) {
			// reset the clickState just in case we interrupted an add/remove edge
			this.clickState = this.need0;
			System.out.println("Adding node.");
			String userInput = "";
			// popup box allowing user to edit function
			userInput = (String) JOptionPane.showInputDialog(this,
					"Please select a new function", "Add node",
					JOptionPane.QUESTION_MESSAGE, null,
					functionOptions, "sin");
			if (userInput == null) {
				return;
			}
			System.out.println(userInput);
			if (userInput.equals("CANCEL")) {
				return;
			}
			// a valid value was found -- apply to the node
			Integer newNodeID = this.editor.graph.addVertex(userInput);
			System.out.println("Added node " + newNodeID);
			// this.editor.graphAltered = true;
			this.editor.drawGraph(true);
			verifyGraph();
		} else if ((a.equals("Remove Node"))) {
			// reset the clickState just in case we interrupted an add/remove edge
			this.clickState = this.need0;
			System.out.println("Removing node " + this.editor.selectedNode
					+ ".");
			if ((this.editor.selectedNode != null)
					&& (this.editor.selectedNode != -1)) {
				if (this.editor.graph.idsToVertices.containsKey(this.editor.selectedNode)) {
					this.editor.graph.removeNode(this.editor.selectedNode);
					// Boolean removed = this.editor.graph.orderedNodes.remove(this.editor.selectedNode);
					// if (!(removed)) {
					// 	System.out.println("Error in GUI.actionPerformed: node "
					// 					+ this.editor.selectedNode
					// 					+ " was not removed.");
					// }
					this.editor.selectedNode = null;
					if (this.editor.graph.idsToVertices.containsKey(this.editor.selectedNode)) {
						System.out.println("ERROR in GUI.actionPerformed: node " + this.editor.selectedNode + " was just deleted, but is present in editor.graph.idsToVertices:\n" + this.editor.graph.idsToVertices.toString());
					}
					// this.editor.graphAltered = true;
					this.editor.drawGraph(true); // recalculate XY, scale, and draw
												// instantly
					System.out.println(editor.graph.orderedNodes.toString());
					System.out.println("Removed in GraphEditor.");
					verifyGraph();
				}
			}
		} else if ((a.equals("Edit Node"))) {
			// reset the clickState just in case we interrupted an add/remove edge
			this.clickState = this.need0;
			System.out.println("Editing node " + this.editor.selectedNode + ".");
			if (this.editor.selectedNode.equals(-1)) {
				// no node was selected
				return;
			}
			String userInput = "";
			// popup box allowing user to edit function
			userInput = (String) JOptionPane.showInputDialog(this,
					"Please select a new function", "Edit node",
					JOptionPane.QUESTION_MESSAGE, null,
					functionOptions, "sin");
			if (userInput == null) {
				return;
			}
			System.out.println(userInput);
			if (userInput.equals("CANCEL")) {
				return;
			}
			// a valid value was found -- apply to the node
			MusicNode n = this.editor.graph.idsToVertices.get(this.editor.selectedNode);
			n.function = userInput;
			MusicNode n2 = this.editor.graph.idsToVertices.get(this.editor.selectedNode);
			if (!(n2.function.equals(userInput))) {
				System.out.println("ERROR in GUI.actionPerformed: userInput function \"" + userInput + "\" not the same as node function \"" + n2.function + "\"");
			}
			// this.editor.graphAltered = true;
			this.editor.drawGraph(true);
			verifyGraph();
		} else if ((a.equals("Add Edge"))) {
			System.out.println("Adding edge");
			// popup box allowing user to edit function
			this.clickState = this.need2;
			this.operation = this.addingEdge;
			// this.editor.graphAltered = true;
			// this.editor.drawGraph();
			// verifyGraph();
		} else if ((a.equals("Remove Edge"))) {
			System.out.println("Removing edge");
			// popup box allowing user to edit function
			this.clickState = this.need2;
			this.operation = this.removingEdge;
			// this.editor.graphAltered = true;
			// this.editor.drawGraph();
			// verifyGraph();
		} else if ((a.equals("Commit Changes"))) {
			// reset the clickState just in case we interrupted an add/remove edge
			this.clickState = this.need0;
			System.out.println("Commiting changes");

			ArrayList<Integer> genes = new ArrayList<Integer>();
			try {
				genes = editor.graph.reverseMap();
				System.out.println("Reverse map found");
			} catch (MusicGraph.NoSuchFunctionException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			// TODO inject the new MusicGraph back into the population
			// Replace in the notepanel
			notepanel.musicGraphs.set(editor.notePanelIndex, editor.graph);

			VariGA.Individual individual = notepanel.gaThread.ga.toIndividual(genes);
			if (individual == null) {
				System.out.println("Individual is null");
			}
			if (editor.graph.popIndex == null) {
				System.out.println("GUI.editor.graph.popIndex is null");
			}
			notepanel.gaThread.ga.pop.set(editor.graph.popIndex, individual);
			verifyGraph();
			auditionIndividual(editor.notePanelIndex);

		} else {
			System.out.println("got an event in actionPerformed");
			String name = a;
			System.out.println("name = " + name);
			int index = name.charAt(1) - '0';
			if (name.charAt(0) == 'a') {
				// audition button
				System.out.println("audition " + index);
				auditionIndividual(index);
			} else {
				// select button
				System.out.println("select " + index);
				// no need to do anthing now -- we read values of
				// selection buttons at the end.
				// selectIndividual(index);
			}
		}
	}

	public Boolean arrayContains(String[] array, String value) {
		for (String v : array) {
			if (v.equals(value)) {
				return true;
			}
		}
		return false;
	}

	public void itemStateChanged(ItemEvent e) {
		// System.out.println("in itemStateChanged" + e);

		Object source = e.getItemSelectable();

		if (e.getStateChange() == ItemEvent.SELECTED) {
			for (int i = 0; i < nInds; i++) {
				JCheckBox cb = selectBtns.get(i);
				if (source == cb) {
					System.out.println("found source -- select" + i);

					// no need to do anything now -- we read values of
					// selection buttons at the end.
					// selectIndividual(i);
				}
			}
			for (int i = 0; i < nInds; i++) {
				JRadioButton rb = auditionBtns.get(i);
				if (source == rb) {
					System.out.println("found source -- audition" + i);
					auditionIndividual(i);
					this.verifyGraph();
				}
			}
		}

		// give focus back to applet so keys can be caught etc.
		notepanel.requestFocus();
	}

	public void auditionIndividual(int ind) {
		notepanel.auditionIndividual(ind);
		if (useNew) {
			if (ind < notepanel.musicGraphs.size()) {
				editor.setGraph(notepanel.musicGraphs.get(ind), ind);
				// editor.draw(); //TODO might be unnecessary, since
				// GraphEditor.setGraph() already calls draw()
			}
		}
	}

	// The procedure for linking GA and GUI:

	// 1. GUI calls pop.eval() which runs non-interactively, and sorts inds,
	// best-first
	// 2. take the top 10 into the GUI
	// 3. user plays and then hits space
	// 4. GUI sets fitness of everything to zero, except those with tick marks
	// 5. call step(false)

	// Here, user has hit space to request new generation (step 3)
	public void step() {
		// we perform step 4.
		for (int i = 0; i < notepanel.gaThread.ga.popsize; i++) {
			notepanel.gaThread.ga.pop.get(i).fitness = 0.0f;
		}
		for (int i = 0; i < nInds; i++) {
			boolean selected = selectBtns.get(i).isSelected();
			if (selected) {
				notepanel.gaThread.ga.pop.get(i).fitness = 1.0f;
			}
		}

		// we ask gaThread to perform step 5.
		notepanel.gaThread.step();
	}

	public static void main(String[] args) {
		if (args.length > 0) {
			GUI gui = new GUI(args[0]);
		} else {
			GUI gui = new GUI("");
		}
	}
}