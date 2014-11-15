/**
 * Huffman decoding.
 * Decodes a binary sourcefile into a targetfile.
 * @author Anthony Barranco
 */

import sun.reflect.generics.tree.Tree;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Decode2
{

    /* Usage:
        SOURCEFILE TARGETFILE
        decode the binary file sourcefile and save the character data to targetfile

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

    /**
     * Input for program
     * @param args <sourcefile targetfile> Input source, output target
     */
    public static void main(String[] args) throws IOException
    {
        if(args.length == 2)
        {
            byte[] input2 = ReadFile(args[0]);
            DecodeToFile(args[1], input2);
        }
        else
        {
            System.out.println("Please provide sourcefile and targetfile");
            byte[] input = ReadFile("samples//encoded//sample7.huf");
            DecodeToFile("test//output.txt", input);
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
     * @param binary the binary file representation of the huffman code and secret message
     */
    static void DecodeToFile(String outputFilePath, byte[] binary) throws IOException
    {
        int numberOfChars = binary[0];
        Comparator<TreeNode> compare = new Comparator<TreeNode>() {
            public int compare(TreeNode n1, TreeNode n2) {
                if(n1.m_Depth != n2.m_Depth) {
                    return n1.m_Depth < n2.m_Depth ? -1 : 1;
                }
                else
                {
                    return n1.m_Char < n2.m_Char ? 1 : -1;
                }
            }};
        PriorityQueue<TreeNode> q = new PriorityQueue<TreeNode>(numberOfChars,compare);
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
/*        for(int i = 0; i < numberOfChars-1; ++i)
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

        int position = 0;
        TreeNode root = new TreeNode(true, -1);
        TreeNode[] ar = new TreeNode[q.size()];
        q.toArray(ar);
        Arrays.sort(ar, compare);
        HashMap<String,Character> decoder = new HashMap<String,Character>();
        for(int i = numberOfChars-1; i >= 0; --i)
        {
            TreeNode node = ar[i];
            TreeNode nextNode = i > 0 ? ar[i-1] : null;
            String converted = "";
            String binaryString = Integer.toBinaryString(position);
            for(int x = 0; x < node.m_Depth-binaryString.length(); ++x)
            {
                converted += "0";
            }
            converted += binaryString;
            decoder.put(converted,node.m_Char);
            if (nextNode != null)
            {
                int newPosition = (position+1);
                int offset = node.m_Depth - nextNode.m_Depth;
                position = newPosition >> offset;
            }
            int length = converted.length();
            TreeNode cur = root;
            for(int bit = 0; bit < length; ++ bit)
            {
                if(converted.charAt(bit) == '0')
                {
                    if(cur.m_Left == null)
                    {
                        if(bit == length-1)
                        {
                            cur.m_Left = node;
                        }
                        else
                        {
                            cur.m_Left = new TreeNode(true, 0);
                            cur = cur.m_Left;
                        }
                    }
                    else
                    {
                        cur = cur.m_Left;
                    }
                }
                else
                {
                    if (cur.m_Right == null)
                    {
                        if ( bit == length - 1)
                        {
                            cur.m_Right = node;
                        }
                        else
                        {
                            cur.m_Right = new TreeNode(true, 0);
                            cur = cur.m_Right;
                        }
                    }
                    else
                    {
                        cur = cur.m_Right;
                    }
                }
            }
        }

        String decodedMsg = "";
        int dataIndex = (numberOfChars*2)+1;
        if(dataIndex > binary.length)
        {
            System.out.println("Input data was not found in binary.");
            return;
        }
        //int[] data = new int[(binary.length - dataIndex)*8];
        int dataPos = 0;
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