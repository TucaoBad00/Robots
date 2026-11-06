package steering.engine;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

import static steering.engine.Vector2.*;
import static java.lang.Math.toRadians;

/**
 * Agente da simulação: um robozinho desenhado em câmera isométrica.
 * Cada tipo de robô só precisa implementar {@link #calculateSteering(World)}.
 * <p>
 * Física a cada quadro:
 * steering → truncado em maxForce → aceleração = F·dt / m → velocidade truncada em maxSpeed.
 */
public abstract class Robot implements Cloneable {
    private static final RandomGenerator RND = RandomGenerator.getDefault();

    /** Raio de colisão do robô em tamanho 1 (unidades de mundo). */
    public static final double BASE_RADIUS = 18;

    private final Color color;
    private final double mass;
    private final double maxForce;
    private final double maxSpeed;
    private final double size;
    private final double phase; // deixa a animação de cada robô fora de sincronia
    private Vector2 position;
    private Vector2 velocity;
    private double heading;     // última direção conhecida (mantida quando o robô para)

    // Último steering truncado, usado no desenho de debug
    private Vector2 lastSteering = vec2();

    public Robot(Consumer<Settings> settings) {
        var cs = new Settings();
        settings.accept(cs);
        this.color = cs.color;
        this.position = cs.position.clone();
        this.velocity = Vector2.byAngle(cs.orientation);
        this.heading = cs.orientation;
        this.mass = cs.mass;
        this.maxForce = cs.maxForce;
        this.maxSpeed = cs.maxSpeed;
        this.size = cs.size;
        this.phase = RND.nextDouble(0, 1000);
    }

    // ---- Accessors ----
    public Vector2 getPosition() { return position.clone(); }
    public Vector2 getVelocity() { return velocity.clone(); }
    public double getSpeed() { return velocity.size(); }

    /** Vetor unitário da direção. Se o robô estiver parado, mantém a última direção. */
    public Vector2 getDirection() { return velocity.size() > 1e-3 ? normalize(velocity) : byAngle(heading); }
    public double getMass() { return mass; }
    public double getMaxForce() { return maxForce; }
    public double getMaxSpeed() { return maxSpeed; }
    public Color getColor() { return color; }
    public double getSize() { return size; }
    public double getRadius() { return BASE_RADIUS * size; }

    public abstract Vector2 calculateSteering(World world);

    /** Expressão do robô. Subclasses podem sobrescrever (ex.: assustado quando foge). */
    protected Mood getMood() {
        return getSpeed() < 6 ? Mood.SLEEPY : Mood.HAPPY;
    }

    /** Fantasia do robô, ligada ao comportamento dele. */
    protected Theme getTheme() {
        return Theme.PLAIN;
    }

    /** Texto curto desenhado no peito (ex.: número do corredor), ou null. */
    protected String getBadge() {
        return null;
    }

    /**
     * Gancho opcional para desenhar informação de debug do comportamento
     * (círculo do wander, caixa de detecção, alvo...). Chamado só com o debug ligado.
     */
    protected void drawDebug(Graphics2D g, Iso iso) {
    }

    void update(World world) {
        final var steeringForce = calculateSteering(world);
        if (steeringForce == null) {
            lastSteering = vec2();
            return;
        }

        lastSteering = truncate(steeringForce, maxForce);

        final var impulse = multiply(lastSteering, world.getSecs()); // F * dt
        final var acceleration = divide(impulse, mass);              // (F*dt)/m
        velocity = truncate(add(velocity, acceleration), maxSpeed);
        position = add(position, multiply(velocity, world.getSecs()));
        if (velocity.size() > 2) heading = velocity.getAngle();

        final var w = world.getWidth() / 2.0;
        final var h = world.getHeight() / 2.0;
        if (position.x < -(w + 20)) position.x =  w;
        if (position.x >  (w + 20)) position.x = -w;
        if (position.y < -(h + 20)) position.y =  h;
        if (position.y >  (h + 20)) position.y = -h;
    }

    // -----------------------------------------------------
    // Drawing (Java2D, câmera isométrica)
    // -----------------------------------------------------
    void draw(Graphics2D g, Iso iso, double time) {
        final var speed01 = Math.min(1.0, getSpeed() / Math.max(1.0, maxSpeed));
        RobotRenderer.draw(g, iso, position, heading, color, size, getMood(), getTheme(), getBadge(),
            time, phase, speed01);
    }

    void drawDebugOverlay(Graphics2D g, Iso iso) {
        drawDebug(g, iso);
        drawGroundArrow(g, iso, position, velocity, 0.35, new Color(40, 90, 255));      // AZUL: velocidade
        drawGroundArrow(g, iso, position, lastSteering, 0.2, new Color(255, 90, 20));   // LARANJA: steering
    }

    private static void drawGroundArrow(Graphics2D g0, Iso iso, Vector2 origin, Vector2 vector, double scale, Color color) {
        if (Vector2.isZero(vector) || vector.size() < 1) return;

        final var tip = add(origin, multiply(vector, scale));
        final var dir = normalize(vector);
        final var left = add(tip, multiply(rotate(dir, toRadians(150)), 10));
        final var right = add(tip, multiply(rotate(dir, toRadians(-150)), 10));

        final var g = (Graphics2D) g0.create();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
        g.setColor(color);
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        final var o = iso.toScreen(origin, 2);
        final var t = iso.toScreen(tip, 2);
        g.draw(new Line2D.Double(o, t));
        g.draw(new Line2D.Double(t, iso.toScreen(left, 2)));
        g.draw(new Line2D.Double(t, iso.toScreen(right, 2)));
        g.dispose();
    }

    @Override
    public Robot clone() {
        try {
            final var other = (Robot) super.clone();
            other.position = position.clone();
            other.velocity = velocity.clone();
            return other;
        } catch (CloneNotSupportedException ignored) {
            return null;
        }
    }

    public static double distance(Robot robot1, Robot robot2) {
        return Vector2.distance(robot1.position, robot2.position);
    }
}
