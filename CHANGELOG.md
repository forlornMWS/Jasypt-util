# Jasypt Tool Plugin Changelog

## [Unreleased]

## [1.1.0] - 2025-05-15

### Fixed
- fix: improve algorithm conflict check to be case-insensitive

## [1.0.9] - 2025-05-03

### Added
- Added support for reading password and algorithm from YAML and properties files for intention action
- handle credential conflicts between saved settings and config file


## [1.0.8] - 2025-04-26

### Added
- Added internationalization (i18n) support
- Added support for file intention detection

### Fixed
- Fixed known issues


## [1.0.7] - 2025-04-23

### Added
- Added support for processing properties files

### Fixed

- Fixed some known bugs


## [1.0.6] - 2025-04-20

### Added
- Added Popup menu for quick select yaml files to encrypt/decrypt

### Fixed

- Fixed some known bugs  

### Changed

- Updated the plugin to support the latest version of IntelliJ IDEA


## [1.0.5] - 2025-01-08

### Fixed

- Fixed the issue where only files under the project path could be selected

### Changed

- Changed the file chooser to use the project resources path as the default path


## [1.0.4] - 2025-01-07

### Added

- Added a button to select files or directories for YAML processing
- Integrated IntelliJ IDEA's file chooser for selecting files or directories

### Changed

- Reorganized buttons in the UI for better usability
- Renamed buttons to more appropriate names

## [1.0.3] - 2024-12-26

### Added

- Added automatic encryption/decryption for YAML files
- Support for multi-module projects

### Fixed

- Fixed handling of environment variables in jasypt configuration