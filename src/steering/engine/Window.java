package steering.engine;

import steering.student.Setup;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public final class Window extends JFrame implements Runnable {
    public static final int INITIAL_WIDTH = 1100;
    public static final int INITIAL_HEIGHT = 820;

    /** Lado da arena quadrada, em unidades de mundo. A origem (0,0) fica no centro. */
    public static final double ARENA_SIZE = 900;

    private final Setup setup;
    private volatile List<Robot> robots;
    private volatile Vector2 clickPos = null;  // coordenadas de MUNDO (no chão)
    private volatile Vector2 mousePos = null;  // coordenadas de MUNDO (no chão)
    private volatile boolean debugMode = true;
    private volatile boolean vectorMode = false; // camada de vetores extra (tecla V)
    private volatile Iso iso = new Iso(INITIAL_WIDTH / 2.0, INITIAL_HEIGHT / 2.0, 0.7);
    private final long startNanos = System.nanoTime();

    private Window() {
        super("Steering Behaviors — robôs isométricos");
        setSize(INITIAL_WIDTH, INITIAL_HEIGHT);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setIgnoreRepaint(true);
        setResizable(true);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                start();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Clique no painel de pesos: ajusta o peso e não vira alvo no chão
                if (Renderer.handleClick(e.getX(), e.getY(), setup.getWeights())) return;
                // O clique na tela é convertido para o ponto do chão correspondente
                clickPos = iso.toWorld(e.getX(), e.getY());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                mousePos = null;
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mousePos = iso.toWorld(e.getX(), e.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                mousePos = iso.toWorld(e.getX(), e.getY());
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                // Painel de pesos: ↑↓ escolhe a linha, ←→ ajusta (Shift = passos de 0,5)
                final var weights = setup.getWeights();
                final var step = e.isShiftDown() ? 0.5 : 0.1;
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP -> weights.select(weights.getSelected() - 1);
                    case KeyEvent.VK_DOWN -> weights.select(weights.getSelected() + 1);
                    case KeyEvent.VK_LEFT -> weights.change(weights.getSelected(), -step);
                    case KeyEvent.VK_RIGHT -> weights.change(weights.getSelected(), step);
                    default -> {
                    }
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                final var key = e.getKeyChar();
                final var weights = setup.getWeights();
                if (Character.toUpperCase(key) == 'D') {
                    debugMode = !debugMode;
                } else if (Character.toUpperCase(key) == 'V') {
                    vectorMode = !vectorMode;
                } else if (Character.toUpperCase(key) == 'R') {
                    weights.reset();
                } else if (key == '+' || key == '=') {
                    weights.change(weights.getSelected(), 0.1);
                } else if (key == '-') {
                    weights.change(weights.getSelected(), -0.1);
                } else if (setup.selectScene(key)) {
                    robots = setup.createRobots();
                    clickPos = null;
                }
            }
        });
        requestFocus();
        this.setup = new Setup();
        this.robots = setup.createRobots();
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new Window().setVisible(true));
    }

    private void start() {
        createBufferStrategy(3);
        final var gameLoop = new Thread(this, "game-loop");
        gameLoop.setDaemon(true);
        gameLoop.start();
    }

    private Rectangle contentArea() {
        final var in = getInsets();
        return new Rectangle(in.left, in.top, getWidth() - in.left - in.right, getHeight() - in.top - in.bottom);
    }

    @Override
    public void run() {
        long prev = System.nanoTime();
        try {
            final var strategy = getBufferStrategy();
            while (true) {
                final long now = System.nanoTime();
                // Limita o dt para a simulação não "pular" quando a janela é arrastada
                final double secs = Math.min((now - prev) / 1e9, 0.05);
                prev = now;

                final var area = contentArea();
                final var frameIso = Renderer.computeIso(area, ARENA_SIZE);
                iso = frameIso;

                final var currentRobots = robots;
                update(currentRobots, secs);

                final var time = (now - startNanos) / 1e9;
                do {
                    do {
                        final var g2d = (Graphics2D) strategy.getDrawGraphics();
                        Renderer.render(g2d, frameIso, area, ARENA_SIZE, currentRobots, setup,
                            clickPos, mousePos, debugMode, vectorMode, time);
                        g2d.dispose();
                    } while (strategy.contentsRestored());
                    strategy.show();
                } while (strategy.contentsLost());
                Toolkit.getDefaultToolkit().sync();

                Thread.sleep(2);
            }
        } catch (InterruptedException e) {
            System.err.println("Interrupted");
        }
        System.exit(0);
    }

    private void update(List<Robot> currentRobots, final double secs) {
        final var mouse = mousePos;
        final var click = clickPos;
        currentRobots.forEach(robot -> robot.update(
            new World(secs, robot, currentRobots, mouse, click, ARENA_SIZE, ARENA_SIZE)
        ));
    }
}
