cp=${CLASSPATH}:.:libraries/jgrapht-jdk1.5.jar:libraries/jMusic1.6.02.jar:libraries/processing-1.2.1-core.jar:libraries/junit-4.9b2.jar

all:
	javac -cp ${cp} MusicGraph.java MusicNode.java MusicGraphTest.java
run:
	java -cp ${cp} MusicGraph
test:
	java -cp ${cp} org.junit.runner.JUnitCore MusicGraphTest

ga:
	javac -cp ${cp} VariGA.java MusicGraph.java FeatureVector.java MusicNode.java
garun:
	java -cp ${cp} VariGA data/AABA_6_4.txyz

gui:
	javac -cp ${cp} GUI.java NotePanel.java MusicGraph.java MusicNode.java VariGA.java GAThread.java GraphEditor.java
guirun:
	java -cp ${cp} GUI data/AABB_6_4.txyz

guirunmousemode:
	java -cp ${cp} GUI

analysis:
	javac -cp ${cp} FileAnalysis.java
analysisrun:
#		java -cp ${cp} FileAnalysis data/results/NoSelectionPiecesForImitation/rep0/AABB_6_4.txyz.midi
	java -cp ${cp} FileAnalysis data/simple_piece.mid
