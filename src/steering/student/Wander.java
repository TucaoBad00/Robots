package steering.student;

import steering.engine.Robot;
import steering.engine.Vector2;

import java.util.random.RandomGenerator;

import static steering.engine.Vector2.*;

/**
 * WANDER (Reynolds): projeta um círculo à frente do robô e escolhe um alvo sobre a borda dele.
 * A cada quadro o ângulo desse alvo muda um pouco de forma aleatória (passeio aleatório),
 * então o robô faz curvas suaves em vez de virar bruscamente.
 * <p>
 * Precisa guardar o ângulo entre os quadros, por isso é uma classe e não uma função.
 */
public final class Wander {
    private static final RandomGenerator RND = RandomGenerator.getDefault();

    private final double distance; // distância do centro do círculo à frente do robô
    private final double radius;   // raio do círculo
    private final double jitter;   // quanto o ângulo pode variar (rad / √s)
    private double angle = RND.nextDouble(-Math.PI, Math.PI);

    private Vector2 lastCenter;
    private Vector2 lastTarget;

    public Wander(double distance, double radius, double jitter) {
        this.distance = distance;
        this.radius = radius;
        this.jitter = jitter;
    }

    public Vector2 calculate(Robot robot, double secs) {
        // Multiplicar por √dt deixa o passeio aleatório independente da taxa de quadros
        angle += RND.nextDouble(-1, 1) * jitter * Math.sqrt(secs);

        final var direction = robot.getDirection();
        final var heading = Math.atan2(direction.y, direction.x);
        lastCenter = add(robot.getPosition(), multiply(direction, distance));
        lastTarget = add(lastCenter, byAngleSize(heading + angle, radius));
        return Behaviors.seek(robot, lastTarget);
    }

    public double getRadius() {
        return radius;
    }

    /** Centro do círculo no último quadro (para debug). */
    public Vector2 getLastCenter() {
        return lastCenter;
    }

    /** Alvo sobre o círculo no último quadro (para debug). */
    public Vector2 getLastTarget() {
        return lastTarget;
    }
}
