plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("kapt") version "2.3.21"
    id("java")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
    id("io.github.goooler.shadow") version "8.1.8"
}

group = "tcc.gamers"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(uri("https://jitpack.io"))
    maven(uri("https://repo.skriptlang.org/releases"))
    maven("https://repo.extendedclip.com/releases/")
}

kotlin{
    jvmToolchain(25)
}


dependencies {
    val democracyLibVersion = "3f0213ee92"
    val democracyLib = "com.github.MCCitiesNetwork:DemocracyLib:$democracyLibVersion"
    implementation(democracyLib)
    paperweight.paperDevBundle("26.1.2.+")

    val skriptVersion = "2.15.2"

    compileOnly("me.clip:placeholderapi:2.12.2")

    compileOnly("com.github.SkriptLang:Skript:$skriptVersion")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    compileOnly("io.github.toxicity188:bettermodel-bukkit-api:3.0.1")

    implementation("io.github.alepandocr:spartan-internal:1.0.35:linux")
    implementation("io.github.alepandocr:spartan-api:1.0.35")

    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    kapt(democracyLib)
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        mergeServiceFiles()
    }

    build {
        dependsOn(shadowJar)
    }
}