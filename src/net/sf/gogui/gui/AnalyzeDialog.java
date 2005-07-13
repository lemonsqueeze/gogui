//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Vector;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gtp.GtpUtils;
import net.sf.gogui.gui.GuiUtils;
import net.sf.gogui.utils.Platform;

//----------------------------------------------------------------------------

/** Dialog for selecting an AnalyzeCommand. */
public final class AnalyzeDialog
    extends JDialog
    implements ActionListener, ListSelectionListener
{
    /** Callback for actions generated by AnalyzeDialog. */
    public interface Callback
    {
        void cbGtpShell();
        
        void cbShowGameTree();
        
        void clearAnalyzeCommand();
        
        void setAnalyzeCommand(AnalyzeCommand command, boolean autoRun,
                               boolean clearBoard, boolean oneRunOnly);
        
        void toTop();
    }

    public AnalyzeDialog(Frame owner, Callback callback,
                         boolean onlySupported, boolean sort,
                         Vector supportedCommands,
                         CommandThread commandThread)
    {
        super(owner, "Analyze");
        m_commandThread = commandThread;
        m_onlySupportedCommands = onlySupported;
        m_sort = sort;
        m_supportedCommands = supportedCommands;
        m_callback = callback;
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        WindowAdapter windowAdapter = new WindowAdapter()
            {
                public void windowClosing(WindowEvent event)
                {
                    close();
                }
            };
        addWindowListener(windowAdapter);
        Container contentPane = getContentPane();
        contentPane.add(createButtons(), BorderLayout.SOUTH);
        contentPane.add(createCommandPanel(), BorderLayout.CENTER);
        comboBoxChanged();
        pack();
        m_list.requestFocusInWindow();
    }

    public void actionPerformed(ActionEvent event)
    {
        String command = event.getActionCommand();
        if (command.equals("clear"))
            clearCommand();
        else if (command.equals("close"))
            close();
        else if (command.equals("comboBoxChanged"))
            comboBoxChanged();
        else if (command.equals("run"))
            setCommand();
        else
            assert(false);
    }

    public GoColor getSelectedColor()
    {
        String selectedItem = (String)m_comboBoxColor.getSelectedItem();
        if (selectedItem.equals("White"))
            return GoColor.WHITE;
        assert(selectedItem.equals("Black"));
        return GoColor.BLACK;
    }

    public void reload()
    {
        try
        {
            Vector supportedCommands = null;
            if (m_onlySupportedCommands)
                supportedCommands = m_supportedCommands;
            AnalyzeCommand.read(m_commands, m_labels, supportedCommands);
            if (m_sort)
                sortLists();
            m_list.setListData(m_labels);
            if (m_labels.size() > 0)
                // Avoid focus problem with Sun JDK 1.4.2 if focus was at an
                // index greater than the new list length
                m_list.setSelectedIndex(0);
            comboBoxChanged();
        }
        catch (Exception e)
        {            
            SimpleDialogs.showError(this, e.getMessage());
        }
    }

    public void saveRecent()
    {
        if (! m_recentModified)
            return;
        File file = getRecentFile();
        PrintStream out;
        try
        {
            out = new PrintStream(new FileOutputStream(file));
        }
        catch (FileNotFoundException e)
        {
            System.err.println("FileNotFoundException in"
                               + " AnalyzeDialog.saveRecent");
            return;
        }
        int max = 20;
        for (int i = 0; i < m_comboBoxHistory.getItemCount() && i < max; ++i)
            out.println(m_comboBoxHistory.getItemAt(i));
        out.close();
    }

    /** Set board size.
        Need for verifying responses to initial value for EPLIST commands.
        Default is 19.
    */
    public void setBoardSize(int boardSize)
    {
        m_boardSize = boardSize;
    }

    public void setOnlySupported(boolean onlySupported)
    {
        m_onlySupportedCommands = onlySupported;
        reload();
    }

    public void setRunButtonEnabled(boolean enabled)
    {
        m_runButton.setEnabled(enabled);
    }

    public void setSelectedColor(GoColor color)
    {
        m_selectedColor = color;
        selectColor();
    }

    public void setSort(boolean sort)
    {
        m_sort = sort;
        reload();
    }

    public void toTop()
    {
        setVisible(true);
        toFront();
    }

    public void valueChanged(ListSelectionEvent e)
    {
        int index = m_list.getSelectedIndex();
        if (index >= 0)
        {
            boolean needsColorArg =
                AnalyzeCommand.needsColorArg((String)m_commands.get(index));
            m_labelColor.setEnabled(needsColorArg);
            m_comboBoxColor.setEnabled(needsColorArg);
            selectColor();
            m_runButton.setEnabled(true);
            m_list.ensureIndexIsVisible(index);
        }
        else
        {
            if (m_runButton.hasFocus())
                m_list.requestFocusInWindow();
            m_runButton.setEnabled(false);
        }
    }

    private boolean m_onlySupportedCommands;

    private boolean m_sort;

    private boolean m_recentModified;

    private int m_boardSize = 19;

    private static final int m_shortcutKeyMask =
        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private GoColor m_selectedColor = GoColor.EMPTY;

    private final CommandThread m_commandThread;

    private JButton m_clearButton;

    private JButton m_runButton;

    private JCheckBox m_autoRun;

    private JCheckBox m_clearBoard;

    private JComboBox m_comboBoxHistory;

    private JComboBox m_comboBoxColor;

    private JLabel m_labelColor;

    private JList m_list;

    private JPanel m_colorPanel;

    private final Vector m_commands = new Vector(128, 64);

    private final Vector m_supportedCommands;

    private final Vector m_labels = new Vector(128, 64);

    private final Callback m_callback;

    private void clearCommand()
    {
        m_callback.clearAnalyzeCommand();
        m_autoRun.setSelected(false);
        m_clearButton.setEnabled(false);
        if (m_clearButton.hasFocus())
            m_list.requestFocusInWindow();
    }

    private void close()
    {
        if (! m_autoRun.isSelected())
            clearCommand();
        saveRecent();
        setVisible(false);
    }

    private void comboBoxChanged()
    {
        String label = (String)m_comboBoxHistory.getSelectedItem();        
        if (! m_labels.contains(label))
        {
            m_list.clearSelection();
            return;
        }
        String selectedValue = (String)m_list.getSelectedValue();
        if (selectedValue == null || ! selectedValue.equals(label))
            m_list.setSelectedValue(label, true);
    }

    private JPanel createButtons()
    {
        JPanel innerPanel = new JPanel(new GridLayout(1, 0, GuiUtils.PAD, 0));
        innerPanel.setBorder(GuiUtils.createEmptyBorder());
        m_runButton = new JButton("Run");
        m_runButton.setToolTipText("Run command");
        m_runButton.setActionCommand("run");
        m_runButton.addActionListener(this);
        m_runButton.setMnemonic(KeyEvent.VK_R);
        getRootPane().setDefaultButton(m_runButton);
        innerPanel.add(m_runButton);
        m_clearButton = new JButton("Clear");
        m_clearButton.setToolTipText("Clear board and cancel auto run");
        m_clearButton.setActionCommand("clear");
        m_clearButton.addActionListener(this);
        m_clearButton.setMnemonic(KeyEvent.VK_C);
        m_clearButton.setEnabled(false);
        innerPanel.add(m_clearButton);
        JPanel outerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        outerPanel.add(innerPanel);
        return outerPanel;
    }

    private JPanel createColorPanel()
    {
        m_colorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        m_labelColor = new JLabel("Color:");
        m_labelColor.setHorizontalAlignment(SwingConstants.LEFT);
        m_colorPanel.add(m_labelColor);
        String[] colors = {"Black", "White"};
        m_comboBoxColor = new JComboBox(colors);
        m_comboBoxColor.setToolTipText("Color argument for command");
        m_colorPanel.add(m_comboBoxColor);
        return m_colorPanel;
    }

    private JPanel createCommandPanel()
    {
        JPanel panel = new JPanel(new BorderLayout());
        m_list = new JList();
        m_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_list.setVisibleRowCount(25);
        MouseAdapter mouseAdapter = new MouseAdapter()
            {
                public void mouseClicked(MouseEvent event)
                {
                    int modifiers = event.getModifiers();
                    int mask = ActionEvent.ALT_MASK;
                    if (event.getClickCount() == 2
                        || ((modifiers & mask) != 0))
                    {
                        int index = m_list.locationToIndex(event.getPoint());
                        selectCommand(index);
                        setCommand();
                    }
                }
            };
        m_list.addMouseListener(mouseAdapter);
        m_list.addListSelectionListener(this);
        JScrollPane scrollPane = new JScrollPane(m_list);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(createLowerPanel(), BorderLayout.SOUTH);
        reload();
        return panel;
    }

    private JPanel createLowerPanel()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        m_comboBoxHistory = new JComboBox();
        m_comboBoxHistory.addActionListener(this);
        panel.add(m_comboBoxHistory);
        JPanel lowerPanel = new JPanel(new GridLayout(0, 2, GuiUtils.PAD, 0));
        panel.add(lowerPanel);
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        lowerPanel.add(leftPanel);
        m_autoRun = new JCheckBox("Auto run");
        m_autoRun.setToolTipText("Automatically run after changes on board");
        leftPanel.add(m_autoRun);
        m_clearBoard = new JCheckBox("Clear board");
        m_clearBoard.setToolTipText("Clear board before displaying result");
        leftPanel.add(m_clearBoard);
        m_clearBoard.setSelected(true);
        JPanel rightPanel = new JPanel();
        rightPanel.add(createColorPanel());
        lowerPanel.add(rightPanel);
        loadRecent();
        return panel;
    }

    private File getRecentFile()
    {
        String home = System.getProperty("user.home");
        return new File(new File(home, ".gogui"), "recent-analyze");
    }

    private void loadRecent()
    {
        m_comboBoxHistory.removeAllItems();
        File file = getRecentFile();
        BufferedReader reader;
        try
        {
            reader = new BufferedReader(new FileReader(file));
        }
        catch (FileNotFoundException e)
        {
            return;
        }
        String line;
        try
        {
            while ((line = reader.readLine()) != null)
            {
                m_comboBoxHistory.addItem(line);
            }
            reader.close();
        }
        catch (IOException e)
        {
            System.err.println("IOException in AnalyzeDialog.loadRecent");
        }
    }

    private void selectCommand(int index)
    {
        String label = (String)m_labels.get(index);
        m_comboBoxHistory.insertItemAt(label, 0);
        for (int i = 1; i < m_comboBoxHistory.getItemCount(); ++i)
            if (((String)m_comboBoxHistory.getItemAt(i)).equals(label))
            {
                m_comboBoxHistory.removeItemAt(i);
                break;
            }
        m_comboBoxHistory.setSelectedIndex(0);
        m_recentModified = true;
    }

    private void selectColor()
    {
        if (m_selectedColor == GoColor.BLACK)
            m_comboBoxColor.setSelectedItem("Black");
        else if (m_selectedColor == GoColor.WHITE)
            m_comboBoxColor.setSelectedItem("White");
    }

    private void setCommand()
    {
        if (m_commandThread.isCommandInProgress())
        {
            SimpleDialogs.showError(this, "Command in progress");
            return;
        }
        int index = m_list.getSelectedIndex();        
        if (index < 0)
            return;
        selectCommand(index);
        String analyzeCommand = (String)m_commands.get(index);
        AnalyzeCommand command = new AnalyzeCommand(analyzeCommand);
        if (command.needsColorArg())
            command.setColorArg(getSelectedColor());
        String label = command.getResultTitle();        
        if (command.needsStringArg())
        {
            String stringArg = JOptionPane.showInputDialog(this, label);
            if (stringArg == null)
                return;
            command.setStringArg(stringArg);
        }
        if (command.needsOptStringArg())
        {
            try
            {
                command.setOptStringArg("");
                String commandWithoutArg =
                    command.replaceWildCards(m_selectedColor);
                String value = m_commandThread.sendCommand(commandWithoutArg);
                String optStringArg =
                    JOptionPane.showInputDialog(this, label, value);
                if (optStringArg == null || optStringArg.equals(value))
                    return;
                command.setOptStringArg(optStringArg);
            }
            catch (GtpError e)
            {
                SimpleDialogs.showError(this, e.getMessage());
                return;
            }
        }
        if (command.getType() == AnalyzeCommand.EPLIST)
        {
            try
            {
                command.setPointListArg(new Vector());
                String commandWithoutArg =
                    command.replaceWildCards(m_selectedColor) + " show";
                String response =
                    m_commandThread.sendCommand(commandWithoutArg);
                Vector pointList =
                    GtpUtils.parsePointListVector(response, m_boardSize);
                command.setPointListArg(pointList);
            }
            catch (GtpError e)
            {
                SimpleDialogs.showError(this, e.getMessage());
                return;
            }
        }
        if (command.needsFileArg())
        {
            File fileArg = SimpleDialogs.showSelectFile(this, label);
            if (fileArg == null)
                return;
            command.setFileArg(fileArg);
        }
        if (command.needsColorArg())
            command.setColorArg(getSelectedColor());
        boolean autoRun = m_autoRun.isSelected();
        boolean clearBoard = m_clearBoard.isSelected();
        if (clearBoard)
            m_callback.clearAnalyzeCommand();
        m_clearButton.setEnabled(true);
        m_callback.setAnalyzeCommand(command, autoRun, false, false);
    }

    private void sortLists()
    {
        for (int i = 0; i < m_labels.size() - 1; ++i)
            for (int j = i + 1; j < m_labels.size(); ++j)
            {
                String labelI = (String)m_labels.get(i);
                String labelJ = (String)m_labels.get(j);
                if (labelI.compareTo(labelJ) > 0)
                {
                    m_labels.set(i, labelJ);
                    m_labels.set(j, labelI);
                    String cmdI = (String)m_commands.get(i);
                    String cmdJ = (String)m_commands.get(j);
                    m_commands.set(i, cmdJ);
                    m_commands.set(j, cmdI);
                }
            }
    }
}

//----------------------------------------------------------------------------
