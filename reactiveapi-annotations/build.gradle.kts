plugins {
    id("java")
}

group = "dev.socialbooster.gradle"
version = "1.3.0-SNAPSHOT"

publishing {
    publications {
        create<MavenPublication>("jar") {
            from(components["java"])
        }
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
            artifactId = "reactiveapi-annotations"
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