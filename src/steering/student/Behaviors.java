package steering.student;

import steering.engine.Robot;
import steering.engine.Vector2;

import java.awt.Color;
import java.util.List;

import static steering.engine.Vector2.*;

/**
 * Steering behaviors como funções puras.
 * Cada função recebe o agente (e o que mais precisar) e devolve uma FORÇA de steering.
 * A engine trunca essa força em maxForce e integra a física.
 * <p>
 * Ideia central (Reynolds): steering = (velocidadeDesejada − velocidadeAtual) × RESPONSE.
 */
public final class Behaviors {
    /**
     * Ganho de resposta (1/s). A engine integra v += F·dt/m, então "desejada − atual" sozinho
     * levaria ~1 s para corrigir a velocidade (robôs lentos e oscilando). Multiplicar por 6
     * equivale a corrigir em ~0,17 s; o resultado continua limitado por maxForce na engine.
     */
    public static final double RESPONSE = 6.0;

    // Cores dos pontos da camada de vetores (tecla V)
    private static final Color PREDICTED_COLOR = new Color(200, 30, 60);
    private static final Color FUTURE_COLOR = new Color(20, 150, 90);
    private static final Color PROJECTION_COLOR = new Color(110, 110, 130);
    private static final Color PATH_TARGET_COLOR = new Color(220, 60, 150);

    private Behaviors() {
    }

    // =====================================================================
    // Comportamentos simples
    // =====================================================================

    /** SEEK: vai em direção ao alvo na velocidade máxima. */
    public static Vector2 seek(Robot robot, Vector2 target) {
        final var desired = subtract(target, robot.getPosition());
        if (desired.isZero()) return vec2();
        desired.resize(robot.getMaxSpeed());
        robot.debugDesired(desired);  // camada V: velocidade desejada
        return desired.subtract(robot.getVelocity()).multiply(RESPONSE);
    }

    /**
     * FLEE: o oposto do seek, foge do ponto de ameaça.
     *
     * @param panicRadius só foge se a ameaça estiver mais perto que isso (0 = sempre foge).
     */
    public static Vector2 flee(Robot robot, Vector2 threat, double panicRadius) {
        final var desired = subtract(robot.getPosition(), threat);
        final var distance = desired.size();
        if (panicRadius > 0 && distance > panicRadius) return vec2();
        if (desired.isZero()) desired.set(robot.getDirection().x, robot.getDirection().y);
        desired.resize(robot.getMaxSpeed());
        robot.debugDesired(desired);  // camada V: velocidade desejada
        return desired.subtract(robot.getVelocity()).multiply(RESPONSE);
    }

    /**
     * ARRIVE: igual ao seek, mas reduz a velocidade desejada linearmente
     * dentro do raio de desaceleração, parando suavemente no alvo.
     */
    public static Vector2 arrive(Robot robot, Vector2 target, double slowingRadius) {
        final var toTarget = subtract(target, robot.getPosition());
        final var distance = toTarget.size();
        if (distance < 2) {
            robot.debugDesired(vec2());  // chegou: a velocidade desejada é zero
            return negate(robot.getVelocity()).multiply(RESPONSE); // anula a velocidade restante
        }

        final var speed = robot.getMaxSpeed() * Math.min(1.0, distance / slowingRadius);
        final var desired = toTarget.resize(speed);
        robot.debugDesired(desired);  // camada V: velocidade desejada
        return desired.subtract(robot.getVelocity()).multiply(RESPONSE);
    }

    /** Freio: força contrária à velocidade. Usado quando o robô não tem nada a fazer. */
    public static Vector2 brake(Robot robot, double strength) {
        return multiply(robot.getVelocity(), -strength);
    }

    // WANDER está na classe Wander, porque precisa guardar estado (o ângulo) entre quadros.

    // =====================================================================
    // Comportamentos com outros agentes
    // =====================================================================

