import org.gradle.internal.os.OperatingSystem

plugins {
    id("java")
    id("application")
}

application {
    mainClass.set("org.almostcraft.AlmostCraftApplication")
}

tasks.named<JavaExec>("run") {
    if (OperatingSystem.current().isMacOsX) {
        jvmArgs("-XstartOnFirstThread")
    }
}

tasks.processResources {
    filesMatching("application.properties") {
        expand("version" to project.version)
    }
}

group = "org.example"
version = "1.0-SNAPSHOT"

val lwjglVersion = "3.3.6"
val jomlVersion = "1.10.8"
val lombokVersion = "1.18.42"

val lwjglNatives = Pair(
    System.getProperty("os.name")!!,
    System.getProperty("os.arch")!!
).let { (name, arch) ->
    when {
        arrayOf("Linux", "SunOS", "Unit").any { name.startsWith(it) } ->
            if (arrayOf("arm", "aarch64").any { arch.startsWith(it) })
                "natives-linux${if (arch.contains("64") || arch.startsWith("armv8")) "-arm64" else "-arm32"}"
            else if (arch.startsWith("ppc"))
                "natives-linux-ppc64le"
            else if (arch.startsWith("riscv"))
                "natives-linux-riscv64"
            else
                "natives-linux"
        arrayOf("Mac OS X", "Darwin").any { name.startsWith(it) } ->
            "natives-macos${if (arch.startsWith("aarch64")) "-arm64" else ""}"
        arrayOf("Windows").any { name.startsWith(it) } ->
            "natives-windows"
        else ->
            throw Error("Unrecognized or unsupported platform. Please set \"lwjglNatives\" manually")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

    implementation("org.lwjgl", "lwjgl")
    implementation("org.lwjgl", "lwjgl-assimp")
    implementation("org.lwjgl", "lwjgl-glfw")
    implementation("org.lwjgl", "lwjgl-openal")
    implementation("org.lwjgl", "lwjgl-opengl")
    implementation("org.lwjgl", "lwjgl-stb")
    runtimeOnly("org.lwjgl", "lwjgl", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-assimp", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-glfw", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-openal", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-opengl", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-stb", classifier = lwjglNatives)
    implementation("org.joml", "joml", jomlVersion)

    // Lombok
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")
    testCompileOnly("org.projectlombok:lombok:$lombokVersion")
    testAnnotationProcessor("org.projectlombok:lombok:$lombokVersion")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("ch.qos.logback:logback-classic:1.5.21")

    // JUnit 5
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.4") // Pour les tests paramétrés
}

// ==================== Configuration des tests ====================

tasks.test {
    useJUnitPlatform()

    // Afficher plus d'infos pendant les tests
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false // Mettre à true pour voir les System.out/err
    }

    // Optionnel : Exécuter les tests en parallèle
    maxParallelForks = Runtime.getRuntime().availableProcessors() / 2
}

// ==================== Tasks personnalisées pour les tests ====================

// Task pour exécuter tous les tests avec logs détaillés
tasks.register<Test>("testVerbose") {
    group = "verification"
    description = "Runs all tests with detailed output"

    useJUnitPlatform()

    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = true
    }
}

// Task pour exécuter seulement les tests rapides (unitaires)
tasks.register<Test>("testUnit") {
    group = "verification"
    description = "Runs only unit tests"

    useJUnitPlatform {
        includeTags("unit")
    }

    testLogging {
        events("passed", "skipped", "failed")
    }
}

// Task pour exécuter seulement les tests d'intégration
tasks.register<Test>("testIntegration") {
    group = "verification"
    description = "Runs only integration tests"

    useJUnitPlatform {
        includeTags("integration")
    }

    testLogging {
        events("passed", "skipped", "failed")
    }
}

// Task pour générer un rapport HTML des tests
tasks.test {
    finalizedBy(tasks.named("testReport"))
}

tasks.register("testReport") {
    group = "verification"
    description = "Generates test report"

    doLast {
        println("Test report available at: build/reports/tests/test/index.html")
    }
}