/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.icerock.gradle.tasks

import dev.icerock.gradle.MRVisibility
import dev.icerock.gradle.configuration.getAndroidRClassPackage
import dev.icerock.gradle.generator.AssetsGenerator
import dev.icerock.gradle.generator.ColorsGenerator
import dev.icerock.gradle.generator.FilesGenerator
import dev.icerock.gradle.generator.FontsGenerator
import dev.icerock.gradle.generator.ImagesGenerator
import dev.icerock.gradle.generator.MRGenerator
import dev.icerock.gradle.generator.MRGenerator.Settings
import dev.icerock.gradle.generator.PluralsGenerator
import dev.icerock.gradle.generator.ResourceGeneratorFeature
import dev.icerock.gradle.generator.StringsGenerator
import dev.icerock.gradle.generator.StringsGenerator.Feature
import dev.icerock.gradle.generator.android.AndroidMRGenerator
import dev.icerock.gradle.generator.apple.AppleMRGenerator
import dev.icerock.gradle.generator.common.CommonMRGenerator
import dev.icerock.gradle.generator.js.JsMRGenerator
import dev.icerock.gradle.generator.jvm.JvmMRGenerator
import dev.icerock.gradle.utils.isStrictLineBreaks
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.konan.target.KonanTarget

@CacheableTask
abstract class GenerateMultiplatformResourcesTask : DefaultTask() {

    @get:InputFiles
    @get:Classpath
    abstract val ownResources: ConfigurableFileCollection

    @get:InputFiles
    @get:Classpath
    abstract val lowerResources: ConfigurableFileCollection

    @get:InputFiles
    @get:Classpath
    abstract val upperResources: ConfigurableFileCollection

    @get:Input
    abstract val platformType: Property<String>

    @get:Input
    abstract val konanTarget: Property<String>

    @get:Input
    abstract val resourcesPackageName: Property<String>

    @get:Input
    abstract val resourcesClassName: Property<String>

    @get:Input
    abstract val iosBaseLocalizationRegion: Property<String>

    @get:Input
    abstract val resourcesVisibility: Property<MRVisibility>

    @get:OutputFile
    abstract val generationReport: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    init {
        group = "moko-resources"
    }

    @TaskAction
    fun generate() {
        logger.warn("i $name have ownResources ${ownResources.from}")
        logger.warn("i $name have lowerResources ${lowerResources.from}")
        logger.warn("i $name have upperResources ${upperResources.from}")

        val settings: Settings = createGeneratorSettings()
        val features: List<ResourceGeneratorFeature<*>> = createGeneratorFeatures(settings)
        val generator: MRGenerator = resolveGenerator(settings, generators = features)
        logger.warn("i generator type: ${generator::class.java.simpleName}")
//        generator.generate()
    }

    private fun resolveGenerator(
        settings: MRGenerator.Settings,
        generators: List<ResourceGeneratorFeature<*>>
    ): MRGenerator {
        return when (KotlinPlatformType.valueOf(platformType.get())) {
            KotlinPlatformType.common -> createCommonGenerator(settings, generators)
            KotlinPlatformType.jvm -> createJvmGenerator()
            KotlinPlatformType.js -> createJsGenerator()
            KotlinPlatformType.androidJvm -> createAndroidJvmGenerator()
            KotlinPlatformType.native -> createNativeGenerator()
            KotlinPlatformType.wasm -> error("moko-resources not support wasm target now")
        }
    }

    private fun createGeneratorSettings(): MRGenerator.Settings {
        return MRGenerator.Settings(
            packageName = resourcesPackageName.get(),
            className = resourcesClassName.get(),
            ownResourcesFileTree = ownResources.asFileTree,
            lowerResourcesFileTree = lowerResources.asFileTree,
            upperResourcesFileTree = upperResources.asFileTree,
            generatedDir = outputDirectory.get(),
            isStrictLineBreaks = project.isStrictLineBreaks,
            visibility = resourcesVisibility.get(),
            androidRClassPackage = project.getAndroidRClassPackage().get(),
            iosLocalizationRegion = iosBaseLocalizationRegion.get(),
        )
    }

    private fun createGeneratorFeatures(
        settings: MRGenerator.Settings
    ): List<ResourceGeneratorFeature<*>> {
        return listOf(
            StringsGenerator.Feature(settings),
            PluralsGenerator.Feature(settings),
            ImagesGenerator.Feature(settings, logger),
            FontsGenerator.Feature(settings),
            FilesGenerator.Feature(settings),
            ColorsGenerator.Feature(settings),
            AssetsGenerator.Feature(settings)
        )
    }

    private fun createCommonGenerator(
        settings: MRGenerator.Settings,
        generators: List<ResourceGeneratorFeature<*>>
    ): CommonMRGenerator {
        return CommonMRGenerator(
            sourceSet =,
            settings = createGeneratorSettings(),
            generators = generators

        )
    }

    private fun createAndroidJvmGenerator(): AndroidMRGenerator {
        TODO()
    }

    private fun createJvmGenerator(): JvmMRGenerator {
        TODO()
    }

    private fun createJsGenerator(): JsMRGenerator {
        TODO()
    }

    private fun createNativeGenerator(): MRGenerator {
        val konanTargetName: String = konanTarget.get()
        val konanTarget: KonanTarget = KonanTarget.predefinedTargets[konanTargetName]
            ?: error("can't find $konanTargetName in KonanTarget")

        return when (konanTarget) {
            KonanTarget.IOS_ARM32,
            KonanTarget.IOS_ARM64,
            KonanTarget.IOS_SIMULATOR_ARM64,
            KonanTarget.IOS_X64,

            KonanTarget.MACOS_ARM64,
            KonanTarget.MACOS_X64,

            KonanTarget.TVOS_ARM64,
            KonanTarget.TVOS_SIMULATOR_ARM64,
            KonanTarget.TVOS_X64,

            KonanTarget.WATCHOS_ARM32,
            KonanTarget.WATCHOS_ARM64,
            KonanTarget.WATCHOS_DEVICE_ARM64,
            KonanTarget.WATCHOS_SIMULATOR_ARM64,
            KonanTarget.WATCHOS_X64,
            KonanTarget.WATCHOS_X86,
            -> createAppleGenerator()

            else -> error("$konanTarget is not supported by moko-resources now!")
        }
    }

    private fun createAppleGenerator(): AppleMRGenerator {
        TODO()
    }
}
