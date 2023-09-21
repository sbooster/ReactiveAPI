plugins {
    id("java")
}

group = "dev.socialbooster.gradle"
version = "1.2.3-SNAPSHOT"

publishing {
    publications {
        create<MavenPublication>("jar") {
            from(components["java"])
        }
    }
}