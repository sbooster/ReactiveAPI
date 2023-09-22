plugins {
    id("java")
}

group = "dev.socialbooster.gradle"
version = "1.3.0-SNAPSHOT"

tasks.withType<PublishToMavenRepository>().configureEach {
    mustRunAfter(project(":reactiveapi").tasks.withType<PublishToMavenRepository>())
}

publishing {
    publications {
        create<MavenPublication>("jar") {
            from(components["java"])
        }
    }
}