package steering.engine;

import java.awt.*;
import java.awt.geom.*;

/**
 * Desenha um robozinho flutuante em perspectiva isométrica.
 * Tudo aqui é cosmético: posição e direção vêm da física do {@link Robot}.
 * Coordenadas locais em "pixels de robô": y negativo = para cima.
 */
final class RobotRenderer {
    private static final double INV_SQRT2 = 1.0 / Math.sqrt(2.0);
    /** Tamanho do desenho em relação às unidades de mundo (o raio de colisão é Robot.BASE_RADIUS). */
    private static final double VISUAL_SCALE = 1.45;
    private static final Color SCREEN = new Color(28, 32, 48);
    private static final Color EYE = new Color(120, 235, 255);
    private static final Color EYE_ANGRY = new Color(255, 96, 80);
    private static final Color CHEEK = new Color(255, 120, 150, 150);
    private static final Font ZZZ = new Font("SansSerif", Font.BOLD, 8);
    private static final Font BADGE = new Font("SansSerif", Font.BOLD, 7);

    private RobotRenderer() {
    }

    static void draw(Graphics2D g0, Iso iso, Vector2 pos, double heading, Color color, double size,
                     Mood mood, Theme theme, String badge, double t, double phase, double speed01) {
        final var g = (Graphics2D) g0.create();
        final var ground = iso.toScreen(pos, 0);
        g.translate(ground.x, ground.y);
        final var k = iso.getScale() * size * VISUAL_SCALE;
        g.scale(k, k);

        // Direção do robô projetada na tela (sem achatar): fx > 0 = direita, fy > 0 = vindo na direção da câmera
        final var dx = Math.cos(heading);
        final var dy = Math.sin(heading);
        final var fx = (dx - dy) * INV_SQRT2;
        final var fy = (dx + dy) * INV_SQRT2;
        final var front = fy > -0.25;

        final var bob = Math.sin(t * 4 + phase) * 1.6;
        final var shake = mood == Mood.SCARED ? Math.sin(t * 45 + phase) * 0.7 : 0;
        final var lean = fx * 2.5 * speed01 + shake;

        // Sombra no chão (diminui quando o robô sobe)
        g.setColor(new Color(40, 30, 70, 60));
        final var sw = 26 - bob;
        g.fill(new Ellipse2D.Double(-sw / 2, -5, sw, 10));

        // Brilho do propulsor
        final var flick = (float) (0.35 + 0.25 * Math.sin(t * 20 + phase));
        g.setColor(withAlpha(new Color(120, 220, 255), flick));
        g.fill(new Ellipse2D.Double(-6 + lean * 0.3, -(9.5 + bob), 12, 5.5));

        // Corpo
        final var bodyBottom = -(7 + bob);
        final var bodyTop = -(27 + bob);
        final var bx = lean * 0.5;

        final var swing = Math.sin(t * 10 + phase) * 0.6 * (0.2 + speed01);
        drawArm(g, color, bx - 11.5, bodyTop + 7, swing);
        drawArm(g, color, bx + 11.5, bodyTop + 7, -swing);

        fillShaded(g, new RoundRectangle2D.Double(bx - 11, bodyTop, 22, bodyBottom - bodyTop, 10, 10), color);
        if (front) {
            final var px = bx + fx * 3;
            g.setColor(withAlpha(Color.WHITE, badge != null ? 0.85f : 0.35f));
            g.fill(new RoundRectangle2D.Double(px - 5, bodyTop + 7, 10, 7, 4, 4));
            if (theme == Theme.MESSENGER) drawTargetEmblem(g, px, bodyTop + 10.5);
            if (badge != null) drawBadge(g, badge, px, bodyTop + 10.5, color);
        }

        // Cabeça
        final var hx = lean;
        final var headBottom = -(25 + bob);
        final var headTop = -(47 + bob);
        drawBehindHead(g, theme, hx, headTop, fx, mood, t, phase, color);
        fillShaded(g, new RoundRectangle2D.Double(hx - 15, headTop, 30, headBottom - headTop, 13, 13), color);
        g.setColor(withAlpha(Color.WHITE, 0.35f));
        g.fill(new Ellipse2D.Double(hx - 11, headTop + 2.5, 9, 4.5));

        if (front) drawFace(g, hx, headTop, fx, fy, mood, t, phase);
        else drawBack(g, hx, headTop, fx, color);

        // Antena (alguns temas trocam por chapéu, radar etc.)
        if (!theme.hidesAntenna()) {
            final var wob = Math.sin(t * 7 + phase) * 1.5 - fx * 2.5 * speed01;
            final var tipX = hx + wob;
            final var tipY = headTop - 9;
            g.setColor(new Color(70, 70, 90));
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new QuadCurve2D.Double(hx, headTop + 0.5, hx, headTop - 5, tipX, tipY));
            final var glow = brighter(color);
            g.setColor(withAlpha(glow, (float) (0.25 + 0.15 * Math.sin(t * 5 + phase))));
            g.fill(circle(tipX, tipY, 6));
            g.setColor(glow);
            g.fill(circle(tipX, tipY, 3.2));
            g.setColor(withAlpha(Color.WHITE, 0.8f));
            g.fill(circle(tipX - 1, tipY - 1, 1.1));
        }

