/*
 * Copyright (c) 2020 Proton Technologies AG
 * 
 * This file is part of ProtonMail.
 * 
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail. If not, see https://www.gnu.org/licenses/.
 */
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.kotlin.dsl.exclude

/**
 * Dependency object for a library
 * @author Davide Farella
 */
interface Dependency {

    /** Version of the dependency */
    val version: String

    /** @return [List] of all the [DepGroup] for this [Dependency] */
    fun allGroups(): List<DepGroup>

    /** @return [List] of all the [DepModule] for this [Dependency] */
    fun allModules() = allGroups().flatMap { it.all() }


    // region Constructor functions
    /** @return [DepModule] created with the receiver [DepGroup] as parent */
    fun DepGroup.module(module: String) = object : DepModule(this, module, version) {}

    /** @return [ProjectPlugin] */
    fun DepGroup.projectPlugin(projectPluginName: String) = Plugin.pPlugin("$group:$projectPluginName", version)

    /** @return [ModulePlugin] */
    fun DepGroup.modulePlugin(modulePluginId: String) = Plugin.mPlugin(modulePluginId)

    /** @return [GradlePlugin] */
    fun DepGroup.gradlePlugin(projectPluginName: String, modulePluginId: String) =
            Plugin.gPlugin("$group:$projectPluginName", version, modulePluginId)
    // endregion
}

/** Group of a [Dependency] */
abstract class DepGroup(val group: String) {

    /** @return [List] of all the [DepModule] for this [DepGroup] */
    abstract fun all(): List<DepModule>

    /** Default implementation of [Dependency.allGroups] */
    open fun allGroups(): List<DepGroup> = listOf(this)

    /** @return name of the group. [group] */
    override fun toString() = group
}

/** Module of a [DepGroup] */
abstract class DepModule(val group: DepGroup, val module: String, val version: String) {

    /** @return path of the module. [group]:[module] */
    override fun toString() = "$group:$module"
}


// region Extensions
/** @return [String] of the full dependency path */
internal operator fun DepModule.invoke() = "$group:$module:$version"

/** @return [List] of [DepModule] created by the receiver and the param */
internal operator fun DepModule.plus(other: DepModule) = listOf(this, other)

/** @return [List] of [DepGroup] created by the receiver and the param */
internal operator fun DepGroup.plus(other: DepGroup) = listOf(this, other)
// endregion

// region exclude utils
fun ModuleDependency.exclude(vararg any: Any) {
    any.forEach {
        when(it) {
            is DepGroup -> exclude(it)
            is DepModule -> exclude(it)
            is List<*> -> it.forEach { e -> exclude(e!!) }
            else -> throw IllegalArgumentException(it.toString())
        }
    }
}
fun ModuleDependency.exclude(vararg groups: DepGroup) {
    groups.forEach(::exclude)
}
fun ModuleDependency.exclude(group: DepGroup) {
    group.all().forEach(::exclude)
}
fun ModuleDependency.exclude(vararg modules: DepModule) {
    modules.forEach(::exclude)
}
fun ModuleDependency.exclude(module: DepModule) {
    exclude(module.group.group, module.module)
}
// endregion
