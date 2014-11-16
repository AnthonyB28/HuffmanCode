/**
 * Huffman decoding.
 * Decodes a binary sourcefile into a targetfile.
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
    1 Read in and build tree
    2 Store codes for lookup efficiently
    3 Decode and write output
 */

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Decode
{

    /**
     * Input for decoding.
     * Can be called with 2 sets of arguments. Arguments include file name and extension (if applicable)
     * @param args <sourceFile targetFile> Input binary path, output text path
     * @param args <-c outputGraphFile sourceFile targetFile> output graph directory, input binary path, output text path
     */
    public static void main(String[] args) throws IOException
    {
        if(args.length >= 2)
        {
            if(args[0] == "-c")
            {
                byte[] input2 = ReadFile(args[2]);
                DecodeToFile(args[3], args[1], input2);
            }
            else
            {
                byte[] input2 = ReadFile(args[0]);
                DecodeToFile(args[1], "", input2);
            }
        }
        else
        {
            System.out.println("Please provide sourcefile and targetfile, or optionally sourcefile");
            byte[] input = ReadFile("samples//encoded//sample5.huf");
            DecodeToFile("output//output.txt", "output//graph.png", input);
        }
    }

    /**
     * Read the encoded binary file
     * @param path File path to binary file
     * @return byte array representation of the binary file
     */
    static byte[] ReadFile(String path) throws IOException
    {
        Path p = Paths.get(path);
        return Files.readAllBytes(p);
    }

    /**
     * Reads the encoded binary, creates a canonical Huffman tree, and then writes the decoded message to the file
     * @param outputFilePath the output path of the decoded msg
     * @param outputGraphFilePath the output path of the huffman tree image, pass empty string to skip
     * @param binary the binary file representation of the huffman code and secret message
     */
    static void DecodeToFile(String outputFilePath, String outputGraphFilePath, byte[] binary) throws IOException
    {
        int numberOfChars = binary[0];
        PriorityQueue<TreeNode> q = new PriorityQueue<TreeNode>(numberOfChars,TreeNode.CanonicalCompare);

        // Parse the binary header and insert into the priorityQ
        for(int i = 1; i <= numberOfChars*2; ++i)
        {
            int charNum = binary[i];
            char val = (char) charNum;
            ++i;
            int length = binary[i];
            TreeNode character = new TreeNode(false, length);
            character.m_Char = val;
            q.add(character);
        }
/*       Sorting via the textbook's algorithm. Doesnt seem to work here. Maybe for encoding?
        for(int i = 0; i < numberOfChars-1; ++i)
        {
            TreeNode node = new TreeNode(true, 0);
            TreeNode x = q.poll();
            TreeNode y = q.poll();
            node.m_Left = x;
            node.m_Right = y;
            node.m_Depth = x.m_Depth + y.m_Depth;
            q.add(node);
        }
        TreeNode root = q.poll();*/

        TreeNode root = new TreeNode(true, -1);
        root.m_ID = "Root";
        TreeNode[] queueArray = new TreeNode[q.size()];
        q.toArray(queueArray);
        Arrays.sort(queueArray, TreeNode.CanonicalCompare); // Gives us the final sorted Huffman tree we need from bottom up
        HashMap<String,Character> decoder = new HashMap<String,Character>();

        GraphViz gv = new GraphViz();
        gv.addln(gv.start_graph());

        // Bottom up approach to creating a Canonical Huffman tree.
        int position = 0;
        for(int i = numberOfChars-1; i >= 0; --i)
        {
            TreeNode nodeToAdd = queueArray[i];
            TreeNode nextNode = i > 0 ? queueArray[i-1] : null;
            String directionStr = "";
            String unpaddedBinary = Integer.toBinaryString(position); // Gives us the canonical representation of the char
            for(int x = 0; x < nodeToAdd.m_Depth-unpaddedBinary.length(); ++x) // Expected depth - unpadded length = padding
            {
                directionStr += "0"; // Padding for the binary code.
            }
            directionStr += unpaddedBinary; // Binary representation of the code we need
            decoder.put(directionStr,nodeToAdd.m_Char);
            if (nextNode != null)
            {
                int newPosition = (position+1);
                int offset = nodeToAdd.m_Depth - nextNode.m_Depth;
                position = newPosition >> offset; // Binary representation of the next letter in the tree.
            }
            int directionBits = directionStr.length();
            TreeNode cur = root;
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
                        if (nodeToAdd.m_Char == '\u0000')
                        {
                            gv.addln(cur.m_ID + " -> " + "EOF");
                        }
                        else if(nodeToAdd.m_Char == '\n')
                        {
                            gv.addln(cur.m_ID + " -> " + "NewLine");
                            gv.addln("NewLine [label=\"\\\\n\"]");
                        }
                        else if(nodeToAdd.m_Char == '\\')
                        {
                            gv.addln(cur.m_ID + " -> " + "BackSlash");
                            gv.addln("BackSlash [label=\"\\\\\"]");
                        }
                        else if(nodeToAdd.m_Char == '\'')
                        {
                            gv.addln(cur.m_ID + " -> " + "SingleQuote");
                            gv.addln("SingleQuote [label=\"\\\'\"]");
                        }
                        else if(nodeToAdd.m_Char == '\"')
                        {
                            gv.addln(cur.m_ID + " -> " + "Quote");
                            gv.addln("Quote [label=\"\\\"\"]");
                        }
                        else
                        {
                            gv.addln(cur.m_ID + " -> " + "\"" + nodeToAdd.m_Char + "\"");
                        }
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
                        gv.addln(cur.m_ID + " -> " + examineNode.m_ID);
                        cur = examineNode;
                    }
                }
                else
                {
                    cur = examineNode;
                }
            }
        }


        gv.addln(gv.end_graph());
        if(outputGraphFilePath.length() > 0)
        {
            WriteGraphFile(gv, outputGraphFilePath);
        }

        String decodedMsg = "";
        int dataIndex = (numberOfChars*2)+1;
        if(dataIndex > binary.length)
        {
            System.out.println("Input data was not found in binary.");
            return;
        }
        //int[] data = new int[(binary.length - dataIndex)*8];
        //int dataPos = 0;
        String directionStr = "";
        for(; dataIndex < binary.length; ++dataIndex)
        {
            byte valByte = binary[dataIndex];
            for(int i = 0; i < 8; ++i)
            {
                int valInt = valByte>>(7-i) & 0x0001;
                //data[dataPos++] += valInt;
                directionStr += valInt;
                if(decoder.containsKey(directionStr))
                {
                    char result = decoder.get(directionStr);
                    if(result == '\u0000')
                    {
                        break;
                    }
                    decodedMsg += result;
                    directionStr = "";
                }
            }
        }

        //decodedMsg = GetDecodedMessageFromTree(root, data);
        System.out.println(decodedMsg);
        PrintWriter writer = new PrintWriter(outputFilePath, "UTF-8");
        writer.print(decodedMsg);
        writer.close();
    }

    /**
     * OPTIONAL FUNCTION
     * Writes a GraphViz to an image.
     * @param gv graph to write
     * @param path path of the file to write to
     */
    static void WriteGraphFile(GraphViz gv, String path)
    {
        String fileType = path.substring(path.length()-3, path.length());
        File out = new File(path);
        byte[] img = gv.getGraph(gv.getDotSource(), fileType);
        gv.writeGraphToFile( img, out );
        System.out.println(gv.getDotSource());
    }

    /**
     * OPTIONAL FUNCTION
     * Searches the tree and decodes the secret message.
     * @param root Root of the huffman tree
     * @param directions the binary representation of the secret message
     * @return the deciphered message
     */
    static String GetDecodedMessageFromTree(TreeNode root, int[] directions)
    {
        String decodedMsg = "";
        int directionIndex = 0;
        TreeNode curNode = root;
        boolean eof = false;
        while(!eof) {
            while (curNode.m_IsNode)
            {
                int direction = directions[directionIndex];
                if (direction != 1 && direction != 0)
                {
                    System.out.println("Some binary data not 1 or 0 "+ directionIndex );
                }
                if (direction == 0)
                {
                    curNode = curNode.m_Left;
                }
                else if (direction == 1)
                {
                    curNode = curNode.m_Right;
                }
                ++directionIndex;
                if (directionIndex > directions.length)
                {
                    System.out.println("Binary data out of bounds in tree search" + directionIndex);
                    decodedMsg += '!';
                }
            }

            // Found a leaf in our search, make sure its a character and not a node
            if (!curNode.m_IsNode)
            {
                // Test for EOF
                if(curNode.m_Char == '\u0000')
                {
                    eof = true;
                }
                else // not eof, append to our decoded msg
                {
                    decodedMsg += curNode.m_Char;
                }
            }
            else // we ended on a node? impossible?
            {
                System.out.println("Search tree ended in not a node " + directionIndex);
                decodedMsg += '!';
            }
            curNode = root; // reset out search, won't matter if we hit EOF
        }
        return decodedMsg;
    }

}