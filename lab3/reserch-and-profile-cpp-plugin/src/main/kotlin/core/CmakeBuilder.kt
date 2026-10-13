package com.dar.daret_rev.plugin.core

import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.Charset
import kotlin.text.startsWith


/**
 * SystemInfo - хранит информацию о тестирующей системе
 *
 * @sample com.dar.daret_rev.plugin.samples.sysInfoSample
 *
 * @property kernelVersion Версия ядра ОС
 * @property cpuModel Весрсия процесора
 * @property cpuThreads Количество доступных потоков процессора
 * @property ramBytes Общий объем оперативной памяти
 */
data class SystemInfo(
    var kernelVersion: String = "",
    var cpuModel: String = "",
    var cpuThreads: Int = 0,
    var ramBytes: Long = 0,
    )
{
    val commands = mapOf(
        "kernel" to listOf("uname", "-r"),
        "cpuModel" to listOf("lscpu"),
        "cpuThreads" to listOf("nproc"),
        "ramBytes" to listOf("free", "-b")
    )

    /**
     * Инициализирует данные о системе
     *
     * @sample com.dar.daret_rev.plugin.samples.sysInfoSample
     *
     * @param processInit Конструктор процессов в корне проекта, через который выполняются команды
     * и собирается информация. При получении `null` создается новый [ProcessBuilder]
     * с раюбочей дерикторией в корне проекта `project.basePath`
     */
    fun Init(processInit: ProcessBuilder? = null) {
        val process = processInit ?: ProcessBuilder()
        kernelVersion = process.command(
            commands.getValue("kernel")
        ).start().inputStream.bufferedReader(Charset.forName("UTF-8")).use {
            it.readText().trim()
        }

        cpuModel = process.command(
            commands.getValue("cpuModel")
        ).start().inputStream.bufferedReader(Charset.forName("UTF-8")).use {
            it.readText().trim()
        }.lines().first {
            it.startsWith("Model name:") || it.startsWith("Имя модели:")
        }.substringAfter(":").trim()

        cpuThreads = process.command(
            commands.getValue("cpuThreads")
        ).start().inputStream.bufferedReader(Charset.forName("UTF-8")).use {
            it.readText().trim().toInt()
        }

        ramBytes = process.command(
            commands.getValue("ramBytes")
        ).start().inputStream.bufferedReader(Charset.forName("UTF-8")).use {
            it.readText().trim()
        }.lines().first { it.startsWith("Mem:") }.substringAfter(":").trim()
            .split(" ")[0].toLong()
    }
}

/**
 * ProjectState собирает проект и данные о нем
 *
 * @sample com.dar.daret_rev.plugin.samples.projStateSample
 *
 * @property commitHash Хэш текущего коммита
 * @property commitSubject Описание коммита
 * @property compilerHave Код возврата проверки окружения и зависимостей
 * @property compile Код возврата сборки
 * @property binary Исполняемый файл проекта
 */
@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
data class ProjectState(
    var commitHash: String = "",
    var commitSubject: String = "",
    var compilerHave: Int = 0,
    var compile: Int = 0,
    var binary: File = File(""),
)
{
    val commands = mapOf(
        "commitHash" to listOf("git", "rev-parse", "--short", "HEAD"),
        "commitSubject" to listOf("git", "log", "-1", "--format=%s"),
        "compilerHave" to listOf("cmake", "-S", ".", "-B", "build", "-DCMAKE_BUILD_TYPE=RelWithDebInfo"),
        "compile" to listOf("cmake", "--build", "build", "-j"),
    )

    /**
     * Получает данные о текущем проекте
     *
     * @sample com.dar.daret_rev.plugin.samples.projStateSample
     *
     * @param processInit Конструктор процессов в корне проекта, через который выполняются команды
     * и собирается информация. При получении `null` создается новый [ProcessBuilder]
     * с раюбочей дерикторией в корне проекта `project.basePath`
     */
    fun Init(processInit: ProcessBuilder? = null): Int {
        val process = processInit ?: ProcessBuilder()

        commitHash = process.command(
            commands.getValue("commitHash")
        ).start().inputStream.bufferedReader(Charset.forName("UTF-8")).use {
            it.readText().trim()
        }

        commitSubject = process.command(
            commands.getValue("commitSubject")
        ).start().inputStream.bufferedReader(Charset.forName("UTF-8")).use {
            it.readText().trim()
        }

        compilerHave = process.command(
            commands.getValue("compilerHave")
        ).start().waitFor()

        if (compilerHave != 0) {
            return compilerHave
        }

        compile = process.command(
            commands.getValue("compile")
        ).start().waitFor()

        var buildDir = File(process.directory(), "build")
        if (!buildDir.exists()) {
            buildDir = File(process.directory(),"cmake-build-debug")
        }
        val cmakeCache = File(buildDir,"CMakeCache.txt")
        val binaryName = cmakeCache.useLines { lines ->
            lines.find { it.startsWith("CMAKE_PROJECT_NAME:") }
                ?.substringAfter("=")
        }
        binary = File(buildDir, binaryName)

        return 0
    }
}

