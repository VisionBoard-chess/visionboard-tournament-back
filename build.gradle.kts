import com.google.protobuf.gradle.*
val exposed_version: String by project
val h2_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val postgres_version: String by project

plugins {
    kotlin("jvm") version "2.3.0"
    id("io.ktor.plugin") version "3.4.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.0"

    id("com.google.protobuf") version "0.9.4"
    jacoco
}

group = "com.example"
version = "0.0.1"

application {
    mainClass.set("com.example.ApplicationKt")
}

kotlin {
    jvmToolchain(21)
}

dependencies {

    implementation("com.google.firebase:firebase-admin:9.2.0")
    implementation("io.ktor:ktor-server-auth:$kotlin_version")

    implementation("io.grpc:grpc-netty-shaded:1.63.0")
    implementation("io.grpc:grpc-protobuf:1.63.0")
    implementation("io.grpc:grpc-stub:1.63.0")
    implementation("io.grpc:grpc-kotlin-stub:1.4.1")
    implementation("com.google.protobuf:protobuf-kotlin:3.25.3")

    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-cio")
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-host-common")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("org.postgresql:postgresql:$postgres_version")
    //implementation("com.h2database:h2:$h2_version")
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("io.ktor:ktor-server-cors")
    implementation("io.ktor:ktor-server-netty")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-config-yaml")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposed_version")

    implementation("io.ktor:ktor-server-websockets")
    implementation("io.ktor:ktor-server-sse")
    implementation("io.ktor:ktor-serialization-gson:3.4.0")

    testImplementation("io.ktor:ktor-server-test-host:$kotlin_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("io.ktor:ktor-server-content-negotiation:$kotlin_version")
    testImplementation("io.ktor:ktor-serialization-kotlinx-json:$kotlin_version")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("io.mockk:mockk-agent:1.13.12")

}

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:3.25.3" }
    plugins {
        id("grpc") { artifact = "io.grpc:protoc-gen-grpc-java:1.63.0" }
        id("grpckt") { artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.1:jdk8@jar" }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc")
                id("grpckt")
            }
            it.builtins { id("kotlin") }
        }
    }
}

tasks.named("shadowJar") {
    doFirst {
        javaClass.getMethod("mergeServiceFiles").invoke(this)
    }
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(false)
    }

    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                include(
                    "**/services/GameService*",
                    "**/services/RoundService*",
                    "**/services/TournamentService*",
                    "**/routes/**",
                )
                exclude(
                    "**/auth/**",
                    "**/client/**",
                    "**/database/**",
                    "**/models/**",
                    "**/tables/**",
                    "**/services/LichessService*",
                    "**/services/UserService*",
                    "**/Application*",
                    "**/Databases*",
                    "**/HTTP*",
                    "**/Routing*",
                    "**/Serialization*",
                )
            }
        })
    )
}
