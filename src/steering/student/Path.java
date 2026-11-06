package steering.student;

import steering.engine.Iso;
import steering.engine.Vector2;

import java.awt.*;
import java.awt.geom.Area;
import java.util.List;

import static steering.engine.Vector2.*;

/**
 * Caminho fechado (circuito) formado por pontos ligados por segmentos, com uma largura (raio).
 * Posições ao longo do caminho são medidas em "distância percorrida" a partir do primeiro ponto.
 */
public final class Path {
    /** Resultado da projeção de um ponto no caminho. */
    public record Projection(double distanceAlong, Vector2 point, double distanceToPath) {
    }

    private final List<Vector2> points;
    private final double radius;
    private final double[] cumulative; // distância acumulada até o início de cada segmento
    private final double length;

    public Path(double radius, List<Vector2> points) {
        this.radius = radius;
        this.points = List.copyOf(points);
        this.cumulative = new double[points.size()];
        double total = 0;
        for (int i = 0; i < points.size(); i++) {
            cumulative[i] = total;
            total += distance(points.get(i), next(i));
        }
        this.length = total;
    }

    private Vector2 next(int i) {
        return points.get((i + 1) % points.size());
    }

    public double getRadius() {
        return radius;
    }

    public double getLength() {
        return length;
    }

    public List<Vector2> getPoints() {
        return points;
    }

    /** Encontra o ponto do caminho mais próximo de {@code p}. */
    public Projection project(Vector2 p) {
        Projection best = null;
        for (int i = 0; i < points.size(); i++) {
            final var a = points.get(i);
            final var ab = subtract(next(i), a);
            final var segmentLength = ab.size();
            final var t = Math.clamp(subtract(p, a).dot(ab) / ab.sizeSqr(), 0.0, 1.0);
            final var closest = add(a, multiply(ab, t));
            final var d = distance(p, closest);
            if (best == null || d < best.distanceToPath()) {
                best = new Projection(cumulative[i] + t * segmentLength, closest, d);
            }
        }
        return best;
    }

    /** Ponto do caminho a uma certa distância do início (dá a volta no circuito). */
    public Vector2 pointAt(double distanceAlong) {
        final var s = ((distanceAlong % length) + length) % length;
        for (int i = points.size() - 1; i >= 0; i--) {
            if (s >= cumulative[i]) {
                final var a = points.get(i);
                final var dir = normalize(subtract(next(i), a));
                return add(a, multiply(dir, s - cumulative[i]));
            }
        }
        return points.getFirst().clone();
    }

    /** Ângulo (rad) da direção do caminho em um ponto. */
    public double angleAt(double distanceAlong) {
        final var d = subtract(pointAt(distanceAlong + 1), pointAt(distanceAlong));
        return Math.atan2(d.y, d.x);
    }

    public void draw(Graphics2D g0, Iso iso) {
        final var g = (Graphics2D) g0.create();

        final var band = new Area();
        for (int i = 0; i < points.size(); i++) {
            final var a = points.get(i);
            final var b = next(i);
            final var n = multiply(perp(normalize(subtract(b, a))), radius);
            band.add(new Area(iso.groundPolygon(add(a, n), add(b, n), subtract(b, n), subtract(a, n))));
            band.add(new Area(iso.groundCircle(a, radius)));
        }
        g.setColor(new Color(120, 110, 165, 90));
        g.fill(band);
        g.setColor(new Color(255, 255, 255, 120));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(band);

        g.setColor(new Color(255, 255, 255, 210));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{8f, 9f}, 0f));
        g.draw(iso.groundPolyline(points, true));

        g.setColor(new Color(150, 120, 220));
        for (var p : points) g.fill(iso.groundCircle(p, 4));
        g.dispose();
    }
}
