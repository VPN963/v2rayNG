MobileTina exact artwork staging folder.
Upload the exact Photo.zip supplied by the user into this folder as:
.github/staging/Photo.zip

The workflow verifies the ZIP and all seven artwork files byte-for-byte, restores them without conversion, removes the incorrect vector resources, verifies the 208dp Auto FAB layout, builds ARMv7, and only then commits the restored artwork.
