plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    implementation(project(":pleo-antaeus-data"))
    implementation("org.quartz-scheduler:quartz:2.3.0")
    api(project(":pleo-antaeus-models"))
}