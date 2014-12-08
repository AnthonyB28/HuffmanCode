/**
 * Representation of a node in the Huffmantree.
 * @author Anthony Barranco
 */

import java.util.Comparator;

public class TreeNode {

    public boolean m_IsNode; // If this node is not a leaf, is true.
    public TreeNode m_Left = null;
    public TreeNode m_Right = null;
    public TreeNode m_Prev = null; // Used for old decoding
    public String m_ID = ""; // GraphViz ID, also the binary representation of the node
    public char m_Char;
    public int m_Depth;

    public TreeNode(boolean node, int depth)
    {
        m_IsNode = node;
        m_Depth = depth;
    }

    // Sorts TreeNodes based on canonical code length or ties with lexi order
    public static Comparator<TreeNode> CanonicalCompare = new Comparator<TreeNode>() {
        public int compare(TreeNode n1, TreeNode n2) {
            if(n1.m_Depth != n2.m_Depth) {
                return n1.m_Depth < n2.m_Depth ? -1 : 1;
            }
            else
            {
                return n1.m_Char < n2.m_Char ? 1 : -1;
            }
        }};

    // Sorts TreeNodes based on canonical code length or ties with lexi order
    public static Comparator<TreeNode> CanonicalCompareEncode = new Comparator<TreeNode>() {
        public int compare(TreeNode n1, TreeNode n2) {
            if(n1.m_Depth != n2.m_Depth) {
                return n1.m_Depth < n2.m_Depth ? -1 : 1;
            }
            else
            {
                return n1.m_Char < n2.m_Char ? -1 : 1;
            }
        }};

    public void GraphVizLabel(GraphViz gv, String parentID)
    {
        if (m_Char == '\u0000')
        {
            gv.addln(parentID + " -> " + "EOF");
        }
        else if(m_Char == '\n')
        {
            gv.addln(parentID + " -> " + "NewLine");
            gv.addln("NewLine [label=\"\\\\n\"]");
        }
        else if(m_Char == '\\')
        {
            gv.addln(parentID + " -> " + "BackSlash");
            gv.addln("BackSlash [label=\"\\\\\"]");
        }
        else if(m_Char == '\'')
        {
            gv.addln(parentID + " -> " + "SingleQuote");
            gv.addln("SingleQuote [label=\"\\\'\"]");
        }
        else if(m_Char == '\"')
        {
            gv.addln(parentID + " -> " + "Quote");
            gv.addln("Quote [label=\"\\\"\"]");
        }
        else
        {
            gv.addln(parentID + " -> " + "\"" + m_Char + "\"");
        }
    }
}
