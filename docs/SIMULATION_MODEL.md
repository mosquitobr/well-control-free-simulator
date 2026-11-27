# Modelo de simulação

Este documento descreve as equações e simplificações usadas pelo motor. O objetivo é didático e coerente, não substituir um simulador de engenharia ou um programa certificado.

## Unidades internas

| Grandeza | Unidade |
| --- | --- |
| Pressão | kPa |
| Profundidade/TVD | m |
| Volume | m³ |
| Vazão | L/min |
| Densidade | kg/m³ |
| Diâmetro | polegadas (convertido para m) |

Conversões principais:

- 1 psi = 6,894757 kPa
- 1 ppg = 119,826427 kg/m³
- 1 bbl = 0,1589872949 m³
- 1 ft = 0,3048 m
- 1 gpm = 3,785411784 L/min
- 1 bbl/min = 42 gpm = 158,9872949 L/min
- 1 Klb = 4,4482216 kN
- 1 Klb·ft = 1,3558179 kN·m

## Hidrostática

```
P_h = ρ · g · TVD / 1000        [kPa]
```

com `g = 9,80665 m/s²`.

No sistema de campo:

```
P_h = 0,052 · MW(ppg) · TVD(ft)
```

A UI usa a primeira forma e converte o resultado.

### Perfil térmico e densidade

A hidrostática do motor é integrada ao longo da TVD com densidade corrigida pela temperatura (`ThermalModel`):

```
ρ(T) = ρ_ref · (1 − α · (T − 15,5))        α = 3,0e-4 /°C
```

- Em terra, a temperatura varia linearmente entre a superfície configurada e o fundo.
- Em submarino, a coluna de água segue a termoclina entre a superfície do mar e o mudline (4 °C por padrão); abaixo do mudline vale o perfil geotérmico.
- A integração usa 30 passos e retorna a pressão hidrostática em kPa, sempre menor que a hidrostática isotérmica a 15,5 °C.
- A temperatura também alimenta a densidade efetiva usada nas perdas de carga e no ECD.

## Pressões de poros e fratura

```
P_poros = gradiente_poros · TVD
P_fratura = gradiente_fratura · TVD_sapata
```

`MAASP` é a máxima pressão de superfície com a lama atual:

```
MAASP = P_fratura,sapata − P_h,lama(TVD_sapata)
```

A sapata usada é a **exposta mais profunda** (`WellConfig.shoeTvdM`), não a sapata rasa já cimentada. O MD correspondente (`shoeMdM`) é limitado ao MD do poço, e `shoeFracGradientKPaPerM` / `shoeFracPressureKPa` alimentam MAASP, alarmes de fratura/perda, kill sheet e `strokesToShoe()`. Sem essa coerência a MAASP ficava artificialmente baixa e era atingida logo após o fechamento.

`MASP` é a máxima pressão de superfície prevista; no cenário submarino inclui a fricção da choke line.

## SIDPP e SICP

Com o poço fechado e estabilizado:

```
SIDPP = P_poros − P_h,lama(TVD)
SICP  = P_poros − (P_h,lama(TVD) − perda_hidrostática_do_influxo) + cabeça_das_linhas_deslocadas
```

A perda hidrostática do influxo é estimada pela altura do influxo no anular e pela diferença entre a densidade da lama e a densidade do fluido invasor. Cada natureza tem densidade própria (`defaultInfluxDensityKgM3`):

| Influxo | Densidade (kg/m³) |
| :--- | ---: |
| Gás | 90 |
| Óleo | 780 |
| Água | 1020 |
| Água salgada | 1080 |

Assim o **SIDPP depende só da coluna de lama** e permanece estável por tipo, enquanto o **SICP cresce de água para óleo e gás**. `KickConfig.influxDensityKgM3` permite sobrescrever a densidade no cenário.

### Build-up de shut-in

O fechamento do BOP não iguala as pressões instantaneamente. `casingPressureKPa` converge para o alvo (`SICP` ou pressão trapada) por uma constante de tempo de primeira ordem:

```
τ = base(tipo) · (100 / permeabilidade)^0,5…3 · (1 + 0,25 · volume)
casing(t + Δt) = casing(t) + (alvo − casing(t)) · (1 − e^(−Δt/τ))
```

