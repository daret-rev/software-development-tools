package com.dar.daret_rev.plugin.core

import java.io.File

/**
 * MarkdownGenerator - отвечает за генерацию документации
 *
 * @see InitMainResrchFile
 */
class MarkdownGenerator {
    companion object {

        /**
         * Генерация главного файла исследования
         *
         * @param file Главный файл исследования
         * @param system [SystemInfo] Информация о тестирующей системе
         * @param project Тестируемый проект
         *
         * @sample com.dar.daret_rev.plugin.samples.initMainResrchFileSample
         */
        fun InitMainResrchFile(file: File, system: SystemInfo, project: String) {
            val markdown = """# Информация о тестирующей системе
|                 |                                            |
|-----------------|--------------------------------------------|
| `kernelVersion` | ${system.kernelVersion}                    |
| `cpuModel`      | ${system.cpuModel}                         |
| `cpuThreads`    | ${system.cpuThreads}                       |
| `ramBytes`      | ${system.ramBytes}                         |

## Исследование улучшения производительности проекта `${project}`

""".trimIndent()
            file.writeText(markdown)
        }

        /**
         * Генерирует шипку для новой версии
         *
         * @param file Главный файл исследования
         * @param projectState
         *
         * @sample com.dar.daret_rev.plugin.samples.startVersionSample
         */
        fun startVersion(file: File, projectState: ProjectState) {
            val markdown = """
                
                ### Коммит ${projectState.commitHash}: ${projectState.commitSubject}
            """.trimIndent()

            file.appendText(markdown)
        }

        /**
         * Визуализирует результаты тестирования [coldAndHotTest]
         *
         * @param file Главный файл исследования
         * @param coldAndHotTestsResult Результаты холодного и горячего тестирования
         * @see ColdAndHotTestsResult
         * @see coldAndHotTest
         *
         * @sample com.dar.daret_rev.plugin.samples.addColdAndHotSample
         */
        fun addColdAndHot(file: File, coldAndHotTestsResult: ColdAndHotTestsResult) {
            val markdown = """
                
                #### Время и память

                |                              |                      |
                |------------------------------|----------------------|
                | Время, холодный запуск, с    | ${coldAndHotTestsResult.coldTime}         |
                | Время, горячий запуск (мин), с       | ${coldAndHotTestsResult.hotTime}          |
                | Время, горячий запуск (медиана), с   | ${coldAndHotTestsResult.mediumHotTime}    |
                | Загрузка CPU                 | ${coldAndHotTestsResult.cpuLoad}          |
                | Пик памяти, МБ               | ${coldAndHotTestsResult.maxMemory}        |
                | Скорость, ГБ/с               | ${coldAndHotTestsResult.speed}            |

            """.trimIndent()

            file.appendText(markdown)
        }

        /**
         * Визуализирует результаты тестирования [hardwareCountersProfile]
         *
         * @param file Главный файл исследования
         * @param hardwareCountersProfileResult Результаты аппаратных счетчиков
         * @see HardwareCountersProfileResult
         * @see hardwareCountersProfile
         *
         * @sample com.dar.daret_rev.plugin.samples.addHardwareCountersSapmle
         */
        fun addHardwareCounters(file: File, hardwareCountersProfileResult: HardwareCountersProfileResult) {
            val markdown = """
                
                #### Аппаратные счётчики

                |                                  |                          |
                |----------------------------------|--------------------------|
                | Такты (cycles)                   | ${hardwareCountersProfileResult.cycles}               |
                | Инструкции (instructions)        | ${hardwareCountersProfileResult.instructions}         |
                | IPC                              | ${hardwareCountersProfileResult.IPC}                  |
                | Ветвления (branches)             | ${hardwareCountersProfileResult.branches}             |
                | Промахи ветвлений (branch-misses)| ${hardwareCountersProfileResult.branchMisses}         |
                | Доля промахов ветвлений, %       | ${hardwareCountersProfileResult.proportionalMisses}   |
                | Промахи кэша (cache-misses)      | ${hardwareCountersProfileResult.cacheMisses}          |

            """.trimIndent()

            file.appendText(markdown)
        }

        /**
         * Показыват ***узкие места***, найденные в тестировании [findBottleneck]
         *
         * @param file Главный файл исследования
         * @param bottlenecks Результаты аппаратных счетчиков
         * @see findBottleneck
         *
         * @sample com.dar.daret_rev.plugin.samples.addBottleneckSamole
         */
        fun addBottleneck(file: File, bottlenecks: String) {
            val header = """

### Узкие места последней версии

Профиль снят через `perf record -g`, показаны функции с долей времени не менее 1%.

```text
    """.trimIndent()

            val markdown = header + "\n" + bottlenecks.trimEnd() + "\n```\n"

            file.appendText(markdown)
        }

        /**
         * Визуализирует результаты тестирования [sanitizerTests]
         *
         * @param file Главный файл исследования
         * @param sanitizerResult Результаты тестирования санитайзерами
         * @see SanitizerResult
         * @see sanitizerTests
         *
         * @sample com.dar.daret_rev.plugin.samples.addSanitizersSample
         */
        fun addSanitizers(file: File, sanitizerResult: SanitizerResult) {
            val markdown = """
                
                
                ### Санитайзеры

                | Проверка                   | Результат     | Первое предупреждение |
                |----------------------------|---------------|-----------------------|
                | Thread (TSan)              | ${sanitizerResult.tsanRes}   | ${sanitizerResult.tsanWarn}          |
                | Address + Undefined (ASan) | ${sanitizerResult.asanRes}   | ${sanitizerResult.asanWarn}          |

            """.trimIndent()

            file.appendText(markdown)
        }
    }
}