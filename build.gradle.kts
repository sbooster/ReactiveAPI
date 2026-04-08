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

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
tasks {
    register<Jar>("sourcesJar") {
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }
    register<Jar>("javadocJar") {
        archiveClassifier.set("javadoc")
        from(javadoc)
    }
    javadoc {
        options.encoding = "UTF-8"
        options.memberLevel = JavadocMemberLevel.PUBLIC
        isFailOnError = false
    }
    withType<JavaCompile> {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
    }
    build {
        dependsOn("sourcesJar", "javadocJar")
    }
    jar {
        enabled = true
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
