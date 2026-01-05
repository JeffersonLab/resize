# resize [![CI](https://github.com/JeffersonLab/resize/actions/workflows/ci.yaml/badge.svg)](https://github.com/JeffersonLab/resize/actions/workflows/ci.yaml) [![Docker](https://img.shields.io/docker/v/jeffersonlab/resize?sort=semver&label=DockerHub)](https://hub.docker.com/r/jeffersonlab/resize)
A [Jakarta EE 10](https://en.wikipedia.org/wiki/Jakarta_EE) web application for resizing images via a wrapper to [ImageMagick](https://imagemagick.org/), developed for use by [Presenter](https://github.com/JeffersonLab/presenter).

![Screenshot](https://github.com/JeffersonLab/resize/raw/main/Screenshot.png?raw=true "Screenshot")

---
- [Quick Start with Compose](https://github.com/JeffersonLab/resize#quick-start-with-compose)
- [Install](https://github.com/JeffersonLab/resize#install)
- [Configure](https://github.com/JeffersonLab/resize#configure)
- [Build](https://github.com/JeffersonLab/resize#build)
- [Release](https://github.com/JeffersonLab/resize#release)
- [Deploy](https://github.com/JeffersonLab/resize#deploy)
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
This application requires a Java 17+ JVM and standard library to run, plus a Jakarta EE 10 application server (developed with Wildfly).

1. Install ImageMagick
2. Download [Wildfly 37.0.1](https://www.wildfly.org/downloads/)
3. [Configure](https://github.com/JeffersonLab/resize#configure) Wildfly and start it
4. Download [resize.war](https://github.com/JeffersonLab/resize/releases) and deploy it to Wildfly
5. Navigate your web browser to [localhost:8080/resize](http://localhost:8080/resize)


## Configure

### Runtime
Set the path to ImageMagick mogrify executable via `MOGRIFY` environment variable.

## Build
This project is built with [Java 21](https://adoptium.net/) (compiled to Java 17 bytecode), and uses the [Gradle 9](https://gradle.org/) build tool to automatically download dependencies and build the project from source:

```
git clone https://github.com/JeffersonLab/resize
cd resize
gradlew build
```
**Note**: If you do not already have Gradle installed, it will be installed automatically by the wrapper script included in the source

**Note for JLab On-Site Users**: Jefferson Lab has an intercepting [proxy](https://gist.github.com/slominskir/92c25a033db93a90184a5994e71d0b78)

## Release
1. Bump the version number in the VERSION file and commit and push to GitHub (using [Semantic Versioning](https://semver.org/)).
2. The [CD](https://github.com/JeffersonLab/resize/blob/main/.github/workflows/cd.yaml) GitHub Action should run automatically invoking:
    - The [Create release](https://github.com/JeffersonLab/java-workflows/blob/main/.github/workflows/gh-release.yaml) GitHub Action to tag the source and create release notes summarizing any pull requests.   Edit the release notes to add any missing details.  A war file artifact is attached to the release.

## Deploy
At JLab this app is found internally at [wildfly5.acc.jlab.org/resize](https://wildfly6.acc.jlab.org/resize) and at [wildflytest5.acc.jlab.org/resize](https://wildflytest6.acc.jlab.org/resize).  The `ace.jlab.org` and `acctest.acc.jlab.org` proxy servers do not proxy this internal only service.   A [deploy script](https://github.com/JeffersonLab/wildfly/blob/main/scripts/deploy.sh) is provided to automate wget and deploy.  Example:

```
/opt/wildfly/cd/deploy.sh resize v1.2.3
```

**JLab Internal Docs**:  [RHEL9 Wildfly](https://acgdocs.acc.jlab.org/en/ace/builds/rhel9-wildfly)
