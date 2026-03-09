// Gatling load test build configuration.
//
// Gatling (https://gatling.io) is a load testing framework that simulates
// virtual users sending HTTP requests and produces HTML performance reports.
//
// The `scala` plugin compiles our Scala simulation files.
// The `io.gatling.gradle` plugin adds the `gatlingRun` task and manages
// Gatling + Scala library dependencies automatically.
//
// Run tests:  ./gradlew gatlingRun
// Reports:    build/reports/gatling/

plugins {
    scala
    id("io.gatling.gradle") version "3.15.0"
}

repositories {
    mavenCentral()
}
