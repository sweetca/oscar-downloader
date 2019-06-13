# oscar-downloader
Git unit to perform actions over repositories code source

# How to start
1. Install and start MongoDB
2. Customize application.git.repository_dir in application.yml - a path to directory where git crawler checks out repositories.
This can be done later by setting APPLICATION_GIT_REPOSITORY_DIR environment variable.
3. To compile and start, run ./mvnw

# REST API

1. Download private gitlab repo
```
POST /download
{
	"id":"0",
	"payload":{
		"componentType":"gitlab",
		"gitUrl":"https://gitlab-ci-token:token@gitlab.com/sweetca/logger.git",
		"accessToken":"token",
		"userName":"sweetca",
		"gitId":"777",
		"gitBranch":"multi_file"
	}
}
```

# Profiles
`dev` profile is default. To enable `prod` profile, use -Pprod or -Dspring.profiles.active=prod
For `prod` profile, set APPLICATION_GIT_REPOSITORY_DIR (e.g. in Dockerfile).

# Deploy

1. Build image:
```
docker build -t git-crawler .
```

Note, that image tag has to be unique, that means that usign `latest` tag won't work. You can use hash generator to generate new tag or use semantic versioning.
