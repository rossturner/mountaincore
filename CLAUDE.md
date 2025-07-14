# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Mountaincore is an open-source Java game developed by Rocket Jump Technology using LibGDX framework. It's a settlement management game where players build and manage dwarf settlements.

## Build System & Commands

**Java Version:** Requires Java 17 (enforced by build script)

**Main Build Commands:**
- `./gradlew build` - Build the entire project
- `./gradlew test` - Run tests using JUnit platform
- `./gradlew clean` - Clean build artifacts

**Running the Game:**
- Main launcher: `DesktopLauncher` class (run with `./core` as working directory)
- Asset editor: `AssetEditorLauncher` class
- Texture packer: `RunTexturePacker` class (packages assets from `./mods/base` to `./assets`)

**Release Tasks:**
- `./gradlew packr` - Package for all platforms (Windows, Mac, Linux)
- `./gradlew zipAll` - Create distribution archives
- `./gradlew release` - Full release including Butler uploads

## Project Structure

**Multi-module Gradle project:**
- `core/` - Main game code and assets
- `desktop/` - Desktop launcher and platform-specific code

**Core Architecture:**
- **Main package:** `technology.rocketjump.mountaincore`
- **Entry point:** `MountaincoreApplicationAdapter` (LibGDX ApplicationAdapter)
- **Dependency injection:** Google Guice via `MountaincoreGuiceModule`
- **Messaging:** Custom message dispatcher system for game events
- **Game state:** Managed through `GameContext` and associated registries

**Key Systems:**
- **Entities:** Component-based entity system with `Entity`, `EntityComponent`, and behavior components
- **Assets:** Custom asset management through `TextureAtlasRepository` and asset dictionaries
- **Rendering:** Multi-layered rendering system with terrain, entities, and UI layers
- **Game loop:** Update/render cycle with `Updatable` components registered through `GameUpdateRegister`
- **Modding:** Extensive mod support via `LocalModRepository` and mod parsing systems
- **Persistence:** JSON-based save system with migration support

**Important Directories:**
- `core/assets/` - Game assets (packed from mod sources)
- `core/mods/base/` - Source game content (entities, definitions, etc.)
- `core/src/main/java/technology/rocketjump/mountaincore/` - Main source code
- `release_tools/` - Packaging configuration and tools

## Testing

- Uses JUnit 5 (Jupiter) with AssertJ and Mockito
- Test runner: `useJUnitPlatform()`
- Run tests: `./gradlew test`

## Working Directory Requirements

**Critical:** The main game classes (`DesktopLauncher`, `RunTexturePacker`) must be run with `./core` as the working directory, as they expect the `assets` directory to be in the current working directory.

## Asset Pipeline

1. Source assets are in `./mods/base/`
2. `RunTexturePacker` processes them into `./assets/`
3. Assets are already processed in the repository - only re-run if source assets change

## Libraries & Dependencies

- **LibGDX** - Primary game framework
- **Google Guice** - Dependency injection
- **Jackson/FastJSON** - JSON processing
- **Commons libraries** - Various utilities
- **TinyLog** - Logging
- **Reflections** - Runtime reflection utilities
- **Steam4J** - Steam integration