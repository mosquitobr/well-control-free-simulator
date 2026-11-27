package com.mosquitobr.controlepoco.core

/**
 * Typed well control method classification decoupled from display strings.
 */
enum class KillMethod {
    DRILLERS,
    WAIT_AND_WEIGHT,
    VOLUMETRIC,
    LUBRICATE_AND_BLEED,
    TUTORIAL,
    EMERGENCY,
    OTHER
}

/**
 * Lightweight bilingual localization layer for :core operational messages,
 * alarms, milestones, recommendations, and Kill Sheet notices.
 *
 * Keeps internal physics and existing PT-BR default tests 100% compatible while
 * guaranteeing zero Portuguese leakage when the UI runs in English ("en").
 */
object CoreI18n {

    fun isEnglish(lang: String): Boolean = lang.startsWith("en", ignoreCase = true)

    fun methodOf(raw: String): KillMethod = when (raw.trim().lowercase()) {
        "volumétrico", "volumetrico", "volumetric" -> KillMethod.VOLUMETRIC
        "wait & weight", "wait and weight" -> KillMethod.WAIT_AND_WEIGHT
        "lubrificar & sangrar", "lubrificar e sangrar", "lubricate & bleed", "lubricate and bleed" ->
            KillMethod.LUBRICATE_AND_BLEED
        "tutorial" -> KillMethod.TUTORIAL
        "emergência", "emergencia", "emergency" -> KillMethod.EMERGENCY
        "sondador", "driller's", "drillers" -> KillMethod.DRILLERS
        else -> KillMethod.OTHER
    }

