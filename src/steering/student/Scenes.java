package steering.student;

import steering.engine.Robot;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Os 9 cenários de demonstração, um por tecla. */
public final class Scenes {
    /** Paleta pastel dos robôs. */
    static final Color[] PALETTE = {
        new Color(110, 170, 255), new Color(255, 190, 110), new Color(140, 215, 160),
        new Color(200, 160, 245), new Color(255, 160, 190), new Color(120, 215, 220),
        new Color(250, 215, 110), new Color(180, 190, 255),
    };

    private Scenes() {
    }

    private static Color color(int i) {
        return PALETTE[i % PALETTE.length];
    }

    public static List<Scene> create() {
        final var obstacles = TestMap.obstacles();
        final var circuit = TestMap.circuit();
        final var cones = TestMap.cones(circuit);
        final var park = TestMap.parkDecor();
        final List<Obstacle> none = List.of();

        return List.of(
            new Scene('1', "1 · Seek — o Mensageiro",
                "Clique no chão: o robô acelera até o ponto na velocidade máxima.\n"
                    + "O seek não freia, então ele passa do alvo e fica orbitando em volta.",
                none, none, null, 0, new Weights(),
                s -> List.of(new SeekBot())),

            new Scene('2', "2 · Flee — os Coelhinhos",
                "Aproxime o mouse: quem estiver dentro do raio de pânico (anel vermelho) foge.\n"
                    + "Fora do raio, os robôs freiam e descansam.",
                none, none, null, FleeBot.PANIC_RADIUS, new Weights(),
                s -> List.of(
                    new FleeBot(-120, -60, color(1)), new FleeBot(60, -140, color(2)),
                    new FleeBot(140, 80, color(3)), new FleeBot(-60, 140, color(4)),
                    new FleeBot(0, 0, color(5)))),

            new Scene('3', "3 · Arrive — o Piloto",
                "Clique no chão: o robô vai até o ponto e freia suavemente dentro do raio de frenagem,\n"
                    + "parando exatamente no alvo (compare com o Seek da cena 1).",
                none, none, null, 0, new Weights(),
                s -> List.of(new ArriveBot())),

            new Scene('4', "4 · Wander — os Exploradores",
                "Os robôs passeiam sem destino. O alvo (ponto rosa) anda aleatoriamente sobre um\n"
                    + "círculo projetado à frente de cada um, gerando curvas suaves.",
                none, none, null, 0, new Weights(),
                s -> many(7, i -> new WanderBot(color(i)))),

            new Scene('5', "5 · Obstacle Avoidance — os Radares",
                "Clique para dar um destino (sem clique eles passeiam). A caixa vermelha à frente\n"
                    + "detecta obstáculos; o mais próximo gera força lateral e frenagem.",
                obstacles, none, null, 0,
                new Weights()
                    .add("avoid", "Obstacle Avoidance", 2.0)
                    .add("goal", "Arrive / Wander", 1.0),
                s -> List.of(
                    new AvoidBot(-380, -380, color(0), s.obstacles(), s.weights()),
                    new AvoidBot(380, -380, color(1), s.obstacles(), s.weights()),
                    new AvoidBot(380, 380, color(2), s.obstacles(), s.weights()),
                    new AvoidBot(-380, 380, color(3), s.obstacles(), s.weights()))),

            new Scene('6', "6 · Path Following — os Maquinistas",
                "Os robôs seguem o circuito. Cada um prevê sua posição, projeta no caminho e\n"
                    + "faz seek num ponto um pouco à frente na pista (ponto rosa).",
                none, park, circuit, 0, new Weights(),
                s -> List.of(
                    new PathBot(s.path(), 0, 150, color(0)),
                    new PathBot(s.path(), 700, 190, color(3)),
                    new PathBot(s.path(), 1400, 220, color(5)))),

            new Scene('7', "7 · Combinação A: Presa — os Pintinhos",
                "Prioridade: Avoidance > Flee do mouse > Separation > Wander (pesos no painel).\n"
                    + "Calmos eles passeiam; quando o mouse (predador) chega perto, fogem sem bater.",
                obstacles, none, null, PreyBot.PANIC_RADIUS,
                new Weights()
                    .add("avoid", "Obstacle Avoidance", 3.0)
                    .add("flee", "Flee do mouse", 2.0)
                    .add("separation", "Separation", 1.2)
                    .add("wander", "Wander", 0.6),
                s -> many(8, i -> new PreyBot(color(i), s.obstacles(), s.weights()))),

            new Scene('8', "8 · Combinação B: Corrida — os Pilotos de corrida",
                "Prioridade: Avoidance > Separation > Path Following (pesos no painel).\n"
                    + "Corredores com velocidades diferentes desviam dos cones e se ultrapassam.",
                cones, park, circuit, 0,
                new Weights()
                    .add("avoid", "Obstacle Avoidance", 3.0)
                    .add("separation", "Separation", 1.5)
                    .add("path", "Path Following", 1.0),
                s -> {
                    final var racers = new ArrayList<Robot>();
                    final double[] speeds = {150, 165, 180, 195, 210, 225};
                    for (int i = 0; i < speeds.length; i++) {
                        racers.add(new RacerBot(s.path(), s.obstacles(), s.weights(), i * 55, speeds[i], color(i), i + 1));
                    }
                    return racers;
                }),

            new Scene('9', "9 · Combinação C: o Lobo × as Ovelhinhas",
                "Lobo: Avoidance > Pursuit.   Ovelhas: Avoidance > Evade > Separation > Wander (pesos no painel).\n"
                    + "Presa pega fica tonta por 2,5 s e o predador escolhe outra.",
                obstacles, none, null, 0,
                new Weights()
                    .add("wolfAvoid", "Lobo: Avoidance", 3.0)
                    .add("pursuit", "Lobo: Pursuit", 1.0)
                    .add("sheepAvoid", "Ovelha: Avoidance", 3.0)
                    .add("evade", "Ovelha: Evade", 2.0)
                    .add("separation", "Ovelha: Separation", 1.0)
                    .add("wander", "Ovelha: Wander", 0.7),
                s -> {
                    final var bots = many(6, i -> new RunnerBot(color(i + 1), s.obstacles(), s.weights()));
                    bots.add(new HunterBot(s.obstacles(), s.weights()));
                    return bots;
                })
        );
    }

    private static List<Robot> many(int count, java.util.function.IntFunction<Robot> factory) {
        final var list = new ArrayList<Robot>();
        for (int i = 0; i < count; i++) list.add(factory.apply(i));
        return list;
    }
}
