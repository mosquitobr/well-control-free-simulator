package com.mosquitobr.controlepoco.data

data class Lesson(
    val titlePt: String,
    val titleEn: String,
    val bodyPt: String,
    val bodyEn: String
)

object Academy {
    val lessons = listOf(
        Lesson(
            "Pressão hidrostática",
            "Hydrostatic pressure",
            "A pressão hidrostática depende da densidade da lama e da profundidade vertical: P = 0,052 × MW × TVD (campo) ou P = ρ × g × h (SI). É a base de todo o controle de poço.",
            "Hydrostatic pressure depends on mud density and vertical depth: P = 0.052 × MW × TVD (field) or P = ρ × g × h (SI). It is the basis of all well control."
        ),
        Lesson(
            "SIDPP e SICP",
            "SIDPP and SICP",
            "Com o poço fechado e estabilizado, a SIDPP representa a pressão de poros menos a hidrostática da lama na coluna. A SICP inclui o efeito do influxo no anular e tende a ser maior quando há gás.",
            "With the well shut in and stabilized, SIDPP represents pore pressure minus the mud hydrostatic in the string. SICP includes the annulus influx and is usually higher with gas."
        ),
        Lesson(
            "KMW, ICP e FCP",
            "KMW, ICP and FCP",
            "KMW = MW + SIDPP / (0,052 × TVD). ICP = SIDPP + SCR. FCP = SCR × KMW / MW. O schedule relaciona pressão de bomba e strokes até a broca.",
            "KMW = MW + SIDPP / (0.052 × TVD). ICP = SIDPP + SCR. FCP = SCR × KMW / MW. The schedule links pump pressure and strokes to bit."
        ),
        Lesson(
            "MAASP e MASP",
            "MAASP and MASP",
            "MAASP é a máxima pressão de superfície que ainda não fratura a sapata, com a lama atual. MASP é a máxima pressão de superfície prevista para o cenário. Em submarino, some a fricção da choke line.",
            "MAASP is the maximum surface pressure that will not fracture the shoe with current mud. MASP is the maximum anticipated surface pressure. Subsea adds choke line friction."
        ),
        Lesson(
            "Detecção de kick",
            "Kick detection",
            "Os sinais incluem ganho no pit, aumento de flow out com bombas paradas, redução de SPP, aumento de torque e gás. Faça flow check antes de concluir.",
            "Signs include pit gain, increased flow out with pumps off, SPP drop, torque increase and gas. Perform a flow check before concluding."
        ),
        Lesson(
            "Shut-in",
            "Shut-in",
            "Pare a bomba, mantenha a coluna suspensa, feche o BOP (anular ou pipe rams), abra a linha de choke e registre SIDPP, SICP e ganho. Não circule antes de estabilizar.",
            "Stop the pump, keep the string suspended, close the BOP (annular or pipe rams), open the choke line and record SIDPP, SICP and gain. Do not circulate before stabilization."
        ),
        Lesson(
            "Método do sondador",
            "Driller's method",
            "Mantém a lama original. Circula o influxo com a vazão de kill e controla a pressão de revestimento pelo choke. Depois pesa a lama e circula novamente.",
            "Keeps original mud. Circulates the influx at kill rate and controls casing pressure with the choke. Then weights the mud and circulates again."
        ),
        Lesson(
            "Wait and Weight",
            "Wait and Weight",
            "Pesa a lama até a KMW antes de circular. A SPP segue o schedule calculado e a pressão de revestimento é controlada pelo choke. É mais eficiente, mas exige preparação.",
            "Weights mud to KMW before circulating. SPP follows the calculated schedule and casing pressure is controlled by the choke. More efficient, but requires preparation."
        ),
        Lesson(
            "Método volumétrico",
            "Volumetric method",
            "Com bombas paradas, o gás migra e expande. Sangrias pequenas e controladas mantêm a pressão de revestimento dentro dos limites enquanto o gás sobe.",
            "With pumps off, gas migrates and expands. Small controlled bleeds keep casing pressure within limits while gas rises."
        ),
        Lesson(
            "Lubrificar e sangrar",
            "Lubricate and bleed",
            "Usado quando não é possível circular. Injeta pequenos volumes de lama no anular e sangra gás/fluido pelo choke, mantendo a pressão de fundo acima da pressão de poros.",
            "Used when circulation is not possible. Inject small mud volumes into the annulus and bleed gas/fluid through the choke, keeping bottom-hole pressure above pore pressure."
        ),
        Lesson(
            "BOP e equipamentos",
            "BOP and equipment",
            "O stack combina annular, pipe rams, blind/shear rams e choke/kill lines. O acumulador fornece energia para fechar. Teste e mantenha os pods e HCR alinhados.",
            "The stack combines annular, pipe rams, blind/shear rams and choke/kill lines. The accumulator provides closing energy. Test and keep pods and HCR lined up."
        ),
        Lesson(
            "Submarino",
            "Subsea",
            "O BOP fica no fundo do mar. A choke line adiciona fricção e volume; o riser adiciona hidrostática e margem. EDS, deadman e autoshear protegem a sonda em emergência.",
            "The BOP is on the seabed. The choke line adds friction and volume; the riser adds hydrostatics and margin. EDS, deadman and autoshear protect the rig in an emergency."
        ),
        Lesson(
            "Física da perfuração: WOB, RPM, ROP e torque",
            "Drilling dynamics: WOB, RPM, ROP and torque",
            "A taxa de penetração (ROP) depende do peso sobre a broca (WOB em Klb), rotação (RPM) e hidráulica. Subir a COP reduz o WOB a zero e cessa o ROP; baixar a COP transfere peso. O torque (Klb·ft) responde à interação broca-formação.",
            "Rate of penetration (ROP) depends on weight on bit (WOB in Klb), rotary speed (RPM) and hydraulics. Raising the string slacks off WOB to zero and stops ROP; lowering transfers weight. Torque (Klb·ft) reflects bit-formation interaction."
        ),
        Lesson(
            "Hidráulica: SPP, perdas de carga e ECD",
            "Hydraulics: SPP, pressure losses and ECD",
            "A SPP representa a soma das perdas de carga na superfície, coluna, broca, motor e anular. O ECD inclui a perda anular e a carga de cascalhos (cuttings), sendo sempre superior à densidade estática enquanto houver circulação.",
            "SPP represents the sum of surface, string, bit, motor and annular pressure losses. ECD accounts for annular friction and cuttings loading, consistently exceeding static mud weight during circulation."
        ),
        Lesson(
            "Pressões reduzidas de circulação (SCR / PRC)",
            "Slow circulating rates (SCR / PRC)",
            "A tomada de SCR registra a SPP na taxa reduzida de kill antes das operações. Essa leitura calibra as equações hidráulicas e determina o ICP (ICP = SIDPP + SCR) no cálculo da kill sheet.",
            "Taking an SCR records SPP at reduced kill rate prior to operations. This measurement calibrates hydraulic formulas and establishes ICP (ICP = SIDPP + SCR) on the kill sheet."
        ),
        Lesson(
            "Manobras e procedimentos do sondador",
            "Driller procedures and maneuvers",
            "Operações de campo seguem passos rigorosos no console: parada de bomba, elevação da COP, verificação de fluxo (flow check), monitoramento do trip tank e alinhamento de manifolds antes de fechar o BOP.",
            "Rig floor operations require strict step-by-step console execution: pump shutdown, string spacing, flow checking, trip tank monitoring and manifold lineups before closing the BOP."
        )
    )
}
