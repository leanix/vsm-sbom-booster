## VSM SBOM Booster

The `vsm-sbom-booster`is a prototype to centralize & automate ([CycloneDX](https://cyclonedx.org/capabilities/sbom/)) SBOM generation. This helps alleviate impediments with the tedious CI/CD-based approach to amend hundreds of CI/CD pipelines and asking engineers to do yet another task.

The prototype is based on the open-source project [**ORT**](https://github.com/oss-review-toolkit/ort), but entails added capabilities to interact both with Git providers APIs (currently only GitHub Cloud, GitHub Enterprise, GitLab Cloud, GitLab Self-Hosted, & BitBucket Cloud) and automatic upload of SBOMs to your VSM workspace.

The prototype only needs access to your repositories to do its job.

We use this prototype internally to LeanIX to generate SBOMs for our projects. Internally, we have seen a success rate of 80% (80% of *real* service repositories produced a valid & high-quality SBOM).

> This is a prototype. Hence, please consider this a contribution of our best-efforts to the community. By no means, is this supported by regular LeanIX SLAs or support. See the below on the details.

## Usage

1. Run the Docker container from the latest image:

GitHub:
```console
docker run --pull=always --rm \
           -v /var/run/docker.sock:/var/run/docker.sock \
           -v <temp-folder-to-be-used-for-storing-data>:/tempDir \
           -e MOUNTED_VOLUME='<temp-folder-to-be-used-for-storing-data>' \
           -e LEANIX_HOST='<leanix-workspace-host>' \
           -e LEANIX_TOKEN='<leanix-technical-user-token>' \
           -e GIT_PROVIDER='GITHUB' \
           -e GITHUB_TOKEN='<github-token>' \
           -e GITHUB_ORGANIZATION='<github-organization>' \
           leanixacrpublic.azurecr.io/vsm-sbom-booster
```

GitLab:
```console
docker run --pull=always --rm \
           -v /var/run/docker.sock:/var/run/docker.sock \
           -v <temp-folder-to-be-used-for-storing-data>:/tempDir \
           -e MOUNTED_VOLUME='<temp-folder-to-be-used-for-storing-data>' \
           -e LEANIX_HOST='<leanix-workspace-host>' \
           -e LEANIX_TOKEN='<leanix-technical-user-token>' \
           -e GIT_PROVIDER='GITLAB' \
           -e GITLAB_TOKEN='<gitlab-token>' \
           -e GITLAB_GROUP='<gitlab-group>' \
           leanixacrpublic.azurecr.io/vsm-sbom-booster
```

BitBucket:
```console
docker run --pull=always --rm \
           -v /var/run/docker.sock:/var/run/docker.sock \
           -v <temp-folder-to-be-used-for-storing-data>:/tempDir \
           -e MOUNTED_VOLUME='<temp-folder-to-be-used-for-storing-data>' \
           -e LEANIX_HOST='<leanix-workspace-host>' \
           -e LEANIX_TOKEN='<leanix-technical-user-token>' \
           -e GIT_PROVIDER='BITBUCKET' \
           -e BITBUCKET_KEY='<bitbucket-key>' \
           -e BITBUCKET_SECRET='<bitbucket-secret>' \
           -e BITBUCKET_WORKSPACE='<bitbucket-workspace>' \
           leanixacrpublic.azurecr.io/vsm-sbom-booster
```

2. After a while your mapping inbox should be receiving new discovery items. These will need to be mapped by you once (see our [user documentation](https://docs-vsm.leanix.net/docs/discover-automate#create-your-service-baseline)).

3. Using Docker Compose the container can be run using the following docker-compose.yml file (GitHub provider) :

```console
version: '3.3'
services:
    vsm-sbom-booster:
        restart: always
        container_name: vsm-sbom-booster
        volumes:
            - /var/run/docker.sock:/var/run/docker.sock
            - <temp-folder-to-be-used-for-storing-data>:/tempDir
        environment:
            - MOUNTED_VOLUME=<temp-folder-to-be-used-for-storing-data>
            - LEANIX_HOST=<leanix-workspace-host>
            - LEANIX_TOKEN=<leanix-technical-user-token>
            - GIT_PROVIDER=GITHUB
            - GITHUB_TOKEN=<github-token>
            - GITHUB_ORGANIZATION=<github-organization>
        image: leanixacrpublic.azurecr.io/vsm-sbom-booster
```

### Environment Variables

#### Runtime settings
The first `-v` param is needed as the setup is a docker-in-docker setup and will need to mount the local docker runtime into the container. Under normal circumstances this param can be copied and pasted.

The second `-v` param is the path to temporary folder that the `vsm-sbom-booster`will use to temporarily clone the projects to attempt to generate the SBOM. e.g. `~/output/temp`. Docker should have Read/Write access on this folder.

`MOUNTED_VOLUME`: This is the same as the second `-v` param. It's required as the container needs an explicit env variable to do its job.

`CONCURRENCY_FACTOR`(optional): The number of parallel jobs `vsm-sbom-booster` will use to generate SBOMs. Note: increasing this number will come at higher compute costs. Default: 3

`ANALYSIS_TIMEOUT`(optional): The timeout, in minutes, that is used to force kill container workers working on analyzing the repository. There are cases that we need to force kill containers with slow progress to free up resources. Default: 30

`DEV_MODE`(optional): This is a flag to enable/disable the dev mode. When enabled, all logs from ORT containers will be preserved in the temp folder location and the ORT project folders will be retained. Additionally, the logging level for the ORT containers will be set to DEBUG. The processing of each repository consists of three separate phases (download, analyze and generate_sbom) that are facilitated by the ORT software. The produced logs are saved in the temp folder using the `<repository_name>_<phase>_log.txt` naming pattern.This is useful for debugging purposes. Default: false

`ALLOW_NO_COMPONENT_SBOMS`(optional): This is a flag to enable/disable the submission of SBOM files without any components. Default: false

`HTTP_PROXY`: (optional): The HTTP proxy to use for the ORT container (See https://github.com/oss-review-toolkit/ort#environment-variables). Default: none

`HTTPS_PROXY`: (optional): The HTTPS proxy to use for the ORT container (See https://github.com/oss-review-toolkit/ort#environment-variables). Default: none

#### LeanIX configs

`LEANIX_TOKEN`: API token with **ADMIN** rights from your VSM workspace. (see admin > technical users).

`LEANIX_HOST`: The host name of your LeanIX workspace. (e.g. For `https://acme.leanix.net` you would provide `acme`)

#### [Discovery API data](https://docs-vsm.leanix.net/reference/discovery_service)

`SOURCE_TYPE` (optional): This will the source system from where you're scanning. This is used in the mapping inbox to understand where discovered data originated from. Default: `vsm-sbom-booster`

`SOURCE_INSTANCE`(optional): Individual instance within the source system e.g. prod or org entity within the source system. This is used in the mapping inbox to understand where discovered data originated from. Default: GitHub Org or GitLab Group + Sub Groups

#### GIT

`GIT_PROVIDER`: This is the Git provider to scan and generate SBOMs from. For now, either `GITHUB` or `GITLAB`.

#### GITHUB (only if `GIT_PROVIDER` is `GITHUB`)

`GITHUB_TOKEN`: The [Personal Access Token](https://github.com/leanix/vsm-github-broker#personal-access-token) with `read:org` and `repo` scopes. 

`GITHUB_ORGANIZATION`: The GitHub organization name which `vsm-sbom-booster` shall scan and try to generate the SBOMs for.

`GITHUB_API_HOST` (optional): if you want to connect the `vsm-sbom-booster` with GitHub Enterprise you will need to provide the host where the GitHub GraphQL API is exposed. e.g. For `https://ghe.domain.com` you would provide `ghe.domain.com`

#### GITLAB (only if `GIT_PROVIDER` is `GITLAB`)

`GITLAB_TOKEN`: The Personal Access Token with API read permissions.

`GITLAB_GROUP`: The GitLab group name which `vsm-sbom-booster` shall scan and try to generate the SBOMs for.

`GITLAB_API_HOST` (optional): if you want to connect the `vsm-sbom-booster` with GitLab self-hosted you will need to provide the host where the GitLab GraphQL API is exposed. e.g. For `https://gl.domain.com` you would provide `gl.domain.com`

#### BITBUCKET (only if `GIT_PROVIDER` is `BITBUCKET`)

`BITBUCKET_KEY`: The key to an [OAuth Consumer](https://support.atlassian.com/bitbucket-cloud/docs/integrate-another-application-through-oauth/) with "Private Consumer" enabled and the READ:REPOSITORY permission.

`BITBUCKET_SECRET`: The secret to an [OAuth Consumer](https://support.atlassian.com/bitbucket-cloud/docs/integrate-another-application-through-oauth/) with "Private Consumer" enabled and the READ:REPOSITORY permission.

`BITBUCKET_WORKSPACE`: The BitBucket Workspace name which `vsm-sbom-booster` shall scan and try to generate the SBOMs for.

## Technical Notes
![Technical Architecture](/vsm-sbom-booster.png)
The setup makes use of a docker-in-docker architecture. This means that the main container contains a java-application that interacts with the ORT docker file, which in turn does the heavy-lifting of attempting to generate the SBOM.

While ORT already has advanced capabilities to understand on the surface of the repository files, which package managers are used, how to build the project and generate the SBOM, it may still sometimes fail for various reasons.

Also, currently the prototype does not filter out any repositories that evidently are not real services (e.g. repositories with markdown only, script with out any dependencies) and will hence fail to generate a SBOM. Likewise, the true success rate might even then be higher.

Today, the prototype only supports to scan GitHub Cloud and GitHub Enterprise. We are open to contribution to extend the functionality to other Git Providers. The prototype only needs the list of repository URLs as input and is otherwise Git Provider agnostic.

We also encourage contributions to extend this prototype to be able to run when package manager files changes, to bring it closer the real build. 

### Support of ORT configuration files
The `vsm-sbom-booster` supports the ORT configuration files if they are present in the `MOUNTED_VOLUME` under the `config` folder. The `config` folder is used as the value of the `ORT_CONFIG_DIR` environmental variable as described [here](https://github.com/oss-review-toolkit/ort#environment-variables). For more details on the ORT configuration files, please refer to the [ORT documentation](https://github.com/oss-review-toolkit/ort#configuration-files).

### Supported Package Managers

For a full list, please refer to the [ORT documentation](https://github.com/oss-review-toolkit/ort#details-on-the-tools).

### Logging
The `vsm-sbom-booster` will per default have verbose logs to understand the inner-workings, as well as any errors. It will also create a summary report in the mounted volume on your host machine. The summary report file name is `summaryReport_<timestamp>.txt`. It contains details for which repository an SBOM file was generated and for which it failed. There are cases where `vsm-sbom-booster` will have to forcefully shutdown stale jobs after a specific amount of time. Keep in mind that those repositories will not be present in the summary report file.

## A word on support
This project is an experimental prototype. Regular LeanIX service SLA and support do not apply to this project. We also maintain this prototype sporadically.

Yet, we are open to discuss the work and are of course open to any contributions by the community 💙

## Contributing

We welcome contributions to the VSM SBOM Booster project. If you're looking to contribute:

1. **Issues**: Feel free to open an [issue](https://github.com/leanix/vsm-sbom-booster/issues) if you find a bug or want to suggest an enhancement. Please provide as much context as possible.

2. **Pull requests**: If you'd like to contribute code, make sure to read our [Contribution Guidelines](./CONTRIBUTING.md) before submitting a pull request.

3. **Security**: If you find a vulnerability, please review our [Security Policy](./SECURITY.md) on how to report it.

Thank you for your interest in contributing to our project!
