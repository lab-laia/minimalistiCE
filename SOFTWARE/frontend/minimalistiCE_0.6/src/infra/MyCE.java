package infra;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.logging.Level;
import java.util.logging.Logger;
import jlaia.Geral;
import serialSupport.SerineSerial;
import serineDispatcher.VirtualDevice;
import jlaia.NumString;
import jlaia.SimpleFileWriter;
import java.util.Timer;
import java.util.TimerTask;

/**
 * For two openC4D
 *
 * @author Claudimir Lucio do Lago
 */
public abstract class MyCE {

    public enum OptionTimeBase {
        MILLISECOND, SECOND, MINUTE, HOUR
    }

    public enum States {
        IDLE, WAITING_SAMPLE, PRE_INJECTING, INJECTING, WAITING_BGE,
        PRE_RUNNING, RUNNING, PRE_POST_RUN, POST_RUN, PROG_ENDED
    }

    // standard stuffs for virtual devices
    SerineSerial serinePort;
    Master master;
    String myID;
    String myIdentification;
    String lastMessage = "";
    boolean inTouch = false;
    String prefixC4D;
    String prefixCE;
    String prefixHV;
    String prefixPump;
    String engagedWith = "";
    // machine state    
    States state = States.IDLE;
    public boolean overAndOver = false;
    // stuffs for the C4D
    public BufferC4D buffer;
    private final DecimalFormat formatador = new DecimalFormat();
    private double factorTime = 1.0;
    private final SimpleFileWriter arq;
    private final SimpleFileWriter arqBackup;
    //private String prefixArq = "";
    private String runName = "";
    private boolean shouldSave = false;
    private boolean mustBackup = false;
    private boolean c4dRunning = false;

    private int numberOfClicks = 0;
    public Parameters par = new Parameters();
    /*
    g – f se GFI (ground fault indicator) da fonte estiver ativo e n se estiver normal (alerta sobre possível problema com o acoplamento de terra); 
    r – r se estiver rodando programa e s em caso contrário; 
    e – e se a fonte estiver habilitada e d se estiver desabilitada; 
    p – + se estiver no modo positivo e - se estiver no modo negativo; 
    v – v se estiver no modo de tensão controlada e u se não estiver; 
    i – i se estiver no modo de corrente controlada e u se não estiver;
    t - number of steps; 
    c - program count; 
    f - frozen (f) or unfrozen (u).
     */
    private boolean hvGFI = false;
    private boolean hvRunning = false;
    private boolean hvEnabled = false;
    private boolean hvPositive = true;
    private boolean hvVcontrolled = false;
    private boolean hvIcontrolled = false;
    private int hvNumberOfSteps = 0;
    private int hvProgramCount = 0;
    private boolean hvFrozen = false;
    /*
    t – tempo (0 000 000 a 9 999 999 ms); 
    s – sinal (‘+’ ou ‘-‘) do potencial; 
    v – potencial (entre 00 000 e 30 000 V); 
    iT – corrente total (entre 000 000 e 300 000 nA); 
    iR – corrente de retorno (entre 000 000 e 300 000 nA).
     */
    private int hvTime = 0;
    private int hvVoltage = 0;
    private int hvTcurrent = 0;
    private int hvRcurrent = 0;
    /*
    i - is controlling pressure ('y' or 'n'); 
    v - pressure (000 000 to 999 999 Pa); 
    t - time (00 000 to 99 999 s);
     */
    private boolean pumpControlling = false;
    private int pumpPressure = 0;
    private int pumpTime = 0;
    private int iop = 0;

    public String getMyIdentification() {
        return myIdentification;
    }

    public boolean isSaving() {
        return shouldSave;
    }

    public States getState() {
        return state;
    }

    public int getNumberOfClicks(boolean clear) {
        int n = numberOfClicks;
        if (clear) {
            numberOfClicks = 0;
        }
        return n;
    }

    public void setMustBackup(boolean mustBackup) {
        this.mustBackup = mustBackup;
    }

    public boolean isHvGFI() {
        return hvGFI;
    }

    public boolean isHvR() {
        return hvRunning;
    }

    public boolean isHvEnabled() {
        return hvEnabled;
    }

    public boolean isHvPositive() {
        return hvPositive;
    }

    public boolean isHvVcontrolled() {
        return hvVcontrolled;
    }

