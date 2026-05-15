plugins {
    application
    java
}

java {
    toolchain {
        // Setting sourceCompatibility and targetCompatibility isn't required as this module ships
        // no complied code.
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "nodomain.freeyourgadget.gadgetbridge.daogen.GBDaoGenerator"
}

dependencies {
    // https://codeberg.org/Freeyourgadget/greenDao
    // As of 2025-06-19, this is bundled directly in the repository due to jitpack build issues.
    //implementation 'com.github.Freeyourgadget:greendao:1998d7cd2d21f662c6044f6ccf3b3a251bbad341'

    implementation(libs.freemarker)
}

sourceSets {
    main {
        java {
            srcDir("src")
        }
        resources {
            srcDir("src-template")
        }
    }
}

tasks.register<JavaExec>("genSources") {
    inputs.dir("src")
    outputs.dir(project.rootProject.file("app/build/generated/sources/gbdao"))

    mainClass = application.mainClass
    classpath = sourceSets.main.get().runtimeClasspath
    workingDir = file("../")
}