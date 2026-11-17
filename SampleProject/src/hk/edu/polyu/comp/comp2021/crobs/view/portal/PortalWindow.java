package hk.edu.polyu.comp.comp2021.crobs.view.portal;

import hk.edu.polyu.comp.comp2021.crobs.logging.OperationLogger;
import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import hk.edu.polyu.comp.comp2021.crobs.model.User;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The second graphical user interface of Crobs, styled as the room booking portal of the
 * fictional university "UNI" (started with -gui2). Like the classic GUI, every action is run as a
 * text command through the shared command processor and logged, so it behaves exactly like the CLI.
 */
public final class PortalWindow {

    private static final String SIGN_IN = "signIn";
    private static final String PORTAL = "portal";
    private static final String DASHBOARD = "Dashboard";
    private static final String ROOMS = "Rooms";
    private static final String BOOKINGS = "My bookings";
    private static final String FIND = "Find a room";
    private static final String SCHEDULE = "Schedule";
    private static final String USERS = "Users";
    private static final String ACTIVITY = "Activity";
    private static final int WINDOW_WIDTH = 1180;
    private static final int WINDOW_HEIGHT = 780;
    private static final int HEADER_CREST = 40;
    private static final int SIGN_IN_CREST = 120;
    private static final int ICON_SIZE = 64;
    private static final int SIDEBAR_WIDTH = 190;
    private static final int NAV_HEIGHT = 40;
    private static final int SIGN_IN_WIDTH = 380;
    private static final int SIGN_IN_FIELD_HEIGHT = 34;
    private static final int TAGLINE_SIZE = 14;

    private final PortalSession session;
    private final JFrame frame = new JFrame("UNI Campus Room Booking");
    private final CardLayout screens = new CardLayout();
    private final JPanel screenPanel = new JPanel(screens);
    private final CardLayout pages = new CardLayout();
    private final JPanel pagePanel = new JPanel(pages);
    private final Map<String, PortalPage> pageByName = new LinkedHashMap<>();
    private final Map<String, NavButton> navByName = new LinkedHashMap<>();
    private final JLabel userLabel = PortalTheme.label("", PortalTheme.bodyFont(), PortalTheme.stone());
    private final JLabel status = PortalTheme.label(" ", PortalTheme.bodyFont(), PortalTheme.ink());
    private final JLabel signInError = PortalTheme.label(" ", PortalTheme.bodyFont(), PortalTheme.berry());
    private final JTextField signInId = new JTextField("sd00000000");
    private final JButton undoButton = PortalTheme.secondary("↶ Undo", () -> run("undo"));
    private final JButton redoButton = PortalTheme.secondary("Redo ↷", () -> run("redo"));
    private final BookingsPage bookingsPage;
    private String currentPage = DASHBOARD;

    private PortalWindow(CROBS crobs, OperationLogger logger) {
        session = new PortalSession(Objects.requireNonNull(crobs, "crobs"), logger);
        bookingsPage = new BookingsPage(session);
        session.addListener(this::afterCommand);
        addPage(DASHBOARD, new DashboardPage(session, () -> navigate(FIND), () -> navigate(SCHEDULE),
                () -> navigate(BOOKINGS), () -> bookingsPage.newBooking("")));
        addPage(ROOMS, new RoomsPage(session));
        addPage(BOOKINGS, bookingsPage);
        addPage(FIND, new FindRoomPage(session, bookingsPage::book));
        addPage(SCHEDULE, new SchedulePage(session));
        addPage(USERS, new UsersPage(session));
        addPage(ACTIVITY, new ActivityPage(session));
        buildFrame();
    }

    /**
     * Opens the portal on the Swing event thread. The portal owns the logger and closes it when
     * the user quits or closes the window.
     *
     * @param crobs the model to operate on
     * @param logger the logger that records every command
     */
    public static void open(CROBS crobs, OperationLogger logger) {
        SwingUtilities.invokeLater(() -> new PortalWindow(crobs, logger).show());
    }

    private void show() {
        frame.setVisible(true);
    }

    private void addPage(String name, PortalPage page) {
        pageByName.put(name, page);
        pagePanel.add(page, name);
        navByName.put(name, new NavButton(name, () -> navigate(name)));
    }

