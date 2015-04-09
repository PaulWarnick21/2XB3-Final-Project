// CS 2XB3 Lab 2 - Final Project
// Hassaan Malik - 1224997
// Trevor Rae - 1324949
// Paul Warnick - 1300963

/*
 * Description:
 * 
 * Data storage, integrity, consistency, queries & mutations. The model class reads information from the text 
 * files provided in the data folder and converts the raw data to streets and intersections on a map. The location
 * is San Joaquin County (California): https://www.google.ca/maps/@37.906698,-121.2071117,10z
 */

package mvc;

//imports for back end graph mapping
import graph.Edge;
import graph.EdgeWeightedGraph;

//standard java imports
import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

//arcGIS imports
import com.esri.core.geometry.Polyline;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleLineSymbol;

public class Model {
	private static double[] latLongArrayStartPoint; // an array to hold the start lat and long of a start point of an edge (v)
	private static double[] latLongArrayEndPoint; // same for end point (w)
	public static EdgeWeightedGraph graph; // the graph that the edges are stored in
 	public static double[][] esriIntersectionCoordinates; // holds an intersections coordinates once they've been converted to esri meters
	public static IntersectionsBST intersectionTree; // the balanced BST that holds all intersection data
	public static double[][] xyCoordinates; // used to store xy coords of intersections
	
	// reads text files to generate map data
	public static void generateData() throws IOException {
		String currentLineString = ""; // a string used to store the current line of a text file
		BufferedReader inputIntersections = new BufferedReader(new FileReader("data/IntersectionsSJ.txt")); //reads the input file

		int lineCount = 0;

		while(inputIntersections.readLine()!=null){	lineCount++; }  //counts number of lines in the file

		graph = new EdgeWeightedGraph(lineCount); // Initializes the graph

		inputIntersections.close(); // closes file

		inputIntersections = new BufferedReader(new FileReader("data/IntersectionsSJ.txt")); // refreshes buffered reader

		int coordinateCounter = 0; 

		int[] streetID = new int[lineCount]; // keys for tree
		esriIntersectionCoordinates = new double[lineCount][2]; // initializes the array
		xyCoordinates = new double[lineCount][2]; // same
		
		while(coordinateCounter != lineCount){ // runs through each line
			currentLineString = inputIntersections.readLine(); // reads the line
			String[] currentLine = currentLineString.split(" "); // splits it appropriately
			streetID[coordinateCounter] = Integer.parseInt(currentLine[0]); // gets intersection ID
			esriIntersectionCoordinates[coordinateCounter][0] = (-150.528512 + 0.000054142 * (Double.parseDouble(currentLine[1]))); // gets long and coverts to esri meters
			esriIntersectionCoordinates[coordinateCounter][1] = (38.247154 - 0.0000561075 * (Double.parseDouble(currentLine[2]))); // gets lat and converts to esri meters
		
			coordinateCounter++;
		}

		inputIntersections.close(); // closes file

		coordinateCounter = 0;

		intersectionTree = new IntersectionsBST(); // Initializes BST

		// inserts all values into the tree
		while (coordinateCounter != lineCount) {
			intersectionTree.insert(streetID[coordinateCounter], esriIntersectionCoordinates[coordinateCounter]); //inserts the values into a balanced BST
			coordinateCounter++;
		}

		currentLineString = "";

		BufferedReader inputStreets = new BufferedReader(new FileReader("data/StreetsSJ.txt")); //reads the input file

		lineCount = 0;

		while(inputStreets.readLine()!=null){ lineCount +=1; } //counts number of lines in the file

		inputStreets.close();

		inputStreets = new BufferedReader(new FileReader("data/StreetsSJ.txt"));

		coordinateCounter = 0;

		// goes through the streets file and constructs the graph ALSO prints to the map through control and view
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
			Controller.streetsLayer.addGraphic(new Graphic(street, streetSymbol, 0));
		
			coordinateCounter++;
		}

		inputStreets.close();
	}
}