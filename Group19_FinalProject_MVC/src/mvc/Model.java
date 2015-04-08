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

public class Model {

	private JFrame window;
	private JMap map;
	private DrawingOverlay myDrawingOverlay; //Overlay to draw your route stops
	private GraphicsLayer stopsLayer;
	private static GraphicsLayer streetsLayer;
	private GraphicsLayer routeLayer;
	private NAFeaturesAsFeature stops = new NAFeaturesAsFeature();
	private JButton startPointButton;
	private JButton stopButton;
	private JButton destinationButton;
	private JButton solveRouteButton;
  
	private int stopCounter = 0;
	private static double[] latLongArrayStartPoint;
	private static double[] latLongArrayEndPoint;
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
	private DijkstraSP shortestPathTree;
	private static IntersectionsBST intersectionTree;
	private static int[] stopArray = new int[1];
	private PictureMarkerSymbol startSymbol = new PictureMarkerSymbol(STARTPOINT_IMAGE); // creates a symbol with the start point url
	private PictureMarkerSymbol stopSymbol = new PictureMarkerSymbol(STOP_IMAGE); // creates a symbol with the start point url
	private PictureMarkerSymbol destinationSymbol = new PictureMarkerSymbol(DESTINATION_IMAGE); // same for destination
	private Edge[][] shortestPath;
	private boolean stopHasBeenClicked = false;
	private boolean destinationHasBeenClicked = false;
	
	public static void generateData() throws IOException {
		String currentLineString = "";
		BufferedReader inputIntersections = new BufferedReader(new FileReader("data/IntersectionsSJ.txt"));//reads the input file

		int lineCount = 0;

		while(inputIntersections.readLine()!=null){	lineCount++; }  //counts number of lines in the file

		graph = new EdgeWeightedGraph(lineCount);

		inputIntersections.close();

		inputIntersections = new BufferedReader(new FileReader("data/IntersectionsSJ.txt"));

		int coordinateCounter = 0;

		int[] streetID = new int[lineCount];
		esriIntersectionCoordinates = new double[lineCount][2];
		xyCoordinates = new double[lineCount][2];
		
		while(coordinateCounter != lineCount){
			currentLineString = inputIntersections.readLine();
			String[] currentLine = currentLineString.split(" ");
			streetID[coordinateCounter] = Integer.parseInt(currentLine[0]);
		
			// coords for ocean (Gray Scale)
			esriIntersectionCoordinates[coordinateCounter][0] = (-150.528512 + 0.000054142 * (Double.parseDouble(currentLine[1])));
			esriIntersectionCoordinates[coordinateCounter][1] = (38.247154 - 0.0000561075 * (Double.parseDouble(currentLine[2])));
		
			coordinateCounter++;
		}

		inputIntersections.close();

		coordinateCounter = 0;

		intersectionTree = new IntersectionsBST();

		while (coordinateCounter != lineCount){
			intersectionTree.insert(streetID[coordinateCounter], esriIntersectionCoordinates[coordinateCounter]); //inserts the values into a balanced BST
			coordinateCounter++;
		}

		currentLineString = "";

		BufferedReader inputStreets = new BufferedReader(new FileReader("data/StreetsSJ.txt"));//reads the input file

		lineCount = 0;

		while(inputStreets.readLine()!=null){ lineCount +=1; } //counts number of lines in the file

		inputStreets.close();

		inputStreets = new BufferedReader(new FileReader("data/StreetsSJ.txt"));

		coordinateCounter = 0;

		while(coordinateCounter != lineCount){
			currentLineString = inputStreets.readLine();
			String[] currentLine = currentLineString.split(" ");
			double[] xyStartCoordsArray = intersectionTree.search(Integer.parseInt(currentLine[1]));
			double[] xyEndCoordsArray = intersectionTree.search(Integer.parseInt(currentLine[2]));
		
			// gets the xy coordinate of the start point of a street
			double xCoordStartPoint = xyStartCoordsArray[0];
			double yCoordStartPoint = xyStartCoordsArray[1];
		
			// gets the xy coordinate of the end point of a street
			double xCoordEndPoint = xyEndCoordsArray[0];
			double yCoordEndPoint = xyEndCoordsArray[1];
		
			// creates the graph out of all the streets
			Edge edgeOneWay = new Edge(Integer.parseInt(currentLine[1]), Integer.parseInt(currentLine[2]), Double.parseDouble(currentLine[3]));
			graph.addEdge(edgeOneWay);
			Edge edgeOtherWay = new Edge(Integer.parseInt(currentLine[2]), Integer.parseInt(currentLine[1]), Double.parseDouble(currentLine[3]));
			graph.addEdge(edgeOtherWay);
		
			// adds a street graphic to the street layer of the map
			Polyline street = new Polyline();
			SimpleLineSymbol streetSymbol = new SimpleLineSymbol(Color.BLUE, 2.0f);
			latLongArrayStartPoint = Controller.convertToEsriMeters(xCoordStartPoint, yCoordStartPoint);
			street.startPath(latLongArrayStartPoint[0],latLongArrayStartPoint[1]);
			latLongArrayEndPoint = Controller.convertToEsriMeters(xCoordEndPoint, yCoordEndPoint);
			street.lineTo(latLongArrayEndPoint[0], latLongArrayEndPoint[1]);
			streetsLayer.addGraphic(new Graphic(street, streetSymbol, 0));
		
			coordinateCounter++;
		}

		inputStreets.close();
	}
}
