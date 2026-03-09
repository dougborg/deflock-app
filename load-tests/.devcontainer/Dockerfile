# Dev container image for running Gatling load tests.
#
# Based on Microsoft's Java 21 dev container, which includes:
# - JDK 21 (Eclipse Temurin)
# - Gradle (via the wrapper in the project)
# - Standard dev tools (git, curl, etc.)
#
# We add Coursier (the Scala package manager) for Scala tooling support
# in VS Code via the Metals extension.

FROM mcr.microsoft.com/devcontainers/java:21

# Install Coursier for Scala tooling (used by the Metals VS Code extension).
# Note: the Scala compiler itself is managed by Gradle via the Gatling plugin,
# so we only need Coursier for IDE support.
RUN curl -fL https://github.com/coursier/coursier/releases/latest/download/cs-x86_64-pc-linux.gz | gzip -d > /usr/local/bin/cs \
    && chmod +x /usr/local/bin/cs