`base` é 16 s para gás, 11 s para óleo e 7 s para água/salgada, limitada a 4–45 s. O gauge de `SIDPP` usa `0,7 · τ`. A fase só passa a `STABILIZED` quando **ambas** as derivadas primeiras (`d(SIDPP)/dt` e `d(SICP)/dt`) permanecem próximas de zero e dentro da tolerância do alvo por 3 s com o choke fechado e sem bombeio. Ao estabilizar, `stabilizedSidppKPa` e `stabilizedSicpKPa` são congelados como valores fixos para aquela lama e kick (sem expansão livre artificial de gás em poço selado), podendo o operador aplicar opcionalmente um fator de segurança acima da estabilização (`shutInSafetyOffsetKPa`). Em coluna homogênea (sem influxo/gás/linha deslocada), com bomba parada, a pressão travada é limitada ao equilíbrio físico e o manômetro de revestimento iguala o de SPP/bengala (tubo em U). A captura é um **latch monotônico** (`captureShutInPair`): uma vez registrada, não regride ao abrir o choke, ligar a bomba ou iniciar a 1ª circulação; se a circulação começar antes do platô, o par é registrado no início e `guideStabilized` permanece concluído. O snapshot expõe `sidppGaugeKPa`, `sicpGaugeKPa`, `recordedSidppKPa`, `recordedSicpKPa`, `stabilizedSidppKPa`, `stabilizedSicpKPa`, `shutInSafetyOffsetKPa`, `sidppValid`, `sicpValid`, `shutInStabilized` e `chokeOpenNoPump`.

Quando o BOP está fechado e a bomba parada (`!pumpOn`), deixar o choke ajustável manual aberto (`hcrOpen && chokePct > 0,2%`) remove a perda de carga localizada e despressuriza o poço pelo choke, podendo perder pressão trapada e reabrir influxo; em `chokeAuto = true`, ao parar a bomba o controlador fecha o choke (`0%`) para manter a pressão trapada.

### Cabeça das linhas deslocadas

Depois do deslocamento com lama de matar, a choke line (e o anular do riser no subsea) impõe uma cabeça hidrostática extra ao gauge de casing. `lineDisplacedHeadKPa` soma `Δρ · g · L` somente para as linhas que comunicam com o casing, mantendo o poço morto após o deslocamento e impedindo que a pressão residual retorne depois da sangria.

## BHP

Com o poço aberto:

```
BHP = P_h(TVD, T) − perda_do_gás + ΔP_cuttings + atrito_anular + surge/swab
```

Com o poço fechado:

```
BHP = P_casing + P_h(TVD, T) − perda_do_gás + ΔP_cuttings + atrito_anular + surge/swab
```

O atrito anular é calculado pela geometria real do trecho (revestimento, poço aberto ou riser), pela vazão, pela densidade efetiva e pelo fator térmico. O snapshot expõe a decomposição `bhpHydrostaticKPa`, `bhpCuttingsKPa`, `bhpAnnularFrictionKPa`, `bhpSurgeKPa` e `bhpChokeKPa` para o painel do sondador.

O **surge/swab** é gerado pela velocidade de movimentação da COP (`surgeKPa`), positivo ao baixar e negativo ao levantar a coluna.

## Kill sheet

```
KMW_sem_margem = MW + SIDPP · 1000 / (g · TVD_ref)
KMW            = KMW_sem_margem + margem
ICP = SIDPP + SCR
FCP = SCR · KMW / MW
```

`TVD_ref` é a TVD da broca/zona de kick (`kickReferenceTvdM()`), não o TD do poço. Em poços direcionais ou com a broca acima do TD, usar o TD subestimaria a KMW e deixaria o poço pressurizado. A margem de segurança é configurável no editor (padrão **0,1 ppg ≈ 12 kg/m³**) e aparece separada da KMW no kill sheet (`killMudNoMarginKgM3` e `marginKgM3`).

O schedule interpola a SPP entre ICP e FCP enquanto a lama de kill desce até a broca e depois mantém FCP. Os strokes até a broca, sapata e superfície vêm da geometria.

O kill sheet é **congelado** no início do controle (`killSheetFrozen`) para preservar SIDPP, SICP, KMW, ICP e FCP enquanto o operador pesa a lama. O snapshot expõe `sidppKPa` e `sicpKPa` no `KillSheetResult`. Sem kick detectado no ciclo atual (`kickDetected = false` e `killSequenceActive = false`), `killSheetResult()` retorna `null`: a planilha pertence ao poço fechado, não é um cálculo livre de lama.

## Segunda circulação e deslocamento de linhas

O controle de kick segue duas circulações distintas:

```
1ª circulação  →  concluir (botão explícito)  →  pressão trapada
               →  pesagem  →  iniciar 2ª circulação (bomba liga automaticamente)
               →  poço morto  →  deslocamento de linhas  →  sangria  →  BOP aberto
```

