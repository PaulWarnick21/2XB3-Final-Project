// CS 2XB3 Lab 2 - Final Project
// Hassaan Malik - 1224997
// Trevor Rae - 1324949
// Paul Warnick - 1300963

/*
 * Description:
 * 
 * Handles all the presentation assets. Here the JMap (an ArcGIS type) is held and maintained in order to
 * show the user everything that is happening in the window. On top of this all the stop graphics are added and 
 * maintained here. Lastly the route is displayed to the map in this class
 */ 

package mvc; // package name

//imports for back end graph mapping
import graph.Edge;

//standard java imports
import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.HashMap;

//swing imports
import javax.swing.JFrame;
import javax.swing.JLayeredPane;

//arcGIS imports
import com.esri.toolkit.overlays.DrawingCompleteEvent;
import com.esri.toolkit.overlays.DrawingCompleteListener;
import com.esri.toolkit.overlays.DrawingOverlay;
import com.esri.toolkit.overlays.DrawingOverlay.DrawingMode;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Polyline;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol.Style;
import com.esri.map.GraphicsLayer;
import com.esri.map.JMap;
import com.esri.map.MapOptions;
import com.esri.map.MapOptions.MapType;

public class View {	

	public static JFrame window; // creates the JFrame the user sees and uses
	private JMap map; // creates a JMap the user interacts with
	