    /** PURSUIT: faz seek na posição FUTURA estimada do alvo. */
    public static Vector2 pursuit(Robot robot, Robot target) {
        final var predicted = predictPosition(robot, target);
        robot.debugPoint("Posição prevista da presa", predicted, target.getPosition(), PREDICTED_COLOR);
        return seek(robot, predicted);
    }

    /** EVADE: faz flee da posição FUTURA estimada da ameaça. */
    public static Vector2 evade(Robot robot, Robot threat, double panicRadius) {
        if (Robot.distance(robot, threat) > panicRadius) return vec2();
        final var predicted = predictPosition(robot, threat);
        robot.debugPoint("Posição prevista da ameaça", predicted, threat.getPosition(), PREDICTED_COLOR);
        return flee(robot, predicted, 0);
    }

    /** Tempo de previsão proporcional à distância: quanto mais longe, mais à frente prevemos. */
    private static Vector2 predictPosition(Robot robot, Robot other) {
        final var distance = Robot.distance(robot, other);
        final var time = distance / (robot.getMaxSpeed() + other.getSpeed());
        return add(other.getPosition(), multiply(other.getVelocity(), time));
    }

    /** SEPARATION: empurra para longe dos vizinhos próximos; mais forte quanto mais perto. */
    public static Vector2 separation(Robot robot, List<Robot> neighbors, double radius) {
        final var force = vec2();
        for (var other : neighbors) {
            final var away = subtract(robot.getPosition(), other.getPosition());
            final var distance = away.size();
            if (distance < 1e-6 || distance > radius) continue;
            force.add(away.resize(1.0 - distance / radius));
        }
        return force.multiply(robot.getMaxForce());
    }

    // =====================================================================
    // Comportamentos avançados
    // =====================================================================

    /** Comprimento da "caixa de detecção" à frente do robô: cresce com a velocidade. */
    public static double detectionLength(Robot robot, double minLength) {
        return minLength + (robot.getSpeed() / robot.getMaxSpeed()) * minLength;
    }

    /** Folga (entre as bordas) a partir da qual um obstáculo próximo começa a empurrar o robô. */
    public static final double CONTACT_MARGIN = 12;

