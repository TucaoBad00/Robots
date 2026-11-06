package steering.engine;

import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.List;

/**
 * Câmera isométrica.
 * <p>
 * O mundo continua 2D (x, y no chão), exatamente como na engine original: toda a física
 * e todos os steering behaviors trabalham em coordenadas de mundo. Esta classe só converte
 * mundo → tela na hora de desenhar (e tela → mundo para o mouse).
 * <p>
 * Projeção: gira o chão 45° e achata o eixo vertical pela metade (isometria 2:1).
 * O eixo z representa a altura acima do chão.
 */
public final class Iso {
    private static final double INV_SQRT2 = 1.0 / Math.sqrt(2.0);

    private final double originX;
    private final double originY;
    private final double scale;

    Iso(double originX, double originY, double scale) {
        this.originX = originX;
        this.originY = originY;
        this.scale = scale;
    }

    /** Pixels de tela por unidade de mundo. */
    public double getScale() {
        return scale;
    }

    public Point2D.Double toScreen(double x, double y, double z) {
        final var sx = (x - y) * INV_SQRT2 * scale + originX;
        final var sy = (x + y) * INV_SQRT2 * 0.5 * scale - z * scale + originY;
        return new Point2D.Double(sx, sy);
    }

    public Point2D.Double toScreen(Vector2 p, double z) {
        return toScreen(p.x, p.y, z);
    }

    public Point2D.Double toScreen(Vector2 p) {
        return toScreen(p.x, p.y, 0);
    }

    /** Converte um ponto da tela para o ponto do chão (z = 0) correspondente. */
    public Vector2 toWorld(double screenX, double screenY) {
        final var a = (screenX - originX) / scale;        // (x - y) / √2
        final var b = (screenY - originY) / scale * 2.0;  // (x + y) / √2
        return new Vector2((a + b) * INV_SQRT2, (b - a) * INV_SQRT2);
    }

    /** Um círculo no chão vira uma elipse 2:1 na tela. */
    public Ellipse2D.Double groundCircle(Vector2 center, double radius) {
        final var c = toScreen(center, 0);
        final var rx = radius * scale;
        final var ry = rx / 2.0;
        return new Ellipse2D.Double(c.x - rx, c.y - ry, rx * 2, ry * 2);
    }

    public Path2D.Double groundPolygon(Vector2... points) {
        return groundPolyline(List.of(points), true);
    }

    public Path2D.Double groundPolyline(List<Vector2> points, boolean closed) {
        final var path = new Path2D.Double();
        for (int i = 0; i < points.size(); i++) {
            final var p = toScreen(points.get(i), 0);
            if (i == 0) path.moveTo(p.x, p.y);
            else path.lineTo(p.x, p.y);
        }
        if (closed) path.closePath();
        return path;
    }

    /** Profundidade para ordenar o desenho: quem tem x + y maior está mais perto da câmera. */
    public static double depth(Vector2 p) {
        return p.x + p.y;
    }
}
