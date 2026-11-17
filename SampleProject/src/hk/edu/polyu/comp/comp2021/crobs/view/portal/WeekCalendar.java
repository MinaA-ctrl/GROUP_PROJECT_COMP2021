package hk.edu.polyu.comp.comp2021.crobs.view.portal;

import hk.edu.polyu.comp.comp2021.crobs.model.ScheduleEntry;

import javax.swing.JComponent;
import javax.swing.Scrollable;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * A seven-day calendar grid that draws the bookings of one room as blocks, like a timetable.
 * Owners the viewer may not see are shown as "hidden" in slate blocks, exactly as the model
 * reports them; bookings with a visible owner are moss green.
 */
final class WeekCalendar extends JComponent implements Scrollable {

    private static final long serialVersionUID = 1L;
    private static final int FIRST_HOUR = 7;
    private static final int LAST_HOUR = 22;
    private static final int HEADER_HEIGHT = 44;
    private static final int GUTTER_WIDTH = 54;
    private static final int ROW_HEIGHT = 42;
    private static final int PREFERRED_WIDTH = 760;
    private static final int BLOCK_ARC = 10;
    private static final int MINUTES_PER_HOUR = 60;
    private static final int TEXT_LINE = 15;
    private static final int DAYS = 7;
    private static final DateTimeFormatter DAY_NAME = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);
    private static final DateTimeFormatter DAY_DATE = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
    private static final Color TODAY = new Color(0xE3EAED);

    private LocalDate weekStart = LocalDate.now();
    private List<ScheduleEntry> entries = Collections.emptyList();
    private boolean loaded;

    /** Creates an empty calendar. */
    WeekCalendar() {
        setPreferredSize(new Dimension(PREFERRED_WIDTH, HEADER_HEIGHT + (LAST_HOUR - FIRST_HOUR) * ROW_HEIGHT));
        setFont(PortalTheme.bodyFont());
    }

    /**
     * Shows the bookings of one week.
     *
     * @param firstDay the first of the seven days
     * @param bookings the bookings of the room in that week
     */
    void show(LocalDate firstDay, List<ScheduleEntry> bookings) {
        weekStart = firstDay;
        entries = new ArrayList<>(bookings);
        loaded = true;
        repaint();
    }

    /** Removes all bookings and shows the empty-state message. */
    void clear() {
        entries = Collections.emptyList();
        loaded = false;
        repaint();
    }

    /**
     * Draws the grid and the booking blocks.
     *
     * @param graphics the graphics context
     */
    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(PortalTheme.paper());
        g.fillRect(0, 0, getWidth(), getHeight());
        double columnWidth = (getWidth() - GUTTER_WIDTH) / (double) DAYS;
        double rowHeight = (getHeight() - HEADER_HEIGHT) / (double) (LAST_HOUR - FIRST_HOUR);
        drawGrid(g, columnWidth, rowHeight);
        if (loaded) {
            for (ScheduleEntry entry : entries) {
                drawBlock(g, entry, columnWidth, rowHeight);
            }
            if (entries.isEmpty()) {
                drawMessage(g, "No bookings for this room in this week.");
            }
        } else {
            drawMessage(g, "Choose a room and click Show schedule.");
        }
        g.dispose();
    }

    private void drawGrid(Graphics2D g, double columnWidth, double rowHeight) {
        LocalDate today = LocalDate.now();
        for (int day = 0; day < DAYS; day++) {
            int x = (int) (GUTTER_WIDTH + day * columnWidth);
            LocalDate date = weekStart.plusDays(day);
            if (date.equals(today)) {
                g.setColor(TODAY);
                g.fillRect(x, 0, (int) columnWidth, getHeight());
            }
            g.setColor(PortalTheme.ink());
            g.setFont(PortalTheme.bodyFont().deriveFont(Font.BOLD));
            g.drawString(date.format(DAY_NAME), x + 8, TEXT_LINE + 4);
            g.setFont(PortalTheme.bodyFont());
            g.setColor(PortalTheme.slate());
            g.drawString(date.format(DAY_DATE), x + 8, 2 * TEXT_LINE + 6);
            g.setColor(PortalTheme.stripe());
            g.drawLine(x, 0, x, getHeight());
        }
        for (int hour = FIRST_HOUR; hour <= LAST_HOUR; hour++) {
            int y = (int) (HEADER_HEIGHT + (hour - FIRST_HOUR) * rowHeight);
            g.setColor(PortalTheme.stripe());
            g.drawLine(GUTTER_WIDTH, y, getWidth(), y);
            g.setColor(PortalTheme.slate());
            g.drawString(LocalTime.of(hour, 0).toString(), 8, y + TEXT_LINE);
        }
        g.setColor(PortalTheme.frost());
        g.drawLine(0, HEADER_HEIGHT, getWidth(), HEADER_HEIGHT);
    }

    private void drawBlock(Graphics2D g, ScheduleEntry entry, double columnWidth, double rowHeight) {
        long day = ChronoUnit.DAYS.between(weekStart, entry.getDate());
        if (day < 0 || day >= DAYS) {
            return;
        }
        double top = clampToDay(entry.getStartTime());
        double bottom = clampToDay(entry.getEndTime());
        int x = (int) (GUTTER_WIDTH + day * columnWidth) + 3;
        int y = (int) (HEADER_HEIGHT + top * rowHeight) + 1;
        int width = (int) columnWidth - 6;
        int height = Math.max((int) ((bottom - top) * rowHeight) - 2, TEXT_LINE + 4);
        boolean visibleOwner = entry.getOwnerUserId() != null;
        g.setColor(visibleOwner ? PortalTheme.moss() : PortalTheme.slate());
        g.fillRoundRect(x, y, width, height, BLOCK_ARC, BLOCK_ARC);
        g.setColor(Color.WHITE);
        int textWidth = width - 8;
        Graphics2D text = (Graphics2D) g.create(x + 6, y, textWidth, height);
        text.setFont(PortalTheme.bodyFont().deriveFont(Font.BOLD));
        text.drawString(fit(text, entry.getStartTime() + "–" + entry.getEndTime(), textWidth), 0, TEXT_LINE);
        text.setFont(PortalTheme.bodyFont());
        if (height >= 2 * TEXT_LINE + 4) {
            text.drawString(fit(text, entry.getPurpose(), textWidth), 0, 2 * TEXT_LINE);
        }
        if (height >= 3 * TEXT_LINE + 4) {
            String owner = visibleOwner ? entry.getOwnerUserId() : "hidden";
            text.drawString(fit(text, entry.getBookingId() + " · " + owner, textWidth), 0, 3 * TEXT_LINE);
        }
        text.dispose();
    }

    private static String fit(Graphics2D g, String text, int width) {
        FontMetrics metrics = g.getFontMetrics();
        if (metrics.stringWidth(text) <= width) {
            return text;
        }
        String shortened = text;
        while (!shortened.isEmpty() && metrics.stringWidth(shortened + "…") > width) {
            shortened = shortened.substring(0, shortened.length() - 1);
        }
        return shortened + "…";
    }

    /**
     * Returns the size of the visible area when the calendar is scrolled.
     *
     * @return the preferred size
     */
    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    /**
     * Scrolls by half an hour.
     *
     * @param visibleRect the visible area
     * @param orientation the scroll direction
     * @param direction up or down
     * @return the scroll distance in pixels
     */
    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return ROW_HEIGHT / 2;
    }

    /**
     * Scrolls by one visible page.
     *
     * @param visibleRect the visible area
     * @param orientation the scroll direction
     * @param direction up or down
     * @return the scroll distance in pixels
     */
    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return visibleRect.height;
    }

    /**
     * Makes the calendar as wide as the visible area.
     *
     * @return always true
     */
    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    /**
     * Stretches the calendar vertically only when there is more room than it needs.
     *
     * @return true if the visible area is taller than the calendar
     */
    @Override
    public boolean getScrollableTracksViewportHeight() {
        return getParent() != null && getParent().getHeight() > getPreferredSize().height;
    }

    private static double clampToDay(LocalTime time) {
        double hours = time.getHour() + time.getMinute() / (double) MINUTES_PER_HOUR - FIRST_HOUR;
        return Math.max(0, Math.min(LAST_HOUR - FIRST_HOUR, hours));
    }

    private void drawMessage(Graphics2D g, String message) {
        g.setColor(PortalTheme.slate());
        g.setFont(PortalTheme.headingFont());
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(message, (getWidth() - metrics.stringWidth(message)) / 2, getHeight() / 2);
    }
}
