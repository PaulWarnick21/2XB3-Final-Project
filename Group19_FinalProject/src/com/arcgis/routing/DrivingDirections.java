package com.arcgis.routing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Polygon;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol.Style;
import com.esri.core.symbol.TextSymbol;
import com.esri.core.tasks.na.NAFeaturesAsFeature;
import com.esri.core.tasks.na.Route;
import com.esri.core.tasks.na.RouteDirection;
import com.esri.core.tasks.na.RouteParameters;
import com.esri.core.tasks.na.RouteResult;
import com.esri.core.tasks.na.RouteTask;
import com.esri.map.GraphicsLayer;
import com.esri.map.JMap;
import com.esri.map.MapOptions;
import com.esri.map.MapOptions.MapType;
import com.esri.toolkit.overlays.DrawingCompleteEvent;
import com.esri.toolkit.overlays.DrawingCompleteListener;
import com.esri.toolkit.overlays.DrawingOverlay;
import com.esri.toolkit.overlays.DrawingOverlay.DrawingMode;

public class DrivingDirections {
	
	private JFrame window;
	private JMap map;
	private GraphicsLayer graphicsLayer; // graphics layer for stops and whole route
	private GraphicsLayer routeSegmentsLayer; //graphics layer for route segment graphics
	private SimpleLineSymbol routeSymbol = new SimpleLineSymbol(Color.BLUE, 5); //Symbol for routes
	private DrawingOverlay myDrawingOverlay; //Overlay to draw your route stops
	private RouteTask task; //object to perform routing
	private NAFeaturesAsFeature stops = new NAFeaturesAsFeature(); //stop graphics
	private static final String ROUTE_URL = "http://sampleserver6.arcgisonline.com/arcgis/rest/services/NetworkAnalysis/SanDiego/NAServer/Route"; // routing service
	private static final String SOLVE_BUTTON = " Solve route "; // solve button string 
	private static final String STOP_BUTTON = " Add a stop "; // stop button string
	private static final String TURN_BUTTON = " Turn by Turn "; // turn by turn button string
	private static final String RESET_BUTTON = " Reset "; // reset button string
	private JTextArea directionText; // text area to display directions
	private JButton stepsButton; // turn by turn directions
	private int numStops = 0;
	private int stepRoute = 0;
	ArrayList<Integer> stepIDs = new ArrayList<Integer>();
	
	public DrivingDirections() {
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
	
	    // Before this application is deployed you must register the application on 
	    // http://developers.arcgis.com and set the Client ID in the application as shown 
	    // below. This will license your application to use Basic level functionality.
	    // 
	    // If you need to license your application for Standard level functionality, please 
	    // refer to the documentation on http://developers.arcgis.com
	    //
	    //ArcGISRuntime.setClientID("your Client ID");
	
	    // Using MapOptions allows for a common online basemap to be chosen
	    MapOptions mapOptions = new MapOptions(MapType.STREETS);
	    map = new JMap(mapOptions);
	    map.setExtent(new Envelope(-13054452, 3847753, -13017762, 3866957.78)); // set map extent to San Diego
	    graphicsLayer = new GraphicsLayer();
	    routeSegmentsLayer = new GraphicsLayer();
	    routeSegmentsLayer.setSelectionColor(Color.RED);
	    
	    map.getLayers().add(routeSegmentsLayer); // add segments route layer first
	    map.getLayers().add(graphicsLayer); // adds stop/route layer next, so that stops display above segments
	    myDrawingOverlay = new DrawingOverlay();
	    myDrawingOverlay.addDrawingCompleteListener(new DrawingCompleteListener() {
			
			@Override
			public void drawingCompleted(DrawingCompleteEvent event) {
				// get the user-drawn stop graphic from the overlay
				Graphic graphic = (Graphic) myDrawingOverlay.getAndClearFeature();
				// add it to the graphicsLayer for display
				graphicsLayer.addGraphic(graphic);
				// add to stops list for route task
				stops.addFeature(graphic);
				// add a text graphic showing the number of the current stop
				numStops++;
				Graphic textGraphic = new Graphic(
				  graphic.getGeometry(), new TextSymbol(12, String.valueOf(numStops), Color.WHITE), 1);
				graphicsLayer.addGraphic(textGraphic);				
			}
		});
	    map.addMapOverlay(myDrawingOverlay);
	    
	    // If you don't use MapOptions, use the empty JMap constructor and add a tiled layer
	    //map = new JMap();
	    //ArcGISTiledMapServiceLayer tiledLayer = new ArcGISTiledMapServiceLayer(
	    //  "http://services.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer");
	    //map.getLayers().add(tiledLayer);
	
	    // Add the JMap to the JFrame's content pane
	    window.getContentPane().add(map);
	    
	    JLayeredPane contentPane = new JLayeredPane();
	    contentPane.setLayout(new BorderLayout());
	    contentPane.setVisible(true);
	    window.add(contentPane);
	    contentPane.add(createPanel());
	    contentPane.add(map);
	    contentPane.add(createToolBar(myDrawingOverlay), BorderLayout.NORTH);
	    
	    try {
	    	  task = RouteTask.createOnlineRouteTask(ROUTE_URL, null);
	    	} catch (Exception e) {
	    	  e.printStackTrace();
	    	  JOptionPane.showMessageDialog(map.getParent(), 
	    	    "An error has occurred. " + e.getLocalizedMessage());
	    	}
	}

