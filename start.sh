#!/bin/bash

sbt -Dakka.remote.netty.tcp.port=$1 cluster-1/run