- **Primeira circulação (B6)**: o volume total é o anular do poço entre a broca e o BOP mais a choke line. Em submarino o riser isolado, a kill line e o booster não entram no retorno. A fase permanece `CIRCULATING` até bombear 100% do volume; após 2 s a etapa conclui sozinha, **sem desligar a bomba nem alterar o choke**, registra a pressão trapada e congela o kill sheet. A sobrepressão do fim da circulação é preservada para a 2ª etapa, nos poços terrestres e submarinos. O snapshot expõe `firstCirculationPumpedM3`, `firstCirculationTotalM3`, `firstCirculationStrokes`, `firstCirculationTotalStrokes`, `firstCirculationRemainingStrokes` e `firstCirculationKickOut`.
- **Flow check (A1)**: ao armar o flow check as bombas são paradas e o motor aguarda 5 s de estabilidade antes de registrar o item. `flowCheckProgress` acompanha a barra e `flowCheckFlowLpm` registra o retorno.
- **Lama velha congelada**: no gatilho do kick, `oldMudKgM3` recebe a densidade existente no poço e permanece fixa. Pesar a lama no pit não altera a hidrostática do poço aberto, então SIDPP/SICP e o kill sheet não caem artificialmente.
- **Peso manual × KMW**: a 2ª circulação exige a maior densidade entre a KMW calculada e o alvo manual definido pelo operador. Um peso manual mais pesado nunca é reduzido; a KMW funciona como piso de segurança e soma a margem configurada.
- **Um kick por cenário**: `kickSequenceComplete` só é liberado após o poço morto e a abertura do BOP. O histórico `kickTriggered` não autoriza mais influxo por si só; após o kill, um novo influxo exige um novo evento de kick (instrutor/modo manual), que reinicia o ciclo.
- **Densidades efetivas**: durante a 2ª circulação, a coluna usa `ρ_efetiva = ρ_velha + (ρ_kill − ρ_velha) · f_COP` e o anular usa a mesma interpolação com `f_anular`. As perdas hidráulicas, a hidrostática e o ECD passam a usar essas densidades efetivas.
- **Estágios**: `KillStage` percorre `FIRST_CIRCULATION → KICK_CONTROLLED → STRING → ANNULUS → CHOKE_LINE → WELL_KILLED → LINE_DISPLACEMENT → READY_TO_OPEN`.
- **Pressão de casing alvo**: a 2ª circulação parte da pressão trapada preservada e reduz o alvo pelo ganho hidrostático da lama de matar no anular (`Δρ · g · TVD` conforme a fração deslocada), reequilibrando o poço sem abrir mão da sobrepressão remanescente.
- **Poço estanque**: com o BOP fechado, ligar a bomba pressuriza o poço; ao desligá-la, a pressão construída é registrada como trapada e mantida (com decaimento apenas por infiltração). A formação ainda pode elevar o casing até o SICP, mas o valor trapado nunca é abandonado.
- **Poço morto**: quando a lama de matar ocupa COP, anular e choke line, a fase passa a `KILLED`.

### Deslocamento de linhas

Antes de abrir o BOP, as linhas de controle precisam estar com lama de matar:

- **Em terra**: kill line + choke line.
- **Em submarino**: kill line, choke line, booster line e anular do riser (volume entre a mesa e o mudline).

O progresso é fracionário (`+25%` ou `100%`) e a barra por linha vem de `lineProgress`. `requiredLineKinds()` ignora linhas com volume desprezível.

A **choke line** é deslocada automaticamente quando a fração de lama de matar no anular (`chokeLineKillFraction`) chega a 100% durante a 2ª circulação, aparecendo como "Já deslocada pela 2ª circulação". Se não chegar, pode ser deslocada manualmente.

Antes do poço morto, as linhas mostram "Aguardando poço morto" e os botões de deslocamento ficam ocultos.

### Abertura do BOP

`openBop()` é bloqueado enquanto:

1. a 1ª e a 2ª circulação não terminarem em poço morto;
2. alguma linha exigida não estiver 100% deslocada;
3. a pressão residual de casing estiver acima de 200 kPa;
4. o poço ainda não estiver morto (`wellDead()`: influxo residual < 0,02 m³ e SICP ≤ 100 kPa).

O motivo do bloqueio aparece no cartão “Deslocamento de linhas” e em `openBopBlockedReason`. O botão `Sangrar pressão residual` fica desabilitado até todas as linhas estarem 100% deslocadas. A sangria só é aceita com o poço morto: se a formação ainda empurra o casing (SICP > 100 kPa), a pressão volta em segundos, então o motor recusa a sangria e orienta ajustar o peso da lama. Com o poço morto, a sangria zera a pressão residual de uma vez.

Na abertura, o poço passa a conter a lama de matar efetiva do anular. Isso evita o sintoma em que o pit mostrava a lama nova, mas a hidrostática continuava com a lama antiga, permitindo um novo kick logo após abrir o BOP.

### Latches do guia e resets

Os passos B são monotônicos: cada item concluído ativa um latch (`guideKickHandled`, `guideDrillingStopped`, `guideBopClosed`, `guideStabilized`, `guideKillSheetReady`, `guideFirstCirculation`, `guideWeightedUp`, `guideSecondCirculation`, `guideLinesDisplaced`, `guideBleedDone`, `guideCycleComplete`) que não volta a falso ao abrir o BOP, alternar fases ou avançar o tempo.

