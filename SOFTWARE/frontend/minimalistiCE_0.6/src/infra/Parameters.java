package infra;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * A class containing a set of parameters to be saved and retrieved from one to
 * other run.
 *
 * @author Claudimir Lucio do Lago
 */
public class Parameters implements Serializable {

    /**
     * The list of parameters to be saved. It is a good idea to set a initial
     * value to each one of them. These values will be used whether those ones
     * that were saved last time are lost.
     */
    public String serialPort = "COM3";
    public int baudRate = 115200;
    public String directory = "";
    public String directoryBackup = "";
    public String prefix = "Electropherogram_";
    public String suffix = ".dat";
    public char separator = '\t';
    
    public double inj_Pressure = 10.0;
    public double run_Pressure = 0.0;
    public double post_Pressure = 10.0;
    public double inj_Voltage = 0.0;
    public double run_Voltage = 10.0;
    public double post_Voltage = 10.0;
    public boolean inj_positive = true;
    public boolean run_positive = true;
    public boolean post_positive = true;
    public double inj_Current = 0.0;
    public double run_Current = 100.0;
    public double post_Current = 100.0;
    public double inj_Time_s = 5.0;  // s
    public double run_Time_min = 10.0;  // min
    public double post_Time_min = 60.0;  // min    
    
    public double capillaryLength = 50.0;
    public double voltage = 30.0;
    public double bgeConductivity = 4.74e-02;
    public double capillaryID = 50.0;
    public double tMfirst = 1.234;
    public double tMsecond = 1.234;
    public double tMref = 1.234;

    /**
     * Allows to save the parameters in a file
     *
     * @param fileName the complete path with the file name
     * @return true if everything is OK
     */
    public boolean save(String fileName) {
        boolean ok = false;
        try {
            try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(fileName)))) {
                out.writeObject(this);
                out.close();
                ok = true;
            }
        } catch (IOException ex) {
            System.out.println("Backup.save error: io error.");
        }
        return ok;
    }

    /**
     * Allows to retrieve the parameters saved in a file
     *
     * @param fileName the complete path with the file name
     * @return the previously stored parameters
     */
    static public Parameters retrieve(String fileName) {
        Parameters p = new Parameters();
        try {
            try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(fileName)))) {
                try {
                    p = (Parameters) in.readObject();
                    in.close();
                    calc();
                } catch (ClassNotFoundException ex) {
                    System.out.println("Backup.load error: unable to load object.");
                }
            }
        } catch (IOException ex) {
            System.out.println("Backup.load error: unable to open stream.");
        }
        return p;
    }

    /**
     * This method should be called after retrieving of parameters from a file
     * or after some change. The idea is to keep consistence among the values
     * and units.
     */
    public static void calc() {        
    }
}
