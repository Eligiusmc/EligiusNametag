plugins {
    `java-library`
    `maven-publish`
}

repositories {
    mavenLocal()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") } // PaperMC
    maven { url = uri("https://oss.sonatype.org/content/groups/public/") }
    maven { url = uri("https://repo.dmulloy2.net/repository/public/") } // ProtocolLib
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://repo.maven.apache.org/maven2/") }
    maven { url = uri("https://repo.extendedclip.com/releases/") } // PlaceholderAPI
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:" + (property("paperVersion") as String) + "-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.3.0")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit")
        exclude(group = "org.spigotmc")
    }
    
    // Testing Dependencies
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("com.github.seeseemelk:MockBukkit-v1.21:3.93.1")
}

group = "com.makrozai"
version = (property("pluginVersion") as String)
description = "EligiusNametag"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

// --- Encoding setup for Java and Javadoc ---
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}

// --- Resources folder handling ---
val folderToDelete = project.file("src/main/resources/#defaults")
val sourceFolder = project.file("src/main/resources")
val destinationFolder = project.file("src/main/resources/#defaults")

val deleteFolder by tasks.registering(Delete::class) {
    delete(folderToDelete)
}

val copyContents by tasks.registering(Copy::class) {
    dependsOn(deleteFolder)
    doFirst {
        destinationFolder.mkdirs()
    }
    from(sourceFolder) {
        exclude("#defaults/**")
        exclude("plugin.yml")
    }
    into(destinationFolder)
}

tasks.named("processResources") {
    dependsOn(copyContents)
}

tasks.processResources {
    filesMatching("paper-plugin.yml") {
        expand(project.properties)
    }
}

tasks.named("build") {
    dependsOn(copyContents)
}

val versionString: String = "${version}"

tasks.named<ProcessResources>("processResources") {
    filesMatching("plugin.yml") {
        expand("projectVersion" to versionString)
    }
}