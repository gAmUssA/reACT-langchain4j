plugins {
    kotlin("jvm") version "2.1.0"
    application
}

group = "dev.demo.react"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("dev.langchain4j:langchain4j:1.12.2")
    implementation("dev.langchain4j:langchain4j-anthropic:1.12.2")
    implementation("com.williamcallahan:tui4j:0.3.3")
    implementation("org.slf4j:slf4j-simple:2.0.16")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        javaParameters = true
    }
}

application {
    mainClass.set("dev.demo.react.MainKt")
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

tasks.register("printClasspath") {
    doLast {
        val cp = sourceSets["main"].runtimeClasspath.asPath
        print(cp)
    }
}
