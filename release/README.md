# release
APK release dihasilkan via GitHub Actions `release.yml` (tag v*).
File `nettra-<version>.apk` akan di-upload ke GitHub Release dan di-copy ke folder ini oleh workflow.

Jangan build release lokal — lihat notes.txt: build berat di GitHub Action.
Untuk trigger manual: `git tag v1.0.0 && git push origin v1.0.0` atau workflow_dispatch.
