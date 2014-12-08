/**
 * Huffman encoding.
 * Encodes a binary sourcefile into a targetfile.
 * @author Anthony Barranco
 *
    Restrictions:
    -longer codes will be to the left of shorter codes
    -same length code words will have numerical values increases with lexicographical order
    e.g a = 0000 and b = 0001 but a =/= 0001 and b =/= 0000
    Easiest to create canonical tree: create optimal tree and then convert

    Binary format:
    tree is a header at the beginning of our binary file (sourcefile)
    First 8 bits represent the number of k of characters in the alphabet (unsigned)
    Then k pairs of bytes representing (Char value, length of codeword)
    Length of codeword = depth of the leaf in the tree.
    0 = left, 1 = right

    Steps:
    1: Count character frequencies
    2: Create Huffman tree
    3: Canoncize huffman tree
    4: Write tree to output file
    5: write coded data to output file
 */
package huffman;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Encode
{

    /**
     * Input for endcoding.
     * Can be called with 2 sets of arguments. Arguments include file name and extension (if applicable)
     * @param args <sourceFile targetFile> Input text path, output binary path
     * @param args <-c outputGraphFile sourceFile targetFile> output graph directory, input text path, output binary path
     */
    public static void main(String[] args) throws IOException
    {
        if(args.length >= 2)
        {
            if(args[0].equals("-c"))
            {
                String message = ReadFile(args[2]);
                EncodeToFile(args[3], args[1], true, message);
            }
            else if(args[0].equals("-h"))
            {
                String message = ReadFile(args[2]);
                EncodeToFile(args[3], args[1], false, message);
            }
            else
            {
                String message = ReadFile(args[0]);
                EncodeToFile(args[1], "", true, message);
            }
        }
        else
        {
            System.out.println("Please provide sourcefile and targetfile, or optionally sourcefile");
            //String message = ReadFile("samples//text//sample7.txt");
            //EncodeToFile("output//outputencode.txt", "output//graphencode.gv", false, message);
        }
    }

    /**
     * Read the encoded binary file
     * @param path File path to binary file
     * @return byte array representation of the binary file
     */
    static String ReadFile(String path) throws IOException
    {
        Path p = Paths.get(path);
        return new String(Files.readAllBytes(p));
    }

    /**
     * Reads the decoded/plain text file, creates a canonical Huffman tree, and then writes the encoded message to a binary file with header
     * @param outputFilePath the output path of the decoded msg
     * @param outputGraphFilePath the output path of the huffman tree image, pass empty string to skip
     * @param canonicalGraphOutput true if graph out is canonical tree or false for the non canonical tree
     * @param message the secret message to encode to a binary file
     */
    static void EncodeToFile(String outputFilePath, String outputGraphFilePath, boolean canonicalGraphOutput, String message) throws IOException
    {
        message += '\u0000';

        // Get the characters/frequencies to use later for header
        ArrayList<TreeNode> charactersToEncode = new ArrayList<TreeNode>();

        // Using textbook algorithm, create noncanonical huffman tree
        GraphViz nonCanonicalGraph = new GraphViz();
        nonCanonicalGraph.addln(nonCanonicalGraph.start_graph());
        TreeNode nonCanonicalRoot = GetNonCanonicalTree(message, nonCanonicalGraph, charactersToEncode);
        nonCanonicalGraph.addln(nonCanonicalGraph.end_graph());

        int numOfChars = charactersToEncode.size();

        // Get the leafs of uncanonical tree, sort them into canonical
        ArrayList<TreeNode> huffmanLeafs = new ArrayList<TreeNode>();
        GetHuffmanLeafs(nonCanonicalRoot,0,huffmanLeafs);
        Collections.sort(huffmanLeafs, TreeNode.CanonicalCompare);

        // Bottom up approach to creating a Canonical Huffman tree.
        GraphViz canonicalGraph = new GraphViz();
        canonicalGraph.addln(canonicalGraph.start_graph());

        HashMap<Character,String> decoder = new HashMap<Character,String>();
        GetCanonicalTree(numOfChars, huffmanLeafs, decoder, canonicalGraph);
        canonicalGraph.addln(canonicalGraph.end_graph());
        if(outputGraphFilePath.length() > 0)
        {
            if(canonicalGraphOutput)
            {
                GraphViz.WriteGraphSource(canonicalGraph, outputGraphFilePath);
                //GraphViz.WriteGraphImageFile(canonicalGraph, outputGraphFilePath + ".png");
            }
            else
            {
                GraphViz.WriteGraphSource(nonCanonicalGraph, outputGraphFilePath);
                //GraphViz.WriteGraphImageFile(nonCanonicalGraph, outputGraphFilePath + "UnCanonical.png");
            }
        }

        // Begin writing binary output
        ByteArrayOutputStream s = new ByteArrayOutputStream();

        // Header
        s.write(numOfChars);
        for(int i = 0; i < numOfChars; ++i)
        {
            char headerChar = charactersToEncode.get(i).m_Char;
            s.write((int) headerChar);
            s.write(decoder.get(headerChar).length());
        }

        // Secret message data
        StringBuilder encodedMsg = new StringBuilder();
        int msgLength = message.length();
        for(int i = 0; i < msgLength; ++i)
        {
            char letter = message.charAt(i);
            if(letter != '\r')
            {
                String encode = decoder.get(letter);
                encodedMsg.append(encode);
            }
        }

        // Padding for data
        if(encodedMsg.length() % 8 != 0)
        {
            while(encodedMsg.length() % 8 != 0)
            {
                encodedMsg.append("0");
            }
        }

        // Write the data and write binary file
        for(int i = 0; i < encodedMsg.length()/8; ++i)
        {
            s.write(Integer.parseInt(encodedMsg.substring(i*8,i*8+8),2));
        }

        //System.out.println("Encoded msg: " + encodedMsg.toString());
        Files.write(Paths.get(outputFilePath), s.toByteArray());
    }

    /**
     * Using the textbook algorithm, creates a non canonical huffman tree and returns the root. Also inserts
     * all unique characters to encode into an arraylist, and modifies graphviz with representation
     * @param message the secret text message to encode
     * @param nonCanonicalGraph graph representation of a tree
     * @param charactersToEncode unique characters to encode
     */
    static TreeNode GetNonCanonicalTree(String message, GraphViz nonCanonicalGraph, ArrayList<TreeNode> charactersToEncode)
    {
        HashMap<Character,Integer> frequencies = new HashMap<Character, Integer>();

        int length = message.length();
        for(int i = 0; i < length; ++i)
        {
            char letter = message.charAt(i);
            if(letter != '\r') {
                if (frequencies.containsKey(letter)) {
                    frequencies.put(letter, frequencies.get(letter) + 1);
                } else {
                    frequencies.put(letter, 1);
                }
            }
        }

        PriorityQueue<TreeNode> q = new PriorityQueue<TreeNode>(frequencies.size(),TreeNode.CanonicalCompareEncode);
        for (Map.Entry<Character, Integer> entry : frequencies.entrySet())
        {
            TreeNode n = new TreeNode(false, entry.getValue());
            n.m_Char = entry.getKey();
            q.add(n);
            charactersToEncode.add(n);
        }

        int numOfChars = q.size();

        for(int i = 1; i < numOfChars; ++i)
        {
            TreeNode x = q.poll();
            TreeNode y = q.poll();

            TreeNode node = new TreeNode(true, 0);
            if(!x.m_IsNode)
            {
                x.GraphVizLabel(nonCanonicalGraph,Integer.toString(node.hashCode()));
            }
            else
            {
                nonCanonicalGraph.addln(node.hashCode() + " -> " + x.hashCode());
            }
            if(!y.m_IsNode)
            {
                y.GraphVizLabel(nonCanonicalGraph, Integer.toString(node.hashCode()));
            }
            else
            {
                nonCanonicalGraph.addln(node.hashCode() + " -> " + y.hashCode());
            }
            node.m_Char = (char) 127;
            node.m_Left = x;
            node.m_Right = y;
            node.m_Depth = x.m_Depth + y.m_Depth;
            q.add(node);
        }

        return q.poll();
    }

    /**
     * Using a sorted list of treenodes, creates a canonical huffman tree with graphviz representation,
     * creates decoder, and returns the root.
     * @param numOfChars number of unique characters in the tree
     * @param huffmanLeafs sorted list of TreeNodes in canonical order
     * @param decoder unique characters tied to their canonical binary representation
     * @param canonicalGraph represenation of canonical tree
     * @return root of the tree
     */
    static void GetCanonicalTree(int numOfChars, ArrayList<TreeNode> huffmanLeafs, HashMap<Character, String> decoder, GraphViz canonicalGraph)
    {
        TreeNode canonicalRoot = new TreeNode(true, -1);
        canonicalRoot.m_ID = "Root";
        int position = 0;
        for(int i = numOfChars-1; i >= 0; --i)
        {
            TreeNode nodeToAdd = huffmanLeafs.get(i);
            TreeNode nextNode = i > 0 ? huffmanLeafs.get(i-1) : null;
            StringBuilder directionStr = new StringBuilder();
            String unpaddedBinary = Integer.toBinaryString(position); // Gives us the canonical representation of the char
            for(int x = 0; x < nodeToAdd.m_Depth-unpaddedBinary.length(); ++x) // Expected depth - unpadded length = padding
            {
                directionStr.append("0"); // Padding for the binary code.
            }
            directionStr.append(unpaddedBinary); // Binary representation of the code we need
            decoder.put(nodeToAdd.m_Char,directionStr.toString());
            if (nextNode != null)
            {
                int newPosition = (position+1);
                int offset = nodeToAdd.m_Depth - nextNode.m_Depth;
                position = newPosition >> offset; // Binary representation of the next letter in the tree.
            }
            int directionBits = directionStr.length();
            TreeNode cur = canonicalRoot;
            // Make the canonical Huffman tree using the direction for this char
            for(int bit = 0; bit < directionBits; ++ bit)
            {
                String nodeID = "_"+directionStr.substring(0,bit+1)+"_";
                boolean left = (directionStr.charAt(bit) == '0');
                TreeNode examineNode = left ? cur.m_Left : cur.m_Right;
                if(examineNode == null)
                {
                    if (bit == directionBits - 1)
                    {
                        nodeToAdd.GraphVizLabel(canonicalGraph, cur.m_ID);
                        if (left)
                        {
                            cur.m_Left = nodeToAdd;
                        }
                        else
                        {
                            cur.m_Right = nodeToAdd;
                        }
                    }
                    else
                    {
                        examineNode = new TreeNode(true, 0);
                        examineNode.m_ID = nodeID;
                        if (left)
                        {
                            cur.m_Left = examineNode;
                        }
                        else
                        {
                            cur.m_Right = examineNode;
                        }
                        canonicalGraph.addln(cur.m_ID + " -> " + examineNode.m_ID);
                        cur = examineNode;
                    }
                }
                else
                {
                    cur = examineNode;
                }
            }
        }
        //return canonicalRoot;
    }

    /**
     * Adds leafs of a tree to a provided list and modifies the depth of the leafs for accuracy
     * @param node TreeNode to search from
     * @param depth current depth searched, increments in each recursive call
     * @param leafsToFind list of leafs that gets added to when found
     */
    static void GetHuffmanLeafs(TreeNode node, int depth, List<TreeNode> leafsToFind)
    {
        if(!node.m_IsNode)
        {
            node.m_Depth = depth;
            leafsToFind.add(node);
        }
        else
        {
            ++depth;
            if(node.m_Left != null)
            {
                GetHuffmanLeafs(node.m_Left, depth, leafsToFind);
            }
            if(node.m_Right != null)
            {
                GetHuffmanLeafs(node.m_Right, depth, leafsToFind);
            }
        }
    }

}