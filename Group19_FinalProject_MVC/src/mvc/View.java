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

public class View {	
	public JFrame window;
	private JMap map;
	private DrawingOverlay myDrawingOverlay; //Overlay to draw your route stops
	private GraphicsLayer stopsLayer;
	private GraphicsLayer streetsLayer;
	private static GraphicsLayer routeLayer;
	private NAFeaturesAsFeature stops = new NAFeaturesAsFeature();
	private JButton startPointButton;
	private JButton stopButton;
	private JButton destinationButton;
	private JButton solveRouteButton;
  
	private int stopCounter = 0;
	private double[] latLongArrayStartPoint;
	private double[] latLongArrayEndPoint;
	private double[][] xyCoordinates;
	private static final String SOLVE_BUTTON = " Solve route "; // solve button string 
	private static final String RESET_BUTTON = " Reset "; // reset button string
	private static final String  STARTPOINT_BUTTON = " Choose Start Point ";
	private static final String  STOP_BUTTON = " Add a Stop ";
	private static final String  DESTINATION_BUTTON = " Choose Destination ";
	private static final String	STARTPOINT_IMAGE = "http://www.tactranconnect.com/images/icon_start.png"; // url for start image
	private static final String STOP_IMAGE = "http://www.tactranconnect.com/images/mapicons/marker_incidents.png"; // url for stop image
	private static final String	DESTINATION_IMAGE = "http://www.tactranconnect.com/images/icon_end.png"; // url for destination image
	private EdgeWeightedGraph graph;
	private static double[] esriCoordsArray;
	private static double[][] esriIntersectionCoordinates;
	private DijkstraSP shortestPathTree;
	private static IntersectionsBST intersectionTree;
	private int[] stopArray = new int[1];
	private PictureMarkerSymbol startSymbol = new PictureMarkerSymbol(STARTPOINT_IMAGE); // creates a symbol with the start point url
	private PictureMarkerSymbol stopSymbol = new PictureMarkerSymbol(STOP_IMAGE); // creates a symbol with the start point url
	private PictureMarkerSymbol destinationSymbol = new PictureMarkerSymbol(DESTINATION_IMAGE); // same for destination
	private Edge[][] shortestPath;
	private boolean stopHasBeenClicked = false;
	private boolean destinationHasBeenClicked = false;
	
