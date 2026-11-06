package steering.student;

import steering.engine.Iso;
import steering.engine.Prop;
import steering.engine.Vector2;

import java.awt.*;
import java.awt.geom.*;

import static steering.engine.Vector2.vec2;

/** Obstáculo circular no chão. Para a física, só importam posição e raio; o tipo muda o visual. */
public final class Obstacle implements Prop {
    public enum Kind { PILLAR, BUSH, CONE }

    private final Kind kind;
    private final Vector2 position;
    private final double radius;
    private final Color color;

    public Obstacle(Kind kind, double x, double y, double radius, Color color) {
        this.kind = kind;
        this.position = vec2(x, y);
        this.radius = radius;
        this.color = color;
    }

    public static Obstacle pillar(double x, double y, double radius, Color color) {
        return new Obstacle(Kind.PILLAR, x, y, radius, color);
    }

    public static Obstacle bush(double x, double y, double radius) {
        return new Obstacle(Kind.BUSH, x, y, radius, new Color(120, 200, 140));
    }

    public static Obstacle cone(double x, double y) {
        return new Obstacle(Kind.CONE, x, y, 13, new Color(255, 150, 70));
    }

    @Override
    public Vector2 getPosition() {
        return position.clone();
    }

    public double getRadius() {
        return radius;
    }

    @Override
    public void draw(Graphics2D g0, Iso iso) {
        final var g = (Graphics2D) g0.create();
        final var s = iso.getScale();
        final var base = iso.toScreen(position, 0);

        // Sombra
        g.setColor(new Color(40, 30, 70, 45));
        final var srx = radius * s * 1.15;
        g.fill(new Ellipse2D.Double(base.x - srx + 5 * s, base.y - srx / 2 + 3 * s, srx * 2, srx));

        switch (kind) {
            case PILLAR -> drawPillar(g, iso, base, s);
            case BUSH -> drawBush(g, iso, s);
            case CONE -> drawCone(g, iso, base, s);
        }
        g.dispose();
    }

    private void drawPillar(Graphics2D g, Iso iso, Point2D.Double base, double s) {
        final var top = iso.toScreen(position, 50);
        final var rx = radius * s;
        final var ry = rx / 2;

        final var side = new Area(new Rectangle2D.Double(base.x - rx, top.y, rx * 2, base.y - top.y));
        side.add(new Area(new Ellipse2D.Double(base.x - rx, base.y - ry, rx * 2, ry * 2)));
        g.setPaint(new LinearGradientPaint((float) (base.x - rx), 0f, (float) (base.x + rx), 0f,
            new float[]{0f, 0.4f, 1f},
            new Color[]{mix(color, Color.WHITE, 0.25), color, mix(color, Color.BLACK, 0.3)}));
        g.fill(side);
        g.setColor(mix(color, Color.BLACK, 0.45));
        g.setStroke(new BasicStroke(1f));
        g.draw(side);

        final var cap = new Ellipse2D.Double(top.x - rx, top.y - ry, rx * 2, ry * 2);
        g.setColor(mix(color, Color.WHITE, 0.4));
        g.fill(cap);
        g.setColor(mix(color, Color.BLACK, 0.35));
        g.draw(cap);
    }

    private void drawBush(Graphics2D g, Iso iso, double s) {
        final double[][] blobs = {
            {-0.45, 0.35, 0.48},
            {0.45, 0.38, 0.48},
            {0.0, 0.55, 0.65},
            {0.0, 1.0, 0.42},
        };
        for (var b : blobs) {
            final var c = iso.toScreen(position, radius * b[1]);
            final var r = radius * b[2] * s;
            final var cx = c.x + radius * b[0] * s;
            g.setColor(mix(color, Color.BLACK, 0.2));
            g.fill(new Ellipse2D.Double(cx - r, c.y - r, r * 2, r * 2));
            g.setColor(color);
            g.fill(new Ellipse2D.Double(cx - r * 0.85, c.y - r, r * 1.6, r * 1.6));
            g.setColor(mix(color, Color.WHITE, 0.35));
            g.fill(new Ellipse2D.Double(cx - r * 0.55, c.y - r * 0.8, r * 0.6, r * 0.45));
        }
    }

    private void drawCone(Graphics2D g, Iso iso, Point2D.Double base, double s) {
        final var plate = radius * 1.1;
        g.setColor(new Color(70, 70, 90));
        g.fill(iso.groundPolygon(
            vec2(position.x - plate, position.y - plate), vec2(position.x + plate, position.y - plate),
            vec2(position.x + plate, position.y + plate), vec2(position.x - plate, position.y + plate)));

        final var rx = radius * 0.8 * s;
        final var height = 32 * s;
        final var cone = new Path2D.Double();
        cone.moveTo(base.x - rx, base.y);
        cone.lineTo(base.x, base.y - height);
        cone.lineTo(base.x + rx, base.y);
        cone.quadTo(base.x, base.y + rx * 0.6, base.x - rx, base.y);
        g.setPaint(new LinearGradientPaint((float) (base.x - rx), 0f, (float) (base.x + rx), 0f,
            new float[]{0f, 0.4f, 1f},
            new Color[]{mix(color, Color.WHITE, 0.3), color, mix(color, Color.BLACK, 0.25)}));
        g.fill(cone);

        // Faixa branca
        final var y1 = base.y - height * 0.35;
        final var y2 = base.y - height * 0.55;
        final var w1 = rx * 0.65;
        final var w2 = rx * 0.45;
        final var stripe = new Path2D.Double();
        stripe.moveTo(base.x - w1, y1);
        stripe.lineTo(base.x - w2, y2);
        stripe.lineTo(base.x + w2, y2);
        stripe.lineTo(base.x + w1, y1);
        stripe.closePath();
        g.setColor(new Color(255, 255, 255, 230));
        g.fill(stripe);
    }

    private static Color mix(Color a, Color b, double t) {
        return new Color(
            (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
            (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
}
