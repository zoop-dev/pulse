plugins {
    application
    java
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "org.gadgetbridge.kaitai_aux.main.KaitaiAuxGen"
}

dependencies {
    implementation(libs.snakeyaml)
}

tasks.register<JavaExec>("genKaitaiAux") {
    description = "Generate auxiliary source files from Kaitai structs"
    val ksyDir = project.rootProject.file("app/src/main/ksy")
    val outputDir = project.rootProject.file("app/build/generated/sources/kaitaiaux")

    inputs.dir(ksyDir)
    outputs.dir(outputDir)

    mainClass = application.mainClass
    args(ksyDir.absolutePath)
    args(outputDir.absolutePath)
    classpath = sourceSets.main.get().runtimeClasspath
}
