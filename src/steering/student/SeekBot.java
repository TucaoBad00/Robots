package steering.student;

import steering.engine.Robot;
import steering.engine.Theme;
import steering.engine.Iso;
import steering.engine.Vector2;
import steering.engine.World;

import java.awt.*;

/** Cena 1 — SEEK: vai até o último clique na velocidade máxima (sem frear). */
public class SeekBot extends Robot {
    private Vector2 target;

    public SeekBot() {
        super(s -> s
            .color(new Color(110, 170, 255))
            .position(-150, 120)
            .maxSpeed(230)
            .maxForce(320)
            .orientationDegrees(-90));
    }

    @Override
    public Vector2 calculateSteering(World world) {
        target = world.getClickPos();
        if (target == null) return Behaviors.brake(this, 2);
        return Behaviors.seek(this, target);
    }

    @Override
    protected void drawDebug(Graphics2D g, Iso iso) {
        if (target != null) DebugDraw.line(g, iso, getPosition(), target, new Color(110, 170, 255, 180));
    }

    @Override
    protected Theme getTheme() {
        return Theme.MESSENGER;
    }
}
