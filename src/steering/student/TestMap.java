package steering.student;

import steering.engine.Vector2;
import steering.engine.Window;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

import static steering.engine.Vector2.*;

/** Mapa de testes: obstáculos espalhados pela arena e um circuito fechado. */
public final class TestMap {
    private static final RandomGenerator RND = RandomGenerator.getDefault();
    private static final Color LAVENDER = new Color(176, 156, 230);
    private static final Color TEAL = new Color(110, 200, 200);
    private static final Color PEACH = new Color(255, 180, 150);
    private static final Color PINK = new Color(245, 150, 190);

    private TestMap() {
    }

    /** Campo de obstáculos usado nos cenários de desvio e nas combinações. */
    public static List<Obstacle> obstacles() {
        return List.of(
            Obstacle.bush(0, 0, 62),
            Obstacle.pillar(-200, -140, 34, LAVENDER),
            Obstacle.pillar(190, -170, 38, TEAL),
            Obstacle.bush(-170, 190, 46),
            Obstacle.pillar(200, 170, 34, PEACH),
            Obstacle.pillar(10, -270, 28, PINK),
            Obstacle.bush(-300, -20, 36),
            Obstacle.pillar(300, 20, 30, LAVENDER),
            Obstacle.bush(60, 270, 32),
            Obstacle.pillar(-60, 330, 24, TEAL),
            Obstacle.pillar(-330, -310, 26, PEACH),
            Obstacle.bush(330, -320, 30),
            Obstacle.pillar(340, 330, 26, PINK),
            Obstacle.bush(-340, 320, 28)
        );
    }

    /** Sorteia uma posição na arena que não esteja em cima de nenhum obstáculo. */
    public static Vector2 freeSpot(List<Obstacle> obstacles, double clearance) {
        final var half = Window.ARENA_SIZE / 2 - 40;
        while (true) {
            final var p = vec2(RND.nextDouble(-half, half), RND.nextDouble(-half, half));
            final var free = obstacles.stream()
                .allMatch(o -> distance(p, o.getPosition()) > o.getRadius() + clearance);
            if (free) return p;
        }
    }

    /** Circuito octogonal em volta do centro da arena. */
    public static Path circuit() {
        return new Path(38, List.of(
            vec2(-300, -300), vec2(0, -345), vec2(300, -300), vec2(345, 0),
            vec2(300, 300), vec2(0, 345), vec2(-300, 300), vec2(-345, 0)
        ));
    }

    /** Cones colocados SOBRE o circuito, alternando de lado, para a combinação "Corrida". */
    public static List<Obstacle> cones(Path path) {
        final var cones = new ArrayList<Obstacle>();
        final var points = path.getPoints();
        for (int i = 1; i < points.size(); i += 2) {
            final var a = points.get(i);
            final var b = points.get((i + 1) % points.size());
            final var middle = multiply(add(a, b), 0.5);
            final var offset = multiply(perp(normalize(subtract(b, a))), (i % 4 == 1) ? 12 : -12);
            final Vector2 p = add(middle, offset);
            cones.add(Obstacle.cone(p.x, p.y));
        }
        return cones;
    }

    /** Decoração no meio do circuito (fora da pista). */
    public static List<Obstacle> parkDecor() {
        return List.of(
            Obstacle.bush(0, 0, 70),
            Obstacle.pillar(-130, -100, 24, LAVENDER),
            Obstacle.pillar(140, 110, 24, PEACH),
            Obstacle.bush(120, -130, 34),
            Obstacle.bush(-120, 140, 34)
        );
    }
}
