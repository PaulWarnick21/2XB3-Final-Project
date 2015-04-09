// CS 2XB3 Lab 2 - Final Project
// Hassaan Malik - 1224997
// Trevor Rae - 1324949
// Paul Warnick - 1300963

/*
 * Description:
 * The controller class is used to; receive, interpret & validate input, create & update views, 
 * query & modify models. Here the controller runs everything need in calculating the quickest route 
 * and determining if a left turn is present. Also controlling which actions take place upon finding a left turn
 * and lastly minor features like conversion of data for view plus finding which intersection is closest to the
 * users click etc.
 * 
 * See (for diagram of MVC): http://1.bp.blogspot.com/-GMvBz2taYH8/UL4v-8e51HI/AAAAAAAAAFk/RnpdpsNOhjY/s1600/mvc_role_diagram.png
 */

package mvc; // package name

// imports for back end graph mapping and shortest path calculations
import dijkstra.DijkstraSP;
import graph.Edge;
import graph.EdgeWeightedGraph;

// standard java imports
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

// swing imports
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;

// arcGIS imports
import com.esri.toolkit.overlays.DrawingOverlay;
import com.esri.toolkit.overlays.DrawingOverlay.DrawingMode;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol.Style;
import com.esri.core.tasks.na.NAFeaturesAsFeature;
import com.esri.map.GraphicsLayer;

public class Controller {
	private static DijkstraSP shortestPathTree; // a shortest path tree for a given vertex (intersection)created below
	private static Edge[][] shortestPath; // an array to hold all routes in the trip
	
	public static DrawingOverlay myDrawingOverlay; // an overlay for used by View to draw the stops to the map
	public static GraphicsLayer stopsLayer; // a graphics layer holding all the stops the user has selected
	public static GraphicsLayer routeLayer; // a graphics layer to hold the route between each stop
	public static GraphicsLayer streetsLayer; // a graphics layer to hold the streets (edges of the graph)
	public static NAFeaturesAsFeature stops = new NAFeaturesAsFeature(); // an esri feature to hold each stops information
	
	// used to create the JButtons with strings 
	public static JButton startPointButton;
	public static JButton stopButton;
	public static JButton destinationButton;
	public static JButton solveRouteButton;
	private static final String STARTPOINT_BUTTON = " Choose Start Point ";
	private static final String STOP_BUTTON = " Add a Stop ";
	private static final String DESTINATION_BUTTON = " Choose Destination ";
	private static final String SOLVE_BUTTON = " Solve route ";
	private static final String RESET_BUTTON = " Reset ";
	
	public static int stopCounter = 0; // int to keep track of the number of stops that haven been placed
	public static boolean stopHasBeenClicked = false; // used in printing specific stop symbols to the map (keeps track of if the add stop button has been clicked)
	public static boolean destinationHasBeenClicked = false; // same but for destination button
	public static int[] stopArray = new int[1]; // used to store the XY coords of each stop the user wants to visit in order ([0] being the start)
	private static final String	STARTPOINT_IMAGE = "http://www.tactranconnect.com/images/icon_start.png"; // url for start image
	private static final String STOP_IMAGE = "http://www.tactranconnect.com/images/mapicons/marker_incidents.png"; // url for stop image
	private static final String	DESTINATION_IMAGE = "http://www.tactranconnect.com/images/icon_end.png"; // url for destination image
	private static PictureMarkerSymbol startSymbol = new PictureMarkerSymbol(STARTPOINT_IMAGE); // creates a symbol with the start point url
	private static PictureMarkerSymbol stopSymbol = new PictureMarkerSymbol(STOP_IMAGE); // creates a symbol with the start point url
	private static PictureMarkerSymbol destinationSymbol = new PictureMarkerSymbol(DESTINATION_IMAGE); // same for destination
	
