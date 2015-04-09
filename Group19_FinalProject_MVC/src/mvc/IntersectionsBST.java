// CS 2XB3 Lab 2 - Final Project
// Hassaan Malik - 1224997
// Trevor Rae - 1324949
// Paul Warnick - 1300963

/*
 * Description:
 * 
 * This class is used to create a self balancing binary search tree that stores all the values associated with the intersections
 * of the map. The keys of the BST are intersection ID's (each intersection it's own identifier) and the values are X/Y coordinates on a plane.
 * The use of a BST allows for easy and quick locating of intersection information.
 */

package mvc; // package name

public class IntersectionsBST {
 
    public Node root; // root value (top of the tree)
    public int closestNode = -1; // used in finding the node ID associated with a set of values
 
    public class Node { // creates the node ADT to hold all information
        int key;
        double[] value;
        public int balance;
        public Node left, right, parent;
 
        Node(int node, double[] val, Node p) { // constructor to create a node based on given values
            key = node;
            value = val;
            parent = p;
        }
    }
 
    public double[] search(int key) { // returns an array containing the XY coordinates of an intersection given an intersection ID
    	Node node = root; // used to set the currently searched node to the root of the current BST 
    	
		while (node != null) // runs until the bottom of the brach has been reached (no more nodes to search)
		{
			if (node.key > key) { node=node.left; } // searches the left subtree if the given node is less than the current
			else if (node.key < key) { node = node.right; } // if it's larger
			else { return (node.value); } // if the value is found
		}
		return null; // if the value is not found
    }
    
    public void findNodeID(Node focusNode, double xCoord, double yCoord) { // used to find the an intersection ID associated with XY coordinates    	
    	if (focusNode != null) { // checks to make sure the bottom of the tree hasn't been reached
    		findNodeID(focusNode.left, xCoord, yCoord); // recursively checks left subtrees
    		if ((focusNode.value[0] == xCoord) && (focusNode.value[1] == yCoord)) { closestNode = (focusNode.key); } // checks if every value of the tree is equal to the given value
    		findNodeID(focusNode.right, xCoord, yCoord); // recursively checks the right subtrees
    	}
    }
    
    // ================================================================ //
    
    // The below code is from the CS 2C03 textbook for Red-Black BSTs   //
    
    // ================================================================ //

    public boolean insert(int key,double[] value) { // inserts a node into the Tree
        if (root == null)
            root = new Node(key,value, null);
        else {
            Node n = root;
            Node parent;
            while (true) {
                if (n.key == key)
                    return false;
 
                parent = n;
 
                boolean goLeft = n.key > key;
                n = goLeft ? n.left : n.right;
 
                if (n == null) {
                    if (goLeft) {
                        parent.left = new Node(key,value, parent);
                    } else {
                        parent.right = new Node(key,value, parent);
                    }
                    rebalance(parent);
                    break;
                }
            }
        }
        return true;
    }

    public void rebalance(Node n) { //reorganizes the tree using left and right rotations until tree is balanced
        setBalance(n);
 
        if (n.balance == -2) {
            if (height(n.left.left) >= height(n.left.right))
                n = rotateRight(n);
            else
                n = rotateLeftThenRight(n);
 
        } else if (n.balance == 2) {
            if (height(n.right.right) >= height(n.right.left))
                n = rotateLeft(n);
            else
                n = rotateRightThenLeft(n);
        }
 
        if (n.parent != null) {
            rebalance(n.parent);
        } else {
            root = n;
        }
    }

    public Node rotateLeft(Node a) { //rotates to the Tree left 
 
        Node b = a.right;
        b.parent = a.parent;
 
        a.right = b.left;
 
        if (a.right != null)
            a.right.parent = a;
 
        b.left = a;
        a.parent = b;
 
        if (b.parent != null) {
            if (b.parent.right == a) {
                b.parent.right = b;
            } else {
                b.parent.left = b;
            }
        }
 
        setBalance(a, b);
 
        return b;
    }
 
    public Node rotateRight(Node a) { //rotates to the Tree right 
 
        Node b = a.left;
        b.parent = a.parent;
 
        a.left = b.right;
 
        if (a.left != null)
            a.left.parent = a;
 
        b.right = a;
        a.parent = b;
 
        if (b.parent != null) {
            if (b.parent.right == a) {
                b.parent.right = b;
            } else {
                b.parent.left = b;
            }
        }
 
        setBalance(a, b);
 
        return b;
    }
 
    public Node rotateLeftThenRight(Node n) { // used in keeping the tree balanced
        n.left = rotateLeft(n.left);
        return rotateRight(n);
    }
 
    public Node rotateRightThenLeft(Node n) { // same as above
        n.right = rotateRight(n.right);
        return rotateLeft(n);
    }
 
    public int height(Node n) { // same as above
        if (n == null)
            return -1;
        return 1 + Math.max(height(n.left), height(n.right));
    }
 
    public void setBalance(Node... nodes) { // same as above
        for (Node n : nodes)
            n.balance = height(n.right) - height(n.left);
    }
}