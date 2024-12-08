plugins {
    java
}

group = "com.falsepattern"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")

    implementation("commons-io:commons-io:2.18.0")
    implementation("org.luaj:luaj-jse:3.0.1")
}