	// determines the closest intersection to where the user has clicked
	public static double[] getClosestIntersection(double esriLat, double esriLong) {
		double[] closestIntersection = new double[2]; // stores the closest intersection if found
		double distance = 0; // counter to compare distances
		double tempDistance = Double.POSITIVE_INFINITY; // starts the distance at max value
		double [] esriCoords = { -esriLat, esriLong }; // stores the users click coords into an array (in esri meters at the moment)
		esriCoords[0] = convertFromEsriMeters(esriCoords)[0]; // converts to lat and long
		esriCoords[1] = convertFromEsriMeters(esriCoords)[1]; // same as above		
		
		for (int i = 0; i < Model.xyCoordinates.length; i++) { // runs through each intersection to find which is closest to the user clicked point
			distance = (Math.sqrt((Math.pow((esriCoords[0] - (Model.esriIntersectionCoordinates[i][0])),2)) + (Math.pow(esriCoords[1] - (Model.esriIntersectionCoordinates[i][1]),2)))); // formula used to calculate the distance between intersections
			if (distance < tempDistance) { // determines which is the smallest distance and therefore the closest intersection
				tempDistance = distance;
				closestIntersection[0] = Model.esriIntersectionCoordinates[i][0];
				closestIntersection[1] = Model.esriIntersectionCoordinates[i][1];
			}
		}
		
		if (tempDistance > 0.07) { // if the selected point is to far off the map / away from an intersection an warning is displayed
		 JOptionPane.showMessageDialog(View.window,
				 "One of the locations you've selected is to far away from the map! Please pick another",
				 "Warning",
				 JOptionPane.WARNING_MESSAGE);				 
		 double[] error = { -1.0, -1.0 }; // returns -1, -1 to represent an error
		 return (error);
		}
	  
		return closestIntersection; // returns the closest intersection
	}
	
	// concatenates to arrays to add a stop (in the form of XY coords) to the current array of stops
	public static void addToStops(int[] currentStopArray, int[] toBeAdded) {
		   int currentStopArrayLength = currentStopArray.length; // sets current length
		   int toBeAddedLenth = toBeAdded.length; // sets length of array to be added
		   stopArray = new int[currentStopArrayLength + toBeAddedLenth]; // adds lengths to create an array large enough for both
		   System.arraycopy(currentStopArray, 0, stopArray, 0, currentStopArrayLength); // copies the old array
		   System.arraycopy(toBeAdded, 0, stopArray, currentStopArrayLength, toBeAddedLenth); // adds the new array
	}
	
