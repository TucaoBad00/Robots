package steering.engine;

import java.util.List;

import static steering.engine.Robot.distance;

public final class World {
    private final Robot current;
    private final List<Robot> robots;
    private final Vector2 mousePos;   // coordenadas de mundo (no chão); pode ser null
    private final Vector2 clickPos;   // coordenadas de mundo (no chão); pode ser null
    private final double secs;

    private final double width;
    private final double height;

    public World(double secs,
                 Robot current,
                 List<Robot> robots,
                 Vector2 mousePos,
                 Vector2 clickPos,
                 double width,
                 double height) {
        this.current = current;
        this.robots = robots;
        this.mousePos = mousePos;
        this.clickPos = clickPos;
        this.secs = secs;
        this.width = width;
        this.height = height;
    }

    public Vector2 getMousePos() {
        return mousePos == null ? null : mousePos.clone();
    }

    public Vector2 getClickPos() {
        return clickPos == null ? null : clickPos.clone();
    }

    public List<Robot> getNeighbors() {
        return this.robots.stream()
            .filter(c -> c != current)
            .toList();
    }

    public List<Robot> getNeighbors(int radius) {
        return this.robots.stream()
            .filter(c -> c != current)
            .filter(c -> distance(current, c) <= radius)
            .toList();
    }

    public double getSecs() {
        return secs;
    }

    /** Largura da arena em unidades de mundo. */
    public double getWidth() { return width; }
    /** Altura (profundidade) da arena em unidades de mundo. */
    public double getHeight() { return height; }
}
