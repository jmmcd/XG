#!/usr/bin/env python

import sys
from math import *

class Piece:

    avals = [0.2, 0.7, 0.5]
    bvals = [0.8, 0.1, 0.9]

    def __init__(self,
                 _sections,
                 _signature,
                 _section_length
                 ):

        self.sections = _sections

        # in bars
        self.section_length = _section_length
        self.piece_length = len(self.sections) * self.section_length

        # time signature, beats per bar
        self.signature = _signature

        self.filename = "data/%s_%d_%d.txyz" % (self.sections,
                                                self.signature,
                                                self.section_length)

    def write(self):
        outf = open(self.filename, "w")
        print("writing " + self.filename)
        for t in range(self.piece_length * self.signature):

            # an upward-sloping curve, from 0 to 1.0.
            # z = (t / float(self.piece_length * self.signature)) ** 5.0

            # a half-sinusoid, starting at 0.5, up to 1.0, then back, slowly.
            z = 0.5 * (1.0 + sin(2.0 * pi * t / float(self.piece_length * self.signature)))

            section = self.sections[t / (self.section_length * self.signature)]

            # We count bars and beats from 1. it's standard in music
            # and prevents discontinuously low values on the outputs
            # leading to silences at the start.
            bar = 1 + ((t % (self.section_length * self.signature))
                       / self.signature)
            beat = 1 + (t % self.signature)

            #  (t % (section_length * signature * 2)) < (section_length * signature):
            if section == "A":
                x = Piece.avals[0]
                y = Piece.avals[1]
                z = Piece.avals[2]
            elif section == "B":
                x = Piece.bvals[0]
                y = Piece.bvals[1]
                z = Piece.bvals[2]
            else:
                print("Error, unknown section!")
                sys.exit(1)

            outf.write("%f %f %f %f %f\n" % (bar, beat, x, y, z))
        outf.close()

pieces = [
    Piece("AABB", 6, 4),
    Piece("AAAB", 4, 4),
    Piece("AAAB", 12, 2),
    Piece("ABB", 4, 4),
    Piece("ABB", 12, 4),
    Piece("AAB", 8, 4),
    Piece("AAABB", 4, 3),
    Piece("AABB", 8, 4),
    Piece("AAAB", 9, 3),
    Piece("AABA", 3, 4),
    # opening_start.mid is 2 repetitions of the theme. Each
    # repetition is 4 bars. Each bar is 12 notes (voice 1), 8
    # notes (voice 2), 1 note (voice 3). so require bar to be
    # divided in 24.
    Piece("AA", 24, 8),
    # simple_piece is just one rep of a two-bar section. each bar is
    # of 16th notes.
    Piece("A", 16, 2),
    Piece("AAAAAAAAAAAAAAA", 6, 1),
    Piece("A", 6, 1)

    ]

if __name__ == "__main__":
    for piece in pieces:
        piece.write()
