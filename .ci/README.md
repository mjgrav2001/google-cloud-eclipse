# Continuous Integration Support for Travis

This directory contains files and helpers for Travis-CI.

## Uploading Build Reports to GCS

A script `upload-build-reports.sh` uploads build reports, like
SWTBot `screenshots/` and Surefire reports in
`.../target/surefire-reports/` to a provided GCS bucket.

### Configuring Auto-Deletion for the GCS Bucket

`gcloud-eclipse-testing.lifecycle.json` provides an auto-deletion configuration
policy suitable for a GCS bucket hosting the build reports.  It should be installed
with:

```
$ gsutil lifecycle set .ci/gcloud-eclipse-testing.lifecycle.json gs://gcloud-eclipse-testing 
```

