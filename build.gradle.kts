// Top-level build file wo du Plugins definierst, die für alle Module gelten.

plugins {
    // Vorhandene Plugins (Beispiel)
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false

    // HIER fügst du das Sonar-Plugin im SELBEN Block hinzu:
    id("org.sonarqube") version "5.0.0.4638"
}

// Enable dependency locking for all projects
subprojects {
    dependencyLocking {
        lockAllConfigurations()
    }
}

// Der Sonar-Konfigurationsblock kommt ganz normal auf die oberste Ebene, NACH den Plugins
sonar {
    properties {
        property("sonar.projectKey", "AAU-SE2-Monopoly_aau-se2-frontend")
        property("sonar.organization", "aau-se2-monopoly")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}