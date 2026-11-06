package steering.student;

import steering.engine.Robot;
import steering.engine.Theme;
import steering.engine.Iso;
import steering.engine.Vector2;
import steering.engine.World;

import java.awt.*;

/** Cena 3 — ARRIVE: vai até o clique e desacelera dentro do raio de frenagem, parando no alvo. */
public class ArriveBot extends Robot {
    public static final double SLOWING_RADIUS = 180;
    private Vector2 target;

    public ArriveBot() {
        super(s -> s
            .color(new Color(255, 190, 110))
            .position(-150, 120)
            .maxSpeed(230)
            .maxForce(320)
            .orientationDegrees(-90));
    }

    @Override
    public Vector2 calculateSteering(World world) {
        target = world.getClickPos();
        if (target == null) return Behaviors.brake(this, 2);
        return Behaviors.arrive(this, target, SLOWING_RADIUS);
    }

    @Override
    protected void drawDebug(Graphics2D g, Iso iso) {
        if (target == null) return;
        DebugDraw.ring(g, iso, target, SLOWING_RADIUS, new Color(230, 140, 40, 170));
        DebugDraw.line(g, iso, getPosition(), target, new Color(230, 140, 40, 170));
    }

    @Override
    protected Theme getTheme() {
        return Theme.PILOT;
    }
}
