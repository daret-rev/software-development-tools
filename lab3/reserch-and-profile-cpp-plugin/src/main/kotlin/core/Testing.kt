package com.dar.daret_rev.plugin.core

import com.dar.daret_rev.plugin.core.CmakeBuilder.ExitValue
import com.dar.daret_rev.plugin.core.CmakeBuilder.GenerationResult
import com.dar.daret_rev.plugin.core.CmakeBuilder.GenerationResult.ERROR
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.util.ExecUtil
import com.jetbrains.cidr.execution.CidrPosixLocaleConstants.LC_ALL
import java.io.File
import java.nio.charset.Charset


/**
 * Результаты холодных и горячих тестов
 *
 * @property coldTime Время холодного запуска
 * @property hotTime Мин время горячего запуска
 * @property mediumHotTime Медиана горячего запуска
 * @property cpuLoad Нагрузка на процессор
 * @property maxMemory Потребление памяти
 * @property speed Скорость, Гб/с
 *
 * @sample com.dar.daret_rev.plugin.samples.coldAndHotTestsResultSample
 */
data class ColdAndHotTestsResult(
    val coldTime: Double,
    val hotTime: Double,
    val mediumHotTime: Double,
    val cpuLoad: Double,
    val maxMemory: Double,
    val speed: Double,
)

/**
 * Запускает холодные и горячие тесты, собирая данные о скорости и памяти
 *
 * Для тестов запрашивается root доступ для сброса кэша ОС и проводится холодный запуск.
 * Далее 5 горячих без сброса, из них берутся реальное время (E), время в пользовательском режиме (U),
 * время в ядре (S) и пик памяти в КБ (M).
 *
 * Результаты формируются в структуру [ColdAndHotTestsResult]:
 * - `coldTime` - E холодного запуска, сж
 * - `hotTime` - минимальное E среди теплых запусков, с;
 * - `mediumHotTime` - медиана E среди теплых запусков, с;
 * - `cpuLoad` - (U + S) / E запуска с минимальным E;
 * - `maxMemory` - M / 1024 того же запуска, МБ;
 * - `speed` - скорость обработки данных, ГБ/с (пока заглушка).
 *
 * @param basePath Корневая директория проекта.
 * @param argsFile Файл с аргументами для тестирования
 * @param projectState [ProjectState] Информация о проекте
 * @see ProjectState
 *
 * @return Pair [ColdAndHotTestsResult] и null при успехе, либо null и [GenerationResult.ERROR] при ошибке
 *
 * @sample com.dar.daret_rev.plugin.samples.coldAndHotTestsSample
 */
internal fun coldAndHotTest(
    basePath: File,
    argsFile: File,
    projectState: ProjectState?):
        Pair<ColdAndHotTestsResult?, GenerationResult?> {
    projectState ?: return null to ERROR(ExitValue.ERROR)
    val rawCommandLine = GeneralCommandLine(
        "/usr/bin/sh", "-c", "sync; echo 3 > /proc/sys/vm/drop_caches"
    ).withWorkDirectory(basePath)
    @Suppress("UnstableApiUsage")
    val reqSudo = ExecUtil.sudo(
        rawCommandLine,
        "Plugin needs to root privileges to drop system cache for cold tests"
    )

    val sudoExitCode = reqSudo.waitFor()
    if (sudoExitCode != 0) {
        return null to ERROR(ExitValue.ERROR, sudoExitCode)
    }

    val argsTxt = argsFile.readText(Charset.forName("UTF-8")).trim()

    val coldTimeCommand = mutableListOf(
        "/usr/bin/time",
        "-f", "%e %U %S %M",
        projectState.binary.absolutePath
    )

    if (!argsTxt.equals("--no-args", ignoreCase = true)
        && argsTxt.isNotBlank()) {
        val cleaned = argsTxt.split(" ").filter { it.isNotBlank() }
        coldTimeCommand.addAll(cleaned)
    }

//    println("cold time command: $coldTimeCommand")

    val coldCommandLine = GeneralCommandLine(coldTimeCommand)
        .withWorkDirectory(basePath)
    val coldProcessRes = CapturingProcessHandler(coldCommandLine).runProcess()
    val coldRowResult = coldProcessRes.stderr.trim()
    val (e, _, _, _) = coldRowResult.split(" ").map { it.toDouble() }
//    println("cold row result: ${e}, ${U}, ${S}, ${M}")

    val hotRuns = mutableListOf<List<Double>>()

    repeat(5) {
        val hotCommandLines = GeneralCommandLine(coldTimeCommand)
            .withWorkDirectory(basePath)
        val hotProcessRes = CapturingProcessHandler(hotCommandLines).runProcess()
        val hotRowResult = hotProcessRes.stderr.trim()
        hotRuns.add(
            hotRowResult.split(" ").map { it.toDouble() }.toList()
        )
    }

    if (hotRuns.isEmpty()) return null to ERROR(ExitValue.ERROR)

    val best = hotRuns.minBy { it[0] }

    val coldAndHotTestsResult = ColdAndHotTestsResult(
        coldTime = e,
        hotTime = best[0],
        mediumHotTime = hotRuns.sumOf { it[0] } / hotRuns.size,
        cpuLoad = if (best[0] > 0) (best[1] + best[2]) / best[0] else best[0],
        maxMemory = best[3] / 1024,
        speed = 0.0,
    )

    return coldAndHotTestsResult to null
}

