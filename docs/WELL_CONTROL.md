# Controle de poço — fundamentos

Resumo conceitual usado no simulador. Não substitui treinamento certificado.

## 1. Pressões

- **Pressão hidrostática**: peso da coluna de fluido. Depende de densidade e TVD.
- **Pressão de poros**: pressão dos fluidos da formação.
- **Pressão de fratura**: pressão que a formação suporta antes de fraturar.
- **SIDPP**: pressão estabilizada na coluna com o poço fechado; indica a diferença entre poros e hidrostática da lama.
- **SICP**: pressão estabilizada no anular; inclui o efeito do influxo.
- **BHP**: pressão no fundo do poço.
- **ECD**: densidade equivalente de circulação, que inclui atrito.
- **MAASP**: máxima pressão de superfície admissível com a lama atual.
- **MASP**: máxima pressão de superfície prevista para o cenário.

## 2. Janela operacional

A lama deve manter:

```
P_poros < BHP < P_fratura
```

Com margem operacional. Em poços profundos e submarinos, a janela entre poros e fratura é estreita; pequenos erros de ECD ou de choke podem gerar kick ou perda.

**Condição de kick:** o influxo só acontece quando a pressão de poros supera a pressão de fundo (`P_poros > BHP`). Se a lama e o ECD mantiverem a BHP acima da pressão de poros, a zona portadora não produz influxo, mesmo com a broca dentro dela. A zona portadora (*carrier zone*) é apenas a rocha que contém o fluido; não é sinônimo de kick programado.

## 3. Detecção de kick

**Indícios primários**

- ganho no pit;
- fluxo de retorno com bombas paradas;
- aumento de volume no trip tank.

**Indícios secundários** (aparecem antes dos primários e justificam atenção imediata)

- aumento de ROP;
- redução de WOB;
- queda de SPP com vazão constante;
- aumento de ECD;
- aceleração espontânea da bomba;
- variação da vazão de retorno em ±10%.

Procedimento didático:

1. Parar a bomba e a rotação.
2. Fazer flow check.
3. Fechar o BOP (anular ou pipe rams) imediatamente: quanto maior o tempo de fechamento, maior o volume de influxo.
4. Abrir a linha de choke e alinhar o manifold.
5. Registrar SIDPP, SICP, ganho e horário.
6. Aguardar estabilização.

## 3.1 Preparo antes do kick (Fase A)

Antes de perfurar a zona portadora, o operador deve:

1. Fazer flow check com as bombas paradas: o guia arma a medição, aguarda 5 s de estabilidade e conclui o item A1 automaticamente.
2. Calibrar o alarme de ganho no pit em **5 bbl** (preferencial) ou **10 bbl**.
3. Calibrar o alarme de perda no pit em **5 bbl** ou **10 bbl**.
4. Calibrar o alarme de variação da vazão de retorno em **±10%**.
5. Registrar a SPP em **50, 100, 150 e 200 gpm**, com a mesma lama e o BOP aberto.
6. Medir o **ΔP da linha de controle (ΔP C.L.)** com o poço fechado e o HCR aberto.
7. Executar o teste funcional do BOP, acionando todos os elementos e o HCR.

As leituras de baixa vazão caracterizam o comportamento hidráulico do poço; o ΔP C.L. corrige a pressão de revestimento quando o influxo sai pela linha de choke.

## 3.2 Condição de kick

O desequilíbrio pode ser classificado por densidade equivalente:

- **Poros acima da hidrostática**: `ED_poros > MW` com o poço estático.
- **Poros acima da ECD**: `ED_poros > ECD` com o poço circulando (ECD corrigida pela temperatura).
- **Zona interna superior**: dentro da formação portadora normal existe uma camada local mais fina com pressão anormalmente alta, gradiente e permeabilidade próprios; o influxo ocorre nessa janela, não na formação inteira. A pressão da zona é validada para ser sempre ≥ à da formação portadora.

O volume efetivo não é instantâneo: cresce pela vazão característica do tipo de influxo (gás > óleo > água) enquanto o BOP está aberto, para no fechamento e pode receber um fator de estabilização se as pressões continuarem subindo. O volume máximo é configurável e limita a entrada.

## 4. Métodos

### Driller's

Mantém a lama original. Circula o influxo com vazão de kill, controlando a pressão de revestimento pelo choke. Depois pesa a lama e circula novamente.

### Wait & Weight

Pesa a lama até a KMW antes de circular. A SPP segue o schedule; a pressão de revestimento é controlada pelo choke. É mais eficiente, mas exige preparação e controle rigoroso.

### Volumétrico

Com bombas paradas, o gás migra e expande. Sangrias pequenas e controladas mantêm a pressão de revestimento dentro dos limites enquanto o gás sobe.

### Lubrificar & sangrar

Usado quando não é possível circular. Injeta pequenos volumes de lama no anular e sangra gás/fluido pelo choke, mantendo BHP acima da pressão de poros.

## 5. Kill sheet

```
KMW = MW + SIDPP / (0,052 · TVD)
ICP = SIDPP + SCR
FCP = SCR · KMW / MW
```

- **KMW**: peso de lama necessário para equilibrar a pressão de poros.
- **ICP**: pressão inicial de circulação.
- **FCP**: pressão final de circulação com a lama nova.
- **Strokes**: volume até a broca, sapata e superfície.
- **Schedule**: SPP esperada em função dos strokes.

## 6. BOP e equipamentos

- **Annular**: vedação em torno da coluna; primeira escolha para kick.
- **Pipe ram**: veda contra o tubo; usar quando a coluna está centrada.
- **Blind/shear ram**: veda poço aberto ou corta a coluna em emergência.
- **Test ram**: permite testar a vedação.
- **HCR**: válvula hidráulica da linha de choke.
- **Acumulador**: fornece energia para fechar o BOP; monitorar pressão e recarga.
- **Choke manifold**: controla a saída de fluido e a pressão de revestimento.
- **MGS/flare**: separa gás e lama e queima o gás com segurança.

## 7. Submarino

- O BOP fica no fundo do mar, abaixo da mudline.
- A choke line é longa, adiciona volume e fricção; a pressão de revestimento precisa ser corrigida.
- O riser adiciona hidrostática e margem operacional.
- O gás pode entrar no riser; monitorar e evitar chegada à sonda.
- EDS, deadman e autoshear isolam o poço e protegem a sonda em emergência.
- A janela entre poros e fratura costuma ser estreita; o controle de choke é crítico.

## 8. Segurança

- Nunca abrir o poço sem plano.
- Não circular abaixo da vazão de kill sem controle.
- Manter pressão de revestimento abaixo da MAASP.
- Monitorar pressão na sapata e perdas.
- Comunicar o kick imediatamente.
- Em gás na superfície, priorizar segurança de pessoas e da sonda.
