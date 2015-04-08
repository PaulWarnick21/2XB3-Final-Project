// CS 2XB3 Lab 2 - Final Project
// Hassaan Malik -
// Trevor Rae - 
// Paul Warnick - 1300963

/*
 * Description:
 * 
 */

package mvc;

//imports for back end graph mapping
import dijkstra.DijkstraSP;
import graph.Edge;
import graph.EdgeWeightedGraph;



//standard java imports
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;



//swing imports
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;



//arcGIS imports
import com.esri.toolkit.overlays.DrawingCompleteEvent;
import com.esri.toolkit.overlays.DrawingCompleteListener;
import com.esri.toolkit.overlays.DrawingOverlay;
import com.esri.toolkit.overlays.DrawingOverlay.DrawingMode;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Polyline;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol.Style;
import com.esri.core.tasks.na.NAFeaturesAsFeature;
import com.esri.map.GraphicsLayer;
import com.esri.map.JMap;
import com.esri.map.MapOptions;
import com.esri.map.MapOptions.MapType;

public class Controller {
	private static JFrame window;
	private JMap map;
	private static DrawingOverlay myDrawingOverlay; //Overlay to draw your route stops
	private static GraphicsLayer stopsLayer;
	private GraphicsLayer streetsLayer;
	private static GraphicsLayer routeLayer;
	private static NAFeaturesAsFeature stops = new NAFeaturesAsFeature();
	private static JButton startPointButton;
	private static JButton stopButton;
	private static JButton destinationButton;
	private static JButton solveRouteButton;
  
	private static int stopCounter = 0;
	private double[] latLongArrayStartPoint;
	private double[] latLongArrayEndPoint;
	private static double[][] xyCoordinates;
	private static final String SOLVE_BUTTON = " Solve route "; // solve button string 
	private static final String RESET_BUTTON = " Reset "; // reset button string
	private static final String  STARTPOINT_BUTTON = " Choose Start Point ";
	private static final String  STOP_BUTTON = " Add a Stop ";
	private static final String  DESTINATION_BUTTON = " Choose Destination ";
	private static final String	STARTPOINT_IMAGE = "http://www.tactranconnect.com/images/icon_start.png"; // url for start image
	private static final String STOP_IMAGE = "http://www.tactranconnect.com/images/mapicons/marker_incidents.png"; // url for stop image
	private static final String	DESTINATION_IMAGE = "http://www.tactranconnect.com/images/icon_end.png"; // url for destination image
	private static EdgeWeightedGraph graph;
	private static double[] esriCoordsArray;
	private static double[][] esriIntersectionCoordinates;
	private static DijkstraSP shortestPathTree;
	private static IntersectionsBST intersectionTree;
	private static int[] stopArray = new int[1];
	private static PictureMarkerSymbol startSymbol = new PictureMarkerSymbol(STARTPOINT_IMAGE); // creates a symbol with the start point url
	private static PictureMarkerSymbol stopSymbol = new PictureMarkerSymbol(STOP_IMAGE); // creates a symbol with the start point url
	private static PictureMarkerSymbol destinationSymbol = new PictureMarkerSymbol(DESTINATION_IMAGE); // same for destination
	private static Edge[][] shortestPath;
	private static boolean stopHasBeenClicked = false;
	private static boolean destinationHasBeenClicked = false;
	
	// determines the closest intersection to where the user has clicked
	public static double[] getClosestIntersection(double esriLat, double esriLong) {
		double[] closestIntersection = new double[2];
		double distance = 0;
		double tempDistance = Double.POSITIVE_INFINITY;
		double [] esriCoords = { -esriLat, esriLong };
		esriCoords[0] = convertFromEsriMeters(esriCoords)[0];
		esriCoords[1] = convertFromEsriMeters(esriCoords)[1];
		
		
		for (int i = 0; i < xyCoordinates.length; i++) {
			distance = (Math.sqrt((Math.pow((esriCoords[0] - (esriIntersectionCoordinates[i][0])),2)) + (Math.pow(esriCoords[1] - (esriIntersectionCoordinates[i][1]),2))));
			if (distance < tempDistance) {
				tempDistance = distance;
				closestIntersection[0] = esriIntersectionCoordinates[i][0];
				closestIntersection[1] = esriIntersectionCoordinates[i][1];
			}
		}
		
		if (tempDistance > 0.07) {
		 JOptionPane.showMessageDialog(window,
				 "One of the locations you've selected is to far away from the map! Please pick another",
				 "Warning",
				 JOptionPane.WARNING_MESSAGE);				 
		 double[] error = { -1.0, -1.0 };
		 return (error);
		}
	  
		return closestIntersection;
	}
		
