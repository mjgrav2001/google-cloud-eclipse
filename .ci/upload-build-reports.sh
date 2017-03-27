#!/bin/sh
# Preserve SWTBot screenshots and surefire reports by
# uploading results to a Google Cloud Storage bucket

if [ $# -ne 1 ]; then
    echo "use: $0 gs://bucket/location" 1>&2
    exit 1
fi

# Following command flattens all screenshots and test reports, which
# works as as screenshots and surefire-reports files # do not clash
find plugins \( -name screenshots -o -name surefire-reports \) -print \
    | gsutil -m cp -r -I "$1"

