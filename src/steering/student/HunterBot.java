package steering.student;

import steering.engine.Robot;
import steering.engine.Theme;
import steering.engine.Iso;
import steering.engine.Mood;
import steering.engine.Vector2;
import steering.engine.World;

import java.awt.*;
import java.util.Comparator;
import java.util.List;

/**
 * Cena 9 — COMBINAÇÃO C, "Predador": persegue a presa livre mais próxima.
 * Prioridade / peso padrão (ajustável na tela pelo painel de pesos): Obstacle Avoidance ×3.0, Pursuit ×1.0 (ou Wander, se todas foram pegas).
 */
public class HunterBot extends Robot {
    private static final double MIN_DETECTION = 60;
    private final List<Obstacle> obstacles;
    private final Weights weights; // pesos ajustáveis na tela (painel da cena)
    private final Wander wander = new Wander(70, 35, 3);
    private Robot prey;

    public HunterBot(List<Obstacle> obstacles, Weights weights) {
        super(s -> s
            .color(new Color(240, 100, 110))
            .position(0, -380)
            .orientationDegrees(90)
            .size(1.3)
            .maxSpeed(185)
            .maxForce(300));
        this.obstacles = obstacles;
        this.weights = weights;
    }

    @Override
    public Vector2 calculateSteering(World world) {
        prey = world.getNeighbors().stream()
            .filter(c -> c instanceof RunnerBot r && !r.isTagged())
            .min(Comparator.comparingDouble(c -> Robot.distance(this, c)))
            .orElse(null);

        final var chase = prey == null
            ? wander.calculate(this, world.getSecs())
            : Behaviors.pursuit(this, prey);
        return new Blend(this)
            .add("Obstacle Avoidance", Behaviors.avoidObstacles(this, obstacles, MIN_DETECTION), weights.get("wolfAvoid"))
            .add(prey == null ? "Wander" : "Pursuit", chase, weights.get("pursuit"))
            .result();
    }

    @Override
    protected Mood getMood() {
        return Mood.ANGRY;
    }

    @Override
    protected void drawDebug(Graphics2D g, Iso iso) {
        DebugDraw.detectionBox(g, iso, this, Behaviors.detectionLength(this, MIN_DETECTION), new Color(230, 80, 80, 150));
        if (prey != null) DebugDraw.line(g, iso, getPosition(), prey.getPosition(), new Color(240, 90, 100, 200));
    }

    @Override
    protected Theme getTheme() {
        return Theme.WOLF;
    }
}
