package steering.student;

import steering.engine.Robot;
import steering.engine.Theme;
import steering.engine.Iso;
import steering.engine.Vector2;
import steering.engine.World;

import java.awt.*;

/** Cena 4 — WANDER: passeia sem destino, fazendo curvas suaves. */
public class WanderBot extends Robot {
    private final Wander wander = new Wander(70, 35, 3.5);

    public WanderBot(Color color) {
        super(s -> s
            .color(color)
            .randomPosition()
            .randomOrientation()
            .maxSpeed(140)
            .maxForce(220));
    }

    @Override
    public Vector2 calculateSteering(World world) {
        return wander.calculate(this, world.getSecs());
    }

    @Override
    protected void drawDebug(Graphics2D g, Iso iso) {
        DebugDraw.wander(g, iso, this, wander);
    }

    @Override
    protected Theme getTheme() {
        return Theme.EXPLORER;
    }
}