/**
 * Структура содержит информацию о профайлинге проекта по аппаратным счетчикам
 *
 * @property cycles (такты), сколько тактов процессора заняла программа
 * @property instructions (инструкции), сколько машинных инструкций выполнено
 * @property branches (ветвления), сколько раз встретились условные переходы в машинном коде
 * @property branchMisses (промахи ветвлений), сколько раз планировщик процессора ошибся с предсказанием ветвления
 * @property cacheMisses (промахи кэша), сколько раз необходимых данных не было в кэше процессора
 * @property IPC (Instructions Per Cycle), количество инструкций за такт
 * @property proportionalMisses (Доля промахов ветвлений, %), насколько предсказуем код
 *
 * @sample com.dar.daret_rev.plugin.samples.hardwareCountersSample
 */
data class HardwareCountersProfileResult(
    var cycles: Long? = null,
    var instructions: Long? = null,
    var branches: Long? = null,
    var branchMisses: Long? = null,
    var cacheMisses: Long? = null,
    var IPC: Double? = null,
    var proportionalMisses: Double? = null,
)

/**
 * Запускает аналитику аппаратных счетчиков через `perf stat`
 *
 * Запускает `perf stat -r 5 -x, -e cycles,instructions,branches,branch-misses,cache-misses`
 * для собранного бинарника. Результат сохраняет во временный CSV-файл `/tmp/stat.csv`.
 * После файл парсится и разбирается на пары ключ-значение
 * -- берется значение (первое поле) и ставится в соответсявие имени параметра (третье поле)
 *
 * Из собранных данных формируется [HardwareCountersProfileResult]:
 * - `cycles`, `instructions`, `branches`, `branchMisses`, `cacheMisses` - значения из счетчиков;
 * - `IPC` - `instructions / cycles`;
 * - `proportionalMisses` - `100 * branchMisses / branches`;
 *
 * Не распознаные счётчики остаются `null` (в отчёте `н/д`).
 * @see HardwareCountersProfileResult
 *
 * @param basePath Корневая директория проекта.
 * @param argsFile Файл с аргументами для тестирования
 * @param projectState [ProjectState] Информация о проекте
 * @param processInit Конструктор процессов в корне проекта, через который выполняются команды
 * и собирается информация. При получении `null` создается новый [ProcessBuilder]
 * с раюбочей дерикторией в корне проекта `project.basePath`
 * @see ProjectState
 *
 * @return Pair [HardwareCountersProfileResult] и null при успехе, либо null и [GenerationResult.ERROR] при ошибке
 *
 * @sample com.dar.daret_rev.plugin.samples.hardwareCountersProfileSample
 */