    private void buildFrame() {
        frame.setIconImage(new UniCrest(ICON_SIZE).toImage());
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                run("quit");
            }
        });
        JPanel portal = new JPanel(new BorderLayout());
        portal.add(buildHeader(), BorderLayout.NORTH);
        portal.add(buildSidebar(), BorderLayout.WEST);
        pagePanel.setBackground(PortalTheme.stone());
        portal.add(pagePanel, BorderLayout.CENTER);
        portal.add(buildStatusBar(), BorderLayout.SOUTH);
        screenPanel.add(buildSignIn(), SIGN_IN);
        screenPanel.add(portal, PORTAL);
        frame.setContentPane(screenPanel);
        bindKey(KeyEvent.VK_Z, "undo");
        bindKey(KeyEvent.VK_Y, "redo");
        frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        frame.setLocationRelativeTo(null);
        screens.show(screenPanel, SIGN_IN);
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PortalTheme.ink());
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 3, 0, PortalTheme.moss()),
                PortalTheme.padding(PortalTheme.gap() / 2)));
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, PortalTheme.gap(), 0));
        brand.setOpaque(false);
        brand.add(new JLabel(new UniCrest(HEADER_CREST)));
        brand.add(PortalTheme.label("UNI", PortalTheme.brandFont(), PortalTheme.stone()));
        brand.add(PortalTheme.label("Campus Room Booking", PortalTheme.headingFont().deriveFont(Font.PLAIN),
                PortalTheme.frost()));
        JPanel account = new JPanel(new FlowLayout(FlowLayout.RIGHT, PortalTheme.gap() / 2, PortalTheme.gap() / 2));
        account.setOpaque(false);
        account.add(userLabel);
        account.add(undoButton);
        account.add(redoButton);
        account.add(PortalTheme.danger("Sign out", () -> run("logout")));
        header.add(brand, BorderLayout.WEST);
        header.add(account, BorderLayout.EAST);
        return header;
    }

    private JComponent buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(PortalTheme.ink());
        sidebar.setBorder(PortalTheme.padding(PortalTheme.gap() / 2));
        sidebar.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));
        sidebar.add(PortalTheme.label("  MENU", PortalTheme.bodyFont(), PortalTheme.slate()));
        sidebar.add(Box.createVerticalStrut(PortalTheme.gap() / 2));
        for (NavButton nav : navByName.values()) {
            sidebar.add(nav);
        }
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(PortalTheme.label("  UNI · Winter term", PortalTheme.bodyFont(), PortalTheme.slate()));
        return sidebar;
    }

    private JComponent buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(PortalTheme.paper());
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, PortalTheme.frost()),
                PortalTheme.padding(PortalTheme.gap() / 2)));
        bar.add(status, BorderLayout.CENTER);
        bar.add(PortalTheme.label("Every action is recorded in the session log files.", PortalTheme.bodyFont(),
                PortalTheme.slate()), BorderLayout.EAST);
        return bar;
    }

    private JComponent buildSignIn() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(PortalTheme.paper());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(4, 0, 0, 0, PortalTheme.moss()),
                PortalTheme.padding(PortalTheme.pageGap() + PortalTheme.gap())));
        card.setPreferredSize(new Dimension(SIGN_IN_WIDTH, SIGN_IN_WIDTH + SIGN_IN_CREST));
        JLabel crest = new JLabel(new UniCrest(SIGN_IN_CREST));
        JLabel name = PortalTheme.label("UNI", PortalTheme.brandFont().deriveFont((float) PortalTheme.pageGap() * 2),
                PortalTheme.ink());
        JLabel tagline = PortalTheme.label("Campus Room Booking", PortalTheme.headingFont().deriveFont(Font.PLAIN,
                TAGLINE_SIZE), PortalTheme.slate());
        JLabel prompt = PortalTheme.label("Sign in with your user ID", PortalTheme.bodyFont(), PortalTheme.ink());
        signInId.setFont(PortalTheme.headingFont().deriveFont(Font.PLAIN));
        signInId.setMaximumSize(new Dimension(Integer.MAX_VALUE, SIGN_IN_FIELD_HEIGHT));
        signInId.setHorizontalAlignment(SwingConstants.CENTER);
        signInId.addActionListener(event -> signIn());
        JButton button = PortalTheme.primary("Sign in", this::signIn);
        JLabel hint = PortalTheme.label("Superadmin ID: sd00000000", PortalTheme.bodyFont(), PortalTheme.slate());
        for (JComponent part : new JComponent[]{crest, name, tagline, prompt, signInId, button, signInError, hint}) {
            part.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        }
        card.add(crest);
        card.add(name);
        card.add(tagline);
        card.add(Box.createVerticalStrut(PortalTheme.pageGap()));
        card.add(prompt);
        card.add(Box.createVerticalStrut(PortalTheme.gap() / 2));
        card.add(signInId);
        card.add(Box.createVerticalStrut(PortalTheme.gap()));
        card.add(button);
        card.add(Box.createVerticalStrut(PortalTheme.gap() / 2));
        card.add(signInError);
        card.add(Box.createVerticalGlue());
        card.add(hint);
        JPanel background = new FrostedBackground();
        background.setLayout(new GridBagLayout());
        background.add(card);
        return background;
    }

    private void signIn() {
        PortalSession.Result result = run("login " + signInId.getText().trim());
        signInError.setText(result.isSuccess() ? " " : result.getMessage());
    }

    private PortalSession.Result run(String command) {
        return session.run(command);
    }

    private void afterCommand(PortalSession.Result result) {
        if (session.isClosed()) {
            frame.dispose();
            return;
        }
        status.setForeground(result.isSuccess() ? PortalTheme.moss() : PortalTheme.berry());
        status.setText((result.isSuccess() ? "✔ " : "✖ ") + result.getMessage());
        Optional<User> user = session.model().getCurrentUser();
        if (user.isPresent()) {
            userLabel.setText(user.get().getName() + " · " + user.get().getUserId() + " · " + user.get().getRole());
            navByName.get(USERS).setVisible(user.get().getRole().toString().contains("admin"));
            if (!navByName.get(currentPage).isVisible()) {
                currentPage = DASHBOARD;
            }
            if (result.getCommand().trim().startsWith("login")) {
                currentPage = DASHBOARD;
                signInId.setText("");
            }
            screens.show(screenPanel, PORTAL);
        } else {
            screens.show(screenPanel, SIGN_IN);
        }
        undoButton.setEnabled(session.model().canUndo());
        redoButton.setEnabled(session.model().canRedo());
        for (PortalPage page : pageByName.values()) {
            page.refresh();
        }
        showPage(currentPage);
    }

    private void navigate(String name) {
        currentPage = name;
        pageByName.get(name).refresh();
        showPage(name);
    }

    private void showPage(String name) {
        pages.show(pagePanel, name);
        for (Map.Entry<String, NavButton> entry : navByName.entrySet()) {
            entry.getValue().setSelected(entry.getKey().equals(name));
        }
    }

    private void bindKey(int key, String command) {
        for (int modifier : new int[]{InputEvent.CTRL_DOWN_MASK, InputEvent.META_DOWN_MASK}) {
            frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(key, modifier), command);
        }
        frame.getRootPane().getActionMap().put(command, new CommandAction(() -> {
            if (session.model().getCurrentUser().isPresent()) {
                run(command);
            }
        }));
    }

    /** A keyboard action that runs a task. */
    private static final class CommandAction extends AbstractAction {

        private static final long serialVersionUID = 1L;
        private final transient Runnable task;

        /**
         * Creates the action.
         *
         * @param task what the action does
         */
        CommandAction(Runnable task) {
            this.task = task;
        }

        /**
         * Runs the task.
         *
         * @param event the key event
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            task.run();
        }
    }

    /** A sidebar entry that is highlighted when its page is shown. */
    private static final class NavButton extends JButton {

        private static final long serialVersionUID = 1L;

        /**
         * Creates a sidebar entry.
         *
         * @param text the page name
         * @param action opens the page
         */
        NavButton(String text, Runnable action) {
            super("   " + text);
            setFont(PortalTheme.bodyFont().deriveFont(Font.BOLD));
            setHorizontalAlignment(SwingConstants.LEFT);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, NAV_HEIGHT));
            setAlignmentX(LEFT_ALIGNMENT);
            addActionListener(event -> action.run());
        }

        /**
         * Paints the highlight behind the selected entry.
         *
         * @param graphics the graphics context
         */
        @Override
        protected void paintComponent(Graphics graphics) {
            setForeground(isSelected() ? PortalTheme.ink() : PortalTheme.stone());
            if (isSelected()) {
                graphics.setColor(PortalTheme.frost());
                graphics.fillRect(0, 2, getWidth(), getHeight() - 4);
                graphics.setColor(PortalTheme.moss());
                graphics.fillRect(0, 2, 4, getHeight() - 4);
            }
            super.paintComponent(graphics);
        }
    }

    /** The sign-in background: stone with soft frost-coloured circles, like snow. */
    private static final class FrostedBackground extends JPanel {

        private static final long serialVersionUID = 1L;
        private static final double[] FLAKES = {0.08, 0.15, 0.10, 0.85, 0.22, 0.06, 0.92, 0.20, 0.12,
            0.78, 0.80, 0.16, 0.30, 0.70, 0.05, 0.65, 0.10, 0.07};

        /**
         * Paints the background.
         *
         * @param graphics the graphics context
         */
        @Override
        protected void paintComponent(Graphics graphics) {
            graphics.setColor(PortalTheme.stone());
            graphics.fillRect(0, 0, getWidth(), getHeight());
            Color frost = PortalTheme.frost();
            graphics.setColor(new Color(frost.getRed(), frost.getGreen(), frost.getBlue(), PortalTheme.pageGap() * 3));
            int unit = Math.min(getWidth(), getHeight());
            for (int i = 0; i + 2 < FLAKES.length; i += 3) {
                int r = (int) (FLAKES[i + 2] * unit);
                graphics.fillOval((int) (FLAKES[i] * getWidth()) - r, (int) (FLAKES[i + 1] * getHeight()) - r,
                        2 * r, 2 * r);
            }
        }
    }
}
