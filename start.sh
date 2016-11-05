#!/bin/bash

sbt "; project cluster-1; set javaOptions += \"-Dakka.remote.netty.tcp.port=$1\"; run"

