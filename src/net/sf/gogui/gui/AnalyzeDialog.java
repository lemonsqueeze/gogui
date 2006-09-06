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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
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
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gtp.GtpUtil;
import net.sf.gogui.util.PrefUtil;

//----------------------------------------------------------------------------

/** Dialog for selecting an AnalyzeCommand. */
public final class AnalyzeDialog
    extends JDialog
    implements ActionListener, ListSelectionListener
{
    /** Callback for actions generated by AnalyzeDialog. */
    public interface Callback
    {
        void clearAnalyzeCommand();
        
        void setAnalyzeCommand(AnalyzeCommand command, boolean autoRun,
                               boolean clearBoard, boolean oneRunOnly);
    }

    public AnalyzeDialog(Frame owner, Callback callback,
                         boolean onlySupported, boolean sort,
                         ArrayList supportedCommands,
                         String programAnalyzeCommands,
                         GuiGtpClient gtp)
    {
        super(owner, "Analyze");
        m_gtp = gtp;
        m_onlySupportedCommands = onlySupported;
        m_sort = sort;
        m_supportedCommands = supportedCommands;
        m_programAnalyzeCommands = programAnalyzeCommands;
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
            runCommand();
        else
            assert(false);
    }

    public void close()
    {
        if (! m_autoRun.isSelected())
            clearCommand();
        saveRecent();
        setVisible(false);
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
            ArrayList supportedCommands = null;
            if (m_onlySupportedCommands)
                supportedCommands = m_supportedCommands;
            AnalyzeCommand.read(m_commands, m_labels, supportedCommands,
                                m_programAnalyzeCommands);
            if (m_sort)
                sortLists();
            m_list.setListData(m_labels.toArray());
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
        int start = (m_firstIsTemp ? 1 : 0);
        int max = Math.min(m_comboBoxHistory.getItemCount(), 20);
        ArrayList list = new ArrayList(max);
        for (int i = start; i < max; ++i)
            list.add(getComboBoxItem(i));
        PrefUtil.putList("net/sf/gogui/gui/analyzedialog/recentcommands",
                         list);
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

    public void valueChanged(ListSelectionEvent e)
    {
        int index = m_list.getSelectedIndex();
        if (index >= 0)
            selectCommand(index);
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

    /** Is the first item in the history combo box a temporary item?
        Avoids that the first item in the history combo box is treated
        as a real history command, if it was not run.
    */
    private boolean m_firstIsTemp;

    private int m_boardSize = GoPoint.DEFAULT_SIZE;

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private GoColor m_selectedColor = GoColor.EMPTY;

    private final GuiGtpClient m_gtp;

    private JButton m_clearButton;

    private JButton m_runButton;

    private JCheckBox m_autoRun;

    private JCheckBox m_clearBoard;

    private JComboBox m_comboBoxHistory;

    private JComboBox m_comboBoxColor;

    private JLabel m_labelColor;

    private JList m_list;

    private JPanel m_colorPanel;

    private final ArrayList m_commands = new ArrayList(128);

    private final ArrayList m_supportedCommands;

    private final ArrayList m_labels = new ArrayList(128);

    private final Callback m_callback;

    private final String m_programAnalyzeCommands;

    private void clearCommand()
    {
        m_callback.clearAnalyzeCommand();
        m_autoRun.setSelected(false);
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
            m_list.setSelectedValue(label, false);
    }

    private JPanel createButtons()
    {
        JPanel innerPanel = new JPanel(new GridLayout(1, 0, GuiUtil.PAD, 0));
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
                        //int index =
                        //   m_list.locationToIndex(event.getPoint());
                        runCommand();
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
        panel.add(m_comboBoxHistory);
        JPanel lowerPanel = new JPanel();
        lowerPanel.setLayout(new BoxLayout(lowerPanel, BoxLayout.Y_AXIS));
        lowerPanel.setBorder(GuiUtil.createEmptyBorder());
        panel.add(lowerPanel);
        JPanel optionsPanel
            = new JPanel(new GridLayout(0, 2, GuiUtil.PAD, 0));
        lowerPanel.add(optionsPanel);
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        optionsPanel.add(leftPanel);
        m_autoRun = new JCheckBox("Auto run");
        m_autoRun.setToolTipText("Automatically run after changes on board");
        leftPanel.add(m_autoRun);
        m_clearBoard = new JCheckBox("Clear board");
        m_clearBoard.setToolTipText("Clear board before displaying result");
        leftPanel.add(m_clearBoard);
        m_clearBoard.setSelected(true);
        JPanel rightPanel = new JPanel();
        rightPanel.add(createColorPanel());
        optionsPanel.add(rightPanel);
        lowerPanel.add(createButtons());
        m_comboBoxHistory.addActionListener(this);
        loadRecent();
        return panel;
    }

    private String getComboBoxItem(int i)
    {
        return (String)m_comboBoxHistory.getItemAt(i);
    }

    private void loadRecent()
    {
        m_comboBoxHistory.removeAllItems();
        ArrayList list =
            PrefUtil.getList("net/sf/gogui/gui/analyzedialog/recentcommands");
        for (int i = 0; i < list.size(); ++i)
            m_comboBoxHistory.addItem((String)list.get(i));
        m_firstIsTemp = false;
    }

    private void runCommand()
    {
        if (m_gtp.isCommandInProgress())
        {
            SimpleDialogs.showError(this, "Command in progress");
            return;
        }
        int index = m_list.getSelectedIndex();        
        if (index < 0)
            return;
        updateRecent(index);
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
                String value = m_gtp.send(commandWithoutArg);
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
                command.setPointListArg(new ArrayList());
                String commandWithoutArg =
                    command.replaceWildCards(m_selectedColor) + " show";
                String response = m_gtp.send(commandWithoutArg);
                ArrayList pointList =
                    GtpUtil.parsePointArrayList(response, m_boardSize);
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
        m_callback.setAnalyzeCommand(command, autoRun, false, false);
    }

    private void selectCommand(int index)
    {
        boolean needsColorArg =
            AnalyzeCommand.needsColorArg((String)m_commands.get(index));
        m_labelColor.setEnabled(needsColorArg);
        m_comboBoxColor.setEnabled(needsColorArg);
        m_runButton.setEnabled(true);
        String label = (String)m_labels.get(index);
        m_comboBoxHistory.removeActionListener(this);
        if (m_firstIsTemp && m_comboBoxHistory.getItemCount() > 0)
            m_comboBoxHistory.removeItemAt(0);
        m_comboBoxHistory.insertItemAt(label, 0);
        m_firstIsTemp = true;
        m_comboBoxHistory.setSelectedIndex(0);
        m_comboBoxHistory.addActionListener(this);
    }

    private void selectColor()
    {
        if (m_selectedColor == GoColor.BLACK)
            m_comboBoxColor.setSelectedItem("Black");
        else if (m_selectedColor == GoColor.WHITE)
            m_comboBoxColor.setSelectedItem("White");
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

    private void updateRecent(int index)
    {
        m_comboBoxHistory.removeActionListener(this);
        String label = (String)m_labels.get(index);
        if (m_comboBoxHistory.getItemCount() == 0
            || ! getComboBoxItem(0).equals(label))
            m_comboBoxHistory.insertItemAt(label, 0);
        for (int i = 1; i < m_comboBoxHistory.getItemCount(); ++i)
            if (getComboBoxItem(i).equals(label))
                m_comboBoxHistory.removeItemAt(i);
        m_comboBoxHistory.setSelectedIndex(0);
        m_firstIsTemp = false;        
        m_recentModified = true;
        m_comboBoxHistory.addActionListener(this);
    }
}

//----------------------------------------------------------------------------
