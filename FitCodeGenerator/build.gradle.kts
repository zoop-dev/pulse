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
    mainClass = "nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen"
}

dependencies {
    implementation(libs.json)
}

tasks.register<JavaExec>("genFit") {
    inputs.dir("src")
    outputs.dir(project.rootProject.file("app/build/generated/sources/fit"))

    mainClass = application.mainClass
    args(project.file("src/main/resources/fit_profile.json").absolutePath)
    args(project.rootProject.file("app/build/generated/sources/fit/nodomain/freeyourgadget/gadgetbridge/service/devices/garmin/fit/").absolutePath)
    args(project.rootProject.file("app/src/main/java/nodomain/freeyourgadget/gadgetbridge/service/devices/garmin/fit/").absolutePath)
    classpath = sourceSets.main.get().runtimeClasspath
}