internal fun hardwareCountersProfile(
    basePath: File,
    argsFile: File,
    projectState: ProjectState?,
    processInit: ProcessBuilder? = null
): Pair<HardwareCountersProfileResult?, GenerationResult?> {
    projectState ?: return null to ERROR(ExitValue.ERROR)

    val statCsv = File("/tmp/stat.csv")
    if (statCsv.exists()) {
        statCsv.delete()
    }

    val process = processInit ?: ProcessBuilder().directory(basePath)
    val argsTxt = argsFile.readText(Charset.forName("UTF-8")).trim()

    val perfStatCommand = mutableListOf(
        "perf", "stat", "-r", "5", "-x,", "-e",
        "cycles,instructions,branches,branch-misses,cache-misses",
        "-o", "/tmp/stat.csv", "--",
        projectState.binary.absolutePath
    )

    if (!argsTxt.equals("--no-args", ignoreCase = true)
        && argsTxt.isNotBlank()) {
        val cleaned = argsTxt.split(" ").filter { it.isNotBlank() }
        perfStatCommand.addAll(cleaned)
    }

    process.environment()[LC_ALL] = "C"
    if (
        process.command(
            perfStatCommand
        ).redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .start().waitFor() != 0
        ) {
        println("perfStatCommand exit with non-zero exit value")
        return null to ERROR(ExitValue.ERROR)
    }

    val hardwareCountersProfileResult = HardwareCountersProfileResult()

    statCsv.bufferedReader().forEachLine { line ->
        if (!line.isEmpty() && !line.startsWith("#")) {
            val parts = line.split(",")
            val value = parts[0].toLong()
            val name = parts[2]

            when  {
                name.contains("cycles", ignoreCase = true) && !name.contains("branch") -> {
                    hardwareCountersProfileResult.cycles = value
                }
                name.contains("instructions", ignoreCase = true) -> {
                    hardwareCountersProfileResult.instructions = value
                }
                name.contains("branch-misses", ignoreCase = true) -> {
                    hardwareCountersProfileResult.branchMisses = value
                }
                name.contains("branches", ignoreCase = true) -> {
                    hardwareCountersProfileResult.branches = value
                }
                name.contains("cache-misses", ignoreCase = true) -> {
                    hardwareCountersProfileResult.cacheMisses = value
                }
            }

            hardwareCountersProfileResult.IPC = if (hardwareCountersProfileResult.instructions != null
                && hardwareCountersProfileResult.cycles != null) {
                hardwareCountersProfileResult.instructions!!.toDouble() / hardwareCountersProfileResult.cycles!!.toDouble()
            } else {
                null
            }
            hardwareCountersProfileResult.proportionalMisses = if (hardwareCountersProfileResult.branchMisses != null
                && hardwareCountersProfileResult.branches != null) {
                100 * hardwareCountersProfileResult.branchMisses!!.toDouble() / hardwareCountersProfileResult.branches!!.toDouble()
            } else {
                null
            }
        }
    }

    return hardwareCountersProfileResult to null
}

/**
 * Ищет ***узкие места***, где программа трати больше всего времени
 * через `perf record` и `perf report`.
 *
 * `perf record` запускает проект с аргументами и пишет профиль в `/tmp/perf.data`,
 * вывод и ошибки отбрасываются. Затем запускается `perf report` и обрабатывает профиль
 * `/tmp/perf.data`, вывод принимается как результат и фильтруется от информационных строк.
 *
 * @param basePath Корневая директория проекта.
 * @param argsFile Файл с аргументами для тестирования
 * @param projectState [ProjectState] Информация о проекте
 * @param processInit Конструктор процессов в корне проекта, через который выполняются команды
 * и собирается информация. При получении `null` создается новый [ProcessBuilder]
 * с раюбочей дерикторией в корне проекта `project.basePath`
 * @see ProjectState
 *
 * @return Pair [String] и null при успехе, либо null и [GenerationResult.ERROR] при ошибке
 *
 * @sample com.dar.daret_rev.plugin.samples.findBottleneckSample
 */
internal fun findBottleneck(
    basePath: File,
    argsFile: File,
    projectState: ProjectState?,
    processInit: ProcessBuilder? = null
): Pair<String?, GenerationResult?> {
    projectState ?: return null to ERROR(ExitValue.ERROR)

    val perfData = File("/tmp/perf.data")
    if (perfData.exists()) {
        perfData.delete()
    }

    val process = processInit ?: ProcessBuilder().directory(basePath)
    val argsTxt = argsFile.readText(Charset.forName("UTF-8")).trim()

    var perfStatCommand = mutableListOf(
        "perf", "record", "-g", "-o", perfData.absolutePath,
        "--", projectState.binary.absolutePath
    )

    if (!argsTxt.equals("--no-args", ignoreCase = true)
        && argsTxt.isNotBlank()) {
        val cleaned = argsTxt.split(" ").filter { it.isNotBlank() }
        perfStatCommand.addAll(cleaned)
    }

    process.environment()[LC_ALL] = "C"

    if (process.command(perfStatCommand)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start().waitFor() != 0) {
        println("perfStatCommand exit with non-zero exit value")
        return null to ERROR(ExitValue.ERROR)
    }

    perfStatCommand = mutableListOf(
        "perf", "report", "-g","caller,0.5,callee,function,percent",
        "--max-stack", "10", "-i", perfData.absolutePath,
        "--stdio", "--no-children", "--percent-limit", "1"
    )

    val reportProc = process.command(perfStatCommand)
        .redirectError(ProcessBuilder.Redirect.DISCARD)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .start()

    var output = reportProc.inputStream.bufferedReader().readText()
    if (reportProc.waitFor() != 0) {
        println("perfStatCommand exit with non-zero exit value")
        return null to ERROR(ExitValue.ERROR)
    }

    output = output.lines().asSequence().filter { !it.startsWith("#") }.joinToString("\n")

    return output to null
}

