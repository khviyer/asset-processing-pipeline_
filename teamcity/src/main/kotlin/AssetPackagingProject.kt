

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.vcs
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

version = "2023.11"

project {
    vcsRoot(AssetPackagingVcsRoot)
    
    buildType(AssetAssembly)
    buildType(DeliveryToGCS)
    buildType(CompositeBuild)
}

object AssetPackagingVcsRoot : GitVcsRoot({
    name = "Asset Packaging VCS Root"
    url = "https://github.com/[your-username]/asset-packaging-pipeline.git"
    branch = "refs/heads/main"
    branchSpec = "+:refs/heads/*"
    authMethod = password {
        userName = "git"
        password = "credentialsJSON:github-token"
    }
})

object AssetAssembly : BuildType({
    name = "Asset Assembly"
    description = "Assembles game assets into zip archives with MD5 naming"
    
    vcs {
        root(AssetPackagingVcsRoot)
    }
    
    steps {
        script {
            name = "Install Dependencies"
            scriptContent = """
                #!/bin/bash
                set -e
                
                # Install Python dependencies
                pip3 install Pillow
                
                # Verify required tools are available
                which python3
                which zip
                which md5sum
            """.trimIndent()
        }
        
        script {
            name = "Process Assets"
            scriptContent = """
                #!/bin/bash
                set -e
                
                # Run asset processing script
                python3 scripts/process_assets.py
                
                # List generated artifacts
                echo "Generated artifacts:"
                ls -la output/
            """.trimIndent()
        }
    }
    
    artifactRules = "output/*.zip"
    
    triggers {
        vcs {
            branchFilter = "+:*"
        }
    }
    
    features {
        vcs {
            labeling = VcsLabeling.SUCCESSFUL
        }
    }
})

object DeliveryToGCS : BuildType({
    name = "Delivery to Google Cloud Storage"
    description = "Delivers assembled assets to Google Cloud Storage (main branch only)"
    
    vcs {
        root(AssetPackagingVcsRoot)
    }
    
    steps {
        script {
            name = "Setup Google Cloud SDK"
            scriptContent = """
                #!/bin/bash
                set -e
                
                # Install gcloud if not available
                if ! command -v gcloud &> /dev/null; then
                    curl https://sdk.cloud.google.com | bash
                    exec -l ${'$'}SHELL
                    gcloud init
                fi
                
                # Authenticate using service account
                echo '%gcs.credentials%' > /tmp/gcs-key.json
                gcloud auth activate-service-account --key-file=/tmp/gcs-key.json
                
                # Set project
                gcloud config set project %gcs.project.id%
            """.trimIndent()
        }
        
        script {
            name = "Upload to GCS"
            scriptContent = """
                #!/bin/bash
                set -e
                
                # Download artifacts from Asset Assembly build
                # (This would be configured via dependency in actual TeamCity)
                
                # Upload to Google Cloud Storage
                gcloud storage cp output/*.zip gs://%gcs.bucket.name%/assets/
                
                echo "Successfully uploaded assets to GCS bucket: %gcs.bucket.name%"
            """.trimIndent()
        }
    }
    
    triggers {
        vcs {
            branchFilter = "+:main"
        }
    }
    
    dependencies {
        dependency(AssetAssembly) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }
            artifacts {
                artifactRules = "output/*.zip"
            }
        }
    }
    
    params {
        param("gcs.bucket.name", "Test-Bucket")
        param("gcs.project.id", "asset-packaging-pipeline")
        password("gcs.credentials", "credentialsJSON:gcs-service-account")
    }
})

object CompositeBuild : BuildType({
    name = "Composite Build"
    description = "Composite build that triggers Asset Assembly -> GCS Delivery chain"
    
    type = BuildTypeSettings.Type.COMPOSITE
    
    vcs {
        root(AssetPackagingVcsRoot)
        showDependenciesChanges = true
    }
    
    triggers {
        vcs {
            branchFilter = "+:*"
        }
    }
    
    dependencies {
        snapshot(AssetAssembly) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        
        snapshot(DeliveryToGCS) {
            onDependencyFailure = FailureAction.IGNORE
            onDependencyCancel = FailureAction.IGNORE
        }
    }
    
    artifactRules = "output/*.zip"
})

