plugins {
    id("proxyai.java-conventions")
}

dependencies {
    implementation(libs.analytics)
    implementation(libs.gson)
    compileOnly(libs.jackson.databind)
}
