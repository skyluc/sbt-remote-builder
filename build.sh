set -x

mvn -Pscala-2.10.x,eclipse-juno,scala-ide-nightly clean verify -Dtycho.localArtifacts=ignore -Dscala-ide-branch=4.0.x -Dsbtrc.repo=file:///home/luc/tmp/sbt-rc-m2repo

