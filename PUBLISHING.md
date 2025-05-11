# Publishing Releases on GitHub

This document outlines the steps to create and publish a new release of the Kompact app on GitHub.

## Creating a Release APK

1. In Android Studio, select **Build > Generate Signed Bundle/APK**
2. Choose **APK** and click **Next**
3. Create or use an existing keystore and complete the signing form
4. Choose **release** build variant and finish the wizard
5. Once built, locate the APK file at `app/release/app-release.apk`
6. Rename the file to `Kompact-vX.Y.Z.apk` (e.g., `Kompact-v1.0.0.apk`)

## Publishing on GitHub Releases

1. Go to your GitHub repository
2. Click on **Releases** in the right sidebar
3. Click **Create a new release** or **Draft a new release**
4. Enter a tag version (e.g., `v1.0.0`)
5. Enter a release title (e.g., `Kompact v1.0.0`)
6. Add release notes describing the changes in this version
7. Drag and drop the APK file or click to upload it
8. Check **This is a pre-release** if appropriate
9. Click **Publish release**

## Updating README.md Links

After creating the release, update the following files with the correct download links:

1. `README.md` - Update download badge and direct links:
   ```markdown
   [![Download APK](https://img.shields.io/badge/Download-APK-green.svg)](https://github.com/YOUR-USERNAME/kompact/releases/download/vX.Y.Z/Kompact-vX.Y.Z.apk)
   ```

2. `releases/README.md` - Add the new release to the list:
   ```markdown
   - [Kompact-vX.Y.Z.apk](https://github.com/YOUR-USERNAME/kompact/releases/download/vX.Y.Z/Kompact-vX.Y.Z.apk) - Release description
   ```

Remember to replace `YOUR-USERNAME` with your actual GitHub username and `X.Y.Z` with the appropriate version number. 