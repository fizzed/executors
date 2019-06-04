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