    public boolean isHvIcontrolled() {
        return hvIcontrolled;
    }

    public int getHvNumberOfSteps() {
        return hvNumberOfSteps;
    }

    public int getHvProgramCount() {
        return hvProgramCount;
    }

    public boolean isHvFrozen() {
        return hvFrozen;
    }

    public double getHvTime_s() {
        return hvTime * 0.001;
    }

    public int getHvVoltage() {
        return hvVoltage;
    }

    public int getHvTcurrent() {
        return hvTcurrent;
    }

    public int getHvRcurrent() {
        return hvRcurrent;
    }

    public void hvBeep() {
        send(prefixHV + "B;");
    }

    public final void hvClear() {
        send(prefixHV + "C;");
        sleep(10);
    }

    public void hvDisable() {
        send(prefixHV + "D;");
    }

    public void hvEnable() {
        send(prefixHV + "E;");
    }

    public void hvFreeze() {
        send(prefixHV + "F;");
    }

    public void hvUnfreeze() {
        send(prefixHV + "U;");
    }

    public void hvRun() {
        send(prefixHV + "R;");
    }

    public void hvHalt() {
        send(prefixHV + "H;");
    }

    private void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException ex) {
            Logger.getLogger(MyCE.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private int constrain(int v, int min, int max) {
        if (v < min) {
            v = min;
        } else if (v > max) {
            v = max;
        }
        return v;
    }

    public void hvSetProgram(double timeSeconds, boolean positive, double voltageKV, double currentUA) {
        int time = constrain((int) (timeSeconds * 1000.0), 0, 9999999);
        int voltage = constrain((int) (voltageKV * 1000.0), 0, 30000);
        int current = constrain((int) (currentUA * 1000.0), 0, 200000);
        String message = prefixHV + "P0000000+00000000000;";  // HV is applied on the injection side
        if (!positive) {
            message = prefixHV + "P0000000-00000000000;";
        }
        message = NumString.put(time, 3, 9, message);
        message = NumString.put(voltage, 11, 15, message);
        message = NumString.put(current, 16, 21, message);
        send(message);
        sleep(10);
    }

    public void pumpSetPressure(double pressurekPa, double timeSeconds) {
        int time = constrain((int) Math.round(timeSeconds), 0, 99);
        int pressure = constrain((int) Math.round(pressurekPa), 0, 40);
        String message = prefixPump + "P00000;";
        message = NumString.put(pressure, 3, 5, message);
        message = NumString.put(time, 6, 7, message);
        send(message);
    }

    public void pumpSetPressure(double pressurekPa) {
        int pressure = constrain((int) Math.round(pressurekPa), 0, 40);
        String message = prefixPump + "P000;";
        message = NumString.put(pressure, 3, 5, message);
        send(message);
    }

    public final void pumpHalt() {
        send(prefixPump + "H;");
    }

    public void pumpZero() {
        send(prefixPump + "Z;");
    }

    public void askIoP() {
        send(prefixPump + "W;");
    }

    public int getIoP() {
        return iop;
    }

    boolean pumpMemo = false;
    boolean valveMemo = false;

    public void pumpOn(boolean pump) {
        pumpMemo = pump;
        String message = prefixPump + "O" + (pumpMemo ? "N" : "F") + (valveMemo ? "N" : "F") + ";";
        send(message);
    }

    public void valveOn(boolean valve) {
        valveMemo = valve;
        String message = prefixPump + "O" + (pumpMemo ? "N" : "F") + (valveMemo ? "N" : "F") + ";";
        send(message);
    }

    public boolean isPumpControlling() {
        return pumpControlling;
    }

    public int getPumpPressure() {
        return pumpPressure;
    }

    public int getPumpTime() {
        return pumpTime;
    }

    public void setLED(int led) {
        String message = prefixCE + "L" + constrain(led, 0, 5) + ";";
        send(message);
    }

    /**
     * Just to see what is been sent to the serial. It is helpful to try to
     * identify if a Serine virtual device is attached to the port.
     *
     * @return the last message receive since the last request
     */
    public String getLastMessage() {
        String message = lastMessage;
        lastMessage = "";
        return message;
    }

    /**
     * @return true if the serial communication is OK
     */
    public boolean isSerialConnected() {
        return serinePort.isConnected();
    }

    /**
     * @return true if the serial port is opened
     */
    public boolean openSerialPort() {
        if (serinePort.isConnected()) {
            serinePort.close();
        }
        return serinePort.connect(par.serialPort, par.baudRate);
    }

    /**
     * Constructor
     *
     * @param id_for_the_master a character valid to identify the master
     * @param permanentIdentification the Serine permanent identification of the
     * @param blockA True if detectors are in block A, or false if they are in
     * block B master
     */
    public MyCE(char id_for_the_master, String permanentIdentification, boolean blockA) {
        this.buffer = new BufferC4D(100);
        master = new Master(id_for_the_master, permanentIdentification);
        serinePort = new SerineSerial();
        serinePort.setAddressed(master);
        prefixCE = "g" + myID;
        prefixHV = "h" + myID;
        prefixPump = "p" + myID;
        prefixC4D = "d" + myID;
        arq = new SimpleFileWriter();
        arqBackup = new SimpleFileWriter();
        par = Parameters.retrieve("last.par");

        Timer timer = new Timer();
        TimerTask tarefa = new TimerTask() {
            @Override
            public void run() {
                machineState();
            }
        };
        timer.scheduleAtFixedRate(tarefa, 100, 503);
    }

    /**
     * Not only close the serial port, but it also runs the disengage method.
     */
    public void close() {
        disengage();
        serinePort.close();
    }

    /**
     * Send a Serine message. The syntax is not checked.
     *
     * @param message a string containing a valid Serine message
     */
    public void send(String message) {
        if (serinePort.isConnected()) {
            serinePort.postHere(message);
        } else {
            System.out.println(System.currentTimeMillis() + "  " + message + " The serial port is not connected.");
        }
    }

    /**
     * Put C4D in wait-for-start-and-stop condition.
     */
    public void runC4D() {
        send(prefixC4D + "Gr;");
    }

    /**
     * Sends a haltC4D message to the detector
     */
    public final void haltC4D() {
        send(prefixC4D + "Gh;");
    }

    /**
     * Ask for just one point from the detector.
     */
    public final void getOneReadC4D() {
        send(prefixC4D + "G;");
    }

    /**
     * Zeroes the chronometer of the detector.
     */
    public final void resetC4Dchronometer() {
        send(prefixC4D + "Z;");
    }

    public void startRun() {
        shouldSave = false;
        getNumberOfClicks(true);
        if (!arq.isOpen()) {
            String now = Geral.timeStamp("d");
            runName = par.prefix + now;
            arq.open(par.directory + runName + par.suffix);
            if (mustBackup) {
                arqBackup.open(par.directoryBackup + runName + "_BK" + par.suffix);
            }
        }
        buffer.reset();
        hvClear();
        hvSetProgram(0.5, par.inj_positive, par.inj_Voltage, par.inj_Current);
        hvSetProgram(par.inj_Time_s, par.inj_positive, par.inj_Voltage, par.inj_Current);
        state = States.WAITING_SAMPLE;
        setLED(2);
    }

    public void stopRun() {
        shouldSave = false;
        if (arq.isOpen()) {
            arq.close();
        }
        if (arqBackup.isOpen()) {
            arqBackup.close();
        }
        runName = "";
        runC4D();
        hvHalt();
        pumpHalt();
        state = States.PROG_ENDED;
        setLED(0);
        upDateStop();
    }

    public abstract void upDateMachineState();
    
    public abstract void upDateStop();

    private void machineState() {
        switch (state) {
            case IDLE:
                break;
            case WAITING_SAMPLE:
                switch (getNumberOfClicks(true)) {
                    case 1:  // start the injection
                        if (par.inj_Pressure > 0.0) {
                            pumpSetPressure(par.inj_Pressure, par.inj_Time_s);
                        }
                        setLED(1);
                        if (par.inj_Voltage > 0.0) {
                            hvRun();
                            setLED(3);
                        }
                        state = States.PRE_INJECTING;
                        break;
                    case 2:  // Jump injection
                        hvHalt();
                        pumpHalt();
                        setLED(2);
                        state = States.WAITING_BGE;
                        break;
                }
                break;
            case PRE_INJECTING:
                if (hvRunning || pumpControlling) {
                    state = States.INJECTING;
                } else if (getNumberOfClicks(true) == 2) {
                    hvHalt();
                    pumpHalt();
                    setLED(2);
                    state = States.WAITING_BGE;
                }
                break;
            case INJECTING:
                if ((!hvRunning && !pumpControlling) || (getNumberOfClicks(true) == 2)) {
                    // Injection is over, so...
                    hvClear(); // prepare
                    hvSetProgram(0.5, par.run_positive, par.run_Voltage, par.run_Current);
                    hvSetProgram(par.run_Time_min * 60.0, par.run_positive, par.run_Voltage, par.run_Current);
                    setLED(2);
                    state = States.WAITING_BGE;
                }
                break;
            case WAITING_BGE:
                switch (getNumberOfClicks(true)) {
                    case 1: // Go to pre_running
                        if (par.run_Pressure > 0.0) {
                            pumpSetPressure(par.run_Pressure);
                        }
                        hvRun();
                        setLED(3);
                        state = States.PRE_RUNNING;
                        break;
                    case 2: // Abort the run
                        stopRun();
                        break;
                }
                break;
            case PRE_RUNNING:
                if (hvRunning) {
                    buffer.reset();
                    runC4D();
                    shouldSave = arq.isOpen();
                    state = States.RUNNING;
                } else if (getNumberOfClicks(true) > 1) {
                    stopRun();
                }
                break;
            case RUNNING:
                if ((!hvRunning) || (getNumberOfClicks(true) > 1)) {
                    shouldSave = false;
                    hvHalt();
                    pumpHalt();
                    hvClear(); // prepare the post run
                    hvSetProgram(0.5, par.post_positive, par.post_Voltage, par.post_Current);
                    hvSetProgram(par.post_Time_min * 60.0, par.post_positive, par.post_Voltage, par.post_Current);
                    state = States.PRE_POST_RUN;
                    if (par.post_Pressure > 0.0) {
                        pumpSetPressure(par.post_Pressure);
                    }
                    if (par.post_Voltage > 0.0) {
                        hvRun();
                    }
                    if (par.post_Voltage == 0.0 && par.post_Pressure == 0.0) {
                        stopRun();
                    }
                    setLED(5);
                }
                break;
            case PRE_POST_RUN:
                if (hvRunning || pumpControlling) {
                    pumpZero();
                    state = States.POST_RUN;
                } else if (getNumberOfClicks(true) > 1) {
                    stopRun();
                }
                break;
            case POST_RUN:
                if ((Math.min(getPumpTime(), getHvTime_s()) > par.post_Time_min * 60.0) || (getNumberOfClicks(true) > 1)) {
                    stopRun();
                }
                break;
            case PROG_ENDED:
                if (overAndOver) {
                    startRun();
                } else {
                    state = States.IDLE;
                    setLED(0);
                }
                break;
        }
        upDateMachineState();
    }

    /**
     * Essentially, this class allows the communication with the other virtual
     * devices as well as the message processing for the master
     */
    public class Master extends VirtualDevice {

        private Master(char master_ID, String master_Identification) {
            super(master_ID);
            myID = String.valueOf(master_ID);
            myIdentification = master_Identification;
        }

        /**
         * The message processor.
         */
        @Override
        public void processMessageToMe() {
            lastMessage = messageString;
            switch (messageChar[1]) {
                case 'd':   //  C4D
                    switch (messageChar[2]) {
                        case 'g':  // get
                            switch (messageChar[3]) {
                                case 's':  // status
                                    c4dRunning = messageChar[4] == 'T';
                                    break;
                                default:
                                    double time = factorTime * NumString.get(3, 9, messageString);
                                    int ad1 = NumString.getInt(10, 16, messageString) - 2097152;
                                    int ad0 = NumString.getInt(17, 23, messageString) - 2097152;
                                    buffer.putPoint(time, ad0, ad1);
                                    if (shouldSave) {
                                        String linha = formatador.format(time) + par.separator + ad0 + par.separator + ad1;
                                        arq.writeln(linha);
                                        if (arqBackup.isOpen()) {
                                            arqBackup.writeln(linha);
                                        }
                                    }
                                    break;
                            }
                            break;
                    }
                    break;
                case 'h':   //  HV
                    switch (messageChar[2]) {
                        case 'g':  // get
                            hvTime = NumString.getInt(3, 9, messageString);
                            hvVoltage = NumString.getInt(11, 15, messageString);
                            hvTcurrent = NumString.getInt(16, 21, messageString);
                            hvRcurrent = NumString.getInt(22, 27, messageString);
                            if (messageChar[10] == '-') {
                                hvVoltage = -hvVoltage;
                            }
                            break;
                        case 's':  // status
                            hvGFI = messageChar[3] == 'f';
                            hvRunning = messageChar[4] == 'r';
                            hvEnabled = messageChar[5] == 'e';
                            hvPositive = messageChar[6] == '+';
                            hvVcontrolled = messageChar[7] == 'v';
                            hvIcontrolled = messageChar[8] == 'i';
                            hvNumberOfSteps = messageChar[9] - '0';
                            hvProgramCount = messageChar[10] - '0';
                            hvFrozen = messageChar[11] == 'f';
                            break;
                    }
                    break;
                case 'p':   //  Pump
                    switch (messageChar[2]) {
                        case 'w':  // IoP
                            iop = NumString.getInt(3, 10, messageString);
                            break;
                        case 's':  // status
                            pumpControlling = messageChar[3] == 'y';
                            pumpPressure = NumString.getInt(4, 9, messageString);
                            pumpTime = NumString.getInt(10, 14, messageString);
                            break;
                    }
                    break;
                case 'g':   //  general
                    switch (messageChar[2]) {
                        case 'B':
                            numberOfClicks = messageChar[3] - '0';
                            break;
                        case 'i':
                            engagedWith = String.copyValueOf(messageChar, 3, messageChar.length - 4);
                            System.out.println(System.currentTimeMillis() + " engaged with " + engagedWith);
                            break;
                        case 'x':
                            inTouch = (messageChar[3] == 'N');
                            if (inTouch) {
                                send(prefixCE + "I;");
                            } else {
                                engagedWith = "";
                            }
                            break;
                    }
                    break;
            }
        }

        /**
         * Allows processing the received broadcast messages.
         */
        @Override
        public void processBroadcastMessage() {
            lastMessage = messageString;
            switch (messageChar[2]) {
                case 'x':
                    break;
            }
        }

    ;

    }

    /**
     * This method should be populated with messages exchange and other
     * stuffs to assure that master and slave had agreed to communicate with
     * each other. This is not about the serial communication. This is the
     * contrary of the disengage method.
     */
    public void engage() {
        send(prefixCE + "XN;");
        send(prefixCE + "STTT;");
        runC4D();
        hvClear();
        pumpHalt();
    }

    /**
     * This method should be populated with messages exchange and other stuffs
     * to assure that master and slave lose connection without problems. This is
     * the contrary of the engage method.
     */
    public void disengage() {
        if (isSerialConnected()) {
            System.out.println("I'm going to disengage...");
            haltC4D();
            hvClear();
            pumpHalt();
            send(prefixCE + "SFFF;");
            send(prefixCE + "XF;");
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Logger.getLogger(MyCE.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * @return true if master and slave are committed
     */
    public boolean isEngaged() {
        return inTouch;
    }

    /**
     * @return a string with the id given by the device
     */
    public String getEngagedWith() {
        return engagedWith;
    }

    /**
     * Select the time base for the data from the detector
     *
     * @param op the option according OptionTimeBase
     * @param decimalPoint The symbol to be used as the decimal separator.
     */
    public void setTimeBase(OptionTimeBase op, char decimalPoint) {
        DecimalFormatSymbols setDec = new DecimalFormatSymbols();
        setDec.setDecimalSeparator(decimalPoint);
        formatador.setDecimalFormatSymbols(setDec);
        if (null != op) {
            switch (op) {
                case MILLISECOND:
                    factorTime = 1.0;
                    formatador.applyPattern("#");
                    break;
                case SECOND:
                    factorTime = 1.0 / 1000.0;
                    formatador.applyPattern("0.000");
                    break;
                case MINUTE:
                    factorTime = 1.0 / 60000.0;
                    formatador.applyPattern("0.00000");
                    break;
                case HOUR:
                    factorTime = 1.0 / 3600000.0;
                    formatador.applyPattern("0.0000000");
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * @return the complete filename
     */
    public String getRunName() {
        return runName;
    }

    /**
     * @return true if an electropherogram is being acquired
     */
    public boolean isC4Drunning() {
        return c4dRunning;
    }

    /**
     * @return true if data and backup files are closed
     */
    public boolean areFilesClosed() {
        return !(arq.isOpen() || arqBackup.isOpen());
    }

}
