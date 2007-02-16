//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Locale;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** Dialog for displaying and editing a program. */
public class ProgramEditor
    implements ObjectListEditor.ItemEditor
{
    public Object editItem(Component parent, Object object,
                           MessageDialogs messageDialogs)
    {
        return editItem(parent, "Edit Program", (Program)object, false,
                        messageDialogs);
    }

    public Program editItem(Component parent, String title, Program program,
                            boolean disableName,
                            MessageDialogs messageDialogs)
    {
        m_disableName = disableName;
        JPanel panel = new JPanel(new BorderLayout(GuiUtil.SMALL_PAD, 0));
        m_panelLeft = new JPanel(new GridLayout(0, 1, 0, GuiUtil.PAD));
        panel.add(m_panelLeft, BorderLayout.WEST);
        m_panelRight = new JPanel(new GridLayout(0, 1, 0, GuiUtil.PAD));
        panel.add(m_panelRight, BorderLayout.CENTER);
        if (! disableName)
            m_label = createEntry("Label", 18, program.m_label);
        createCommandEntry(program.m_command, messageDialogs);
        if (! disableName)
        {
            m_name = createEntry("Name", 18, program.m_name, false);
            m_version = createEntry("Version", 18, program.m_version, false);
        }
        JOptionPane optionPane = new JOptionPane(panel,
                                                 JOptionPane.PLAIN_MESSAGE,
                                                 JOptionPane.OK_CANCEL_OPTION);
        m_dialog = optionPane.createDialog(parent, title);
        m_dialog.addWindowListener(new WindowAdapter() {
                public void windowActivated(WindowEvent e) {
                    if (m_label != null)
                        m_label.requestFocusInWindow();
                    else
                        m_command.requestFocusInWindow();
                }
            });
        boolean done = false;
        while (! done)
        {
            m_dialog.setVisible(true);
            Object value = optionPane.getValue();
            if (! (value instanceof Integer)
                || ((Integer)value).intValue() != JOptionPane.OK_OPTION)
                return null;
            done = validate(parent, messageDialogs);
        }
        String newLabel = "";
        String newName = "";
        String newVersion = "";
        if (! disableName)
        {
            newLabel = m_label.getText().trim();
            newName = m_name.getText().trim();
            newVersion = m_version.getText().trim();
        }
        String newCommand = m_command.getText().trim();
        Program newProgram = new Program(newLabel, newName, newVersion,
                                         newCommand);
        m_dialog.dispose();
        return newProgram;
    }

    public String getItemLabel(Object object)
    {
        return ((Program)object).m_label;
    }

    public Object cloneItem(Object object)
    {
        return new Program((Program)object);
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private JPanel m_panelLeft;

    private JPanel m_panelRight;

    private JTextField m_label;

    private JTextField m_name;

    private JTextField m_version;

    private JTextField m_command;

    private JDialog m_dialog;

    private boolean m_disableName;

    private JTextField createEntry(String labelText, int cols, String text)
    {
        return createEntry(labelText, cols, text, true);
    }

    private JTextField createEntry(String labelText, int cols, String text,
                                   boolean editable)
    {
        JComponent label = createEntryLabel(labelText);
        m_panelLeft.add(label);        
        Box box = Box.createVerticalBox();
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        box.add(Box.createVerticalGlue());
        box.add(panel);
        box.add(Box.createVerticalGlue());
        JTextField field = new JTextField(cols);
        field.setText(text);
        if (! editable)
            GuiUtil.setEditableFalse(field);
        panel.add(field);
        m_panelRight.add(box);
        return field;
    }

    private JComponent createEntryLabel(String text)
    {
        Box box = Box.createHorizontalBox();
        JLabel label = new JLabel(text + ":");
        label.setAlignmentY(Component.CENTER_ALIGNMENT);
        GuiUtil.setUnlimitedSize(label);
        box.add(label);
        return box;
    }
    private void createCommandEntry(String text,
                                    final MessageDialogs messageDialogs)
    {
        m_panelLeft.add(createEntryLabel("Command"));
        Box box = Box.createVerticalBox();
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        box.add(Box.createVerticalGlue());
        box.add(panel);
        box.add(Box.createVerticalGlue());
        m_command = new JTextField(30);
        m_command.setText(text);
        panel.add(m_command);
        panel.add(GuiUtil.createSmallFiller());
        JButton button = new JButton();
        panel.add(button);
        button.setIcon(GuiUtil.getIcon("document-open-16x16", "Browse"));
        button.setToolTipText("Browse for Go program");
        button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    File file =
                        FileDialogs.showOpen(m_dialog, "Select Go Program");
                    if (file == null)
                        return;
                    String text = file.toString();
                    if (text.indexOf(' ') >= 0)
                        text = "\"" + text + "\"";        
                    String fileNameToLower =
                        file.getName().toLowerCase(Locale.ENGLISH);
                    if (fileNameToLower.startsWith("gnugo"))
                    {
                        String message =
                            "Append option '--mode gtp' for GNU Go?";
                        if (messageDialogs.showQuestion(m_dialog, message,
                                                        "GNU Go needs this " +
                                                        "option to startup " +
                                                        "in GTP mode.",
                                                        "Append --mode gtp",
                                                        false))
                            text = text + " --mode gtp";
                    }
                    m_command.setText(text);
                    m_command.setCaretPosition(text.length());
                    m_command.requestFocusInWindow();
                }
            });
        m_panelRight.add(box);
    }

    private boolean validate(Component parent, MessageDialogs messageDialogs)
    {
        if (! m_disableName)
        {
            if (m_label.getText().trim().equals(""))
            {
                String mainMessage = "Label cannot be empty";
                String optionalMessage =
                    "You need to enter a label that will be used for "
                    + "the menu item for the Go program.";
                messageDialogs.showError(parent, mainMessage, optionalMessage,
                                         false);
                return false;
            }
        }
        if (m_command.getText().trim().equals(""))
        {
            String mainMessage = "Command cannot be empty";
            String optionalMessage =
                "You need to specify the command line for invoking the Go " +
                "program.";
            messageDialogs.showError(parent, mainMessage, optionalMessage,
                                     false);
            return false;
        }
        return true;
    }
}
