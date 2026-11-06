package steering.student;

import steering.engine.Robot;
import steering.engine.Vector2;

import java.awt.Color;

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
 * <p>
 * Cada força também é registrada para a camada de vetores (tecla V): o que o comportamento
 * pediu (já com peso) e o que realmente coube no orçamento.
 */
public final class Blend {
    private final Robot robot;
    private final double budget;
    private final Vector2 total = vec2();
    private boolean full = false;

    public Blend(Robot robot) {
        this.robot = robot;
        this.budget = robot.getMaxForce();
    }

    /**
     * Adiciona um comportamento (chame na ordem de prioridade).
     *
     * @param name   nome mostrado na legenda da camada de vetores
     * @param force  força calculada pelo comportamento
     * @param weight peso (vem do painel de pesos)
     */
    public Blend add(String name, Vector2 force, double weight) {
        if (force == null || force.isZero()) return this;

        final var weighted = multiply(force, weight);
        final var accepted = weighted.clone();
        final var remaining = budget - total.size();
        if (full || remaining <= 0) {
            full = true;
            accepted.set(0, 0);           // não sobrou orçamento: este comportamento fica de fora
        } else if (weighted.size() > remaining) {
            accepted.resize(remaining);   // entra só o pedaço que cabe
            full = true;
        }
        total.add(accepted);
        robot.debugForce(name, weighted, accepted, colorOf(name));
        return this;
    }

    public Vector2 result() {
        return total.clone();
    }

    /** Cor fixa por comportamento, para a legenda da camada de vetores ficar consistente. */
    private static Color colorOf(String name) {
        return switch (name) {
            case "Obstacle Avoidance" -> new Color(225, 60, 70);
            case "Flee", "Evade" -> new Color(140, 80, 220);
            case "Separation" -> new Color(0, 150, 140);
            case "Wander" -> new Color(220, 70, 170);
            case "Path Following", "Arrive" -> new Color(215, 150, 0);
            case "Pursuit" -> new Color(150, 70, 30);
            default -> new Color(90, 90, 110);
        };
    }
}
