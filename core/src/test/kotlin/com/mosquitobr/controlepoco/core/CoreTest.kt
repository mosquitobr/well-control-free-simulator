package com.mosquitobr.controlepoco.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreTest {
    @Test
    fun `conversoes de unidades`() {
        assertEquals(1198.264, Units.ppgToKgM3(10.0), 0.01)
        assertEquals(6894.757, Units.psiToKpa(1000.0), 0.01)
        assertEquals(0.158987, Units.bblToM3(1.0), 0.000001)
        assertEquals(6.2898, Units.m3ToBbl(1.0), 0.001)
        assertEquals(1000.0, Units.mToFt(304.8), 0.001)
        // Petrobras: Klb, Klb·ft, bbl/min
        assertEquals(22.48, Units.knToKlb(100.0), 0.02)
        assertEquals(100.0, Units.klbToKn(Units.knToKlb(100.0)), 0.01)
        assertEquals(73.76, Units.knmToKlbFt(100.0), 0.02)
        assertEquals(100.0, Units.klbFtToKnm(Units.knmToKlbFt(100.0)), 0.01)
        assertEquals(1.0, Units.lpmToBblMin(158.987), 0.01)
        assertEquals(158.99, Units.bblMinToLpm(1.0), 0.01)
    }

    @Test
    fun `hidrostatica basica`() {
        val p = Units.hydrostaticKpa(1200.0, 1000.0)
        assertEquals(11767.98, p, 1.0)
    }

    @Test
    fun `volumes anulares sao positivos e crescentes`() {
        val scenario = Scenarios.byId("drillers-surface")
        val geometry = WellGeometry(scenario.well, scenario.drillString)
        val v1 = geometry.annularVolumeToDepth(1000.0)
        val v2 = geometry.annularVolumeToDepth(2000.0)
        assertTrue(v1 > 0.0)
        assertTrue(v2 > v1)
        assertTrue(geometry.strokesToBit(3000.0, 11.8) > 0.0)
    }

    @Test
    fun `kill sheet calcula kmw icp e maasp`() {
        val input = KillSheetInput(
            currentMudKgM3 = 1200.0,
            tvdM = 3000.0,
            sidppKPa = 700.0,
            sicpKPa = 900.0,
            scrKPa = 1800.0,
            killRateLpm = 900.0,
            pumpOutputLPerStroke = 11.8,
            shoeTvdM = 2000.0,
            fracGradientKPaPerM = 18.0,
            strokesToBit = 500.0,
            strokesToShoe = 800.0,
            totalStrokes = 1600.0
        )
        val result = KillSheetCalculator.calculate(input)
        assertEquals(1223.79, result.killMudKgM3, 0.5)
        assertEquals(2500.0, result.icpKPa, 0.1)
        assertEquals(1835.69, result.fcpKPa, 1.0)
        assertEquals(12464.0, result.maaspKPa, 1.0)
        assertEquals(13, result.schedule.size)
    }

    @Test
    fun `kick e detectado e o poco fecha`() {
        val scenario = Scenarios.byId("drillers-surface")
        val sim = Simulator(scenario)
        repeat(12) { sim.advance(2.0) }
        val detected = sim.snapshot()
        assertTrue(detected.kickDetected)
        assertTrue(detected.influxM3 > 0.0)
        sim.setPump(false, 0.0)
        sim.closeBop(BopElement.ANNULAR)
        repeat(4) { sim.advance(2.0) }
        val shut = sim.snapshot()
        assertTrue(shut.closedBop)
        assertTrue(shut.sicpKPa >= 0.0)
        assertNotNull(sim.killSheetResult())
    }

    @Test
    fun `kick dinamico cresce pela vazao caracteristica ate o volume maximo`() {
        val scenario = Scenarios.custom(
            CustomWellSpec(
                id = "custom:dynamic-kick",
                name = "Kick dinâmico",
                tvdM = 600.0,
                mdM = 600.0,
                shoeTvdM = 350.0,
                poreGradientKPaPerM = 12.0,
                fracGradientKPaPerM = 22.0,
                mudPpg = 9.6,
                kickEnabled = true,
                kickTriggerMode = KickTriggerMode.IMMEDIATE,
                kickVolumeM3 = 0.5,
                maxKickVolumeM3 = 0.5,
                characteristicRateLpm = 600.0,
                zoneTopMdM = 600.0,
                zoneBaseMdM = 600.0,
                initialBitDepthMdM = 600.0,
                initialSpm = 0.0
            )
        )
        val sim = Simulator(scenario)
        sim.advance(2.0)
        val v1 = sim.snapshot().influxM3
        sim.advance(10.0)
        val v2 = sim.snapshot().influxM3
        assertTrue("O volume deve crescer com o tempo de BOP aberto", v2 > v1)
        assertTrue("O volume não pode passar do máximo", v2 <= 0.5 + 1e-6)

        sim.closeBop(BopElement.ANNULAR)
        val atClose = sim.snapshot().influxM3
        repeat(10) { sim.advance(2.0) }
        val afterClose = sim.snapshot().influxM3
        assertTrue(
            "BOP fechado deve interromper a entrada principal de influxo",
            afterClose - atClose < 0.06
        )
        assertTrue(
            "O volume fechado não pode passar do máximo",
            afterClose <= 0.5 + 1e-6
        )
        assertTrue(
            "O fator de estabilização deve ficar na faixa documentada",
            sim.snapshot().kickStabilizationFactor in 1.0..1.30
        )
    }

    @Test
    fun `volume maximo limita o kick mesmo com bop aberto por muito tempo`() {
        val scenario = Scenarios.custom(
            CustomWellSpec(
                id = "custom:kick-cap",
                name = "Kick limitado",
                tvdM = 600.0,
                mdM = 600.0,
                shoeTvdM = 350.0,
                poreGradientKPaPerM = 12.0,
                fracGradientKPaPerM = 22.0,
                mudPpg = 9.6,
                kickEnabled = true,
                kickTriggerMode = KickTriggerMode.IMMEDIATE,
                kickVolumeM3 = 0.4,
                maxKickVolumeM3 = 0.4,
                characteristicRateLpm = 1200.0,
                zoneTopMdM = 600.0,
                zoneBaseMdM = 600.0,
                initialBitDepthMdM = 600.0,
                initialSpm = 0.0
            )
        )
        val sim = Simulator(scenario)
        repeat(60) { sim.advance(2.0) }
        assertEquals("O volume deve parar no máximo configurado", 0.4, sim.snapshot().influxM3, 1e-6)
    }

    @Test
    fun `flow check conclui apos estabilidade com bombas paradas`() {
        val sim = Simulator(Scenarios.byId("fundamentos"))
        sim.flowCheck()
        assertTrue("O flow check deve ficar em andamento", sim.snapshot().flowCheckRunning)
        var guard = 0
        while (sim.snapshot().flowCheckRunning && guard < 120) {
            sim.advance(1.0)
            guard++
        }
        val snap = sim.snapshot()
        assertTrue(
            "O flow check deve concluir o item A1",
            snap.prepChecks.any { it.id == PrepCheckId.FLOW_CHECK && it.done }
        )
        assertTrue("O progresso deve chegar a 100%", snap.flowCheckProgress >= 1.0)
    }

    @Test
    fun `b6 subsea exclui riser e soma apenas anular ate o bop e choke line`() {
        val scenario = Scenarios.byId("subsea-drillers")
        val geometry = WellGeometry(scenario.well, scenario.drillString)
        val bit = scenario.initialBitDepthM
        val annulusBop = geometry.annularVolumeToBop(bit)
        val choke = geometry.chokeLineVolumeM3()
        val total = geometry.circulationVolumeToSurface(bit)
        assertEquals("B6 = anular broca→BOP + choke line", annulusBop + choke, total, 1e-6)
        assertTrue("A choke line deve ter volume", choke > 0.0)
        assertTrue(
            "O anular completo inclui o riser isolado e deve ser maior",
            geometry.annularVolumeToDepth(bit) > annulusBop + 1e-6
        )
        assertTrue(
            "O B6 não pode incluir o riser",
            total < geometry.annularVolumeToDepth(bit) + choke
        )
    }

    @Test
    fun `subsea tem riser e choke line`() {
        val scenario = Scenarios.byId("subsea-drillers")
        assertTrue(scenario.well.isSubsea)
        assertEquals(1200.0, scenario.well.riser!!.waterDepthM, 0.1)
        val sim = Simulator(scenario)
        repeat(14) { sim.advance(2.0) }
        val snap = sim.snapshot()
        assertTrue(snap.stackType == StackType.SUBSEA)
        assertTrue(snap.maspKPa >= 0.0)
    }

    @Test
    fun `todos os cenarios abrem`() {
        assertTrue(Scenarios.all.size >= 12)
        Scenarios.all.forEach { scenario ->
            val sim = Simulator(scenario)
            sim.advance(1.0)
            assertTrue(sim.snapshot().score in 0..100)
        }
    }

    // --- v2: trajetória, camadas, gatilhos, caminho do gás, alarmes, manobra e 16x ---

    @Test
    fun `trajetoria build and hold mantem MD e TVD coerentes`() {
        val well = Scenarios.byId("directional-hp").well
        assertTrue(well.mdM > well.tvdM)
        assertEquals(well.tvdM, well.tvdAtMd(well.mdM), 0.5)
        assertEquals(well.mdM, well.mdAtTvd(well.tvdM), 1.0)
        assertTrue(well.trajectory.size >= 3)
        assertTrue(well.incAtMd(well.mdM) > 20.0)
        assertEquals(0.0, well.tvdAtMd(0.0), 0.01)
        // TVD deve crescer de forma monotônica ao longo do MD.
        var previous = -1.0
        var md = 0.0
        while (md <= well.mdM) {
            val tvd = well.tvdAtMd(md)
            assertTrue(tvd >= previous - 0.001)
            previous = tvd
            md += 100.0
        }
    }

    @Test
    fun `camadas de formacao controlam poros fratura e pressao`() {
        val well = Scenarios.byId("narrow-window").well
        assertTrue(well.formations.size >= 3)
        well.formations.forEach { layer ->
            assertTrue(layer.fracGradientKPaPerM > layer.poreGradientKPaPerM)
            assertTrue(layer.hardness > 0.0)
        }
        val reservoir = well.formations.first { it.kickZone }
        val pore = well.porePressureAtMd(reservoir.topMdM)
        val frac = well.fracPressureAtMd(reservoir.topMdM)
        assertTrue(pore > 0.0)
        assertTrue(frac > pore)
        assertTrue(well.porePressureKPa > 0.0)
        val shoePore = well.porePressureAtMd(well.mdAtTvd(well.shoeTvdM))
        assertTrue(well.shoeFracKPa > shoePore)
    }

    @Test
    fun `gatilho por profundidade perfura ate a zona e injeta kick`() {
        val scenario = Scenarios.byId("depth-trigger")
        assertEquals(KickTriggerMode.DEPTH, scenario.kick.triggerMode)
        val sim = Simulator(scenario)
        assertTrue(!sim.snapshot().kickTriggered)
        sim.setDrilling(70.0, 80.0)
        var guard = 0
        while (!sim.snapshot().kickTriggered && guard < 180) {
            sim.advance(1.0)
            guard++
        }
        val snap = sim.snapshot()
        assertTrue("Kick não disparou por profundidade", snap.kickTriggered)
        assertTrue(snap.bitDepthM >= scenario.kick.triggerDepthMdM)
        assertTrue(snap.influxM3 > 0.0)
        assertTrue(snap.milestones.any { it.contains("Kick") })
    }

    @Test
    fun `gatilho por tempo dispara no instante configurado`() {
        val scenario = Scenarios.byId("narrow-window")
        assertEquals(KickTriggerMode.TIME, scenario.kick.triggerMode)
        val sim = Simulator(scenario)
        sim.advance(5.0)
        assertTrue(!sim.snapshot().kickTriggered)
        repeat(12) { sim.advance(2.0) }
        val snap = sim.snapshot()
        assertTrue(snap.kickTriggered)
        assertTrue(snap.timeSec >= scenario.kick.triggerAtSec)
        assertTrue(snap.influxM3 > 0.0)
    }

    @Test
    fun `kick manual nao dispara antes da acao do operador`() {
        val scenario = Scenarios.byId("swab-trip")
        assertEquals(KickTriggerMode.MANUAL, scenario.kick.triggerMode)
        val sim = Simulator(scenario)
        repeat(10) { sim.advance(2.0) }
        assertTrue(!sim.snapshot().kickTriggered)
        sim.triggerManualKick()
        assertTrue(sim.snapshot().kickTriggered)
        assertTrue(sim.snapshot().influxM3 > 0.0)
    }

    @Test
    fun `gas em subsea fechado segue pela choke line ate o manifold`() {
        val spec = CustomWellSpec(
            id = "custom:test-choke",
            name = "Teste choke line",
            surfaceType = SurfaceType.SUBSEA,
            tvdM = 500.0,
            mdM = 500.0,
            shoeTvdM = 320.0,
            waterDepthM = 120.0,
            mudPpg = 13.0,
            kickEnabled = true,
            kickTriggerMode = KickTriggerMode.MANUAL,
            initialSpm = 0.0
        )
        val scenario = Scenarios.custom(spec)
        assertTrue(scenario.well.isSubsea)
        val sim = Simulator(scenario)
        sim.closeBop(BopElement.ANNULAR)
        sim.setHcr(true)
        sim.triggerManualKick(0.6)
        assertEquals(InfluxPath.CHOKE_LINE, sim.influxPath)
        val mudline = scenario.well.riser?.waterDepthM ?: 0.0
        var guard = 0
        var sawRiser = false
        while (sim.influxTopTvdM > mudline + 1.0 && guard < 1500) {
            sim.advance(2.0)
            if (sim.influxPath == InfluxPath.RISER) sawRiser = true
            guard++
        }
        val snap = sim.snapshot()
        val debug = "guard=$guard time=${snap.timeSec} base=${snap.influxBaseTvdM} top=${snap.influxTopTvdM} vol=${snap.influxM3} path=${snap.influxPath} hcr=${snap.hcrOpen} closed=${snap.closedBop}"
        assertTrue("Gás não chegou ao manifold: $debug", snap.influxTopTvdM <= mudline + 1.0)
        assertTrue("Gás não pode subir pelo riser com BOP fechado: $debug", !sawRiser)
        assertEquals(InfluxPath.CHOKE_LINE, snap.influxPath)
    }

    @Test
    fun `alarme pode ser silenciado e o log de ganho pausado`() {
        val sim = Simulator(Scenarios.byId("sandbox"))
        assertTrue(sim.snapshot().alarmSoundEnabled)
        assertTrue(sim.snapshot().gainLogEnabled)
        sim.setAlarmSound(false)
        sim.setGainLog(false)
        val muted = sim.snapshot()
        assertTrue(!muted.alarmSoundEnabled)
        assertTrue(!muted.gainLogEnabled)
        sim.muteAlarm(AlarmCode.PIT_GAIN, 60.0)
        assertTrue(sim.message?.contains("silenciado") == true)
        sim.setAlarmSound(true)
        sim.setGainLog(true)
        assertTrue(sim.snapshot().alarmSoundEnabled)
        assertTrue(sim.snapshot().gainLogEnabled)
    }

    @Test
    fun `manobra preenche poco e valida alinhamento dos manifolds`() {
        val scenario = Scenarios.byId("swab-trip")
        val sim = Simulator(scenario)
        val startDepth = sim.bitDepthM
        sim.tripPipe(-27.0)
        val pulled = sim.snapshot()
        assertTrue(pulled.bitDepthM < startDepth)
        assertTrue(pulled.holeFillM3 > 0.0)
        sim.fillHole(0.5)
        assertTrue(sim.snapshot().holeFillM3 <= pulled.holeFillM3)

        val sandbox = Simulator(Scenarios.byId("sandbox"))
        sandbox.setLineups(ManifoldLineup.DRILLING, ManifoldLineup.TRIP)
        assertTrue(!sandbox.snapshot().lineupValid)
        sandbox.closeBop(BopElement.ANNULAR)
        sandbox.setLineups(ManifoldLineup.TRIP, ManifoldLineup.TRIP)
        assertTrue(sandbox.snapshot().lineupValid)
        sandbox.setLineups(ManifoldLineup.KILL, ManifoldLineup.KILL)
        assertTrue("O HCR não deve abrir sozinho: é controle manual", !sandbox.snapshot().hcrOpen)
        sandbox.setHcr(true)
        assertTrue(sandbox.snapshot().hcrOpen)
        sandbox.setLineups(ManifoldLineup.DRILLING, ManifoldLineup.DRILLING)
        assertTrue("O HCR não deve fechar sozinho ao voltar para perfuração", sandbox.snapshot().hcrOpen)
    }

    @Test
    fun `simulacao em 16x permanece estavel`() {
        val scenario = Scenarios.byId("drillers-surface")
        val sim = Simulator(scenario)
        sim.setDrilling(60.0, 90.0)
        repeat(300) { sim.advance(1.6) }
        val snap = sim.snapshot()
        assertTrue(snap.timeSec > 400.0)
        assertTrue(snap.bhpKPa.isFinite())
        assertTrue(snap.sppKPa.isFinite())
        assertTrue(snap.casingPressureKPa.isFinite())
        assertTrue(snap.pitM3 >= 0.0)
        assertTrue(snap.score in 0..100)
        assertTrue(snap.milestones.isNotEmpty())
    }

    @Test
    fun `cronograma e marcos registram a operacao`() {
        val sim = Simulator(Scenarios.byId("drillers-surface"))
        repeat(14) { sim.advance(2.0) }
        val snap = sim.snapshot()
        assertTrue(snap.milestones.isNotEmpty())
        assertTrue(snap.milestones.first().contains("Cenário iniciado"))
        assertTrue(snap.milestones.any { it.contains("Kick") })
        sim.closeBop(BopElement.ANNULAR)
        sim.advance(2.0)
        val kill = sim.killSheetResult()
        assertNotNull(kill)
        assertTrue(kill!!.schedule.isNotEmpty())
        assertTrue(kill.schedule.first().stroke >= 0.0)
    }

    // --- v2.1: perfuração realista, hidráulica, SCR, COP e procedimentos ---

    @Test
    fun `perfuracao realista avanca profundidade com ROP nao nulo e oscilacao coerente`() {
        val scenario = Scenarios.byId("sandbox")
        val sim = Simulator(scenario)
        sim.setPump(true, 70.0)
        sim.setWobSetpointKlb(25.0)
        sim.setRpmSetpoint(110.0)
        sim.setRotary(true)
        sim.setCopOffset(0.0)

        // Rampa de rotação e estabilização de WOB
        repeat(15) { sim.advance(0.5) }
        val snapInitial = sim.snapshot()
        val startDepth = snapInitial.bitDepthM

        assertTrue("ROP deve ser não nulo quando perfurando", snapInitial.ropMHr > 5.0)
        assertTrue("WOB deve estar próximo do setpoint", snapInitial.wobKlb > 15.0)
        assertTrue("Torque deve ser positivo", snapInitial.torqueKNm > 5.0)

        repeat(60) { sim.advance(1.0) }
        val snapFinal = sim.snapshot()
        assertTrue(
            "Profundidade da broca deve avançar com o tempo (inicial $startDepth, final ${snapFinal.bitDepthM})",
            snapFinal.bitDepthM > startDepth + 0.1
        )
        assertTrue("Carga de cascalhos deve aumentar durante a perfuração", snapFinal.cuttingsLoad > 0.0)
    }

    @Test
    fun `perfuracao cessa quando bomba desliga, rotacao desliga ou COP e levantada`() {
        val scenario = Scenarios.byId("sandbox")
        val sim = Simulator(scenario)
        sim.setPump(true, 60.0)
        sim.setWobSetpointKlb(25.0)
        sim.setRpmSetpoint(100.0)
        sim.setRotary(true)
        repeat(15) { sim.advance(0.5) }
        assertTrue("Deve estar perfurando", sim.snapshot().ropMHr > 0.0)

        // 1. Desliga rotação
        sim.setRotary(false)
        repeat(10) { sim.advance(0.5) }
        assertEquals(0.0, sim.snapshot().ropMHr, 0.001)

        // Retoma rotação
        sim.setRotary(true)
        repeat(10) { sim.advance(0.5) }
        assertTrue(sim.snapshot().ropMHr > 0.0)

        // 2. Levanta COP (offset 1.5 m)
        sim.setCopOffset(1.5)
        repeat(10) { sim.advance(0.5) }
        assertEquals(0.0, sim.snapshot().ropMHr, 0.001)
        assertEquals(0.0, sim.snapshot().wobKlb, 0.1)

        // Baixa COP novamente
        sim.setCopOffset(0.0)
        repeat(10) { sim.advance(0.5) }
        assertTrue(sim.snapshot().ropMHr > 0.0)

        // 3. Desliga bomba
        sim.setPump(false, 0.0)
        repeat(10) { sim.advance(0.5) }
        assertEquals(0.0, sim.snapshot().ropMHr, 0.001)
    }

    @Test
    fun `WOB e RPM escalam ROP e torque de forma coerente`() {
        val scenario = Scenarios.byId("sandbox")
        val sim = Simulator(scenario)
        sim.setPump(true, 70.0)
        sim.setRotary(true)
        sim.setCopOffset(0.0)

        // WOB baixo
        sim.setWobSetpointKlb(10.0)
        sim.setRpmSetpoint(80.0)
        repeat(15) { sim.advance(0.5) }
        val ropLow = sim.snapshot().ropMHr
        val torqueLow = sim.snapshot().torqueKNm

        // WOB alto
        sim.setWobSetpointKlb(30.0)
        repeat(15) { sim.advance(0.5) }
        val ropHigh = sim.snapshot().ropMHr
        val torqueHigh = sim.snapshot().torqueKNm

        assertTrue("ROP com WOB 30 Klb deve ser maior que com 10 Klb ($ropHigh vs $ropLow)", ropHigh > ropLow)
        assertTrue("Torque com WOB 30 Klb deve ser maior que com 10 Klb ($torqueHigh vs $torqueLow)", torqueHigh > torqueLow)
    }

    @Test
    fun `hidraulica calcula perdas proporcionais e tomada de SCR calibra modelo`() {
        val scenario = Scenarios.byId("fundamentos")
        val sim = Simulator(scenario)
        sim.setPump(true, 40.0)
        repeat(10) { sim.advance(0.5) }
        val sppLow = sim.snapshot().sppKPa

        sim.setPump(true, 75.0)
        repeat(10) { sim.advance(0.5) }
        val snapHigh = sim.snapshot()
        val sppHigh = snapHigh.sppKPa

        assertTrue("SPP a 75 SPM deve ser maior que a 40 SPM ($sppHigh vs $sppLow)", sppHigh > sppLow)
        assertTrue("Perdas por trecho devem ser positivas", snapHigh.losses.surfaceKPa > 0.0)
        assertTrue("Perdas na coluna devem ser positivas", snapHigh.losses.stringKPa > 0.0)
        assertTrue("Perdas na broca devem ser positivas", snapHigh.losses.bitKPa > 0.0)
        assertTrue("Perdas no anular devem ser positivas", snapHigh.losses.annulusKPa > 0.0)

        // Tomada de SCR
        val taken = sim.takeScr()
        assertTrue("Tomada de SCR deve ser bem-sucedida com bomba estável", taken)
        val snapScr = sim.snapshot()
        assertTrue(snapScr.scrTaken)
        assertEquals(ScrSource.MEASURED, snapScr.scrSource)
        assertEquals(snapScr.sppKPa, snapScr.scrKPa, snapScr.sppKPa * 0.05)
    }

    @Test
    fun `ECD excede densidade estatica da lama durante circulacao`() {
        val scenario = Scenarios.byId("fundamentos")
        val sim = Simulator(scenario)
        sim.setPump(true, 65.0)
        repeat(10) { sim.advance(0.5) }
        val snap = sim.snapshot()
        assertTrue(
            "ECD (${snap.ecdKgM3}) deve ser estritamente maior que densidade estática (${snap.mudDensityKgM3})",
            snap.ecdKgM3 > snap.mudDensityKgM3
        )
    }

    @Test
    fun `procedimento de tomada de SCR guia operador e avanca automaticamente`() {
        val scenario = Scenarios.byId("fundamentos")
        val sim = Simulator(scenario)
        sim.startProcedure(ProcedureId.SCR)
        val p1 = sim.snapshot().procedure
        assertNotNull(p1)
        assertEquals(ProcedureId.SCR, p1!!.id)
        assertEquals(0, p1.currentIndex)

        // Não avança se a bomba não estiver na taxa de kill
        sim.advanceProcedure()
        assertEquals(0, sim.snapshot().procedure!!.currentIndex)

        // Ajusta bomba para taxa de kill; o procedimento deve avançar sozinho.
        val killSpm = scenario.killRateLpm / scenario.rig.pumps.first().displacementLPerStroke
        sim.setPump(true, killSpm)
        repeat(40) { sim.advance(1.0) }

        val snap = sim.snapshot()
        assertNull("Procedimento deve concluir automaticamente com a bomba estável", snap.procedure)
        assertTrue("SCR deve ser registrada automaticamente", snap.scrTaken)
        assertEquals(ScrSource.MEASURED, snap.scrSource)
    }

    @Test
    fun `varredura de terminologia garante ausencia de sapato e perfurador`() {
        Scenarios.all.forEach { scenario ->
            val allTexts = listOf(
                scenario.title, scenario.subtitle, scenario.description, scenario.briefing,
                scenario.titleEn, scenario.subtitleEn, scenario.descriptionEn, scenario.briefingEn
            ) + scenario.objectives + scenario.objectivesEn

            allTexts.forEach { text ->
                assertTrue(
                    "Cenário ${scenario.id} contém termo proibido 'sapato': $text",
                    !text.lowercase().contains("sapato")
                )
                assertTrue(
                    "Cenário ${scenario.id} contém termo proibido 'perfurador': $text",
                    !text.lowercase().contains("perfurador")
                )
            }
        }
    }

    @Test
    fun `modelo termico aquece fundo e reduz hidrostatica`() {
        val well = Scenarios.byId("fundamentos").well
        val surface = ThermalModel.tempAtTvd(well, 0.0)
        val bottom = ThermalModel.tempAtTvd(well, well.tvdM)
        assertTrue("Fundo ($bottom °C) deve ser mais quente que a superfície ($surface °C)", bottom > surface)
        val rhoHot = ThermalModel.densityAtTemp(1200.0, 90.0)
        assertTrue("Densidade deve cair com a temperatura", rhoHot < 1200.0)
        val integrated = ThermalModel.integratedHydrostaticKPa(well, 1200.0, well.tvdM)
        val isothermal = Units.hydrostaticKpa(1200.0, well.tvdM)
        assertTrue("Hidrostática integrada ($integrated) deve ser menor que a isotérmica ($isothermal)", integrated < isothermal)
        assertTrue(ThermalModel.temperatureFactor(well, well.tvdM) < 1.0)
    }

    @Test
    fun `poco personalizado aplica geometria linhas e normaliza zona de kick`() {
        val spec = CustomWellSpec(
            id = "custom:geometria",
            name = "Geometria custom",
            tvdM = 3000.0,
            mdM = 3200.0,
            shoeTvdM = 2000.0,
            triggerDepthMdM = 2900.0,
            kickTriggerMode = KickTriggerMode.DEPTH,
            zoneTopMdM = 0.0,
            zoneBaseMdM = 0.0,
            openHoleDiameterIn = 8.5,
            tempBottomC = 102.0,
            customCasings = listOf(
                CasingSpec("Surface", 0.0, 1200.0, 12.615, 13.375, 17.5),
                CasingSpec("Production", 1200.0, 3000.0, 8.681, 9.625, 12.25)
            )
        )
        val scenario = Scenarios.custom(spec)
        assertEquals(2900.0, scenario.kick.triggerDepthMdM, 0.1)
        assertEquals(2900.0, scenario.kick.zoneTopMdM, 0.1)
        assertTrue(scenario.kick.zoneBaseMdM > scenario.kick.zoneTopMdM)
        assertEquals(2, scenario.well.casings.size)
        assertEquals(102.0, scenario.well.tempBottomC, 0.01)
        assertTrue(scenario.well.lines.chokeLineLengthM >= 100.0)
    }

    @Test
    fun `poco personalizado direcional inicia acima do gatilho e so kicka ao cruzar`() {
        val spec = CustomWellSpec(
            id = "custom:direcional",
            name = "Direcional HP",
            tvdM = 3200.0,
            mdM = 3500.0,
            shoeTvdM = 2200.0,
            inclinationDeg = 34.0,
            poreGradientKPaPerM = 11.8,
            fracGradientKPaPerM = 18.2,
            mudPpg = 10.0,
            kickEnabled = true,
            kickVolumeM3 = 3.2,
            kickTriggerMode = KickTriggerMode.DEPTH,
            triggerAtSec = 24.0,
            zoneTopMdM = 3350.0,
            zoneBaseMdM = 3460.0,
            triggerDepthMdM = 3350.0
        )
        val scenario = Scenarios.custom(spec)
        assertTrue(
            "Broca deve começar acima do gatilho",
            scenario.initialBitDepthM < scenario.kick.triggerDepthMdM
        )
        val sim = Simulator(scenario)
        assertTrue("Não pode iniciar com kick", !sim.snapshot().kickTriggered)
        sim.advance(1.0)
        assertTrue("Kick não pode disparar antes do gatilho", !sim.snapshot().kickTriggered)

        sim.setDrilling(70.0, 80.0)
        var guard = 0
        while (!sim.snapshot().kickTriggered && guard < 1500) {
            sim.advance(2.0)
            guard++
        }
        val snap = sim.snapshot()
        assertTrue("Kick deve disparar ao cruzar o gatilho", snap.kickTriggered)
        assertTrue(snap.bitDepthM >= scenario.kick.triggerDepthMdM)
        assertTrue(snap.influxM3 > 0.0)
    }

    @Test
    fun `gatilho por profundidade nao dispara quando broca ja esta abaixo`() {
        val base = Scenarios.custom(
            CustomWellSpec(
                id = "custom:ja-abaixo",
                name = "Já abaixo",
                tvdM = 3000.0,
                mdM = 3000.0,
                shoeTvdM = 2000.0,
                kickTriggerMode = KickTriggerMode.DEPTH,
                zoneTopMdM = 2500.0,
                zoneBaseMdM = 2600.0,
                triggerDepthMdM = 2500.0
            )
        )
        val scenario = base.copy(initialBitDepthM = 2700.0)
        val sim = Simulator(scenario)
        repeat(5) { sim.advance(1.0) }
        assertTrue(
            "Comparação absoluta não pode disparar kick fora do cruzamento",
            !sim.snapshot().kickTriggered
        )
    }

    @Test
    fun `sugestao ativa inicia acima do gatilho e valor editado e respeitado`() {
        val base = CustomWellSpec(
            id = "custom:sugestao",
            name = "Sugestão",
            tvdM = 3000.0,
            mdM = 3200.0,
            shoeTvdM = 2000.0,
            kickTriggerMode = KickTriggerMode.DEPTH,
            zoneTopMdM = 2800.0,
            zoneBaseMdM = 2900.0,
            triggerDepthMdM = 2800.0
        )
        val suggested = Scenarios.custom(base)
        assertEquals(2795.0, suggested.initialBitDepthM, 0.01)

        val fromSurface = Scenarios.custom(base.copy(initialBitDepthMdM = 0.0))
        assertEquals(0.0, fromSurface.initialBitDepthM, 0.01)

        val lowIntensity = Scenarios.custom(base.copy(kickIntensityKPa = 0.0))
        assertEquals(0.0, lowIntensity.kick.intensityKPa, 0.01)
        val defaultIntensity = Scenarios.custom(base)
        assertEquals(CustomWellDefaults.DEFAULT_KICK_INTENSITY_KPA, defaultIntensity.kick.intensityKPa, 0.01)
    }

    @Test
    fun `intensidade da zona altera a pressao de poros e o sidpp`() {
        fun scenario(intensity: Double): Scenario {
            val spec = CustomWellSpec(
                id = "custom:intensidade-$intensity",
                name = "Intensidade",
                tvdM = 3000.0,
                mdM = 3000.0,
                shoeTvdM = 2000.0,
                poreGradientKPaPerM = 11.5,
                kickEnabled = true,
                kickTriggerMode = KickTriggerMode.MANUAL,
                zoneTopMdM = 2800.0,
                zoneBaseMdM = 3000.0,
                triggerDepthMdM = 2800.0,
                kickDepthMdM = 3000.0,
                kickIntensityKPa = intensity,
                initialBitDepthMdM = 2900.0
            )
            return Scenarios.custom(spec)
        }
        val without = Simulator(scenario(0.0)).sidppKPa()
        val withExtra = Simulator(scenario(1200.0)).sidppKPa()
        assertEquals(1200.0, withExtra - without, 5.0)
    }

    @Test
    fun `sapata de revestimento direcional usa tvd da trajetoria`() {
        val spec = CustomWellSpec(
            id = "custom:sapata-direcional",
            name = "Sapata direcional",
            tvdM = 3200.0,
            mdM = 3500.0,
            shoeTvdM = 2200.0,
            inclinationDeg = 34.0,
            customCasings = listOf(
                CasingSpec("Surface", 0.0, 1500.0, 12.615, 13.375, 17.5),
                CasingSpec("Production", 1500.0, 3400.0, 8.681, 9.625, 12.25)
            )
        )
        val scenario = Scenarios.custom(spec)
        val production = scenario.well.casings.first { it.name == "Production" }
        val expected = scenario.well.tvdAtMd(3400.0)
        assertEquals(expected, production.shoeTvdM, 0.01)
        assertTrue("TVD da sapata deve ser menor que o TMD", production.shoeTvdM < 3400.0)
    }

    @Test
    fun `kickDepthMdM controla onde o influxo programado entra`() {
        val spec = CustomWellSpec(
            id = "custom:kick-depth",
            name = "Profundidade do influxo",
            tvdM = 3000.0,
            mdM = 3200.0,
            shoeTvdM = 2000.0,
            inclinationDeg = 25.0,
            kickEnabled = true,
            kickTriggerMode = KickTriggerMode.TIME,
            triggerAtSec = 4.0,
            zoneTopMdM = 2900.0,
            zoneBaseMdM = 3150.0,
            triggerDepthMdM = 2900.0,
            kickDepthMdM = 3100.0,
            initialBitDepthMdM = 2850.0
        )
        val scenario = Scenarios.custom(spec)
        assertEquals(3100.0, scenario.kick.depthM, 0.01)
        val sim = Simulator(scenario)
        repeat(3) { sim.advance(1.5) }
        val snap = sim.snapshot()
        assertTrue(snap.kickTriggered)
        val expectedTvd = scenario.well.tvdAtMd(3100.0)
        assertEquals(expectedTvd, snap.influxBaseTvdM, 2.0)
    }

    @Test
    fun `poco equilibrado na zona portadora nao sofre influxo fisico`() {
        val spec = CustomWellSpec(
            id = "custom:equilibrado",
            name = "Equilibrado",
            tvdM = 3000.0,
            mdM = 3000.0,
            shoeTvdM = 2000.0,
            poreGradientKPaPerM = 10.5,
            fracGradientKPaPerM = 18.0,
            mudPpg = 11.2,
            kickEnabled = true,
            kickTriggerMode = KickTriggerMode.DEPTH,
            zoneTopMdM = 2800.0,
            zoneBaseMdM = 3000.0,
            triggerDepthMdM = 2990.0,
            kickIntensityKPa = 0.0,
            initialBitDepthMdM = 2850.0
        )
        val sim = Simulator(Scenarios.custom(spec))
        repeat(10) { sim.advance(1.0) }
        val snap = sim.snapshot()
        assertTrue("BHP > P_poros não deve gerar influxo", snap.influxM3 == 0.0)
        assertTrue(!snap.kickDetected)
    }


    // --- v2.4: segunda circulação, lama de matar e deslocamento de linhas ---

    private fun fastKillScenario(subsea: Boolean = false): Scenario = Scenarios.custom(
        CustomWellSpec(
            id = if (subsea) "custom:kill-seq-subsea" else "custom:kill-seq",
            name = if (subsea) "Sequência de kill subsea" else "Sequência de kill",
            surfaceType = if (subsea) SurfaceType.SUBSEA else SurfaceType.LAND,
            waterDepthM = if (subsea) 200.0 else 0.0,
            tvdM = if (subsea) 900.0 else 600.0,
            mdM = if (subsea) 900.0 else 600.0,
            shoeTvdM = if (subsea) 500.0 else 350.0,
            poreGradientKPaPerM = 12.0,
            fracGradientKPaPerM = 22.0,
            mudPpg = 9.6,
            kickEnabled = true,
            kickTriggerMode = KickTriggerMode.IMMEDIATE,
            kickVolumeM3 = 0.3,
            zoneTopMdM = if (subsea) 900.0 else 600.0,
            zoneBaseMdM = if (subsea) 900.0 else 600.0,
            initialBitDepthMdM = if (subsea) 900.0 else 600.0,
            initialSpm = 0.0,
            killLineLengthM = 100.0,
            chokeLineLengthM = 100.0,
            boosterLineLengthM = 100.0
        )
    )

    @Test
    fun `pesar lama nao derruba sidpp enquanto a lama velha esta no poco`() {
        val sim = Simulator(fastKillScenario())
        sim.closeBop(BopElement.ANNULAR)
        sim.setHcr(true)
        repeat(10) { sim.advance(1.0) }
        val sidppBefore = sim.sidppKPa()
        val oldDensity = sim.oldMudKgM3
        sim.weightUpToKillMud()
        repeat(120) { sim.advance(1.0) }
        assertTrue("A lama do pit deve pesar", sim.mudDensityKgM3 > oldDensity + 10.0)
        assertEquals("SIDPP não pode cair só por pesar a lama", sidppBefore, sim.sidppKPa(), 5.0)
    }

    @Test
    fun `segunda circulacao mata o poco e bloqueia abertura ate deslocar linhas`() {
        val sim = Simulator(fastKillScenario())
        sim.closeBop(BopElement.ANNULAR)
        sim.setHcr(true)
        sim.setChoke(15.0)
        sim.setChokeAuto(true, sim.sicpKPa())
        sim.setPump(true, 60.0)

        var guard = 0
        while (sim.snapshot().firstCirculationFraction < 1.0 && guard < 6000) {
            sim.advance(1.0)
            guard++
        }
        val b6 = sim.snapshot()
        assertTrue("A 1ª circulação deve atingir 100%", b6.firstCirculationFraction >= 1.0)
        assertTrue(
            "O volume bombeado deve cobrir o total",
            b6.firstCirculationPumpedM3 >= b6.firstCirculationTotalM3 - 0.01
        )
        assertTrue("Os strokes totais devem ser positivos", b6.firstCirculationTotalStrokes > 0.0)
        assertEquals("Não devem restar strokes", 0.0, b6.firstCirculationRemainingStrokes, 1.0)
        assertTrue("Não deve haver influxo ao fim da 1ª circulação", b6.firstCirculationKickOut)

        // A etapa conclui sozinha 2 s após atingir 100%.
        guard = 0
        while (sim.phase != Phase.KICK_CONTROLLED && guard < 60) {
            sim.advance(1.0)
            guard++
        }
        assertEquals("A 1ª circulação deve concluir sozinha", Phase.KICK_CONTROLLED, sim.phase)
        val frozen = sim.killSheetFrozen
        assertNotNull(frozen)
        assertTrue("A 2ª circulação ainda não começou", sim.killStage == KillStage.KICK_CONTROLLED)

        sim.setPump(false, 0.0)
        sim.weightUpToKillMud()
        val kmw = sim.killSheetFrozen?.killMudKgM3 ?: 0.0
        guard = 0
        while (sim.mudDensityKgM3 < kmw - 2.0 && guard < 600) {
            sim.advance(1.0)
            guard++
        }
        assertTrue("A lama deve chegar à KMW", sim.mudDensityKgM3 >= kmw - 2.0)
        assertTrue("Deve iniciar a 2ª circulação", sim.startSecondCirculation())
        assertEquals(Phase.DISPLACING_KILL_MUD, sim.phase)

        sim.setChokeAuto(true, sim.killCasingTargetKPa())
        sim.setPump(true, 60.0)
        guard = 0
        while (sim.phase != Phase.KILLED && guard < 9000) {
            sim.advance(2.0)
            guard++
        }
        assertEquals("A 2ª circulação deve matar o poço", Phase.KILLED, sim.phase)
        assertEquals(KillStage.LINE_DISPLACEMENT, sim.killStage)
        assertTrue("Poço morto exige linha deslocada", !sim.snapshot().lineDisplacementComplete)

        sim.setPump(false, 0.0)
        sim.openBop()
        assertTrue("BOP não pode abrir antes de deslocar as linhas", sim.snapshot().closedBop)

        val required = sim.requiredLineKinds()
        required.forEach { kind ->
            var lineGuard = 0
            while (kind !in sim.snapshot().linesDisplaced && lineGuard < 10) {
                sim.displaceLine(kind, 1.0)
                lineGuard++
            }
        }
        assertTrue("Todas as linhas devem estar deslocadas", sim.snapshot().lineDisplacementComplete)

        repeat(10) { sim.bleedResidualPressure() }
        val ready = sim.snapshot()
        assertTrue("Pressão residual deve ir a zero", ready.residualPressureKPa <= 200.0)
        assertTrue("Poço deve estar liberado para abrir", ready.readyToOpen)
        sim.openBop()
        assertTrue("BOP deve abrir após poço morto e linhas deslocadas", !sim.snapshot().closedBop)
    }

    @Test
    fun `fase A registra leituras de 50 100 150 e 200 gpm`() {
        val sim = Simulator(Scenarios.byId("fundamentos"))
        for (gpm in listOf(50, 100, 150, 200)) {
            sim.setRateGpm(gpm.toDouble())
            repeat(20) { sim.advance(1.0) }
            assertTrue(
                "Leitura de $gpm gpm deve ser aceita com a vazão estabilizada",
                sim.takeScrAt(gpm)
            )
        }
        val snap = sim.snapshot()
        assertEquals(setOf(50, 100, 150, 200), snap.scrAtRateKPa.keys)
        snap.scrAtRateKPa.forEach { (gpm, kPa) ->
            assertTrue("Leitura de $gpm gpm deve ser positiva", kPa > 0.0)
        }
        val scrChecks = snap.prepChecks.filter {
            it.id == PrepCheckId.SCR_50 || it.id == PrepCheckId.SCR_100 ||
                it.id == PrepCheckId.SCR_150 || it.id == PrepCheckId.SCR_200
        }
        assertTrue("As quatro leituras devem constar no preparo", scrChecks.size == 4)
        assertTrue("As quatro leituras devem estar concluídas", scrChecks.all { it.done })
    }

    @Test
    fun `leitura de baixa vazao e recusada fora do alvo`() {
        val sim = Simulator(Scenarios.byId("fundamentos"))
        sim.setRateGpm(200.0)
        repeat(20) { sim.advance(1.0) }
        assertTrue("50 gpm deve ser recusado a 200 gpm", !sim.takeScrAt(50))
        assertTrue("Vazões fora da lista devem ser recusadas", !sim.takeScrAt(75))
    }

    @Test
    fun `delta p da linha de controle exige poco fechado e hcr aberto`() {
        val sim = Simulator(Scenarios.byId("fundamentos"))
        sim.setRateGpm(50.0)
        repeat(20) { sim.advance(1.0) }
        assertTrue("Delta P C.L. exige poço fechado", !sim.takeChokeLineDeltaP())

        sim.closeBop(BopElement.ANNULAR)
        assertTrue("Delta P C.L. exige HCR aberto", !sim.takeChokeLineDeltaP())

        sim.closeBop(BopElement.HCR)
        repeat(5) { sim.advance(1.0) }
        assertTrue("Delta P C.L. deve ser registrado", sim.takeChokeLineDeltaP())

        val snap = sim.snapshot()
        assertTrue("Delta P C.L. deve ser positivo", snap.chokeLineDeltaPKPa > 0.0)
        val check = snap.prepChecks.first { it.id == PrepCheckId.DELTA_P_CHOKE_LINE }
        assertTrue("Delta P C.L. deve constar no preparo", check.done)
    }

    @Test
    fun `atrito de linha cresce com a vazao`() {
        val low = HydraulicModel.lineFrictionKPa(3.0, 100.0, 200.0, 1150.0)
        val high = HydraulicModel.lineFrictionKPa(3.0, 100.0, 400.0, 1150.0)
        assertTrue("Atrito deve ser positivo", low > 0.0)
        assertTrue("Atrito deve crescer com a vazão", high > low)
        assertEquals(0.0, HydraulicModel.lineFrictionKPa(3.0, 100.0, 0.0, 1150.0), 1e-9)
    }

    @Test
    fun `alarmes de ganho e perda sao calibrados em barris`() {
        val sim = Simulator(Scenarios.byId("fundamentos"))
        sim.setGainAlarmBbl(5.0)
        sim.setLossAlarmBbl(10.0)
        val calibrado = sim.snapshot()
        assertEquals(5.0, Units.m3ToBbl(calibrado.pitGainAlarmM3), 0.01)
        assertEquals(10.0, Units.m3ToBbl(calibrado.pitLossAlarmM3), 0.01)
        assertTrue(calibrado.prepChecks.first { it.id == PrepCheckId.GAIN_ALARM }.done)
        assertTrue(calibrado.prepChecks.first { it.id == PrepCheckId.LOSS_ALARM }.done)
    }

    @Test
    fun `ganho acima do limite calibrado gera alarme de pit`() {
        val sim = Simulator(Scenarios.byId("drillers-surface"))
        sim.setGainAlarmBbl(1.0)
        repeat(40) { sim.advance(1.0) }
        val snap = sim.snapshot()
        assertTrue("O kick deve ter sido detectado", snap.kickDetected)
        assertTrue("O ganho deve superar o limite de 1 bbl", snap.pitGainM3 > Units.bblToM3(1.0))
        assertTrue(
            "Ganho acima do limite deve gerar PIT_GAIN",
            snap.alarms.any { it.code == AlarmCode.PIT_GAIN }
        )
    }

    @Test
    fun `alarme de variacao do retorno e calibrado em percentual`() {
        val sim = Simulator(Scenarios.byId("fundamentos"))
        sim.setReturnFlowAlarmPct(10.0)
        assertEquals(10.0, sim.snapshot().returnFlowAlarmPct, 0.001)
        assertTrue(sim.snapshot().prepChecks.first { it.id == PrepCheckId.RETURN_FLOW_ALARM }.done)
    }

    @Test
    fun `teste funcional do bop conclui apos acionar todos os elementos`() {
        val scenario = Scenarios.byId("fundamentos")
        val sim = Simulator(scenario)
        scenario.rig.bop.elements.distinct().forEach { sim.closeBop(it) }
        val snap = sim.snapshot()
        val check = snap.prepChecks.first { it.id == PrepCheckId.BOP_TEST }
        assertTrue("Todos os elementos, incluindo HCR, devem concluir o teste", check.done)
    }

    @Test
    fun `indicios secundarios ficam quietos nos primeiros segundos`() {
        val sim = Simulator(Scenarios.byId("fundamentos"))
        repeat(10) { sim.advance(1.0) }
        assertTrue(
            "Sem kick e com linha de base recente não deve haver indício",
            sim.snapshot().secondaryIndicators.isEmpty()
        )
    }

    @Test
    fun `variacao de retorno aparece como indicio secundario durante o kick`() {
        val sim = Simulator(Scenarios.byId("drillers-surface"))
        repeat(30) { sim.advance(1.0) }
        val snap = sim.snapshot()
        assertTrue("O kick deve ter sido detectado", snap.kickDetected)
        assertTrue(
            "Retorno fora de ±10% deve ser um indício secundário",
            SecondaryIndicator.RETURN_FLOW_CHANGE in snap.secondaryIndicators
        )
    }

    @Test
    fun `condicao de kick distingue hidrostatica e ecd`() {
        val estatico = Simulator(Scenarios.byId("kill-sheet"))
        estatico.setPump(false, 0.0)
        estatico.advance(1.0)
        assertEquals(
            KickCondition.PORE_GT_HYDROSTATIC,
            estatico.snapshot().kickCondition
        )

        val dinamico = Simulator(Scenarios.byId("kill-sheet"))
        dinamico.advance(0.5)
        assertEquals(KickCondition.PORE_GT_ECD, dinamico.snapshot().kickCondition)
    }

    @Test
    fun `zona interna superior e classificada como zona anormal`() {
        val sim = Simulator(Scenarios.byId("depth-trigger"))
        sim.setDrilling(150.0, 100.0)
        repeat(150) { sim.advance(1.0) }
        val snap = sim.snapshot()
        assertTrue("A broca deve entrar na zona de gatilho", snap.bitDepthM > 2999.5)
        assertEquals(
            KickCondition.INTERNAL_HIGH_ZONE,
            snap.kickCondition
        )
    }

    @Test
    fun `preparo completo agrega os dez itens da fase A`() {
        val sim = Simulator(Scenarios.byId("fundamentos"))
        val snap = sim.snapshot()
        assertEquals(PrepCheckId.values().size, snap.prepChecks.size)
        assertTrue("Preparo recém-iniciado não está completo", !snap.prepComplete)
        assertEquals(0, snap.prepDoneCount)
    }

    // --- v2.7.0: correções A–F ---

    @Test
    fun `kmw usa tvd da zona de kick e soma a margem de seguranca`() {
        val spec = CustomWellSpec(
            id = "custom:kmw-tvd",
            name = "KMW pela TVD da zona",
            tvdM = 1000.0,
            mdM = 1000.0,
            shoeTvdM = 600.0,
            poreGradientKPaPerM = 12.0,
            fracGradientKPaPerM = 20.0,
            mudPpg = 8.5,
            killMarginPpg = 0.2,
            kickEnabled = true,
            kickTriggerMode = KickTriggerMode.IMMEDIATE,
            kickVolumeM3 = 0.4,
            zoneTopMdM = 600.0,
            zoneBaseMdM = 600.0,
            triggerDepthMdM = 600.0,
            kickDepthMdM = 600.0,
            initialBitDepthMdM = 600.0,
            initialSpm = 0.0
        )
        val sim = Simulator(Scenarios.custom(spec))
        sim.closeBop(BopElement.ANNULAR)
        repeat(8) { sim.advance(1.0) }
        val snap = sim.snapshot()
        val sheet = sim.killSheetResult()
        assertNotNull("O kill sheet deve existir após o fechamento", sheet)
        val margin = Units.ppgToKgM3(0.2)
        assertEquals("A margem configurada deve ser aplicada", margin, sheet!!.marginKgM3, 0.5)
        assertTrue(
            "A KMW deve ser maior que a KMW sem margem",
            sheet.killMudKgM3 > sheet.killMudNoMarginKgM3
        )
        val expected = snap.oldMudKgM3 +
            snap.sidppKPa * 1000.0 / (Units.G * 600.0) + margin
        assertEquals("KMW deve usar a TVD da broca/zona (600 m)", expected, sheet.killMudKgM3, 1.0)
        val usingTd = snap.oldMudKgM3 +
            snap.sidppKPa * 1000.0 / (Units.G * 1000.0) + margin
        assertTrue(
            "A KMW não pode usar a TVD do TD (1000 m)",
            kotlin.math.abs(sheet.killMudKgM3 - usingTd) > 20.0
        )
    }

    @Test
    fun `sangria e recusada antes do poco morto e das linhas deslocadas`() {
        val sim = Simulator(fastKillScenario())
        sim.closeBop(BopElement.ANNULAR)
        assertTrue("Sangria não pode ocorrer durante o controle", !sim.bleedResidualPressure())

        sim.setHcr(true)
        sim.setChoke(15.0)
        sim.setChokeAuto(true, sim.sicpKPa())
        sim.setPump(true, 60.0)
        var guard = 0
        while (sim.snapshot().firstCirculationFraction < 1.0 && guard < 6000) {
            sim.advance(1.0)
            guard++
        }
        guard = 0
        while (sim.phase != Phase.KICK_CONTROLLED && guard < 60) {
            sim.advance(1.0)
            guard++
        }
        assertEquals(Phase.KICK_CONTROLLED, sim.phase)
        assertTrue("Sangria não pertence à 1ª circulação", !sim.bleedResidualPressure())
    }

    @Test
    fun `abrir bop apos matar o poco nao reabre influxo`() {
        val sim = Simulator(fastKillScenario())
        sim.closeBop(BopElement.ANNULAR)
        sim.setHcr(true)
        sim.setChoke(15.0)
        sim.setChokeAuto(true, sim.sicpKPa())
        sim.setPump(true, 60.0)
        var guard = 0
        while (sim.snapshot().firstCirculationFraction < 1.0 && guard < 6000) {
            sim.advance(1.0)
            guard++
        }
        guard = 0
        while (sim.phase != Phase.KICK_CONTROLLED && guard < 60) {
            sim.advance(1.0)
            guard++
        }
        sim.setPump(false, 0.0)
        sim.weightUpToKillMud()
        val kmw = sim.killSheetFrozen?.killMudKgM3 ?: 0.0
        guard = 0
        while (sim.mudDensityKgM3 < kmw - 2.0 && guard < 600) {
            sim.advance(1.0)
            guard++
        }
        assertTrue(sim.startSecondCirculation())
        sim.setChokeAuto(true, sim.killCasingTargetKPa())
        sim.setPump(true, 60.0)
        guard = 0
        while (sim.phase != Phase.KILLED && guard < 9000) {
            sim.advance(2.0)
            guard++
        }
        assertEquals(Phase.KILLED, sim.phase)
        sim.setPump(false, 0.0)
        sim.requiredLineKinds().forEach { kind ->
            var lineGuard = 0
            while (kind !in sim.snapshot().linesDisplaced && lineGuard < 10) {
                sim.displaceLine(kind, 1.0)
                lineGuard++
            }
        }
        repeat(10) { sim.bleedResidualPressure() }
        sim.openBop()
        assertTrue("BOP deve abrir com poço morto", !sim.snapshot().closedBop)

        repeat(30) { sim.advance(1.0) }
        val after = sim.snapshot()
        assertTrue("Não pode entrar novo influxo após abrir o BOP", after.influxM3 < 0.02)
        assertTrue("O casing deve permanecer em zero", after.casingPressureKPa < 100.0)
        assertEquals("O poço deve voltar à perfuração", Phase.DRILLING, after.phase)
        assertTrue("O poço deve constar como morto", after.wellDead)
        assertEquals("A lama do poço deve ser a de matar", after.mudDensityKgM3, after.targetMudDensityKgM3, 0.5)
    }

    @Test
    fun `guia b2 permanece concluida ao religar bombas sem perfurar`() {
        val idle = Simulator(fastKillScenario())
        idle.advance(2.0)
        assertTrue(
            "A guia B2 não pode concluir antes de haver perfuração",
            !idle.snapshot().drillingStopped
        )

        val sim = Simulator(Scenarios.byId("sandbox"))
        sim.setPump(true, 60.0)
        sim.setDrilling(60.0, 90.0)
        repeat(30) { sim.advance(1.0) }
        assertTrue("A perfuração deve produzir ROP", sim.ropMHr > 0.5)
        assertTrue("Antes de parar, a guia B2 está pendente", !sim.snapshot().drillingStopped)

        sim.setPump(false, 0.0)
        sim.setRotary(false)
        sim.setWobSetpointKlb(0.0)
        sim.setCopOffset(1.5)
        repeat(5) { sim.advance(1.0) }
        assertTrue("Parar bomba, rotação e WOB conclui a guia B2", sim.snapshot().drillingStopped)

        sim.setPump(true, 60.0)
        sim.setWobSetpointKlb(0.0)
        sim.setRpmSetpoint(0.0)
        repeat(20) { sim.advance(1.0) }
        val after = sim.snapshot()
        assertTrue("Sem rotação/WOB não se perfura", after.ropMHr < 0.5)
        assertTrue("A guia B2 não pode voltar a pendente só por ligar a bomba", after.drillingStopped)
    }

    @Test
    fun `simulacao em 32x permanece estavel`() {
        val scenario = Scenarios.byId("drillers-surface")
        val sim = Simulator(scenario)
        sim.setDrilling(60.0, 90.0)
        repeat(300) { sim.advance(1.056) }
        val snap = sim.snapshot()
        assertTrue(snap.timeSec > 300.0)
        assertTrue(snap.bhpKPa.isFinite())
        assertTrue(snap.sppKPa.isFinite())
        assertTrue(snap.casingPressureKPa.isFinite())
        assertTrue(snap.pitM3 >= 0.0)
        assertTrue(snap.score in 0..100)
    }

    @Test
    fun `simulacao em 64x permanece estavel`() {
        val sim = Simulator(Scenarios.byId("drillers-surface"))
        sim.setDrilling(60.0, 90.0)
        repeat(300) { sim.advance(2.112) }
        val snap = sim.snapshot()
        assertTrue("O passo de 64x deve avançar o tempo de simulação", snap.timeSec > 600.0)
        assertTrue(snap.bhpKPa.isFinite())
        assertTrue(snap.sppKPa.isFinite())
        assertTrue(snap.casingPressureKPa.isFinite())
        assertTrue(snap.pitM3 >= 0.0)
        assertTrue(snap.score in 0..100)
    }

    @Test
    fun `gas em poco terrestre no mgs nao penaliza mas retorno desalinhado penaliza`() {
        fun surfaceGasSim(hcrOpen: Boolean): Simulator {
            val spec = CustomWellSpec(
                id = if (hcrOpen) "custom:test-mgs" else "custom:test-gas-surface",
                name = if (hcrOpen) "Teste MGS" else "Teste gás na superfície",
                tvdM = 500.0,
                mdM = 500.0,
                shoeTvdM = 320.0,
                mudPpg = 13.0,
                kickEnabled = true,
                kickTriggerMode = KickTriggerMode.MANUAL,
                initialSpm = 0.0,
                killLineLengthM = 100.0,
                chokeLineLengthM = 100.0
            )
            val sim = Simulator(Scenarios.custom(spec))
            sim.closeBop(BopElement.ANNULAR)
            sim.setHcr(hcrOpen)
            sim.triggerManualKick(0.4)
            sim.setPump(true, 60.0)
            var guard = 0
            while (!sim.snapshot().gasAtSurface && guard < 6000) {
                sim.advance(1.0)
                guard++
            }
            return sim
        }

        val controlled = surfaceGasSim(hcrOpen = true)
        val cSnap = controlled.snapshot()
        assertTrue("Gás deve chegar ao MGS", cSnap.gasAtSurface)
        assertTrue("Retorno no MGS não pode marcar vazamento", !cSnap.gasReachedSurface)

        val uncontrolled = surfaceGasSim(hcrOpen = false)
        val uSnap = uncontrolled.snapshot()
        assertTrue("Gás deve chegar à superfície", uSnap.gasAtSurface)
        assertTrue("Retorno desalinhado deve marcar vazamento", uSnap.gasReachedSurface)
        assertTrue(
            "Vazamento confirmado deve reduzir o score",
            uSnap.score < cSnap.score
        )
    }

    private fun runToFirstCirculationEnd(sim: Simulator): Boolean {
        sim.closeBop(BopElement.ANNULAR)
        sim.setHcr(true)
        sim.setChoke(15.0)
        sim.setChokeAuto(true, sim.sicpKPa())
        sim.setPump(true, 60.0)
        var guard = 0
        while (sim.phase != Phase.KICK_CONTROLLED && guard < 20000) {
            sim.advance(1.0)
            guard++
        }
        return sim.phase == Phase.KICK_CONTROLLED
    }

    @Test
    fun `fim da primeira circulacao preserva bomba choke e pressao`() {
        for (subsea in listOf(false, true)) {
            val sim = Simulator(fastKillScenario(subsea))
            assertTrue("Deve concluir a 1ª circulação (subsea=$subsea)", runToFirstCirculationEnd(sim))
            val atFinish = sim.snapshot()
            assertTrue("Bomba deve continuar ligada (subsea=$subsea)", atFinish.pumpOn)
            assertTrue(
                "Pressão trapada deve ser registrada (subsea=$subsea)",
                atFinish.trappedPressureKPa > 0.0
            )
            repeat(30) { sim.advance(1.0) }
            val held = sim.snapshot()
            assertTrue("Bomba segue ligada após a conclusão (subsea=$subsea)", held.pumpOn)
            assertTrue(
                "Casing não pode cair para SIDPP/SICP ao concluir (subsea=$subsea)",
                held.casingPressureKPa >= atFinish.trappedPressureKPa * 0.9
            )
            assertTrue(
                "SPP deve continuar acima do SIDPP enquanto circula (subsea=$subsea)",
                held.sppKPa > sim.sidppKPa() + 1.0
            )
        }
    }

    @Test
    fun `migracao difere por tipo de kick com gas mais rapido que agua`() {
        fun migration(type: InfluxType): Double {
            val sim = Simulator(
                Scenarios.custom(
                    CustomWellSpec(
                        id = "custom:mig-$type",
                        name = "Migração",
                        tvdM = 1500.0,
                        mdM = 1500.0,
                        shoeTvdM = 1200.0,
                        kickEnabled = true,
                        kickTriggerMode = KickTriggerMode.TIME,
                        triggerAtSec = 5.0,
                        kickType = type,
                        kickVolumeM3 = 1.0,
                        zoneTopMdM = 1500.0,
                        zoneBaseMdM = 1500.0,
                        initialBitDepthMdM = 1500.0,
                        initialSpm = 55.0
                    )
                )
            )
            repeat(10) { sim.advance(1.0) }
            sim.setPump(false, 0.0)
            sim.closeBop(BopElement.ANNULAR)
            var guard = 0
            while (!sim.snapshot().shutInStabilized && guard < 300) {
                sim.advance(1.0)
                guard++
            }
            repeat(120) { sim.advance(1.0) }
            return sim.snapshot().migrationOverpressureKPa
        }

        val gas = migration(InfluxType.GAS)
        val oil = migration(InfluxType.OIL)
        val water = migration(InfluxType.WATER)
        assertTrue("Gás deve acumular sobrepressão (gas=$gas)", gas > 5.0)
        assertTrue("Gás deve migrar mais que óleo (gas=$gas oil=$oil)", gas > oil)
        assertTrue("Óleo deve migrar mais que água (oil=$oil water=$water)", oil > water)
    }

    @Test
    fun `tubo em U iguala revestimento e sidpp apos expulsar o kick`() {
        // Após a 1ª circulação o influxo sai e a coluna fica homogênea: o poço
        // vira um tubo em U e revestimento e SIDPP devem coincidir com bomba parada.
        val sim = Simulator(fastKillScenario())
        assertTrue("Deve concluir a 1ª circulação", runToFirstCirculationEnd(sim))
        sim.setPump(false, 0.0)
        repeat(180) { sim.advance(1.0) }
        val snap = sim.snapshot()
        assertTrue("Influxo deve ter saído do poço", snap.influxM3 <= 0.08)
        val diff = kotlin.math.abs(snap.casingPressureKPa - sim.sidppKPa())
        assertTrue(
            "Revestimento e SIDPP devem ser iguais em tubo em U (dif=$diff)",
            diff <= kotlin.math.max(30.0, sim.sidppKPa() * 0.03)
        )
        assertTrue(
            "SPP deve refletir o SIDPP com bomba parada",
            kotlin.math.abs(snap.sppKPa - sim.sidppKPa()) <= kotlin.math.max(30.0, sim.sidppKPa() * 0.03)
        )
    }

    @Test
    fun `poco estanque pressuriza e mantem pressao com bomba desligada`() {
        for (subsea in listOf(false, true)) {
            val sim = Simulator(fastKillScenario(subsea))
            sim.closeBop(BopElement.ANNULAR)
            sim.setHcr(false)
            sim.setChokeAuto(false)
            sim.setChoke(0.0)
            sim.setPump(true, 60.0)
            repeat(40) { sim.advance(1.0) }
            val pressurized = sim.snapshot().casingPressureKPa
            assertTrue(
                "A bomba deve pressurizar o poço estanque (subsea=$subsea)",
                pressurized > sim.sicpKPa() + 100.0
            )
            sim.setPump(false, 0.0)
            repeat(60) { sim.advance(1.0) }
            val held = sim.snapshot()
            assertTrue(
                "A pressão deve ficar trapada após desligar a bomba (subsea=$subsea)",
                held.casingPressureKPa >= pressurized * 0.85
            )
        }
    }

    @Test
    fun `alvo de casing cai com a lama de matar no anular`() {
        val sim = Simulator(fastKillScenario())
        assertTrue("Deve concluir a 1ª circulação", runToFirstCirculationEnd(sim))
        val trapped = sim.snapshot().trappedPressureKPa
        assertTrue("Pressão trapada deve existir", trapped > 0.0)

        sim.setPump(false, 0.0)
        sim.weightUpToKillMud()
        val kmw = sim.killSheetFrozen?.killMudKgM3 ?: 0.0
        var guard = 0
        while (sim.mudDensityKgM3 < kmw - 2.0 && guard < 1200) {
            sim.advance(1.0)
            guard++
        }
        assertTrue("Deve iniciar a 2ª circulação", sim.startSecondCirculation())

        val initialTarget = sim.killCasingTargetKPa()
        assertTrue("O alvo inicial deve preservar a pressão trapada", initialTarget > 0.0)
        sim.setChokeAuto(true, initialTarget)
        sim.setPump(true, 60.0)

        var previous = initialTarget
        var sawDecrease = false
        guard = 0
        while (sim.phase != Phase.KILLED && guard < 9000) {
            sim.advance(2.0)
            val target = sim.killCasingTargetKPa()
            assertTrue("O alvo não pode subir acima do inicial", target <= initialTarget + 1.0)
            assertTrue("O alvo não pode ser negativo", target >= 0.0)
            if (target < previous - 1.0) sawDecrease = true
            previous = target
            guard++
        }
        assertTrue("O alvo deve cair com o preenchimento do anular", sawDecrease)
        assertTrue("O alvo final deve ser menor que o inicial", sim.killCasingTargetKPa() < initialTarget)
    }

    @Test
    fun `esquematico separa injecao retorno e manifold ativo`() {
        val sim = Simulator(Scenarios.byId("drillers-surface"))
        sim.setPump(true, 60.0)
        sim.setLineups(ManifoldLineup.DRILLING, ManifoldLineup.DRILLING)
        sim.setHcr(false)
        repeat(2) { sim.advance(0.5) }
        var snap = sim.snapshot()
        assertEquals(InjectionPath.STANDPIPE, snap.injectionPath)
        assertEquals(ReturnPath.FLOWLINE, snap.returnPath)
        assertTrue("Standpipe ativo na perfuração", snap.standpipeActive)
        assertTrue("Kill line não pode parecer ativa na perfuração", !snap.killLineActive)
        assertTrue("Choke manifold não pode parecer ativo na perfuração", !snap.chokeManifoldActive)

        sim.closeBop(BopElement.ANNULAR)
        repeat(2) { sim.advance(0.5) }
        snap = sim.snapshot()
        assertEquals(ReturnPath.BLOCKED, snap.returnPath)
        assertTrue("Sem HCR não há retorno pelo choke", !snap.chokeManifoldActive)

        sim.setHcr(true)
        sim.setLineups(ManifoldLineup.KILL, ManifoldLineup.KILL)
        repeat(2) { sim.advance(0.5) }
        snap = sim.snapshot()
        assertEquals(ReturnPath.CHOKE_LINE, snap.returnPath)
        assertEquals(InjectionPath.KILL_LINE, snap.injectionPath)
        assertTrue("Kill line ativa no alinhamento de kill", snap.killLineActive)
        assertTrue("Choke manifold ativo no retorno pela choke line", snap.chokeManifoldActive)
        assertTrue("Standpipe não pode parecer ativo ao mesmo tempo", !snap.standpipeActive)

        val subsea = Simulator(Scenarios.byId("subsea-drillers"))
        subsea.setPump(true, 60.0)
        repeat(2) { subsea.advance(0.5) }
        assertEquals(ReturnPath.RISER, subsea.snapshot().returnPath)
    }

    // --- Física transiente de shut-in, sangria e reset parcial (2.9.0) -----

    private fun typedKickScenario(type: InfluxType): Scenario = Scenarios.custom(
        CustomWellSpec(
            id = "custom:type-${type.name}",
            name = "Kick ${type.name}",
            surfaceType = SurfaceType.LAND,
            tvdM = 600.0,
            mdM = 600.0,
            shoeTvdM = 350.0,
            poreGradientKPaPerM = 12.0,
            fracGradientKPaPerM = 22.0,
            mudPpg = 9.6,
            kickEnabled = true,
            kickType = type,
            kickTriggerMode = KickTriggerMode.IMMEDIATE,
            kickVolumeM3 = 0.3,
            zoneTopMdM = 600.0,
            zoneBaseMdM = 600.0,
            initialBitDepthMdM = 600.0,
            initialSpm = 0.0
        )
    )

    @Test
    fun `shut-in constroi pressao gradualmente ate estabilizar`() {
        val sim = Simulator(fastKillScenario())
        sim.closeBop(BopElement.ANNULAR)
        val target = sim.sicpKPa()
        val immediately = sim.snapshot()
        assertTrue("O SICP alvo deve ser mensurável", target > 500.0)
        assertTrue(
            "A pressão de casing não pode saltar para o SICP no fechamento",
            immediately.casingPressureKPa < target * 0.5
        )
        assertTrue(
            "O gauge de SIDPP não pode saltar no fechamento",
            immediately.sidppGaugeKPa < immediately.sidppKPa * 0.5
        )

        sim.advance(3.0)
        val early = sim.snapshot()
        assertTrue(
            "A pressão deve crescer durante o build-up",
            early.casingPressureKPa > immediately.casingPressureKPa + 5.0
        )
        assertTrue(
            "O build-up ainda não pode ter terminado em 3 s",
            early.casingPressureKPa < target * 0.95
        )

        repeat(120) { sim.advance(1.0) }
        val settled = sim.snapshot()
        assertEquals(
            "O casing deve estabilizar no SICP",
            target, settled.casingPressureKPa, target * 0.05
        )
        assertTrue("A guia deve reconhecer a estabilização", settled.shutInStabilized)
        assertEquals(
            "O gauge de SIDPP deve acompanhar o valor estabilizado",
            settled.sidppKPa, settled.sidppGaugeKPa, 80.0
        )
    }

    @Test
    fun `sidpp nao depende do tipo e sicp cresce de agua para gas`() {
        fun shutIn(type: InfluxType): Simulator {
            val sim = Simulator(typedKickScenario(type))
            // Deixa o influxo crescer com o BOP aberto para dar corpo à coluna.
            repeat(45) { sim.advance(1.0) }
            sim.setPump(false, 0.0)
            sim.closeBop(BopElement.ANNULAR)
            repeat(120) { sim.advance(1.0) }
            return sim
        }

        val gas = shutIn(InfluxType.GAS)
        val oil = shutIn(InfluxType.OIL)
        val water = shutIn(InfluxType.WATER)

        assertEquals(
            "O SIDPP depende da coluna de lama, não do tipo de influxo",
            water.sidppKPa(), gas.sidppKPa(), 15.0
        )
        assertEquals(
            "O SIDPP depende da coluna de lama, não do tipo de influxo",
            oil.sidppKPa(), gas.sidppKPa(), 15.0
        )
        assertTrue(
            "Gás deve gerar SICP maior que óleo (${gas.sicpKPa()} vs ${oil.sicpKPa()})",
            gas.sicpKPa() > oil.sicpKPa() + 20.0
        )
        assertTrue(
            "Óleo deve gerar SICP maior que água (${oil.sicpKPa()} vs ${water.sicpKPa()})",
            oil.sicpKPa() > water.sicpKPa() + 10.0
        )
    }

    @Test
    fun `sangria apos poco morto nao devolve pressao e guia nao regride`() {
        val sim = Simulator(fastKillScenario())
        sim.closeBop(BopElement.ANNULAR)
        sim.setHcr(true)
        sim.setChoke(15.0)
        sim.setChokeAuto(true, sim.sicpKPa())
        sim.setPump(true, 60.0)

        var guard = 0
        while (sim.snapshot().firstCirculationFraction < 1.0 && guard < 6000) {
            sim.advance(1.0)
            guard++
        }
        guard = 0
        while (sim.phase != Phase.KICK_CONTROLLED && guard < 120) {
            sim.advance(1.0)
            guard++
        }
        assertEquals(Phase.KICK_CONTROLLED, sim.phase)

        sim.setPump(false, 0.0)
        sim.weightUpToKillMud()
        val kmw = sim.killSheetFrozen?.killMudKgM3 ?: 0.0
        guard = 0
        while (sim.mudDensityKgM3 < kmw - 2.0 && guard < 600) {
            sim.advance(1.0)
            guard++
        }
        assertTrue(sim.startSecondCirculation())
        sim.setChokeAuto(true, sim.killCasingTargetKPa())
        sim.setPump(true, 60.0)
        guard = 0
        while (sim.phase != Phase.KILLED && guard < 9000) {
            sim.advance(2.0)
            guard++
        }
        assertEquals("A 2ª circulação deve matar o poço", Phase.KILLED, sim.phase)
        sim.setPump(false, 0.0)

        sim.requiredLineKinds().forEach { kind ->
            var lineGuard = 0
            while (kind !in sim.snapshot().linesDisplaced && lineGuard < 10) {
                sim.displaceLine(kind, 1.0)
                lineGuard++
            }
        }
        val beforeBleed = sim.snapshot()
        assertTrue("Linhas deslocadas", beforeBleed.lineDisplacementComplete)
        repeat(10) { sim.bleedResidualPressure() }
        val afterBleed = sim.snapshot()
        assertTrue("A sangria deve concluir", afterBleed.residualBleedDone)
        assertTrue("A pressão residual deve zerar", afterBleed.casingPressureKPa <= 100.0)

        repeat(40) { sim.advance(1.0) }
        val later = sim.snapshot()
        assertTrue(
            "A pressão não pode voltar depois da sangria (${later.casingPressureKPa} kPa)",
            later.casingPressureKPa <= 100.0
        )
        assertTrue("Poço deve seguir morto", later.wellDead)

        fun latches(s: SimSnapshot): Set<String> = buildSet {
            if (s.guideKickHandled) add("B1")
            if (s.guideDrillingStopped) add("B2")
            if (s.guideBopClosed) add("B3")
            if (s.guideStabilized) add("B4")
            if (s.guideKillSheetReady) add("B5")
            if (s.guideFirstCirculation) add("B6")
            if (s.guideWeightedUp) add("B7")
            if (s.guideSecondCirculation) add("B8")
            if (s.guideLinesDisplaced) add("B9")
            if (s.guideBleedDone) add("B10")
        }

        val beforeOpen = sim.snapshot()
        sim.openBop()
        val opened = sim.snapshot()
        assertTrue("O BOP deve abrir após a sangria", !opened.closedBop)
        assertTrue(
            "Nenhum passo B pode regredir ao abrir o BOP",
            latches(opened).containsAll(latches(beforeOpen))
        )
        repeat(10) { sim.advance(1.0) }
        val stillOpen = sim.snapshot()
        assertTrue(
            "Os latches devem sobreviver ao tempo com BOP aberto",
            latches(stillOpen).containsAll(latches(beforeOpen))
        )
        assertTrue("Ciclo deve ficar concluído", stillOpen.guideCycleComplete)
    }

    @Test
    fun `reset parcial preserva score e profundidade e reinicia checklist`() {
        val sim = Simulator(fastKillScenario())
        sim.closeBop(BopElement.ANNULAR)
        repeat(30) { sim.advance(1.0) }
        val before = sim.snapshot()
        assertTrue("O checklist deve ter avançado", before.guideKickHandled && before.guideBopClosed)
        val depthBefore = before.bitDepthM
        val scoreBefore = before.score
        val mudBefore = before.mudDensityKgM3
        val maxCasingBefore = before.maxCasingKPa

        assertTrue("O reset parcial deve ser aceito", sim.resetGuideAndContinue())
        val after = sim.snapshot()

        assertEquals("A profundidade deve ser preservada", depthBefore, after.bitDepthM, 0.001)
        assertEquals("O score deve ser preservado", scoreBefore, after.score)
        assertEquals("A lama deve ser preservada", mudBefore, after.mudDensityKgM3, 0.001)
        assertEquals(
            "Os picos de pressão devem ser preservados",
            maxCasingBefore, after.maxCasingKPa, 0.001
        )
        assertEquals("A fase volta à perfuração", Phase.DRILLING, after.phase)
        assertTrue("O BOP deve estar aberto", !after.closedBop)
        assertTrue(
            "O checklist B deve reiniciar",
            !after.guideKickHandled && !after.guideBopClosed &&
                !after.guideStabilized && !after.guideKillSheetReady
        )
        assertEquals(
            "O próximo kick deve ser manual",
            KickTriggerMode.MANUAL, after.kickTriggerMode
        )
        assertNull("O kill sheet deve ser descartado", sim.killSheetResult())
    }

    // --- Auditoria 2.9.2: latches B, manobras, choke e SIDPP/SICP fixos ----

    @Test
    fun `itens B nao concluem antes do fato nem com teste do BOP na fase A`() {
        val sim = Simulator(Scenarios.byId("drillers-surface"))
        val t0 = sim.snapshot()
        assertTrue(
            "Nenhum item B pode nascer concluído em t=0",
            !t0.guideKickHandled && !t0.guideDrillingStopped && !t0.guideBopClosed &&
                !t0.guideStabilized && !t0.guideKillSheetReady && !t0.guideFirstCirculation &&
                !t0.guideWeightedUp && !t0.guideSecondCirculation && !t0.guideLinesDisplaced &&
                !t0.guideBleedDone
        )

        // Teste funcional de BOP na Fase A (antes de qualquer kick)
        val prepSim = Simulator(Scenarios.byId("fundamentos"))
        Scenarios.byId("fundamentos").rig.bop.elements.distinct().forEach { prepSim.closeBop(it) }
        prepSim.openBop()
        val afterPrep = prepSim.snapshot()
        assertTrue("Teste do BOP conclui A10", afterPrep.prepChecks.first { it.id == PrepCheckId.BOP_TEST }.done)
        assertTrue(
            "Abrir BOP na Fase A não pode concluir B3, B9 ou B10",
            !afterPrep.guideBopClosed && !afterPrep.guideLinesDisplaced && !afterPrep.guideBleedDone
        )

        // Na 2ª circulação, B8 só conclui ao atingir WELL_KILLED / Phase.KILLED
        val killSim = Simulator(fastKillScenario())
        assertTrue(runToFirstCirculationEnd(killSim))
        killSim.setPump(false, 0.0)
        killSim.weightUpToKillMud()
        val kmw = killSim.killSheetFrozen?.killMudKgM3 ?: 0.0
        var guard = 0
        while (killSim.mudDensityKgM3 < kmw - 2.0 && guard < 600) {
            killSim.advance(1.0)
            guard++
        }
        assertTrue(killSim.startSecondCirculation())
        assertTrue("B8 não pode concluir no início da 2ª circulação", !killSim.snapshot().guideSecondCirculation)
    }

    @Test
    fun `procedimento de flow check conclui item A1 igual ao botao direto`() {
        val sim = Simulator(Scenarios.byId("fundamentos"))
        sim.startProcedure(ProcedureId.FLOW_CHECK)
        sim.setPump(false, 0.0)
        repeat(15) { sim.advance(1.0) }
        repeat(4) { sim.advanceProcedure() }
        val snap = sim.snapshot()
        assertNull("Procedimento deve concluir", snap.procedure)
        assertTrue(
            "Procedimento FLOW_CHECK deve concluir A1",
            snap.prepChecks.any { it.id == PrepCheckId.FLOW_CHECK && it.done }
        )
    }

    @Test
    fun `manobras respeitam BOP fechado bomba rotacao e trip tank`() {
        val sim = Simulator(Scenarios.byId("swab-trip"))
        val startDepth = sim.bitDepthM
        val startPit = sim.pitM3
        val startTripTank = sim.tripTankM3

        sim.closeBop(BopElement.ANNULAR)
        sim.tripPipe(-27.0)
        assertEquals("Não pode manobrar com BOP fechado", startDepth, sim.bitDepthM, 0.001)
        sim.openBop()

        sim.setPump(true, 40.0)
        sim.tripPipe(-27.0)
        assertEquals("Não pode manobrar com bomba ligada", startDepth, sim.bitDepthM, 0.001)
        sim.setPump(false, 0.0)

        sim.setRotary(true)
        sim.tripPipe(-27.0)
        assertEquals("Não pode manobrar com rotação ligada", startDepth, sim.bitDepthM, 0.001)
        sim.setRotary(false)

        val fullBhp = sim.snapshot().bhpHydrostaticKPa
        sim.tripPipe(-27.0)
        val pulled = sim.snapshot()
        assertTrue("Deve retirar 27 m com poço livre", pulled.bitDepthM < startDepth - 26.0)
        assertTrue("Déficit reduz coluna hidrostática", pulled.bhpHydrostaticKPa < fullBhp)

        sim.fillHole(0.05)
        val filled = sim.snapshot()
        assertTrue("Preencher poço deve consumir do Trip Tank", filled.tripTankM3 < startTripTank)
        assertEquals("Preencher poço não deve debitar direto do pit", startPit, filled.pitM3, 0.001)
        assertTrue("Preencher reduz déficit", filled.holeFillM3 < pulled.holeFillM3)

        // Alinhamento TRIP aberto com ambos manifolds em TRIP
        val tripSim = Simulator(Scenarios.byId("sandbox"))
        tripSim.setLineups(ManifoldLineup.TRIP, ManifoldLineup.TRIP)
        assertTrue("TRIP/TRIP com BOP aberto é alinhamento válido de manobra", tripSim.snapshot().lineupValid)
        assertEquals(ReturnPath.TRIP_TANK, tripSim.snapshot().returnPath)
    }

    @Test
    fun `choke automatico fecha sem bombeio e choke aberto manual despressuriza`() {
        val sim = Simulator(fastKillScenario())
        assertTrue(runToFirstCirculationEnd(sim))
        val trapped = sim.snapshot().trappedPressureKPa
        assertTrue("Pressão trapada positiva", trapped > 200.0)

        // Ao parar a bomba com choke automático, o choke fecha para trapar a pressão
        sim.setPump(false, 0.0)
        repeat(10) { sim.advance(1.0) }
        val autoHeld = sim.snapshot()
        assertEquals("Choke automático deve fechar ao parar a bomba", 0.0, autoHeld.chokePct, 0.1)
        assertTrue(
            "Pressão trapada deve ser mantida",
            autoHeld.casingPressureKPa >= trapped * 0.9
        )

        // Em manual com choke aberto e sem bombeio, o poço despressuriza
        sim.setChokeAuto(false)
        sim.setChoke(25.0)
        repeat(20) { sim.advance(1.0) }
        val bled = sim.snapshot()
        assertTrue("Deve sinalizar choke aberto sem bombeio", bled.chokeOpenNoPump)
        assertTrue(
            "Choke aberto sem bombeio deve despressurizar (${bled.casingPressureKPa} vs $trapped)",
            bled.casingPressureKPa < trapped * 0.7
        )
    }

    @Test
    fun `sidpp e sicp permanecem fixos apos estabilizacao exceto fator de seguranca`() {
        val sim = Simulator(fastKillScenario())
        sim.closeBop(BopElement.ANNULAR)
        var guard = 0
        while (!sim.snapshot().shutInStabilized && guard < 200) {
            sim.advance(1.0)
            guard++
        }
        val atStab = sim.snapshot()
        assertTrue("Deve estabilizar", atStab.shutInStabilized)
        val fixedSidpp = atStab.recordedSidppKPa
        val fixedSicp = atStab.recordedSicpKPa

        // Avança mais 90 s fechado: valores registrados não devem derivar
        repeat(90) { sim.advance(1.0) }
        val later = sim.snapshot()
        assertEquals("SIDPP estabilizado deve permanecer fixo", fixedSidpp, later.recordedSidppKPa, 0.5)
        assertEquals("SICP estabilizado deve permanecer fixo", fixedSicp, later.recordedSicpKPa, 0.5)

        // Não aceita reduzir abaixo da estabilização
        assertTrue("Não pode reduzir abaixo do estabilizado", !sim.setShutInSafetyOffset(-100.0))
        assertTrue(
            "Par menor que estabilizado deve ser recusado",
            !sim.applyShutInSafetyPair(fixedSidpp - 50.0, fixedSicp)
        )

        // Aceita fator de segurança acima da estabilização e reflete no kill sheet
        val kmwBefore = sim.killSheetResult()!!.killMudKgM3
        assertTrue("Deve aceitar fator de segurança positivo", sim.setShutInSafetyOffset(500.0))
        val safeSnap = sim.snapshot()
        assertEquals(fixedSidpp + 500.0, safeSnap.recordedSidppKPa, 0.5)
        assertEquals(fixedSicp + 500.0, safeSnap.recordedSicpKPa, 0.5)
        assertTrue("KMW deve subir com fator de segurança", sim.killSheetResult()!!.killMudKgM3 > kmwBefore)
    }

    @Test
    fun `bop test permite abrir elementos individuais sem concluir latches da fase B`() {
        val sim = Simulator(Scenarios.byId("subsea-drillers"))
        sim.closeBop(BopElement.TEST_RAM)
        assertTrue(sim.snapshot().closedBop)
        sim.openBopElement(BopElement.TEST_RAM)
        val snap = sim.snapshot()
        assertTrue("TEST_RAM deve reabrir individualmente", !snap.closedBop)
        assertEquals(Phase.DRILLING, snap.phase)
        assertTrue(!snap.guideBopClosed && !snap.guideBleedDone && !snap.guideLinesDisplaced)
    }

    // --- Auditoria 2.11.0: guias por método, bomba pós-kill, gauges e equilíbrio ---

    @Test
    fun `parar bomba apos segunda circulacao nao faz pressao saltar para valor pre-kill`() {
        val sim = Simulator(fastKillScenario())
        assertTrue(runToFirstCirculationEnd(sim))
        sim.setPump(false, 0.0)
        sim.weightUpToKillMud()
        val kmw = sim.killSheetFrozen?.killMudKgM3 ?: 0.0
        var guard = 0
        while (sim.mudDensityKgM3 < kmw - 2.0 && guard < 600) {
            sim.advance(1.0)
            guard++
        }
        assertTrue(sim.startSecondCirculation())
        sim.setChokeAuto(true, sim.killCasingTargetKPa())
        sim.setPump(true, 60.0)
        guard = 0
        while (sim.phase != Phase.KILLED && guard < 9000) {
            sim.advance(2.0)
            guard++
        }
        assertEquals(Phase.KILLED, sim.phase)

        // Fecha o choke manualmente e pressuriza com a bomba ligada, depois desliga
        sim.setChokeAuto(false)
        sim.setChoke(0.0)
        sim.setHcr(false)
        repeat(10) { sim.advance(1.0) }
        val beforeStop = sim.snapshot().casingPressureKPa
        assertTrue("Pressão antes de parar a bomba deve ser positiva", beforeStop > 50.0)

        sim.setPump(false, 0.0)
        repeat(15) { sim.advance(1.0) }
        val afterStop = sim.snapshot()
        assertTrue(
            "Ao desligar a bomba após poço morto, casing não pode saltar acima do valor bombeado (${afterStop.casingPressureKPa} vs $beforeStop)",
            afterStop.casingPressureKPa <= beforeStop + 1.0
        )
        assertTrue(
            "SIDPP de gauge não pode voltar ao valor pré-kill (${afterStop.sidppGaugeKPa})",
            afterStop.sidppGaugeKPa <= 5.0
        )
    }

    @Test
    fun `manometros de SIDPP e SICP ficam validos com BOP fechado e durante circulacao estabilizada`() {
        val fundamentos = Simulator(Scenarios.byId("fundamentos"))
        fundamentos.setPump(false, 0.0)
        fundamentos.closeBop(BopElement.ANNULAR)
        val closedNoKick = fundamentos.snapshot()
        assertTrue("SICP deve ficar válido com BOP fechado", closedNoKick.sicpValid)
        assertTrue("SIDPP deve ficar válido com BOP fechado e bomba parada", closedNoKick.sidppValid)

        val sim = Simulator(fastKillScenario())
        sim.closeBop(BopElement.ANNULAR)
        var guard = 0
        while (!sim.snapshot().shutInStabilized && guard < 200) {
            sim.advance(1.0)
            guard++
        }
        assertTrue(sim.snapshot().shutInStabilized)
        sim.setHcr(true)
        sim.setChoke(15.0)
        sim.setPump(true, 60.0)
        sim.advance(2.0)
        val circulating = sim.snapshot()
        assertTrue("SICP deve permanecer válido bombeando fechado", circulating.sicpValid)
        assertTrue("SIDPP estabilizado deve permanecer válido como referência", circulating.sidppValid)
    }

    @Test
    fun `deteccao de kick nao dispara antes de indicios primarios`() {
        val sim = Simulator(Scenarios.byId("drillers-surface"))
        val t0 = sim.snapshot()
        assertTrue("Não pode nascer marcando kick detectado", !t0.kickDetected)
        assertEquals("Não pode nascer com semente falsa de influxo", 0.0, t0.influxM3, 1e-6)
        assertTrue(
            "Não pode registrar milestone de kick antes do fato",
            t0.milestones.none { it.contains("Kick detectado") }
        )
    }

    @Test
    fun `wait and weight executa circulacao unica apos estabilizar e pesar lama`() {
        val sim = Simulator(Scenarios.byId("ww-surface"))
        repeat(25) { sim.advance(1.0) }
        assertTrue("Kick deve ser detectado no cenário W&W", sim.snapshot().kickDetected)
        sim.setPump(false, 0.0)
        sim.setRotary(false)
        sim.setWobSetpointKlb(0.0)
        sim.closeBop(BopElement.ANNULAR)
        var guard = 0
        while (!sim.snapshot().shutInStabilized && guard < 250) {
            sim.advance(1.0)
            guard++
        }
        assertTrue("W&W deve estabilizar", sim.snapshot().shutInStabilized)
        assertTrue("Kill sheet deve confirmar", sim.confirmKillSheet())

        sim.weightUpToKillMud()
        val kmw = sim.killSheetFrozen?.killMudKgM3 ?: 0.0
        guard = 0
        while (sim.mudDensityKgM3 < kmw - 2.0 && guard < 600) {
            sim.advance(1.0)
            guard++
        }
        assertTrue("W&W deve iniciar circulação única direto de STABILIZED", sim.startSecondCirculation())
        sim.setHcr(true)
        sim.setChoke(20.0)
        sim.setChokeAuto(true, sim.killCasingTargetKPa())
        sim.setPump(true, 60.0)
        guard = 0
        while (sim.phase != Phase.KILLED && guard < 12000) {
            sim.advance(2.0)
            guard++
        }
        assertEquals("W&W deve matar o poço em circulação única", Phase.KILLED, sim.phase)
    }

    @Test
    fun `cenario combined casing possui geometria anular fisica valida`() {
        val sim = Simulator(Scenarios.byId("combined-casing"))
        repeat(30) { sim.advance(1.0) }
        val snap = sim.snapshot()
        assertTrue("BHP deve estar em ordem física realista (${snap.bhpKPa})", snap.bhpKPa < 100_000.0)
        assertTrue("Influxo deve entrar normalmente", snap.influxM3 > 0.0)
    }

}
