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

val javaOperatorVersion = "1.8.4"

dependencies {
    implementation(enforcedPlatform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
	implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.javaoperatorsdk:operator-framework-spring-boot-starter:${javaOperatorVersion}")
    implementation("io.javaoperatorsdk:operator-framework:${javaOperatorVersion}")

    annotationProcessor("io.javaoperatorsdk:operator-framework:${javaOperatorVersion}")

    //https://github.com/fabric8io/kubernetes-client/tree/master/crd-generator
    compileOnly("io.fabric8:crd-generator-apt:5.6.0")

    testImplementation("io.javaoperatorsdk:operator-framework-spring-boot-starter-test:${javaOperatorVersion}")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks {
    test {
    	useJUnitPlatform()
    }
}
