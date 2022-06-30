# Contributing to FWU Keycloak extensions

We love your input! We want to make contributing to this project as easy and transparent as possible, whether it's:

- Reporting a bug
- Discussing the current state of the code
- Submitting a fix
- Proposing new features
- Becoming a maintainer

## We develop with GitHub

We use github to host code, to test and release the deliverables, as well as accept pull requests.

Pull requests are the best way to propose changes to the codebase (we use Github Flow). We actively welcome your pull requests:

1. Create your branch from master.
2. If you've added code that should be tested, add tests.
3. If you've changed APIs, update the documentation.
4. Ensure the test suite passes.
5. Make sure your code lints.
6. Issue that pull request!

## Any contributions you make will be under the Apache License

In short, when you submit code changes, your submissions are understood to be under the same [Apache License](https://choosealicense.com/licenses/apache-2.0/) version 2.0 that covers the project. Feel free to contact the maintainers if that's a concern.

## Report bugs using GitHub issues

We use GitHub issues to track public bugs. Report a bug by opening a new issue; it's that easy!

### Write bug reports with detail, background, and sample code

**Great Bug Reports** tend to have:

- A quick summary and/or background
- Steps to reproduce
  - Be specific!
  - Give sample code if you can.
- What you expected would happen
- What actually happens
- Notes (possibly including why you think this might be happening, or stuff you tried that didn't work)

## Creating a new module

The project is a multi-module Maven project. When creating a new module, make sure to add:

1. Make sure the folder has the same name as the module name in the `pom.xml`.
2. `README.md` explaining the extension.

## Automatic release process

We use [semantic versioning](https://semver.org/) defined in `projectVersion` in [`pom.xml`](pom.xml).
When this version is changed a new release will be created with the next [main](https://github.com/FWU-DE/fwu-kc-extensions/actions/workflows/main.yaml) pipeline.
The pipeline will deploy the Maven artifacts to GitHub packages aswell as create a GitHub release with the new version and attach the module artifacts to it.
Make sure to update [`CHANGELOG.md`](CHANGELOG.md) when creating a new release.
