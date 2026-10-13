package com.dar.daret_rev.plugin.samples

import com.intellij.openapi.project.Project
import androidx.compose.runtime.Composable
import com.dar.daret_rev.plugin.ResearchPanel
import com.dar.daret_rev.plugin.UiState
import com.dar.daret_rev.plugin.core.CmakeBuilder
import com.dar.daret_rev.plugin.core.ColdAndHotTestsResult
import com.dar.daret_rev.plugin.core.HardwareCountersProfileResult
import com.dar.daret_rev.plugin.core.MarkdownGenerator
import com.dar.daret_rev.plugin.core.ProjectState
import com.dar.daret_rev.plugin.core.SanitizerResult
import com.dar.daret_rev.plugin.core.SystemInfo
import com.dar.daret_rev.plugin.core.analyzeSanRes
import com.dar.daret_rev.plugin.core.coldAndHotTest
import com.dar.daret_rev.plugin.core.findBottleneck
import com.dar.daret_rev.plugin.core.hardwareCountersProfile
import com.dar.daret_rev.plugin.core.runSanitizer
import com.dar.daret_rev.plugin.core.sanitizerTests
import java.io.File


@Composable
fun reserchPanelSample(project: Project) {
    ResearchPanel(project)
}

internal fun uiStateSample(state: UiState) {
    val uiState: UiState

    when (state) {
        is UiState.Plane -> uiState = UiState.Plane
        is UiState.Running -> uiState = UiState.Running
        is UiState.NeedArgs -> uiState = UiState.NeedArgs
        is UiState.Success -> uiState = UiState.Success
        is UiState.Error -> uiState = UiState.Error("Something error")
    }
}

fun sysInfoSample() {
    val systemInfo = SystemInfo()
    systemInfo.Init()
}

fun projStateSample() {
    val projectState = ProjectState()
    projectState.Init()
}

fun cmakeBuilderSample(project: Project) {
    val cmakeBuilder = CmakeBuilder(project)
}

fun exitValueSample(exitCode: CmakeBuilder.ExitValue) {
    val exitValue: CmakeBuilder.ExitValue = exitCode
    when (exitValue) {
        CmakeBuilder.ExitValue.NO_ARGS -> println("Please, input args")
        CmakeBuilder.ExitValue.ERROR -> error("Something error")
    }
}

suspend fun generateVersionSample(project: Project) {
    CmakeBuilder(project).GenerateVersion()
}
suspend fun generationResultSample(project: Project) {
    val generationResult = CmakeBuilder(project).GenerateVersion()

    when (generationResult) {
        is CmakeBuilder.GenerationResult.SUCCESS -> println("Success")
        is CmakeBuilder.GenerationResult.ERROR -> error("Something error")
    }
}

fun coldAndHotTestsResultSample() {
    val coldAndHotTestsResult = ColdAndHotTestsResult(
        coldTime = 0.12,
        hotTime = 0.04,
        mediumHotTime = 0.05,
        cpuLoad = 0.1,
        maxMemory = 2.2,
        speed = 1.0,
    )
}

fun coldAndHotTestsSample(basePath: File, argsFile: File, projectState: ProjectState) {
    val resultColdAndHot = coldAndHotTest(basePath, argsFile, projectState)
    resultColdAndHot.second?.let {
        println("coldAndHotTest: $it")
    }
}

fun hardwareCountersSample() {
    val hardwareCountersProfileResult = HardwareCountersProfileResult(
        cycles = 454686787,
        instructions = 749843546867867,
        branches = 456467354,
        branchMisses = 545768768,
        cacheMisses = 545768768,
        IPC = 654898.687,
        proportionalMisses = 54.4,
    )
}

fun hardwareCountersProfileSample(
    basePath: File,
    argsFile: File,
    projectState: ProjectState?,
    processInit: ProcessBuilder? = null
) {
    val resultHard = hardwareCountersProfile(basePath, argsFile, projectState, processInit)
    resultHard.second?.let {
        println("hardwareCountersProfile: $it")
    }
}

fun findBottleneckSample(
    basePath: File,
    argsFile: File,
    projectState: ProjectState?,
    processInit: ProcessBuilder? = null
) {
    val findBottleneckRes = findBottleneck(basePath, argsFile, projectState, processInit)
    findBottleneckRes.second?.let {
        println("Bottleneck: $it")
    }
}

fun sanitizerResultSample() {
    val sanitizerResult = SanitizerResult(
        tsanRes = "ЧИСТО",
        tsanWarn = "",
        asanRes = "ЧИСТО",
        asanWarn = "",
    )
}

fun sanitizerTestsSample(
    basePath: File,
    argsFile: File,
    projectState: ProjectState?,
    processInit: ProcessBuilder? = null
) {
    val resultSan = sanitizerTests(basePath, argsFile, projectState, processInit)
    resultSan.second?.let {
        println("San tests: $it")
    }
}

fun analyzeSanResSample(tsanCode: Int, asanCode: Int, tsanErr: String, asanErr: String) {
    val (tsanRes, tsanWarn) = analyzeSanRes(tsanCode, tsanErr)
    val (asanRes, asanWarn) = analyzeSanRes(asanCode, asanErr)
}

fun runSanitizerSample(
    args: List<String>,
    basePath: File,
    projectState: ProjectState,
) {
    val tsanBin = File(
        basePath, "build-tsan/${projectState.binary.name}"
    )
    val asanBin = File(
        basePath, "build-asan/${projectState.binary.name}"
    )

    val (tsanCode, tsanErr) = runSanitizer(
        tsanBin, args, basePath, "TSAN_OPTIONS", "halt_on_error=1"
    )
    val (asanCode, asanErr) = runSanitizer(
        asanBin, args, basePath, "ASAN_OPTIONS", "detect_leaks=1"
    )
}

fun initMainResrchFileSample(reserchMainFile: File, systemInfo: SystemInfo, project: Project) {
    MarkdownGenerator.InitMainResrchFile(
        reserchMainFile, systemInfo, project.name
    )
}

fun startVersionSample(file: File, projectState: ProjectState) {
    MarkdownGenerator.startVersion(file, projectState)
}

fun addColdAndHotSample(file: File, coldAndHotTestsResult: ColdAndHotTestsResult) {
    MarkdownGenerator.addColdAndHot(file, coldAndHotTestsResult)
}

fun addHardwareCountersSapmle(file: File, hardwareCountersProfileResult: HardwareCountersProfileResult) {
    MarkdownGenerator.addHardwareCounters(file, hardwareCountersProfileResult)
}

fun addBottleneckSamole(file: File, bottlenecks: String) {
    MarkdownGenerator.addBottleneck(file, bottlenecks)
}

fun addSanitizersSample(file: File, sanitizerResult: SanitizerResult) {
    MarkdownGenerator.addSanitizers(file, sanitizerResult)
}