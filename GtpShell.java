//=============================================================================
// $Id$
// $Source$
//=============================================================================

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import gtp.*;
import utils.*;

//=============================================================================

class GtpShellText
    extends JTextPane
    implements Scrollable
{
    public GtpShellText()
    {
        m_fontSize = getFont().getSize();
        m_font = new Font("Monospaced", Font.PLAIN, m_fontSize);
        setFont(m_font);
        StyleContext context = StyleContext.getDefaultStyleContext();
        Style def = context.getStyle(StyleContext.DEFAULT_STYLE);
        Style error = addStyle("error", def);
        StyleConstants.setForeground(error, Color.red);
        Style output = addStyle("output", def);
        StyleConstants.setBold(output, true);
        Style log = addStyle("log", def);
        StyleConstants.setForeground(log, new Color(0.5f, 0.5f, 0.5f));
        setEditable(false);
    }

    public void appendComment(String text)
    {
        appendStyledText(text, "log");
    }

    public void appendError(String text)
    {
        appendStyledText(text, "error");
    }

    public void appendInput(String text)
    {
        appendStyledText(text, null);
    }

    public void appendLog(String text)
    {
        appendStyledText(text, "log");
    }

    public void appendOutput(String text)
    {
        appendStyledText(text, "output");
    }

    public boolean getScrollableTracksViewportWidth()
    {
        return false;
    }
    
    private int m_fontSize;
    private Font m_font;
    private Style m_currentStyle;

    private void appendStyledText(String text, String style)
    {
        StyledDocument doc = getStyledDocument();
        Style s = null;
        if (style != null)
            s = getStyle(style);
        try
        {
            if (m_currentStyle != s)
            {
                setLogicalStyle(s);
                m_currentStyle = s;
            }
            setEditable(true);
            doc.insertString(doc.getLength(), text, null);
            setEditable(false);
        }
        catch (BadLocationException e)
        {
            assert(false);
        }
    }
}

//=============================================================================