        drawOnHead(g, theme, hx, headTop, fx, front, t, phase, color);

        if (mood == Mood.SLEEPY) drawZzz(g, hx, headTop, t, phase);
        if (mood == Mood.SCARED) drawSweat(g, hx + 15.5, headTop + 6);

        g.dispose();
    }

    // =====================================================================
    // Acessórios temáticos
    // =====================================================================

    /** Partes que ficam ATRÁS da cabeça (a base delas é escondida pela cabeça). */
    private static void drawBehindHead(Graphics2D g, Theme theme, double hx, double headTop, double fx,
                                       Mood mood, double t, double phase, Color color) {
        switch (theme) {
            case BUNNY -> {
                // Orelhas em pé; murcham para trás quando assustado
                final var droop = mood == Mood.SCARED ? 0.75 : 0.12 + 0.05 * Math.sin(t * 3 + phase);
                drawBunnyEar(g, hx - 6, headTop + 4, -droop, color);
                drawBunnyEar(g, hx + 6, headTop + 4, droop, color);
            }
            case WOLF -> {
                final var fur = mix(color, Color.BLACK, 0.3);
                drawWolfEar(g, hx - 9, headTop + 3, -1, fur);
                drawWolfEar(g, hx + 9, headTop + 3, 1, fur);
            }
            case SHEEP -> {
                // Orelhinhas caídas para os lados
                g.setColor(mix(color, Color.BLACK, 0.35));
                g.fill(rotated(new Ellipse2D.Double(hx - 22, headTop + 6, 9, 4.5), 0.35, hx - 17.5, headTop + 8));
                g.fill(rotated(new Ellipse2D.Double(hx + 13, headTop + 6, 9, 4.5), -0.35, hx + 17.5, headTop + 8));
            }
            default -> {
            }
        }
    }

    /** Partes que ficam POR CIMA da cabeça (chapéus, radar, lã...). */
    private static void drawOnHead(Graphics2D g, Theme theme, double hx, double headTop, double fx,
                                   boolean front, double t, double phase, Color color) {
        final var thin = new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        switch (theme) {
            case MESSENGER -> {
                // Boné com aba virada para onde o robô olha
                final var capColor = new Color(250, 120, 90);
                final var dome = new Arc2D.Double(hx - 14, headTop - 8, 28, 18, 0, 180, Arc2D.CHORD);
                if (front) {
                    g.setColor(mix(capColor, Color.BLACK, 0.25));
                    g.fill(new Ellipse2D.Double(hx - 9 + fx * 9, headTop - 1, 18, 5));
                }
                g.setColor(capColor);
                g.fill(dome);
                g.setColor(mix(capColor, Color.BLACK, 0.4));
                g.setStroke(thin);
                g.draw(dome);
                g.setColor(Color.WHITE);
                g.fill(new Arc2D.Double(hx - 4 + fx * 4, headTop - 6, 8, 8, 0, 180, Arc2D.CHORD));
                g.setColor(mix(capColor, Color.WHITE, 0.4));
                g.fill(circle(hx, headTop - 8, 1.8));
            }
            case PILOT -> {
                // Faixa e óculos de aviador na testa
                g.setColor(new Color(120, 80, 50));
                g.fill(new RoundRectangle2D.Double(hx - 15, headTop + 0.5, 30, 3.5, 2, 2));
                if (front) {
                    for (var ox : new double[]{-5.5, 5.5}) {
                        final var cx = hx + fx * 4.5 + ox;
                        g.setColor(new Color(220, 180, 80));
                        g.fill(circle(cx, headTop + 2, 4.2));
                        g.setColor(new Color(150, 220, 255));
                        g.fill(circle(cx, headTop + 2, 3));
                        g.setColor(withAlpha(Color.WHITE, 0.8f));
                        g.fill(circle(cx - 1, headTop + 1, 0.9));
                    }
                }
            }
            case EXPLORER -> {
                // Chapéu de safári
                final var khaki = new Color(225, 200, 140);
                g.setColor(mix(khaki, Color.BLACK, 0.12));
                g.fill(new Ellipse2D.Double(hx - 21, headTop - 4, 42, 9));
                g.setColor(mix(khaki, Color.BLACK, 0.4));
                g.setStroke(thin);
                g.draw(new Ellipse2D.Double(hx - 21, headTop - 4, 42, 9));
                final var dome = new Arc2D.Double(hx - 13, headTop - 14, 26, 22, 0, 180, Arc2D.CHORD);
                g.setColor(khaki);
                g.fill(dome);
                g.setColor(mix(khaki, Color.BLACK, 0.4));
                g.draw(dome);
                g.setColor(new Color(120, 90, 60));
                g.fill(new Rectangle2D.Double(hx - 12.5, headTop - 1, 25, 2.5));
                g.setColor(new Color(110, 190, 120));
                g.fill(rotated(new Ellipse2D.Double(hx + 6, headTop - 6, 7, 3), -0.6, hx + 9.5, headTop - 4.5));
            }
            case RADAR -> {
                // Mastro + prato de radar girando
                g.setColor(new Color(70, 70, 90));
                g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Double(hx, headTop + 0.5, hx, headTop - 7));
                final var spin = Math.cos(t * 3 + phase);
                final var w = 4 + 14 * Math.abs(spin);
                final var dish = new Ellipse2D.Double(hx - w / 2, headTop - 13, w, 8);
                g.setColor(spin > 0 ? new Color(225, 230, 240) : new Color(170, 175, 195));
                g.fill(dish);
                g.setColor(new Color(90, 95, 120));
                g.setStroke(thin);
                g.draw(dish);
                g.setColor(new Color(255, 90, 90));
                g.fill(circle(hx, headTop - 9, 1.5));
                final var ping = ((t * 0.8 + phase) % 1.0 + 1.0) % 1.0;
                g.setColor(withAlpha(new Color(90, 220, 160), (float) (0.7 * (1 - ping))));
                g.draw(new Arc2D.Double(hx - 4 - ping * 10, headTop - 13 - ping * 6, 8 + ping * 20, 8 + ping * 12,
                    30, 120, Arc2D.OPEN));
            }
            case CONDUCTOR -> {
                // Quepe de maquinista
                final var navy = new Color(60, 70, 120);
                final var cap = new RoundRectangle2D.Double(hx - 12, headTop - 9, 24, 10, 6, 6);
                g.setColor(navy);
                g.fill(cap);
                g.setColor(mix(navy, Color.WHITE, 0.25));
                g.fill(new Rectangle2D.Double(hx - 12, headTop - 2, 24, 2.5));
                g.setColor(mix(navy, Color.BLACK, 0.4));
                g.setStroke(thin);
                g.draw(cap);
                if (front) {
                    g.setColor(new Color(30, 30, 45));
                    g.fill(new Ellipse2D.Double(hx - 11 + fx * 5, headTop - 0.5, 22, 4));
                    g.setColor(new Color(240, 200, 80));
                    g.fill(circle(hx + fx * 4, headTop - 5, 2.2));
                }
            }
            case CHICK -> {
                // Topete de penas
                final var yellow = new Color(255, 214, 90);
                for (var a : new double[]{-0.55, 0.0, 0.55}) {
                    final var feather = rotated(new Ellipse2D.Double(hx - 2, headTop - 10, 4, 11), a, hx, headTop);
                    g.setColor(yellow);
                    g.fill(feather);
                    g.setColor(mix(yellow, Color.BLACK, 0.35));
                    g.setStroke(thin);
                    g.draw(feather);
                }
            }
            case RACER -> {
                // Capacete com faixa na cor do robô
                final var shell = new Arc2D.Double(hx - 17, headTop - 7, 34, 28, 0, 180, Arc2D.CHORD);
                g.setColor(new Color(248, 248, 252));
                g.fill(shell);
                final var oldClip = g.getClip();
                g.clip(shell);
                g.setColor(color);
                g.fill(new Rectangle2D.Double(hx - 3 + fx * 6, headTop - 8, 6, 16));
                g.setClip(oldClip);
                g.setColor(new Color(120, 120, 140));
                g.setStroke(thin);
                g.draw(shell);
                g.setColor(new Color(40, 40, 55));
                g.fill(new Rectangle2D.Double(hx - 17, headTop + 6, 34, 1.5));
            }
            case WOLF -> {
                // Tufo de pelo
                final var fur = mix(color, Color.BLACK, 0.3);
                final var tuft = new Path2D.Double();
                tuft.moveTo(hx - 6, headTop + 1);
                tuft.lineTo(hx - 3, headTop - 4);
                tuft.lineTo(hx, headTop + 0.5);
                tuft.lineTo(hx + 3, headTop - 5);
                tuft.lineTo(hx + 6, headTop + 1);
                tuft.closePath();
                g.setColor(fur);
                g.fill(tuft);
            }
            case SHEEP -> {
                // Lã fofinha
                final double[][] puffs = {
                    {-13, 3, 4.5}, {-9, -2, 5.5}, {-3, -5, 6}, {4, -5, 6}, {10, -2, 5.5}, {13.5, 3, 4.5}, {0, 0, 6},
                };
                for (var p : puffs) {
                    g.setColor(new Color(200, 200, 215));
                    g.fill(circle(hx + p[0], headTop + p[1] + 0.6, p[2]));
                }
                for (var p : puffs) {
                    g.setColor(new Color(252, 252, 255));
                    g.fill(circle(hx + p[0], headTop + p[1], p[2] - 0.6));
                }
            }
            default -> {
            }
        }
    }

    private static void drawBunnyEar(Graphics2D g0, double x, double y, double angle, Color color) {
        final var g = (Graphics2D) g0.create();
        g.translate(x, y);
        g.rotate(angle);
        final var outer = new Ellipse2D.Double(-3.8, -22, 7.6, 24);
        g.setColor(color);
        g.fill(outer);
        g.setColor(mix(color, Color.BLACK, 0.5));
        g.setStroke(new BasicStroke(1f));
        g.draw(outer);
        g.setColor(new Color(255, 170, 190));
        g.fill(new Ellipse2D.Double(-1.8, -19, 3.6, 16));
        g.dispose();
    }

    private static void drawWolfEar(Graphics2D g, double x, double y, int side, Color fur) {
        final var ear = new Path2D.Double();
        ear.moveTo(x - 5.5, y + 2);
        ear.lineTo(x + side * 2.5, y - 13);
        ear.lineTo(x + 5.5, y + 2);
        ear.closePath();
        g.setColor(fur);
        g.fill(ear);
        g.setColor(mix(fur, Color.BLACK, 0.4));
        g.setStroke(new BasicStroke(1f));
        g.draw(ear);
        final var inner = new Path2D.Double();
        inner.moveTo(x - 2.5, y + 1);
        inner.lineTo(x + side * 1.5, y - 8);
        inner.lineTo(x + 2.5, y + 1);
        inner.closePath();
        g.setColor(new Color(255, 170, 170));
        g.fill(inner);
    }

    private static void drawTargetEmblem(Graphics2D g, double cx, double cy) {
        g.setColor(new Color(250, 120, 90));
        g.fill(circle(cx, cy, 3));
        g.setColor(Color.WHITE);
        g.fill(circle(cx, cy, 2));
        g.setColor(new Color(250, 120, 90));
        g.fill(circle(cx, cy, 1));
    }

    private static void drawBadge(Graphics2D g, String badge, double cx, double cy, Color color) {
        g.setFont(BADGE);
        final var fm = g.getFontMetrics();
        g.setColor(mix(color, Color.BLACK, 0.55));
        g.drawString(badge, (float) (cx - fm.stringWidth(badge) / 2.0), (float) (cy + fm.getAscent() / 2.0 - 0.6));
    }

    private static Shape rotated(Shape shape, double angle, double px, double py) {
        return AffineTransform.getRotateInstance(angle, px, py).createTransformedShape(shape);
    }

    private static void drawFace(Graphics2D g, double hx, double headTop, double fx, double fy,
                                 Mood mood, double t, double phase) {
        final var w = 21 - Math.abs(fx) * 5;
        final var sx = hx + fx * 4.5;
        final var top = headTop + 5;
        final var h = 12.5;

        g.setColor(SCREEN);
        g.fill(new RoundRectangle2D.Double(sx - w / 2, top, w, h, 7, 7));
        g.setColor(withAlpha(Color.WHITE, 0.08f));
        g.fill(new RoundRectangle2D.Double(sx - w / 2 + 1.5, top + 1, w - 3, h / 2 - 1, 5, 5));

        final var ey = top + h / 2;
        final var spread = w * 0.24;
        final var exL = sx - spread;
        final var exR = sx + spread;
        final var round = new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

        switch (mood) {
            case HAPPY -> {
                final var blinking = ((t + phase) % 3.7) < 0.13;
                final var eh = blinking ? 0.8 : 5.2;
                for (var ex : new double[]{exL, exR}) {
                    g.setColor(withAlpha(EYE, 0.25f));
                    g.fill(new Ellipse2D.Double(ex - 3.2, ey - eh / 2 - 1.2, 6.4, eh + 2.4));
                    g.setColor(EYE);
                    g.fill(new Ellipse2D.Double(ex - 1.8, ey - eh / 2, 3.6, eh));
                    if (!blinking) {
                        g.setColor(Color.WHITE);
                        g.fill(circle(ex - 0.6, ey - 1.2, 0.7));
                    }
                }
            }
            case SLEEPY -> {
                g.setColor(EYE);
                g.setStroke(round);
                for (var ex : new double[]{exL, exR}) {
                    g.draw(new Arc2D.Double(ex - 2.2, ey - 2, 4.4, 3.2, 180, 180, Arc2D.OPEN));
                }
            }
            case SCARED -> {
                g.setColor(new Color(230, 240, 255));
                g.setStroke(round);
                final var left = new Path2D.Double();
                left.moveTo(exL - 1.8, ey - 2.2);
                left.lineTo(exL + 1.6, ey);
                left.lineTo(exL - 1.8, ey + 2.2);
                final var right = new Path2D.Double();
                right.moveTo(exR + 1.8, ey - 2.2);
                right.lineTo(exR - 1.6, ey);
                right.lineTo(exR + 1.8, ey + 2.2);
                g.draw(left);
                g.draw(right);
            }
            case ANGRY -> {
                for (var ex : new double[]{exL, exR}) {
                    g.setColor(withAlpha(EYE_ANGRY, 0.3f));
                    g.fill(circle(ex, ey + 0.5, 3.2));
                    g.setColor(EYE_ANGRY);
                    g.fill(circle(ex, ey + 0.5, 2));
                }
                g.setColor(new Color(255, 150, 140));
                g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Double(exL - 2.6, ey - 3.6, exL + 2.2, ey - 1.8));
                g.draw(new Line2D.Double(exR + 2.6, ey - 3.6, exR - 2.2, ey - 1.8));
            }
        }

        if (fy > -0.1 && mood != Mood.ANGRY) {
            g.setColor(CHEEK);
            g.fill(new Ellipse2D.Double(sx - w / 2 - 1.5, top + h + 1.2, 4.5, 2.6));
            g.fill(new Ellipse2D.Double(sx + w / 2 - 3, top + h + 1.2, 4.5, 2.6));
        }
    }

    private static void drawBack(Graphics2D g, double hx, double headTop, double fx, Color color) {
        final var vx = hx - 7 + fx * 3;
        g.setColor(mix(color, Color.BLACK, 0.35));
        g.fill(new RoundRectangle2D.Double(vx, headTop + 7, 14, 9, 4, 4));
        g.setColor(withAlpha(Color.WHITE, 0.45f));
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 3; i++) {
            final var y = headTop + 9.5 + i * 2.2;
            g.draw(new Line2D.Double(vx + 3, y, vx + 11, y));
        }
        g.setColor(mix(color, Color.WHITE, 0.5));
        g.fill(circle(hx - 11 + fx * 2, headTop + 11, 1.1));
        g.fill(circle(hx + 11 + fx * 2, headTop + 11, 1.1));
    }

    private static void drawArm(Graphics2D g0, Color color, double x, double y, double angle) {
        final var g = (Graphics2D) g0.create();
        g.translate(x, y);
        g.rotate(angle);
        final var arm = new RoundRectangle2D.Double(-2.5, 0, 5, 10, 5, 5);
        g.setColor(mix(color, Color.BLACK, 0.2));
        g.fill(arm);
        g.setColor(mix(color, Color.BLACK, 0.5));
        g.setStroke(new BasicStroke(0.9f));
        g.draw(arm);
        g.setColor(new Color(215, 215, 230));
        g.fill(circle(0, 10, 2.6));
        g.dispose();
    }

    private static void drawZzz(Graphics2D g, double hx, double headTop, double t, double phase) {
        for (int i = 0; i < 2; i++) {
            final var p = ((t * 0.6 + phase + i * 0.5) % 1.0 + 1.0) % 1.0;
            g.setFont(ZZZ.deriveFont((float) (6 + 4 * p)));
            g.setColor(withAlpha(new Color(90, 80, 140), (float) (1 - p)));
            g.drawString("z", (float) (hx + 12 + p * 6), (float) (headTop - 4 - p * 12));
        }
    }

    private static void drawSweat(Graphics2D g, double x, double y) {
        final var drop = new Path2D.Double();
        drop.moveTo(x, y - 3);
        drop.quadTo(x + 2.5, y + 1, x, y + 2.5);
        drop.quadTo(x - 2.5, y + 1, x, y - 3);
        g.setColor(new Color(140, 200, 255, 230));
        g.fill(drop);
    }

    private static void fillShaded(Graphics2D g, RectangularShape shape, Color color) {
        final var b = shape.getBounds2D();
        g.setPaint(new LinearGradientPaint(
            (float) b.getMinX(), 0f, (float) b.getMaxX(), 0f,
            new float[]{0f, 0.45f, 1f},
            new Color[]{mix(color, Color.WHITE, 0.3), color, mix(color, Color.BLACK, 0.25)}
        ));
        g.fill(shape);
        g.setColor(mix(color, Color.BLACK, 0.5));
        g.setStroke(new BasicStroke(1.1f));
        g.draw(shape);
    }

    private static Ellipse2D circle(double cx, double cy, double r) {
        return new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2);
    }

    static Color withAlpha(Color c, float alpha) {
        final var a = Math.clamp(Math.round(alpha * 255), 0, 255);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    static Color mix(Color a, Color b, double t) {
        return new Color(
            (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
            (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t)
        );
    }

    private static Color brighter(Color c) {
        return mix(c, Color.WHITE, 0.45);
    }
}