- **Reset parcial** (`resetGuideAndContinue()`): mantém profundidade, lama, picos (`maxCasing`, `maxPitGain`) e score; zera pressões, influxo, kill sheet, progresso das linhas e checklist A+B; devolve o próximo kick ao modo `MANUAL` na profundidade atual. O score preservado é fixado por `scoreOffset` (`score = rawScore() + scoreOffset`), que impede a queda artificial de 5 pontos por `kickDetected = false` e mantém os pontos já conquistados.
- **Reset total**: a UI recria o `Simulator` com `generation++`, zerando estado e score; o histórico salvo no aparelho permanece.

### Resultados salvos

“Salvar resultado” grava apenas o resumo (`SimulationResult`), sem a série temporal, em `simulation_results.json` no armazenamento interno do app (`MODE_PRIVATE`, até 50 resultados). A tela **Resultados salvos** fica na Home e permite excluir itens e exportar o histórico completo como JSON via Storage Access Framework. A exportação não exige permissão de armazenamento nem `FileProvider`.

## Choke A / Choke B

O simulador suporta múltiplos chokes (ex.: Choke A e Choke B) configurados no cenário. O operador escolhe o choke ativo no topo da aba Choke. O cálculo de vazão usa a área (`maxAreaM2`) e o coeficiente de vazão (`cv`) do choke selecionado. Ao trocar o choke com a bomba ligada, aparece aviso para reajustar a abertura e observar a pressão de casing.

## Fluxo visual no esquemático

O esquema animado mostra o caminho real do fluido:

- **Injeção**: com as bombas ligadas e alinhamento de perfuração, o fluxo desce pelo standpipe → coluna → broca. Com o poço fechado e alinhamento de kill, desce pela kill line. `injectionPath`, `standpipeActive` e `killLineActive` vêm do motor, de modo que standpipe e kill line nunca aparecem ativos ao mesmo tempo.
- **Retorno**: broca → anular → BOP (lama de matar abaixo, lama antiga acima da interface horizontal). O retorno nunca é desenhado pelo interior da coluna.
- **Layout**: riser no centro; kill line à esquerda; choke line à direita; booster entre o riser e o choke, conectado acima do LMRP. Flowline, pits, standpipe manifold e choke manifold ficam sempre na superfície. Em submarino, nível do mar, lâmina d'água e mudline usam escalas distintas.
- **Manifolds coerentes**: `chokeManifoldActive` só fica verdadeiro quando o retorno passa pela choke line. O HCR é 100% manual: mudar o alinhamento não abre nem fecha o HCR, apenas avisa quando a choke line é necessária e o HCR está fechado.
- **Retorno conforme o estado**:
  - BOP aberto em terra: anular → BOP → flowline de retorno ao pit.
  - BOP aberto em submarino: anular → BOP → riser até a superfície.
  - BOP fechado + HCR aberto: anular → BOP → choke line → choke manifold.
  - BOP fechado + HCR fechado: retorno bloqueado (sem fluxo de retorno).

Pontos animados se movem com velocidade proporcional à vazão/SPM. Cores distinguem lama antiga (cinza/azul) de lama de matar (vermelho/teal).

## Manifolds, SCR e HCR

- **Perfurando**: bomba → standpipe → coluna → broca → anular → choke manifold.
- **Kill**: standpipe/kill manifold alinhado à kill line para circular lama de matar.
- **Choke**: retorno controlado pelo choke manifold, MGS e flare, segurando a pressão do poço.
- **Manobra**: trip tank e linhas de manobra, com BOP fechado para retirar ou recolocar a coluna.
- **Lubrificar**: injeta lama no anular e sangra gás pelo choke, sem circulação completa.

**SCR** é a pressão de circulação lenta medida na taxa de kill e calibra `ICP = SIDPP + SCR`. **HCR** é a válvula hidráulica de alta relação de fechamento da choke/kill line: isola a linha, mas não é elemento de vedação do poço.

## Áudio procedural

O motor de áudio sintetiza bomba, rotação, peso da coluna e gás na superfície sem arquivos. A fundamental de uma bomba tripla a 45 SPM é 0,75 Hz (infrassom); por isso `PumpPulseSynth` gera um transiente percussivo por stroke com ressonâncias em 82, 147 e 219 Hz, ataque de ~3,5 ms e decaimento de ~85 ms. O som acompanha a rotação e fica audível mesmo em baixas SPM.

## Geometria e strokes

Área anular:

```
A = π/4 · (D_poco² − D_coluna²)
```

O volume é integrado em passos de 1 m para acomodar revestimentos e coluna com diâmetros diferentes. Strokes:

```
strokes = volume / (saída_da_bomba_L_por_stroke · 0,001)
```

## Trajetória e MD/TVD

Poços direcionais usam um perfil simplificado **build-and-hold** em 2D/3D-lite:

- estações com MD, inclinação, azimute, TVD e afastamento (north/east);
- build com raio constante até a inclinação alvo;
- trecho hold com inclinação constante até o TD.

A interpolação entre estações fornece `tvdAtMd(md)` e `mdAtTvd(tvd)`. Profundidades de casing, zonas e gatilhos podem ser definidas em MD ou TVD, e a UI mostra as duas quando relevante.

## Formações em camadas

Cada poço pode ter camadas com:

- topo e base em MD;
- tipo (arenito, folhelho, calcário, dolomita, sal, conglomerado);
- dureza, porosidade, permeabilidade e fator de ROP;
- gradiente de poros e de fratura.

O motor usa a camada da broca para poros, fratura e resposta de perfuração. A camada marcada como `kickZone` é destacada no monitor de kick.

## Perfuração realista (WOB, RPM, COP e ROP)

A taxa de penetração (ROP) é simulada com física dinâmica e servocontrole amortecido:

1. **Posição da COP (coluna de perfuração)**:
   - Offset da COP varia de -1,5 m (assentada) a +2,0 m (levantada);
   - Fator de transferência de peso: $f_{COP} = \text{clamp}(1 - \text{offset}/1,2, 0, 1,25)$;
   - COP levantada (offset ≥ 1,2 m) zera o peso sobre a broca ($WOB \to 0$) e cessa o ROP imediatamente.

2. **Dinâmica e oscilação de WOB**:
   - $WOB_{alvo} = WOB_{setpoint} \cdot f_{COP}$;
   - Acompanha o setpoint com constante de tempo de ~0,7 s;
   - Oscilações determinísticas coerentes (±5,5% a 0,22 Hz e ±2,5% a 0,93 Hz) reproduzem o ruído mecânico real da broca.

3. **Taxa de penetração (ROP)**:
   - Limiar mínimo: exige bomba ligada (> 5 SPM), rotação ligada (> 5 RPM) e $WOB > 2\text{ Klb}$;
   - Equação:
     $$ROP = 24 \cdot \left(\frac{WOB - 2}{23}\right)^{0,85} \cdot \left(\frac{RPM}{120}\right)^{0,6} \cdot f_{dureza} \cdot f_{formação} \cdot f_{desgaste} \cdot f_{founder} \cdot f_{ripple}$$
   - Acima de 45 Klb, atua o efeito *founder* reduzindo a eficiência de penetração;
   - Ripple de ±6% simula a microheterogeneidade da formação;
   - O avanço de profundidade (`bitDepthM`) integra continuamente no tempo ($dt$), aprofundando o poço além do TD inicial (`holeDepthM = max(well.mdM, bitDepthM)`).

4. **Torque**:
   - Torque reativo em Klb·ft ($kN\cdot m$ internamente):
     $$Torque = (2,0 + 0,28 \cdot WOB + 0,035 \cdot RPM + 0,0012 \cdot TVD + 0,25 \cdot dureza) \cdot ripple$$
   - Com rotação sem peso (fora do fundo), reflete apenas o arraste da coluna.

## Hidráulica e perdas de carga

As perdas são calculadas por trecho com geometria, vazão, reologia simplificada, densidade efetiva (temperatura) e desgaste da broca (`HydraulicModel`):

1. **Superfície**: perda distribuída calibrada por `hydraulicScale`.
2. **Coluna**: integração por segmentos de DP, HWDP e DC, usando o ID de cada trecho e fator de atrito dependente de vazão.
3. **Broca**: perda por TFA, coeficiente de descarga e desgaste.
4. **Anular**: integração pela geometria real (revestimento, poço aberto ou riser), com diâmetro hidráulico e fator de atrito.
5. **Motor de fundo**: parcela proporcional à vazão e à densidade.

A **SCR medida** recalibra `hydraulicScale = SCR_medida / ΔP_modelo`, mantendo a proporcionalidade com a vazão e a geometria. Sem medição, o cenário fornece uma estimativa.

O **ECD** é reportado contra o peso de lama de superfície:

```
ECD = MW + ΔP_cuttings/(g·TVD) + ΔP_anular/(g·TVD)
```

Durante circulação normal, o ECD é estritamente maior que o peso estático da lama. Cuttings aumentam com alto ROP e são limpos gradualmente com a circulação contínua. A temperatura reduz a densidade real usada nas perdas, mas não mascara o acréscimo de ECD por atrito e cascalhos.

## Geometria configurável

O poço personalizado aceita:

