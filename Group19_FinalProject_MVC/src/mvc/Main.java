// CS 2XB3 Lab 2 - Final Project
// Hassaan Malik - 1224997
// Trevor Rae - 1324949
// Paul Warnick - 1300963

/*
 * Description:
 * 
 * The main class to run MVC, creating the mapping application and displaying it to the screen for the user. 
 */

package mvc; // package name

import java.awt.EventQueue; // java import

public class Main {
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@SuppressWarnings("static-access")
			@Override
			public void run() {
				try { // tries to run the application
					View noLeftTurnRouteApplication = new View(); // creates the mapping application object
					noLeftTurnRouteApplication.window.setVisible(true); // sets the window to visible
				} catch (Exception e) { // if an error occurs, catch the program
					e.printStackTrace();
				}
			}
		});
	}
}