/**
 * Оркестратор исследования проекта, открытого в IDE.
 *
 * Класс собирает информацию о состоянии проекта [ProjectState]
 * и тестирующей системе [SystemInfo], инициализирует исследование и анализирует версии.
 * @see ProjectState
 * @see SystemInfo
 *
 * [Init]:
 * проверяет окружени и в случае необходимости создает нужные каталоги и файлы.
 * @see Init
 *
 * [Start]:
 * Собирает информацию о системе и проекте, подготавливает ее для тестирований.
 * @see Start
 *
 * [GenerateVersion]:
 * Непосредственно проводит тестирование и генерирует отчет через [MarkdownGenerator]
 * @see GenerateVersion
 *
 * @param project Проект, открытый в IDE.
 *
 * @sample com.dar.daret_rev.plugin.samples.cmakeBuilderSample
 */
class CmakeBuilder(private val project: Project) {

    private var systemInfo: SystemInfo? = null
    private var projectState: ProjectState? = null

    /**
     * Код возврата, показывающий тип ошибки:
     *
     * - [ERROR] Произошла ошибка в процессе работы
     * - [NO_ARGS] Необходимо указать аргументы для продолжения работы
     *
     * @sample com.dar.daret_rev.plugin.samples.exitValueSample
     */
    enum class ExitValue {
        ERROR,
        NO_ARGS
    }

    /**
     * Результат процесса тестирования и генерации отчета
     *
     * - [SUCCESS] Программа завершилась удачно
     * - [ERROR] Произошла какая-то ошибка
     *
     * @sample com.dar.daret_rev.plugin.samples.generationResultSample
     */
    sealed class GenerationResult() {
        /**
         * Удачное выполнение
         */
        object SUCCESS: GenerationResult()

        /**
         * Ошибка при выполнении
         *
         * @param value [ExitValue] Тип возврата ошибки
         * @param code Код ошибки
         *
         * @see ExitValue
         */
        data class ERROR(val value: ExitValue, val code: Int = 0): GenerationResult()
    }


    /**
     * Подготавливает каталог исследования в корне проекта и создаёт главный файл отчёта.
     *
     * Если каталога `reserch` нет, создается. Если главного файла исследования `RESERCH.md` нет,
     * создается и заполняется. В него идут собранные данные о системе ([SystemInfo]), через
     * генерацию шапки отчета ([MarkdownGenerator.InitMainResrchFile]). Если каталог и файл существуют,
     * то не создаются повторно.
     * @see SystemInfo
     * @see MarkdownGenerator.InitMainResrchFile
     *
     * Процесс переключается на `Dispatchers.IO`, чтобы не блокировать UI поток.
     *
     * Пример вызова:
     *
     * ```kotlin
     * val process = ProcessBuilder().directory(basePath)
     * Init(process)
     * ```
     *
     * @param processInit Конструктор процессов в корне проекта, через который выполняются команды
     * и собирается информация. При получении `null` создается новый [ProcessBuilder]
     * с раюбочей дерикторией в корне проекта `project.basePath`
     *
     * @return `true` если каталог или файл были созданы при этом вызове (инициализауия прошла),
     * и `false` в остальных случаях
     */
    private suspend fun Init(processInit: ProcessBuilder? = null): Boolean {
        return withContext(Dispatchers.IO) {
            val basePath = File(project.basePath!!)
            val process = processInit ?: ProcessBuilder().directory(basePath)
            var init = false
            val reserchDir = File(basePath, "reserch")
            if (!reserchDir.exists()) {
                reserchDir.mkdirs()
                init = true
            }
            val reserchMainFile = File(reserchDir, "RESERCH.md")
            if (!reserchMainFile.exists()) {
                reserchMainFile.createNewFile()
                systemInfo = SystemInfo()
                systemInfo?.Init(process)
                MarkdownGenerator.InitMainResrchFile(reserchMainFile, systemInfo!!, project.name)
                init = true
            }

            return@withContext init
        }
    }

