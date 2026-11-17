package hk.edu.polyu.comp.comp2021.crobs.view.portal;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Colours, fonts and small styling helpers of the UNI portal. The palette is a frosted
 * winter theme: ink, slate, frost, stone and moss.
 */
final class PortalTheme {

    private static final Color INK = new Color(0x141B24);
    private static final Color SLATE = new Color(0x4E5A56);
    private static final Color FROST = new Color(0x9DB2BC);
    private static final Color STONE = new Color(0xD6D1C9);
    private static final Color MOSS = new Color(0x55703A);
    private static final Color BERRY = new Color(0x9E3B3B);
    private static final Color PAPER = new Color(0xFBFAF7);
    private static final Color STRIPE = new Color(0xEFEDE8);

    private static final Font BRAND_FONT = new Font(Font.SERIF, Font.BOLD, 26);
    private static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 22);
    private static final Font HEADING_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 15);
    private static final Font BODY_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    private static final Font STAT_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 30);
    private static final Font MONO_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    private static final int GAP = 12;
    private static final int PAGE_GAP = 24;

    private static final int ROW_HEIGHT = 26;
    private static final int BUTTON_ARC = 14;
    private static final int BUTTON_HEIGHT = 32;
    private static final int BUTTON_PADDING = 18;

    private PortalTheme() {
    }

    /**
     * Returns the very dark blue-black for the header, the sidebar and titles.
     *
     * @return very dark blue-black for the header, the sidebar and titles
     */
    static Color ink() {
        return INK;
    }

    /**
     * Returns the grey-green for secondary text and borders.
     *
     * @return grey-green for secondary text and borders
     */
    static Color slate() {
        return SLATE;
    }

    /**
     * Returns the frosted blue-grey for highlights and table headers.
     *
     * @return frosted blue-grey for highlights and table headers
     */
    static Color frost() {
        return FROST;
    }

    /**
     * Returns the warm light grey for the page background.
     *
     * @return warm light grey for the page background
     */
    static Color stone() {
        return STONE;
    }

    /**
     * Returns the dark moss green for primary actions and success messages.
     *
     * @return dark moss green for primary actions and success messages
     */
    static Color moss() {
        return MOSS;
    }

    /**
     * Returns the muted red for errors and destructive actions.
     *
     * @return muted red for errors and destructive actions
     */
    static Color berry() {
        return BERRY;
    }

    /**
     * Returns the card and input background.
     *
     * @return card and input background
     */
    static Color paper() {
        return PAPER;
    }

    /**
     * Returns the second colour of striped table rows.
     *
     * @return second colour of striped table rows
     */
    static Color stripe() {
        return STRIPE;
    }

    /**
     * Returns the brand font for the UNI name.
     *
     * @return brand font for the UNI name
     */
    static Font brandFont() {
        return BRAND_FONT;
    }

    /**
     * Returns the page title font.
     *
     * @return page title font
     */
    static Font titleFont() {
        return TITLE_FONT;
    }

    /**
     * Returns the section heading font.
     *
     * @return section heading font
     */
    static Font headingFont() {
        return HEADING_FONT;
    }

    /**
     * Returns the normal text font.
     *
     * @return normal text font
     */
    static Font bodyFont() {
        return BODY_FONT;
    }

    /**
     * Returns the large number font used on dashboard cards.
     *
     * @return large number font used on dashboard cards
     */
    static Font statFont() {
        return STAT_FONT;
    }

    /**
     * Returns the monospaced font for the activity log.
     *
     * @return monospaced font for the activity log
     */
    static Font monoFont() {
        return MONO_FONT;
    }

    /**
     * Returns the standard gap between components, in pixels.
     *
     * @return standard gap between components, in pixels
     */
    static int gap() {
        return GAP;
    }

    /**
     * Returns the large gap around pages, in pixels.
     *
     * @return large gap around pages, in pixels
     */
    static int pageGap() {
        return PAGE_GAP;
    }

    /**
     * Creates an empty border of the same size on every side.
     *
     * @param size the border width in pixels
     * @return the border
     */
    static Border padding(int size) {
        return BorderFactory.createEmptyBorder(size, size, size, size);
    }

    /**
     * Creates a filled, rounded button.
     *
     * @param text the button label
     * @param fill the background colour
     * @param action what happens when the button is clicked
     * @return the button
     */
    static JButton button(String text, Color fill, Runnable action) {
        JButton button = new RoundButton(text, fill);
        button.addActionListener(event -> action.run());
        return button;
    }

    /**
     * Creates a moss-green button for the main action of a page.
     *
     * @param text the button label
     * @param action what happens when the button is clicked
     * @return the button
     */
    static JButton primary(String text, Runnable action) {
        return button(text, MOSS, action);
    }

    /**
     * Creates a slate button for secondary actions.
     *
     * @param text the button label
     * @param action what happens when the button is clicked
     * @return the button
     */
    static JButton secondary(String text, Runnable action) {
        return button(text, SLATE, action);
    }

    /**
     * Creates a red button for destructive actions.
     *
     * @param text the button label
     * @param action what happens when the button is clicked
     * @return the button
     */
    static JButton danger(String text, Runnable action) {
        return button(text, BERRY, action);
    }

    /**
     * Creates a label with the given font and colour.
     *
     * @param text the text
     * @param font the font
     * @param color the text colour
     * @return the label
     */
    static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    /**
     * Creates a white card with a thin border, used to group content.
     *
     * @param content the content of the card
     * @return the card
     */
    static JPanel card(JComponent content) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(PAPER);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(FROST), padding(GAP)));
        content.setOpaque(false);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    /**
     * Creates a styled, read-only table with striped rows.
     *
     * @param rows the table data
     * @return the table
     */
    static JTable table(DefaultTableModel rows) {
        JTable table = new JTable(rows);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setRowHeight(ROW_HEIGHT);
        table.setFont(BODY_FONT);
        table.setShowVerticalLines(false);
        table.setGridColor(STRIPE);
        table.setBackground(PAPER);
        table.setSelectionBackground(FROST);
        table.setSelectionForeground(INK);
        table.setDefaultRenderer(Object.class, new StripedRenderer());
        table.getTableHeader().setDefaultRenderer(new HeaderRenderer());
        table.getTableHeader().setReorderingAllowed(false);
        return table;
    }

    /** A table model whose cells cannot be edited directly. */
    static final class ReadOnlyTableModel extends DefaultTableModel {

        private static final long serialVersionUID = 1L;

        /**
         * Creates an empty table with the given columns.
         *
         * @param columns the column titles
         */
        ReadOnlyTableModel(String... columns) {
            super(columns, 0);
        }

        /**
         * Prevents editing; all changes go through commands.
         *
         * @param row the row index
         * @param column the column index
         * @return always false
         */
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }

    /** Paints alternating row colours. */
    private static final class StripedRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        /**
         * Returns the component that paints one cell.
         *
         * @param table the table
         * @param value the cell value
         * @param selected whether the row is selected
         * @param focused whether the cell has the focus
         * @param row the row index
         * @param column the column index
         * @return this renderer, configured for the cell
         */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, false, row, column);
            setBorder(padding(4));
            if (!selected) {
                setBackground(row % 2 == 0 ? PAPER : STRIPE);
                setForeground(INK);
            }
            return this;
        }
    }

    /** Paints table headers in the frost colour. */
    private static final class HeaderRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        /**
         * Returns the component that paints one cell.
         *
         * @param table the table
         * @param value the cell value
         * @param selected whether the row is selected
         * @param focused whether the cell has the focus
         * @param row the row index
         * @param column the column index
         * @return this renderer, configured for the cell
         */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, false, false, row, column);
            setBackground(FROST);
            setForeground(INK);
            setFont(BODY_FONT.deriveFont(Font.BOLD));
            setBorder(padding(4));
            return this;
        }
    }

    /** A button that paints itself as a filled rounded rectangle on every look and feel. */
    private static final class RoundButton extends JButton {

        private static final long serialVersionUID = 1L;
        private final Color fill;

        /**
         * Creates the button.
         *
         * @param text the label
         * @param fill the background colour
         */
        RoundButton(String text, Color fill) {
            super(text);
            this.fill = fill;
            setFont(BODY_FONT.deriveFont(Font.BOLD));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        /**
         * Returns the size: the label plus padding, at a fixed height.
         *
         * @return the preferred size
         */
        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            return new Dimension(size.width + BUTTON_PADDING, BUTTON_HEIGHT);
        }

        /**
         * Paints the rounded background, then the label.
         *
         * @param graphics the graphics context
         */
        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color base = isEnabled() ? fill : FROST;
            g.setColor(getModel().isPressed() ? base.darker() : base);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), BUTTON_ARC, BUTTON_ARC);
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
