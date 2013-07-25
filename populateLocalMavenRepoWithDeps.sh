#!/bin/bash -e

set -x

M2_REPO=$1

TEMP_FOLDER=$(mktemp -d --tmpdir=/tmp sbt-rbXXXX)

cd ${TEMP_FOLDER}


# $1 - group id
# $2 - artifact id
# $3 - version
function fetchAndInstall {
  wget "http://typesafe.artifactoryonline.com/typesafe/ivy-releases/$1/$2/$3/jars/$2.jar"
  mvn install:install-file -Dmaven.repo.local=${M2_REPO} -Dfile="$2.jar" -DgroupId="$1" -DartifactId="$2" -Dversion="$3" -Dpackaging="jar"
  rm $2.jar
}


fetchAndInstall "org.scala-sbt" "launcher-interface" "0.12.4-RC3"
fetchAndInstall "org.scala-sbt" "io" "0.12.4-RC3"
fetchAndInstall "org.scala-sbt" "io" "0.13.0-Beta2"

cd
rm -r ${TEMP_FOLDER}
