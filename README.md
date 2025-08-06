# Asset Packaging Pipeline

This repository contains the implementation of an asset packaging pipeline using TeamCity Kotlin DSL.

## Project Structure

- `assets/`: Contains the raw game assets (images, JSON bundles).
- `scripts/`: Contains Python scripts for asset processing.
- `teamcity/`: Contains the TeamCity Kotlin DSL configuration for the pipeline.
- `output/`: (Generated) Contains the processed asset archives.

## Pipeline Overview

The pipeline consists of three main builds:

1.  **Asset Assembly**: Processes raw assets, rotates images (if specified in JSON), converts to PNG, and packages them into MD5-named zip archives.
2.  **Delivery to Google Cloud Storage (GCS)**: Uploads the assembled asset archives to a specified GCS bucket. This step is only triggered for builds from the `main` branch.
3.  **Composite Build**: A composite build that orchestrates the Asset Assembly and GCS Delivery steps.

## Setup and Usage

### Prerequisites

-   TeamCity Cloud (recent version)
-   Build Agent with:
    -   `docker.io`
    -   `imagemagick` (used by the Python script for image manipulation)
    -   `zip`
    -   `unzip`
    -   `wget`
    -   `jq`
    -   `python3` (3.9+ recommended)
    -   `python3-pip`
-   Google Cloud Platform (GCP) project with a GCS bucket and a service account key for authentication.

### TeamCity Configuration

1.  **Import Project**: Import the `teamcity/` directory into your TeamCity project as a Kotlin DSL configuration.
2.  **VCS Root**: Update the `AssetPackagingVcsRoot` in `teamcity/src/main/kotlin/AssetPackagingProject.kt` with your Git repository URL.
3.  **GCS Parameters**: In the `DeliveryToGCS` build configuration, set the following parameters:
    -   `gcs.bucket.name`: Your GCS bucket name (e.g., `Test-Bucket`)
    -   `gcs.project.id`: Your GCP project ID (e.g., `asset-packaging-pipeline`)
    -   `gcs.credentials`: Your Google Cloud service account key (JSON format). This should be stored as a secure parameter in TeamCity.

### Running the Pipeline

-   The pipeline will automatically start when a new commit appears in any branch.
-   The GCS delivery step will only execute for commits on the `main` branch.

## Author Header

All relevant files include an author header as specified in the task requirements.