    /**
     * OBSTACLE AVOIDANCE (Reynolds / Buckland), em duas partes.
     * <p>
     * PARTE 1 — zona de contato (qualquer direção): obstáculos a menos de {@link #CONTACT_MARGIN}
     * da borda do robô empurram para fora e anulam a parte da velocidade que vai contra eles.
     * Assim o robô desliza pela borda em vez de bater, mesmo quando o obstáculo está ao lado
     * (onde a caixa da parte 2 não enxerga).
     * <p>
     * PARTE 2 — caixa de detecção (à frente):
     * <ol>
     *   <li>Projeta uma caixa à frente do robô (largura = diâmetro do robô).</li>
     *   <li>Leva cada obstáculo para o espaço local do robô (eixo "frente" e eixo "lado").</li>
     *   <li>Descarta os que estão atrás, longe demais ou fora da largura da caixa.</li>
     *   <li>Escolhe o mais próximo que cruza a caixa.</li>
     *   <li>Gera uma força lateral (para o lado oposto ao obstáculo) e uma de frenagem;
     *       as duas são maiores quanto mais perto o obstáculo estiver.</li>
     * </ol>
     */
    public static Vector2 avoidObstacles(Robot robot, List<Obstacle> obstacles, double minLength) {
        final var position = robot.getPosition();
        final var velocity = robot.getVelocity();
        final var forward = robot.getDirection();
        final var side = perp(forward);
        final var boxLength = detectionLength(robot, minLength);
        final var force = vec2();

        // ---------- PARTE 1: zona de contato ----------
        for (var obstacle : obstacles) {
            final var away = subtract(position, obstacle.getPosition());
            final var distance = away.size();
            final var gap = distance - (obstacle.getRadius() + robot.getRadius()); // folga entre as bordas
            if (gap >= CONTACT_MARGIN) continue;

            final var normal = distance < 1e-6 ? negate(forward) : away.normalize(); // aponta para fora
            // Empurrão para fora: 0 na borda da margem, maxForce encostando, até 2× se sobrepondo
            final var push = Math.clamp((CONTACT_MARGIN - gap) / CONTACT_MARGIN, 0.0, 2.0);
            force.add(multiply(normal, push * robot.getMaxForce()));
            // Anula só a velocidade que vai CONTRA o obstáculo; a velocidade ao longo da borda continua
            final var inward = -velocity.dot(normal);
            if (inward > 0) force.add(multiply(normal, inward * RESPONSE));
        }

        // ---------- PARTE 2: caixa de detecção ----------
        Obstacle closest = null;
        double closestHit = Double.MAX_VALUE;
        double closestLocalX = 0;
        double closestLocalY = 0;
        double closestRadius = 0;

        for (var obstacle : obstacles) {
            final var toObstacle = subtract(obstacle.getPosition(), position);
            final var expanded = obstacle.getRadius() + robot.getRadius();

            final var localX = toObstacle.dot(forward); // distância à frente
            final var localY = toObstacle.dot(side);    // deslocamento lateral
            if (localX < 0 || localX - expanded > boxLength) continue;
            if (Math.abs(localY) >= expanded) continue;

            // Ponto onde o eixo "frente" cruza o círculo expandido
            final var hit = localX - Math.sqrt(expanded * expanded - localY * localY);
            if (hit < closestHit) {
                closestHit = hit;
                closest = obstacle;
                closestLocalX = localX;
                closestLocalY = localY;
                closestRadius = expanded;
            }
        }

        if (closest == null) return force;

        // Quanto mais perto, maior o multiplicador (1 a 2)
        final var proximity = 1.0 + (boxLength - Math.clamp(closestLocalX, 0, boxLength)) / boxLength;
        final var sideSign = closestLocalY > 0 ? -1.0 : 1.0;
        // Força lateral mínima de 50%: um obstáculo que só "raspa" na caixa também é evitado com firmeza
        final var lateral = 0.5 + 0.5 * (closestRadius - Math.abs(closestLocalY)) / closestRadius;
        force.add(multiply(side, sideSign * lateral * proximity * robot.getMaxForce()));

        // Frenagem proporcional à velocidade: freia quem vem rápido, mas não trava quem já está devagar
        final var speedRatio = robot.getSpeed() / robot.getMaxSpeed();
        final var braking = 0.5 * robot.getMaxForce() * (proximity - 1.0) * speedRatio;
        force.add(multiply(forward, -braking));

        return force;
    }

    /**
     * Ponto-alvo do PATH FOLLOWING:
     * 1. prevê onde o robô estará daqui a pouco;
     * 2. projeta essa previsão no caminho (ponto mais próximo);
     * 3. avança {@code lookAhead} unidades ao longo do caminho a partir dali.
     */
    public static Vector2 pathTarget(Robot robot, Path path, double predictDistance, double lookAhead) {
        final var future = add(robot.getPosition(), multiply(robot.getDirection(), predictDistance));
        final var projection = path.project(future);
        final var target = path.pointAt(projection.distanceAlong() + lookAhead);
        // camada V: previsão → projeção no caminho → alvo
        robot.debugPoint("Previsão (path)", future, robot.getPosition(), FUTURE_COLOR);
        robot.debugPoint("Projeção no caminho", projection.point(), future, PROJECTION_COLOR);
        robot.debugPoint("Alvo no caminho", target, projection.point(), PATH_TARGET_COLOR);
        return target;
    }

    /** PATH FOLLOWING: seek no ponto-alvo que "anda" sempre à frente sobre o caminho. */
    public static Vector2 followPath(Robot robot, Path path, double predictDistance, double lookAhead) {
        return seek(robot, pathTarget(robot, path, predictDistance, lookAhead));
    }
}
