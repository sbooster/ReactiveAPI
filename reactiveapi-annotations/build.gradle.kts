plugins {
    id("java")
}

group = "dev.socialbooster.gradle"
version = "1.2.3-SNAPSHOT"

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