// CS 2XB3 Lab 2 - Final Project
// Hassaan Malik - 1224997
// Trevor Rae - 1324949
// Paul Warnick - 1300963

/*
 * Description:
 * 
 * Used as a helper class for Edge Weighted Graph to create a collection of all the edges in the graph.
 */

package graph;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class Bag<Item> implements Iterable<Item> {
    private int N;               // number of elements in bag
    private Node<Item> first;    // beginning of bag

    // helper linked list class
    private static class Node<Item> {
        private Item item;
        private Node<Item> next;
    }
    
    public Bag() {
        first = null;
        N = 0;
    }

    public boolean isEmpty() {
        return first == null;
    }

    public int size() {
        return N;
    }

    public void add(Item item) {
        Node<Item> oldfirst = first;
        first = new Node<Item>();
        first.item = item;
        first.next = oldfirst;
        N++;
    }

    public Iterator<Item> iterator()  {
        return new ListIterator<Item>(first);  
    }

    @SuppressWarnings("hiding")
	private class ListIterator<Item> implements Iterator<Item> {
        private Node<Item> current;

        public ListIterator(Node<Item> first) {
            current = first;
        }

        public boolean hasNext()  { return current != null;                     }
        public void remove()      { throw new UnsupportedOperationException();  }

        public Item next() {
            if (!hasNext()) throw new NoSuchElementException();
            Item item = current.item;
            current = current.next; 
            return item;
        }
    }
}