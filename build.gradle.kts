plugins {
    `java-library`
    `maven-publish`
    id("com.modrinth.minotaur") version "2.8.7"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenLocal()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") } // PaperMC
    maven { url = uri("https://oss.sonatype.org/content/groups/public/") }

    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://repo.maven.apache.org/maven2/") }
    maven { url = uri("https://repo.extendedclip.com/releases/") } // PlaceholderAPI
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:" + (property("paperVersion") as String) + "-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")

    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit")
        exclude(group = "org.spigotmc")
    }
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.bstats:bstats-bukkit:3.2.1")
    implementation("redis.clients:jedis:5.1.2")

    
    // Testing Dependencies
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.24.2")
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

tasks.processResources {
    filesMatching("paper-plugin.yml") {
        expand(project.properties)
    }
}

val versionString: String = "${version}"

tasks.named<ProcessResources>("processResources") {
    filesMatching("plugin.yml") {
        expand("projectVersion" to versionString)
    }
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    relocate("org.bstats", "com.makrozai.eligiusnametag.libs.bstats")
    relocate("redis.clients.jedis", "com.makrozai.eligiusnametag.libs.jedis")
    relocate("org.apache.commons.pool2", "com.makrozai.eligiusnametag.libs.commons.pool2")
    relocate("org.json", "com.makrozai.eligiusnametag.libs.json")
}


// --- Modrinth Publishing Configuration ---
modrinth {
    token.set(System.getenv("MODRINTH_API_TOKEN"))
    projectId.set("eligiusnametag") // You can use the slug here
    versionNumber.set(versionString)
    
    // Modrinth expects 'release', 'beta', or 'alpha'
    val channelEnv = System.getenv("CHANNEL") ?: "Release"
    versionType.set(channelEnv.lowercase())
    
    uploadFile.set(tasks.named("shadowJar"))
    gameVersions.addAll("1.21", "1.21.1", "1.21.3", "1.21.4")
    loaders.addAll("paper", "folia")
    syncBodyFrom.set(rootProject.file("MODRINTH.md").readText())
}