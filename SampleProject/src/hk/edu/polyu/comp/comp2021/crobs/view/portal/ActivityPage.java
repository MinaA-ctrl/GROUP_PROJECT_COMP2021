package hk.edu.polyu.comp.comp2021.crobs.view.portal;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;

/**
 * The session activity: every command run by the portal with Crobs' answer, the same text the
 * command-line interface prints. Any CLI command can also be typed here.
 */
final class ActivityPage extends PortalPage {

    private static final long serialVersionUID = 1L;

    private final JTextArea transcript = new JTextArea();
    private final JTextField command = new JTextField();

    /**
     * Creates the activity page and starts recording the session's commands.
     *
     * @param session the portal session
     */
    ActivityPage(PortalSession session) {
        super(session, "Activity", "Every action of this session, exactly as recorded in the log files.");
        transcript.setEditable(false);
        transcript.setFont(PortalTheme.monoFont());
        transcript.setBackground(PortalTheme.paper());
        transcript.setForeground(PortalTheme.ink());
        transcript.setBorder(PortalTheme.padding(PortalTheme.gap() / 2));
        command.setFont(PortalTheme.monoFont());
        command.addActionListener(event -> runTyped());
        session.addListener(result -> {
            transcript.append("crobs> " + result.getCommand() + "\n" + result.getOutput() + "\n");
            transcript.setCaretPosition(transcript.getDocument().getLength());
        });
        JPanel input = new JPanel(new BorderLayout(PortalTheme.gap() / 2, 0));
        input.setOpaque(false);
        input.add(new JLabel("crobs> "), BorderLayout.WEST);
        input.add(command, BorderLayout.CENTER);
        input.add(PortalTheme.primary("Run", this::runTyped), BorderLayout.EAST);
        JPanel content = new JPanel(new BorderLayout(0, PortalTheme.gap()));
        content.setOpaque(false);
        content.add(PortalTheme.card(new JScrollPane(transcript)), BorderLayout.CENTER);
        content.add(input, BorderLayout.SOUTH);
        setContent(content);
    }

    /** Nothing to reload: the transcript is updated as commands run. */
    @Override
    void refresh() {
        command.setEnabled(!session().isClosed());
    }

    private void runTyped() {
        String text = command.getText();
        command.setText("");
        if (!text.trim().isEmpty()) {
            run(text);
        }
    }
}
