package steering.engine;

import steering.student.Setup;
import steering.student.Weights;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

import static steering.engine.Vector2.vec2;

/** Desenha um quadro completo da cena em câmera isométrica. */
final class Renderer {
    private static final double SLAB = 18;          // espessura da "placa" do chão, em unidades de mundo
    private static final double ROBOT_HEADROOM = 60; // espaço acima do chão para a cabeça dos robôs
    private static final Color TILE_A = new Color(238, 234, 250);
    private static final Color TILE_B = new Color(226, 238, 248);
    private static final Font TITLE = new Font("SansSerif", Font.BOLD, 16);
    private static final Font TEXT = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font SMALL = new Font("SansSerif", Font.PLAIN, 11);
    private static final String KEYS =
        "1 Seek   2 Flee   3 Arrive   4 Wander   5 Obstacle Avoidance   6 Path Following   "
            + "7 Presa   8 Corrida   9 Predador × Presa      ↑↓←→ pesos";

    private record Item(double depth, Runnable draw) {
    }

    private Renderer() {
    }

    /** Escolhe escala e origem para a arena caber na área disponível da janela. */
    static Iso computeIso(Rectangle area, double arena) {
        final var diamondW = arena * Math.sqrt(2);
        final var diamondH = diamondW / 2;
        final var top = 30.0;
        final var bottom = 46.0;
        final var availH = Math.max(100, area.height - top - bottom);
        final var s = Math.max(0.2, Math.min((area.width - 40) / diamondW, availH / (diamondH + SLAB + ROBOT_HEADROOM)));
        final var block = (diamondH + SLAB + ROBOT_HEADROOM) * s;
        final var blockTop = area.y + top + (availH - block) / 2;
        final var originY = blockTop + ROBOT_HEADROOM * s + diamondH * s / 2;
        return new Iso(area.x + area.width / 2.0, originY, s);
    }

    static void render(Graphics2D g, Iso iso, Rectangle area, double arena, List<Robot> robots, Setup setup,
                       Vector2 clickPos, Vector2 mousePos, boolean debug, boolean vectors, double time) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // Fundo
        final var fullW = area.x * 2 + area.width;
        final var fullH = area.y + area.height + 10;
        g.setPaint(new GradientPaint(0, 0, new Color(214, 206, 240), 0, fullH, new Color(196, 226, 240)));
        g.fillRect(0, 0, fullW, fullH);

        drawFloor(g, iso, arena);
        setup.drawGround(g, iso, mousePos);

        if (clickPos != null) {
            final var r = 12 + 3 * Math.sin(time * 5);
            g.setColor(new Color(110, 90, 170, 200));
            g.setStroke(new BasicStroke(2f));
            g.draw(iso.groundCircle(clickPos, r));
            g.fill(iso.groundCircle(clickPos, 3));
        }

        // Robôs e cenário ordenados por profundidade (de trás para frente)
        final var items = new ArrayList<Item>();
        for (var prop : setup.getProps()) {
            items.add(new Item(Iso.depth(prop.getPosition()), () -> prop.draw(g, iso)));
        }
        for (var robot : robots) {
            items.add(new Item(Iso.depth(robot.getPosition()), () -> robot.draw(g, iso, time)));
        }
        items.sort(Comparator.comparingDouble(Item::depth));
        items.forEach(item -> item.draw().run());

        if (debug) {
            robots.forEach(robot -> robot.drawDebugOverlay(g, iso));
        }
        if (vectors) {
            robots.forEach(robot -> robot.drawVectorOverlay(g, iso));
        }

