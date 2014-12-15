Added GraphViz output per the directions.

With DOT Source output:
java huffman.Decode -c CANONICAL_TREE_FILE SOURCEFILE TARGETFILE
java huffman.Encode -c CANONICAL_TREE_FILE SOURCEFILE TARGETFILE
java huffman.Encode -h NONCANONICAL_TREE_FILE SOURCEFILE TARGETFILE
Ex: java huffman.Decode -c graph.gv huffman01.huf decoded.txt

Without:
java huffman.Decode SOURCEFILE TARGETFILE
java huffman.Encode SOURCEFILE TARGETFILE
Ex: java huffman.Decode huffman01.huf decoded.txt
