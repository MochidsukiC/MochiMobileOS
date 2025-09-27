# 🔧 MochiMobileOS Forge Module

Minecraft Forge 1.20.1 integration for MochiMobileOS, enabling smartphone-like OS functionality within Minecraft.

## 🚀 Features

- **Complete OS Environment**: Smartphone-like interface within Minecraft
- **MOD Application System**: Other MODs can register apps via `PhoneAppRegistryEvent`
- **App Store Integration**: Install and manage MOD applications
- **Multi-Page Launcher**: iOS/Android-style home screen with drag & drop
- **System Services**: Virtual File System, Settings Manager, Layout Manager
- **Security System**: Pattern lock with gesture-based authentication
- **Control Center**: System toggles and quick settings
- **Notification Center**: System messages and notifications

## 📋 Requirements

- **Minecraft**: 1.20.1
- **Minecraft Forge**: 47.2.0+
- **Java**: 17+
- **Gradle**: 7.6+ (included via wrapper)

## 🛠️ Building

### Quick Build
```bash
# Windows
.\build_forge_mod.bat

# Linux/Mac
./gradlew build
```

### Development Build
```bash
# Clean and build
./gradlew clean build

# Run in development environment
./gradlew runClient

# Generate IDE project files
./gradlew genEclipseRuns  # For Eclipse
./gradlew genIntellijRuns # For IntelliJ
```

## 🎮 Running for Development

### Client Testing
```bash
# Windows
.\run_forge_client.bat

# Linux/Mac
./gradlew runClient
```

### Server Testing
```bash
./gradlew runServer
```

## 📦 Installation

1. **Build the MOD**:
   ```bash
   ./gradlew build
   ```

2. **Locate the JAR**:
   - Find `build/libs/mochimobileos-*.jar`

3. **Install in Minecraft**:
   - Copy JAR to `%APPDATA%\.minecraft\mods\` (Windows)
   - Or `~/.minecraft/mods/` (Linux/Mac)

4. **Launch Minecraft**:
   - Use Minecraft Forge 1.20.1 profile
   - MochiMobileOS will initialize automatically

## 🔌 MOD Integration API

Other MOD developers can integrate with MochiMobileOS:

### Registering an Application

```java
@Mod("your_mod_id")
public class YourMod {

    @SubscribeEvent
    public void onPhoneAppRegistry(PhoneAppRegistryEvent event) {
        // Create your application
        IApplication yourApp = new YourCustomApp();

        // Register with MochiMobileOS
        event.registerApp(yourApp);

        System.out.println("Registered app: " + yourApp.getApplicationName());
    }
}
```

### Implementing IApplication

```java
public class YourCustomApp implements IApplication {

    @Override
    public String getName() {
        return "Your App Name";
    }

    @Override
    public String getApplicationId() {
        return "your.mod.id.app";
    }

    @Override
    public PImage getIcon(PApplet p) {
        // Return your app icon
        return createIcon(p);
    }

    @Override
    public Screen getEntryScreen(Kernel kernel) {
        // Return your main screen
        return new YourAppScreen(kernel);
    }

    @Override
    public void onInstall(Kernel kernel) {
        // Called when user installs from AppStore
        System.out.println("Installing " + getName());
        // Setup app data, create directories, etc.
    }
}
```

## 📁 Project Structure

```
forge/
├── src/main/java/
│   └── jp/moyashi/phoneos/forge/
│       ├── MochiMobileOSMod.java         # Main MOD class
│       └── event/
│           ├── PhoneAppRegistryEvent.java # App registration event
│           └── ModAppRegistry.java       # App registry manager
├── src/main/resources/
│   ├── META-INF/
│   │   └── mods.toml                     # MOD metadata
│   └── pack.mcmeta                       # Resource pack metadata
├── build.gradle.kts                      # Build configuration
├── gradle.properties                     # Build properties
├── run_forge_client.bat                  # Quick test launcher (Windows)
├── build_forge_mod.bat                   # Quick build script (Windows)
└── README.md                             # This file
```

## ⚙️ Configuration

Edit `gradle.properties` to customize:

```properties
# Minecraft/Forge versions
minecraft_version=1.20.1
forge_version=47.2.0
mappings_channel=parchment
mappings_version=2023.06.26-1.20.1

# MOD information
mod_id=mochimobileos
mod_name=MochiMobileOS
mod_version=1.1-SNAPSHOT
mod_group_id=jp.moyashi.phoneos

# Dependencies
processing_version=4.4.4
gson_version=2.10.1
```

## 🐛 Troubleshooting

### Build Issues
- **Java Version**: Ensure Java 17+ is installed
- **Memory**: Increase Gradle memory if needed: `org.gradle.jvmargs=-Xmx4G`
- **Dependencies**: Run `./gradlew --refresh-dependencies`

### Runtime Issues
- **ClassNotFoundException**: Check that core module is properly included
- **Processing Issues**: Verify JOGL libraries are loaded correctly
- **MOD Registration**: Check that `PhoneAppRegistryEvent` is fired during `FMLCommonSetupEvent`

### Development Setup
- **IDE Integration**: Run `./gradlew genIntellijRuns` for IntelliJ
- **Debugging**: Use `./gradlew runClient --debug-jvm` for debugger attachment
- **Hot Reload**: Enable Forge development mode for faster iteration

## 📚 Additional Resources

- [Minecraft Forge Documentation](https://docs.minecraftforge.net/)
- [ForgeGradle Documentation](https://forgegradle.readthedocs.io/)
- [Processing Framework](https://processing.org/)
- [MochiMobileOS Core Documentation](../core/README.md)

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch
3. Make changes to the forge module
4. Test with `./gradlew runClient`
5. Build with `./gradlew build`
6. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](../LICENSE) file for details.