	// generates the map
	public View() throws IOException {
		window = new JFrame();
		window.setSize(800, 600);
		window.setLocationRelativeTo(null); // center on screen
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.getContentPane().setLayout(new BorderLayout(0, 0));

		// dispose map just before application window is closed.
		window.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent windowEvent) {
				super.windowClosing(windowEvent);
				map.dispose();
			}
		});

		// Using MapOptions allows for a common online base map to be chosen
		MapOptions mapOptions = new MapOptions(MapType.GRAY_BASE);
		map = new JMap(mapOptions);
    
		// envelope for ocean
		map.setExtent(new Envelope(-16732452, 4533753, -16719762, 4619957.78));
		
		// adds graphic layers to the map
		routeLayer = new GraphicsLayer();
		map.getLayers().add(routeLayer);
		streetsLayer = new GraphicsLayer();
		map.getLayers().add(streetsLayer);
		stopsLayer = new GraphicsLayer();
		map.getLayers().add(stopsLayer);
		
		addStopGraphics();	
    
		// Add the JMap to the JFrame's content pane
		window.getContentPane().add(map);
    
		JLayeredPane contentPane = new JLayeredPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.setVisible(true);
		window.add(contentPane);
		contentPane.add(map);
		contentPane.add(Controller.createToolBar(myDrawingOverlay), BorderLayout.NORTH);
		
		Model.generateData(); 
	}
	
	// adds the stop graphics to the map
	private void addStopGraphics() {
		myDrawingOverlay = new DrawingOverlay();
		myDrawingOverlay.addDrawingCompleteListener(new DrawingCompleteListener() {		
			@Override
			public void drawingCompleted(DrawingCompleteEvent event) {
				// get the user-drawn stop graphic from the overlay
				Graphic graphic = (Graphic) myDrawingOverlay.getAndClearFeature();
				// add it to the stopsLayer for display
				if (graphic.getSymbol().toString().contains("Size=0")) {}
			
				// adds the start point graphic stop layer upon mouse click
				else if (startPointButton.isEnabled()) {
					// features for adding stop graphic and enabling certain buttons
					startPointButton.setEnabled(false);
					stopsLayer.addGraphic(graphic);
					stopButton.setEnabled(true);
					destinationButton.setEnabled(true);
					stopCounter++;
				
					// finds the the closest intersection (vertex) on the map for navigation purposes
					String[] startPointLatInfo = (graphic.getGeometry().toString().split(","));
					String[] startPointLongInfo = startPointLatInfo[0].split("-");
					double startPointLatEsri = Double.parseDouble(startPointLongInfo[1]);
					startPointLatInfo[1] = startPointLatInfo[1].replace(' ', ']');
					startPointLatInfo[1] = startPointLatInfo[1].replace("]", "");
					double startPointLongEsri = Double.parseDouble(startPointLatInfo[1]);
				
					// checks if the start point location is out of bounds
					if ((Controller.getClosestIntersection(startPointLatEsri, startPointLongEsri)[0]) == -1.0 || Controller.getClosestIntersection(startPointLatEsri, startPointLongEsri)[1] == -1.0) {
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
						myDrawingOverlay.setUp(
								DrawingMode.POINT,
								new SimpleMarkerSymbol(Color.GRAY, 0, Style.X),
								attributes);
					}
					
					else {
						double startClosestNodeLat = Controller.getClosestIntersection(startPointLatEsri, startPointLongEsri)[0];
						double startClosestNodeLong = Controller.getClosestIntersection(startPointLatEsri, startPointLongEsri)[1];
						intersectionTree.findNodeID(intersectionTree.root, startClosestNodeLat, startClosestNodeLong);
						stopArray[0] = intersectionTree.closestNode;
					}
				}
				
				// adds the destination graphic to the stop layer upon mouse click
				else if (!destinationButton.isEnabled() && stopCounter == 1) {
					// modifies which buttons are enabled and adds the destination graphic
					stopsLayer.addGraphic(graphic);
					solveRouteButton.setEnabled(true);
					stopCounter++;
					
					// finds the closest intersection (vertex) on the map for navigation purposes
					String[] destinationLatInfo = (graphic.getGeometry().toString().split(","));
					String[] destinationLongInfo = destinationLatInfo[0].split("-");
					double destinationLatEsri = Double.parseDouble(destinationLongInfo[1]);
					destinationLatInfo[1] =  destinationLatInfo[1].replace(' ', ']');
					destinationLatInfo[1] =  destinationLatInfo[1].replace("]", "");
					double destinationLongEsri = Double.parseDouble( destinationLatInfo[1]);
				
					// checks if the destination location is out of bounds
					if ((Controller.getClosestIntersection(destinationLatEsri, destinationLongEsri)[0]) == -1.0 || Controller.getClosestIntersection(destinationLatEsri, destinationLongEsri)[1] == -1.0) {
						startPointButton.setEnabled(true);
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
						myDrawingOverlay.setUp(
								DrawingMode.POINT,
								new SimpleMarkerSymbol(Color.GRAY, 0, Style.X),
								attributes);
					}
				
					else {
						double destinationClosestNodeLat = Controller.getClosestIntersection(destinationLatEsri, destinationLongEsri)[0];
						double destinationClosestNodeLong = Controller.getClosestIntersection(destinationLatEsri, destinationLongEsri)[1];
						intersectionTree.findNodeID(intersectionTree.root, destinationClosestNodeLat, destinationClosestNodeLong);
						int[] closestNode = {intersectionTree.closestNode};
						Controller.addToStops(stopArray, closestNode);
					}
				}
				
				else if (stopButton.isEnabled() && stopHasBeenClicked && !destinationHasBeenClicked) { 
					// modifies which buttons are enabled and adds the stop graphic
					stopsLayer.addGraphic(graphic);
					
					// finds the closest intersection (vertex) on the map for navigation purposes
					String[] stopLatInfo = (graphic.getGeometry().toString().split(","));
					String[] stopLongInfo = stopLatInfo[0].split("-");
					double stopLatEsri = Double.parseDouble(stopLongInfo[1]);
					stopLatInfo[1] =  stopLatInfo[1].replace(' ', ']');
					stopLatInfo[1] =  stopLatInfo[1].replace("]", "");
					double stopLongEsri = Double.parseDouble( stopLatInfo[1]);
				
					// checks if the stop location is out of bounds
					if ((Controller.getClosestIntersection(stopLatEsri, stopLongEsri)[0]) == -1.0 || Controller.getClosestIntersection(stopLatEsri, stopLongEsri)[1] == -1.0) {
						startPointButton.setEnabled(true);
						stopButton.setEnabled(false);
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
						myDrawingOverlay.setUp(
								DrawingMode.POINT,
								new SimpleMarkerSymbol(Color.GRAY, 0, Style.X),
								attributes);
					}
				
					else {
						double stopClosestNodeLat = Controller.getClosestIntersection(stopLatEsri, stopLongEsri)[0];
						double stopClosestNodeLong = Controller.getClosestIntersection(stopLatEsri, stopLongEsri)[1];
						intersectionTree.findNodeID(intersectionTree.root, stopClosestNodeLat, stopClosestNodeLong);
						int[] closestNode = {intersectionTree.closestNode};
						Controller.addToStops(stopArray, closestNode);
					}
				}
			}
		});
		map.addMapOverlay(myDrawingOverlay);
	}

	// adds the optimal route to the map 
	public static void displayRoute(Edge[] route, boolean isLoop) {
		if (isLoop == true) {
			if(route[0]!=null){
				Polyline routeStreet = new Polyline();
				SimpleLineSymbol routeSymbol = new SimpleLineSymbol(Color.GREEN, 6.0f);
				double[] latLongArrayStartPointRoute = Controller.convertToEsriMeters((intersectionTree.search(route[0].either())[0]), (intersectionTree.search(route[0].either())[1])); 
				double[] latLongArrayEndPointRoute; 
				routeStreet.startPath(latLongArrayStartPointRoute[0], latLongArrayStartPointRoute[1]);
				
				for (int i = 0; i < route.length; i++) {
					if(route[i]!=null){
						latLongArrayEndPointRoute = Controller.convertToEsriMeters((intersectionTree.search(route[i].other(route[i].either()))[0]), (intersectionTree.search(route[i].other(route[i].either()))[1]));
						routeStreet.lineTo(latLongArrayEndPointRoute[0], latLongArrayEndPointRoute[1]);
					}
				}
				routeLayer.addGraphic(new Graphic(routeStreet, routeSymbol, 0));
			}
		}
		
		else {
			Polyline routeStreet = new Polyline();
			SimpleLineSymbol routeSymbol = new SimpleLineSymbol(Color.RED, 10.0f);
			double[] latLongArrayStartPointRoute = Controller.convertToEsriMeters((intersectionTree.search(route[0].either())[0]), (intersectionTree.search(route[0].either())[1])); 
			double[] latLongArrayEndPointRoute; 
			routeStreet.startPath(latLongArrayStartPointRoute[0], latLongArrayStartPointRoute[1]);
			
			for (int i = 0; i < route.length; i++) {
				latLongArrayEndPointRoute = Controller.convertToEsriMeters((intersectionTree.search(route[i].other(route[i].either()))[0]), (intersectionTree.search(route[i].other(route[i].either()))[1]));
				routeStreet.lineTo(latLongArrayEndPointRoute[0], latLongArrayEndPointRoute[1]);
			}
			routeLayer.addGraphic(new Graphic(routeStreet, routeSymbol, 0));
		}
	}
}
