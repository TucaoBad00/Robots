package steering.student;

import steering.engine.Robot;
import steering.engine.Theme;
import steering.engine.Iso;
import steering.engine.Vector2;
import steering.engine.World;

import java.awt.*;
import java.util.List;

/**
 * Cena 5 — OBSTACLE AVOIDANCE.
 * O robô vai até o clique (ou passeia, se não houver clique) e desvia dos obstáculos.
 * O desvio tem prioridade máxima no Blend.
 */
public class AvoidBot extends Robot {
    private static final double MIN_DETECTION = 60;
    private final List<Obstacle> obstacles;
    private final Weights weights; // pesos ajustáveis na tela (painel da cena)
    private final Wander wander = new Wander(70, 35, 3);

    public AvoidBot(double x, double y, Color color, List<Obstacle> obstacles, Weights weights) {
        super(s -> s
            .color(color)
            .position(x, y)
            .randomOrientation()
            .maxSpeed(170)
            .maxForce(300));
        this.obstacles = obstacles;
        this.weights = weights;
    }

    @Override
    public Vector2 calculateSteering(World world) {
        final var click = world.getClickPos();
        final var goal = click == null
            ? wander.calculate(this, world.getSecs())
            : Behaviors.arrive(this, click, 120);
        return new Blend(this)
            .add("Obstacle Avoidance", Behaviors.avoidObstacles(this, obstacles, MIN_DETECTION), weights.get("avoid"))
            .add(click == null ? "Wander" : "Arrive", goal, weights.get("goal"))
            .result();
    }

    @Override
    protected void drawDebug(Graphics2D g, Iso iso) {
        DebugDraw.detectionBox(g, iso, this, Behaviors.detectionLength(this, MIN_DETECTION), new Color(230, 80, 80, 170));
    }

    @Override
    protected Theme getTheme() {
        return Theme.RADAR;
    }
}
