package com.skillw.pouvoir.internal.core.script.common

import com.skillw.pouvoir.Pouvoir
import com.skillw.pouvoir.Pouvoir.scriptManager
import com.skillw.pouvoir.api.able.Registrable
import com.skillw.pouvoir.api.event.ManagerTime
import com.skillw.pouvoir.api.manager.Manager.Companion.addExec
import com.skillw.pouvoir.api.map.BaseMap
import com.skillw.pouvoir.api.map.SingleExecMap
import com.skillw.pouvoir.api.script.annotation.ScriptAnnotationData
import com.skillw.pouvoir.api.script.engine.PouScriptEngine
import com.skillw.pouvoir.internal.core.script.javascript.PouJavaScriptEngine
import com.skillw.pouvoir.util.FileUtils.pathNormalize
import java.io.File
import javax.script.CompiledScript

// key = file.pathNormalize();
class PouCompiledScript(
    val file: File,
    val md5: String,
    private val scriptLines: List<String>,
    val script: CompiledScript,
    val pouEngine: PouScriptEngine,
) : Registrable<String> {

    override val key: String = file.pathNormalize()

    //function name to annotations
    val annotationData = BaseMap<String, Set<ScriptAnnotationData>>()

    private val fileAnnotations = HashSet<ScriptAnnotationData>()

    init {
        initAnnotation()
    }


    fun invoke(
        function: String = "main",
        arguments: Map<String, Any> = emptyMap(),
        vararg parameters: Any?,
    ): Any? {
        return PouJavaScriptEngine.bridge.invoke(script, function, arguments, *parameters)
    }

    private var lastHeadIndex = 0

    private fun initAnnotation() {
        for (index in scriptLines.indices) {
            val str = scriptLines[index]
            val matcher = pouEngine.functionPattern.matcher(str)
            if (!matcher.find()) continue
            val function = matcher.group("name")
            val annotations = getAnnotations(index, function)
            annotationData[function] = annotations
        }

        fileAnnotations.addAll(
            getAnnotations(
                1,
                "null"
            ).filter { Pouvoir.scriptAnnotationManager[it.annotation]?.fileAnnotation == true })
    }


    private fun getAnnotations(
        index: Int,
        function: String,
    ): Set<ScriptAnnotationData> {
        val annotations = HashSet<ScriptAnnotationData>()
        var lastIndex = index
        while (lastIndex >= 1) {
            val last = scriptLines[--lastIndex]
            val matcher = pouEngine.getAnnotationPattern().matcher(last)
            if (!matcher.find()) {
                if (lastHeadIndex == 0)
                    lastHeadIndex = lastIndex
                break
            }
            annotations.add(
                ScriptAnnotationData(
                    matcher.group("key"),
                    this,
                    function,
                    matcher.group("args")
                )
            )
        }
        return annotations
    }

    private val execs = SingleExecMap()
    fun onDeleted(key: String, exec: (() -> Unit)) {
        execs += key to exec
    }


    private var deleted: Boolean = false

    fun delete() {
        deleted = true
        execs.forEach { it.value.invoke() }
    }

    private fun Set<ScriptAnnotationData>.process() {
        data@ for (data in this) {
            val annotation = Pouvoir.scriptAnnotationManager[data.annotation] ?: continue@data
            if (annotation.awakeWhenEnable && !Pouvoir.plugin.isEnabled) {
                scriptManager.addExec(
                    data.script.key + "::" + data.function + "@" + data.annotation,
                    ManagerTime.ENABLE
                ) {
                    annotation.handle(data)
                }
                continue@data
            }
            annotation.handle(data)
        }
    }

    override fun register() {
        scriptManager.remove(key)?.delete()
        fileAnnotations.process()
        annotationData.values.forEach { it.process() }
        scriptManager[key] = this
    }
}