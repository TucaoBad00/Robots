package steering.student;

import steering.engine.Robot;
import steering.engine.Theme;
import steering.engine.Iso;
import steering.engine.Vector2;
import steering.engine.World;

import java.awt.*;
import java.util.List;

/**
 * Cena 8 — COMBINAÇÃO B, "Corrida".
 * Prioridade / peso padrão (ajustável na tela pelo painel de pesos):
 * <ol>
 *   <li>Obstacle Avoidance ×3.0 — desviar dos cones na pista;</li>
 *   <li>Separation         ×1.5 — não encostar nos outros corredores (ultrapassagens);</li>
 *   <li>Path Following     ×1.0 — seguir o circuito usa o orçamento que sobrar.</li>
 * </ol>
 * Cada corredor tem velocidade máxima diferente, então as ultrapassagens acontecem naturalmente.
 */
public class RacerBot extends Robot {
    private static final double MIN_DETECTION = 45;
    private final Path path;
    private final List<Obstacle> obstacles;
    private final Weights weights; // pesos ajustáveis na tela (painel da cena)
    private final int number;
    private Vector2 target;

    public RacerBot(Path path, List<Obstacle> obstacles, Weights weights, double startAlong, double maxSpeed, Color color, int number) {
        super(s -> s
            .color(color)
            .position(path.pointAt(startAlong))
            .orientation(path.angleAt(startAlong))
            .maxSpeed(maxSpeed)
            .maxForce(360));
        this.path = path;
        this.obstacles = obstacles;
        this.weights = weights;
        this.number = number;
    }

    @Override
    protected String getBadge() {
        return String.valueOf(number);
    }

    @Override
    public Vector2 calculateSteering(World world) {
        target = Behaviors.pathTarget(this, path, 25, 70);
        return new Blend(this)
            .add(Behaviors.avoidObstacles(this, obstacles, MIN_DETECTION), weights.get("avoid"))
            .add(Behaviors.separation(this, world.getNeighbors(55), 55), weights.get("separation"))
            .add(Behaviors.seek(this, target), weights.get("path"))
            .result();
    }

    @Override
    protected void drawDebug(Graphics2D g, Iso iso) {
        DebugDraw.detectionBox(g, iso, this, Behaviors.detectionLength(this, MIN_DETECTION), new Color(230, 80, 80, 150));
        if (target != null) DebugDraw.dot(g, iso, target, 4, new Color(230, 70, 150));
    }

    @Override
    protected Theme getTheme() {
        return Theme.RACER;
    }
}
