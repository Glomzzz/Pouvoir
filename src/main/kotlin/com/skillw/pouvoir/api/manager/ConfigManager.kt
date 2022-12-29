package com.skillw.pouvoir.api.manager

import com.skillw.pouvoir.api.map.BaseMap
import com.skillw.pouvoir.api.plugin.SubPouvoir
import com.skillw.pouvoir.util.FileUtils.loadYaml
import com.skillw.pouvoir.util.MessageUtils.warning
import com.skillw.pouvoir.util.Pair
import org.bukkit.configuration.file.YamlConfiguration
import taboolib.common.platform.function.getDataFolder
import taboolib.common5.FileWatcher
import taboolib.library.reflex.Reflex.Companion.getProperty
import taboolib.module.lang.Language
import java.io.File

/**
 * Config manager
 *
 * @constructor Create empty Config manager
 * @property subPouvoir
 */
abstract class ConfigManager(final override val subPouvoir: SubPouvoir) : Manager,
    BaseMap<String, YamlConfiguration>() {
    override val key = "ConfigManager"
    private val fileMap = BaseMap<File, YamlConfiguration>()
    private val watcher = FileWatcher()

    /** Server file */
    val serverFile: File by lazy {
        File(
            getDataFolder().parentFile.absolutePath.toString().replace("\\plugins", "")
        )
    }

    init {
        val map = HashMap<String, Pair<File, YamlConfiguration>>()
        //Init Map
        for (field in subPouvoir::class.java.fields) {
            if (!field.annotations.any { it.annotationClass.simpleName == "Config" }) continue
            val file = field.get(subPouvoir).getProperty<File>("file") ?: continue
            map[field.name] = Pair(file, file.loadYaml()!!)
        }
        //Register Config
        map.forEach {
            val key = it.key
            val pair = it.value
            val file = pair.key
            val yaml = pair.value
            fileMap.register(file, yaml)
            this.register(key, yaml)
        }
        for (it in fileMap.keys) {
            if (watcher.hasListener(it)) {
                watcher.removeListener(it)
            }
            watcher.addSimpleListener(it) {
                val yaml = fileMap[it]!!
                yaml.load(it)
                this[it.nameWithoutExtension] = yaml
            }
        }
    }

    override operator fun get(key: String): YamlConfiguration {
        val result = super.get(key) ?: kotlin.run {
            warning("The config $key dose not exist in the SubPouvoir ${subPouvoir.key}!")
            return YamlConfiguration.loadConfiguration(getDataFolder())
        }
        return result
    }

    /** Sub reload */
    protected open fun subReload() {}

    final override fun onReload() {
        Language.reload()
        subReload()
    }

    /**
     * Create if not exists
     *
     * @param name
     * @param fileNames
     */
    fun createIfNotExists(name: String, vararg fileNames: String) {
        val path = subPouvoir.plugin.dataFolder.path
        val dir = File("$path/$name")
        if (!dir.exists()) {
            dir.mkdir()
            for (fileName in fileNames) {
                try {
                    subPouvoir.plugin.saveResource("$name/$fileName", true)
                } catch (throwable: Throwable) {
                    throwable.printStackTrace()
                }
            }
        }
    }
}