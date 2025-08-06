plugins {
    id("com.github.rodm.teamcity-server") version "1.5.2"
}

repositories {
    mavenCentral()
}

teamcity {
    version = "2023.11"
}

dependencies {
    implementation("org.jetbrains.teamcity:configs-dsl-kotlin:2023.11")
}

