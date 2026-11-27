# Cenários

Catálogo dos 19 cenários. Dificuldade de 1 (introdutório) a 5 (avançado).

| ID | Título | Stack | Método | Dif. | Foco |
| --- | --- | --- | --- | --- | --- |
| `fundamentos` | Fundamentos: pressões e lama | Superfície | Tutorial | 1 | Hidrostática, ECD, MAASP |
| `kill-sheet` | Kill sheet na prática | Superfície | Driller's | 2 | SIDPP/SICP, KMW, ICP, FCP |
| `drillers-surface` | Kick perfurando — Driller's | Superfície | Driller's | 2 | Detecção, shut-in, circulação |
| `ww-surface` | Kick perfurando — Wait & Weight | Superfície | Wait & Weight | 3 | Pesar lama e seguir schedule |
| `swab-trip` | Kick de manobra (swab) | Superfície | Driller's | 3 | Trip tank, flow check, gatilho manual |
| `volumetric` | Gás migrando — volumétrico | Superfície | Volumétrico | 4 | Sangria controlada |
| `lubricate` | Lubrificar e sangrar | Superfície | Lubrificar & sangrar | 4 | Coluna travada, BHP |
| `losses` | Perda de circulação e kick | Superfície | Driller's | 4 | Perda, sapata, influxo |
| `failures` | Falhas de equipamento | Superfície | Driller's | 4 | Bomba e choke |
| `subsea-drillers` | Submarino — Driller's | Submarino | Driller's | 4 | Choke line e riser |
| `subsea-ww` | Submarino — Wait & Weight | Submarino | Wait & Weight | 5 | MASP e riser margin |
| `subsea-eds` | Submarino — EDS e emergência | Submarino | Emergência | 5 | Perda de energia, gás no riser |
| `assessment` | Avaliação final | Superfície | Avaliação | 5 | Kick oculto, falhas, score |
| `directional-hp` | Direcional — zona de alta pressão | Superfície | Driller's | 4 | TMD/TVD, inclinação 34°, kick na zona |
| `subsea-choke-line` | Submarino — gás pela choke line | Submarino | Driller's | 4 | BOP fechado, HCR aberto, riser isolado |
| `depth-trigger` | Kick por profundidade | Superfície | Driller's | 3 | Gatilho por TMD na zona HP |
| `narrow-window` | Águas profundas — janela estreita | Submarino | Wait & Weight | 5 | Poros altos, fratura baixa, MASP |
| `combined-casing` | Casing combinado e liner | Superfície | Driller's | 4 | Múltiplos diâmetros, sapata e liner |
| `sandbox` | Sandbox | Superfície | Sandbox | 1 | Experimentação livre |

## Briefings

Cada cenário traz título, subtítulo, descrição, briefing, objetivos e notas de instrutor em PT-BR e EN. Os dados ficam em `core/src/main/kotlin/.../Scenarios.kt`.

## Condições iniciais

- Poços de superfície: 2.800–3.600 m TVD, sapatas de 2.000–2.500 m, lama 9,4–10,2 ppg; direcional com MD > TVD.
- Submarino: 4.200–5.200 m TVD, lâmina d'água 1.200–1.800 m, lama 9,8–10,9 ppg.
- Sondas: 1500/2000 HP em superfície; drillship com três bombas e BOP subsea.
- Kicks: gás, óleo, água ou água salgada; volumes de 0,5–3,2 m³; gatilho imediato, por tempo, por profundidade, híbrido ou manual.
- Casings: simples, combinado e liner; formações em camadas com poros/fratura próprios.

## Gatilhos de kick

| Modo | Quando dispara |
| --- | --- |
| `IMMEDIATE` | No início do cenário |
| `TIME` | Em `kick.triggerAtSec` |
| `DEPTH` | Ao atingir `triggerDepthMdM` ou o topo da zona |
| `HYBRID` | Tempo **ou** profundidade |
| `MANUAL` | Somente pelo botão do operador |

## Eventos programados

| Cenário | Eventos |
| --- | --- |
| `lubricate` | coluna travada aos 35 s |
| `losses` | perda de 2 m³ aos 20 s |
| `failures` | falha de bomba aos 30 s; choke plugado aos 50 s |
| `subsea-eds` | perda de energia aos 40 s |
| `assessment` | choke desgastado aos 45 s; falha de bomba aos 70 s |

## Objetivos e avaliação

O debrief considera:

- kick detectado e shut-in;
- pico de pressão de revestimento vs. MAASP;
- pico de pressão na sapata vs. fratura;
- ganho de pit vs. limite;
- gás que alcançou a superfície em qualquer momento (`gasReachedSurface`);
- conclusão do kill.

A aprovação didática é score ≥ 70.

## Poços personalizados

O editor grava `CustomWellSpec` localmente e `Scenarios.custom()` monta o cenário. É possível configurar TVD/TMD, sapata, lâmina d'água, gradientes, lama, kick, diâmetro do poço aberto, revestimentos, linhas/riser, temperaturas e coluna de perfuração. Quando o gatilho é por profundidade, `triggerDepthMdM` é autoritativo e topo/base da zona são normalizados a partir dele.

## Como adicionar um cenário

1. Use `baseSurfaceScenario` ou `baseSubseaScenario`.
2. Defina poço, lama, kick, eventos e limites coerentes.
3. Adicione textos em PT-BR e EN.
4. Inclua em `Scenarios.all`.
5. Rode `./gradlew :core:test` — o teste `todos os cenarios abrem` cobre o novo item.
6. Atualize a tabela deste documento e o README.
