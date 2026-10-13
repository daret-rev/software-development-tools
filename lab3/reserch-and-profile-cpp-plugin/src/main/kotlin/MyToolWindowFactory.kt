package com.dar.daret_rev.plugin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dar.daret_rev.plugin.core.CmakeBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.jewel.bridge.addComposeTab
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import java.io.File

open class MyToolWindowFactory : ToolWindowFactory {
     lateinit var project: Project

    override fun shouldBeAvailable(project: Project) = true

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        this.project = project
        toolWindow.addComposeTab("Research", focusOnClickInside = true) {
            ResearchPanel(project)
        }
    }
}


/**
 * Состояние работы плагина, изменяется в процессе работы и влияет на процесс
 *
 * @sample com.dar.daret_rev.plugin.samples.uiStateSample
 *
 * @see Plane
 * @see Running
 * @see NeedArgs
 * @see Error
 * @see Success
 */
internal sealed interface UiState {
    /**
     * Стартовое сотояние плагина
     */
    object Plane : UiState

    /**
     * Состояние выполнения работы, нужно для анимации загрузки
     */
    object Running : UiState

    /**
     * Состояние, появляющееся после возврата ошибки [CmakeBuilder.ExitValue.NO_ARGS],
     * переводит окно в запрос аргументов у пользователя
     */
    object NeedArgs : UiState

    /**
     * Ошибка, возвращается из тестов и выводится пользователю
     *
     * @param text Текст ошибки
     */
    data class Error(val text: String) : UiState

    /**
     * Состояние успешного выполнения задачи
     */
    object Success : UiState
}

/**
 * Панель для генерации отчета об исследовании ([Composable] компонент)
 *
 * Панель может запускать генерацию очета текущей,
 * если исследования не существует, создает новое,
 * при отсутствии аргументов запрашивает их.
 *
 * @sample com.dar.daret_rev.plugin.samples.reserchPanelSample
 *
 * @param project [Project] Текущий проект, открытый в IDE.
 */
@Composable
internal fun ResearchPanel(project: Project) {
    var state by remember { mutableStateOf<UiState>(UiState.Plane) }
    val argsState = rememberTextFieldState()
    var emptyHint by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    /**
     * Слушатель кнопки генерации документации по версии
     *
     * Запускает генерацию версии через [CmakeBuilder.GenerateVersion]
     * и обрабатывает результат [CmakeBuilder.GenerationResult]:
     * - [CmakeBuilder.GenerationResult.SUCCESS] - Переводит окно в состояние завершения
     * - [CmakeBuilder.GenerationResult.ERROR] - Ананлизирует ошибку
     *
     * [CmakeBuilder.GenerationResult.ERROR] обрабатывает ошибки:
     *  - [CmakeBuilder.ExitValue.ERROR] - Выводит код ошибки
     *  - [CmakeBuilder.ExitValue.NO_ARGS] - Запрашивает добавление аргументов
     *
     * Пример вызова:
     * ```kotlin
     * DefaultButton(onClick = { start() }, enabled = !busy) {
     *             Text("Press start")
     *         }
     * ```
     */
    fun start() {
        println("START CLICKED")
        state = UiState.Running
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    CmakeBuilder(project).GenerateVersion()
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            state = when (result) {
                null -> UiState.Error(MyMessageBundle.message("generation.error.message"))
                is CmakeBuilder.GenerationResult.SUCCESS -> UiState.Success
                is CmakeBuilder.GenerationResult.ERROR -> when (result.value) {
                    CmakeBuilder.ExitValue.NO_ARGS -> UiState.NeedArgs
                    CmakeBuilder.ExitValue.ERROR -> {
                        val code = if (result.code != 0) " (code ${result.code})" else ""
                        UiState.Error(MyMessageBundle.message("generation.error.message") + code)
                    }
                }
            }
        }
    }

    /**
     * Запрашивает и сохраняет аргументы для тестов.
     *
     * Пустые аргументы не принимаются, аргументы сохранятся
     * в файл `reserch/.args.txt`, а после запускается генерация версии.
     *
     * Пример вызова:
     * ```kotlin
     * DefaultButton(onClick = { saveArgs() }) { Text("Save args") }
     * ```
     */
    fun saveArgs() {
        val input = argsState.text.toString().trim()
        if (input.isBlank()) { emptyHint = true; return }
        emptyHint = false
        val base = project.basePath ?: return
        File(base, "reserch/.args.txt").apply {
            parentFile.mkdirs()
            writeText(input, Charsets.UTF_8)
        }
        argsState.clearText()
        start()
    }

    val errorColor = JewelTheme.globalColors.text.error
    val okColor = Color(0xFF62B543)
    val busy = state is UiState.Running

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DefaultButton(onClick = { start() }, enabled = !busy) {
            Text(MyMessageBundle.message("toolwindow.MyToolWindow.start.button"))
        }

        when (val s = state) {
            UiState.Plane -> {}
            UiState.Running -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator()
                Text(MyMessageBundle.message("generation.proccess.inProccess"))
            }
            UiState.NeedArgs -> {
                Text(MyMessageBundle.message("generation.noargs.message"), color = errorColor)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        state = argsState,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(MyMessageBundle.message("generation.noargs.need.input")) }
                    )
                    DefaultButton(onClick = { saveArgs() }) { Text(MyMessageBundle.message("generation.noargs.need.save")) }
                }
                if (emptyHint) {
                    Text(MyMessageBundle.message("generation.noargs.empty"), color = errorColor)
                }
            }
            is UiState.Error -> Text(s.text, color = errorColor)
            UiState.Success -> Text(MyMessageBundle.message("generation.proccess.update"), color = okColor)
        }
    }
}
