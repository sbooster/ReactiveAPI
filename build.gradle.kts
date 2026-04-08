plugins {
    id("java")
    id("java-gradle-plugin")
    id("maven-publish")
}

subprojects {
    apply(plugin = "maven-publish")
}

group = "dev.socialbooster.gradle"
version = "1.5.2"

val rootPackage = "${project.group}.${project.name.toLowerCase()}"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":reactiveapi-annotations"))
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

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
            artifactId = "reactiveapi"
        }
    }
    repositories {
        maven {
            name = "BillmarsSoft"
            url = uri("https://repo.billmarssoft.com/releases/")
            credentials {
                username = System.getenv("REPOSITORY_USERNAME")
                password = System.getenv("REPOSITORY_PASSWORD")
            }
        }
    }
}
