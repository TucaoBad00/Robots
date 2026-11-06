package steering.student;

import steering.engine.Robot;
import steering.engine.Vector2;

import static steering.engine.Vector2.multiply;
import static steering.engine.Vector2.vec2;

/**
 * Combina comportamentos com "soma ponderada truncada com prioridade" (Buckland).
 * <p>
 * Os comportamentos são adicionados em ORDEM DE PRIORIDADE, cada um com seu peso.
 * Cada força entra enquanto houver "orçamento" (maxForce do agente). Quando o orçamento
 * acaba, os comportamentos de menor prioridade são ignorados naquele quadro.
 * <p>
 * Assim, se o robô está prestes a bater num obstáculo, o desvio usa toda a força
 * disponível e o wander não atrapalha; quando não há perigo, o desvio vale zero e
 * todo o orçamento fica para os outros comportamentos.
 */
public final class Blend {
    private final double budget;
    private final Vector2 total = vec2();
    private boolean full = false;

    public Blend(Robot robot) {
        this.budget = robot.getMaxForce();
    }

    public Blend add(Vector2 force, double weight) {
        if (full || force == null || force.isZero()) return this;

        final var weighted = multiply(force, weight);
        final var remaining = budget - total.size();
        if (remaining <= 0) {
            full = true;
            return this;
        }
        if (weighted.size() > remaining) {
            weighted.resize(remaining);
            full = true;
        }
        total.add(weighted);
        return this;
    }

    public Vector2 result() {
        return total.clone();
    }
}
