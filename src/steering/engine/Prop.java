package steering.engine;

import java.awt.Graphics2D;

/**
 * Objeto de cenário (obstáculo, decoração) desenhado junto com os robôs,
 * ordenado por profundidade para a câmera isométrica.
 */
public interface Prop {
    Vector2 getPosition();

    void draw(Graphics2D g, Iso iso);
}
