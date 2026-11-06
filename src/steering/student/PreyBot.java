package steering.student;

import steering.engine.Robot;
import steering.engine.Theme;
import steering.engine.Iso;
import steering.engine.Mood;
import steering.engine.Vector2;
import steering.engine.World;

import java.awt.*;
import java.util.List;

import static steering.engine.Vector2.isZero;

/**
 * Cena 7 — COMBINAÇÃO A, "Presa".
 * Prioridade / peso padrão (ajustável na tela pelo painel de pesos):
 * <ol>
 *   <li>Obstacle Avoidance ×3.0 — nunca atravessar obstáculos, mesmo em pânico;</li>
 *   <li>Flee do mouse      ×2.0 — o "predador" é o cursor; fugir importa mais que passear;</li>
 *   <li>Separation         ×1.2 — o grupo não se amontoa ao fugir;</li>
 *   <li>Wander             ×0.6 — passeio calmo quando não há perigo.</li>
 * </ol>
 */
public class PreyBot extends Robot {
    public static final double PANIC_RADIUS = 190;
    private static final double MIN_DETECTION = 55;
    private final List<Obstacle> obstacles;
    private final Weights weights; // pesos ajustáveis na tela (painel da cena)
    private final Wander wander = new Wander(60, 30, 3);
    private boolean scared;

    public PreyBot(Color color, List<Obstacle> obstacles, Weights weights) {
        super(s -> s
            .color(color)
            .position(TestMap.freeSpot(obstacles, 40))
            .randomOrientation()
            .maxSpeed(210)
            .maxForce(340));
        this.obstacles = obstacles;
        this.weights = weights;
    }

    @Override
    public Vector2 calculateSteering(World world) {
        final var mouse = world.getMousePos();
        final var flee = mouse == null ? null : Behaviors.flee(this, mouse, PANIC_RADIUS);
        scared = !isZero(flee);

        final var blend = new Blend(this)
            .add(Behaviors.avoidObstacles(this, obstacles, MIN_DETECTION), weights.get("avoid"))
            .add(flee, weights.get("flee"))
            .add(Behaviors.separation(this, world.getNeighbors(60), 60), weights.get("separation"));
        // Passeia devagar quando calmo: o wander só usa o orçamento que sobrar
        if (!scared) blend.add(wander.calculate(this, world.getSecs()), weights.get("wander"));
        return blend.result();
    }

    @Override
    protected Mood getMood() {
        return scared ? Mood.SCARED : Mood.HAPPY;
    }

    @Override
    protected void drawDebug(Graphics2D g, Iso iso) {
        DebugDraw.detectionBox(g, iso, this, Behaviors.detectionLength(this, MIN_DETECTION), new Color(230, 80, 80, 150));
        if (!scared) DebugDraw.wander(g, iso, this, wander);
    }

    @Override
    protected Theme getTheme() {
        return Theme.CHICK;
    }
}
