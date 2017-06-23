Summary
-------

This is **XG**, aka *GraphMusic*, an evolutionary music system written
in Java. It was written by James McDermott, then extended by Dylan
Sherry, in MIT CSAIL 2010-2012.



Copyright
---------

Copyright (c) James McDermott and Dylan Sherry 2010-2012.

Contact
-------

James McDermott <jamesmichaelmcdermott@gmail.com>.

License
-------

This software is licensed for use under the GNU GPL 3.0 or greater.




Citation
--------

XG was described in two papers. Please cite one or both as appropriate
if using this code.

"An Executable Graph Representation for Evolutionary Generative
Music", James McDermott and Una-May O'Reilly, 2011.

@InProceedings{mcdermott-oreilly-xg,
  author = 		 {James McDermott and Una-May O'Reilly},
  title = 		 {An Executable Graph Representation for Evolutionary
                  Generative Music},
  booktitle = {GECCO '11},
  year = 	 {2011},
  address = 	 {Dublin}
}

@incollection{mcdermott2013evolutionary,
  title={Evolutionary and generative music informs music HCI—and vice versa},
  author={James McDermott and Dylan Sherry and Una-May O'Reilly},
  booktitle={Music and human-computer interaction},
  pages={223--240},
  year={2013},
  publisher={Springer}
}

Description
-----------

It generates music from graphs (see `MusicGraph.java`) in which nodes
(see `MusicNode.java`) represent mathematical functions and edges
represent the flow of data through the graph. Input nodes take in data
representing the bar and beat, as well as several abstract control
variables. Output nodes create notes and rests.

Each graph is generated from a variable-length integer array (see
`MusicGraph.java`) which specifies the node types and the connections.
The arrays are seen as genomes: they are evolved (see `VariGA.java`)
under interactive or non-interactive selection. Ideally, after several
generations, genomes which lead to bad graphs are weeded out and good
ones are continually recombined, to produce better and better graphs,
and thus better and better music.

A GUI (see `GUI.java` and `GAThread.java`) is provided for interactive
aesthetic evolution and for real-time control of the abstract control
variables (see `NotePanel.java`). They can also be input from a file,
such as `data/abab_2mins.txyz`. A Python script is provided to
generate such files: `scripts/generate_xyz.py`.

Non-interactive selection can be based on, for example, numerical
feature vectors calculated from the musical data (see
`FeatureVector.java`).

There is also a method for editing the graphs (phenotypes) directly,
leading indirectly to changes in the music (see `GraphEditor.java`).




Install
-------

The provided Makefile is used to compile and run code. To run an
interactive GA, use "make gui && make guirun". To run a
non-interactive GA, use "make ga && make garun".

NB: Due to changes in directory structure, tools, Java versions,
etc. it may not compile directly as-is. Slight changes are likely
required. Pull requests are welcome.

XG requires several libraries (provided: see libraries/): JGraphT,
Processing, JMusic. Thanks are due to the authors and maintainers of
these libraries for their hard work and for releasing their code as
free software.
