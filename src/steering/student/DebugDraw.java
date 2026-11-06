package steering.student;

import steering.engine.Robot;
import steering.engine.Iso;
import steering.engine.Vector2;

import java.awt.*;
import java.awt.geom.Line2D;

import static steering.engine.Vector2.*;

/** Utilitários para desenhar informação de debug no chão (com a tecla D). */
public final class DebugDraw {
    private static final Stroke THIN = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final Stroke DASHED = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
        1f, new float[]{5f, 5f}, 0f);

    private DebugDraw() {
    }

    public static void ring(Graphics2D g, Iso iso, Vector2 center, double radius, Color color) {
        g.setColor(color);
        g.setStroke(DASHED);
        g.draw(iso.groundCircle(center, radius));
    }

    public static void dot(Graphics2D g, Iso iso, Vector2 center, double radius, Color color) {
        g.setColor(color);
        g.fill(iso.groundCircle(center, radius));
    }

    public static void line(Graphics2D g, Iso iso, Vector2 a, Vector2 b, Color color) {
        g.setColor(color);
        g.setStroke(THIN);
        g.draw(new Line2D.Double(iso.toScreen(a), iso.toScreen(b)));
    }

    /** Caixa de detecção do obstacle avoidance. */
    public static void detectionBox(Graphics2D g, Iso iso, Robot robot, double length, Color color) {
        final var pos = robot.getPosition();
        final var forward = robot.getDirection();
        final var side = multiply(perp(forward), robot.getRadius());
        final var front = add(pos, multiply(forward, length));
        g.setColor(color);
        g.setStroke(DASHED);
        g.draw(iso.groundPolygon(add(pos, side), add(front, side), subtract(front, side), subtract(pos, side)));
    }

    public static void wander(Graphics2D g, Iso iso, Robot robot, Wander wander) {
        if (wander.getLastCenter() == null) return;
        final var color = new Color(120, 90, 200, 170);
        line(g, iso, robot.getPosition(), wander.getLastCenter(), color);
        ring(g, iso, wander.getLastCenter(), wander.getRadius(), color);
        dot(g, iso, wander.getLastTarget(), 4, new Color(230, 70, 150));
    }
}
