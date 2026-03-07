# CHANGELOG

## [1.1.0] - 2026/02/07
### Added
- Implemented database persistence.
- Storage limit functionality.
- Added new field for generated request ID.
- Implemented download continuation on ongoing requests via request ID.
- Implemented request and forget functionality by using request ID to retrieve download request and file.

## [1.0.1] - 2025/12/17
### Added
- Added more comprehensive error handling.

### Changed
- Improved resource expiry functionality.
- Increased maximum concurrent requests.

### Fixed
- Fixed issue with quality control.

## [1.0.0] - 2025/12/12

### Added
- Added support for youtube music and higher audio quality of up to `256kbps`.
- Added `Best` and `Worst` options in frontend for video and audio quality.
- Added metadata embedding for downloads.
- Added progress bars.

### Fixed
- Fixed cancel feature being clunky.

### Changed
- Improved UI flow.