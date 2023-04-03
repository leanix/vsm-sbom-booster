To build image locally:

```console
docker build --platform linux/x86_64 -t vsm-sbom-booster .
```

To run image locally:

```console
docker run --platform linux/x86_64 
           -v /var/run/docker.sock:/var/run/docker.sock \
           -v <temp-folder-to-be-used-for-storing-data>:/tempDir \
           -e MOUNTED_VOLUME='<temp-folder-to-be-used-for-storing-data>' \
           -e LEANIX_TOKEN='<leanix-technical-user-token>' \
           -e GITHUB_GRAPHQL_API_URL='https://api.github.com/graphql' \
           -e GITHUB_TOKEN='<github-token>' \
           -e GITHUB_ORGANIZATION='<github-organization>' \
           -e REGION='<leanix-region>' \
           -e SOURCE_TYPE='<sourceType>' \
           -e SOURCE_INSTANCE='<sourceInstance>' \
           -e CONCURRENCY_FACTOR='1' \
           vsm-sbom-booster -rm
```