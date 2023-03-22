# resize
A [Java EE 8](https://en.wikipedia.org/wiki/Jakarta_EE) web application for resizing images via a thin wrapper to [ImageMagick](https://imagemagick.org/), and developed for use by [Presenter](https://github.com/JeffersonLab/presenter).

![Screenshot](https://github.com/JeffersonLab/resize/raw/main/Screenshot.png?raw=true "Screenshot")

---
- [Quick Start with Compose](https://github.com/JeffersonLab/resize#quick-start-with-compose)
- [Install](https://github.com/JeffersonLab/resize#install)
- [Configure](https://github.com/JeffersonLab/resize#configure)
- [Build](https://github.com/JeffersonLab/resize#build)
- [Release](https://github.com/JeffersonLab/resize#release)
---

## Quick Start with Compose
1. Grab project
```
git clone https://github.com/JeffersonLab/resize
cd resize
```
2. Launch [Compose](https://github.com/docker/compose)
```
docker compose up
```
3. Navigate to page
```
http://localhost:8080/resize
```

## Install
This application requires a Java 11+ JVM and standard library to run, plus a Java EE 8+ application server (developed with Wildfly).

1. Install ImageMagick
2. Download [Wildfly 26.1.3](https://www.wildfly.org/downloads/)
3. [Configure](https://github.com/JeffersonLab/resize#configure) Wildfly and start it
4. Download [resize.war](https://github.com/JeffersonLab/resize/releases) and deploy it to Wildfly
5. Navigate your web browser to [localhost:8080/resize](http://localhost:8080/resize)


## Configure

### Runtime
Set the path to ImageMagick mogrify executable via environment variable.

## Build
This project is built with [Java 17](https://adoptium.net/) (compiled to Java 11 bytecode), and uses the [Gradle 7](https://gradle.org/) build tool to automatically download dependencies and build the project from source:

```
git clone https://github.com/JeffersonLab/resize
cd resize
gradlew build
```
**Note**: If you do not already have Gradle installed, it will be installed automatically by the wrapper script included in the source

**Note for JLab On-Site Users**: Jefferson Lab has an intercepting [proxy](https://gist.github.com/slominskir/92c25a033db93a90184a5994e71d0b78)

## Release
1. Bump the release date and version number in build.gradle and commit and push to GitHub (using [Semantic Versioning](https://semver.org/)).
2. Create a new release on the GitHub Releases page corresponding to the same version in the build.gradle.   The release should enumerate changes and link issues.   A war artifact can be attached to the release to facilitate easy install by users.
3. Build and publish a new Docker image [from the GitHub tag](https://gist.github.com/slominskir/a7da801e8259f5974c978f9c3091d52c#8-build-an-image-based-of-github-tag). GitHub is configured to do this automatically on git push of semver tag (typically part of GitHub release) or the [Publish to DockerHub](https://github.com/JeffersonLab/resize/actions/workflows/docker-publish.yml) action can be manually triggered after selecting a tag.
4. Bump and commit quick start [image version](https://github.com/JeffersonLab/resize/blob/main/docker-compose.override.yml)
