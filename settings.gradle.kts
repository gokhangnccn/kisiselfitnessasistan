pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven {  url = uri("https://jitpack.io")}
        jcenter(){
            content {
                includeModule("com.theartofdev.edmodo", "android-image-cropper")
            }
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven {  url = uri("https://jitpack.io")}
        jcenter(){
            content {
                includeModule("com.theartofdev.edmodo", "android-image-cropper")
            }
        }
    }
}


rootProject.name = "KFA"
include(":app")
