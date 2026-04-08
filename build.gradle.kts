subprojects {
	pluginManager.withPlugin("java") {
		extensions.configure(JavaPluginExtension::class.java) {
			when {
				path.startsWith(":generator-") -> {
					toolchain.languageVersion.set(JavaLanguageVersion.of(17))
					sourceCompatibility = JavaVersion.VERSION_17
					targetCompatibility = JavaVersion.VERSION_17
				}

				path.startsWith(":sample-library-") -> {
					toolchain.languageVersion.set(JavaLanguageVersion.of(25))
					sourceCompatibility = JavaVersion.VERSION_25
					targetCompatibility = JavaVersion.VERSION_25
				}
			}
		}
	}
}
