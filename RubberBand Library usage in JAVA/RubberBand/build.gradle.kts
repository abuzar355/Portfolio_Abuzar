plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.psambit9791:wavfile:0.1")
    implementation("com.github.psambit9791:wavfile:0.1")
    implementation("com.github.psambit9791:wavfile:0.1")
    implementation("com.github.psambit9791:wavfile:0.1")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

tasks.test {
    useJUnitPlatform()
}}