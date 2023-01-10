plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
    application
}

// Makes generated code visible to IDE
kotlin.sourceSets.main {
    kotlin.srcDirs(
        file("$buildDir/generated/ksp/main/kotlin"),
    )
}

dependencies {
    implementation(project(":annotations"))
    ksp(project(":processor"))
}
