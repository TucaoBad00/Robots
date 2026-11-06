package steering.student;

import steering.engine.Robot;
import steering.engine.Theme;
import steering.engine.Mood;
import steering.engine.Vector2;
import steering.engine.World;

import java.awt.*;

import static steering.engine.Vector2.isZero;

/**
 * Cena 2 — FLEE: foge do cursor quando ele entra no raio de pânico.
 * Fora do raio, freia até parar (senão continuaria deslizando para sempre).
 */
public class FleeBot extends Robot {
    public static final double PANIC_RADIUS = 200;
    private boolean scared;

    public FleeBot(double x, double y, Color color) {
        super(s -> s
            .color(color)
            .position(x, y)
            .maxSpeed(240)
            .maxForce(360)
            .randomOrientation());
    }

    @Override
    public Vector2 calculateSteering(World world) {
        final var mouse = world.getMousePos();
        final var flee = mouse == null ? null : Behaviors.flee(this, mouse, PANIC_RADIUS);
        scared = !isZero(flee);
        return scared ? flee : Behaviors.brake(this, 1.5);
    }

    @Override
    protected Mood getMood() {
        return scared ? Mood.SCARED : super.getMood();
    }

    @Override
    protected Theme getTheme() {
        return Theme.BUNNY;
    }
}
