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

dependencies {
    implementation(enforcedPlatform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
	implementation("org.springframework.boot:spring-boot-starter-actuator")

	implementation("io.kubernetes:client-java:${kubernetesJavaClientVersion}")
	implementation("io.kubernetes:client-java-spring-integration:${kubernetesJavaClientVersion}")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
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
