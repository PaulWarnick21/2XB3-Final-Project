import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JToolBar;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

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
import com.esri.core.symbol.TextSymbol;
import com.esri.core.tasks.na.NAFeaturesAsFeature;
import com.esri.map.GraphicsLayer;
import com.esri.map.JMap;
import com.esri.map.MapOptions;
import com.esri.map.MapOptions.MapType;

public class Testing {

  private JFrame window;
  private JMap map;
  private DrawingOverlay myDrawingOverlay; //Overlay to draw your route stops
  private int numStops = 0;
  private GraphicsLayer graphicsLayer;
  private GraphicsLayer streetsLayer;
  private NAFeaturesAsFeature stops = new NAFeaturesAsFeature();
  private double[] dubArray;
  private double[] dubArray1;
  private static final String SOLVE_BUTTON = " Solve route "; // solve button string 
  private static final String STOP_BUTTON = " Add a stop "; // stop button string
  private static final String RESET_BUTTON = " Reset "; // reset button string
  private static double[] temp;
  
  public Testing() throws IOException {
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

    // Using MapOptions allows for a common online basemap to be chosen
    MapOptions mapOptions = new MapOptions(MapType.GRAY_BASE);
    map = new JMap(mapOptions);
    
    map.setExtent(new Envelope(-16732452, 4533753, -16719762, 4619957.78));
    
    graphicsLayer = new GraphicsLayer();
    map.getLayers().add(graphicsLayer);
    
    streetsLayer = new GraphicsLayer();
    map.getLayers().add(streetsLayer);
    SimpleLineSymbol street = new SimpleLineSymbol(Color.RED, 2.0f);
    
    //////////////////////////////////////////////
    
    String singleLine = "";
	
	BufferedReader in1 = new BufferedReader(new FileReader("data/IntersectionsSJ.txt"));//reads the input file

	int count = 0;
	while(in1.readLine()!=null){//counts number of lines in the file
		count +=1;
	}
	in1.close();
	
	BufferedReader in2 = new BufferedReader(new FileReader("data/IntersectionsSJ.txt"));
	
	int count2 = 0;
	
	int[] nodes = new int[count];
	double[][] coordinates = new double[count][2];
	while(count2!=count){
		singleLine = in2.readLine();
		String[] temp = singleLine.split(" ");
		nodes[count2] = Integer.parseInt(temp[0]);
		coordinates[count2][0]= (-150.528512 + 0.000054142 * (Double.parseDouble(temp[1])));
		coordinates[count2][1]= (38.247154 - 0.0000561075 * (Double.parseDouble(temp[2])));
		//coordinates[count2][0]= (-121.528512 + 0.000054142 * (Double.parseDouble(temp[1])));
		//coordinates[count2][1]= (38.247154 - 0.0000561075 * (Double.parseDouble(temp[2])));
		count2++;
	}
	in2.close();
	
	count2=0;
	
	SelfBalancingTree theTree1 = new SelfBalancingTree();
	while (count2 != count){
		theTree1.insert(nodes[count2],coordinates[count2]);//inserts the values into a balanced BST
		count2++;
	}
	
    singleLine = "";
	
	BufferedReader in3 = new BufferedReader(new FileReader("data/StreetsSJ.txt"));//reads the input file

	count = 0;
	while(in3.readLine()!=null){//counts number of lines in the file
		count +=1;
	}
	in3.close();
	
	BufferedReader in4 = new BufferedReader(new FileReader("data/StreetsSJ.txt"));
	
	count2 = 0;
	
	while(count2!=count){
		singleLine = in4.readLine();
		String[] temp = singleLine.split(" ");
		double[] data1 = theTree1.search(Integer.parseInt(temp[1]));
		double[] data2 = theTree1.search(Integer.parseInt(temp[2]));
		
		double x1= data1[0];
		double y1= data1[1];
		
		double x2= data2[0];
		double y2= data2[1];
		///////////////////////////g2d.drawLine((int)x1+150, (int)y1+10, (int)x2+150, (int)y2+10);
		Polyline test = new Polyline();
		dubArray = convert(x1, y1);
		test.startPath(dubArray[0],dubArray[1]);
		dubArray1 = convert(x2, y2);
		test.lineTo(dubArray1[0], dubArray1[1]);
		streetsLayer.addGraphic(new Graphic(test, street));
		
		
		count2++;
	}
	in4.close();
	
	//////////////////////////////////////
    
   /* Polyline steetCoords = new Polyline();
    convert(-121.904167, 41.974556);
    steetCoords.startPath(temp[0],temp[1]);
    
    Graphic streetGraphic = new Graphic(steetCoords, street);
    streetsLayer.addGraphic(streetGraphic);*/
    
    myDrawingOverlay = new DrawingOverlay();
    myDrawingOverlay.addDrawingCompleteListener(new DrawingCompleteListener() {
		
		@Override
		public void drawingCompleted(DrawingCompleteEvent event) {
			// get the user-drawn stop graphic from the overlay
			Graphic graphic = (Graphic) myDrawingOverlay.getAndClearFeature();
			// add it to the graphicsLayer for display
			graphicsLayer.addGraphic(graphic);
			// add a text graphic showing the number of the current stop
			numStops++;
			Graphic textGraphic = new Graphic(
			  graphic.getGeometry(), new TextSymbol(12, String.valueOf(numStops), Color.WHITE), 1);
			graphicsLayer.addGraphic(textGraphic);
			/*Graphic test = new Graphic(graphic.getGeometry(), new SimpleMarkerSymbol(Color.RED, 10, Style.CIRCLE), 1);
			graphicsLayer.addGraphic(test);*/
		}
	});
    map.addMapOverlay(myDrawingOverlay);
    
    // Add the JMap to the JFrame's content pane
    window.getContentPane().add(map);
    
    JLayeredPane contentPane = new JLayeredPane();
    contentPane.setLayout(new BorderLayout());
    contentPane.setVisible(true);
    window.add(contentPane);
    contentPane.add(map);
    contentPane.add(createToolBar(myDrawingOverlay), BorderLayout.NORTH);
  }
	