- diâmetro do poço aberto;
- lista de revestimentos (topo, base, ID, OD e diâmetro do buraco, com `shoeTvdM` derivado da trajetória `tvdAtMd(bottomMdM)`);
- IDs e comprimentos de kill line, choke line e booster line;
- riser ID/OD e lâmina d'água;
- temperaturas de superfície, fundo, mar e mudline;
- coluna de perfuração customizada (DP, HWDP, DC, BHA e TFA);
- profundidade inicial da broca (`initialBitDepthMdM`), profundidade do influxo (`kickDepthMdM`) e sobrepressão da zona (`kickIntensityKPa`), com sugestão ativa (`CustomWellDefaults`) quando o campo fica vazio no editor e respeito ao valor informado quando preenchido;
- entrada e exibição no sistema de unidades ativo em Ajustes (`PETROBRAS`, `US_FIELD` ou `SI`), convertendo para SI no motor.

A geometria alimenta volumes, strokes, áreas anulares, perdas de carga, hidrostática e o caminho do influxo. Em poços direcionais, os limites das camadas de formação são convertidos de TVD para TMD pela trajetória. Em submarino, o riser é reconhecido acima do mudline e os revestimentos customizados são persistidos integralmente em JSON local (`CustomWellStore`).

## Tomada prévia de SCR / PRC

A operação de tomada de SCR no painel do sondador mede a SPP real na vazão de kill com o poço aberto e bombas estabilizadas. A medição fixa a calibração do modelo hidráulico e alimenta a kill sheet:
```
ICP = SIDPP + SCR_medida
FCP = SCR_medida · (KMW / MW)
```
Sem a tomada de SCR, o simulador utiliza o valor estimado do cenário e a kill sheet exibe um aviso amarelo recomendando a medição prévia.

## Preparo de baixa vazão, alarmes e indícios secundários

A Fase A do guia usa a mesma hidráulica do motor para caracterizar o poço antes do kick:

- **Leituras de 50, 100, 150 e 200 gpm**: a bomba rampa até a vazão alvo e o operador registra a SPP estabilizada. A leitura é aceita com desvio de até 12% do alvo, BOP aberto e bomba estável, e alimenta `scrAtRateKPa`.
- **ΔP da linha de controle**: calculado por atrito distribuído na choke line,

```
f = 0,024 · (Q/0,015)^(-0,2)
ΔP_CL = f · (L/ID) · (ρ·v²/2) / 1000        [kPa]
```

  com `Q` em m³/s, `L` e `ID` em metros e `ρ` em kg/m³. A leitura exige poço fechado, HCR aberto e vazão reduzida.
- **Alarme de ganho/perda**: `ΔV_pit = pit - pitReferência`; o alarme dispara quando `ΔV_pit ≥ limite` ou `-ΔV_pit ≥ limite`. O limite é calibrado em 5 ou 10 bbl e convertido para m³ no motor.
- **Alarme de retorno**: compara `flowOutLpm` com a vazão da bomba; dispara quando o desvio absoluto supera o limite percentual (usual ±10%).

Os **indícios secundários** comparam o estado atual com uma linha de base de perfuração (média móvel exponencial, constante de tempo de 45 s), congelada quando o kick é detectado. Há uma janela de estabilização de 15 s após ligar a bomba para evitar falso positivo:

| Indício | Critério |
| --- | --- |
| ROP↑ | `ROP > 1,35 · ROP_base` e diferença > 1 m/h |
| WOB↓ | `WOB < 0,75 · WOB_base` |
| SPP↓ | `SPP < 0,88 · SPP_base` com bomba ligada |
| ECD↑ | `ECD > ECD_base + 12 kg/m³` |
| Bomba↑ | `SPM > SPM_base + 6` |
| Retorno ±10% | desvio absoluto acima do limite calibrado |

A **condição de kick** é classificada por densidade equivalente:

```
ED_poros = P_poros(broca) · 1000 / (g · TVD)
```

- `PORE_GT_HYDROSTATIC`: `ED_poros > MW` com o poço estático;
- `PORE_GT_ECD`: `ED_poros > ECD` com o poço circulando;
- `INTERNAL_HIGH_ZONE`: a broca está na janela de sobrepressão dentro da formação portadora normal.

## Procedimentos guiados passo a passo

O simulador inclui um motor de etapas ordenadas para as principais manobras:
- **Tomada de SCR**: (1) bomba na taxa de kill, (2) estabilização de 5 s, (3) registro de SCR, (4) confirmação;
- **Flow check**: (1) bomba parada, (2) observação de retorno por 10 s, (3) registro de fluxo estático;
- **Trip tank**: (1) bomba parada, (2) alinhamento do manifold, (3) preenchimento e confirmação de volume;
- **Teste funcional do BOP**: (1) fechamento do test ram, (2) confirmação de pressão do acumulador, (3) reabertura;
- **Alinhamento de kill**: (1) standpipe para kill, (2) choke para kill, (3) abertura do HCR.

Cada etapa valida uma condição física. Ao ser cumprida e permanecer estável por 1,5 s, o procedimento avança automaticamente; o botão "Concluir etapa" continua disponível para confirmação manual. As dicas são exibidas em PT-BR e EN.