	private Component createPanel() {
		
		// driving directions panel
		JComponent panel = new JPanel();
		panel.setLocation(10, 50);
		panel.setBackground(new Color(0, 0, 0, 100));
		panel.setBorder(new LineBorder(Color.BLACK, 1, false));
		panel.setLayout(new BorderLayout());
		panel.setSize(200, 150);
		
		// panel title
		JLabel txtTitle = new JLabel("Driving Directions");
		txtTitle.setHorizontalAlignment(SwingConstants.CENTER);
		txtTitle.setFont(new Font(txtTitle.getFont().getName(), Font.BOLD, 14));
		txtTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
		txtTitle.setForeground(Color.WHITE);
		
		// scrolling text area to display direction text
		directionText = new JTextArea();
		directionText.setLineWrap(true);
		directionText.setWrapStyleWord(true);
		directionText.setEditable(false);
		directionText.setBorder(BorderFactory.createEmptyBorder(15,15,15,15));
		JScrollPane scrollPane = new JScrollPane(directionText);
		
		stepsButton = new JButton(TURN_BUTTON); // button - when clicked, show turn by turn directions
		stepsButton.addActionListener(new ActionListener() {
		      
			  @Override
			  public void actionPerformed(ActionEvent e) {
				  doTurnByTurn();
			        
			  }
			});
		
		// group the above UI items into the JPanel
		panel.add(txtTitle, BorderLayout.NORTH);
		panel.add(scrollPane, BorderLayout.CENTER);
		panel.add(stepsButton, BorderLayout.SOUTH);
		
		return panel;
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
			    // enable turn by turn button
			    stepsButton.setEnabled(true);
			    doRouting();
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
			    // disable the turn by turn button
			    stepsButton.setEnabled(false);
			    // reset graphic layers, stop features and global variables
			    directionText.setText("");
			    graphicsLayer.removeAll();
			    routeSegmentsLayer.removeAll();
			    stops.clearFeatures();
			    stepIDs.clear();
			    numStops = 0;
			    stepRoute = 0;
			  }
			});
		toolBar.add(resetButton);
		return toolBar;
	}

	protected void doTurnByTurn() {
		// check there is a next step in the route
		if (stepRoute < routeSegmentsLayer.getNumberOfGraphics()) {
			Graphic selected = routeSegmentsLayer.getGraphic(stepIDs.get(stepRoute).intValue());   
			// Highlight route segment on the map
			routeSegmentsLayer.select(stepIDs.get(stepRoute).intValue());
			String direction = ((String) selected.getAttributeValue("text"));
			Double time = (Double) selected.getAttributeValue("time");
			Double length = (Double) selected.getAttributeValue("length");
			// Update the label with this direction's information
			String label = String.format("%s%nTime: %.1f minutes, Length: %.1f miles",
					direction, time, length);
			directionText.setText(label);
			stepRoute++;
			} 
		else {
			directionText.setText("End of route");
			stepsButton.setEnabled(false);
		}
		
	}

	protected void doRouting() {
		// initialise route results and parameters
		RouteResult result = null;
		RouteParameters parameters = null;
		try {
		  parameters = task.retrieveDefaultRouteTaskParameters();
		  parameters.setOutSpatialReference(map.getSpatialReference());
		  stops.setSpatialReference(map.getSpatialReference());
		  parameters.setStops(stops);
		  // set parameter to return turn by turn directions
		  parameters.setReturnDirections(true);
		  result = task.solve(parameters);
		  showResult(result);
		} catch (Exception e) {
		  e.printStackTrace();
		  JOptionPane.showMessageDialog(map.getParent(), 
		    "An error has occurred. " + e.getLocalizedMessage());
		}	
	}

	// --------------------------------- TODO --------------------------------- // 
	private void showResult(RouteResult result) { 
		if (result != null) {
		    // get the routing directions from the top route
		    Route topRoute = result.getRoutes().get(0);
		    topRoute.getRoutingDirections();
	
		    // add route segments to the route layer
		    for (RouteDirection rd : topRoute.getRoutingDirections()) {
		    	HashMap<String, Object> attribs = new HashMap<String, Object>();
		    	attribs.put("text", rd.getText());
		    	attribs.put("time", Double.valueOf(rd.getMinutes()));
		    	attribs.put("length", Double.valueOf(rd.getLength()));
		      
		    	// --------------------------------- TODO --------------------------------- //
		      
		    	if (rd.getManeuver().toString().contains("Left")) { System.out.println(rd.getManeuver().toString()); }
		    	//System.out.println(rd.getManeuver().toString());
		      
		    	// --------------------------------- TODO --------------------------------- //
		    	Graphic a = new Graphic(rd.getGeometry(), routeSymbol, attribs);
		    	int graphicID = routeSegmentsLayer.addGraphic(a);
		    	stepIDs.add(Integer.valueOf(graphicID));
		    }
		// --------------------------------- TODO --------------------------------- // 
	
		    // add the whole-route graphic
		    Graphic routeGraphic = new Graphic(topRoute.getRouteGraphic().getGeometry(),
		        new SimpleLineSymbol(Color.BLUE, 2.0f), 0);
		    graphicsLayer.addGraphic(routeGraphic);
		    
		    // Get the full route summary and show in text area
		    String routeSummary = String.format(
		      "%s%nTotal time: %.1f minutes %nLength: %.1f miles",
		      topRoute.getRouteName(), Double.valueOf(topRoute.getTotalMinutes()),
		      Double.valueOf(topRoute.getTotalMiles()));
		    directionText.setText(routeSummary);

		    // Zoom to the extent of the whole route plus a 500m buffer
		    Polygon bufferedExtent = GeometryEngine.buffer(
		      topRoute.getEnvelope(), map.getSpatialReference(), 500, null);
		    map.setExtent(bufferedExtent);
		  }
		  else {
		    JOptionPane.showMessageDialog(map.getParent(), "No route found!");
		  }
		}		

/**
   * Starting point of this application.
   * @param args
   */
  public static void main(String[] args) {
    EventQueue.invokeLater(new Runnable() {

      @Override
      public void run() {
        try {
          DrivingDirections application = new DrivingDirections();
          application.window.setVisible(true);
        } catch (Exception e) {
          e.printStackTrace();
          }
      }
    });
  }
}
