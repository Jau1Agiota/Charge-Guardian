plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
android {
 namespace = "com.chargeguardian.app"
 compileSdk = 36
 buildToolsVersion = "36.0.0"
 defaultConfig { applicationId = "com.chargeguardian.app"; minSdk = 26; targetSdk = 36; versionCode = 1; versionName = "1.0.0" }
 buildFeatures { compose = true }
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
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
