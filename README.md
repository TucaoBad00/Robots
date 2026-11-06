# Steering Behaviors — Robozinhos Isométricos

Trabalho prático de **Steering Behaviors** (Inteligência Artificial e Ilusão de Inteligência em Jogos, PUCPR).

O projeto parte do template de engine fornecido pela professora e mantém a arquitetura dele:
cada agente é uma subclasse de `Robot` que só precisa implementar `calculateSteering(World)`, ou seja,
devolver **um vetor de força**. A engine trunca essa força, integra a física e desenha.
Os agentes são **robozinhos flutuantes** vistos por uma **câmera isométrica**.

## Como executar

Requer **JDK 21+**.

```bat
run.bat
```

Ou manualmente:

```bash
javac -encoding UTF-8 -d out -sourcepath src src/steering/engine/Window.java
java -cp out steering.engine.Window
```

### Controles

| Tecla / ação | Efeito |
|---|---|
| `1` … `9` | Troca de cenário (apertar de novo reinicia) |
| `D` | Liga/desliga o desenho de debug |
| `V` | Liga/desliga a camada de vetores (independente do `D`) |
| Clique no chão | Define o alvo (Seek, Arrive, Obstacle Avoidance) |
| Mover o mouse | O cursor é a "ameaça" nos cenários de Flee e Presa |
| `−` / `+` no painel de pesos | Ajusta o peso daquela linha em 0,1 (cenas 5, 7, 8 e 9) |
| `↑` `↓` | Escolhe a linha do painel de pesos |
| `←` `→` (ou `−` `+`) | Ajusta o peso escolhido em 0,1 (com `Shift`, em 0,5) |
| `R` ou botão **Restaurar** | Volta todos os pesos da cena para o padrão |

**Debug (`D`):** seta **azul** = velocidade, seta **laranja** = força de steering aplicada.
Cada comportamento também desenha suas informações: círculo do wander, caixa de detecção, alvo no caminho etc.

**Camada de vetores (`V`):** mostra o que fica "escondido" dentro dessas duas setas. Uma legenda no
canto inferior esquerdo lista o que está na tela.
- **Velocidade desejada** (verde tracejada) nos robôs sem combinação, na mesma escala da seta azul.
  A seta laranja é a correção `(desejada − azul) × 6`.
- **Força de cada comportamento** nas combinações (cenas 5, 7, 8 e 9), cada um com sua cor e na
  escala da seta laranja. Se o orçamento do `Blend` cortou parte da força, o pedido aparece
  tracejado por trás. Por isso, dá para ver os pesos e a prioridade agindo enquanto mexe no painel.
- **Pontos calculados:** a posição prevista da presa (Pursuit) e da ameaça (Evade), e a cadeia
  previsão → projeção no caminho → alvo do Path Following.

Esses vetores são registrados pelos próprios comportamentos enquanto calculam a força
(`robot.debugDesired`, `robot.debugPoint` em `Behaviors`, `robot.debugForce` no `Blend`) e são
apagados a cada quadro.

## Cenários

Cada robô usa uma **fantasia que combina com o que ele faz**, para dar para reconhecer o comportamento só de olhar.

| Tecla | Cenário | Robô temático | Comportamentos |
|---|---|---|---|
| 1 | Seek | **Mensageiro**: boné e alvo no peito, vai direto ao destino | Seek no clique |
| 2 | Flee | **Coelhinhos**: orelhas que murcham quando se assustam | Flee do mouse (com raio de pânico) + freio |
| 3 | Arrive | **Piloto**: óculos de aviador, faz o "pouso" suave | Arrive no clique |
| 4 | Wander | **Exploradores**: chapéu de safári, passeiam sem rumo | Wander |
| 5 | Obstacle Avoidance | **Radares**: prato de radar girando no lugar da antena | Avoidance + (Arrive no clique ou Wander) |
| 6 | Path Following | **Maquinistas**: quepe de trem, seguem os trilhos | Path Following no circuito |
| 7 | **Combinação A — Presa** | **Pintinhos**: topete de penas, fogem do cursor em bando | Avoidance + Flee + Separation + Wander |
| 8 | **Combinação B — Corrida** | **Pilotos de corrida**: capacete com faixa e número no peito | Avoidance + Separation + Path Following |
| 9 | **Combinação C — Predador × Presa** | **Lobo × Ovelhinhas**: orelhas pontudas e cara brava × lã fofinha | Pursuit/Evade + Avoidance + Separation + Wander |

Os temas são só visuais. Cada agente sobrescreve `getTheme()` (e o corredor também `getBadge()`
para o número), e o [`RobotRenderer`](src/steering/engine/RobotRenderer.java) desenha o acessório.

---

## Física da engine (base de tudo)

A cada quadro, para cada agente, a engine faz:

```
F   = truncate(calculateSteering(world), maxForce)
v   = truncate(v + F·dt / massa, maxSpeed)
pos = pos + v·dt
```

### Ganho de resposta

A fórmula clássica de Reynolds é `steering = velocidadeDesejada − velocidadeAtual`.
Como a engine multiplica a força por `dt`, usar essa diferença diretamente faz o robô levar
cerca de 1 s para corrigir a velocidade: ele fica lento e o Arrive oscila em volta do alvo.
Por isso os comportamentos baseados em velocidade desejada multiplicam o resultado por
`RESPONSE = 6` (tempo de resposta de ~0,17 s). O resultado continua limitado por `maxForce`,
então a física continua "honesta": aceleração e curvas têm limite.

---

## Comportamentos simples

Todos ficam em [`Behaviors.java`](src/steering/student/Behaviors.java), exceto o Wander, que fica em [`Wander.java`](src/steering/student/Wander.java).

### Seek (buscar)
```
desejada = normalize(alvo − posição) · maxSpeed
steering = (desejada − velocidade) · RESPONSE
```
O robô sempre quer ir na velocidade máxima em direção ao alvo. Ele **não freia**: passa do ponto,
faz a curva e volta, ficando em órbita em volta do alvo. Isso fica bem visível na cena 1.

### Flee (fugir)
É o Seek invertido: `desejada = normalize(posição − ameaça) · maxSpeed`.
Foi adicionado um **raio de pânico**: fora dele a força é zero. Na cena 2, quando não há ameaça,
o robô aplica um **freio** (`−velocidade · k`) para não deslizar para sempre, já que na engine
força zero significa manter a velocidade.

### Arrive (chegar)
```
distância = |alvo − posição|
velocidade desejada = maxSpeed · min(1, distância / raioDeFrenagem)
```
Fora do raio de frenagem ele é igual ao Seek. Dentro do raio, a velocidade desejada cai
linearmente até zero, e o robô para exatamente no alvo. Com o ganho de resposta, o sistema fica
**superamortecido** e não passa do alvo (cena 3: o anel laranja é o raio de frenagem).

### Wander (vagar)
1. Projeta um **círculo** a uma distância fixa à frente do robô.
2. Mantém um ângulo sobre a borda desse círculo, que muda um pouco a cada quadro (passeio aleatório).
3. Faz Seek no ponto da borda.

Como o alvo muda pouco a cada quadro e está sempre **à frente**, o robô faz curvas suaves
e orgânicas em vez de tremer. A variação do ângulo é `jitter · √dt`, o que deixa o comportamento
igual em qualquer taxa de quadros. Parâmetros usados: distância 70, raio 35, jitter 3,5.

---

## Comportamentos avançados

### Obstacle Avoidance (desvio de obstáculos)
Baseado em Reynolds e na implementação de Buckland, com uma **zona de contato** extra. A força final
é a soma de duas partes.

**Parte 1 — zona de contato (em qualquer direção).** A caixa da parte 2 só olha para a frente, mas um
robô que passa raspando tem o obstáculo **ao lado**. Por isso, todo obstáculo cuja borda esteja a
menos de `CONTACT_MARGIN = 12` unidades da borda do robô gera:
- um **empurrão para fora**, de 0 na borda da margem até `maxForce` encostando (até 2× se sobrepondo);
- uma força que **anula só a parte da velocidade que vai contra o obstáculo** (`−v·n`, na direção da
  normal). A velocidade ao longo da borda é mantida, então o robô **desliza** pelo obstáculo em vez de
  bater e voltar.

**Parte 2 — caixa de detecção (à frente).**
1. Uma **caixa de detecção** se estende à frente do robô, com a largura do robô e um comprimento
   que **cresce com a velocidade** (`min + (v/vmax)·min`). Quanto mais rápido, mais longe ele olha.
2. Cada obstáculo é levado para o **espaço local** do robô:
   `localX = (o − p)·frente`, `localY = (o − p)·lado`.
3. São descartados os obstáculos atrás (`localX < 0`), longe demais ou fora da largura
   (`|localY| ≥ raioObstáculo + raioRobô`).
4. Entre os que sobram, o escolhido é o de **interseção mais próxima** com o eixo da caixa.
5. A resposta tem duas forças, multiplicadas por um fator de proximidade (de 1 a 2):
   - **força lateral** para o lado oposto ao obstáculo, com **no mínimo 50%** da intensidade, para que
     um obstáculo que só "raspa" na caixa também seja evitado com firmeza;
   - **frenagem** proporcional à velocidade atual: freia quem vem rápido, mas não trava um robô
     que já está devagar na frente do obstáculo.

