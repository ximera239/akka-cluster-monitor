#!/bin/bash

#sbt "; project cluster-1; set javaOptions += \"-Dakka.remote.netty.tcp.port=$1\"; run"
sbt "; project cluster-1; set javaOptions ++= Seq(\"-Dakka.remote.netty.tcp.port=$1\", \"-Dakka.persistence.journal.leveldb.dir=journal-$1\"); run"

