
package com.example.v2raytoclash

import java.io.File

object RootBridge {
    data class Result(val ok:Boolean,val output:String)

    private const val TARGET="/data/data/com.boxproxy.box/files/box/mihomo"

    fun check(): Result = run("su -c id")

    fun copy(yamlFile: File): Result {
        if(!yamlFile.exists()) return Result(false,"الملف غير موجود")
        val escaped = yamlFile.absolutePath.replace("'","'\\''")
        val name = yamlFile.name.replace("'","'\\''")
        val cmd = "mkdir -p '$TARGET' && cat '$escaped' > '$TARGET/$name' && chmod 777 '$TARGET/$name'"
        val first = run("su -mm -c \"$cmd\"")
        if(first.ok) return Result(true,"تم النقل عبر su -mm\n$TARGET/$name")
        val second = run("su -c \"nsenter -t 1 -m -- sh -c '$cmd'\"")
        return if(second.ok) Result(true,"تم النقل عبر nsenter\n$TARGET/$name")
        else Result(false,"فشل النقل عبر su -mm و nsenter\n${second.output}")
    }

    private fun run(command:String):Result {
        return try {
            val p=Runtime.getRuntime().exec(arrayOf("sh","-c",command))
            val out=p.inputStream.bufferedReader().readText()
            val err=p.errorStream.bufferedReader().readText()
            val code=p.waitFor()
            Result(code==0,(out+"\n"+err).trim())
        } catch(e:Exception) { Result(false,e.toString()) }
    }
}
