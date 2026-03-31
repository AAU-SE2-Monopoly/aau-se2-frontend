// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false

    // SonarCloud Plugin hinzugefügt
    id("org.sonarqube") version "5.0.0.4638"
}

// SonarCloud Konfiguration an das Ende der Datei anhängen
sonar {
    properties {
        property("sonar.projectKey", "AAU-SE2-Monopoly_aau-se2-frontend")
        property("sonar.organization", "aau-se2-monopoly")
        property("sonar.host.url", "https://sonarcloud.io")

    }
}