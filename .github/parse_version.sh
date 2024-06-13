# Define the path to the file
FILE="./app/build.gradle.kts"

# Extract the version numbers from the file
versionMajor=$(grep 'val versionMajor' "$FILE" | awk '{print $4}')
versionMinor=$(grep 'val versionMinor' "$FILE" | awk '{print $4}')
versionPatch=$(grep 'val versionPatch' "$FILE" | awk '{print $4}')

# Output the version in the desired format
echo "$versionMajor.$versionMinor.$versionPatch"