## Síntese de áudio operacional (`RigAudioEngine`)

O áudio contínuo é gerado proceduramente através de `AudioTrack` PCM 16-bit a 22.050 Hz em stream de jogo/sonificação:
- **Bomba de lama**: pulsos de baixa frequência sintonizados com o SPM da bomba e seus harmônicos;
- **Mesa rotativa / Top drive**: zumbido contínuo modulado pela velocidade de rotação (RPM);
- **Peso da coluna / Broca**: crepitação sutil de baixa frequência modulada pelo WOB e movimentação da COP;
- **Gás na superfície**: rugido e ruído sibilante (*roar/hiss*) disparados assim que o influxo atinge a choke line ou o manifold; o nível cai suavemente (fade-out) e o som para quando o gás é completamente expulso do poço;
- **Alarmes**: bipes alternados integrados no mesmo canal com prioridade audível.

## Gatilhos de kick

`KickTriggerMode` define quando o influxo configurado é aplicado:

- `IMMEDIATE`: no início do cenário;
- `TIME`: em `triggerAtSec`;
- `DEPTH`: quando a broca **cruza** `triggerDepthMdM` (`previousBitDepthM < triggerDepthMdM && bitDepthM >= triggerDepthMdM`);
- `HYBRID`: tempo **ou** cruzamento de profundidade;
- `MANUAL`: somente por ação do operador.

Em todos os modos agendados, o influxo didático entra em `kick.depthM` (por padrão a base da zona de kick). `kick.intensityKPa` representa a sobrepressão adicional da zona (kPa), somada à pressão de poros dentro do intervalo da zona e refletida em `SIDPP`/`SICP`.

`kickEnabled` é a chave mestra. Quando habilitado, o influxo físico só ocorre se `P_poros > BHP` e o poço estiver aberto; a condição foi corrigida para nunca disparar com o poço sobrebalanceado nem reabrir influxo depois de um kill concluído. O gatilho programado apenas antecipa o volume didático do cenário. `FormationLayer.carrierZone` marca a zona portadora, que não é necessariamente uma zona de kick: a entrada de fluido continua exigindo subbalance.

## Rotas do influxo

`InfluxPath` acompanha o gás no poço:

```
FORMATION → ANNULUS → BOP → RISER → SURFACE
                     ↘ CHOKE_LINE → superfície/manifold
```

Em submarino com BOP fechado e HCR aberto, o caminho é `CHOKE_LINE`: o gás sobe pelo anular até o BOP e segue pela choke line até o manifold. Ele **não** é desenhado no riser acima do BOP. Com BOP aberto, o caminho é `RISER`. Em superfície fechada, o gás vai para `BOP` e depois para a superfície.

## Manobra e manifolds

A retirada/entrada de stands altera o MD e calcula o volume anular deslocado. A retirada acumula preenchimento pendente e reduz o trip tank; o preenchimento consome pit. O swab reduz a BHP e pode puxar influxo da zona.

`ManifoldLineup` representa o alinhamento de standpipe e choke: perfurando, kill, choke, manobra, lubrificar, EDS ou personalizado. Alinhamento de manobra com BOP aberto é inválido e gera alarme. Alinhamento de kill abre o HCR.

## Influxo

O influxo ocorre quando a BHP cai abaixo da pressão de poros. O modelo combina o subbalance físico com uma vazão característica por natureza do influxo:

```
q_influxo = vazão característica enquanto o BOP está aberto
V_efetivo = min(V_máx, Q_tipo · t_fechamento · k_estab)
```

- **Vazões características padrão**: gás 1.200 L/min, óleo 500 L/min, água/água salgada 200 L/min, editáveis no editor de poço e na aba Kick.
- **Volume máximo**: `kick.maxVolumeM3` limita a entrada mesmo com o poço aberto por muito tempo.
- **Fechamento do BOP**: congela a entrada principal e registra `kickClosingTimeSec` e `kickVolumeAtCloseM3`.
- **Estabilização pós-fechamento**: 8 s após o fechamento, com as bombas paradas, o fator sobe para 1,15 se a pressão de revestimento ainda varia ou o gás migrou mais de 0,5 m, e para 1,30 se a variação passar de 2% ou a migração de 3 m. O volume extra é sempre limitado ao máximo. Se o operador já retomou a circulação, as variações são operacionais e o fator não se aplica.
- **Zona interna**: intervalo mais fino que a formação portadora, com gradiente de poros e permeabilidade próprios, sempre com pressão ≥ à da portadora. O gradiente pode ser digitado em kPa/m, psi/m, psi/ft ou em **densidade equivalente (ED)**, usando o mesmo seletor `Gradiente / Densidade (ED)` do editor.
- **Permeabilidade típica**: o editor sugere arenito (100 mD; faixa 10–1.000 mD, apertado < 1 mD) e carbonato (10 mD; faixa 0,1–100 mD, fraturado pode passar de 1.000 mD). O valor é didático e não altera a vazão característica do influxo.

