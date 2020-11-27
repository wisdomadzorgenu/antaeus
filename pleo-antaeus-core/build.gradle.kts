plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    implementation(project(":pleo-antaeus-data"))
    implementation("org.quartz-scheduler:quartz:2.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.2")
    api(project(":pleo-antaeus-models"))
}