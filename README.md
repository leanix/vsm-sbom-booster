## VSM SBOM Booster

The `vsm-sbom-booster`is a prototype to centralize & automate ([CycloneDX](https://cyclonedx.org/capabilities/sbom/)) SBOM generation. This helps alleviate impediments with the tedious CI/CD-based approach to amend hundreds of CI/CD pipelines and asking engineers to do yet another task.

The prototype is based on the open-source project [**ORT**](https://github.com/oss-review-toolkit/ort), but entails added capabilities to interact both with Git providers APIs (currently only GitHub Cloud & GitHub Enterprise) and automatic upload of SBOMs to your VSM workspace.

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
           -e LEANIX_REGION='<leanix-workspace-region>' \
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
           -e LEANIX_REGION='<leanix-workspace-region>' \
           -e LEANIX_TOKEN='<leanix-technical-user-token>' \
           -e GIT_PROVIDER='GITLAB' \
           -e GITLAB_TOKEN='<github-token>' \
           -e GITLAB_GROUP='<github-organization>' \
           leanixacrpublic.azurecr.io/vsm-sbom-booster
```

2. After a while your mapping inbox should be receiving new discovery items. These will need to be mapped by you once (see our [user documentation](https://docs-vsm.leanix.net/docs/discover-automate#create-your-service-baseline)).

### Environment Variables

#### Runtime settings
The first `-v` param is needed as the setup is a docker-in-docker setup and will need to mount the local docker runtime into the container. Under normal circumstances this param can be copied and pasted.

The second `-v` param is the path to temporary folder that the `vsm-sbom-booster`will use to temporarily clone the projects to attempt to generate the SBOM. e.g. `~/output/temp`. Docker should have Read/Write access on this folder.

`MOUNTED_VOLUME`: this is the same as the second `-v` param. It's required as the container needs an explicit env variable to do its job.

`CONCURRENCY_FACTOR`(optional): the number of parallel jobs `vsm-sbom-booster` will use to generate SBOMs. Note: increasing this number will come at higher compute costs. Default: 3

#### LeanIX configs

`LEANIX_HOST`: The host name of your LeanIX workspace. e.g. For `https://acme.leanix.net` you would provide `acme`

`LEANIX_REGION`: The region that your VSM workspace is hosted in. One of: eu|de|us|au|ca|ch

`LEANIX_TOKEN`: API token with **ADMIN** rights from your VSM workspace. (see admin > technical users).

#### Discovery API data

`SOURCE_TYPE` (optional): this will the source system from where you're scanning. This is used in the mapping inbox to understand where discovered data originated from. Default: `vsm-sbom-booster`

`SOURCE_INSTANCE`(optional): individual instance within the source system e.g. prod or org entity within the source system. This is used in the mapping inbox to understand where discovered data originated from. Default: GitHub Org or GitLab Group + Sub Groups

#### GIT

`GIT_PPROVIDER`: This is the Git provider to scan and generate SBOMs from. For now, either `GITHUB` or `GITLAB`.

#### GITHUB (only if `GIT_PROVIDER` is `GITHUB`)

`GITHUB_TOKEN`: The [Personal Access Token](https://github.com/leanix/vsm-github-broker#personal-access-token) with `read:org` scope. 

`GITHUB_ORGANIZATION`: the GitHub organization name which `vsm-sbom-booster`shall scan and try to generate the SBOMs for.

`GITHUB_API_HOST` (optional): if you want to connect the `vsm-sbom-booster`with GitHub Enterprise you will need to provide the host where the GitHub GraphQL API is exposed. e.g. For `https://ghe.domain.com` you would provide `ghe.domain.com`

#### GITLAB (only if `GIT_PROVIDER` is `GITLAB`)

`GITLAB_TOKEN`: The Personal Access Token with API read permissions.

`GITLAB_GROUP`: the GitLab group name which `vsm-sbom-booster`shall scan and try to generate the SBOMs for.

`GITLAB_API_HOST` (optional): if you want to connect the `vsm-sbom-booster`with GitLab self-hosted you will need to provide the host where the GitLab GraphQL API is exposed. e.g. For `https://gl.domain.com` you would provide `gl.domain.com`


## Technical Notes
![Technical Architecture](/vsm-sbom-booster.png)
The setup makes use of a docker-in-docker architecture. This means that the main container contains a java-application that interacts with the ORT docker file, which in turn does the heavy-lifting of attempting to generate the SBOM.

While ORT already has advanced capabilities to understand on the surface of the repository files, which package managers are used, how to build the project and generate the SBOM, it may still sometimes fail for various reasons.

Also, currently the prototype does not filter out any repositories that evidently are not real services (e.g. repositories with markdown only, script with out any dependencies) and will hence fail to generate a SBOM. Likewise, the true success rate might even then be higher.

Today, the prototype only supports to scan GitHub Cloud and GitHub Enterprise. We are open to contribution to extend the functionality to other Git Providers. The prototype only needs the list of repository URLs as input and is otherwise Git Provider agnostic.

We also encourage contributions to extend this prototype to be able to run when package manager files changes, to bring it closer the real build. 

### Supported Package Managers

For a full list, please refer to the [ORT documentation](https://github.com/oss-review-toolkit/ort#details-on-the-tools).

### Logging
The `vsm-sbom-booster` will per default have verbose logs to understand the inner-workings, as well as any errors. It will also create a summary report in the mounted volume on your host machine. The summary report file name is `summaryReport_<timestamp>.txt`. It contains details for which repository an SBOM file was generated and for which it failed. There are cases where `vsm-sbom-booster` will have to forcefully shutdown stale jobs after a specific amount of time. Keep in mind that those repositories will not be present in the summary report file.

## A word on support
This project is an experimental prototype. Regular LeanIX service SLA and support do not apply to this project. We also maintain this prototype sporadically.

Yet, we are open to discuss the work and are of course open to any contributions by the community 💙

## Contribution 
If you find this project useful, but you're missing some functionality, feel free to open a PR with a description of the use case for the change.

Likewise, if you bump into any bugs feel free to open an issue. It might be helpful to check with [ORT open issues](https://github.com/oss-review-toolkit/ort/issues), if it's something that's already known.