O volume e a profundidade do influxo são atualizados a cada passo. Com as bombas paradas, o gás migra para cima; com circulação, a velocidade é derivada da vazão e da área anular.

## Gás, migração e expansão

- Migração com bombas paradas: ~0,11 m/s (didático, deslocamento do influxo).
- **Sobrepressão de migração em poço selado**: cada fluido sobe a uma velocidade típica e acrescenta `ρ_lama · g · v_migração` à SICP viva. Gás ~0,02 m/s (~0,23 kPa/s), óleo ~0,005, água ~0,002 e água salgada ~0,0015 m/s. Limitada pela MAASP e dissipada por sangria/circulação/expulsão (`migrationOverpressureKPa`). O par registrado do kill sheet (`recordedSicpKPa`) não muda; apenas o manômetro vivo (`snapshot.sicpKPa`) sobe.
- Gás, óleo e água usam a mesma base de migração, mas só o gás acumula sobrepressão relevante; água e salgada permanecem planas.
- Expansão lenta com o poço fechado para dar dinâmica ao método volumétrico.
- Ao chegar à superfície, o gás tende a elevar a pressão de revestimento se não houver sangria.
- Retorno alinhado ao separador atmosférico (MGS), pelo flowline em terra ou pela choke line no submarino, é operação controlada: gera aviso informativo e não penaliza o score. Retorno desalinhado caracteriza vazamento (`gasReachedSurface`) com alarme crítico.
- A sangria reduz a pressão de revestimento e o volume do pit.
- A rota depende do BOP, HCR e tipo de stack; ver "Rotas do influxo".

## Velocidade de simulação

A UI oferece 1x, 2x, 5x, 8x, 16x, 32x e 64x. O motor integra em passos de até 0,05 s; a velocidade multiplica o tempo real por quadro, mantendo o mesmo passo físico. `advance()` aceita até 2,2 s por chamada para não perder velocidade em 64x. Os testes cobrem avanço prolongado em 16x, 32x e 64x para garantir valores finitos e estáveis.

## Choke

A vazão pelo choke é modelada por um orifício:

```
A = A_max · (abertura/100)^1,15 · Cv · fatores_de_falha
q = A · sqrt(2 · ΔP / ρ)
```

`chokePlugged` reduz a área; `chokeWashed` aumenta.

O modo automático combina **feed-forward** de vazão com **PI** em pressão de revestimento:

```
abertura_ff = f(q_bomba, ΔP, área_efetiva)
erro        = P_casing − setpoint
abertura    = abertura_ff + Kp · erro + integral
```

O atuador do choke tem inércia: a posição segue o comando com lag de 1ª ordem (~2 s), evitando reação instantânea a mudanças de bomba. Com bomba parada o choke só alivia excesso sobre o setpoint e volta a fechar, preservando a pressão trapada. O controlador **nunca abre o HCR**: sem caminho de retorno (`!hcrOpen` ou lineup inválido) ele fica inerte, levanta `RETURN_PATH_CLOSED` e a UI mostra `chokeAutoBlockedByReturnPath`.

## Volumes de superfície

```
pit += (q_influxo − q_perda) · dt
```

A sangria debita o pit no momento da ação, sem dupla subtração. Perdas por fratura ocorrem quando a pressão na sapata excede o gradiente de fratura.

## BOP e acumulador

Cada elemento fechado consome óleo do acumulador; a recarga é lenta. O HCR é tratado como linha de choke alinhada, não como elemento de vedação. O fechamento do BOP com bombas paradas leva o poço à fase `SHUT_IN`; após 12 s, `STABILIZED`.

## Fases

`DRILLING → KICK_DETECTED → SHUT_IN → STABILIZED → CIRCULATING → KILLED`

`FAILED` ocorre quando a pressão de revestimento excede 135% da MAASP. `KILL_PREP` existe no modelo para evolução futura.

## Score

O score parte de 100 e penaliza:

- pressão de revestimento próxima/acima da MAASP;
- ganho de pit acima do limite;
- falha de limite;
- gás que alcançou a superfície em qualquer momento (`gasReachedSurface`);
- circular com choke fechado.

Bônus de 5 pontos ao concluir o kill. O resultado é limitado a 0–100.

## Limitações conhecidas

- Fluido monofásico simplificado; não há modelo PVT completo.
- A reologia é simplificada por fatores de atrito; não há modelo Herschel-Bulkley completo.
- O swab/surge é derivado da velocidade da COP, sem acoplamento reológico completo.
- Poços direcionais usam build-and-hold 2D/3D-lite, sem anti-collision nem survey real.
- A trajetória e as camadas são didáticas, não um plano de poço certificado.
- O método é didático; o app não valida procedimento operacional real.