/**
 * Структура для хранения результатов санитайзеров
 *
 * @property tsanRes Вердикт по результатам TSan
 * @property tsanWarn Доп. информация из вывода TSan
 * @property asanRes Вердикт по результатам ASan
 * @property asanWarn Доп. информация из вывода ASan
 *
 * @sample com.dar.daret_rev.plugin.samples.sanitizerResultSample
 */
data class SanitizerResult(
    val tsanRes: String,
    val tsanWarn: String,
    val asanRes: String,
    val asanWarn: String,
)

/**
 * Запускаем тестирования с санитайзерами для выявления ошибок.
 *
 * Запускаем отдельные сборки `tsan` для поиска гонок и проблем с многопоточностью,
 * а также `asan` для выявления проблем с памятью. Сборки настраиваются и запускаются отдельно,
 * т.к. имеют разный функционал. В случае неудачи (exit!=0) любого этапа выходим с ошибкой.
 *
 * Далее запускаются сами санитайзеры `build-tsan/<binary>`, `build-asan/<binary>`,
 * с заданными аргументами из `.args.txt` через [runSanitizer], а результаты анализируются
 * через [analyzeSanRes]
 * @see runSanitizer
 * @see analyzeSanRes
 *
 * @param basePath Корневая директория проекта.
 * @param argsFile Файл с заданными аргументами для исследования
 * @param projectState [ProjectState] Информация о проекте

 * @param processInit Конструктор процессов в корне проекта, через который выполняются команды
 * и собирается информация. При получении `null` создается новый [ProcessBuilder]
 * с раюбочей дерикторией в корне проекта `project.basePath`
 *
 * @return [SanitizerResult] и null в случае успеха, либо null и [GenerationResult.ERROR] в случаем ошибки
 * @see SanitizerResult
 *
 * @sample com.dar.daret_rev.plugin.samples.sanitizerTestsSample
 */
internal fun sanitizerTests(
    basePath: File,
    argsFile: File,
    projectState: ProjectState?,
    processInit: ProcessBuilder? = null
): Pair<SanitizerResult?, GenerationResult?> {
    projectState ?: return null to ERROR(ExitValue.ERROR)

    val process = processInit ?: ProcessBuilder().directory(basePath)
    process.environment()[LC_ALL] = "C"
    val argsTxt = argsFile.readText(Charset.forName("UTF-8")).trim()

    var command = mutableListOf(
        "cmake", "-S", ".", "-B", "build-tsan", "-DCMAKE_BUILD_TYPE=Debug",
        "-DCMAKE_CXX_COMPILER=clang++", "-DCMAKE_CXX_FLAGS=-fsanitize=thread -g",
        "-DCMAKE_EXE_LINKER_FLAGS=-fsanitize=thread"
    )
    var res = process.command(command).start().waitFor()
    if (res != 0) {
        return null to ERROR(ExitValue.ERROR, res)
    }

    command = mutableListOf(
        "cmake", "--build", "build-tsan", "-j"
    )
    res = process.command(command).start().waitFor()
    if (res != 0) {
        return null to ERROR(ExitValue.ERROR, res)
    }

    command = mutableListOf(
        "cmake", "-S", ".", "-B", "build-asan", "-DCMAKE_BUILD_TYPE=Debug",
        "-DCMAKE_CXX_COMPILER=clang++", "-DCMAKE_CXX_FLAGS=-fsanitize=address,undefined -g",
        "-DCMAKE_EXE_LINKER_FLAGS=-fsanitize=address,undefined"
    )
    res = process.command(command).start().waitFor()
    if (res != 0) {
        return null to ERROR(ExitValue.ERROR, res)
    }

    command = mutableListOf(
        "cmake", "--build", "build-asan", "-j"
    )
    res = process.command(command).start().waitFor()
    if (res != 0) {
        return null to ERROR(ExitValue.ERROR, res)
    }

    val args = if (!argsTxt.equals("--no-args", ignoreCase = true) && argsTxt.isNotBlank()) {
        argsTxt.split(" ").filter { it.isNotBlank() }
    } else {
        emptyList()
    }

    val tsanBin = File(basePath, "build-tsan/${projectState.binary.name}")
    val asanBin = File(basePath, "build-asan/${projectState.binary.name}")

    val (tsanCode, tsanErr) = runSanitizer(tsanBin, args, basePath, "TSAN_OPTIONS", "halt_on_error=1")
    val (asanCode, asanErr) = runSanitizer(asanBin, args, basePath, "ASAN_OPTIONS", "detect_leaks=1")

    val (tsanRes, tsanWarn) = analyzeSanRes(tsanCode, tsanErr)
    val (asanRes, asanWarn) = analyzeSanRes(asanCode, asanErr)
    val sanitizerResult = SanitizerResult(
        tsanRes = tsanRes,
        tsanWarn = tsanWarn,
        asanRes = asanRes,
        asanWarn = asanWarn,
    )

    return sanitizerResult to null
}

