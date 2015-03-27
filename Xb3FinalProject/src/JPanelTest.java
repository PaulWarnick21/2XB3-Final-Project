import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.JPanel;

class Surface extends JPanel {

    private void doDrawing(Graphics g) throws IOException {

        Graphics2D g2d = (Graphics2D) g;

        g2d.setColor(Color.blue);

        String singleLine = "";
		
		BufferedReader in1 = new BufferedReader(new FileReader("Data/TG.txt"));//reads the input file

		int count = 0;
		while(in1.readLine()!=null){//counts number of lines in the file
			count +=1;
		}
		in1.close();
		
		BufferedReader in2 = new BufferedReader(new FileReader("Data/TG.txt"));
		
		int count2 = 0;
		
		int[] nodes = new int[count];
		double[][] coordinates = new double[count][2];
		while(count2!=count){
			singleLine = in2.readLine();
			String[] temp = singleLine.split(" ");
			nodes[count2] = Integer.parseInt(temp[0]);
			coordinates[count2][0]= (Double.parseDouble(temp[1]))/15;
			coordinates[count2][1]= (Double.parseDouble(temp[2]))/15;
			double x = coordinates[count2][0];
			double y = coordinates[count2][1];
			g2d.drawLine((int)x+150, (int)y+10, (int)x+150, (int)y+10);
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
		
		BufferedReader in3 = new BufferedReader(new FileReader("Data/TG2.txt"));//reads the input file

		count = 0;
		while(in3.readLine()!=null){//counts number of lines in the file
			count +=1;
		}
		in3.close();
		
		BufferedReader in4 = new BufferedReader(new FileReader("Data/TG2.txt"));
		
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
			g2d.drawLine((int)x1+150, (int)y1+10, (int)x2+150, (int)y2+10);
			
			count2++;
		}
		in4.close();
    }

    @Override
    public void paintComponent(Graphics g) {

        super.paintComponent(g);
        try {
			doDrawing(g);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}


public class JPanelTest extends JFrame{
	public JPanelTest(){
		initUI();
	}
	
	private void initUI(){
		setTitle("Points");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		add(new Surface());
		
		setSize(1000, 1000);
		setLocationRelativeTo(null);
	}
	
	public static void main(String[] args) {

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            
            public void run() {

                JPanelTest ps = new JPanelTest();
                ps.setVisible(true);
            }
        });
    }
}