	// generates the map
	public View() throws IOException {
		window = new JFrame(); // Initializes the JFrame
		window.setSize(800, 600); // sets the window size
		window.setLocationRelativeTo(null); // center on screen
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // exits if the exit button is hit
		window.getContentPane().setLayout(new BorderLayout(0, 0)); // sets the layout

		// dispose map just before application window is closed.
		window.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent windowEvent) {
				super.windowClosing(windowEvent);
				map.dispose();
			}
		});

		// Using MapOptions allows for a common online base map to be chosen
		MapOptions mapOptions = new MapOptions(MapType.GRAY_BASE); // sets the map type 
		map = new JMap(mapOptions); // creates the map
    
		// envelope for ocean
		map.setExtent(new Envelope(-16732452, 4533753, -16719762, 4619957.78));
		
		// adds graphic layers to the map
		Controller.routeLayer = new GraphicsLayer();
		map.getLayers().add(Controller.routeLayer);
		Controller.streetsLayer = new GraphicsLayer();
		map.getLayers().add(Controller.streetsLayer);
		Controller.stopsLayer = new GraphicsLayer();
		map.getLayers().add(Controller.stopsLayer);
		
		addStopGraphics(); // adds all stop graphics to the map where the user clicked
    
		// Add the JMap to the JFrame's content pane
		window.getContentPane().add(map);
    
		JLayeredPane contentPane = new JLayeredPane(); // creates the content pane add everything to
		contentPane.setLayout(new BorderLayout()); // sets the layout
		contentPane.setVisible(true); // sets to visible
		window.add(contentPane); // adds to the JFrame
		contentPane.add(map);
		contentPane.add(Controller.createToolBar(Controller.myDrawingOverlay), BorderLayout.NORTH); // adds the tool bar with all buttons
		
		Model.generateData(); // generates the data from the text files (graphs)
	}
	
	// adds the stop graphics to the map
	private void addStopGraphics() {
		Controller.myDrawingOverlay = new DrawingOverlay(); // creates the overlay
		Controller.myDrawingOverlay.addDrawingCompleteListener(new DrawingCompleteListener() { // creates the listener for drawing stops	
			@Override
			public void drawingCompleted(DrawingCompleteEvent event) {
				// get the user-drawn stop graphic from the overlay
				Graphic graphic = (Graphic) Controller.myDrawingOverlay.getAndClearFeature(); // initializes a new graphic
				
				if (graphic.getSymbol().toString().contains("Size=0")) {} // error check
			
				// the below section adds the start point graphic stop layer upon mouse click
				else if (Controller.startPointButton.isEnabled()) {
					// features for adding stop graphic and enabling certain buttons
					Controller.startPointButton.setEnabled(false);
					Controller.stopsLayer.addGraphic(graphic);
					Controller.stopButton.setEnabled(true);
					Controller.destinationButton.setEnabled(true);
					Controller.stopCounter++;
				
					// finds the the closest intersection (vertex) on the map for navigation purposes
					String[] startPointLatInfo = (graphic.getGeometry().toString().split(","));
					String[] startPointLongInfo = startPointLatInfo[0].split("-");
					double startPointLatEsri = Double.parseDouble(startPointLongInfo[1]);
					startPointLatInfo[1] = startPointLatInfo[1].replace(' ', ']');
					startPointLatInfo[1] = startPointLatInfo[1].replace("]", "");
					double startPointLongEsri = Double.parseDouble(startPointLatInfo[1]);
				
					// checks if the start point location is out of bounds
					if ((Controller.getClosestIntersection(startPointLatEsri, startPointLongEsri)[0]) == -1.0 || Controller.getClosestIntersection(startPointLatEsri, startPointLongEsri)[1] == -1.0) {
						Controller.startPointButton.setEnabled(true);
						Controller.stopButton.setEnabled(false);
						Controller.destinationButton.setEnabled(false);
						Controller.solveRouteButton.setEnabled(false);
						Controller.myDrawingOverlay.setEnabled(true);
						Controller.stopArray = new int[1];
						Controller.stopHasBeenClicked = false;
						Controller.destinationHasBeenClicked = false;
						
						// reset graphic layers, stop features and global variables
						Controller.stopCounter = 0;
						Controller.routeLayer.removeAll();
						Controller.stopsLayer.removeAll();
						Controller.stops.clearFeatures();
						HashMap<String, Object> attributes = new HashMap<String, Object>();
						attributes.put("type", "Start");
						Controller.myDrawingOverlay.setUp(
								DrawingMode.POINT,
								new SimpleMarkerSymbol(Color.GRAY, 0, Style.X),
								attributes);
					}
					
					// the stop can be displayed to the map
					else {
						double startClosestNodeLat = Controller.getClosestIntersection(startPointLatEsri, startPointLongEsri)[0]; // finds the start nodes lat
						double startClosestNodeLong = Controller.getClosestIntersection(startPointLatEsri, startPointLongEsri)[1]; // same for long
						Model.intersectionTree.findNodeID(Model.intersectionTree.root, startClosestNodeLat, startClosestNodeLong); // finds the closest node to the start node (user click)
						Controller.stopArray[0] = Model.intersectionTree.closestNode; // adds it to the stop array
					}
				}
				
				// below two methods are the same as above just for different cases (i.e. additional stops and destination) 
				
				// adds the destination graphic to the stop layer upon mouse click
				else if (!Controller.destinationButton.isEnabled() && Controller.stopCounter == 1) {
					// modifies which buttons are enabled and adds the destination graphic
					Controller.stopsLayer.addGraphic(graphic);
					Controller.solveRouteButton.setEnabled(true);
					Controller.stopCounter++;
					
					// finds the closest intersection (vertex) on the map for navigation purposes
					String[] destinationLatInfo = (graphic.getGeometry().toString().split(","));
					String[] destinationLongInfo = destinationLatInfo[0].split("-");
					double destinationLatEsri = Double.parseDouble(destinationLongInfo[1]);
					destinationLatInfo[1] =  destinationLatInfo[1].replace(' ', ']');
					destinationLatInfo[1] =  destinationLatInfo[1].replace("]", "");
					double destinationLongEsri = Double.parseDouble( destinationLatInfo[1]);
				
					// checks if the destination location is out of bounds
					if ((Controller.getClosestIntersection(destinationLatEsri, destinationLongEsri)[0]) == -1.0 || Controller.getClosestIntersection(destinationLatEsri, destinationLongEsri)[1] == -1.0) {
						Controller.startPointButton.setEnabled(true);
						Controller.destinationButton.setEnabled(false);
						Controller.solveRouteButton.setEnabled(false);
						Controller.myDrawingOverlay.setEnabled(true);
						Controller.stopArray = new int[1];
						Controller.stopHasBeenClicked = false;
						Controller.destinationHasBeenClicked = false;
						
						// reset graphic layers, stop features and global variables
						Controller.stopCounter = 0;
						Controller.routeLayer.removeAll();
						Controller.stopsLayer.removeAll();
						Controller.stops.clearFeatures();
						HashMap<String, Object> attributes = new HashMap<String, Object>();
						attributes.put("type", "Start");
						Controller.myDrawingOverlay.setUp(
								DrawingMode.POINT,
								new SimpleMarkerSymbol(Color.GRAY, 0, Style.X),
								attributes);
					}
				
					else {
						double destinationClosestNodeLat = Controller.getClosestIntersection(destinationLatEsri, destinationLongEsri)[0];
						double destinationClosestNodeLong = Controller.getClosestIntersection(destinationLatEsri, destinationLongEsri)[1];
						Model.intersectionTree.findNodeID(Model.intersectionTree.root, destinationClosestNodeLat, destinationClosestNodeLong);
						int[] closestNode = {Model.intersectionTree.closestNode};
						Controller.addToStops(Controller.stopArray, closestNode);
					}
				}
				
				// adds all "in between" stops to the map
				else if (Controller.stopButton.isEnabled() && Controller.stopHasBeenClicked && !Controller.destinationHasBeenClicked) { 
					// modifies which buttons are enabled and adds the stop graphic
					Controller.stopsLayer.addGraphic(graphic);
					
					// finds the closest intersection (vertex) on the map for navigation purposes
					String[] stopLatInfo = (graphic.getGeometry().toString().split(","));
					String[] stopLongInfo = stopLatInfo[0].split("-");
					double stopLatEsri = Double.parseDouble(stopLongInfo[1]);
					stopLatInfo[1] =  stopLatInfo[1].replace(' ', ']');
					stopLatInfo[1] =  stopLatInfo[1].replace("]", "");
					double stopLongEsri = Double.parseDouble( stopLatInfo[1]);
				
					// checks if the stop location is out of bounds
					if ((Controller.getClosestIntersection(stopLatEsri, stopLongEsri)[0]) == -1.0 || Controller.getClosestIntersection(stopLatEsri, stopLongEsri)[1] == -1.0) {
						Controller.startPointButton.setEnabled(true);
						Controller.stopButton.setEnabled(false);
						Controller.solveRouteButton.setEnabled(false);
						Controller.myDrawingOverlay.setEnabled(true);
						Controller.stopArray = new int[1];
						Controller.stopHasBeenClicked = false;
						Controller.destinationHasBeenClicked = false;
						
						// reset graphic layers, stop features and global variables
						Controller.stopCounter = 0;
						Controller.routeLayer.removeAll();
						Controller.stopsLayer.removeAll();
						Controller.stops.clearFeatures();
						HashMap<String, Object> attributes = new HashMap<String, Object>();
						attributes.put("type", "Start");
						Controller.myDrawingOverlay.setUp(
								DrawingMode.POINT,
								new SimpleMarkerSymbol(Color.GRAY, 0, Style.X),
								attributes);
					}
				
					else {
						double stopClosestNodeLat = Controller.getClosestIntersection(stopLatEsri, stopLongEsri)[0];
						double stopClosestNodeLong = Controller.getClosestIntersection(stopLatEsri, stopLongEsri)[1];
						Model.intersectionTree.findNodeID(Model.intersectionTree.root, stopClosestNodeLat, stopClosestNodeLong);
						int[] closestNode = {Model.intersectionTree.closestNode};
						Controller.addToStops(Controller.stopArray, closestNode);
					}
				}
			}
		});
		map.addMapOverlay(Controller.myDrawingOverlay); // adds the overlay to the map
	}

	// adds the optimal route to the map & draws right turn loops 
	public static void displayRoute(Edge[] route, boolean isLoop) {
		if (isLoop == true) { // checks to see if the route is a right turn loop or just a regular navigation route
			if(route[0]!=null){ // error checking
				Polyline routeStreet = new Polyline(); // makes a new polyline
				SimpleLineSymbol routeSymbol = new SimpleLineSymbol(Color.GREEN, 10.0f); // sets the colour of the polyline (route colour) to green (for the right turn loops) 
				double[] latLongArrayStartPointRoute = Controller.convertToEsriMeters((Model.intersectionTree.search(route[0].either())[0]), (Model.intersectionTree.search(route[0].either())[1])); // finds the start point of the route
				double[] latLongArrayEndPointRoute;
				routeStreet.startPath(latLongArrayStartPointRoute[0], latLongArrayStartPointRoute[1]);  // sets the start of polyline
				
				for (int i = 0; i < route.length; i++) { // runs through the length
					if(route[i] != null) { // error checking
						latLongArrayEndPointRoute = Controller.convertToEsriMeters((Model.intersectionTree.search(route[i].other(route[i].either()))[0]), (Model.intersectionTree.search(route[i].other(route[i].either()))[1])); // finds each point of the route
						routeStreet.lineTo(latLongArrayEndPointRoute[0], latLongArrayEndPointRoute[1]); // draws a polyline for each edge in the current route
					}
				}
				Controller.routeLayer.addGraphic(new Graphic(routeStreet, routeSymbol, 0)); // adds the route graphic to the map
			}
		}
		
		// same as above but for the actual shortest path, coloured in red
		else {
			Polyline routeStreet = new Polyline();
			SimpleLineSymbol routeSymbol = new SimpleLineSymbol(Color.RED, 10.0f);
			double[] latLongArrayStartPointRoute = Controller.convertToEsriMeters((Model.intersectionTree.search(route[0].either())[0]), (Model.intersectionTree.search(route[0].either())[1])); 
			double[] latLongArrayEndPointRoute; 
			routeStreet.startPath(latLongArrayStartPointRoute[0], latLongArrayStartPointRoute[1]);
			
			for (int i = 0; i < route.length; i++) {
				latLongArrayEndPointRoute = Controller.convertToEsriMeters((Model.intersectionTree.search(route[i].other(route[i].either()))[0]), (Model.intersectionTree.search(route[i].other(route[i].either()))[1]));
				routeStreet.lineTo(latLongArrayEndPointRoute[0], latLongArrayEndPointRoute[1]);
			}
			Controller.routeLayer.addGraphic(new Graphic(routeStreet, routeSymbol, 0));
		}
	}
}