**Por que a zona de contato foi necessária.** Na primeira versão, um obstáculo ao lado do robô
(`localX < 0`) ou quase fora da largura da caixa gerava força zero, enquanto o Seek/Path Following
continuavam puxando o robô para ele. O único socorro era um empurrão quando os corpos já se
sobrepunham, o que causava um vai-e-vem: o robô perdia velocidade e ficava girando colado no obstáculo.
Medição em 3 simulações de 60 s por cena, contando qualquer encostar de corpo:

| Cena | Contato antes | Parado encostado antes | Contato depois | Parado encostado depois |
|---|---|---|---|---|
| 5 | 5,8% | 8,2% | 0% | 0% |
| 7 | 2,3% | 0,6% | 0% | 0% |
| 8 | 3,0% | 11,3% | 0% | 0% |
| 9 | 2,6% | 3,6% | 0% | 0% |

(% do tempo de cada robô.) Depois da correção, os robôs continuam rendendo: na cena 8 os corredores
fazem 3,6 a 5,1 voltas por minuto (máximo teórico 3,7 a 5,6), e nas cenas 7 e 9 a velocidade média
fica em 86–88% da máxima.

### Path Following (seguir caminho)
O caminho ([`Path.java`](src/steering/student/Path.java)) é um circuito fechado de segmentos com uma largura.
1. **Previsão:** estima onde o robô estará daqui a pouco (`posição + direção · 25`).
2. **Projeção:** encontra o ponto do caminho mais próximo dessa previsão, medido como
   "distância percorrida ao longo do caminho".
3. **Olhar adiante:** avança `lookAhead` unidades ao longo do caminho a partir desse ponto e faz Seek ali.

O alvo (ponto rosa no debug) sempre "corre" à frente sobre a pista, então o robô completa voltas
indefinidamente e corta as curvas de forma natural. Diferente da versão original de Reynolds, que só
corrige quando sai da largura do caminho, aqui o Seek é contínuo. Isso mantém os robôs em movimento,
o que é necessário porque na engine força zero não acelera ninguém.

### Extras usados nas combinações
- **Separation:** soma vetores que apontam para longe dos vizinhos dentro de um raio, com peso
  `1 − d/raio` (quanto mais perto, mais forte).
- **Pursuit:** Seek na **posição futura** do alvo: `pos + vel · T`, com `T = distância / (vmax + vAlvo)`.
- **Evade:** Flee da posição futura da ameaça, com raio de pânico.

---

## Combinações

### Como os comportamentos são somados — [`Blend.java`](src/steering/student/Blend.java)
É usada a **soma ponderada truncada com prioridade** (Buckland):
os comportamentos entram **em ordem de prioridade**, cada um multiplicado pelo seu peso, enquanto
houver "orçamento" de força (`maxForce`). Quando o orçamento acaba, os de menor prioridade
são ignorados naquele quadro.

Por que essa escolha em vez de uma soma ponderada simples?
- Numa soma simples, um Wander forte pode **anular** parte do desvio e fazer o robô raspar no obstáculo.
- Com prioridade, quando há perigo o desvio consome o orçamento e manda. Quando não há perigo,
  o desvio vale zero e **todo** o orçamento fica para os outros comportamentos. O movimento fica
  fluido e seguro ao mesmo tempo.

### Ajustando os pesos ao vivo — [`Weights.java`](src/steering/student/Weights.java)
Nas cenas com combinação (5, 7, 8 e 9), um **painel no canto superior direito** mostra os pesos na
ordem de prioridade. Dá para mudar cada peso com os botões `−` / `+` ou pelo teclado. A mudança vale
na hora para todos os robôs da cena, porque eles leem o peso a cada quadro (`weights.get("avoid")`).
Os pesos vão de 0 a 5. O tracinho na barra marca o valor padrão, e um peso alterado aparece em
destaque. Os valores ficam guardados ao trocar de cena; `R` ou **Restaurar** volta ao padrão.

Experimentos que valem mostrar na defesa:
- **Cena 7, Avoidance = 0:** os pintinhos passam a atravessar os obstáculos (medido: 14% do tempo
  encostando, contra 0% com o padrão). Isso mostra por que o desvio tem a maior prioridade.
- **Cena 7, Wander alto:** os pintinhos ficam agitados mesmo calmos, porque o wander ganha mais orçamento.
- **Cena 8, Separation = 0:** os corredores passam a se sobrepor nas ultrapassagens (medido: cerca
  de 2,6× mais sobreposições entre pares de corredores).
- **Cena 9, Lobo: Pursuit = 5:** o lobo fica mais agressivo e pega mais ovelhas (medido: 19–23 por
  minuto, contra 13–17 com o padrão), mas **continua sem encostar em obstáculos** (0%). Como o desvio
  vem primeiro na prioridade, aumentar o peso de quem vem depois não tira a segurança.