    private val exactToEn: Map<String, String> = mapOf(
        "Buraco aberto" to "Open hole",
        "Formação" to "Formation",
        "Estabilize o poço fechado antes de ajustar o fator de segurança." to
            "Stabilize the shut-in well before adjusting the safety factor.",
        "Fator de segurança não pode ficar abaixo de SIDPP/SICP estabilizados." to
            "Safety factor cannot be below stabilized SIDPP/SICP.",
        "Estabilize o poço fechado antes de definir o par SIDPP/SICP." to
            "Stabilize the shut-in well before setting the SIDPP/SICP pair.",
        "Os valores de segurança devem permanecer iguais ou acima da estabilização." to
            "Safety values must remain equal to or above stabilized pressures.",
        "Choke line deslocada pela 2ª circulação" to
            "Choke line displaced by 2nd circulation",
        "2ª circulação concluída, mas o poço ainda está pressurizado: a lama de matar não equilibra a formação" to
            "2nd circulation completed, but well is still pressurized: kill mud does not balance formation",
        "Poço ainda pressurizado: a lama não equilibra a formação. Não sangre; ajuste o peso da lama." to
            "Well is still pressurized: mud does not balance formation. Do not bleed; adjust mud weight.",
        "Poço morto: lama de matar na COP, anular e choke line" to
            "Well dead: kill mud across drillstring, annulus, and choke line",
        "Poço morto" to "Well dead",
        "Linhas deslocadas: falta sangrar a pressão residual" to
            "Lines displaced: residual pressure bleed required",
        "Poço em falha: não abra o BOP." to "Well in failure state: do not open BOP.",
        "Mantenha o poço fechado: conclua a 1ª e a 2ª circulação até o poço morto." to
            "Keep well shut in: complete 1st and 2nd circulations until well is dead.",
        "Desloque a kill line, o booster e o anular do riser antes de abrir." to
            "Displace kill line, booster line, and riser annulus before opening.",
        "Gás no separador atmosférico (MGS) pelo retorno" to
            "Gas routed to mud-gas separator (MGS) via return",
        "Gás no separador atmosférico (MGS)" to "Gas in mud-gas separator (MGS)",
        "Gás chegou à superfície sem separador" to "Gas reached surface without MGS",
        "Gás chegando à superfície" to "Gas reaching surface",
        "Falha da bomba de lama" to "Mud pump failure",
        "Bomba de lama falhou" to "Mud pump failed",
        "Choke plugado" to "Choke plugged",
        "Choke desgastado" to "Choke washed out",
        "Vazamento no acumulador" to "Accumulator leak",
        "Pressão do acumulador baixa" to "Low accumulator pressure",
        "Acumulador abaixo do mínimo" to "Accumulator below minimum",
        "Perda de circulação" to "Lost circulation",
        "Coluna travada" to "Stuck pipe",
        "Coluna travada: avalie rotação e peso." to "Stuck pipe: evaluate rotation and WOB.",
        "Deriva de sensor" to "Sensor drift",
        "Sensor de pressão com deriva: confirme por segunda fonte." to
            "Pressure sensor drifting: verify with secondary gauge.",
        "Perda de energia" to "Power loss",
        "Volume máximo de kick atingido: feche o poço imediatamente." to
            "Maximum kick volume reached: shut in the well immediately.",
        "Gás no separador atmosférico (MGS) pela choke line" to
            "Gas routed to mud-gas separator (MGS) via choke line",
        "Influxo desviado para a choke line" to "Influx diverted into choke line",
        "Gás expulso do poço — superfície limpa" to "Gas circulated out — surface clear",
        "Choke aberto sem bombeio: poço despressurizando!" to
            "Choke open with pumps off: well depressurizing!",
        "Choke aberto sem bombeio: poço despressurizando! Feche o choke para trapar a pressão." to
            "Choke open with pumps off: well depressurizing! Close choke to trap pressure.",
        "Fluxo com bombas paradas" to "Flow with pumps off",
        "MAASP excedida" to "MAASP exceeded",
        "Pressão de revestimento próxima da MAASP" to "Casing pressure approaching MAASP",
        "Pressão na sapata acima da fratura" to "Shoe pressure exceeded fracture limit",
        "SPP baixa com bomba ligada" to "Low SPP with pump running",
        "Poço fechado (shut-in)" to "Well shut in",
        "Poço fechado" to "Well shut in",
        "Pressões estabilizadas" to "Pressures stabilized",
        "1ª circulação de kill iniciada" to "1st kill circulation started",
        "1ª circulação em 100% do volume de circulação (anular + choke line)" to
            "1st circulation at 100% circulating volume (annulus + choke line)",
        "100% do volume circulado, mas ainda há influxo: mantenha a circulação até expulsar o gás." to
            "100% volume circulated, but influx remains: keep circulating until gas is removed.",
        "Falha: limite de pressão excedido" to "Failure: pressure limit exceeded",
        "Limite de pressão excedido" to "Pressure limit exceeded",
        "Pare a bomba, faça flow check e feche o BOP." to
            "Stop pump, perform flow check, and close BOP.",
        "Aguarde estabilização e confirme as pressões." to
            "Wait for stabilization and confirm shut-in pressures.",
        "1ª circulação: remova o influxo mantendo a lama velha." to
            "1st circulation: circulate out influx using original mud.",
        "Mantenha a pressão de revestimento dentro da MAASP." to
            "Keep casing pressure within MAASP.",
        "Pressão trapada: pese a lama até a KMW antes da 2ª circulação." to
            "Trapped pressure held: weight mud up to KMW before 2nd circulation.",
        "Abra a segunda circulação e siga ICP → FCP no kill sheet." to
            "Start 2nd circulation and follow ICP → FCP on the kill sheet.",
        "2ª circulação: substitua a lama velha pela lama de matar." to
            "2nd circulation: displace original mud with kill mud.",
        "Choke automático segue o kill sheet; não deixe a pressão cair." to
            "Auto choke follows the kill sheet; do not allow pressure to drop.",
        "Poço morto: desloque kill line, booster e anular do riser." to
            "Well dead: displace kill line, booster line, and riser annulus.",
        "Sangre a pressão residual pelo choke até ≈ 0 e abra o BOP." to
            "Bleed residual pressure through choke to ≈ 0 and open BOP.",
        "Abra o choke com cuidado: pressão próxima da MAASP." to
            "Open choke carefully: pressure is near MAASP.",
        "Houve risco de perda: reduza a pressão na sapata." to
            "Lost circulation risk detected: reduce shoe pressure.",
        "Bomba indisponível." to "Pump unavailable.",
        "Alinhado em TRIP: volte os manifolds para Perfuração antes de ligar a bomba principal." to
            "Lined up on TRIP: return manifolds to Drilling before starting main pump.",
        "Bomba parada" to "Pump stopped",
        "Rotação desligada" to "Rotary stopped",
        "Ligue a bomba e estabilize a vazão para tomar a SCR." to
            "Start pump and stabilize rate to record SCR.",
        "Abra o BOP para tomar a SCR com o poço em circulação." to
            "Open BOP to record SCR while circulating.",
        "SPP inválida para registrar a SCR." to "Invalid SPP to record SCR.",
        "Vazões de preparo válidas: 50, 100, 150 e 200 gpm." to
            "Valid prep rates: 50, 100, 150, and 200 gpm.",
        "Abra o BOP para tomar as leituras de baixa vazão com o poço circulando." to
            "Open BOP to record slow circulating rates while circulating.",
        "Feche o BOP para medir o Delta P da linha de controle (Delta P C.L.)." to
            "Close BOP to measure choke line friction (Delta P C.L.).",
        "Abra o HCR: a linha de controle precisa estar alinhada para a leitura." to
            "Open HCR: choke line must be lined up for the reading.",
        "Ligue a bomba em vazão reduzida para medir o Delta P C.L." to
            "Start pump at slow circulating rate to measure Delta P C.L.",
        "Delta P C.L. desprezível nessa vazão. Aumente a vazão e repita." to
            "Negligible Delta P C.L. at this rate. Increase rate and repeat.",
        "Procedimento cancelado" to "Procedure cancelled",
        "Som de alarme ativado" to "Alarm sound enabled",
        "Som de alarme silenciado" to "Alarm sound muted",
        "Log de ganho retomado" to "Pit gain log resumed",
        "Log de ganho pausado" to "Pit gain log paused",
        "Alinhamento inválido: BOP aberto para manobra" to
            "Invalid lineup: BOP open for tripping",
        "Alinhamento inválido: feche o BOP antes do alinhamento de manobra." to
            "Invalid lineup: close BOP before switching to tripping lineup.",
        "Kick já detectado: configure a zona antes do gatilho." to
            "Kick already detected: configure kick zone before trigger.",
        "Configuração do kick atualizada." to "Kick configuration updated.",
        "Kick já iniciado neste cenário." to "Kick already started in this scenario.",
        "Choke automático ativo" to "Auto choke enabled",
        "Choke manual" to "Manual choke",
        "HCR aberto: linha de choke alinhada" to "HCR open: choke line lined up",
        "HCR aberto" to "HCR open",
        "HCR fechado" to "HCR closed",
        "BOP aberto." to "BOP open.",
        "BOP aberto com poço morto e linhas deslocadas" to
            "BOP opened with well dead and lines displaced",
        "BOP aberto: poço morto, lama de matar estabilizada." to
            "BOP open: well dead, kill mud stabilized.",
        "Simulação em falha: use o reset completo." to
            "Simulation in failure state: use full reset.",
        "Sem kill sheet ativo: aguarde um kick e estabilize o poço fechado." to
            "No active kill sheet: wait for a kick and stabilize shut-in well.",
        "A 1ª circulação ainda não está pronta. Bombeie 100% do volume de circulação (anular + choke line)." to
            "1st circulation not ready yet. Pump 100% of circulating volume (annulus + choke line).",
        "1ª circulação concluída: pressão trapada" to
            "1st circulation completed: trapped pressure held",
        "Aguarde estabilizar SIDPP/SICP e pese a lama antes da circulação Wait & Weight." to
            "Wait for SIDPP/SICP stabilization and weight up mud before Wait & Weight circulation.",
        "Conclua a 1ª circulação e mantenha a pressão trapada antes da 2ª circulação." to
            "Complete 1st circulation and hold trapped pressure before 2nd circulation.",
        "Kill sheet indisponível: estabilize SIDPP/SICP antes de circular." to
            "Kill sheet unavailable: stabilize SIDPP/SICP before circulating.",
        "Circulação Wait & Weight iniciada: lama de matar a caminho da broca" to
            "Wait & Weight circulation started: kill mud heading to bit",
        "2ª circulação iniciada: lama de matar a caminho da broca" to
            "2nd circulation started: kill mud heading to bit",
        "2ª circulação: siga ICP → FCP e mantenha o choke no kill sheet." to
            "2nd circulation: follow ICP → FCP and track kill sheet on choke.",
        "Desloque as linhas somente depois de matar o poço." to
            "Displace lines only after well is dead.",
        "Linha não exigida neste cenário." to "Line not required in this scenario.",
        "Linhas já deslocadas: sangre a pressão residual e abra o BOP." to
            "Lines already displaced: bleed residual pressure and open BOP.",
        "Todas as linhas deslocadas com lama de matar" to
            "All lines displaced with kill mud",
        "Sangre a pressão residual somente com o poço morto." to
            "Bleed residual pressure only when well is dead.",
        "Desloque todas as linhas antes de sangrar a pressão residual." to
            "Displace all lines before bleeding residual pressure.",
        "Poço ainda pressurizado: a lama não equilibra a formação." to
            "Well is still pressurized: mud does not balance formation.",
        "Pressão residual sangrada: pronto para abrir o BOP" to
            "Residual pressure bled: ready to open BOP",
        "Feche o BOP antes de sangrar." to "Close BOP before bleeding.",
        "Abra o HCR e o choke (e valide o alinhamento) para sangrar." to
            "Open HCR and choke (and verify lineup) to bleed.",
        "Sem pressão suficiente para sangrar." to "Insufficient pressure to bleed.",
        "Feche o BOP antes de lubrificar." to "Close BOP before lubricating.",
        "Volume insuficiente no pit." to "Insufficient pit volume.",
        "Flow check já registrado para este poço." to "Flow check already recorded for this well.",
        "Flow check armado: aguardando as bombas pararem para medir o retorno." to
            "Flow check armed: waiting for pumps to stop to measure return flow.",
        "Flow check em andamento: meça o retorno com as bombas paradas." to
            "Flow check in progress: monitor return flow with pumps off.",
        "FLOW CHECK: sem fluxo. Poço estático." to "FLOW CHECK: no flow. Well static.",
        "Feche o BOP antes de alinhar o kill manifold para controle." to
            "Close BOP before lining up kill manifold for well control.",
        "Linhas de kill alinhadas: standpipe e choke em KILL, HCR aberto." to
            "Kill lines lined up: standpipe and choke on KILL, HCR open.",
        "Abra o BOP antes de manobrar a coluna." to "Open BOP before tripping drillstring.",
        "Pare as bombas antes de manobrar." to "Stop pumps before tripping.",
        "Desligue a rotação antes de manobrar." to "Stop rotary before tripping.",
        "Alinhamento incompatível para manobra com BOP aberto." to
            "Incompatible lineup for tripping with BOP open.",
        "Volte os manifolds para Perfuração ou Trip antes de manobrar." to
            "Return manifolds to Drilling or Trip before tripping.",
        "Coluna no limite do poço." to "Bit reached well depth limit.",
        "Swab: influxo durante manobra" to "Swab: influx during tripping",
        "Abra o BOP antes de preencher o poço pelo trip tank." to
            "Open BOP before filling hole via trip tank.",
        "Com a bomba ligada o poço já está em circulação." to
            "Pump is running; well is already circulating.",
        "Alinhamento inválido para preenchimento." to "Invalid lineup for hole fill.",
        "Poço já está cheio." to "Well is already full.",
        "Volume insuficiente no Trip Tank: reabasteça pelo pit antes de preencher." to
            "Insufficient Trip Tank volume: refill from active pit first.",
        "Volume insuficiente no pit para transferir ao Trip Tank." to
            "Insufficient active pit volume to transfer to Trip Tank.",
        "Aguarde 5 s com vazão e SPP estáveis." to "Wait 5 s with stable flow rate and SPP.",
        "Confirme para registrar SPP, SPM e vazão." to "Confirm to record SPP, SPM, and flow rate.",
        "Confirme a pressão do acumulador." to "Confirm accumulator pressure.",
        "Bomba na taxa de kill" to "Pump at kill rate",
        "Estabilizar vazão e SPP" to "Stabilize flow and SPP",
        "Lama de kill até a broca" to "Kill mud to bit",
        "Lama de kill até a sapata" to "Kill mud to shoe",
        "Deslocamento até a superfície" to "Displacement to surface"
    )

