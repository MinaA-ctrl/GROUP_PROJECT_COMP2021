package hk.edu.polyu.comp.comp2021.crobs.view.portal;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.QuadCurve2D;
import java.awt.image.BufferedImage;

/**
 * The crest of the fictional university "UNI": an ink shield with a moss border, the letters
 * UNI and a frosted sprig with three berries. It is drawn with Java2D, so it is sharp at any size.
 */
final class UniCrest implements Icon {

    /** Shield outline as x/y pairs in units of the icon size: top, right shoulder, right side. */
    private static final double[] SHIELD = {0.50, 0.03, 0.93, 0.13, 0.91, 0.52};
    /** Control and end points of the curves to the bottom tip and back up the left side. */
    private static final double[] SHIELD_CURVES = {0.86, 0.80, 0.50, 0.97, 0.14, 0.80, 0.09, 0.52};
    private static final double LEFT_SHOULDER_X = 0.07;
    private static final double SHOULDER_Y = 0.13;
    private static final double BORDER_WIDTH = 0.055;
    private static final double INNER_INSET = 0.92;
    private static final double LETTER_SIZE = 0.30;
    private static final double LETTER_BASELINE = 0.52;
    private static final double[] STEM = {0.27, 0.79, 0.50, 0.66, 0.74, 0.72};
    private static final double[] BERRIES = {0.42, 0.74, 0.52, 0.80, 0.61, 0.73};
    private static final double BERRY_RADIUS = 0.048;
    private static final double[] LEAVES = {0.30, 0.72, 0.66, 0.62};
    private static final double LEAF_WIDTH = 0.12;
    private static final double LEAF_HEIGHT = 0.06;
    private static final double STEM_WIDTH = 0.018;
    private static final double SHINE = 0.35;

    private final int size;

    /**
     * Creates a crest of the given size.
     *
     * @param size the width and height in pixels
     */
    UniCrest(int size) {
        this.size = size;
    }

    /**
     * Returns the width of the crest.
     *
     * @return the size in pixels
     */
    @Override
    public int getIconWidth() {
        return size;
    }

    /**
     * Returns the height of the crest.
     *
     * @return the size in pixels
     */
    @Override
    public int getIconHeight() {
        return size;
    }

    /**
     * Draws the crest.
     *
     * @param component the component being painted
     * @param graphics the graphics context
     * @param x the left edge
     * @param y the top edge
     */
    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.translate(x, y);
        GeneralPath shield = shield(size);
        g.setColor(PortalTheme.ink());
        g.fill(shield);
        g.setColor(PortalTheme.moss());
        g.setStroke(new BasicStroke((float) (size * BORDER_WIDTH)));
        g.draw(shield);
        drawInnerLine(g);
        drawLetters(g);
        drawSprig(g);
        g.dispose();
    }

    /**
     * Renders the crest into an image, e.g. for the window icon.
     *
     * @return a new image of the crest
     */
    Image toImage() {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        paintIcon(null, g, 0, 0);
        g.dispose();
        return image;
    }

    private static GeneralPath shield(double s) {
        GeneralPath path = new GeneralPath();
        path.moveTo(SHIELD[0] * s, SHIELD[1] * s);
        path.lineTo(SHIELD[2] * s, SHIELD[3] * s);
        path.lineTo(SHIELD[4] * s, SHIELD[5] * s);
        path.quadTo(SHIELD_CURVES[0] * s, SHIELD_CURVES[1] * s, SHIELD_CURVES[2] * s, SHIELD_CURVES[3] * s);
        path.quadTo(SHIELD_CURVES[4] * s, SHIELD_CURVES[5] * s, SHIELD_CURVES[6] * s, SHIELD_CURVES[7] * s);
        path.lineTo(LEFT_SHOULDER_X * s, SHOULDER_Y * s);
        path.closePath();
        return path;
    }

    private void drawInnerLine(Graphics2D g) {
        Graphics2D inner = (Graphics2D) g.create();
        double offset = size * (1 - INNER_INSET) / 2;
        inner.translate(offset, offset);
        inner.setColor(PortalTheme.slate());
        inner.setStroke(new BasicStroke(1f));
        inner.draw(shield(size * INNER_INSET));
        inner.dispose();
    }

    private void drawLetters(Graphics2D g) {
        g.setFont(new Font(Font.SERIF, Font.BOLD, (int) Math.round(size * LETTER_SIZE)));
        FontMetrics metrics = g.getFontMetrics();
        String text = "UNI";
        g.setColor(PortalTheme.stone());
        g.drawString(text, (size - metrics.stringWidth(text)) / 2, (int) Math.round(size * LETTER_BASELINE));
    }

    private void drawSprig(Graphics2D g) {
        g.setColor(PortalTheme.frost());
        g.setStroke(new BasicStroke((float) (size * STEM_WIDTH), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new QuadCurve2D.Double(STEM[0] * size, STEM[1] * size, STEM[2] * size, STEM[3] * size,
                STEM[4] * size, STEM[5] * size));
        g.setColor(PortalTheme.moss().brighter());
        for (int i = 0; i < LEAVES.length; i += 2) {
            g.fill(new Ellipse2D.Double(LEAVES[i] * size, LEAVES[i + 1] * size,
                    LEAF_WIDTH * size, LEAF_HEIGHT * size));
        }
        double r = BERRY_RADIUS * size;
        for (int i = 0; i < BERRIES.length; i += 2) {
            double cx = BERRIES[i] * size;
            double cy = BERRIES[i + 1] * size;
            g.setColor(PortalTheme.frost());
            g.fill(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
            g.setColor(new Color(1f, 1f, 1f, (float) SHINE));
            g.fill(new Ellipse2D.Double(cx - r / 2, cy - r / 2, r / 2, r / 2));
        }
    }
}
