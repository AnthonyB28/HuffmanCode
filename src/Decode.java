/**
 * Huffman decoding.
 * Decodes a binary sourcefile into a targetfile.
 * @author Anthony Barranco
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

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
        System.out.println("Hello World decode!");
        byte[] input = ReadFile("src//HexEd2");
        DecodeToFile("", input);
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

    static void DecodeToFile(String path, byte[] binary)
    {
        TreeNode root = new TreeNode(true);
        int numberOfChars = binary[0];
        for(int i = 1; i <= numberOfChars*2; ++i)
        {
            int charNum = binary[i];
            char val = (char) charNum;
            ++i;
            int length = binary[i];
            TreeNode curNode = root;
            for(int depth = 0; depth < length; ++depth)
            {
                // At depth where we want our node
                if(depth == length-1)
                {
                    if(curNode.m_Left == null)
                    {
                        curNode.m_Left = new TreeNode(false);
                        curNode.m_Left.m_Char = val;
                    }
                    else if(curNode.m_Right == null)
                    {
                        // Make sure we're canonical
                        if(!curNode.m_Left.m_IsNode)
                        {
                            if(curNode.m_Left.m_Char < val) // Keep smaller on the left
                            {
                                curNode.m_Right = new TreeNode(false);
                                curNode.m_Right.m_Char = val;
                            }
                            else // Left is bigger than us, put it on the right
                            {
                                curNode.m_Right = curNode.m_Left;
                                curNode.m_Left = new TreeNode(false);
                                curNode.m_Left.m_Char = val;
                            }
                        }
                        else
                        {
                            curNode.m_Right = new TreeNode(false);
                            curNode.m_Right.m_Char = val;
                        }
                    }
                }
                else // Keep traversing
                {
                    if (curNode.m_Left != null)
                    {
                        if(!curNode.m_Left.m_IsNode)
                        {
                            System.out.println("Entered node that had a value:" + val);
                            return;
                        }
                        curNode = curNode.m_Left;
                    }
                    else
                    {
                        curNode.m_Left = new TreeNode(true);
                        curNode = curNode.m_Left;
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
        ArrayList directionsToDecode = new ArrayList();
        for(; dataIndex < binary.length-1; ++dataIndex)
        {
            //TODO
        }
    }

    //TODO
    static char SearchTree(TreeNode root, byte[] binary, int dataIndex)
    {
        TreeNode curNode = root;
        while(curNode.m_IsNode)
        {
            int direction = binary[dataIndex];
            if(direction != 1 || direction != 0)
            {
                System.out.println("Some binary data not 1 or 0");
            }
            if(direction == 0)
            {
                curNode = curNode.m_Left;
            }
            else if(direction == 1)
            {
                curNode = curNode.m_Right;
            }
            ++dataIndex;
            if(dataIndex > binary.length)
            {
                System.out.println("Binary data out of bounds in tree search");
                return 'a';
            }
        }

        return 'a';
    }

}