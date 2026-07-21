# Elytra Boundary (Fabric 1.21.1)

Server-side Fabric mod. Clients do not need to install it.

## Behavior

- Only checks players in the Overworld.
- Boundary center: X=1141, Z=2548.
- XZ radius: 3000 blocks.
- Walking outside the radius is allowed.
- A player is kicked only while actively Elytra-gliding outside the radius.

## Build with GitHub Actions

1. Upload the contents of this folder to the root of a GitHub repository.
2. Confirm that `build.gradle` is visible at the repository root.
3. Open Actions > Build Fabric Mod > Run workflow.
4. Download the artifact named `elytra-boundary-fabric-1.21.1`.
5. Use the non-sources JAR from the artifact ZIP.

## Server installation

Place the built JAR in the server's `mods` folder. Fabric Loader and Fabric API must already be installed on the server.