	public static void addToStops(int[] currentStopArray, int[] toBeAdded) {
		   int currentStopArrayLength = currentStopArray.length;
		   int toBeAddedLenth = toBeAdded.length;
		   stopArray = new int[currentStopArrayLength + toBeAddedLenth];
		   System.arraycopy(currentStopArray, 0, stopArray, 0, currentStopArrayLength);
		   System.arraycopy(toBeAdded, 0, stopArray, currentStopArrayLength, toBeAddedLenth);
	}
	
	// creates the tool bar containing the buttons
	public static Component createToolBar(DrawingOverlay drawingOverlay) {
		JToolBar toolBar = new JToolBar();
		toolBar.setLayout(new FlowLayout(FlowLayout.CENTER));
		toolBar.setFloatable(false);
		
		// add Start Point button
		startPointButton = new JButton(STARTPOINT_BUTTON);
		startPointButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				startSymbol.setOffsetY((float) 16); // shifts the picture upwards
				HashMap<String, Object> attributes = new HashMap<String, Object>();
			    attributes.put("type", "Start");
			    drawingOverlay.setUp(
			        DrawingMode.POINT,
			        startSymbol, // the picutre for the stop
			        attributes);
			}
		});
		toolBar.add(startPointButton);
		
		// add Stop button
		stopButton = new JButton(STOP_BUTTON);
		stopButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stopHasBeenClicked = true;
				stopSymbol.setOffsetY((float) 16); // shifts the picture upwards
				HashMap<String, Object> attributes = new HashMap<String, Object>();
			    attributes.put("type", "Stop");
			    drawingOverlay.setUp(
			        DrawingMode.POINT,
			        stopSymbol, // the picutre for the stop
			        attributes);
			}
		});
		toolBar.add(stopButton);
		stopButton.setEnabled(false);
		
		// add Destination button
		destinationButton = new JButton(DESTINATION_BUTTON);
		destinationButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				destinationHasBeenClicked = true;
				destinationSymbol.setOffsetY((float) 16); // shifts the picture upwards
				HashMap<String, Object> attributes = new HashMap<String, Object>();
			    attributes.put("type", "Destination");
			    drawingOverlay.setUp(
			        DrawingMode.POINT,
			        destinationSymbol, // the picture for the stop
			        attributes);
			    stopButton.setEnabled(false);
			    destinationButton.setEnabled(false);
			}
		});
		toolBar.add(destinationButton);
		destinationButton.setEnabled(false);

		// solve route button
		solveRouteButton = new JButton(SOLVE_BUTTON);
		solveRouteButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// disable the toolbar buttons and overlay 
				startPointButton.setEnabled(false);
				stopButton.setEnabled(false);
			    destinationButton.setEnabled(false);
			    solveRouteButton.setEnabled(false);
			    myDrawingOverlay.setEnabled(false);
			    getRoute(graph, stopArray);
			    leftTurnChecker();
			}
		});
		toolBar.add(solveRouteButton);
		solveRouteButton.setEnabled(false);

		// reset button
		JButton resetButton = new JButton(RESET_BUTTON);
		resetButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// enable the toolbar buttons and overlay  
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
			    		attributes);
			}
		});
		toolBar.add(resetButton);
		return toolBar;
	}

	@SuppressWarnings("unused")
	private static void getRoute(EdgeWeightedGraph g, int[] stopArray){
		shortestPath = new Edge[stopArray.length - 1][];
		for (int i = 0; i < stopArray.length - 1; i++) {
			int pathLengthCounter = 0;
			Edge[] edgeArray = {};
			shortestPathTree = new DijkstraSP(g, stopArray[i]);
			
			if (shortestPathTree.hasPathTo(stopArray[i + 1])) {				
				for (Edge currentEdge : shortestPathTree.pathTo(stopArray[i + 1])) {
					pathLengthCounter++;	      		
				}
				
				if (pathLengthCounter != 0) {				
				   	edgeArray = new Edge[pathLengthCounter];
		        	int setCurrentEdgeCounter = 0;
		        	
		        	for (Edge currentEdge : shortestPathTree.pathTo(stopArray[i + 1])) {
		            	edgeArray[setCurrentEdgeCounter] = currentEdge;
		            	setCurrentEdgeCounter++;
		        	}
		        	
		        	Collections.reverse(Arrays.asList(edgeArray));
		        	
		        	pathLengthCounter = 0;
				  	View.displayRoute(edgeArray, false);
				  	shortestPath[i] = new Edge[edgeArray.length];
				  	for (int j = 0; j < edgeArray.length; j++) {
				  		shortestPath[i][j] = edgeArray[j];
				  	}
				}
			}			  	
		}
	}

	private static void leftTurnChecker() {
		for (int j = 0; j < shortestPath.length; j++) {
			if (shortestPath[j] != null) {
				for (int i = 1; i < shortestPath[j].length; i++) {
					Edge firstEdge = shortestPath[j][i - 1];
					Edge secondEdge = shortestPath[j][i];
					int firstEdgeV = firstEdge.either();
					int firstEdgeW = firstEdge.other(firstEdge.either());
					int secondEdgeV = secondEdge.other(secondEdge.either());
					
					double[] firstIntersection = intersectionTree.search(firstEdgeV);
					double[] middleIntersection = intersectionTree.search(firstEdgeW);
					double[] lastIntersection = intersectionTree.search(secondEdgeV);
					
					double firstIntersectionX = ((firstIntersection[0] + 150.528512) / 0.000054142);
					double firstIntersectionY = ((firstIntersection[1] - 38.247154) / (-0.0000561075));
					
					double middleIntersectionX = ((middleIntersection[0] + 150.528512) / 0.000054142);
					double middleIntersectionY = ((middleIntersection[1] - 38.247154) / (-0.0000561075));
					
					double lastIntersectionX = ((lastIntersection[0] + 150.528512) / 0.000054142);
					double lastIntersectionY = ((lastIntersection[1] - 38.247154) / (-0.0000561075));
					
					double firstEdgeAngle = -Math.toDegrees(Math.atan2((middleIntersectionY - firstIntersectionY), (middleIntersectionX - firstIntersectionX)));
					double secondEdgeAngle = -Math.toDegrees(Math.atan2((lastIntersectionY - middleIntersectionY), (lastIntersectionX - middleIntersectionX)));
						
					if (firstEdgeAngle < 0) {
						firstEdgeAngle += 360;
					}
					
					if (secondEdgeAngle < 0) {
						secondEdgeAngle += 360;
					}
					
					if (firstEdgeAngle >= 0 && firstEdgeAngle < 180) { // first & second quad
						if ((secondEdgeAngle > firstEdgeAngle) && (secondEdgeAngle < (firstEdgeAngle + 180))) { // if left
							Iterable<Edge> intersectionAdjList = graph.adj(firstEdgeW);
							int checkEitherCounter = 1;
							int intersectionID = -1;
							double[] tempAdjIntersection;
							double tempAdjAngle; 
							
							for (Edge adjEdge : intersectionAdjList) {
								if (checkEitherCounter == 2) {
									intersectionID = adjEdge.either();
								}
								
								if (intersectionID == adjEdge.either()) {
									
									tempAdjIntersection = intersectionTree.search(adjEdge.other(adjEdge.either()));
									double tempAdjIntersectionX = ((tempAdjIntersection[0] + 150.528512) / 0.000054142);
									double tempAdjIntersectionY = ((tempAdjIntersection[1] - 38.247154) / (-0.0000561075));
									tempAdjAngle = -Math.toDegrees(Math.atan2((tempAdjIntersectionY - middleIntersectionY), (tempAdjIntersectionX - middleIntersectionX)));
									
									if (tempAdjAngle < 0) {
										tempAdjAngle += 360;
									}
									
									if ((tempAdjAngle < secondEdgeAngle) && (tempAdjAngle > (firstEdgeAngle - 20))) {
										//System.out.println("Left 1/2");
										Edge[] threeRightTurns = rightTurnLoop(intersectionAdjList,firstEdge);
										View.displayRoute(threeRightTurns, true);
									}							
								}
								checkEitherCounter++;
							}
						}
					}
					
					else { // third & forth quad
						if ((secondEdgeAngle > firstEdgeAngle) || ((secondEdgeAngle > 0) && (secondEdgeAngle < (firstEdgeAngle - 180)))) { // if left
							if (secondEdgeAngle > firstEdgeAngle) { // if second edge angle is between first and 360
								Iterable<Edge> intersectionAdjList = graph.adj(firstEdgeW);
								int checkEitherCounter = 1;
								int intersectionID = -1;
								double[] tempAdjIntersection;
								double tempAdjAngle; 
								
								for (Edge adjEdge : intersectionAdjList) {
									if (checkEitherCounter == 2) {
										intersectionID = adjEdge.either();
									}
									
									if (intersectionID == adjEdge.either()) {
										
										tempAdjIntersection = intersectionTree.search(adjEdge.other(adjEdge.either()));
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
								Iterable<Edge> intersectionAdjList = graph.adj(firstEdgeW);
								int checkEitherCounter = 1;
								int intersectionID = -1;
								double[] tempAdjIntersection;
								double tempAdjAngle; 
								
								for (Edge adjEdge : intersectionAdjList) {
									if (checkEitherCounter == 2) {
										intersectionID = adjEdge.either();
									}
									
									if (intersectionID == adjEdge.either()) {
										
										tempAdjIntersection = intersectionTree.search(adjEdge.other(adjEdge.either()));
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

	@SuppressWarnings("unused")
	private static Edge[] rightTurnLoop(Iterable<Edge> intersectionAdjList, Edge firstEdge){
		int listChecker = 1;
		int firstEdgeV = firstEdge.either();
		int firstEdgeW = firstEdge.other(firstEdge.either());
		double[] firstIntersection = intersectionTree.search(firstEdgeV);
		double firstIntersectionX = ((firstIntersection[0] + 150.528512) / 0.000054142);
		double firstIntersectionY = ((firstIntersection[1] - 38.247154) / (-0.0000561075));
		double[] middleIntersection = intersectionTree.search(firstEdgeW);
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
				double[] tempIntersection = intersectionTree.search(tempEdgeW);
				double tempIntersectionX = ((tempIntersection[0] + 150.528512) / 0.000054142);
				double tempIntersectionY = ((tempIntersection[1] - 38.247154) / (-0.0000561075));
				double secondEdgeAngle = -Math.toDegrees(Math.atan2((tempIntersectionY - middleIntersectionY), (tempIntersectionX - middleIntersectionX)));
				
				if (secondEdgeAngle < 0 ) {
					secondEdgeAngle += 360;
				}
				if(Math.abs((secondEdgeAngle-firstEdgeAngle))<=20){
					edges[0] = adjEdge;
					edges[1] = rightTurnChecker(adjEdge);
					edges[2] = rightTurnChecker(edges[1]);
					edges[3] = rightTurnChecker(edges[2]);
					if(edges[3]== null || edges[3].getW()!=firstEdge.getW()){
						edges[0]=null;
						edges[1]=null;
						edges[2]=null;
						edges[3]=null;
					}
				}
			}
			listChecker++;
		}
		return edges;
	}
	
	private static Edge rightTurnChecker(Edge firstEdge){
		if(firstEdge!=null){
			int firstEdgeV = firstEdge.either();
			int firstEdgeW = firstEdge.other(firstEdge.either());
			
			double[] firstIntersection = intersectionTree.search(firstEdgeV);
			double[] middleIntersection = intersectionTree.search(firstEdgeW);
			
			double firstIntersectionX = ((firstIntersection[0] + 150.528512) / 0.000054142);
			double firstIntersectionY = ((firstIntersection[1] - 38.247154) / (-0.0000561075));
			
			double middleIntersectionX = ((middleIntersection[0] + 150.528512) / 0.000054142);
			double middleIntersectionY = ((middleIntersection[1] - 38.247154) / (-0.0000561075));
			
			Iterable<Edge> intersectionAdjList = graph.adj(firstEdgeW);
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
					
					tempAdjIntersection = intersectionTree.search(adjEdge.other(adjEdge.either()));
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

	public static double[] convertToEsriMeters(double longitude, double latitude) {
		if ((Math.abs(longitude) > 180 || Math.abs(latitude) > 90)) { return null; }
		
		double num = longitude * 0.017453292519943295;
	    double x = 6378137.0 * num;
	    double a = latitude * 0.017453292519943295;
	
	    longitude = x;
	    latitude = 3189068.5 * Math.log((1.0 + Math.sin(a)) / (1.0 - Math.sin(a)));
	    esriCoordsArray = new double[2];
	    esriCoordsArray[0] = longitude;
	    esriCoordsArray[1] = latitude;
	    return esriCoordsArray;
	}
  
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