	// creates the tool bar containing the buttons
	public static Component createToolBar(DrawingOverlay drawingOverlay) {
		JToolBar toolBar = new JToolBar();
		toolBar.setLayout(new FlowLayout(FlowLayout.CENTER)); // sets layout
		toolBar.setFloatable(false); // disable the ability to move the tool bar
		
		// add Start Point button
		startPointButton = new JButton(STARTPOINT_BUTTON); // sets the string
		startPointButton.addActionListener(new ActionListener() { // a new action listener to check if an action has been preformed
			@Override
			public void actionPerformed(ActionEvent e) {
				startSymbol.setOffsetY((float) 16); // shifts the picture upwards
				HashMap<String, Object> attributes = new HashMap<String, Object>();
			    attributes.put("type", "Start");
			    drawingOverlay.setUp(
			        DrawingMode.POINT,
			        startSymbol,
			        attributes); // sets up the drawing overlay with the correct symbol for the start point
			}
		});
		toolBar.add(startPointButton); // adds to tool bar
		
		// add Stop button
		stopButton = new JButton(STOP_BUTTON);
		stopButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stopHasBeenClicked = true; // used in checking if a stop symbol should be placed upon mouse click
				stopSymbol.setOffsetY((float) 16); // shifts the picture upwards
				HashMap<String, Object> attributes = new HashMap<String, Object>();
			    attributes.put("type", "Stop");
			    drawingOverlay.setUp(
			        DrawingMode.POINT,
			        stopSymbol, // the picture for the stop
			        attributes);
			}
		});
		toolBar.add(stopButton); // adds to tool bar
		stopButton.setEnabled(false); // initially sets to false
		
		// add Destination button
		destinationButton = new JButton(DESTINATION_BUTTON);
		destinationButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				destinationHasBeenClicked = true; // same as above button
				destinationSymbol.setOffsetY((float) 16); // shifts the picture upwards
				HashMap<String, Object> attributes = new HashMap<String, Object>();
			    attributes.put("type", "Destination");
			    drawingOverlay.setUp(
			        DrawingMode.POINT,
			        destinationSymbol, // the picture for the stop
			        attributes);
			    stopButton.setEnabled(false); // disables the add stop button to avoid errors of the user placing more stops, also makes the program easier to follow and use 
			    destinationButton.setEnabled(false); // disable the destination button to show the route planning is over
			}
		});
		toolBar.add(destinationButton); // adds to the tool bar
		destinationButton.setEnabled(false); // disables it initially

		// solve route button
		solveRouteButton = new JButton(SOLVE_BUTTON);
		solveRouteButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// sets all buttons to false except for reset and solves the route
				startPointButton.setEnabled(false);
				stopButton.setEnabled(false);
			    destinationButton.setEnabled(false);
			    solveRouteButton.setEnabled(false);
			    myDrawingOverlay.setEnabled(false);
			    getRoute(Model.graph, stopArray); // calculates the route
			    leftTurnChecker(); // checks the route for left turns and handles them appropriately
			}
		});
		toolBar.add(solveRouteButton); // adds to the tool bar
		solveRouteButton.setEnabled(false); // initially disables the button

		// reset button
		JButton resetButton = new JButton(RESET_BUTTON);
		resetButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// resets all associated variables to reset the route picking process
			    startPointButton.setEnabled(true);
			    stopButton.setEnabled(false);
			    destinationButton.setEnabled(false);
			    solveRouteButton.setEnabled(false);
			    myDrawingOverlay.setEnabled(true);
			    stopArray = new int[1];
			    stopHasBeenClicked = false;
				destinationHasBeenClicked = false;
			    
			    // reset graphic layers, stop features and global variables
			    stopCounter = 0;
			    routeLayer.removeAll();
			    stopsLayer.removeAll();
			    stops.clearFeatures();
			    HashMap<String, Object> attributes = new HashMap<String, Object>();
			    attributes.put("type", "Start");
			    drawingOverlay.setUp(
			    		DrawingMode.POINT,
			    		new SimpleMarkerSymbol(Color.GRAY, 0, Style.X),
			    		attributes); // places an invisible symbol to avoid errors
			}
		});
		toolBar.add(resetButton); // adds to tool bar
		return toolBar; // returns
	}

	// calculates the shortest route using Dijkstra's algorithm 
	@SuppressWarnings("unused")
	private static void getRoute(EdgeWeightedGraph g, int[] stopArray){
		shortestPath = new Edge[stopArray.length - 1][]; // Initializes the current position of the shortest paths array  
		for (int i = 0; i < stopArray.length - 1; i++) { // runs through each stop
			int pathLengthCounter = 0; // used for determining the length of each route
			Edge[] edgeArray = {}; // used to hold the current section of the trips shortest path
			shortestPathTree = new DijkstraSP(g, stopArray[i]); // creates the shortest path tree from the current start point
			
			if (shortestPathTree.hasPathTo(stopArray[i + 1])) { // only runs if there is a path to the next stop
				for (Edge currentEdge : shortestPathTree.pathTo(stopArray[i + 1])) { // calculates the length of the path
					pathLengthCounter++;	      		
				}
				
				if (pathLengthCounter != 0) { // checks if the path is != 0 (i.e. 2 stops at the same intersection)
				   	edgeArray = new Edge[pathLengthCounter]; // creates an array the length of the route
		        	int setCurrentEdgeCounter = 0;
		        	
		        	for (Edge currentEdge : shortestPathTree.pathTo(stopArray[i + 1])) { // runs through the path
		            	edgeArray[setCurrentEdgeCounter] = currentEdge; // copies each array position
		            	setCurrentEdgeCounter++;
		        	}
		        	
		        	Collections.reverse(Arrays.asList(edgeArray)); // reverses the array so it's in the proper order
		        	
		        	pathLengthCounter = 0; // resets
				  	View.displayRoute(edgeArray, false); // displays the route, arg2 is false because this is not a right turn loop
				  	shortestPath[i] = new Edge[edgeArray.length]; // initializes the array to the proper length
				  	for (int j = 0; j < edgeArray.length; j++) { // copies the array over to the trip array
				  		shortestPath[i][j] = edgeArray[j];
				  	}
				}
			}			  	
		}
	}
	
	// used to determine if a right turn has been encounter in the route
	private static void leftTurnChecker() {
		for (int j = 0; j < shortestPath.length; j++) { // runs through the trip (each route)
			if (shortestPath[j] != null) { // error check (avoids null pointer exceptions)
				for (int i = 1; i < shortestPath[j].length; i++) { // runs through the current route
					Edge firstEdge = shortestPath[j][i - 1]; // finds the current edge
					Edge secondEdge = shortestPath[j][i]; // finds the next edge
					int firstEdgeV = firstEdge.either(); // gets the first intersection
					int firstEdgeW = firstEdge.other(firstEdge.either()); // second
					int secondEdgeV = secondEdge.other(secondEdge.either()); // third
					
					 // gets lat / long info from BST
					double[] firstIntersection = Model.intersectionTree.search(firstEdgeV);
					double[] middleIntersection = Model.intersectionTree.search(firstEdgeW);
					double[] lastIntersection = Model.intersectionTree.search(secondEdgeV);
					
					// calculates the XY coods of each intersection
					double firstIntersectionX = ((firstIntersection[0] + 150.528512) / 0.000054142);
					double firstIntersectionY = ((firstIntersection[1] - 38.247154) / (-0.0000561075));					
					double middleIntersectionX = ((middleIntersection[0] + 150.528512) / 0.000054142);
					double middleIntersectionY = ((middleIntersection[1] - 38.247154) / (-0.0000561075));					
					double lastIntersectionX = ((lastIntersection[0] + 150.528512) / 0.000054142);
					double lastIntersectionY = ((lastIntersection[1] - 38.247154) / (-0.0000561075));
					
					// gets the angle of both the edges
					double firstEdgeAngle = -Math.toDegrees(Math.atan2((middleIntersectionY - firstIntersectionY), (middleIntersectionX - firstIntersectionX)));
					double secondEdgeAngle = -Math.toDegrees(Math.atan2((lastIntersectionY - middleIntersectionY), (lastIntersectionX - middleIntersectionX)));
						
					if (firstEdgeAngle < 0) { // checks if angle needs to be corrected
						firstEdgeAngle += 360;
					}
					
					if (secondEdgeAngle < 0) { // same
						secondEdgeAngle += 360;
					}
					
					if (firstEdgeAngle >= 0 && firstEdgeAngle < 180) { // if the current edge is in the first & second quad
						if ((secondEdgeAngle > firstEdgeAngle) && (secondEdgeAngle < (firstEdgeAngle + 180))) { // if the next edge is a left turn
							Iterable<Edge> intersectionAdjList = Model.graph.adj(firstEdgeW); // gets the turning intersections adj list
							int checkEitherCounter = 1;
							int intersectionID = -1;
							double[] tempAdjIntersection; // used for adj list checking
							double tempAdjAngle; 
							
							for (Edge adjEdge : intersectionAdjList) { // runs through the adj list
								if (checkEitherCounter == 2) { // eliminates all repeated edges
									intersectionID = adjEdge.either();
								}
								
								if (intersectionID == adjEdge.either()) { // ensures that an edge is useful
									// below calculates the angle of each adj list edge
									tempAdjIntersection = Model.intersectionTree.search(adjEdge.other(adjEdge.either())); 
									double tempAdjIntersectionX = ((tempAdjIntersection[0] + 150.528512) / 0.000054142);
									double tempAdjIntersectionY = ((tempAdjIntersection[1] - 38.247154) / (-0.0000561075));
									tempAdjAngle = -Math.toDegrees(Math.atan2((tempAdjIntersectionY - middleIntersectionY), (tempAdjIntersectionX - middleIntersectionX)));
									
									if (tempAdjAngle < 0) { // ensures that the angle is correct
										tempAdjAngle += 360;
									}
									
									if ((tempAdjAngle < secondEdgeAngle) && (tempAdjAngle > (firstEdgeAngle - 20))) { // checks if the angle is indeed a left and not just a straight turn
										Edge[] threeRightTurns = rightTurnLoop(intersectionAdjList,firstEdge); // calls right turn loop which returns the loop of turns the user has to take in the event of a left turn
										View.displayRoute(threeRightTurns, true); // displays the loop using view
									}							
								}
								checkEitherCounter++;
							}
						}
					}
					
					else { // the following is the same as above but for the third and forth quadrants with a few tweeks to ensure the left turn is alway correct
						if ((secondEdgeAngle > firstEdgeAngle) || ((secondEdgeAngle > 0) && (secondEdgeAngle < (firstEdgeAngle - 180)))) { // if left
							if (secondEdgeAngle > firstEdgeAngle) { // if second edge angle is between first and 360
								Iterable<Edge> intersectionAdjList = Model.graph.adj(firstEdgeW);
								int checkEitherCounter = 1;
								int intersectionID = -1;
								double[] tempAdjIntersection;
								double tempAdjAngle; 
								
								for (Edge adjEdge : intersectionAdjList) {
									if (checkEitherCounter == 2) {
										intersectionID = adjEdge.either();
									}
									
									if (intersectionID == adjEdge.either()) {
										
										tempAdjIntersection = Model.intersectionTree.search(adjEdge.other(adjEdge.either()));
										double tempAdjIntersectionX = ((tempAdjIntersection[0] + 150.528512) / 0.000054142);
										double tempAdjIntersectionY = ((tempAdjIntersection[1] - 38.247154) / (-0.0000561075));
										tempAdjAngle = -Math.toDegrees(Math.atan2((tempAdjIntersectionY - middleIntersectionY), (tempAdjIntersectionX - middleIntersectionX)));
										
										if (tempAdjAngle < 0) {
											tempAdjAngle += 360;
										}
										
										if ((tempAdjAngle < secondEdgeAngle) && (tempAdjAngle > (firstEdgeAngle - 20))) {
											Edge[] threeRightTurns = rightTurnLoop(intersectionAdjList,firstEdge);
											View.displayRoute(threeRightTurns, true);
										}							
									}
									checkEitherCounter++;
								}
							}
							
							else if (secondEdgeAngle < firstEdgeAngle) { // if second edge angle is between first and 0
								Iterable<Edge> intersectionAdjList = Model.graph.adj(firstEdgeW);
								int checkEitherCounter = 1;
								int intersectionID = -1;
								double[] tempAdjIntersection;
								double tempAdjAngle; 
								
								for (Edge adjEdge : intersectionAdjList) {
									if (checkEitherCounter == 2) {
										intersectionID = adjEdge.either();
									}
									
									if (intersectionID == adjEdge.either()) {
										
										tempAdjIntersection = Model.intersectionTree.search(adjEdge.other(adjEdge.either()));
										double tempAdjIntersectionX = ((tempAdjIntersection[0] + 150.528512) / 0.000054142);
										double tempAdjIntersectionY = ((tempAdjIntersection[1] - 38.247154) / (-0.0000561075));
										tempAdjAngle = -Math.toDegrees(Math.atan2((tempAdjIntersectionY - middleIntersectionY), (tempAdjIntersectionX - middleIntersectionX)));
										
										if (tempAdjAngle < 0) {
											tempAdjAngle += 360;
										}
										
										if ((tempAdjAngle >= 0) && (tempAdjAngle <= secondEdgeAngle)) { 
											Edge[] threeRightTurns = rightTurnLoop(intersectionAdjList,firstEdge);
											View.displayRoute(threeRightTurns, true);
										}
									}
									checkEitherCounter++;
								}
							}
						}
					}
				}	
			}
		}
	}

	// returns the route the user has to take in the even a left turns happens
	@SuppressWarnings("unused")
	private static Edge[] rightTurnLoop(Iterable<Edge> intersectionAdjList, Edge firstEdge){
		// the below code calculates all needed values of the current edge then determines which adj list edge of the left turn intersection is a "straight"
		int listChecker = 1;
		int firstEdgeV = firstEdge.either();
		int firstEdgeW = firstEdge.other(firstEdge.either());
		double[] firstIntersection = Model.intersectionTree.search(firstEdgeV);
		double firstIntersectionX = ((firstIntersection[0] + 150.528512) / 0.000054142);
		double firstIntersectionY = ((firstIntersection[1] - 38.247154) / (-0.0000561075));
		double[] middleIntersection = Model.intersectionTree.search(firstEdgeW);
		double middleIntersectionX = ((middleIntersection[0] + 150.528512) / 0.000054142);
		double middleIntersectionY = ((middleIntersection[1] - 38.247154) / (-0.0000561075));
		double firstEdgeAngle = -Math.toDegrees(Math.atan2((middleIntersectionY - firstIntersectionY), (middleIntersectionX - firstIntersectionX)));
		Edge[] edges = new Edge[4];
		if (firstEdgeAngle < 0) {
			firstEdgeAngle += 360;
		}
		for (Edge adjEdge : intersectionAdjList) {
			if(firstEdgeW == adjEdge.either()){
				int tempEdgeW = adjEdge.getW();
				double[] tempIntersection = Model.intersectionTree.search(tempEdgeW);
				double tempIntersectionX = ((tempIntersection[0] + 150.528512) / 0.000054142);
				double tempIntersectionY = ((tempIntersection[1] - 38.247154) / (-0.0000561075));
				double secondEdgeAngle = -Math.toDegrees(Math.atan2((tempIntersectionY - middleIntersectionY), (tempIntersectionX - middleIntersectionX)));
				
				if (secondEdgeAngle < 0 ) {
					secondEdgeAngle += 360;
				}
				
				// once the straight has been found a right turn checker is called to get the next 3 rights				
				if(Math.abs((secondEdgeAngle-firstEdgeAngle))<=20) { // if straight
					edges[0] = adjEdge; // first edge of loop is the straight
					edges[1] = rightTurnChecker(adjEdge); // next right
					edges[2] = rightTurnChecker(edges[1]); // next right
					edges[3] = rightTurnChecker(edges[2]); // next right
					
					if(edges[3] == null || edges[3].getW()!=firstEdge.getW()) { // catches null pointer exceptions 
						edges[0] = null;
						edges[1] = null;
						edges[2] = null;
						edges[3] = null;
					}
				}
			}
			listChecker++;
		}
		return edges; // returns the right turn loop
	}
	
	// returns the next right turn of a current path
	private static Edge rightTurnChecker(Edge firstEdge){
		// ======================================================================================================================= //
		
		// this method is the same as the left turn checker but it's tweaked for catching right turns instead (angles are changed) //
		
		// ======================================================================================================================= //
		
		if (firstEdge != null){
			int firstEdgeV = firstEdge.either();
			int firstEdgeW = firstEdge.other(firstEdge.either());
			
			double[] firstIntersection = Model.intersectionTree.search(firstEdgeV);
			double[] middleIntersection = Model.intersectionTree.search(firstEdgeW);
			
			double firstIntersectionX = ((firstIntersection[0] + 150.528512) / 0.000054142);
			double firstIntersectionY = ((firstIntersection[1] - 38.247154) / (-0.0000561075));
			
			double middleIntersectionX = ((middleIntersection[0] + 150.528512) / 0.000054142);
			double middleIntersectionY = ((middleIntersection[1] - 38.247154) / (-0.0000561075));
			
			Iterable<Edge> intersectionAdjList = Model.graph.adj(firstEdgeW);
			int checkEitherCounter = 1;
			int intersectionID = -1;
			double[] tempAdjIntersection;
			double tempAdjAngle; 
			
			double firstEdgeAngle = -Math.toDegrees(Math.atan2((middleIntersectionY - firstIntersectionY), (middleIntersectionX - firstIntersectionX)));
			
			if (firstEdgeAngle < 0) {
				firstEdgeAngle += 360;
			}
			
			for (Edge adjEdge : intersectionAdjList) {
				if (checkEitherCounter == 2) {
					intersectionID = adjEdge.either();
				}
				
				if (intersectionID == adjEdge.either()) {
					
					tempAdjIntersection = Model.intersectionTree.search(adjEdge.other(adjEdge.either()));
					double tempAdjIntersectionX = ((tempAdjIntersection[0] + 150.528512) / 0.000054142);
					double tempAdjIntersectionY = ((tempAdjIntersection[1] - 38.247154) / (-0.0000561075));
					tempAdjAngle = -Math.toDegrees(Math.atan2((tempAdjIntersectionY - middleIntersectionY), (tempAdjIntersectionX - middleIntersectionX)));
					
					if (tempAdjAngle < 0) {
						tempAdjAngle += 360;
					}
					
					if (firstEdgeAngle >= 0 && firstEdgeAngle < 180) { // first & second quad
						if ((firstEdgeAngle < 10) && (tempAdjAngle > (firstEdgeAngle + 190)) && (tempAdjAngle <= 350)){
							return(adjEdge);
						}
						
						else if ((tempAdjAngle > (firstEdgeAngle + 180)) && (tempAdjAngle <= 360)) { // if left
							return(adjEdge);
						}
						
						else if ((tempAdjAngle < (firstEdgeAngle - 10)) && (tempAdjAngle >= 0)) {
							return(adjEdge);
						}
					}	
					
					else { // third & forth quad
						if ((tempAdjAngle < (firstEdgeAngle - 10)) && (tempAdjAngle > (firstEdgeAngle - 170))) {
							return(adjEdge);
						}
					}
				}
				checkEitherCounter++;
			}
		}		
		return(null);
	}
	
	// converts latitude and longitude to esri meters (the unit used by the ArcGis platform)
	public static double[] convertToEsriMeters(double longitude, double latitude) {
		if ((Math.abs(longitude) > 180 || Math.abs(latitude) > 90)) { return null; } // makes sure the coordinates are in the right format
		
		// the below formula is provided by ArcGIS for easy conversions
		double num = longitude * 0.017453292519943295;
	    double x = 6378137.0 * num;
	    double a = latitude * 0.017453292519943295;
	    double[] esriCoordsArray;
	
	    longitude = x;
	    latitude = 3189068.5 * Math.log((1.0 + Math.sin(a)) / (1.0 - Math.sin(a)));
	    esriCoordsArray = new double[2];
	    esriCoordsArray[0] = longitude;
	    esriCoordsArray[1] = latitude;
	    return esriCoordsArray;
	}
  
	// the reverse of the above formula
	// converts from esri meters to regular latitude
	public static double[] convertFromEsriMeters(double[] esri) {
		double x = 0;
		double y = 0;
		x = esri[0];
		y = esri[1];
		
		double num1 = Math.pow(Math.E, y/3189068.5);
		double latitude = Math.asin((num1 -1)/(1+num1));
		
		latitude = latitude/0.017453292519943295;
		double longitude = x/6378137.0;
		longitude = longitude/0.017453292519943295;
		double[] ret = new double[2];
		ret[0]= longitude;
		ret[1]= latitude;
		return ret;
	}
}