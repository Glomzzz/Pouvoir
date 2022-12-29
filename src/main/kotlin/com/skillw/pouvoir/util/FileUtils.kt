package com.skillw.pouvoir.util

import com.skillw.pouvoir.Pouvoir
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.configuration.serialization.ConfigurationSerializable
import taboolib.common.platform.function.console
import taboolib.common.platform.function.getDataFolder
import taboolib.module.lang.sendLang
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*


object FileUtils {

    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun <T : ConfigurationSerializable> loadMultiply(mainFile: File, clazz: Class<T>): List<Pair<T, File>> {
        return mainFile.run {
            val list = LinkedList<Pair<T, File>>()
            for (file in listSubFiles().filter { it.extension == "yml" }) {
                val config = file.loadYaml() ?: continue
                for (key in config.getKeys(false)) {
                    try {
                        list.add(
                            Pair(
                                (clazz.getMethod(
                                    "deserialize",
                                    org.bukkit.configuration.ConfigurationSection::class.java
                                ).invoke(null, config[key]!!) as? T? ?: continue), file
                            )
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            list
        }
    }

    @JvmStatic
    fun ConfigurationSection.toMap(): Map<String, Any> {
        val newMap = HashMap<String, Any>()
        for (it in this.getKeys(false)) {
            val value = this[it]
            if (value is ConfigurationSection) {
                newMap[it] = value.toMap()
                continue
            }
            newMap[it] = value!!
        }
        return newMap
    }

    @JvmStatic
    fun File.loadYaml(): YamlConfiguration? {
        val config = YamlConfiguration()
        try {
            config.load(this)
        } catch (e: Throwable) {
            console().sendLang("wrong-config", name)
            console().sendLang("wrong-config-cause", ColorUtils.unColor(e.message ?: "null"))
            e.printStackTrace()
            return null
        }
        return config
    }

    @JvmStatic
    val serverFolder = getDataFolder().parentFile.parentFile

    @JvmStatic
    fun File.listSubFiles(): List<File> {
        val files: MutableList<File> = ArrayList()
        if (isDirectory) {
            listFiles()?.forEach { files.addAll(it.listSubFiles()) }
        } else {
            files.add(this)
        }
        return files
    }

    @JvmStatic
    fun File.loadYamls(): List<YamlConfiguration> {
        val yamls = LinkedList<YamlConfiguration>()
        for (subFile in listSubFiles()) {
            if (subFile.isFile && subFile.name.endsWith(".yml")) {
                yamls.add(subFile.loadYaml() ?: continue)
                continue
            }
            if (subFile.isDirectory)
                yamls.addAll(subFile.loadYamls())
        }
        return yamls

    }

    @JvmStatic
    fun File.pathNormalize(): String {
        return absolutePath.replace(Pouvoir.configManager.serverFile.absolutePath, "").replace("\\", "/")
    }


    @JvmStatic
    fun File.md5(): String? {
        val bi: BigInteger
        try {
            val buffer = ByteArray(8192)
            var len: Int
            val md = MessageDigest.getInstance("MD5")
            val fis = this.inputStream()
            while ((fis.read(buffer).also { len = it }) != -1) {
                md.update(buffer, 0, len)
            }
            fis.close()
            val b = md.digest()
            bi = BigInteger(1, b)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
        return bi.toString(16)
    }


}