public class GtpShell
    extends JDialog
    implements ActionListener, ItemListener, KeyListener
{
    public interface Callback
    {
        public boolean sendGtpCommand(String command) throws Gtp.Error;
    }

    GtpShell(Frame owner, String titleprefix, Callback callback)
    {
        super(owner, titleprefix + ": GTP Shell");
        m_callback = callback;
        createMenu();
        Container contentPane = getContentPane();

        m_gtpShellText = new GtpShellText();
        JScrollPane scrollPane =
            new JScrollPane(m_gtpShellText,
                            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);        
        int fontSize = m_gtpShellText.getFont().getSize();
        scrollPane.setPreferredSize(new Dimension(fontSize * 51,
                                                  fontSize * 53));
        scrollPane.getViewport().setBackground(m_gtpShellText.getBackground());
        contentPane.add(scrollPane, BorderLayout.CENTER);
        
        m_comboBox = new JComboBox();
        m_editor = m_comboBox.getEditor();
        m_textField = (JTextField)m_editor.getEditorComponent();
        m_textField.setFocusTraversalKeysEnabled(false);
        m_textField.addKeyListener(this);
        m_model = (MutableComboBoxModel)m_comboBox.getModel();
        m_comboBox.setEditable(true);
        m_comboBox.setFont(m_gtpShellText.getFont());
        m_comboBox.addActionListener(this);
        m_comboBox.addItemListener(this);
        contentPane.add(m_comboBox, BorderLayout.SOUTH);
        pack();
    }
    
    public void actionPerformed(ActionEvent event)
    {
        String command = event.getActionCommand();
        if (command.equals("comboBoxEdited"))
            comboBoxEdited();
        else if (command.equals("save-log"))
            saveLog();
        else if (command.equals("save-commands"))
            saveCommands();
        else if (command.equals("close"))
            hide();
    }
    
    public void itemStateChanged(ItemEvent e)
    {
        String text = m_textField.getText().trim();
        if (! text.equals(""))
        {
            // On Windows JDK 1.4 seleting an item automatically
            // selects all text in the text field, so we undo it.
            m_textField.setText(text);
            m_textField.setCaretPosition(text.length());
        }
    }

    public void keyPressed(KeyEvent e)
    {
    }

    public void keyReleased(KeyEvent e) 
    {
        char c = e.getKeyChar();        
        if (c == KeyEvent.CHAR_UNDEFINED
            || c == KeyEvent.VK_ESCAPE)
            return;
        else if (c == KeyEvent.VK_TAB)
            findBestCompletion();
        popupCompletions();
    }

    public void keyTyped(KeyEvent e)
    {
    }

    public void loadHistory()
    {
        File file = getHistoryFile();
        try
        {
            BufferedReader in = new BufferedReader(new FileReader(file));
            String line = in.readLine();
            while (line != null)
            {
                appendToHistory(line);
                line = in.readLine();
            }
        }
        catch (IOException e)
        {
        }
    }

    public void receivedResponse(String response)
    {
        assert(SwingUtilities.isEventDispatchThread());
        m_log.append(response);
        if (! response.equals("") && response.charAt(0) == '=')
            m_gtpShellText.appendInput(response);
        else
            m_gtpShellText.appendError(response);
    }
    
    public void receivedStdErr(String s)
    {
        assert(SwingUtilities.isEventDispatchThread());
        m_log.append(s);
        m_gtpShellText.appendLog(s);
    }
    
    public void toTop()
    {
        setVisible(true);
        toFront();
        requestFocus();
        m_textField.requestFocus();
    }

    public void sentCommand(String command)
    {
        assert(SwingUtilities.isEventDispatchThread());
        m_log.append(command);
        m_log.append("\n");
        m_commands.append(command);
        m_commands.append("\n");
        m_gtpShellText.appendOutput(command + "\n");
    }
    
    public void saveHistory()
    {
        File file = getHistoryFile();
        try
        {
            PrintWriter out = new PrintWriter(new FileOutputStream(file));
            int maxHistory = 100;
            int n = m_history.size();
            if (n > maxHistory)
                n = maxHistory;
            for (int i = m_history.size() - n; i < m_history.size(); ++i)
                out.println(m_history.get(i));
            out.close();
        }
        catch (FileNotFoundException e)
        {
        }

    }

    public void setInitialCompletions(Vector completions)
    {
        for (int i = completions.size() - 1; i >= 0; --i)
            m_history.add(completions.get(i));
        loadHistory();
        addAllCompletions(m_history);
    }

    public void setProgramCommand(String command)
    {
        m_programCommand = command;
    }

    public void setProgramName(String name)
    {
        m_programName = name;
    }

    public void setProgramVersion(String version)
    {
        m_programVersion = version;
    }

    private boolean m_showModifyWarning = true;
    private Callback m_callback;
    private ComboBoxEditor m_editor;
    private JTextField m_textField;
    private JComboBox m_comboBox;
    private GtpShellText m_gtpShellText;
    private MutableComboBoxModel m_model;
    private StringBuffer m_log = new StringBuffer(8192);
    private StringBuffer m_commands = new StringBuffer(4096);
    private Vector m_history = new Vector(128, 128);
    private String m_programCommand = "unknown";
    private String m_programName = "unknown";
    private String m_programVersion = "unknown";

    private void addAllCompletions(Vector completions)
    {
        // On Windows JDK 1.4 changing the popup automatically
        // selects all text in the text field, so we remember and
        // restore the state.
        String oldText = m_textField.getText();
        int oldCaretPosition = m_textField.getCaretPosition();
        if (completions.size() > m_comboBox.getItemCount())
            m_comboBox.hidePopup();
        m_comboBox.removeAllItems();
        for (int i = completions.size() - 1; i >= 0 ; --i)
        {
            Object object = wrapperObject((String)completions.get(i));
            m_comboBox.addItem(object);
        }
        m_comboBox.setSelectedIndex(-1);
        m_textField.setText(oldText);
        m_textField.setCaretPosition(oldCaretPosition);
    }

    private JMenuItem addMenuItem(JMenu menu, JMenuItem item, String command)
    {
        item.addActionListener(this);
        item.setActionCommand(command);
        menu.add(item);
        return item;
    }

    private JMenuItem addMenuItem(JMenu menu, String label, String command)
    {
        JMenuItem item = new JMenuItem(label);
        return addMenuItem(menu, item, command);        
    }

    private void appendToHistory(String command)
    {
        command = command.trim();
        int i = m_history.indexOf(command);
        if (i >= 0)
            m_history.remove(i);
        m_history.add(command);
    }

    private void comboBoxEdited()
    {
        String command = m_comboBox.getSelectedItem().toString();
        String c = command.trim();
        if (c.equals(""))
            return;
        if (c.startsWith("#"))
        {
            m_log.append(command);
            m_log.append("\n");
            m_gtpShellText.appendComment(command + "\n");
        }
        else
        {
            c.toLowerCase();
            if (m_showModifyWarning
                && (c.startsWith("boardsize")
                    || c.startsWith("black")
                    || c.startsWith("genmove_black")
                    || c.startsWith("genmove_white")
                    || c.startsWith("loadsgf")
                    || c.startsWith("white")
                    || c.startsWith("quit")))
            {
                String message = 
                    "This command will modify the board state\n" +
                    "and will cause the graphical board to be out of sync.\n" +
                    "You must start a new game before using\n" +
                    "the graphical board again.";
                int messageType = JOptionPane.WARNING_MESSAGE;
                int optionType = JOptionPane.OK_CANCEL_OPTION;
                JOptionPane optionPane =
                    new JOptionPane(message, messageType, optionType);
                JDialog dialog =
                    optionPane.createDialog(this, "GoGui: Warning");
                DialogUtils.center(dialog, this);
                dialog.setVisible(true);
                
                Object value = optionPane.getValue();
                if (value == null)
                    return;
                int intValue = ((Integer)value).intValue();
                if (intValue != JOptionPane.OK_OPTION)
                    return;
                message = 
                    "Would you like to disable the warnings about\n" +
                    "commands modifying the board state?";
                m_showModifyWarning =
                    ! SimpleDialogs.showQuestion(this, message);
            }
            try
            {
                if (! m_callback.sendGtpCommand(command))
                    return;
            }
            catch (Gtp.Error e)
            {
            }
        }
        appendToHistory(command);
        m_comboBox.hidePopup();
        addAllCompletions(m_history);
        m_textField.setText("");
    }

    private void createMenu()
    {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);
        addMenuItem(menu, "Save Log", "save-log");
        addMenuItem(menu, "Save Commands", "save-commands");
        menu.addSeparator();
        addMenuItem(menu, "Close", "close");
        menuBar.add(menu);
        setJMenuBar(menuBar);
    }

    private void findBestCompletion()
    {
        String text = m_textField.getText().trim();
        if (text.equals(""))
            return;
        String bestCompletion = null;
        for (int i = 0; i < m_history.size(); ++i)
        {
            String completion = (String)m_history.get(i);
            if (completion.startsWith(text))
            {
                if (bestCompletion == null)
                {
                    bestCompletion = completion;
                    continue;
                }
                int j = text.length();
                while (true)
                {
                    if (j >= bestCompletion.length())
                    {
                        break;
                    }
                    if (j >= completion.length())
                        break;
                    if (bestCompletion.charAt(j) != completion.charAt(j))
                        break;
                    ++j;
                }
                bestCompletion = completion.substring(0, j);
            }
        }       
        if (bestCompletion != null)
            m_textField.setText(bestCompletion);
    }

    private File getHistoryFile()
    {
        String home = System.getProperty("user.home");
        File dir = new File(home, ".gogui");
        if (! dir.exists())
            dir.mkdir();
        return new File(dir, "gtpshell-history");
    }

    private void popupCompletions()
    {
        String text = m_textField.getText();
        text = text.replaceAll("^ *", "");
        Vector completions = new Vector(128, 128);
        for (int i = 0; i < m_history.size(); ++i)
        {
            String c = (String)m_history.get(i);
            if (c.startsWith(text))
                completions.add(c);
        }
        addAllCompletions(completions);
        if (text.length() > 0)
            if (completions.size() > 1
                || (completions.size() == 1
                    && ! text.equals(completions.get(0))))
                m_comboBox.showPopup();
    }

    private File queryFile()
    {
        String dir = System.getProperties().getProperty("user.dir");
        JFileChooser chooser = new JFileChooser(dir);
        chooser.setMultiSelectionEnabled(false);
        int ret = chooser.showSaveDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION)
            return chooser.getSelectedFile();
        return null;
    }

    private void save(StringBuffer s)
    {
        File file = queryFile();
        if (file == null)
            return;
        try
        {
            PrintStream out = new PrintStream(new FileOutputStream(file));
            out.println("# Name: " + m_programName);
            out.println("# Version: " + m_programVersion);
            out.println("# Command: " + m_programCommand);
            out.print(s);
            out.close();
        }
        catch (FileNotFoundException e)
        {
            JOptionPane.showMessageDialog(this, "Could not save to file.",
                                          "GoGui: Error",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveLog()
    {
        save(m_log);
    }

    private void saveCommands()
    {
        save(m_commands);
    }

    /** Create wrapper object for addItem.
        See JDK 1.4 doc for JComboBox.addItem.
     */
    private Object wrapperObject(final String item)
    {
        return new Object()
            {
                public String toString()
                {
                    return item;
                }
            };
    }

}
