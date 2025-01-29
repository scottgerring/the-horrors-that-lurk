# The Horror that Lurks

Supporting app for the talk of the same name.
The `main` branch contains the complete, working app, with the native build issues resolved for everything. You can use `build-it-all.sh` to build it all, and then `quarkus dev` or `docker-compose up` in [dives-java](dives-java) to see it in action.

The interesting parts are in [horror-api](horror-api) and the supporting build infra and code; this is where the native code magic happens.

## Other Branches

* [broken-v2.0-glibc](https://github.com/scottgerring/the-horrors-that-lurk/tree/broken-v2.0-glibc) everything works except for the Alpine image. This is because we're using `glibc` in the `horror-api` build.
* [broken-v2.0-arch](https://github.com/scottgerring/the-horrors-that-lurk/tree/broken-v2.0-arch) `quarkus dev` works on mac, but the `docker-compose` file in `dives-java` doesn't, because we don't have an ARM64 version of the `horror-api`.
* [broken-v2.0-nomac](https://github.com/scottgerring/the-horrors-that-lurk/tree/broken-v2.0-nomac) - nothing works! `quarkus dev` is broken on the Mac, because we don't have a MacOS version of the `horror-api`.
