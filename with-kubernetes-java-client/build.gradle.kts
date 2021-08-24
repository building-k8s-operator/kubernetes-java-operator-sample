plugins {
    java
    id("org.springframework.boot") version "2.5.2"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
	mavenCentral()
}

val kubernetesJavaClientVersion = "13.0.0"
val awaitilityVersion = "4.1.0"

dependencies {
    implementation(enforcedPlatform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
	implementation(project(":adoption-center"))
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("io.kubernetes:client-java:${kubernetesJavaClientVersion}")
	implementation("io.kubernetes:client-java-spring-integration:${kubernetesJavaClientVersion}")
	implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
	implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.awaitility:awaitility:${awaitilityVersion}")
}

tasks {
    test {
    	useJUnitPlatform()
    }
}

sourceSets {
	main {
		java {
			srcDirs("src/generated/java")
		}
	}
}
