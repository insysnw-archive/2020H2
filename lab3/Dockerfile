FROM gradle:6.7.1-jdk8 AS build

ARG project
ARG category

COPY . /build

WORKDIR /build

RUN gradle --console=plain --no-daemon --stacktrace :${project}-${category}:installDist \
    && mv /build/modules/${project}/${category}/build/install/${project}-${category}/bin/${project}-${category} \
        /build/modules/${project}/${category}/build/install/${project}-${category}/bin/entrypoint \
    && rm /build/modules/${project}/${category}/build/install/${project}-${category}/bin/${project}-${category}.bat

FROM openjdk:8-jre-alpine AS app

ARG project
ARG category
ARG port

COPY --from=build /build/modules/${project}/${category}/build/install/${project}-${category} /app

EXPOSE ${port}/tcp
ENTRYPOINT [ "/app/bin/entrypoint" ]
