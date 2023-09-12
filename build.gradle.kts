plugins {
    id("java")
    id("java-gradle-plugin")
    id("maven-publish")
    id("io.github.gradle-nexus.publish-plugin").version("1.1.0")
}

group = "dev.socialbooster.gradle"
version = "1.3.0-SNAPSHOT"

val rootPackage = "${project.group}.${project.name.toLowerCase()}"

repositories {
    mavenCentral()
//    mavenLocal()
}

dependencies {
    implementation("com.google.guava:guava:31.1-jre")

    implementation("com.google.code.gson:gson:2.10")

    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")

    testImplementation("junit:junit:4.13.2")
}

gradlePlugin {
    val reactiveAPI by plugins.creating {
        id = rootPackage
        implementationClass = "${rootPackage}.ReactiveAPIPlugin"
    }
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

//For test. Publish to local maven repository
//publishing {
//    publications {
//        create<MavenPublication>("reactiveapi") {
//            from(components["java"])
//        }
//    }
//    repositories {
//        mavenLocal()
//    }
//}

nexusPublishing {
    repositories {
        create("myNexus") {
            nexusUrl.set(uri("https://repo.animecraft.fun/"))
            snapshotRepositoryUrl.set(uri("https://repo.animecraft.fun/repository/maven-snapshots/"))
            username.set(System.getenv("NEXUS_USERNAME"))
            password.set(System.getenv("NEXUS_PASSWORD"))
        }
    }
}
