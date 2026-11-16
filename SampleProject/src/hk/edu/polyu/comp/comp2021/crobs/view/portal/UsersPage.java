package hk.edu.polyu.comp.comp2021.crobs.view.portal;

import hk.edu.polyu.comp.comp2021.crobs.model.UserType;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/** Account administration: admins add and remove regular users, the superadmin changes roles. */
final class UsersPage extends PortalPage {

    private static final long serialVersionUID = 1L;

    private final JTextField name = PortalForms.textField("");
    private final JTextField removeId = PortalForms.textField("");
    private final JTextField roleId = PortalForms.textField("");
    private final JComboBox<String> role = new JComboBox<>(new String[]{"admin", "regular"});
    private final JPanel roleCard;

    /**
     * Creates the users page.
     *
     * @param session the portal session
     */
    UsersPage(PortalSession session) {
        super(session, "Users", "Create and remove accounts. Only the superadmin can change roles.");
        JPanel cards = new JPanel();
        cards.setOpaque(false);
        cards.setLayout(new BoxLayout(cards, BoxLayout.Y_AXIS));
        cards.add(section("Add a regular user", "The new user ID is shown after the account is created.",
                PortalForms.form("Full name", name), PortalTheme.primary("Add user", this::addUser)));
        cards.add(gap());
        cards.add(section("Remove a regular user", "All bookings of the user are cancelled as well.",
                PortalForms.form("User ID", removeId), PortalTheme.danger("Remove user", this::removeUser)));
        cards.add(gap());
        roleCard = section("Change a user's role", "Promote a regular user to admin, or back to regular.",
                PortalForms.form("User ID", roleId, "New role", role),
                PortalTheme.primary("Change role", this::changeRole));
        cards.add(roleCard);
        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.add(cards, BorderLayout.NORTH);
        setContent(content);
    }

    /** Shows the role card only to the superadmin. */
    @Override
    void refresh() {
        roleCard.setVisible(currentUser().map(user -> user.getRole() == UserType.SUPERADMIN).orElse(false));
    }

    private void addUser() {
        PortalSession.Result result = run("addUser " + name.getText().trim());
        if (result.isSuccess()) {
            JOptionPane.showMessageDialog(this, result.getOutput() + "\nGive this ID to " + name.getText().trim()
                    + " so they can sign in.", "Account created", JOptionPane.INFORMATION_MESSAGE,
                    new UniCrest(PortalTheme.pageGap() * 2));
            name.setText("");
        }
    }

    private void removeUser() {
        String id = removeId.getText().trim();
        if (PortalForms.confirm(this, "Remove user " + id + " and cancel all of their bookings?")) {
            if (run("removeUser " + id).isSuccess()) {
                removeId.setText("");
            }
        }
    }

    private void changeRole() {
        run("changeRole " + roleId.getText().trim() + " " + role.getSelectedItem());
    }

    private static JPanel section(String title, String hint, JComponent form, JComponent button) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, PortalTheme.gap(), 0));
        row.setOpaque(false);
        row.add(form);
        row.add(button);
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.add(PortalTheme.label(title, PortalTheme.headingFont(), PortalTheme.ink()));
        box.add(PortalTheme.label(hint, PortalTheme.bodyFont(), PortalTheme.slate()));
        box.add(row);
        for (java.awt.Component part : box.getComponents()) {
            ((JComponent) part).setAlignmentX(LEFT_ALIGNMENT);
        }
        return PortalTheme.card(box);
    }

    private static JPanel gap() {
        JPanel gap = new JPanel();
        gap.setOpaque(false);
        gap.setBorder(PortalTheme.padding(PortalTheme.gap() / 2));
        return gap;
    }
}