    private val patternRules: List<Pair<Regex, (MatchResult) -> String>> = listOf(
        Regex("^Cenário iniciado · (.+)$") to { "Scenario started · ${it.groupValues[1]}" },
        Regex("^BOP fechado: (.+)$") to { "BOP closed: ${it.groupValues[1]}" },
        Regex("^BOP aberto: (.+)$") to { "BOP opened: ${it.groupValues[1]}" },
        Regex("^Abertura bloqueada: (.+)$") to { "Opening blocked: ${translate(it.groupValues[1], "en")}" },
        Regex("^Bomba ligada: (.+) SPM$") to { "Pump started: ${it.groupValues[1]} SPM" },
        Regex("^Rotação ligada: (.+) RPM$") to { "Rotary started: ${it.groupValues[1]} RPM" },
        Regex("^Kick injetado · (.+) bbl$") to { "Kick injected · ${it.groupValues[1]} bbl" },
        Regex("^Kick injetado manualmente: (.+) bbl\\.$") to { "Manual kick injected: ${it.groupValues[1]} bbl." },
        Regex("^Kick detectado · ganho de (.+) bbl$") to { "Kick detected · ${it.groupValues[1]} bbl gain" },
        Regex("^Kick detectado: indício primário confirmado \\((.+) bbl\\)$") to
            { "Kick detected: primary indicator confirmed (${it.groupValues[1]} bbl)" },
        Regex("^Volume máximo de kick atingido \\((.+) bbl\\)$") to
            { "Maximum kick volume reached (${it.groupValues[1]} bbl)" },
        Regex("^Estabilização pós-fechamento: fator (.+) aplicado ao volume$") to
            { "Post-shut-in stabilization: factor ${it.groupValues[1]} applied to influx volume" },
        Regex("^Sangre a pressão residual pelo choke antes de abrir \\((.+) psi\\)\\.$") to
            { "Bleed residual pressure through choke before opening (${it.groupValues[1]} psi)." },
        Regex("^Poço ainda pressurizado \\(SICP (.+) psi\\): a lama de matar não equilibra a formação\\.$") to
            { "Well still pressurized (SICP ${it.groupValues[1]} psi): kill mud does not balance formation." },
        Regex("^Poço ainda pressurizado \\((.+) psi\\): não sangre nem abra o BOP; ajuste o peso da lama\\.$") to
            { "Well still pressurized (${it.groupValues[1]} psi): do not bleed or open BOP; adjust mud weight." },
        Regex("^Poço ainda pressurizado \\(SICP (.+) psi\\)\\. Não sangre: ajuste o peso da lama até a KMW\\.$") to
            { "Well still pressurized (SICP ${it.groupValues[1]} psi). Do not bleed: weight mud up to KMW." },
        Regex("^Pesando lama para (.+) ppg$") to { "Weighting mud to ${it.groupValues[1]} ppg" },
        Regex("^Pese a lama até (.+) ppg \\(KMW \\+ margem\\) antes de iniciar a circulação com lama de matar\\.$") to
            { "Weight mud up to ${it.groupValues[1]} ppg (KMW + margin) before circulating kill mud." },
        Regex("^Pressão trapada: (.+) psi\\. Pese a lama até a KMW; ao parar a bomba, mantenha o choke fechado para não despressurizar\\.$") to
            { "Trapped pressure: ${it.groupValues[1]} psi. Weight mud to KMW; keep choke closed when stopping pump." },
        Regex("^1ª circulação concluída: pressão trapada preservada(.+)$") to
            { "1st circulation completed: trapped pressure preserved${it.groupValues[1]}" },
        Regex("^Pressão residual sangrada \\((.+) → 0 psi\\)\\.(.*)$") to
            { "Residual pressure bled (${it.groupValues[1]} → 0 psi).${if (it.groupValues[2].isNotBlank()) " Ready to open BOP." else ""}" },
        Regex("^Choke ativo: (.+)$") to { "Active choke: ${it.groupValues[1]}" },
        Regex("^Choke ativo alterado para (.+) — reajuste a abertura e observe a pressão de casing\\.$") to
            { "Active choke switched to ${it.groupValues[1]} — adjust opening and monitor casing pressure." },
        Regex("^Manifolds alinhados: (.+)$") to { "Manifolds lined up: ${it.groupValues[1]}" },
        Regex("^Alarme (.+) silenciado por (.+) min$") to { "Alarm ${it.groupValues[1]} muted for ${it.groupValues[2]} min" },
        Regex("^Alarme de ganho calibrado em (.+) bbl\\.$") to { "Pit gain alarm calibrated at ${it.groupValues[1]} bbl." },
        Regex("^Alarme de perda calibrado em (.+) bbl\\.$") to { "Pit loss alarm calibrated at ${it.groupValues[1]} bbl." },
        Regex("^Alarme de vazão de retorno calibrado em ±(.+)%\\.$") to { "Return flow alarm calibrated at ±${it.groupValues[1]}%." },
        Regex("^Pit de referência reancorado em (.+) m³\\.$") to { "Reference pit reset to ${it.groupValues[1]} m³." },
        Regex("^FLOW CHECK: fluxo presente \\((.+) L/min\\)\\. Feche o poço\\.$") to
            { "FLOW CHECK: flow detected (${it.groupValues[1]} L/min). Shut in the well." },
        Regex("^Sangrado (.+) bbl$") to { "Bled ${it.groupValues[1]} bbl" },
        Regex("^Lubrificado (.+) bbl$") to { "Lubricated ${it.groupValues[1]} bbl" },
        Regex("^Coluna em (.+) m MD \\((.+) m TVD\\)$") to { "Bit at ${it.groupValues[1]} m MD (${it.groupValues[2]} m TVD)" },
        Regex("^Poço preenchido pelo Trip Tank: (.+) bbl$") to { "Hole filled via Trip Tank: ${it.groupValues[1]} bbl" },
        Regex("^Transferido (.+) bbl do pit para o Trip Tank\\.$") to { "Transferred ${it.groupValues[1]} bbl from pit to Trip Tank." },
        Regex("^Linha deslocada: (.+)$") to { "Line displaced: ${it.groupValues[1]}" },
        Regex("^Procedimento iniciado: (.+)$") to { "Procedure started: ${it.groupValues[1]}" },
        Regex("^Procedimento concluído: (.+)$") to { "Procedure completed: ${it.groupValues[1]}" },
        Regex("^Guia reiniciada: perfuração retomada em (.+) m$") to { "Guide reset: drilling resumed at ${it.groupValues[1]} m" },
        Regex("^Perfuração: WOB (.+)$") to { "Drilling: WOB ${it.groupValues[1]}" },
        Regex("^Variação da vazão de retorno: (.+)%(.*)$") to { "Return flow deviation: ${it.groupValues[1]}%" },
        Regex("^Fator de segurança aplicado: \\+(.+) psi(.*)$") to { "Safety factor applied: +${it.groupValues[1]} psi" },
        Regex("^Ligue a bomba e estabilize a vazão antes de registrar a leitura de (.+) gpm\\.$") to
            { "Start pump and stabilize flow before recording ${it.groupValues[1]} gpm reading." },
        Regex("^SPP inválida para registrar a leitura de (.+) gpm\\.$") to
            { "Invalid SPP to record ${it.groupValues[1]} gpm reading." },
        Regex("^Delta P C\\.L\\.: (.+) psi$") to { "Choke Line Delta P: ${it.groupValues[1]} psi" }
    )

