package steering.engine;

/**
 * Fantasia do robô, escolhida de acordo com o que ele faz. Afeta só o desenho.
 */
public enum Theme {
    /** Sem acessório (antena padrão). */
    PLAIN(false),
    /** Seek — mensageiro: boné e alvo no peito; vai direto ao destino. */
    MESSENGER(true),
    /** Flee — coelhinho: orelhas compridas que murcham quando ele se assusta. */
    BUNNY(false),
    /** Arrive — piloto: óculos de aviador; faz o "pouso" suave no alvo. */
    PILOT(false),
    /** Wander — explorador: chapéu de safári; passeia sem rumo. */
    EXPLORER(true),
    /** Obstacle Avoidance — sensor: radar giratório no lugar da antena. */
    RADAR(true),
    /** Path Following — maquinista: quepe de trem; segue os trilhos. */
    CONDUCTOR(true),
    /** Combinação A — pintinho: topete de penas; foge do cursor em bando. */
    CHICK(true),
    /** Combinação B — corredor: capacete com faixa e número no peito. */
    RACER(true),
    /** Combinação C (predador) — lobo: orelhas pontudas. */
    WOLF(true),
    /** Combinação C (presa) — ovelhinha: lã fofa na cabeça. */
    SHEEP(true);

    private final boolean hidesAntenna;

    Theme(boolean hidesAntenna) {
        this.hidesAntenna = hidesAntenna;
    }

    public boolean hidesAntenna() {
        return hidesAntenna;
    }
}
