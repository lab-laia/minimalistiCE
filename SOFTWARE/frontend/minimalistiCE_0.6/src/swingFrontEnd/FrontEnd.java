package swingFrontEnd;

import com.formdev.flatlaf.FlatLightLaf;
import infra.BufferC4D;
import infra.MyCE;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import jssc.SerialPortList;
import javax.swing.JFileChooser;
import jlaia.NumString;
import jlaia.SimpleFileWriter;
import swingFrontEnd.graphUtil.XYGraph;
import swingFrontEnd.graphUtil.XYGraph2;

/**
 *
 * @author Claudimir Lucio do Lago
 */
public class FrontEnd extends javax.swing.JFrame {

    MyCE myCE;
    String[] baudRate = {"110", "150", "300", "1200", "2400", "4800", "9600",
        "19200", "38400", "57600", "115200", "230400", "460800", "921600"};
    private final XYGraph graph1st = new XYGraph("1st C4D", "time  /  min", 100000);
    private final XYGraph graph2nd = new XYGraph("2nd C4D", "time  /  min", 100000);
    private final XYGraph2 graphAdj = new XYGraph2("C4D", "adjusted time  /  min", 100000);
    private final XYGraph2 graphMonitor = new XYGraph2("C4D", "time  /  min", 10000);
    double fAdjT_1 = 1.0;
    double fAdjT_2 = 1.0;
    int sumBaseline = 0;
    int numBaseline = 0;
    int baseline = 0;

