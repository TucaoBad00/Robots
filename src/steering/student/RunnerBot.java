package steering.student;

import steering.engine.Robot;
import steering.engine.Theme;
import steering.engine.Mood;
import steering.engine.Vector2;
import steering.engine.World;

import java.awt.*;
import java.util.List;

import static steering.engine.Vector2.isZero;

/**
 * Cena 9 — COMBINAÇÃO C, "Presa do predador".
 * Prioridade / peso padrão (ajustável na tela pelo painel de pesos):
 * <ol>
 *   <li>Obstacle Avoidance ×3.0;</li>
 *   <li>Evade do predador  ×2.0 — foge da posição FUTURA do caçador;</li>
 *   <li>Separation         ×1.0;</li>
 *   <li>Wander             ×0.7 — só quando não há perigo.</li>
 * </ol>
 * Quando o caçador encosta, a presa fica "pega" (tonta) por alguns segundos e o caçador troca de alvo.
 */
public class RunnerBot extends Robot {
    public static final double PANIC_RADIUS = 170;
    private static final double MIN_DETECTION = 55;
    private static final double TAG_TIME = 2.5;
    private final List<Obstacle> obstacles;
    private final Weights weights; // pesos ajustáveis na tela (painel da cena)
    private final Wander wander = new Wander(60, 30, 3);
    private boolean scared;
    private double taggedTimer;

    public RunnerBot(Color color, List<Obstacle> obstacles, Weights weights) {
        super(s -> s
            .color(color)
            .position(TestMap.freeSpot(obstacles, 40))
            .randomOrientation()
            .maxSpeed(195)
            .maxForce(300));
        this.obstacles = obstacles;
        this.weights = weights;
    }

    public boolean isTagged() {
        return taggedTimer > 0;
    }

    @Override
    public Vector2 calculateSteering(World world) {
        taggedTimer = Math.max(0, taggedTimer - world.getSecs());

        final var blend = new Blend(this)
            .add("Obstacle Avoidance", Behaviors.avoidObstacles(this, obstacles, MIN_DETECTION), weights.get("sheepAvoid"));

        scared = false;
        for (var other : world.getNeighbors()) {
            if (!(other instanceof HunterBot hunter)) continue;
            if (Robot.distance(this, hunter) < getRadius() + hunter.getRadius() + 4) taggedTimer = TAG_TIME;
            final var evade = Behaviors.evade(this, hunter, PANIC_RADIUS);
            if (!isZero(evade)) {
                scared = true;
                blend.add("Evade", evade, weights.get("evade"));
            }
        }
        blend.add("Separation", Behaviors.separation(this, world.getNeighbors(60), 60), weights.get("separation"));
        if (!scared) blend.add("Wander", wander.calculate(this, world.getSecs()), weights.get("wander"));
        return blend.result();
    }

    @Override
    protected Mood getMood() {
        if (isTagged()) return Mood.SLEEPY; // ficou tonto depois de ser pego
        return scared ? Mood.SCARED : Mood.HAPPY;
    }

    @Override
    protected Theme getTheme() {
        return Theme.SHEEP;
    }
}
