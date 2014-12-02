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

import sun.reflect.generics.tree.Tree;

import java.io.*;
import java.lang.reflect.Array;
import java.math.BigInteger;
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
            if(args[0] == "-c")
            {
                String input2 = ReadFile(args[2]);
                EncodeToFile(args[3], args[1], input2);
            }
            else
            {
                String input2 = ReadFile(args[0]);
                EncodeToFile(args[1], "", input2);
            }
        }
        else
        {
            System.out.println("Please provide sourcefile and targetfile, or optionally sourcefile");
            String input = ReadFile("samples//text//sample5.txt");
            EncodeToFile("output//outputencode.txt", "output//graphencode.gv", input);
        }

        Decode.main(new String[0]);
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
     * @param message the secret message to encode to a binary file
     */
    static void EncodeToFile(String outputFilePath, String outputGraphFilePath, String message) throws IOException
    {
        message += '\u0000';
        PriorityQueue<TreeNode> q = GetSortedQueue(message);

        int numOfChars = q.size();
        TreeNode[] queueArray = new TreeNode[q.size()];
        q.toArray(queueArray);
        //Arrays.sort(queueArray, TreeNode.CanonicalCompare); // Gives us the final sorted Huffman tree we need from bottom up

        for(int i = 0; i < numOfChars-1; ++i)
        {
            TreeNode node = new TreeNode(true, 0);
            node.m_Char = (char) 127;
            TreeNode x = q.poll();
            TreeNode y = q.poll();
            if(!x.m_IsNode)
            {
                if (x.m_Depth > y.m_Depth) {
                    node.m_Left = x;
                    node.m_Right = y;
                } else if (x.m_Depth == y.m_Depth && x.m_Char < y.m_Char) {
                    node.m_Left = x;
                    node.m_Right = y;
                } else {
                    node.m_Left = y;
                    node.m_Right = x;
                }
            }
            else
            {
                node.m_Left = x;
                node.m_Right = y;
            }
            node.m_Depth = x.m_Depth + y.m_Depth;
            q.add(node);
        }
        TreeNode root = q.poll();
        root.m_Depth = -1;
        root.m_ID = "Root";

        GraphViz gv = new GraphViz();
        gv.addln(gv.start_graph());

        ByteArrayOutputStream s = new ByteArrayOutputStream();
        s.write(numOfChars);
        HashMap<Character,String> decoder = new HashMap<Character,String>();
        for(int i = 0; i < queueArray.length; ++i)
        {
            s.write((int)queueArray[i].m_Char);
            String code = binarySearch(queueArray[i].m_Char, root, "");
            s.write(code.length());
            decoder.put(queueArray[i].m_Char, code);
        }

        String encodedMsg = "";
        int msgLength = message.length();
        for(int i = 0; i < msgLength; ++i)
        {
            String encode = decoder.get(message.charAt(i));
            encodedMsg += encode;
        }

        if(encodedMsg.length() % 8 != 0)
        {
            while(encodedMsg.length() % 8 != 0)
            {
                encodedMsg += "0";
            }
        }

        for(int i = 0; i < encodedMsg.length()/8; ++i)
        {
            s.write(Integer.parseInt(encodedMsg.substring(i*8,i*8+8),2));
        }

        System.out.println("Encoded msg: " + encodedMsg);
        Files.write(Paths.get(outputFilePath), s.toByteArray());
    }

    static PriorityQueue<TreeNode> GetSortedQueue(String message)
    {
        HashMap<Character,Integer> frequencies = new HashMap<Character, Integer>();

        int length = message.length();
        for(int i = 0; i < length; ++i)
        {
            char letter = message.charAt(i);
            if(frequencies.containsKey(letter))
            {
                frequencies.put(letter, frequencies.get(letter)+1);
            }
            else
            {
                frequencies.put(letter, 1);
            }
        }

        PriorityQueue<TreeNode> q = new PriorityQueue<TreeNode>(TreeNode.CanonicalCompareEncode);
        for (Map.Entry<Character, Integer> entry : frequencies.entrySet())
        {
            TreeNode n = new TreeNode(false, entry.getValue());
            n.m_Char = entry.getKey();
            q.add(n);
        }
        //Collections.sort(nodeList, TreeNode.CanonicalCompareEncode);

        //Queue<TreeNode> q = nodeList;
        return q;
    }

    /**
     * OPTIONAL FUNCTION
     * Writes a GraphViz source to a file
     * @param gv graph to write
     * @param path path of the file to write to
     */
    static void WriteGraphSource(GraphViz gv, String path) throws FileNotFoundException, UnsupportedEncodingException
    {
        PrintWriter writer = new PrintWriter(path, "UTF-8");
        writer.print(gv.getDotSource());
        writer.close();
    }

    /**
     * OPTIONAL FUNCTION
     * Writes a GraphViz to an image.
     * @param gv graph to write
     * @param path path of the file to write to
     */
    static void WriteGraphImageFile(GraphViz gv, String path)
    {
        String fileType = path.substring(path.length()-3, path.length());
        File out = new File(path);
        byte[] img = gv.getGraph(gv.getDotSource(), fileType);
        gv.writeGraphToFile( img, out );
        System.out.println(gv.getDotSource());
    }


    static String binarySearch(char toFind, TreeNode node, String directions)
    {
        if(!node.m_IsNode)
        {
            if(node.m_Char == toFind)
            {
                return directions;
            }
            else
            {
                return null;
            }
        }
        else
        {
            if(node.m_Left != null)
            {
                String result = binarySearch(toFind, node.m_Left, directions+"0");
                if(result != null)
                {
                    return result;
                }
            }
            if(node.m_Right != null)
            {
                String result = binarySearch(toFind, node.m_Right, directions + "1");
                if (result != null) {
                    return result;
                }
            }
            return null;
        }
    }

}