As tabelas abaixo mostram os **pesos padrão** e a justificativa de cada um.

### Combinação A — Presa (tecla 7) — [`PreyBot`](src/steering/student/PreyBot.java)
| Prioridade | Comportamento | Peso padrão | Justificativa |
|---|---|---|---|
| 1 | Obstacle Avoidance | 3.0 | Bater num obstáculo quebra a ilusão de inteligência; nunca pode perder |
| 2 | Flee do mouse | 2.0 | Sobreviver vem antes de passear |
| 3 | Separation | 1.2 | Ao fugir em grupo, eles se espalham em vez de se sobrepor |
| 4 | Wander | 0.6 | Só quando calmos; peso baixo deixa o passeio tranquilo |

**Resultado:** os robôs passeiam devagar e felizes. Quando o cursor (predador) se aproxima, ficam
assustados (olhos `> <` e gota de suor), fogem em leque e contornam os obstáculos.

### Combinação B — Corrida (tecla 8) — [`RacerBot`](src/steering/student/RacerBot.java)
| Prioridade | Comportamento | Peso padrão | Justificativa |
|---|---|---|---|
| 1 | Obstacle Avoidance | 3.0 | Os cones estão **na pista**: é preciso desviar sem sair dela |
| 2 | Separation | 1.5 | Evita que corredores mais rápidos atravessem os mais lentos |
| 3 | Path Following | 1.0 | Seguir a pista usa o orçamento que sobrar |

Cada corredor tem uma velocidade máxima diferente (150 a 225), então os mais rápidos
**ultrapassam** os mais lentos pela lateral, empurrados pela Separation, e voltam para a pista
pelo Path Following.

### Combinação C — Predador × Presa (tecla 9) — [`HunterBot`](src/steering/student/HunterBot.java) / [`RunnerBot`](src/steering/student/RunnerBot.java)
- **Predador** (vermelho, maior, cara brava): Avoidance ×3 > Pursuit ×1 na presa livre mais próxima.
- **Presas:** Avoidance ×3 > Evade ×2 > Separation ×1 > Wander ×0,7.

O predador é um pouco mais lento que as presas (185 contra 195), mas o Pursuit antecipa a
trajetória e corta caminho. Quando ele encosta numa presa, ela fica "tonta" por 2,5 s e o predador
troca de alvo. Assim a perseguição nunca trava em um único robô.

---

## Mapa de testes

[`TestMap.java`](src/steering/student/TestMap.java): arena 900×900 com pilares e arbustos espalhados,
um circuito octogonal com largura 38 e cones colocados sobre a pista para a Corrida.

## Estrutura do código

```
src/steering/engine/    ← engine (template da professora + câmera isométrica)
  Robot.java            agente: física + desenho do robô + ganchos getMood/getTheme/drawDebug
  Window.java           loop principal, mouse/teclado, troca de cenário
  World.java            o que o agente "enxerga" (mouse, clique, vizinhos, dt)
  Vector2.java          álgebra de vetores
  Settings.java         builder de configuração do agente (+ size)
  Iso.java              NOVO: câmera isométrica (mundo ↔ tela)
  Renderer.java         NOVO: desenha chão, cenário e robôs ordenados por profundidade, HUD
  RobotRenderer.java    NOVO: o robozinho (olhos, antena, braços, expressões)
  Mood.java / Prop.java NOVO: expressão do robô / objeto de cenário
  Theme.java            NOVO: fantasia temática de cada robô
src/steering/student/   ← o trabalho
  Behaviors.java        Seek, Flee, Arrive, Pursuit, Evade, Separation, Obstacle Avoidance, Path Following
  Wander.java           Wander (com estado)
  Blend.java            combinação por soma ponderada truncada com prioridade
  Weights.java          pesos ajustáveis ao vivo pelo painel da tela
  Path.java, Obstacle.java, TestMap.java
  *Bot.java             um agente por cenário/combinação
  Scenes.java, Scene.java, Setup.java
```

### O que mudou na engine original
- **Física:** nada. A integração continua idêntica, e foi adicionado só um limite de `dt` (0,05 s)
  para a simulação não "pular" quando a janela é arrastada.
- **Câmera:** o mundo continua 2D (x, y no chão) e só o desenho é isométrico. O mouse é convertido
  de volta da tela para o chão, então `getClickPos()` e `getMousePos()` vêm em coordenadas de mundo.
- **Visual:** robozinhos isométricos com expressões (feliz, sonolento quando parado,
  assustado ao fugir, bravo no predador).
- **Arena fixa** de 900×900 (antes dependia do tamanho da janela). O wrap-around nas bordas foi mantido.
- `getDirection()` mantém a última direção quando o robô para (antes voltava para o ângulo 0).
