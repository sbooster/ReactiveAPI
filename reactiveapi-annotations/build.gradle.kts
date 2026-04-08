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