    private void initMyDevice() {

        myCE = new MyCE('m', "t_masterC4D", true) {
            @Override
            public void upDateMachineState() {
                if (myCE.isSerialConnected()) {
                    String message = myCE.getLastMessage();
                    if (!message.contentEquals("")) {
                        jLabel_from_the_port.setText(message);
                    }
                    if (myCE.isEngaged()) {
                        // update graphs            
                        BufferC4D.DataPoint point;
                        while (myCE.buffer.number() > 0) {
                            point = myCE.buffer.nextPoint();
                            numBaseline += 1;
                            sumBaseline += point.det1 - point.det0;
                            if (myCE.isSaving()) {
                                graph1st.letUpdate(jRadioButton_autoScale_1.isSelected());
                                graph1st.addPoint(point.time, point.det0, jRadioButton_autoScale_1.isSelected());
                                graph2nd.letUpdate(jRadioButton_autoScale_2.isSelected());
                                graph2nd.addPoint(point.time, point.det1, jRadioButton_autoScale_2.isSelected());
                                graphAdj.letUpdate(jRadioButton_autoScale_Adj.isSelected());
                                graphAdj.addPoint1(fAdjT_1 * point.time, point.det0 + baseline + 100, jRadioButton_autoScale_Adj.isSelected());
                                graphAdj.addPoint2(fAdjT_2 * point.time, point.det1 - baseline, jRadioButton_autoScale_Adj.isSelected());
                            } else {
                                graphMonitor.letUpdate(jRadioButton_autoScale_Adj.isSelected());
                                graphMonitor.addPoint1(fAdjT_1 * point.time, point.det0 + baseline + 100, jRadioButton_autoScale_Adj.isSelected());
                                graphMonitor.addPoint2(fAdjT_2 * point.time, point.det1 - baseline, jRadioButton_autoScale_Adj.isSelected());
                            }
                        }
                        // HV Status
                        jLabelHV_GFI.setText(myCE.isHvGFI() ? "ground fault!!!" : "no GFI");
                        jLabelHV_Enabled.setText(myCE.isHvEnabled() ? "enabled" : "disabled");
                        jLabelHV_running.setText(myCE.isHvR() ? "running" : "stoped");
                        jLabelHV_positive.setText(myCE.isHvPositive() ? "positive mode" : "negative mode");
                        jLabelHV_Vcontrolled.setText(myCE.isHvVcontrolled() ? "voltage controlled" : "free voltage");
                        jLabelHV_Icontrolled.setText(myCE.isHvIcontrolled() ? "current controlled" : "free current");
                        jLabelHV_Step.setText("step " + myCE.getHvProgramCount() + "/" + myCE.getHvNumberOfSteps());
                        jLabelHV_time.setText(NumString.format(myCE.getHvTime_s(), 10, 1) + " s");
                        jLabelHV_totalCurrent.setText(NumString.format(myCE.getHvTcurrent() / 1000.0, 8, 3) + " uA total");
                        jLabelHV_returnCurrent.setText(NumString.format(myCE.getHvRcurrent() / 1000.0, 8, 3) + " uA electrophoresis");
                        // C4D Status
                        jLabelC4D_running.setText(myCE.isC4Drunning() ? "running" : "idle");
                        // Pump Status
                        jLabelPump_pumping.setText(myCE.isPumpControlling() ? "pumping" : "idle");
                        jLabelPump_pressure.setText(NumString.format(myCE.getPumpPressure() / 1000.0, 5, 1) + " kPa");
                        jLabelPump_time.setText(myCE.getPumpTime() + " s");
                        jLabel_IoP.setText(myCE.getIoP() + " Pa s");
                        // status line
                        jLabel_Raw_Status.setText(myCE.getState().toString());
                        jLabelHV_voltage.setText(NumString.format(myCE.getHvVoltage() / 1000.0, 6, 2) + " kV");
                        jLabelHV_current.setText(NumString.format(myCE.getHvRcurrent() / 1000.0, 8, 3) + " uA");
                        jLabel_Pressure.setText(myCE.getPumpPressure() / 1000 + " kPa");
                        jTabbedPane_Input.setEnabled(myCE.getState() == MyCE.States.IDLE);
                        // status click
                        jLabel_NumberOfClicks.setText(myCE.getNumberOfClicks(false) + " clicks");

                        jTextField_fileName.setText(myCE.getRunName());
                        jLabel_areFilesClosed.setText(myCE.areFilesClosed() ? "closed" : "open");
                        jButton_save_Report.setEnabled(!myCE.areFilesClosed());
                    }
                }
            }

            @Override
            public void upDateStop() {
                jLabel_prog_pressure.setText("pressure / kPa");
                jLabel_prog_voltage.setText("voltage / kV");
                jLabel_prog_current.setText("current / uA");
                jLabel_prog_time.setText("time");
                graphMonitor.removeAllPoints();
                graph1st.renderer.setSeriesPaint(0, Color.DARK_GRAY);
                graph2nd.renderer.setSeriesPaint(0, Color.DARK_GRAY);
                graphAdj.renderer.setSeriesPaint(0, Color.CYAN);
                graphAdj.renderer.setSeriesPaint(1, Color.MAGENTA);
                myCE.buffer.reset();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    Logger.getLogger(FrontEnd.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        jComboBox_port.setModel(new javax.swing.DefaultComboBoxModel(SerialPortList.getPortNames()));
        jComboBox_baudRate.setModel(new javax.swing.DefaultComboBoxModel(baudRate));
        jPanel_This_Port.setVisible(false);
        jPanel_just_Test.setVisible(false);
        jComboBox_port.setSelectedItem(myCE.par.serialPort);
        jComboBox_baudRate.setSelectedItem(String.valueOf(myCE.par.baudRate));
        jLabel_directory.setText(myCE.par.directory);
        jLabel_directoryBackup.setText(myCE.par.directoryBackup);
        jTextField_Prefix.setText(myCE.par.prefix);
        jComboBox_complement.setSelectedItem(myCE.par.suffix);
        myCE.setTimeBase(MyCE.OptionTimeBase.MINUTE, '.');
        jTextField_fileName.setEditable(false);

        jTF_inj_Pressure.setText(NumString.format(myCE.par.inj_Pressure, 6, 1));
        jTF_inj_Voltage.setText(NumString.format(myCE.par.inj_Voltage, 6, 2));
        jTF_inj_Current.setText(NumString.format(myCE.par.inj_Current, 6, 1));
        jTF_inj_Time.setText(String.valueOf((int) myCE.par.inj_Time_s));
        jTB_polarity_inj.setSelected(!myCE.par.inj_positive);
        jTB_polarity_inj.doClick();

        jTF_run_Pressure.setText(NumString.format(myCE.par.run_Pressure, 6, 1));
        jTF_run_Voltage.setText(NumString.format(myCE.par.run_Voltage, 6, 2));
        jTF_run_Current.setText(NumString.format(myCE.par.run_Current, 6, 1));
        jTF_run_Time.setText(NumString.format(myCE.par.run_Time_min, 6, 1));
        jTB_polarity_run.setSelected(!myCE.par.run_positive);
        jTB_polarity_run.doClick();

        jTF_post_Pressure.setText(NumString.format(myCE.par.post_Pressure, 6, 1));
        jTF_post_Voltage.setText(NumString.format(myCE.par.post_Voltage, 6, 2));
        jTF_post_Current.setText(NumString.format(myCE.par.post_Current, 6, 1));
        jTF_post_Time.setText(NumString.format(myCE.par.post_Time_min, 6, 1));
        jTB_polarity_post.setSelected(!myCE.par.post_positive);
        jTB_polarity_post.doClick();

        jTextField_CapillaryLength.setText(NumString.format(myCE.par.capillaryLength, 6, 1));
        jTextField_InnerDiam.setText(NumString.format(myCE.par.capillaryID, 6, 1));
        jTextField_Voltage.setText(NumString.format(myCE.par.voltage, 6, 2));
        jTextField_BGEconductivity.setText(String.valueOf(myCE.par.bgeConductivity));
        recalcCurrent();
        jTextField_tMfirst.setText(NumString.format(myCE.par.tMfirst, 8, 3));
        jTextField_tMsecond.setText(NumString.format(myCE.par.tMsecond, 8, 3));
        jTextField_tMref.setText(NumString.format(myCE.par.tMref, 8, 3));
        fAdjT_1 = myCE.par.tMref / myCE.par.tMfirst;
        fAdjT_2 = myCE.par.tMref / myCE.par.tMsecond;

    }

    public FrontEnd() {
        initComponents();
        CleanUpAdapter cleaner = new CleanUpAdapter();
        this.addWindowListener(cleaner);

        jPanel_MonitorC4D.add(graph1st, BorderLayout.CENTER);
        graph1st.setBounds(jPanel_mold1st.getBounds());
        graph1st.setVisible(true);
        graph1st.setFactorXAxis(1.1);

        jPanel_MonitorC4D.add(graph2nd, BorderLayout.CENTER);
        graph2nd.setBounds(jPanel_mold2nd.getBounds());
        graph2nd.setVisible(true);
        graph2nd.setFactorXAxis(1.1);

        jPanel_MonitorC4Dplus.add(graphAdj, BorderLayout.CENTER);
        graphAdj.setBounds(jPanel_moldAdj.getBounds());
        graphAdj.setVisible(true);
        graphAdj.setFactorXAxis(1.1);

        jPanel_MonitorMonitor.add(graphMonitor, BorderLayout.CENTER);
        graphMonitor.setBounds(jPanel_moldMonitor.getBounds());
        graphMonitor.setVisible(true);
        graphMonitor.setFactorXAxis(1.1);
        graphMonitor.renderer.setSeriesPaint(0, Color.BLUE);
        graphMonitor.renderer.setSeriesPaint(1, Color.RED);

        jTabbedPane_Input.setEnabled(false); //Disable everything, but the connection tab
        jTabbedPane_monitor.setEnabled(false);

        initMyDevice();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jFileChooser_directory = new javax.swing.JFileChooser();
        PanelStatus = new javax.swing.JPanel();
        jLabel_Raw_Status = new javax.swing.JLabel();
        jLabelHV_voltage = new javax.swing.JLabel();
        jLabelHV_current = new javax.swing.JLabel();
        jLabel_Pressure = new javax.swing.JLabel();
        jTabbedPane_Input = new javax.swing.JTabbedPane();
        jPanel_Connection = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jComboBox_port = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        jComboBox_baudRate = new javax.swing.JComboBox();
        jButton_ConectSerial = new javax.swing.JButton();
        jPanel_This_Port = new javax.swing.JPanel();
        jTextField_command_line = new javax.swing.JTextField();
        jButton_send_a_message = new javax.swing.JButton();
        jLabel_title_from_the_serial = new javax.swing.JLabel();
        jLabel_from_the_port = new javax.swing.JLabel();
        jButton_Accept = new javax.swing.JButton();
        jLabel_EngagedWith = new javax.swing.JLabel();
        jPanel_just_Test = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jButton_once = new javax.swing.JButton();
        jButton_halt = new javax.swing.JButton();
        jButton_halt1 = new javax.swing.JButton();
        jButton_rescan = new javax.swing.JButton();
        jPanel_Conditions = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jRadioButton_MultipleRuns = new javax.swing.JRadioButton();
        jLabel10 = new javax.swing.JLabel();
        jLabel31 = new javax.swing.JLabel();
        jLabel33 = new javax.swing.JLabel();
        jTF_inj_Pressure = new javax.swing.JTextField();
        jTB_polarity_inj = new javax.swing.JToggleButton();
        jLabel3 = new javax.swing.JLabel();
        jTF_run_Pressure = new javax.swing.JTextField();
        jTF_post_Pressure = new javax.swing.JTextField();
        jTF_inj_Voltage = new javax.swing.JTextField();
        jTF_run_Voltage = new javax.swing.JTextField();
        jTF_post_Voltage = new javax.swing.JTextField();
        jTF_inj_Current = new javax.swing.JTextField();
        jTF_run_Current = new javax.swing.JTextField();
        jTF_post_Current = new javax.swing.JTextField();
        jTF_inj_Time = new javax.swing.JTextField();
        jTF_post_Time = new javax.swing.JTextField();
        jTF_run_Time = new javax.swing.JTextField();
        jTB_polarity_run = new javax.swing.JToggleButton();
        jTB_polarity_post = new javax.swing.JToggleButton();
        jLabel29 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel27 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jTextField_Prefix = new javax.swing.JTextField();
        jButton_chooseDirectory = new javax.swing.JButton();
        jLabel_directory = new javax.swing.JLabel();
        jButton_chooseDirectoryBackup = new javax.swing.JButton();
        jLabel_directoryBackup = new javax.swing.JLabel();
        jLabel34 = new javax.swing.JLabel();
        jComboBox_complement = new javax.swing.JComboBox();
        jLabel32 = new javax.swing.JLabel();
        jComboBox_separator = new javax.swing.JComboBox();
        jCheckBox_MustBackup = new javax.swing.JCheckBox();
        jButton_StartExperiment = new javax.swing.JButton();
        jPanel_Experiment = new javax.swing.JPanel();
        jLabel_directory1 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jTextField_fileName = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel_areFilesClosed = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jLabel12 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jTextField_CapillaryLength = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        jTextField_Voltage = new javax.swing.JTextField();
        jLabel19 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jTextField_BGEconductivity = new javax.swing.JTextField();
        jLabel20 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        jTextField_InnerDiam = new javax.swing.JTextField();
        jLabel22 = new javax.swing.JLabel();
        jLabel_Expected_Current = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextPane_Comments = new javax.swing.JTextPane();
        jButton_save_Report = new javax.swing.JButton();
        jCheckBox_saveElectrophoreticConditions = new javax.swing.JCheckBox();
        jLabel17 = new javax.swing.JLabel();
        jButton_StopExperiment = new javax.swing.JButton();
        jButton_StopRun = new javax.swing.JButton();
        jButton_AskIoP = new javax.swing.JButton();
        jPanel_Pro = new javax.swing.JPanel();
        jButton_PumpOn = new javax.swing.JButton();
        jButton_PumpOff = new javax.swing.JButton();
        jButton_ValveOn = new javax.swing.JButton();
        jButton_ValveOff = new javax.swing.JButton();
        jButton_SetPressure = new javax.swing.JButton();
        jButton_PumpHalt = new javax.swing.JButton();
        jTextField_Pressure = new javax.swing.JTextField();
        jLabel28 = new javax.swing.JLabel();
        jTabbedPane_monitor = new javax.swing.JTabbedPane();
        jPanel_MonitorC4D = new javax.swing.JPanel();
        jPanel_mold1st = new javax.swing.JPanel();
        jPanel_mold2nd = new javax.swing.JPanel();
        jRadioButton_autoScale_1 = new javax.swing.JRadioButton();
        jRadioButton_autoScale_2 = new javax.swing.JRadioButton();
        jPanel_MonitorC4Dplus = new javax.swing.JPanel();
        jPanel_SetMarkers = new javax.swing.JPanel();
        jLabel13 = new javax.swing.JLabel();
        jTextField_tMfirst = new javax.swing.JTextField();
        jLabel23 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jTextField_tMsecond = new javax.swing.JTextField();
        jLabel24 = new javax.swing.JLabel();
        jLabel25 = new javax.swing.JLabel();
        jTextField_tMref = new javax.swing.JTextField();
        jLabel26 = new javax.swing.JLabel();
        jButton_UpdateBaseline = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JSeparator();
        jSeparator3 = new javax.swing.JSeparator();
        jPanel_moldAdj = new javax.swing.JPanel();
        jRadioButton_autoScale_Adj = new javax.swing.JRadioButton();
        jPanel_MonitorMonitor = new javax.swing.JPanel();
        jPanel_moldMonitor = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel30 = new javax.swing.JLabel();
        jLabel_prog_pressure = new javax.swing.JLabel();
        jLabel_prog_voltage = new javax.swing.JLabel();
        jLabel_prog_current = new javax.swing.JLabel();
        jLabel_prog_time = new javax.swing.JLabel();
        jPanel_MonitorPro = new javax.swing.JPanel();
        jPanel_HVmonitor = new javax.swing.JPanel();
        jLabelHV_GFI = new javax.swing.JLabel();
        jLabelHV_running = new javax.swing.JLabel();
        jLabelHV_Enabled = new javax.swing.JLabel();
        jLabelHV_positive = new javax.swing.JLabel();
        jLabelHV_Vcontrolled = new javax.swing.JLabel();
        jLabelHV_Icontrolled = new javax.swing.JLabel();
        jLabelHV_time = new javax.swing.JLabel();
        jLabelHV_totalCurrent = new javax.swing.JLabel();
        jLabelHV_returnCurrent = new javax.swing.JLabel();
        jLabelHV_Step = new javax.swing.JLabel();
        jPanel_C4D_monitor = new javax.swing.JPanel();
        jLabelC4D_running = new javax.swing.JLabel();
        jLabel_NumberOfClicks = new javax.swing.JLabel();
        jPanel_PumpMonitor = new javax.swing.JPanel();
        jLabelPump_pumping = new javax.swing.JLabel();
        jLabelPump_pressure = new javax.swing.JLabel();
        jLabelPump_time = new javax.swing.JLabel();
        jLabel_IoP = new javax.swing.JLabel();

        jFileChooser_directory.setCurrentDirectory(new java.io.File("C:\\lixo"));
        jFileChooser_directory.setDialogTitle("Place to store the experiments");
        jFileChooser_directory.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("minimalistiCE");
        setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
        setLocation(new java.awt.Point(10, 10));
        setPreferredSize(new java.awt.Dimension(1000, 700));

        PanelStatus.setBorder(javax.swing.BorderFactory.createTitledBorder("Status"));

        jLabel_Raw_Status.setText("status");

        jLabelHV_voltage.setText("0 kV");

        jLabelHV_current.setText("0 uA");

        jLabel_Pressure.setText("0 kPa");

        javax.swing.GroupLayout PanelStatusLayout = new javax.swing.GroupLayout(PanelStatus);
        PanelStatus.setLayout(PanelStatusLayout);
        PanelStatusLayout.setHorizontalGroup(
            PanelStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PanelStatusLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel_Raw_Status, javax.swing.GroupLayout.PREFERRED_SIZE, 178, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabelHV_voltage, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabelHV_current, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(41, 41, 41)
                .addComponent(jLabel_Pressure, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        PanelStatusLayout.setVerticalGroup(
            PanelStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PanelStatusLayout.createSequentialGroup()
                .addGroup(PanelStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel_Raw_Status)
                    .addComponent(jLabelHV_voltage)
                    .addComponent(jLabelHV_current)
                    .addComponent(jLabel_Pressure))
                .addGap(0, 8, Short.MAX_VALUE))
        );

        jPanel_Connection.setToolTipText("");

        jLabel1.setText("select com port");

        jComboBox_port.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "com1", "com2" }));

        jLabel2.setText("baud rate");

        jComboBox_baudRate.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "9600", "115200" }));

        jButton_ConectSerial.setText("Connect");
        jButton_ConectSerial.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_ConectSerialActionPerformed(evt);
            }
        });

        jPanel_This_Port.setBorder(javax.swing.BorderFactory.createTitledBorder("This port"));
        jPanel_This_Port.setToolTipText("This port");
        jPanel_This_Port.setPreferredSize(new java.awt.Dimension(600, 160));

        jButton_send_a_message.setText("send the message:");
        jButton_send_a_message.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_send_a_messageActionPerformed(evt);
            }
        });

        jLabel_title_from_the_serial.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel_title_from_the_serial.setText("message from the port:");

        jLabel_from_the_port.setBackground(new java.awt.Color(204, 204, 204));
        jLabel_from_the_port.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel_from_the_port.setForeground(new java.awt.Color(0, 153, 153));
        jLabel_from_the_port.setText("<none>");

        jButton_Accept.setText("Engage with this port");
        jButton_Accept.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_AcceptActionPerformed(evt);
            }
        });

        jLabel_EngagedWith.setText("id");

        javax.swing.GroupLayout jPanel_This_PortLayout = new javax.swing.GroupLayout(jPanel_This_Port);
        jPanel_This_Port.setLayout(jPanel_This_PortLayout);
        jPanel_This_PortLayout.setHorizontalGroup(
            jPanel_This_PortLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_This_PortLayout.createSequentialGroup()
                .addGroup(jPanel_This_PortLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel_This_PortLayout.createSequentialGroup()
                        .addGroup(jPanel_This_PortLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButton_send_a_message)
                            .addComponent(jLabel_from_the_port, javax.swing.GroupLayout.PREFERRED_SIZE, 336, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel_title_from_the_serial))
                        .addGap(0, 65, Short.MAX_VALUE))
                    .addComponent(jTextField_command_line)
                    .addGroup(jPanel_This_PortLayout.createSequentialGroup()
                        .addComponent(jButton_Accept)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel_EngagedWith, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel_This_PortLayout.setVerticalGroup(
            jPanel_This_PortLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_This_PortLayout.createSequentialGroup()
                .addGroup(jPanel_This_PortLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton_Accept)
                    .addComponent(jLabel_EngagedWith))
                .addGap(18, 18, 18)
                .addComponent(jLabel_title_from_the_serial)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel_from_the_port)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton_send_a_message)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jTextField_command_line, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(319, 319, 319))
        );

        jPanel_just_Test.setBorder(javax.swing.BorderFactory.createTitledBorder("Just for test"));

        jPanel2.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));

        jButton_once.setText("LED 2");
        jButton_once.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_onceActionPerformed(evt);
            }
        });

        jButton_halt.setText("LED 5");
        jButton_halt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_haltActionPerformed(evt);
            }
        });

        jButton_halt1.setText("LED 0");
        jButton_halt1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_halt1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton_once)
                    .addComponent(jButton_halt)
                    .addComponent(jButton_halt1))
                .addContainerGap(144, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButton_once)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton_halt)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton_halt1)
                .addContainerGap(40, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel_just_TestLayout = new javax.swing.GroupLayout(jPanel_just_Test);
        jPanel_just_Test.setLayout(jPanel_just_TestLayout);
        jPanel_just_TestLayout.setHorizontalGroup(
            jPanel_just_TestLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_just_TestLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(92, Short.MAX_VALUE))
        );
        jPanel_just_TestLayout.setVerticalGroup(
            jPanel_just_TestLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_just_TestLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jButton_rescan.setText("rescan");
        jButton_rescan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_rescanActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel_ConnectionLayout = new javax.swing.GroupLayout(jPanel_Connection);
        jPanel_Connection.setLayout(jPanel_ConnectionLayout);
        jPanel_ConnectionLayout.setHorizontalGroup(
            jPanel_ConnectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_ConnectionLayout.createSequentialGroup()
                .addGroup(jPanel_ConnectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel_ConnectionLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jPanel_just_Test, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jPanel_This_Port, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 417, Short.MAX_VALUE))
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel_ConnectionLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel_ConnectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel_ConnectionLayout.createSequentialGroup()
                        .addComponent(jButton_rescan)
                        .addGap(18, 18, 18)
                        .addComponent(jComboBox_port, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel_ConnectionLayout.createSequentialGroup()
                        .addGap(33, 33, 33)
                        .addComponent(jLabel1)))
                .addGap(18, 18, 18)
                .addGroup(jPanel_ConnectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(jComboBox_baudRate, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton_ConectSerial, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(24, 24, 24))
        );
        jPanel_ConnectionLayout.setVerticalGroup(
            jPanel_ConnectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_ConnectionLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel_ConnectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel_ConnectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboBox_port, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jComboBox_baudRate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton_ConectSerial)
                    .addComponent(jButton_rescan))
                .addGap(38, 38, 38)
                .addComponent(jPanel_This_Port, javax.swing.GroupLayout.PREFERRED_SIZE, 197, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jPanel_just_Test, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(96, Short.MAX_VALUE))
        );

        jTabbedPane_Input.addTab("connection", jPanel_Connection);

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("Experimental conditions"));

        jRadioButton_MultipleRuns.setText("multiple runs");
        jRadioButton_MultipleRuns.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButton_MultipleRunsActionPerformed(evt);
            }
        });

        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel10.setText("time");

        jLabel31.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel31.setText("pressure / kPa");

        jLabel33.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel33.setText("voltage / kV");

        jTF_inj_Pressure.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTF_inj_Pressure.setText("0");
        jTF_inj_Pressure.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTF_inj_PressureFocusLost(evt);
            }
        });
        jTF_inj_Pressure.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTF_inj_PressureActionPerformed(evt);
            }
        });

        jTB_polarity_inj.setText("+");
        jTB_polarity_inj.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTB_polarity_injActionPerformed(evt);
            }
        });

        jLabel3.setText("injection                       run                    post run ");

        jTF_run_Pressure.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTF_run_Pressure.setText("0");
        jTF_run_Pressure.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTF_run_PressureFocusLost(evt);
            }
        });
        jTF_run_Pressure.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTF_run_PressureActionPerformed(evt);
            }
        });

        jTF_post_Pressure.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTF_post_Pressure.setText("0");
        jTF_post_Pressure.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTF_post_PressureFocusLost(evt);
            }
        });
        jTF_post_Pressure.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTF_post_PressureActionPerformed(evt);
            }
        });

        jTF_inj_Voltage.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTF_inj_Voltage.setText("0");
        jTF_inj_Voltage.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTF_inj_VoltageFocusLost(evt);
            }
        });
        jTF_inj_Voltage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTF_inj_VoltageActionPerformed(evt);
            }
        });

        jTF_run_Voltage.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTF_run_Voltage.setText("0");
        jTF_run_Voltage.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTF_run_VoltageFocusLost(evt);
            }
        });
        jTF_run_Voltage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTF_run_VoltageActionPerformed(evt);
            }
        });

        jTF_post_Voltage.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTF_post_Voltage.setText("0");
        jTF_post_Voltage.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTF_post_VoltageFocusLost(evt);
            }
        });
        jTF_post_Voltage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTF_post_VoltageActionPerformed(evt);
            }
        });

        jTF_inj_Current.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTF_inj_Current.setText("0");
        jTF_inj_Current.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTF_inj_CurrentFocusLost(evt);
            }
        });
        jTF_inj_Current.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTF_inj_CurrentActionPerformed(evt);
            }
        });

        jTF_run_Current.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTF_run_Current.setText("0");
        jTF_run_Current.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTF_run_CurrentFocusLost(evt);
            }
        });
        jTF_run_Current.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTF_run_CurrentActionPerformed(evt);
            }
        });

        jTF_post_Current.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTF_post_Current.setText("0");
        jTF_post_Current.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTF_post_CurrentFocusLost(evt);
            }
        });
        jTF_post_Current.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTF_post_CurrentActionPerformed(evt);
            }
        });

        jTF_inj_Time.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTF_inj_Time.setText("20");
        jTF_inj_Time.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTF_inj_TimeFocusLost(evt);
            }
        });
        jTF_inj_Time.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTF_inj_TimeActionPerformed(evt);
            }
        });

        jTF_post_Time.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTF_post_Time.setText("20");
        jTF_post_Time.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTF_post_TimeFocusLost(evt);
            }
        });
        jTF_post_Time.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTF_post_TimeActionPerformed(evt);
            }
        });

        jTF_run_Time.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTF_run_Time.setText("20");
        jTF_run_Time.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTF_run_TimeFocusLost(evt);
            }
        });
        jTF_run_Time.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTF_run_TimeActionPerformed(evt);
            }
        });

        jTB_polarity_run.setText("+");
        jTB_polarity_run.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTB_polarity_runActionPerformed(evt);
            }
        });

        jTB_polarity_post.setText("+");
        jTB_polarity_post.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTB_polarity_postActionPerformed(evt);
            }
        });

        jLabel29.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel29.setText("current / uA");

        jLabel4.setText("s");

        jLabel11.setText("min");

        jLabel27.setText("min");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGap(135, 135, 135)
                        .addComponent(jLabel3))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jLabel31, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(47, 47, 47)
                        .addComponent(jTF_inj_Pressure, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(45, 45, 45)
                        .addComponent(jTF_run_Pressure, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(45, 45, 45)
                        .addComponent(jTF_post_Pressure, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jLabel33, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(20, 20, 20)
                        .addComponent(jTB_polarity_inj, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(jTF_inj_Voltage, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jTB_polarity_run, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(jTF_run_Voltage, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jTB_polarity_post, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(jTF_post_Voltage, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jRadioButton_MultipleRuns, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addComponent(jLabel29, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(47, 47, 47)
                                .addComponent(jTF_inj_Current, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(49, 49, 49))
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(47, 47, 47)
                                .addComponent(jTF_inj_Time, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(20, 20, 20)))
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addComponent(jTF_run_Time, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(16, 16, 16)
                                .addComponent(jTF_post_Time, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel27, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addComponent(jTF_run_Current, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(45, 45, 45)
                                .addComponent(jTF_post_Current, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addGap(1, 1, 1))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addComponent(jLabel3)
                .addGap(1, 1, 1)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGap(3, 3, 3)
                        .addComponent(jLabel31))
                    .addComponent(jTF_inj_Pressure, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTF_run_Pressure, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTF_post_Pressure, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGap(3, 3, 3)
                        .addComponent(jLabel33))
                    .addComponent(jTB_polarity_inj)
                    .addComponent(jTF_inj_Voltage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTB_polarity_run)
                    .addComponent(jTF_run_Voltage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTB_polarity_post)
                    .addComponent(jTF_post_Voltage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGap(3, 3, 3)
                        .addComponent(jLabel29))
                    .addComponent(jTF_inj_Current, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTF_run_Current, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTF_post_Current, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel10)
                    .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jTF_inj_Time, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel4))
                    .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jTF_post_Time, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel27))
                    .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jTF_run_Time, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel11)))
                .addGap(27, 27, 27)
                .addComponent(jRadioButton_MultipleRuns)
                .addGap(21, 21, 21))
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Files"));

        jLabel5.setText("prefix:");

        jTextField_Prefix.setText("Electropherogram_");
        jTextField_Prefix.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField_PrefixFocusLost(evt);
            }
        });
        jTextField_Prefix.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField_PrefixActionPerformed(evt);
            }
        });

        jButton_chooseDirectory.setText("directory");
        jButton_chooseDirectory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_chooseDirectoryActionPerformed(evt);
            }
        });

        jLabel_directory.setText("directory");

        jButton_chooseDirectoryBackup.setText("backup directory");
        jButton_chooseDirectoryBackup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_chooseDirectoryBackupActionPerformed(evt);
            }
        });

        jLabel_directoryBackup.setText("directory");

        jLabel34.setText("file complement:");

        jComboBox_complement.setModel(new javax.swing.DefaultComboBoxModel(new String[] { ".dat", ".txt", ".ele", "no complement" }));
        jComboBox_complement.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox_complementActionPerformed(evt);
            }
        });

        jLabel32.setText("column separator:");

        jComboBox_separator.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "space", "tab", "comma" }));
        jComboBox_separator.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox_separatorActionPerformed(evt);
            }
        });

        jCheckBox_MustBackup.setSelected(true);
        jCheckBox_MustBackup.setText("generate backup");
        jCheckBox_MustBackup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox_MustBackupActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel_directoryBackup, javax.swing.GroupLayout.PREFERRED_SIZE, 433, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel34)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBox_complement, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel32)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBox_separator, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jButton_chooseDirectoryBackup)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField_Prefix, javax.swing.GroupLayout.PREFERRED_SIZE, 274, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jButton_chooseDirectory)
                    .addComponent(jLabel_directory, javax.swing.GroupLayout.PREFERRED_SIZE, 386, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jCheckBox_MustBackup)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField_Prefix, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton_chooseDirectory)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel_directory)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBox_MustBackup)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 15, Short.MAX_VALUE)
                .addComponent(jButton_chooseDirectoryBackup)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel_directoryBackup)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboBox_complement, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel34)
                    .addComponent(jLabel32)
                    .addComponent(jComboBox_separator, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(28, 28, 28))
        );

        jButton_StartExperiment.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jButton_StartExperiment.setText("start the experiment");
        jButton_StartExperiment.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_StartExperimentActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel_ConditionsLayout = new javax.swing.GroupLayout(jPanel_Conditions);
        jPanel_Conditions.setLayout(jPanel_ConditionsLayout);
        jPanel_ConditionsLayout.setHorizontalGroup(
            jPanel_ConditionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_ConditionsLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(jPanel_ConditionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 419, javax.swing.GroupLayout.PREFERRED_SIZE)))
            .addGroup(jPanel_ConditionsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButton_StartExperiment, javax.swing.GroupLayout.PREFERRED_SIZE, 229, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanel_ConditionsLayout.setVerticalGroup(
            jPanel_ConditionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_ConditionsLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButton_StartExperiment, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jTabbedPane_Input.addTab("before the experiment", jPanel_Conditions);

        jLabel6.setText("Directory:");

        jLabel7.setText("Filename:");

        jLabel8.setText("prefix:");

        jLabel_areFilesClosed.setText("closed");

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Electrophoretic Calculator"));

        jLabel12.setText("capillary length:");

        jLabel18.setText("cm");

        jTextField_CapillaryLength.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextField_CapillaryLength.setText("10.0");
        jTextField_CapillaryLength.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField_CapillaryLengthFocusLost(evt);
            }
        });
        jTextField_CapillaryLength.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField_CapillaryLengthActionPerformed(evt);
            }
        });

        jLabel15.setText("voltage:");

        jTextField_Voltage.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextField_Voltage.setText("30.00");
        jTextField_Voltage.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField_VoltageFocusLost(evt);
            }
        });
        jTextField_Voltage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField_VoltageActionPerformed(evt);
            }
        });

        jLabel19.setText("kV");

        jLabel16.setText("BGE conductivity:");

        jTextField_BGEconductivity.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextField_BGEconductivity.setText("4.74e-02");
        jTextField_BGEconductivity.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField_BGEconductivityFocusLost(evt);
            }
        });
        jTextField_BGEconductivity.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField_BGEconductivityActionPerformed(evt);
            }
        });

        jLabel20.setText("S/m");

        jLabel21.setText("capillary i.d.:");

        jTextField_InnerDiam.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextField_InnerDiam.setText("50");
        jTextField_InnerDiam.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField_InnerDiamFocusLost(evt);
            }
        });
        jTextField_InnerDiam.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField_InnerDiamActionPerformed(evt);
            }
        });

        jLabel22.setText("µm");

        jLabel_Expected_Current.setText("Expected current:");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGap(0, 6, Short.MAX_VALUE)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel12, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel21, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel15, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel16, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                            .addComponent(jTextField_BGEconductivity, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextField_Voltage, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextField_InnerDiam, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextField_CapillaryLength, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel19)
                            .addComponent(jLabel20, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel22, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel18, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel_Expected_Current)
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel12)
                    .addComponent(jTextField_CapillaryLength, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel18))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel21)
                    .addComponent(jTextField_InnerDiam, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel22))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel15)
                    .addComponent(jTextField_Voltage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel19))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField_BGEconductivity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel16)
                    .addComponent(jLabel20))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel_Expected_Current)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jScrollPane1.setViewportView(jTextPane_Comments);

        jButton_save_Report.setText("Save Report");
        jButton_save_Report.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_save_ReportActionPerformed(evt);
            }
        });

        jCheckBox_saveElectrophoreticConditions.setSelected(true);
        jCheckBox_saveElectrophoreticConditions.setText("Save Electrophoretic conditions");

        jLabel17.setText("Report:");

        jButton_StopExperiment.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jButton_StopExperiment.setText("stop the experiment");
        jButton_StopExperiment.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_StopExperimentActionPerformed(evt);
            }
        });

        jButton_StopRun.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jButton_StopRun.setText("end this run");
        jButton_StopRun.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_StopRunActionPerformed(evt);
            }
        });

        jButton_AskIoP.setText("ask IoP");
        jButton_AskIoP.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_AskIoPActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel_ExperimentLayout = new javax.swing.GroupLayout(jPanel_Experiment);
        jPanel_Experiment.setLayout(jPanel_ExperimentLayout);
        jPanel_ExperimentLayout.setHorizontalGroup(
            jPanel_ExperimentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_ExperimentLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel_ExperimentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(jPanel_ExperimentLayout.createSequentialGroup()
                        .addGroup(jPanel_ExperimentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel17)
                            .addGroup(jPanel_ExperimentLayout.createSequentialGroup()
                                .addComponent(jButton_StopRun, javax.swing.GroupLayout.PREFERRED_SIZE, 184, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jButton_StopExperiment, javax.swing.GroupLayout.PREFERRED_SIZE, 199, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel_ExperimentLayout.createSequentialGroup()
                        .addGroup(jPanel_ExperimentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextField_fileName)
                            .addGroup(jPanel_ExperimentLayout.createSequentialGroup()
                                .addGroup(jPanel_ExperimentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel8)
                                    .addGroup(jPanel_ExperimentLayout.createSequentialGroup()
                                        .addComponent(jLabel7)
                                        .addGap(222, 222, 222)
                                        .addComponent(jLabel_areFilesClosed))
                                    .addComponent(jLabel6))
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel_ExperimentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel_directory1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel_ExperimentLayout.createSequentialGroup()
                        .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(jPanel_ExperimentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel_ExperimentLayout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel_ExperimentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel_ExperimentLayout.createSequentialGroup()
                                        .addGap(21, 21, 21)
                                        .addComponent(jButton_save_Report, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                    .addComponent(jCheckBox_saveElectrophoreticConditions)))
                            .addGroup(jPanel_ExperimentLayout.createSequentialGroup()
                                .addGap(50, 50, 50)
                                .addComponent(jButton_AskIoP)
                                .addGap(0, 0, Short.MAX_VALUE)))))
                .addContainerGap())
        );
        jPanel_ExperimentLayout.setVerticalGroup(
            jPanel_ExperimentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_ExperimentLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel_ExperimentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jButton_StopExperiment, javax.swing.GroupLayout.DEFAULT_SIZE, 45, Short.MAX_VALUE)
                    .addComponent(jButton_StopRun, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGroup(jPanel_ExperimentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel_ExperimentLayout.createSequentialGroup()
                        .addGap(76, 76, 76)
                        .addComponent(jLabel_directory1)
                        .addGap(34, 34, 34)
                        .addComponent(jLabel9))
                    .addGroup(jPanel_ExperimentLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel6)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel8)
                        .addGap(24, 24, 24)
                        .addGroup(jPanel_ExperimentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel7)
                            .addComponent(jLabel_areFilesClosed))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField_fileName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel17)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 116, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(jPanel_ExperimentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel_ExperimentLayout.createSequentialGroup()
                        .addGap(43, 43, 43)
                        .addComponent(jButton_AskIoP)
                        .addGap(32, 32, 32)
                        .addComponent(jCheckBox_saveElectrophoreticConditions)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton_save_Report))
                    .addGroup(jPanel_ExperimentLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(44, Short.MAX_VALUE))
        );

        jTabbedPane_Input.addTab("experiment", jPanel_Experiment);

        jButton_PumpOn.setText("pump On");
        jButton_PumpOn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_PumpOnActionPerformed(evt);
            }
        });

        jButton_PumpOff.setText("pump Off");
        jButton_PumpOff.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_PumpOffActionPerformed(evt);
            }
        });

        jButton_ValveOn.setText("valve On");
        jButton_ValveOn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_ValveOnActionPerformed(evt);
            }
        });

        jButton_ValveOff.setText("valve Off");
        jButton_ValveOff.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_ValveOffActionPerformed(evt);
            }
        });

        jButton_SetPressure.setText("set Pressure");
        jButton_SetPressure.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_SetPressureActionPerformed(evt);
            }
        });

        jButton_PumpHalt.setText("pump Halt");
        jButton_PumpHalt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_PumpHaltActionPerformed(evt);
            }
        });

        jTextField_Pressure.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextField_Pressure.setText("10");

        jLabel28.setText("kPa");

        javax.swing.GroupLayout jPanel_ProLayout = new javax.swing.GroupLayout(jPanel_Pro);
        jPanel_Pro.setLayout(jPanel_ProLayout);
        jPanel_ProLayout.setHorizontalGroup(
            jPanel_ProLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_ProLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel_ProLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel_ProLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jButton_ValveOn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton_PumpOn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel_ProLayout.createSequentialGroup()
                        .addComponent(jTextField_Pressure, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel28, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel_ProLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel_ProLayout.createSequentialGroup()
                        .addComponent(jButton_SetPressure)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton_PumpHalt))
                    .addGroup(jPanel_ProLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(jButton_PumpOff, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton_ValveOff, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap(100, Short.MAX_VALUE))
        );
        jPanel_ProLayout.setVerticalGroup(
            jPanel_ProLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_ProLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel_ProLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton_PumpOn)
                    .addComponent(jButton_PumpOff))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel_ProLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton_ValveOn)
                    .addComponent(jButton_ValveOff))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel_ProLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton_SetPressure)
                    .addComponent(jButton_PumpHalt)
                    .addComponent(jTextField_Pressure, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel28))
                .addContainerGap(481, Short.MAX_VALUE))
        );

        jTabbedPane_Input.addTab("pro", jPanel_Pro);

        jPanel_mold1st.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jPanel_mold1st.setEnabled(false);
        jPanel_mold1st.setOpaque(false);
        jPanel_mold1st.setPreferredSize(new java.awt.Dimension(400, 300));

        javax.swing.GroupLayout jPanel_mold1stLayout = new javax.swing.GroupLayout(jPanel_mold1st);
        jPanel_mold1st.setLayout(jPanel_mold1stLayout);
        jPanel_mold1stLayout.setHorizontalGroup(
            jPanel_mold1stLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jPanel_mold1stLayout.setVerticalGroup(
            jPanel_mold1stLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 244, Short.MAX_VALUE)
        );

        jPanel_mold2nd.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jPanel_mold2nd.setEnabled(false);
        jPanel_mold2nd.setOpaque(false);
        jPanel_mold2nd.setPreferredSize(new java.awt.Dimension(400, 300));

        javax.swing.GroupLayout jPanel_mold2ndLayout = new javax.swing.GroupLayout(jPanel_mold2nd);
        jPanel_mold2nd.setLayout(jPanel_mold2ndLayout);
        jPanel_mold2ndLayout.setHorizontalGroup(
            jPanel_mold2ndLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jPanel_mold2ndLayout.setVerticalGroup(
            jPanel_mold2ndLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 244, Short.MAX_VALUE)
        );

        jRadioButton_autoScale_1.setSelected(true);
        jRadioButton_autoScale_1.setText("automatic time scale 1st C4D");

        jRadioButton_autoScale_2.setSelected(true);
        jRadioButton_autoScale_2.setText("automatic time scale 2nd C4D");

        javax.swing.GroupLayout jPanel_MonitorC4DLayout = new javax.swing.GroupLayout(jPanel_MonitorC4D);
        jPanel_MonitorC4D.setLayout(jPanel_MonitorC4DLayout);
        jPanel_MonitorC4DLayout.setHorizontalGroup(
            jPanel_MonitorC4DLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_MonitorC4DLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel_MonitorC4DLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel_mold1st, javax.swing.GroupLayout.DEFAULT_SIZE, 514, Short.MAX_VALUE)
                    .addGroup(jPanel_MonitorC4DLayout.createSequentialGroup()
                        .addComponent(jRadioButton_autoScale_1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 157, Short.MAX_VALUE)
                        .addComponent(jRadioButton_autoScale_2))
                    .addComponent(jPanel_mold2nd, javax.swing.GroupLayout.DEFAULT_SIZE, 514, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel_MonitorC4DLayout.setVerticalGroup(
            jPanel_MonitorC4DLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_MonitorC4DLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel_mold1st, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel_MonitorC4DLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jRadioButton_autoScale_1)
                    .addComponent(jRadioButton_autoScale_2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel_mold2nd, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        jTabbedPane_monitor.addTab("C4D", jPanel_MonitorC4D);

        jPanel_SetMarkers.setBorder(javax.swing.BorderFactory.createTitledBorder("marker"));

        jLabel13.setText("migration time at 1st C4D:");

        jTextField_tMfirst.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextField_tMfirst.setText("10.0");
        jTextField_tMfirst.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField_tMfirstFocusLost(evt);
            }
        });

        jLabel23.setText("min");

        jLabel14.setText("migration time at 2nd C4D:");

        jTextField_tMsecond.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextField_tMsecond.setText("10.0");
        jTextField_tMsecond.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField_tMsecondFocusLost(evt);
            }
        });

        jLabel24.setText("min");

        jLabel25.setText("reference migration time:");

        jTextField_tMref.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextField_tMref.setText("10.0");
        jTextField_tMref.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField_tMrefFocusLost(evt);
            }
        });

        jLabel26.setText("min");

        jButton_UpdateBaseline.setText("update baseline");
        jButton_UpdateBaseline.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_UpdateBaselineActionPerformed(evt);
            }
        });

        jSeparator2.setBackground(new java.awt.Color(0, 0, 255));
        jSeparator2.setForeground(new java.awt.Color(0, 0, 255));

        jSeparator3.setBackground(new java.awt.Color(255, 0, 0));
        jSeparator3.setForeground(new java.awt.Color(255, 0, 0));

        javax.swing.GroupLayout jPanel_SetMarkersLayout = new javax.swing.GroupLayout(jPanel_SetMarkers);
        jPanel_SetMarkers.setLayout(jPanel_SetMarkersLayout);
        jPanel_SetMarkersLayout.setHorizontalGroup(
            jPanel_SetMarkersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_SetMarkersLayout.createSequentialGroup()
                .addGroup(jPanel_SetMarkersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel25, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel14, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel13, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel_SetMarkersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel_SetMarkersLayout.createSequentialGroup()
                        .addGroup(jPanel_SetMarkersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextField_tMref, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextField_tMsecond, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextField_tMfirst, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel_SetMarkersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel_SetMarkersLayout.createSequentialGroup()
                                .addGroup(jPanel_SetMarkersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel23)
                                    .addComponent(jLabel24))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel_SetMarkersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jSeparator2, javax.swing.GroupLayout.DEFAULT_SIZE, 50, Short.MAX_VALUE)
                                    .addComponent(jSeparator3))
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(jPanel_SetMarkersLayout.createSequentialGroup()
                                .addComponent(jLabel26)
                                .addContainerGap(67, Short.MAX_VALUE))))
                    .addGroup(jPanel_SetMarkersLayout.createSequentialGroup()
                        .addComponent(jButton_UpdateBaseline, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())))
        );
        jPanel_SetMarkersLayout.setVerticalGroup(
            jPanel_SetMarkersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_SetMarkersLayout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addGroup(jPanel_SetMarkersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel13)
                    .addComponent(jTextField_tMfirst, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel23)
                    .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel_SetMarkersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel14)
                    .addComponent(jTextField_tMsecond, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel24)
                    .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel_SetMarkersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel25)
                    .addComponent(jTextField_tMref, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel26))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton_UpdateBaseline)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel_moldAdj.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jPanel_moldAdj.setEnabled(false);
        jPanel_moldAdj.setOpaque(false);
        jPanel_moldAdj.setPreferredSize(new java.awt.Dimension(400, 300));

        javax.swing.GroupLayout jPanel_moldAdjLayout = new javax.swing.GroupLayout(jPanel_moldAdj);
        jPanel_moldAdj.setLayout(jPanel_moldAdjLayout);
        jPanel_moldAdjLayout.setHorizontalGroup(
            jPanel_moldAdjLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jPanel_moldAdjLayout.setVerticalGroup(
            jPanel_moldAdjLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 278, Short.MAX_VALUE)
        );

        jRadioButton_autoScale_Adj.setSelected(true);
        jRadioButton_autoScale_Adj.setText("automatic time scale");

        javax.swing.GroupLayout jPanel_MonitorC4DplusLayout = new javax.swing.GroupLayout(jPanel_MonitorC4Dplus);
        jPanel_MonitorC4Dplus.setLayout(jPanel_MonitorC4DplusLayout);
        jPanel_MonitorC4DplusLayout.setHorizontalGroup(
            jPanel_MonitorC4DplusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_MonitorC4DplusLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel_MonitorC4DplusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel_moldAdj, javax.swing.GroupLayout.DEFAULT_SIZE, 514, Short.MAX_VALUE)
                    .addGroup(jPanel_MonitorC4DplusLayout.createSequentialGroup()
                        .addGroup(jPanel_MonitorC4DplusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jRadioButton_autoScale_Adj)
                            .addComponent(jPanel_SetMarkers, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 200, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel_MonitorC4DplusLayout.setVerticalGroup(
            jPanel_MonitorC4DplusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel_MonitorC4DplusLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel_moldAdj, javax.swing.GroupLayout.PREFERRED_SIZE, 284, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jRadioButton_autoScale_Adj)
                .addGap(18, 18, 18)
                .addComponent(jPanel_SetMarkers, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(80, Short.MAX_VALUE))
        );

        jTabbedPane_monitor.addTab("C4D plus", jPanel_MonitorC4Dplus);

        jPanel_moldMonitor.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));

        javax.swing.GroupLayout jPanel_moldMonitorLayout = new javax.swing.GroupLayout(jPanel_moldMonitor);
        jPanel_moldMonitor.setLayout(jPanel_moldMonitorLayout);
        jPanel_moldMonitorLayout.setHorizontalGroup(
            jPanel_moldMonitorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 495, Short.MAX_VALUE)
        );
        jPanel_moldMonitorLayout.setVerticalGroup(
            jPanel_moldMonitorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 201, Short.MAX_VALUE)
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Program"));

        jLabel30.setFont(new java.awt.Font("Courier New", 0, 12)); // NOI18N
        jLabel30.setText("parameter  |   injection  |     run      |   post run");

        jLabel_prog_pressure.setFont(new java.awt.Font("Courier New", 0, 12)); // NOI18N
        jLabel_prog_pressure.setText("pressure / kPa");

        jLabel_prog_voltage.setFont(new java.awt.Font("Courier New", 0, 12)); // NOI18N
        jLabel_prog_voltage.setText("voltage / kV");

        jLabel_prog_current.setFont(new java.awt.Font("Courier New", 0, 12)); // NOI18N
        jLabel_prog_current.setText("current / uA");

        jLabel_prog_time.setFont(new java.awt.Font("Courier New", 0, 12)); // NOI18N
        jLabel_prog_time.setText("time");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel30, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel_prog_pressure, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel_prog_voltage, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel_prog_current, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel_prog_time, javax.swing.GroupLayout.PREFERRED_SIZE, 436, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel30)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel_prog_pressure)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel_prog_voltage)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel_prog_current)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel_prog_time)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel_MonitorMonitorLayout = new javax.swing.GroupLayout(jPanel_MonitorMonitor);
        jPanel_MonitorMonitor.setLayout(jPanel_MonitorMonitorLayout);
        jPanel_MonitorMonitorLayout.setHorizontalGroup(
            jPanel_MonitorMonitorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_MonitorMonitorLayout.createSequentialGroup()
                .addGroup(jPanel_MonitorMonitorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel_MonitorMonitorLayout.createSequentialGroup()
                        .addGap(19, 19, 19)
                        .addComponent(jPanel_moldMonitor, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel_MonitorMonitorLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel_MonitorMonitorLayout.setVerticalGroup(
            jPanel_MonitorMonitorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_MonitorMonitorLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(208, 208, 208)
                .addComponent(jPanel_moldMonitor, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane_monitor.addTab("monitor   ", jPanel_MonitorMonitor);

        jPanel_HVmonitor.setBorder(javax.swing.BorderFactory.createTitledBorder("High Voltage Power Source"));
        jPanel_HVmonitor.setName("HV"); // NOI18N

        jLabelHV_GFI.setText("GFI");

        jLabelHV_running.setText("running");

        jLabelHV_Enabled.setText("enabled");

        jLabelHV_positive.setText("positive mode");

        jLabelHV_Vcontrolled.setText("free voltage");

        jLabelHV_Icontrolled.setText("free current");

        jLabelHV_time.setText("0 s");

        jLabelHV_totalCurrent.setText("0 uA total");

        jLabelHV_returnCurrent.setText("0 uA electrophoresis");

        jLabelHV_Step.setText("step 0/0");

        javax.swing.GroupLayout jPanel_HVmonitorLayout = new javax.swing.GroupLayout(jPanel_HVmonitor);
        jPanel_HVmonitor.setLayout(jPanel_HVmonitorLayout);
        jPanel_HVmonitorLayout.setHorizontalGroup(
            jPanel_HVmonitorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_HVmonitorLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel_HVmonitorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabelHV_GFI, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabelHV_running, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabelHV_Enabled, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabelHV_positive, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabelHV_Vcontrolled, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabelHV_Icontrolled, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabelHV_returnCurrent, javax.swing.GroupLayout.DEFAULT_SIZE, 185, Short.MAX_VALUE)
                    .addComponent(jLabelHV_Step, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabelHV_time, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabelHV_totalCurrent, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel_HVmonitorLayout.setVerticalGroup(
            jPanel_HVmonitorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_HVmonitorLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabelHV_GFI)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabelHV_running)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabelHV_Enabled)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabelHV_positive)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabelHV_Vcontrolled)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabelHV_Icontrolled)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabelHV_Step)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabelHV_time)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabelHV_totalCurrent)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabelHV_returnCurrent)
                .addContainerGap(14, Short.MAX_VALUE))
        );

        jPanel_C4D_monitor.setBorder(javax.swing.BorderFactory.createTitledBorder("C4D"));

        jLabelC4D_running.setText("running");

        jLabel_NumberOfClicks.setText("0");

        javax.swing.GroupLayout jPanel_C4D_monitorLayout = new javax.swing.GroupLayout(jPanel_C4D_monitor);
        jPanel_C4D_monitor.setLayout(jPanel_C4D_monitorLayout);
        jPanel_C4D_monitorLayout.setHorizontalGroup(
            jPanel_C4D_monitorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_C4D_monitorLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel_C4D_monitorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabelC4D_running, javax.swing.GroupLayout.DEFAULT_SIZE, 225, Short.MAX_VALUE)
                    .addGroup(jPanel_C4D_monitorLayout.createSequentialGroup()
                        .addComponent(jLabel_NumberOfClicks, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel_C4D_monitorLayout.setVerticalGroup(
            jPanel_C4D_monitorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_C4D_monitorLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabelC4D_running)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel_NumberOfClicks)
                .addContainerGap(22, Short.MAX_VALUE))
        );

        jPanel_PumpMonitor.setBorder(javax.swing.BorderFactory.createTitledBorder("Pump"));

        jLabelPump_pumping.setText("---");

        jLabelPump_pressure.setText("- kPa");

        jLabelPump_time.setText("- s");

        jLabel_IoP.setText("- Pa s");

        javax.swing.GroupLayout jPanel_PumpMonitorLayout = new javax.swing.GroupLayout(jPanel_PumpMonitor);
        jPanel_PumpMonitor.setLayout(jPanel_PumpMonitorLayout);
        jPanel_PumpMonitorLayout.setHorizontalGroup(
            jPanel_PumpMonitorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_PumpMonitorLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel_PumpMonitorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabelPump_pumping, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabelPump_pressure, javax.swing.GroupLayout.DEFAULT_SIZE, 225, Short.MAX_VALUE)
                    .addComponent(jLabelPump_time, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel_IoP, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel_PumpMonitorLayout.setVerticalGroup(
            jPanel_PumpMonitorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_PumpMonitorLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(jLabelPump_pumping)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabelPump_pressure)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabelPump_time)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel_IoP)
                .addContainerGap(38, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel_MonitorProLayout = new javax.swing.GroupLayout(jPanel_MonitorPro);
        jPanel_MonitorPro.setLayout(jPanel_MonitorProLayout);
        jPanel_MonitorProLayout.setHorizontalGroup(
            jPanel_MonitorProLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_MonitorProLayout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(jPanel_HVmonitor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel_MonitorProLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel_MonitorProLayout.createSequentialGroup()
                        .addComponent(jPanel_C4D_monitor, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(36, 36, 36))
                    .addGroup(jPanel_MonitorProLayout.createSequentialGroup()
                        .addComponent(jPanel_PumpMonitor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        jPanel_MonitorProLayout.setVerticalGroup(
            jPanel_MonitorProLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_MonitorProLayout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addGroup(jPanel_MonitorProLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel_MonitorProLayout.createSequentialGroup()
                        .addComponent(jPanel_C4D_monitor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jPanel_PumpMonitor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel_HVmonitor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(240, Short.MAX_VALUE))
        );

        jTabbedPane_monitor.addTab("monitor pro", jPanel_MonitorPro);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane_Input, javax.swing.GroupLayout.PREFERRED_SIZE, 423, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTabbedPane_monitor, javax.swing.GroupLayout.PREFERRED_SIZE, 526, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addComponent(PanelStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jTabbedPane_monitor)
                    .addComponent(jTabbedPane_Input, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(PanelStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton_ConectSerialActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_ConectSerialActionPerformed
        if (myCE.isSerialConnected()) {
            myCE.close();
            jButton_ConectSerial.setText("Connect");
            jButton_Accept.setEnabled(true);
            jLabel_from_the_port.setText("");
            jLabel_EngagedWith.setText("");
            jPanel_This_Port.setVisible(false);
            jPanel_just_Test.setVisible(false);
            jTabbedPane_Input.setEnabled(false);
            jTabbedPane_monitor.setEnabled(false);
        } else {
            myCE.par.serialPort = jComboBox_port.getSelectedItem().toString();
            myCE.par.baudRate = Integer.parseInt(jComboBox_baudRate.getSelectedItem().toString());
            jButton_ConectSerial.setText("Disconnect");
            jPanel_This_Port.setVisible(myCE.openSerialPort());
        }
    }//GEN-LAST:event_jButton_ConectSerialActionPerformed

    private void jButton_onceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_onceActionPerformed
        myCE.setLED(2);
    }//GEN-LAST:event_jButton_onceActionPerformed

    private void jButton_haltActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_haltActionPerformed
        myCE.setLED(5);
    }//GEN-LAST:event_jButton_haltActionPerformed

    private void jButton_chooseDirectoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_chooseDirectoryActionPerformed
        if (!myCE.par.directory.equals("")) {
            jFileChooser_directory.setCurrentDirectory(new File(myCE.par.directory));
        }
        int retorno = jFileChooser_directory.showOpenDialog(this);
        if (retorno == JFileChooser.APPROVE_OPTION) {
            myCE.par.directory = jFileChooser_directory.getSelectedFile().toString() + "\\";
            jLabel_directory.setText(myCE.par.directory);
            if (myCE.par.directoryBackup.equals("")) {
                myCE.par.directoryBackup = myCE.par.directory;
                jLabel_directoryBackup.setText(myCE.par.directoryBackup);
            }
        }
    }//GEN-LAST:event_jButton_chooseDirectoryActionPerformed

    private void jButton_chooseDirectoryBackupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_chooseDirectoryBackupActionPerformed
        if (!myCE.par.directoryBackup.equals("")) {
            jFileChooser_directory.setCurrentDirectory(new File(myCE.par.directoryBackup));
        }
        int retorno = jFileChooser_directory.showOpenDialog(this);
        if (retorno == JFileChooser.APPROVE_OPTION) {
            myCE.par.directoryBackup = jFileChooser_directory.getSelectedFile().toString() + "\\";
            jLabel_directoryBackup.setText(myCE.par.directoryBackup);
        }
    }//GEN-LAST:event_jButton_chooseDirectoryBackupActionPerformed

    private void jComboBox_complementActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox_complementActionPerformed
        myCE.par.suffix = jComboBox_complement.getSelectedItem().toString();
        if (myCE.par.suffix.equals("no complement")) {
            myCE.par.suffix = "";
        }
    }//GEN-LAST:event_jComboBox_complementActionPerformed

    private void jTextField_PrefixFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField_PrefixFocusLost
        myCE.par.prefix = jTextField_Prefix.getText();
    }//GEN-LAST:event_jTextField_PrefixFocusLost

    private void jComboBox_separatorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox_separatorActionPerformed
        String c = jComboBox_separator.getSelectedItem().toString();
        switch (c) {
            case "space":
                myCE.par.separator = ' ';
                break;
            case "tab":
                myCE.par.separator = (char) 9;
                break;
            case "comma":
                myCE.par.separator = ',';
                break;
            default:
                break;
        }
    }//GEN-LAST:event_jComboBox_separatorActionPerformed

    private void startRun() {
        graph1st.removeAllPoints();
        graph2nd.removeAllPoints();
        graphAdj.removeAllPoints();
        sumBaseline = 0;
        numBaseline = 0;
        graph1st.renderer.setSeriesPaint(0, Color.BLUE);
        graph2nd.renderer.setSeriesPaint(0, Color.RED);
        graphAdj.renderer.setSeriesPaint(0, Color.BLUE);
        graphAdj.renderer.setSeriesPaint(1, Color.RED);
        myCE.setMustBackup(jCheckBox_MustBackup.isSelected());
        jLabel_prog_pressure.setText("pressure   |   " + NumString.format(myCE.par.inj_Pressure, 6, 1) + " kPa |   " + NumString.format(myCE.par.run_Pressure, 6, 1) + " kPa |   " + NumString.format(myCE.par.post_Pressure, 6, 1) + " kPa");
        jLabel_prog_voltage.setText("voltage    |   " + NumString.format(myCE.par.inj_Voltage, 6, 2) + " kV  |   " + NumString.format(myCE.par.run_Voltage, 6, 2) + " kV  |   " + NumString.format(myCE.par.post_Voltage, 6, 2) + " kV");
        jLabel_prog_current.setText("current    |   " + NumString.format(myCE.par.inj_Current, 6, 1) + " uA  |   " + NumString.format(myCE.par.run_Current, 6, 1) + " uA  |   " + NumString.format(myCE.par.post_Current, 6, 1) + " uA");
        jLabel_prog_time.setText("time       |   " + NumString.format(myCE.par.inj_Time_s, 6, 1) + " s   |   " + NumString.format(myCE.par.run_Time_min, 6, 1) + " min |   " + NumString.format(myCE.par.post_Time_min, 6, 1) + " min");
        myCE.startRun();
    }

    private void jButton_AcceptActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_AcceptActionPerformed
        if (myCE.isSerialConnected()) {
            jPanel_just_Test.setVisible(true);
            jTabbedPane_Input.setEnabled(true);
            jTabbedPane_monitor.setEnabled(true);
            jButton_Accept.setEnabled(false);
            if (!myCE.isEngaged()) {
                myCE.engage();
                if (myCE.isEngaged()) {
                    jLabel_EngagedWith.setText(myCE.getEngagedWith());
                }
            }
        }
    }//GEN-LAST:event_jButton_AcceptActionPerformed

    private void jButton_send_a_messageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_send_a_messageActionPerformed
        myCE.send(jTextField_command_line.getText());
    }//GEN-LAST:event_jButton_send_a_messageActionPerformed

    private void recalcCurrent() {
        myCE.par.capillaryLength = NumString.getDouble(jTextField_CapillaryLength.getText(), 0.1, 999.9, 50.0);
        jTextField_CapillaryLength.setText(NumString.format(myCE.par.capillaryLength, 6, 1));
        myCE.par.voltage = NumString.getDouble(jTextField_Voltage.getText(), -40.0, 40.0, 30.0);
        jTextField_Voltage.setText(NumString.format(myCE.par.voltage, 6, 2));
        myCE.par.bgeConductivity = NumString.getDouble(jTextField_BGEconductivity.getText(), 1.0e-07, 20.0, 4.74e-02);
        jTextField_BGEconductivity.setText(String.valueOf(myCE.par.bgeConductivity));
        myCE.par.capillaryID = NumString.getDouble(jTextField_InnerDiam.getText(), 1.0, 1000.0, 50.0);
        jTextField_InnerDiam.setText(NumString.format(myCE.par.capillaryID, 6, 1));
        double current = 0.025 * Math.PI * myCE.par.voltage * (myCE.par.capillaryID * myCE.par.capillaryID) * myCE.par.bgeConductivity / myCE.par.capillaryLength;
        jLabel_Expected_Current.setText("Expected current: " + NumString.format(current, 6, 1) + " uA");
    }

    private void jTextField_CapillaryLengthFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField_CapillaryLengthFocusLost
        recalcCurrent();
    }//GEN-LAST:event_jTextField_CapillaryLengthFocusLost

    private void jTextField_VoltageFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField_VoltageFocusLost
        recalcCurrent();
    }//GEN-LAST:event_jTextField_VoltageFocusLost

    private void jTextField_BGEconductivityFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField_BGEconductivityFocusLost
        recalcCurrent();
    }//GEN-LAST:event_jTextField_BGEconductivityFocusLost

    private void jTextField_InnerDiamFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField_InnerDiamFocusLost
        recalcCurrent();
    }//GEN-LAST:event_jTextField_InnerDiamFocusLost

    private void jTextField_tMfirstFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField_tMfirstFocusLost
        myCE.par.tMfirst = NumString.getDouble(jTextField_tMfirst.getText(), 0.001, 9999.0, 1.234);
        jTextField_tMfirst.setText(NumString.format(myCE.par.tMfirst, 8, 3));
    }//GEN-LAST:event_jTextField_tMfirstFocusLost

    private void jTextField_tMsecondFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField_tMsecondFocusLost
        myCE.par.tMsecond = NumString.getDouble(jTextField_tMsecond.getText(), 0.001, 9999.0, 1.234);
        jTextField_tMsecond.setText(NumString.format(myCE.par.tMsecond, 8, 3));
    }//GEN-LAST:event_jTextField_tMsecondFocusLost

    private void jTextField_tMrefFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField_tMrefFocusLost
        myCE.par.tMref = NumString.getDouble(jTextField_tMref.getText(), 0.001, 9999.0, 1.234);
        jTextField_tMref.setText(NumString.format(myCE.par.tMref, 8, 3));
    }//GEN-LAST:event_jTextField_tMrefFocusLost

    private void jButton_UpdateBaselineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_UpdateBaselineActionPerformed
        fAdjT_1 = myCE.par.tMref / myCE.par.tMfirst;
        fAdjT_2 = myCE.par.tMref / myCE.par.tMsecond;
        baseline = (sumBaseline / numBaseline) / 2;
        graphAdj.removeAllPoints();
        for (int i = 0; i < graph1st.xy.getItemCount() - 1; i++) {
            graphAdj.addPoint1(fAdjT_1 * graph1st.xy.getDataItem(i).getXValue(), graph1st.xy.getDataItem(i).getYValue() + baseline + 100, false);
            graphAdj.addPoint2(fAdjT_2 * graph2nd.xy.getDataItem(i).getXValue(), graph2nd.xy.getDataItem(i).getYValue() - baseline, false);
        }
    }//GEN-LAST:event_jButton_UpdateBaselineActionPerformed

    private void jButton_rescanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_rescanActionPerformed
        jComboBox_port.setModel(new javax.swing.DefaultComboBoxModel(SerialPortList.getPortNames()));
    }//GEN-LAST:event_jButton_rescanActionPerformed

    private void jTextField_CapillaryLengthActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField_CapillaryLengthActionPerformed
        recalcCurrent();
    }//GEN-LAST:event_jTextField_CapillaryLengthActionPerformed

    private void jTextField_InnerDiamActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField_InnerDiamActionPerformed
        recalcCurrent();
    }//GEN-LAST:event_jTextField_InnerDiamActionPerformed

    private void jTextField_VoltageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField_VoltageActionPerformed
        recalcCurrent();
    }//GEN-LAST:event_jTextField_VoltageActionPerformed

    private void jTextField_BGEconductivityActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField_BGEconductivityActionPerformed
        recalcCurrent();
    }//GEN-LAST:event_jTextField_BGEconductivityActionPerformed

    private void jButton_save_ReportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_save_ReportActionPerformed
        SimpleFileWriter arq = new SimpleFileWriter();
        arq.open(myCE.par.directory + myCE.getRunName() + "_report.txt");
        if (arq.isOpen()) {
            if (jCheckBox_saveElectrophoreticConditions.isSelected()) {
                arq.writeln("Electrophoretic conditions");
                arq.writeln("Capillary length: " + NumString.format(myCE.par.capillaryLength, 6, 1) + " cm");
                arq.writeln("Capillary i.d.: " + NumString.format(myCE.par.capillaryID, 6, 1) + " um");
                arq.writeln("Voltage: " + NumString.format(myCE.par.voltage, 6, 2) + " kV");
                arq.writeln("BGE conductivity: " + NumString.format(myCE.par.bgeConductivity, 6, 1) + " S/m");
                arq.writeln("");
            }
            arq.writeln("IoP = " + myCE.getIoP() + " Pa.s");
            arq.writeln("Comments:");
            arq.write(jTextPane_Comments.getText());
            arq.close();
        }
    }//GEN-LAST:event_jButton_save_ReportActionPerformed

    private void getInjVoltage() {
        myCE.par.inj_Voltage = NumString.getDouble(jTF_inj_Voltage.getText(), 0.0, 30.0, 0.0);
        jTF_inj_Voltage.setText(NumString.format(myCE.par.inj_Voltage, 6, 2));
    }

    private void jTF_inj_VoltageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTF_inj_VoltageActionPerformed
        getInjVoltage();
    }//GEN-LAST:event_jTF_inj_VoltageActionPerformed

    private void jRadioButton_MultipleRunsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButton_MultipleRunsActionPerformed
        myCE.overAndOver = jRadioButton_MultipleRuns.isSelected();
    }//GEN-LAST:event_jRadioButton_MultipleRunsActionPerformed

    private void getInjPressure() {
        myCE.par.inj_Pressure = NumString.getDouble(jTF_inj_Pressure.getText(), 0.0, 45.0, 10.0);
        jTF_inj_Pressure.setText(NumString.format(myCE.par.inj_Pressure, 5, 1));
    }

    private void jTF_inj_PressureActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTF_inj_PressureActionPerformed
        getInjPressure();
    }//GEN-LAST:event_jTF_inj_PressureActionPerformed

    private void jTF_inj_PressureFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTF_inj_PressureFocusLost
        getInjPressure();
    }//GEN-LAST:event_jTF_inj_PressureFocusLost

    private void getPostVoltage() {
        myCE.par.post_Voltage = NumString.getDouble(jTF_post_Voltage.getText(), 0.0, 30.0, 10.0);
        jTF_post_Voltage.setText(NumString.format(myCE.par.post_Voltage, 6, 2));
    }

    private void jTF_post_VoltageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTF_post_VoltageActionPerformed
        getPostVoltage();
    }//GEN-LAST:event_jTF_post_VoltageActionPerformed

    private void getRunPressure() {
        myCE.par.run_Pressure = NumString.getDouble(jTF_run_Pressure.getText(), 0.0, 45.0, 10.0);
        jTF_run_Pressure.setText(NumString.format(myCE.par.run_Pressure, 5, 1));
    }

    private void jTF_run_PressureActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTF_run_PressureActionPerformed
        getRunPressure();
    }//GEN-LAST:event_jTF_run_PressureActionPerformed

    private void jTF_inj_VoltageFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTF_inj_VoltageFocusLost
        getInjVoltage();
    }//GEN-LAST:event_jTF_inj_VoltageFocusLost

    private void getInjCurrent() {
        myCE.par.inj_Current = NumString.getDouble(jTF_inj_Current.getText(), 0.0, 200.0, 100.0);
        jTF_inj_Current.setText(NumString.format(myCE.par.inj_Current, 6, 1));
    }

    private void jTF_inj_CurrentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTF_inj_CurrentActionPerformed
        getInjCurrent();
    }//GEN-LAST:event_jTF_inj_CurrentActionPerformed

    private void jTF_inj_CurrentFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTF_inj_CurrentFocusLost
        getInjCurrent();
    }//GEN-LAST:event_jTF_inj_CurrentFocusLost

    private void getInjTime() {
        myCE.par.inj_Time_s = NumString.getDouble(jTF_inj_Time.getText(), 0.0, 99.0, 5.0);
        jTF_inj_Time.setText(String.valueOf((int) myCE.par.inj_Time_s));
    }

    private void jTF_inj_TimeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTF_inj_TimeActionPerformed
        getInjTime();
    }//GEN-LAST:event_jTF_inj_TimeActionPerformed

    private void jTF_inj_TimeFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTF_inj_TimeFocusLost
        getInjTime();
    }//GEN-LAST:event_jTF_inj_TimeFocusLost

    private void jTF_run_PressureFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTF_run_PressureFocusLost
        getRunPressure();
    }//GEN-LAST:event_jTF_run_PressureFocusLost

    private void getRunVoltage() {
        myCE.par.run_Voltage = NumString.getDouble(jTF_run_Voltage.getText(), 0.0, 30.0, 0.0);
        jTF_run_Voltage.setText(NumString.format(myCE.par.run_Voltage, 6, 2));
    }

    private void jTF_run_VoltageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTF_run_VoltageActionPerformed
        getRunVoltage();
    }//GEN-LAST:event_jTF_run_VoltageActionPerformed

    private void jTF_run_VoltageFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTF_run_VoltageFocusLost
        getRunVoltage();
    }//GEN-LAST:event_jTF_run_VoltageFocusLost

    private void getRunCurrent() {
        myCE.par.run_Current = NumString.getDouble(jTF_run_Current.getText(), 0.0, 200.0, 100.0);
        jTF_run_Current.setText(NumString.format(myCE.par.run_Current, 6, 1));
    }

    private void jTF_run_CurrentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTF_run_CurrentActionPerformed
        getRunCurrent();
    }//GEN-LAST:event_jTF_run_CurrentActionPerformed

    private void jTF_run_CurrentFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTF_run_CurrentFocusLost
        getRunCurrent();
    }//GEN-LAST:event_jTF_run_CurrentFocusLost

    private void getRunTime() {
        myCE.par.run_Time_min = NumString.getDouble(jTF_run_Time.getText(), 0.0, 120.0, 15.0);
        jTF_run_Time.setText(NumString.format(myCE.par.run_Time_min, 6, 1));
    }

    private void jTF_run_TimeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTF_run_TimeActionPerformed
        getRunTime();
    }//GEN-LAST:event_jTF_run_TimeActionPerformed

    private void jTF_run_TimeFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTF_run_TimeFocusLost
        getRunTime();
    }//GEN-LAST:event_jTF_run_TimeFocusLost

    private void getPostPressure() {
        myCE.par.post_Pressure = NumString.getDouble(jTF_post_Pressure.getText(), 0.0, 45.0, 10.0);
        jTF_post_Pressure.setText(NumString.format(myCE.par.post_Pressure, 5, 1));
    }

    private void jTF_post_PressureActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTF_post_PressureActionPerformed
        getPostPressure();
    }//GEN-LAST:event_jTF_post_PressureActionPerformed

    private void jTF_post_PressureFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTF_post_PressureFocusLost
        getPostPressure();
    }//GEN-LAST:event_jTF_post_PressureFocusLost

    private void jTF_post_VoltageFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTF_post_VoltageFocusLost
        getPostVoltage();
    }//GEN-LAST:event_jTF_post_VoltageFocusLost

    private void getPostCurrent() {
        myCE.par.post_Current = NumString.getDouble(jTF_post_Current.getText(), 0.0, 200.0, 100.0);
        jTF_post_Current.setText(NumString.format(myCE.par.post_Current, 6, 1));
    }

    private void jTF_post_CurrentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTF_post_CurrentActionPerformed
        getPostCurrent();
    }//GEN-LAST:event_jTF_post_CurrentActionPerformed

    private void jTF_post_CurrentFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTF_post_CurrentFocusLost
        getPostCurrent();
    }//GEN-LAST:event_jTF_post_CurrentFocusLost

    private void getPostTime() {
        myCE.par.post_Time_min = NumString.getDouble(jTF_post_Time.getText(), 0.0, 120.0, 15.0);
        jTF_post_Time.setText(NumString.format(myCE.par.post_Time_min, 6, 1));
    }

    private void jTF_post_TimeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTF_post_TimeActionPerformed
        getPostTime();
    }//GEN-LAST:event_jTF_post_TimeActionPerformed

    private void jTF_post_TimeFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTF_post_TimeFocusLost
        getPostTime();
    }//GEN-LAST:event_jTF_post_TimeFocusLost

    Color lightRed = Color.getHSBColor(0.0f, 0.3f, 1.0f);
    Color lightBlue = Color.getHSBColor(0.55f, 0.3f, 1.0f);

    private void jTB_polarity_injActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTB_polarity_injActionPerformed
        myCE.par.inj_positive = jTB_polarity_inj.isSelected();
        if (jTB_polarity_inj.isSelected()) {
            jTB_polarity_inj.setText("+");
            jTB_polarity_inj.setBackground(lightRed);
        } else {
            jTB_polarity_inj.setText("-");
            jTB_polarity_inj.setBackground(lightBlue);
        }
    }//GEN-LAST:event_jTB_polarity_injActionPerformed

    private void jTB_polarity_runActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTB_polarity_runActionPerformed
        myCE.par.run_positive = jTB_polarity_run.isSelected();
        if (jTB_polarity_run.isSelected()) {
            jTB_polarity_run.setText("+");
            jTB_polarity_run.setBackground(lightRed);
        } else {
            jTB_polarity_run.setText("-");
            jTB_polarity_run.setBackground(lightBlue);
        }
    }//GEN-LAST:event_jTB_polarity_runActionPerformed

    private void jTB_polarity_postActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTB_polarity_postActionPerformed
        myCE.par.post_positive = jTB_polarity_post.isSelected();
        if (jTB_polarity_post.isSelected()) {
            jTB_polarity_post.setText("+");
            jTB_polarity_post.setBackground(lightRed);
        } else {
            jTB_polarity_post.setText("-");
            jTB_polarity_post.setBackground(lightBlue);
        }
    }//GEN-LAST:event_jTB_polarity_postActionPerformed

    private void jButton_StartExperimentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_StartExperimentActionPerformed
        jTabbedPane_Input.setSelectedComponent(jPanel_Experiment);
        jTabbedPane_Input.setEnabled(false);
        startRun();
    }//GEN-LAST:event_jButton_StartExperimentActionPerformed

    private void jButton_StopExperimentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_StopExperimentActionPerformed
        myCE.overAndOver = false;
        myCE.stopRun();
        jTabbedPane_Input.setEnabled(true);
    }//GEN-LAST:event_jButton_StopExperimentActionPerformed

    private void jButton_halt1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_halt1ActionPerformed
        myCE.setLED(0);
    }//GEN-LAST:event_jButton_halt1ActionPerformed

    private void jButton_PumpOffActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_PumpOffActionPerformed
        myCE.pumpOn(false);
    }//GEN-LAST:event_jButton_PumpOffActionPerformed

    private void jButton_PumpOnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_PumpOnActionPerformed
        myCE.pumpOn(true);
    }//GEN-LAST:event_jButton_PumpOnActionPerformed

    private void jButton_ValveOnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_ValveOnActionPerformed
        myCE.valveOn(true);
    }//GEN-LAST:event_jButton_ValveOnActionPerformed

    private void jButton_ValveOffActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_ValveOffActionPerformed
        myCE.valveOn(false);
    }//GEN-LAST:event_jButton_ValveOffActionPerformed

    private void jButton_SetPressureActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_SetPressureActionPerformed
        myCE.pumpSetPressure(NumString.getDouble(jTextField_Pressure.getText(), 0.0, 40.0, 10.0));
    }//GEN-LAST:event_jButton_SetPressureActionPerformed

    private void jButton_PumpHaltActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_PumpHaltActionPerformed
        myCE.pumpHalt();
    }//GEN-LAST:event_jButton_PumpHaltActionPerformed

    private void jButton_StopRunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_StopRunActionPerformed
        myCE.stopRun();
    }//GEN-LAST:event_jButton_StopRunActionPerformed

    private void jTextField_PrefixActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField_PrefixActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField_PrefixActionPerformed

    private void jCheckBox_MustBackupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox_MustBackupActionPerformed
        jButton_chooseDirectoryBackup.setEnabled(jCheckBox_MustBackup.isSelected());
    }//GEN-LAST:event_jCheckBox_MustBackupActionPerformed

    private void jButton_AskIoPActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_AskIoPActionPerformed
        myCE.askIoP();
    }//GEN-LAST:event_jButton_AskIoPActionPerformed

    /**
     * This adapter allows a safe ending procedure for the application when the
     * window is closed.
     */
    private class CleanUpAdapter extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent we) {
            System.out.println("Cleaning and ending the program...");
            myCE.par.save("last.par");
            myCE.close();
            System.exit(0);
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        FlatLightLaf.setup();
        java.awt.EventQueue.invokeLater(() -> {
            new FrontEnd().setVisible(true);
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel PanelStatus;
    private javax.swing.JButton jButton_Accept;
    private javax.swing.JButton jButton_AskIoP;
    private javax.swing.JButton jButton_ConectSerial;
    private javax.swing.JButton jButton_PumpHalt;
    private javax.swing.JButton jButton_PumpOff;
    private javax.swing.JButton jButton_PumpOn;
    private javax.swing.JButton jButton_SetPressure;
    private javax.swing.JButton jButton_StartExperiment;
    private javax.swing.JButton jButton_StopExperiment;
    private javax.swing.JButton jButton_StopRun;
    private javax.swing.JButton jButton_UpdateBaseline;
    private javax.swing.JButton jButton_ValveOff;
    private javax.swing.JButton jButton_ValveOn;
    private javax.swing.JButton jButton_chooseDirectory;
    private javax.swing.JButton jButton_chooseDirectoryBackup;
    private javax.swing.JButton jButton_halt;
    private javax.swing.JButton jButton_halt1;
    private javax.swing.JButton jButton_once;
    private javax.swing.JButton jButton_rescan;
    private javax.swing.JButton jButton_save_Report;
    private javax.swing.JButton jButton_send_a_message;
    private javax.swing.JCheckBox jCheckBox_MustBackup;
    private javax.swing.JCheckBox jCheckBox_saveElectrophoreticConditions;
    private javax.swing.JComboBox jComboBox_baudRate;
    private javax.swing.JComboBox jComboBox_complement;
    private javax.swing.JComboBox jComboBox_port;
    private javax.swing.JComboBox jComboBox_separator;
    private javax.swing.JFileChooser jFileChooser_directory;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabelC4D_running;
    private javax.swing.JLabel jLabelHV_Enabled;
    private javax.swing.JLabel jLabelHV_GFI;
    private javax.swing.JLabel jLabelHV_Icontrolled;
    private javax.swing.JLabel jLabelHV_Step;
    private javax.swing.JLabel jLabelHV_Vcontrolled;
    private javax.swing.JLabel jLabelHV_current;
    private javax.swing.JLabel jLabelHV_positive;
    private javax.swing.JLabel jLabelHV_returnCurrent;
    private javax.swing.JLabel jLabelHV_running;
    private javax.swing.JLabel jLabelHV_time;
    private javax.swing.JLabel jLabelHV_totalCurrent;
    private javax.swing.JLabel jLabelHV_voltage;
    private javax.swing.JLabel jLabelPump_pressure;
    private javax.swing.JLabel jLabelPump_pumping;
    private javax.swing.JLabel jLabelPump_time;
    private javax.swing.JLabel jLabel_EngagedWith;
    private javax.swing.JLabel jLabel_Expected_Current;
    private javax.swing.JLabel jLabel_IoP;
    private javax.swing.JLabel jLabel_NumberOfClicks;
    private javax.swing.JLabel jLabel_Pressure;
    private javax.swing.JLabel jLabel_Raw_Status;
    private javax.swing.JLabel jLabel_areFilesClosed;
    private javax.swing.JLabel jLabel_directory;
    private javax.swing.JLabel jLabel_directory1;
    private javax.swing.JLabel jLabel_directoryBackup;
    private javax.swing.JLabel jLabel_from_the_port;
    private javax.swing.JLabel jLabel_prog_current;
    private javax.swing.JLabel jLabel_prog_pressure;
    private javax.swing.JLabel jLabel_prog_time;
    private javax.swing.JLabel jLabel_prog_voltage;
    private javax.swing.JLabel jLabel_title_from_the_serial;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel_C4D_monitor;
    private javax.swing.JPanel jPanel_Conditions;
    private javax.swing.JPanel jPanel_Connection;
    private javax.swing.JPanel jPanel_Experiment;
    private javax.swing.JPanel jPanel_HVmonitor;
    private javax.swing.JPanel jPanel_MonitorC4D;
    private javax.swing.JPanel jPanel_MonitorC4Dplus;
    private javax.swing.JPanel jPanel_MonitorMonitor;
    private javax.swing.JPanel jPanel_MonitorPro;
    private javax.swing.JPanel jPanel_Pro;
    private javax.swing.JPanel jPanel_PumpMonitor;
    private javax.swing.JPanel jPanel_SetMarkers;
    private javax.swing.JPanel jPanel_This_Port;
    private javax.swing.JPanel jPanel_just_Test;
    private javax.swing.JPanel jPanel_mold1st;
    private javax.swing.JPanel jPanel_mold2nd;
    private javax.swing.JPanel jPanel_moldAdj;
    private javax.swing.JPanel jPanel_moldMonitor;
    private javax.swing.JRadioButton jRadioButton_MultipleRuns;
    private javax.swing.JRadioButton jRadioButton_autoScale_1;
    private javax.swing.JRadioButton jRadioButton_autoScale_2;
    private javax.swing.JRadioButton jRadioButton_autoScale_Adj;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JToggleButton jTB_polarity_inj;
    private javax.swing.JToggleButton jTB_polarity_post;
    private javax.swing.JToggleButton jTB_polarity_run;
    private javax.swing.JTextField jTF_inj_Current;
    private javax.swing.JTextField jTF_inj_Pressure;
    private javax.swing.JTextField jTF_inj_Time;
    private javax.swing.JTextField jTF_inj_Voltage;
    private javax.swing.JTextField jTF_post_Current;
    private javax.swing.JTextField jTF_post_Pressure;
    private javax.swing.JTextField jTF_post_Time;
    private javax.swing.JTextField jTF_post_Voltage;
    private javax.swing.JTextField jTF_run_Current;
    private javax.swing.JTextField jTF_run_Pressure;
    private javax.swing.JTextField jTF_run_Time;
    private javax.swing.JTextField jTF_run_Voltage;
    private javax.swing.JTabbedPane jTabbedPane_Input;
    private javax.swing.JTabbedPane jTabbedPane_monitor;
    private javax.swing.JTextField jTextField_BGEconductivity;
    private javax.swing.JTextField jTextField_CapillaryLength;
    private javax.swing.JTextField jTextField_InnerDiam;
    private javax.swing.JTextField jTextField_Prefix;
    private javax.swing.JTextField jTextField_Pressure;
    private javax.swing.JTextField jTextField_Voltage;
    private javax.swing.JTextField jTextField_command_line;
    private javax.swing.JTextField jTextField_fileName;
    private javax.swing.JTextField jTextField_tMfirst;
    private javax.swing.JTextField jTextField_tMref;
    private javax.swing.JTextField jTextField_tMsecond;
    private javax.swing.JTextPane jTextPane_Comments;
    // End of variables declaration//GEN-END:variables
}