        drawHud(g, area, setup, debug, vectors);
        drawWeights(g, area, setup.getWeights());
        if (vectors) drawVectorLegend(g, area, robots);
    }

    /** Legenda da camada de vetores (canto inferior esquerdo). */
    private static void drawVectorLegend(Graphics2D g, Rectangle area, List<Robot> robots) {
        final var legend = new LinkedHashMap<String, Color>();
        var cut = false;
        for (var robot : robots) {
            robot.collectLegend(legend);
            cut |= robot.hasCutForce();
        }
        final var lines = new ArrayList<String>();
        if (legend.isEmpty()) lines.add("Nenhum vetor extra neste momento.");
        final var notes = new ArrayList<String>();
        if (legend.keySet().stream().anyMatch(k -> k.startsWith("Força:"))) {
            notes.add("Forças na escala da seta laranja; tracejado = pedido cortado pelo orçamento"
                + (cut ? "" : " (nenhum agora)"));
        }
        if (legend.containsKey("Velocidade desejada")) {
            notes.add("Desejada na escala da seta azul: laranja = (desejada − azul) × 6");
        }

        g.setFont(TEXT);
        final var fm = g.getFontMetrics();
        var width = fm.stringWidth("Camada de vetores (V)") + 20;
        for (var key : legend.keySet()) width = Math.max(width, fm.stringWidth(key) + 46);
        for (var note : notes) width = Math.max(width, fm.stringWidth(note) + 24);
        for (var line : lines) width = Math.max(width, fm.stringWidth(line) + 24);

        final var rowH = 17;
        final var height = 34 + (legend.size() + notes.size() + lines.size()) * rowH;
        final var x = area.x + 14;
        final var y = area.y + area.height - 38 - 12 - height;
        panel(g, x, y, width, height);

        g.setFont(TITLE.deriveFont(13f));
        g.setColor(new Color(70, 55, 120));
        g.drawString("Camada de vetores (V)", x + 12, y + 20);
        g.setFont(TEXT);
        var ry = y + 38;
        for (var entry : legend.entrySet()) {
            g.setColor(entry.getValue());
            if (entry.getKey().startsWith("●")) {
                g.fill(new java.awt.geom.Ellipse2D.Double(x + 18, ry - 8, 8, 8));
            } else {
                g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new java.awt.geom.Line2D.Double(x + 12, ry - 4, x + 34, ry - 4));
            }
            g.setColor(new Color(80, 80, 105));
            g.drawString(entry.getKey().replace("● ", ""), x + 42, ry);
            ry += rowH;
        }
        g.setColor(new Color(120, 115, 145));
        for (var line : lines) {
            g.drawString(line, x + 12, ry);
            ry += rowH;
        }
        g.setFont(SMALL);
        for (var note : notes) {
            g.drawString(note, x + 12, ry);
            ry += rowH;
        }
    }

    // =====================================================================
    // Painel de pesos (canto superior direito)
    // =====================================================================

    /** Área clicável do painel: index = linha do peso (-1 = Restaurar), delta = quanto somar. */
    private record Button(Rectangle2D rect, int index, double delta) {
    }

    private static volatile List<Button> buttons = List.of();
    private static volatile Rectangle2D panelRect = null;

    /**
     * Trata um clique no painel de pesos.
     *
     * @return true se o clique foi no painel (aí ele NÃO vira alvo no chão).
     */
    static boolean handleClick(double x, double y, Weights weights) {
        for (var button : buttons) {
            if (!button.rect().contains(x, y)) continue;
            if (button.index() < 0) {
                weights.reset();
            } else {
                weights.select(button.index());
                weights.change(button.index(), button.delta());
            }
            return true;
        }
        final var panel = panelRect;
        return panel != null && panel.contains(x, y);
    }

    private static void drawWeights(Graphics2D g, Rectangle area, Weights weights) {
        if (weights.isEmpty()) {
            buttons = List.of();
            panelRect = null;
            return;
        }
        final var entries = weights.getEntries();
        final var width = 300;
        final var rowH = 26;
        final var x = area.x + area.width - width - 14;
        final var y = area.y + 14;
        final var height = 44 + entries.size() * rowH + 36;
        panel(g, x, y, width, height);
        panelRect = new Rectangle2D.Double(x, y, width, height);

        final var accent = new Color(11, 122, 114);
        final var ink = new Color(70, 55, 120);
        final var muted = new Color(120, 115, 145);

        g.setFont(TITLE.deriveFont(14f));
        g.setColor(ink);
        g.drawString("Pesos dos comportamentos", x + 12, y + 22);
        g.setFont(SMALL);
        g.setColor(muted);
        g.drawString("a ordem das linhas é a prioridade", x + 12, y + 37);

        final var newButtons = new ArrayList<Button>();
        for (int i = 0; i < entries.size(); i++) {
            final var entry = entries.get(i);
            final var ry = y + 44 + i * rowH;
            final var changed = Math.abs(entry.getValue() - entry.getDefaultValue()) > 1e-9;

            if (i == weights.getSelected()) {
                g.setColor(new Color(11, 122, 114, 30));
                g.fill(new RoundRectangle2D.Double(x + 6, ry, width - 12, rowH - 2, 8, 8));
            }

            g.setFont(TEXT);
            g.setColor(new Color(80, 80, 105));
            g.drawString((i + 1) + "  " + entry.getLabel(), x + 12, ry + 17);

            // Barra: valor atual e um tracinho no valor padrão
            final var barX = x + 150;
            final var barW = 56.0;
            g.setColor(new Color(225, 222, 238));
            g.fill(new RoundRectangle2D.Double(barX, ry + 10, barW, 6, 6, 6));
            g.setColor(changed ? accent : new Color(150, 140, 200));
            g.fill(new RoundRectangle2D.Double(barX, ry + 10, barW * entry.getValue() / Weights.MAX, 6, 6, 6));
            final var defaultX = barX + barW * entry.getDefaultValue() / Weights.MAX;
            g.setColor(ink);
            g.fill(new Rectangle2D.Double(defaultX - 0.75, ry + 7, 1.5, 12));

            final var minus = new Rectangle2D.Double(x + width - 92, ry + 2, 20, 20);
            final var plus = new Rectangle2D.Double(x + width - 32, ry + 2, 20, 20);
            drawButton(g, minus, "−");
            drawButton(g, plus, "+");
            newButtons.add(new Button(minus, i, -Weights.STEP));
            newButtons.add(new Button(plus, i, Weights.STEP));

            final var value = String.format(java.util.Locale.ROOT, "%.1f", entry.getValue());
            g.setFont(changed ? TEXT.deriveFont(Font.BOLD) : TEXT);
            g.setColor(changed ? accent : new Color(80, 80, 105));
            final var vw = g.getFontMetrics().stringWidth(value);
            g.drawString(value, (float) (x + width - 52 - vw / 2.0), ry + 17);
        }

        final var footerY = y + 44 + entries.size() * rowH + 8;
        g.setFont(SMALL);
        g.setColor(muted);
        g.drawString("↑↓ escolhe   ←→ ajusta   R restaura", x + 12, footerY + 15);
        final var reset = new Rectangle2D.Double(x + width - 92, footerY, 80, 22);
        drawButton(g, reset, "Restaurar");
        newButtons.add(new Button(reset, -1, 0));
        buttons = List.copyOf(newButtons);
    }

    private static void drawButton(Graphics2D g, Rectangle2D rect, String label) {
        final var shape = new RoundRectangle2D.Double(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight(), 8, 8);
        g.setColor(new Color(240, 238, 250));
        g.fill(shape);
        g.setColor(new Color(150, 140, 200, 160));
        g.setStroke(new BasicStroke(1f));
        g.draw(shape);
        g.setFont(TEXT.deriveFont(Font.BOLD));
        g.setColor(new Color(70, 55, 120));
        final var fm = g.getFontMetrics();
        g.drawString(label, (float) (rect.getCenterX() - fm.stringWidth(label) / 2.0),
            (float) (rect.getCenterY() + fm.getAscent() / 2.0 - 2));
    }

    private static void drawFloor(Graphics2D g, Iso iso, double arena) {
        final var h = arena / 2;
        final int n = 9;
        final var tile = arena / n;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                final var x0 = -h + i * tile;
                final var y0 = -h + j * tile;
                g.setColor(((i + j) % 2 == 0) ? TILE_A : TILE_B);
                g.fill(iso.groundPolygon(vec2(x0, y0), vec2(x0 + tile, y0), vec2(x0 + tile, y0 + tile), vec2(x0, y0 + tile)));
            }
        }

        // Laterais da placa (dão volume ao chão)
        final var drop = SLAB * iso.getScale();
        final var right = iso.toScreen(h, -h, 0);
        final var bottom = iso.toScreen(h, h, 0);
        final var left = iso.toScreen(-h, h, 0);
        g.setColor(new Color(186, 176, 220));
        g.fill(quad(left.x, left.y, bottom.x, bottom.y, bottom.x, bottom.y + drop, left.x, left.y + drop));
        g.setColor(new Color(162, 152, 202));
        g.fill(quad(bottom.x, bottom.y, right.x, right.y, right.x, right.y + drop, bottom.x, bottom.y + drop));

        g.setColor(new Color(255, 255, 255, 170));
        g.setStroke(new BasicStroke(2f));
        g.draw(iso.groundPolygon(vec2(-h, -h), vec2(h, -h), vec2(h, h), vec2(-h, h)));
    }

    private static Path2D quad(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
        final var p = new Path2D.Double();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        p.lineTo(x3, y3);
        p.lineTo(x4, y4);
        p.closePath();
        return p;
    }

    private static void drawHud(Graphics2D g, Rectangle area, Setup setup, boolean debug, boolean vectors) {
        final var title = setup.getTitle();
        final var lines = setup.getDescription().split("\n");

        g.setFont(TITLE);
        final var titleMetrics = g.getFontMetrics();
        var width = titleMetrics.stringWidth(title);
        g.setFont(TEXT);
        final var textMetrics = g.getFontMetrics();
        for (var line : lines) width = Math.max(width, textMetrics.stringWidth(line));

        final var x = area.x + 14;
        final var y = area.y + 14;
        final var panelW = width + 28;
        final var panelH = 40 + lines.length * 17;
        panel(g, x, y, panelW, panelH);

        g.setFont(TITLE);
        g.setColor(new Color(70, 55, 120));
        g.drawString(title, x + 14, y + 25);
        g.setFont(TEXT);
        g.setColor(new Color(80, 80, 105));
        for (int i = 0; i < lines.length; i++) {
            g.drawString(lines[i], x + 14, y + 46 + i * 17);
        }

        final var bar = KEYS + "      D: debug " + (debug ? "ligado" : "desligado")
            + "   V: vetores " + (vectors ? "ligado" : "desligado");
        final var barW = textMetrics.stringWidth(bar) + 28;
        final var barX = area.x + (area.width - barW) / 2;
        final var barY = area.y + area.height - 38;
        panel(g, barX, barY, barW, 26);
        g.setColor(new Color(80, 80, 105));
        g.drawString(bar, barX + 14, barY + 17);
    }

    private static void panel(Graphics2D g, double x, double y, double w, double h) {
        final var shape = new RoundRectangle2D.Double(x, y, w, h, 16, 16);
        g.setColor(new Color(255, 255, 255, 205));
        g.fill(shape);
        g.setColor(new Color(150, 140, 200, 120));
        g.setStroke(new BasicStroke(1f));
        g.draw(shape);
    }
}
