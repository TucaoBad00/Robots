package steering.student;

import steering.engine.Robot;
import steering.engine.Iso;
import steering.engine.Prop;
import steering.engine.Vector2;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Ponto de entrada do aluno na engine.
 * A janela pede os robôs daqui, e também o cenário: obstáculos, caminho e textos.
 * As teclas 1–9 trocam de cenário.
 */
public class Setup {
    private final List<Scene> scenes = Scenes.create();
    private volatile Scene current = scenes.getFirst();

    /** Retorna uma lista com todos os robôs do cenário atual. */
    public List<Robot> createRobots() {
        return current.factory().apply(current);
    }

    /** Troca o cenário se a tecla corresponder a um. Retorna true se trocou (ou reiniciou). */
    public boolean selectScene(char key) {
        for (var scene : scenes) {
            if (scene.key() == key) {
                current = scene;
                return true;
            }
        }
        return false;
    }

    public String getTitle() {
        return current.title();
    }

    public String getDescription() {
        return current.description();
    }

    /** Pesos ajustáveis da cena atual (vazio nas cenas sem combinação). */
    public Weights getWeights() {
        return current.weights();
    }

    /** Objetos de cenário desenhados (ordenados por profundidade junto com os robôs). */
    public List<Prop> getProps() {
        final var scene = current;
        final var props = new ArrayList<Prop>(scene.obstacles());
        props.addAll(scene.decor());
        return props;
    }

    /** Desenha o que fica no chão, por baixo de tudo: caminho e raio de pânico do mouse. */
    public void drawGround(Graphics2D g0, Iso iso, Vector2 mousePos) {
        final var scene = current;
        final var g = (Graphics2D) g0.create();
        if (scene.path() != null) scene.path().draw(g, iso);
        if (scene.threatRadius() > 0 && mousePos != null) {
            g.setColor(new Color(240, 90, 100, 40));
            g.fill(iso.groundCircle(mousePos, scene.threatRadius()));
            g.setColor(new Color(240, 90, 100, 170));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{7f, 6f}, 0f));
            g.draw(iso.groundCircle(mousePos, scene.threatRadius()));
            g.fill(iso.groundCircle(mousePos, 4));
        }
        g.dispose();
    }
}
