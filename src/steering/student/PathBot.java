package steering.student;

import steering.engine.Robot;
import steering.engine.Theme;
import steering.engine.Iso;
import steering.engine.Vector2;
import steering.engine.World;

import java.awt.*;

/** Cena 6 — PATH FOLLOWING: segue o circuito, mirando sempre um ponto à frente na pista. */
public class PathBot extends Robot {
    private static final double PREDICT = 25;
    private static final double LOOK_AHEAD = 60;
    private final Path path;
    private Vector2 target;

    public PathBot(Path path, double startAlong, double maxSpeed, Color color) {
        super(s -> s
            .color(color)
            .position(path.pointAt(startAlong))
            .orientation(path.angleAt(startAlong))
            .maxSpeed(maxSpeed)
            .maxForce(300));
        this.path = path;
    }

    @Override
    public Vector2 calculateSteering(World world) {
        target = Behaviors.pathTarget(this, path, PREDICT, LOOK_AHEAD);
        return Behaviors.seek(this, target);
    }

    @Override
    protected void drawDebug(Graphics2D g, Iso iso) {
        if (target == null) return;
        DebugDraw.line(g, iso, getPosition(), target, new Color(150, 110, 230, 180));
        DebugDraw.dot(g, iso, target, 5, new Color(230, 70, 150));
    }

    @Override
    protected Theme getTheme() {
        return Theme.CONDUCTOR;
    }
}
