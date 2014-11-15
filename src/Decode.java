/**
 * Huffman decoding.
 * Decodes a binary sourcefile into a targetfile.
 * @author Anthony Barranco
 */

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Decode{

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
    public static void main(String[] args) throws IOException {
        byte[] input = ReadFile("samples\\encoded\\sample7.huf");
        DecodeToFile("test//output.txt", input);
        if(args.length == 2)
        {
            byte[] input2 = ReadFile(args[0]);
            DecodeToFile(args[1], input2);
        }
        else
        {
            System.out.println("Please provide sourcefile and targetfile");
        }
    }

    static byte[] ReadFile(String path) throws IOException
    {
        Path p = Paths.get(path);
        return Files.readAllBytes(p);
    }

    static void DecodeToFile(String outputFilePath, byte[] binary) throws IOException
    {
        TreeNode root = new TreeNode(true, -1);
        int numberOfChars = binary[0];
        for(int i = 1; i <= numberOfChars*2; ++i)
        {
            int charNum = binary[i];
            char val = (char) charNum;
            ++i;
            int length = binary[i];
            TreeNode curNode = root;
            boolean visitedPrev = false;
            boolean rightTree = false;
            for(int depth = 0; depth < length; ++depth)
            {
                // At depth where we want our node
                if(depth == length-1)
                {
                    if(curNode.m_Left == null)
                    {
                        curNode.m_Left = new TreeNode(false, depth);
                        curNode.m_Left.m_Prev = curNode;
                        curNode.m_Left.m_Char = val;
                    }
                    else if(curNode.m_Right == null)
                    {
                        // Make sure we're canonical
                        if(!curNode.m_Left.m_IsNode)
                        {
                            if(curNode.m_Left.m_Char < val) // Keep smaller on the left
                            {
                                curNode.m_Right = new TreeNode(false, depth);
                                curNode.m_Right.m_Prev = curNode;
                                curNode.m_Right.m_Char = val;
                            }
                            else // Left is bigger than us, put it on the right
                            {
                                curNode.m_Right = curNode.m_Left;
                                curNode.m_Left = new TreeNode(false, depth);
                                curNode.m_Prev = curNode;
                                curNode.m_Left.m_Char = val;
                            }
                        }
                        else
                        {
                            curNode.m_Right = new TreeNode(false, depth);
                            curNode.m_Right.m_Prev = curNode;
                            curNode.m_Right.m_Char = val;
                        }
                        rightTree = false;
                    }
                    else
                    {
                        rightTree = true;

                        curNode = (curNode.m_Prev == null) ? curNode.m_Right : curNode.m_Prev;
                        depth = curNode.m_Depth;
                        if (visitedPrev) {
                            depth += 1;
                            visitedPrev = false;
                        } else {
                            visitedPrev = true;
                        }

                    }
                }
                else // Keep traversing
                {
                    TreeNode nodeToEvaluate = rightTree ? curNode.m_Right : curNode.m_Left;
                    if (nodeToEvaluate != null)
                    {
                        if(!nodeToEvaluate.m_IsNode)
                        {
                            curNode.m_Right = nodeToEvaluate;
                            curNode.m_Left = new TreeNode(true, nodeToEvaluate.m_Depth);
                            curNode.m_Left.m_Prev = curNode;
                            System.out.println("Entered node that had a value:" + val);
                            curNode = curNode.m_Left;
                        }
                        else
                        {
                            curNode = nodeToEvaluate;
                        }
                    }
                    else
                    {
                        nodeToEvaluate = new TreeNode(true, depth);
                        nodeToEvaluate.m_Prev = curNode;
                        if(!rightTree)
                        {
                            curNode.m_Left = nodeToEvaluate;
                        }
                        else
                        {
                            curNode.m_Right = nodeToEvaluate;

                                System.out.println("Right tree stop turn");
                        }
                        curNode = nodeToEvaluate;
                    }
                    rightTree = false;

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
        int[] data = new int[(binary.length - dataIndex)*8];
        int dataPos = 0;
        for(; dataIndex < binary.length; ++dataIndex)
        {
            byte valByte = binary[dataIndex];
            for(int i = 0; i < 8; ++i)
            {
                int valInt = valByte>>(7-i) & 0x0001;
                data[dataPos++] += valInt;
            }
        }
        decodedMsg = GetDecodedMessage(root, data);
        System.out.println(decodedMsg);
        PrintWriter writer = new PrintWriter(outputFilePath, "UTF-8");
        writer.print(decodedMsg);
        writer.close();
    }

    static String GetDecodedMessage(TreeNode root, int[] directions)
    {
        String decodedMsg = "";
        int directionIndex = 0;
        TreeNode curNode = root;
        boolean eof = false;
        while(!eof) {
            while (curNode.m_IsNode) {
                int direction = directions[directionIndex];
                if (direction != 1 && direction != 0) {
                    System.out.println("Some binary data not 1 or 0 "+ directionIndex );
                }
                if (direction == 0) {
                    curNode = curNode.m_Left;
                } else if (direction == 1) {
                    curNode = curNode.m_Right;
                }
                ++directionIndex;
                if (directionIndex > directions.length) {
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