    /**
     * Генерация отчёта для текущей версии проекта.
     *
     * Сначала вызывается [Start], он готовит среду и инициализирует необходимые файлы.
     * Проверят наличие аргументов для тестирований в `reserch/.args.txt` или явное их отсутствие.
     * Если их нет возвращает [ExitValue.NO_ARGS], чтобы пользователь добавил аргументы.
     *
     * После выполняеттестирования и мобирает информацию, записывая данные
     * в `reserch/RESERCH.md` через [MarkdownGenerator]:
     * 1. [coldAndHotTest]: холодный и горячий запуск, время и память;
     * 2. [hardwareCountersProfile]: аппаратные счетчики `perf stat`;
     * 3. [findBottleneck]: ***узкие места*** `perf record` & `perf report`;
     * 4. [sanitizerTests]: тестирование TSan и ASan;
     *
     * При получении ошибки в тесте, завершает выполнение и возвращает ошибку.
     *
     * Функция блокирующая, поэтому вызывать ее с UI потока нельзя,
     * необходимо переключиться на рабочий поток.
     *
     * @sample com.dar.daret_rev.plugin.samples.generateVersionSample
     *
     * @param processInit Конструктор процессов в корне проекта, через который выполняются команды
     * и собирается информация. При получении `null` создается новый [ProcessBuilder]
     * с раюбочей дерикторией в корне проекта `project.basePath`
     *
     * @return [GenerationResult.SUCCESS] елси все выполнилось корректно,
     * [GenerationResult.ERROR] с [ExitValue.NO_ARGS] если нужно ввести аргументы
     * и [GenerationResult.ERROR] с [ExitValue.ERROR] в случае ошибки
     */
    suspend fun GenerateVersion(processInit: ProcessBuilder? = null): GenerationResult {
        Start()
        projectState ?: return GenerationResult.ERROR(ExitValue.ERROR)
        val basePath = File(project.basePath!!)
        val process = processInit ?: ProcessBuilder().directory(basePath)
        val reserchDir = File(basePath, "reserch")
        val reserchMainFile = File(reserchDir, "RESERCH.md")
        val argsFile = File(reserchDir, ".args.txt")
        if (!argsFile.exists()) {
            println("no args!")
            return GenerationResult.ERROR(ExitValue.NO_ARGS)
        }

        MarkdownGenerator.startVersion(reserchMainFile, projectState!!)

        val resultColdAndHot = coldAndHotTest(basePath, argsFile, projectState)
        resultColdAndHot.second?.let {
            println("coldAndHotTest: $it")
            return it
        }
        val coldAndHotTestsResult = resultColdAndHot.first ?: return GenerationResult.ERROR(ExitValue.ERROR)
//        println("cold result ${coldAndHotTestsResult}")
        MarkdownGenerator.addColdAndHot(reserchMainFile, coldAndHotTestsResult)

        val resultHard = hardwareCountersProfile(basePath, argsFile, projectState, process)
        resultHard.second?.let {
            return it
        }
        val hardwareCountersProfileResult = resultHard.first ?: return GenerationResult.ERROR(ExitValue.ERROR)
//        println("hardware counters profile: $hardwareCountersProfileResult")
        MarkdownGenerator.addHardwareCounters(reserchMainFile, hardwareCountersProfileResult)

        val findBottleneckRes = findBottleneck(basePath, argsFile, projectState, process)
        findBottleneckRes.second?.let {
            return it
        }
        val prefReportBottleneck = findBottleneckRes.first ?: return GenerationResult.ERROR(ExitValue.ERROR)
//        println("pref report bottleneck: $prefReportBottleneck")
        MarkdownGenerator.addBottleneck(reserchMainFile, prefReportBottleneck)

        val resultSan = sanitizerTests(basePath, argsFile, projectState, process)
        resultSan.second?.let {
            return it
        }

        val sanitizerResult = resultSan.first ?: return GenerationResult.ERROR(ExitValue.ERROR)
//        println("sanitizerResult: $sanitizerResult")
        MarkdownGenerator.addSanitizers(reserchMainFile, sanitizerResult)

        return GenerationResult.SUCCESS
    }

    /**
     * Подготавливает окружение.
     *
     * Переключает поток `Dispatchers.IO`, чтобы увести обработку с UI потока,
     * создаёт каталог и главный файл отчёта, если их нет через [Init],
     * затем создаёт и заполняет [ProjectState] (метаданные текущего проекта). Функция
     * приостанавливающая и держит поток до полго завершения, поэтому
     * после нее данные точно валидны.
     *
     *
     * Пример вызова:
     *
     * ```
     * Start()
     * ```
     */
    suspend fun Start() {
        withContext(Dispatchers.IO) {
            val basePath = File(project.basePath!!)
            val process = ProcessBuilder().directory(basePath)
            Init(process)
            projectState = ProjectState()
            projectState?.Init(process)
//            println("project state: $projectState")
        }
    }
}