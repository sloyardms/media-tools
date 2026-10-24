# media-tools

Interactive command-line tool for organizing image gallery folders. Java 21, Maven.

## Build and run

```bash
mvn package
```

```bash
run.bat
```

`mvn package` runs the tests and produces the self-contained `target/media-tools.jar`.

## Menu options

### Remove thumbnail duplicates

Finds images that are smaller versions of another image in the **same folder** and removes the smaller ones.

1. Asks for the folder, whether to include subfolders, the dHash threshold (default 6 differing bits out of 64)
   and the aspect-ratio tolerance for hash matches (default 2%).
2. Only image files are considered: `jpg jpeg png gif bmp webp tif tiff`, plus files ending in a thumbnail
   marker (`photo.md`) when their content is an image. Videos and other files are ignored. HEIC/AVIF are not
   supported.
3. Matching:
   - **Name:** `photo.md.jpg`, `photo.sm.png`, `photo.thumb.webp` or `photo.md` against `photo.<any extension>`.
     The pair is also checked with dHash; if the pictures differ, or have the same dimensions, it is listed
     under **Needs review** and never removed.
   - **Hash:** for everything else, images with a similar aspect ratio and a dHash distance within the
     threshold.
   - "Smaller" means fewer pixels (width x height, read from the file header), not file size.
4. Prints a dry-run table and asks for confirmation (default: no).
5. Removed files go to the Recycle Bin. If that is not available, they are moved to `_removed/` under the scanned
   folder, keeping their relative path. Nothing is deleted permanently.
6. Writes `thumbnail-cleanup-<timestamp>.csv` in the scanned folder.

dHash: images are decoded with source subsampling to roughly 256 px on the long side, then reduced to 9x8
grayscale by area averaging, so an original and its thumbnail hash the same way.

## Adding a menu option

Implement `sloyardms.mediatools.menu.MenuAction` and register it in `App`:

```java
Menu menu = new Menu("Media Tools")
        .register(new RemoveThumbnailDuplicatesAction())
        .register(new MyNewAction());
```

Use the `ConsoleIO` passed to `run` for all prompts and output, so the action can be tested with scripted input.
