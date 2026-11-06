package steering.student;

import java.util.ArrayList;
import java.util.List;

/**
 * Pesos ajustáveis dos comportamentos de uma cena.
 * <p>
 * Os robôs leem o peso a cada quadro com {@link #get(String)}, então qualquer mudança feita
 * no painel da tela (ou pelo teclado) vale na hora para todos os robôs da cena.
 * A ordem das entradas é a ordem de PRIORIDADE usada no {@link Blend}.
 */
public final class Weights {
    public static final double MIN = 0.0;
    public static final double MAX = 5.0;
    public static final double STEP = 0.1;

    /** Um peso: chave usada no código, nome mostrado na tela, valor padrão e valor atual. */
    public static final class Entry {
        private final String key;
        private final String label;
        private final double defaultValue;
        private volatile double value;

        private Entry(String key, String label, double defaultValue) {
            this.key = key;
            this.label = label;
            this.defaultValue = defaultValue;
            this.value = defaultValue;
        }

        public String getLabel() { return label; }
        public double getValue() { return value; }
        public double getDefaultValue() { return defaultValue; }
    }

    private final List<Entry> entries = new ArrayList<>();
    private volatile int selected = 0;

    /** Adiciona um peso. Chame na ordem de prioridade. */
    public Weights add(String key, String label, double defaultValue) {
        entries.add(new Entry(key, label, defaultValue));
        return this;
    }

    /** Valor atual do peso com essa chave. */
    public double get(String key) {
        for (var entry : entries) {
            if (entry.key.equals(key)) return entry.value;
        }
        throw new IllegalArgumentException("Peso desconhecido: " + key);
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int getSelected() {
        return selected;
    }

    public void select(int index) {
        if (entries.isEmpty()) return;
        selected = Math.floorMod(index, entries.size());
    }

    /** Soma {@code delta} ao peso da linha {@code index}, sempre entre MIN e MAX. */
    public void change(int index, double delta) {
        if (index < 0 || index >= entries.size()) return;
        final var entry = entries.get(index);
        final var rounded = Math.round((entry.value + delta) * 10.0) / 10.0;
        entry.value = Math.clamp(rounded, MIN, MAX);
    }

    /** Volta todos os pesos para os valores padrão. */
    public void reset() {
        for (var entry : entries) entry.value = entry.defaultValue;
    }
}
