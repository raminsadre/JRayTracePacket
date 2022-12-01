package jraytrace;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 *
 * @author ramin
 */
public class WindowOutputDevice extends OutputDevice {
    private JFrame frame ;
    private JPanel panel ;
    private BufferedImage canvas ;
    
    public WindowOutputDevice() {
    }
    
    @Override
    public void open(int width,int height) {
        frame=new JFrame("Output window") ;
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);        
        canvas=new BufferedImage(width,height, BufferedImage.TYPE_INT_ARGB) ;
        
        panel=new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(canvas.getWidth(), canvas.getHeight());
            }

            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.drawImage(canvas, null, null);
            }            
        } ;
        frame.add(panel) ;
        frame.pack();
        frame.setVisible(true);       
    }
    
    @Override
    public void flush() {
        panel.repaint() ;
    }
    
    @Override
    public void close() {
        flush() ;
    }
    
    @Override
    public void setPixel(int x,int y,Color c) {
        // canvas.setRGB(x, y, c.getRGB());
        // This is a few milliseconds per frame faster:
        canvas.getRaster().getDataBuffer().setElem(y*canvas.getHeight()+x, c.getRGB());
    }

    public JPanel getPanel() {
        return panel;
    }
}
