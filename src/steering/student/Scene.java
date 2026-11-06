package steering.student;

import steering.engine.Robot;

import java.util.List;
import java.util.function.Function;

/**
 * Um cenário de demonstração.
 *
 * @param key          tecla que ativa o cenário
 * @param obstacles    obstáculos que os robôs desviam
 * @param decor        objetos só visuais (os robôs não desviam)
 * @param path         caminho a desenhar/seguir, ou null
 * @param threatRadius raio de pânico desenhado em volta do mouse (0 = nenhum)
 * @param weights      pesos ajustáveis na tela (vazio = cena sem combinação)
 * @param factory      cria os robôs do cenário
 */
public record Scene(char key, String title, String description,
                    List<Obstacle> obstacles, List<Obstacle> decor, Path path, double threatRadius, Weights weights,
                    Function<Scene, List<Robot>> factory) {
}