/**
 * Патерны/маркеры возможных ошибок в санитайзере, сигнализируют об ошибке
 */
private val markers = listOf(
    "WARNING: ThreadSanitizer", "ERROR: AddressSanitizer",
    "runtime error:", "LeakSanitizer"
)

/**
 * Анализ информации, полученной от санетайзера
 *
 * @param code Код возврата санитайзера
 * @param stderr Вывод санитайзера
 *
 * @return Pair `Вердикт по санетайзеру` и `полученная информация`
 *
 * @sample com.dar.daret_rev.plugin.samples.analyzeSanResSample
 */
internal fun analyzeSanRes(code: Int, stderr: String): Pair<String, String> {
    if ("unexpected memory mapping" in stderr) {
        return "НЕ ЗАПУСТИЛСЯ" to "ThreadSanitizer: unexpected memory mapping"
    }

    val warn = stderr.lines().asSequence().firstOrNull { line -> markers.any { it in line} }
    return  when {
        warn != null -> "НАЙДЕНЫ ПРОБЛЕМЫ" to warn
        code != 0 -> "НАЙДЕНЫ ПРОБЛЕМЫ" to "код возврата $code: ${stderr.lines().asSequence().firstOrNull().orEmpty()}"
        else -> "ЧИСТО" to ""
    }
}

/**
 * Запуск санитайзера и получение информации
 *
 * Запускаем санитайзер с аргументами [args], в корне проекта [basePath]
 * с переменной окружения [envName]=[envValue]. Поток вывода игнорируем, а
 * поток ошибок перехватываем, т.к. санитайзеры выводят туда находки и
 * возвращаем в паре с кодом возврата.
 *
 * @param binary исполняемый файл, собранный санитайзером.
 * @param args Аргументы, указаные для исследования. (Может быть [emptyList])
 * @param basePath Корневая директория проекта.
 * @param envName Имя переменной окружения с настройками санитайзера
 * @param envValue Значение переменной окружения
 * @param processInit Конструктор процессов в корне проекта, через который выполняются команды
 * и собирается информация. При получении `null` создается новый [ProcessBuilder]
 * с раюбочей дерикторией в корне проекта `project.basePath`
 *
 * @return Pair код возврата и вывод потока ошибок.
 *
 * @sample com.dar.daret_rev.plugin.samples.runSanitizerSample
 */
internal fun runSanitizer(
    binary: File,
    args: List<String>,
    basePath: File,
    envName: String,
    envValue: String,
    processInit: ProcessBuilder? = null
): Pair<Int, String> {
    val process = processInit ?: ProcessBuilder().directory(basePath)
    process.command(listOf(binary.absolutePath) + args)
        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
        .environment()[LC_ALL] = "C"
    process.environment()[envName] = envValue

    val proc = process.start()
    val stderr = proc.errorStream.bufferedReader().readText()
    val codeExit = proc.waitFor()
    return codeExit to stderr
}