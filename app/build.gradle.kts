plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
android {
 namespace = "com.chargeguardian.app"
 compileSdk = 36
 buildToolsVersion = "36.0.0"
 defaultConfig { applicationId = "com.chargeguardian.app"; minSdk = 26; targetSdk = 36; versionCode = 2; versionName = "1.1.0" }
 buildFeatures { compose = true; buildConfig = true }
 testOptions { unitTests.isIncludeAndroidResources = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }

}
dependencies {
 implementation(platform("androidx.compose:compose-bom:2025.12.00"))
 implementation("androidx.activity:activity-compose:1.12.2")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
 implementation("androidx.datastore:datastore-preferences:1.2.0")
 implementation("androidx.core:core-ktx:1.17.0")
 testImplementation("junit:junit:4.13.2")
 testImplementation("org.robolectric:robolectric:4.16.1")
 testImplementation("androidx.compose.ui:ui-test-junit4")
 debugImplementation("androidx.compose.ui:ui-test-manifest")
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

// Robolectric resolves Android framework jars separately from Gradle dependencies.
tasks.withType<Test>().configureEach {
 val testHome = layout.buildDirectory.dir("test-home").get().asFile
 systemProperty("user.home", testHome.absolutePath)
 doFirst { testHome.mkdirs() }
 systemProperty("robolectric.dependency.repo.url", "https://repo.maven.apache.org/maven2")
 listOf("https.proxyHost", "https.proxyPort", "http.proxyHost", "http.proxyPort", "javax.net.ssl.trustStore").forEach { key ->
  System.getProperty(key)?.let { systemProperty(key, it) }
 }
}
