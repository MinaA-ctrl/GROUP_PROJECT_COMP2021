package hk.edu.polyu.comp.comp2021.crobs.view.portal;

import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import hk.edu.polyu.comp.comp2021.crobs.model.User;
import hk.edu.polyu.comp.comp2021.crobs.model.UserType;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.Optional;

/**
 * Base class of the portal pages. A page has a title, a short description and content,
 * and refreshes itself from the model after every command.
 */
abstract class PortalPage extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient PortalSession session;

    /**
     * Creates a page with a title bar.
     *
     * @param session the session used to run commands and read the model
     * @param title the page title
     * @param description a one-line explanation shown under the title
     */
    PortalPage(PortalSession session, String title, String description) {
        super(new BorderLayout(0, PortalTheme.gap()));
        this.session = session;
        setBackground(PortalTheme.stone());
        setBorder(PortalTheme.padding(PortalTheme.pageGap()));
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.add(PortalTheme.label(title, PortalTheme.titleFont(), PortalTheme.ink()));
        heading.add(PortalTheme.label(description, PortalTheme.bodyFont(), PortalTheme.slate()));
        heading.setBorder(BorderFactory.createEmptyBorder(0, 0, PortalTheme.gap(), 0));
        add(heading, BorderLayout.NORTH);
    }

    /**
     * Returns the session of the portal.
     *
     * @return the session
     */
    protected PortalSession session() {
        return session;
    }

    /**
     * Returns the model.
     *
     * @return the model
     */
    protected CROBS model() {
        return session.model();
    }

    /**
     * Returns the logged-in user.
     *
     * @return the user, or an empty optional when nobody is logged in
     */
    protected Optional<User> currentUser() {
        return model().getCurrentUser();
    }

    /**
     * Tells whether the logged-in user is an admin or the superadmin.
     *
     * @return true for admins
     */
    protected boolean isAdmin() {
        return currentUser().map(user -> user.getRole() != UserType.REGULAR).orElse(false);
    }

    /**
     * Runs a command through the session.
     *
     * @param command the command text
     * @return the result
     */
    protected PortalSession.Result run(String command) {
        return session.run(command);
    }

    /**
     * Places the main content of the page.
     *
     * @param content the content
     */
    protected void setContent(JComponent content) {
        add(content, BorderLayout.CENTER);
    }

    /** Re-reads the model and updates the page. Called after every command and on navigation. */
    abstract void refresh();
}
