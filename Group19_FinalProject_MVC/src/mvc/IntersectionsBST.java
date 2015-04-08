// CS 2XB3 Lab 2 - Final Project
// Hassaan Malik -
// Trevor Rae - 
// Paul Warnick - 1300963

/*
 * Description:
 * 
 */

package mvc;

import java.util.Arrays;

public class IntersectionsBST {
 
    public Node root;
    public int closestNode = -1;
 
    public class Node {//ADT for SelfBalancingTree
        int key;
        double[] value;
        public int balance;
        public Node left, right, parent;
 
        Node(int node,double[] val, Node p) {
            key = node;
            value = val;
            parent = p;
        }
    }
 
    public double[] search(int key){
    	Node node = root;
		while(node!=null)
		{
			if(node.key>key) node=node.left;
			else if(node.key<key) node=node.right;
			else{
				return node.value;
			}
		}

		return null;    	
    }
    
    public void findNodeID(Node focusNode, double xCoord, double yCoord) {    	
    	if (focusNode != null) {
    		findNodeID(focusNode.left, xCoord, yCoord);
    		if ((focusNode.value[0] == xCoord) && (focusNode.value[1] == yCoord)) { closestNode = (focusNode.key); }
    		findNodeID(focusNode.right, xCoord, yCoord);
    	}
    }
    
    public boolean insert(int key,double[] value) {//inserts a node into the Tree
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
 
    public void rebalance(Node n) {//reorganizes the tree using left and right rotations until tree is balanced
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
 
    public Node rotateLeft(Node a) {//rotates to the Tree left 
 
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
 
    public Node rotateRight(Node a) {//rotates to the Tree right 
 
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
 
    public Node rotateLeftThenRight(Node n) {
        n.left = rotateLeft(n.left);
        return rotateRight(n);
    }
 
    public Node rotateRightThenLeft(Node n) {
        n.right = rotateRight(n.right);
        return rotateLeft(n);
    }
 
    public int height(Node n) {
        if (n == null)
            return -1;
        return 1 + Math.max(height(n.left), height(n.right));
    }
 
    public void setBalance(Node... nodes) {
        for (Node n : nodes)
            n.balance = height(n.right) - height(n.left);
    }

    static Node[] all = new Node[0];
    
    public static Node[] printBalance(Node n) {//This method returns an array of locations that are less than 100 km 
        if (n != null) {
            printBalance(n.left);
            if(n.key <= 100){
            	all = Arrays.copyOf(all, all.length+1);
            	all[all.length-1] = n;
            }
            printBalance(n.right);
        }
        return all;
    }
}