// CS 2XB3 Lab 2 - Final Project
// Hassaan Malik -
// Trevor Rae - 
// Paul Warnick - 1300963

/*
 * Description:
 * 
 */

package mvc;

import java.awt.EventQueue;

public class Main {
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					View noLeftTurnRouteApplication = new View();
					noLeftTurnRouteApplication.window.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}