    fun translate(text: String, lang: String): String {
        if (!isEnglish(lang) || text.isBlank()) return text
        exactToEn[text]?.let { return it }
        for ((regex, builder) in patternRules) {
            val match = regex.matchEntire(text)
            if (match != null) return builder(match)
        }
        return text
    }

    fun localizeKillSheet(result: KillSheetResult, lang: String): KillSheetResult {
        if (!isEnglish(lang)) return result
        val translatedWarnings = result.warnings.map { w ->
            when {
                w.startsWith("SICP menor que SIDPP") ->
                    "SICP lower than SIDPP: check stabilization and sensors."
                w.startsWith("SIDPP não positivo") ->
                    "Non-positive SIDPP: confirm the well is shut in and stabilized."
                w.startsWith("Lama de kill acima da pressão") ->
                    "Kill mud exceeds shoe fracture pressure: lost circulation risk."
                w.startsWith("Margem de segurança aplicada:") ->
                    w.replace("Margem de segurança aplicada:", "Safety margin applied:")
                w.startsWith("Informe a fricção da choke/kill line") ->
                    "Provide choke/kill line friction for the subsea scenario."
                w.startsWith("SCR estimada:") ->
                    "Estimated SCR: take an SCR reading at kill rate before circulating."
                w.startsWith("Dados de lama/profundidade inválidos") ->
                    "Invalid mud weight or depth data."
                else -> translate(w, lang)
            }
        }
        val translatedSchedule = result.schedule.map { row ->
            row.copy(stage = translate(row.stage, lang))
        }
        return result.copy(warnings = translatedWarnings, schedule = translatedSchedule)
    }
}
