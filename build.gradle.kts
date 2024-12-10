plugins {
    java
}

group = "com.falsepattern"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val JAVA_VERSION = JavaVersion.VERSION_21

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(JAVA_VERSION.majorVersion)
        vendor = JvmVendorSpec.ADOPTIUM
    }
}

tasks.compileJava {
    sourceCompatibility = JAVA_VERSION.majorVersion
    targetCompatibility = JAVA_VERSION.majorVersion
    javaCompiler = javaToolchains.compilerFor(java.toolchain)
}

tasks.register<JavaExec>("decompile") {
    group = "application"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "com.falsepattern.sasqpatch.Main"
}

tasks.register<JavaExec>("recompile") {
    group = "application"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "com.falsepattern.sasqpatch.Main"
    args = listOf("recompile")
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")

    implementation("commons-io:commons-io:2.18.0")
    implementation("org.luaj:luaj-jse:2.0.3")
}

