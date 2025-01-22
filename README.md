# Executors by Fizzed

[![Maven Central](https://img.shields.io/maven-central/v/com.fizzed/executors?color=blue&style=flat-square)](https://mvnrepository.com/artifact/com.fizzed/bigmap)

[![Java 8](https://img.shields.io/github/actions/workflow/status/fizzed/executors/java8.yaml?branch=master&label=Java%208&style=flat-square)](https://github.com/fizzed/executors/actions/workflows/java8.yaml)
[![Java 11](https://img.shields.io/github/actions/workflow/status/fizzed/executors/java11.yaml?branch=master&label=Java%2011&style=flat-square)](https://github.com/fizzed/executors/actions/workflows/java11.yaml)
[![Java 17](https://img.shields.io/github/actions/workflow/status/fizzed/executors/java17.yaml?branch=master&label=Java%2017&style=flat-square)](https://github.com/fizzed/executors/actions/workflows/java17.yaml)
[![Java 21](https://img.shields.io/github/actions/workflow/status/fizzed/executors/java21.yaml?branch=master&label=Java%2021&style=flat-square)](https://github.com/fizzed/executors/actions/workflows/java21.yaml)

[![Linux x64](https://img.shields.io/github/actions/workflow/status/fizzed/executors/java11.yaml?branch=master&label=Linux%20x64&style=flat-square)](https://github.com/fizzed/executors/actions/workflows/java11.yaml)
[![MacOS arm64](https://img.shields.io/github/actions/workflow/status/fizzed/executors/macos-arm64.yaml?branch=master&label=MacOS%20arm64&style=flat-square)](https://github.com/fizzed/executors/actions/workflows/macos-arm64.yaml)
[![Windows x64](https://img.shields.io/github/actions/workflow/status/fizzed/executors/windows-x64.yaml?branch=master&label=Windows%20x64&style=flat-square)](https://github.com/fizzed/executors/actions/workflows/windows-x64.yaml)

[Fizzed, Inc.](http://fizzed.com) (Follow on Twitter: [@fizzed_inc](http://twitter.com/fizzed_inc))

Executors by Fizzed helps you build, manage, and operate long-lived workers (threads)
on the JVM.

Java's standard ExecutorService and its various implementations (correctly) hide where
and how your Runnable is executing.  They are designed to abstract the run() method.

However, when building long-lived tasks, where some parts of your code are critical
to not be interrupted (e.g. during a graceful shutdown). That's where the lack of
context within your run() of how you're executing becomes a problem.  This usually
consists of waiting for work and being able to prevent shutdown until that work is
completed.

This framework is a simple layer built on top of the ExecutorService to give your
Workers the context and building blocks for rock-solid long-lived tasks.


Pattern 1: poll and accept next job (e.g. Blocking Queue)

    while (!stopped) {
        job = pollAndAcceptNextJob()   // <-- critical start

	if (job) {
            // handle job
	}			       // <-- critical end

        sleep(2 seconds)               // <-- interrupt ok
    }


Pattern 2: poll then accept next job (e.g. JMS w/ manual ack)

    while (!stopped) {
        job = pollNextJob()            // <-- interrupt ok

        if (job) {
	    // accept job (ack it)     // <-- critical start
            // handle job
        }                              // <-- critical end

        sleep(2 seconds)               // <-- interrupt ok
    }