  private double[] convert(double mercatorX_lon, double mercatorY_lat)
  {
	      if ((Math.abs(mercatorX_lon) > 180 || Math.abs(mercatorY_lat) > 90))
	          return null;
	
	      double num = mercatorX_lon * 0.017453292519943295;
	      double x = 6378137.0 * num;
	      double a = mercatorY_lat * 0.017453292519943295;
	
	      mercatorX_lon = x;
	      mercatorY_lat = 3189068.5 * Math.log((1.0 + Math.sin(a)) / (1.0 - Math.sin(a)));
	      temp = new double[2];
	      temp[0] = mercatorX_lon;
	      temp[1] = mercatorY_lat;
	      return temp;
  }
	  
  private Component createToolBar(DrawingOverlay drawingOverlay) {
		JToolBar toolBar = new JToolBar();
		toolBar.setLayout(new FlowLayout(FlowLayout.CENTER));

		// add stops button
		final JButton stopButton = new JButton(STOP_BUTTON);
		stopButton.addActionListener(new ActionListener() {
			  @Override
			  public void actionPerformed(ActionEvent e) {
			    // Add a new stop graphic in the map overlay
				HashMap<String, Object> attributes = new HashMap<String, Object>();
			    attributes.put("type", "Stop");
			    drawingOverlay.setUp(
			        DrawingMode.POINT,
			        new SimpleMarkerSymbol(Color.BLUE, 25, Style.CIRCLE),
			        attributes);
			  }
			});
		toolBar.add(stopButton);

		// solve route button
		final JButton solveRouteButton = new JButton(SOLVE_BUTTON);
		solveRouteButton.addActionListener(new ActionListener() {
			  @Override
			  public void actionPerformed(ActionEvent e) {
			    // disable the toolbar buttons and overlay 
			    stopButton.setEnabled(false);
			    solveRouteButton.setEnabled(false);
			    myDrawingOverlay.setEnabled(false);
			  }
			});
		toolBar.add(solveRouteButton);

		// reset button
		JButton resetButton = new JButton(RESET_BUTTON);
		resetButton.addActionListener(new ActionListener() {
			  @Override
			  public void actionPerformed(ActionEvent e) {
			    // enable the toolbar buttons and overlay  
			    stopButton.setEnabled(true);
			    solveRouteButton.setEnabled(true);
			    myDrawingOverlay.setEnabled(true);
			    // reset graphic layers, stop features and global variables
			    graphicsLayer.removeAll();
			    stops.clearFeatures();
			    numStops = 0;
			  }
			});
		toolBar.add(resetButton);
		return toolBar;
	}

  public static void main(String[] args) {
    EventQueue.invokeLater(new Runnable() {

      @Override
      public void run() {
        try {
          Testing application = new Testing();
          application.window.setVisible(true);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }
}
