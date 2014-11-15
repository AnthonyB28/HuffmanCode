import sun.reflect.generics.tree.Tree;

/**
 * Representation of a node in the Huffmantree.
 *
 * @author Anthony Barranco
 */

public class TreeNode {

    public boolean m_IsNode;
    public TreeNode m_Left = null;
    public TreeNode m_Right = null;
    public char m_Char;
    public int m_Depth;

    public TreeNode(boolean node, int depth)
    {
        m_IsNode = node;
        m_Depth = depth;
    }
}
