plugins {
    java
}

java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
}

group = "me.nils"
version = "1.0"

repositories {
    mavenCentral()
}

val embed by configurations.creating
configurations.implementation.get().extendsFrom(embed)

dependencies {
    embed("org.ow2.asm:asm-tree:9.2")
    embed("org.ow2.asm:asm:9.2")
}

tasks.jar {
    from(embed.map { if(it.isDirectory) it else zipTree(it) }) {
        exclude("**/module-info.class")
    }

    archiveFileName.set("${project.name}.jar")

    manifest {
        attributes["Premain-Class"] = "me.onils.Agent"
        attributes["Can-Retransform-Classes"] = true
    }
}