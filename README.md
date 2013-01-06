appstart
========

The Universal Java Application Launcher

This is just a fork from http://code.google.com/p/appstart/! All code by mdallav@gmail.com.

## Introduction
One of the frustrations about writing Java desktop applications is the setup needed to launch the Java Virtual Machine. This is often implemented using some platform-dependent launch scripts, which their only role is to find a suitable JRE and launch it with some options.

This is not optimal, since this defeats the cross-platform nature of Java.

Enter Appstart, the universal cross-platform Java application launcher.

## What is Appstart ?
Appstart is a cross-platform application launcher (is written in Java itself), that relieves you from writing another shell/batch script to launch a Java desktop application. All you have to do is:

put the appstart.jar in your application dir. You can rename it to your likings, such as run.jar, start.jar, your_app_name.jar
setup the appstart.properties file, specifying the main class to launch and other VM options (see AppstartProperties)
then put all your jars and classes in the subfolder "lib".
Voila! You can now double-click the appstart.jar and your application is started, without hideous DOS windows, class-path headhaches or fears of the dreaded OutOfMemoryException.

Even more: do you want to show a splash screen for your application ? Then place a splash.img file (can be jpeg, png or gif format) and you will automatically have a splash screen that is loaded at JRE startup (this feature leverages the -splash option of JavaSE 6).

To develop Appstart i took inspiration from the Java Web Start technology, but this project is much simpler and oriented towards standard desktop applications.

## Features

* Easy set-up and configuration, uses a small start.jar file and a appstart.properties file. No fighting with class-path or current directory issues.
* Each application resides in its own folder in disk.
* Absolutely cross-platform
* No dependencies, only a Java Runtime Environment is needed
* Do not write and run .bat or .sh scripts anymore. All you ned to do is a double click on start.jar !
* Specify system properties and VM heap space for the application in the appstart.properties file. No more hard-coded configuration or OutOfMemoryException !
* Support for JavaSE 6 splash screen
* Open source, licensed with the MIT license

## But isn't this already done by other projects ?
There are other projects with similar objectives, some of these (like launch4j) create small .exe files that launch the Java virtual machine. Of course this solution works only on Windows machines: linux machines will still have to run a shell script.

## How Appstart works

At the core of Appstart, a child process is spawned after the launch of the Appstart jar file. Basically, everything boils down to:
`Runtime.getRuntime().exec(<java process with parameters>);`
of course, there is more that that:
* The position of the "java" launcher is taken from the system property ${java.home}. So, the JRE used to run start.jar is also used to run the child process.
* The folder in which resides the start.jar file is called the "Application folder"
* A file named appstart.properties is searched inside the application folder. Inside it there must be the reference to the main class to launch and eventual JVM options to use.
* If a subfolder of the application folder named "lib" is found, every folder and jar file placed in there will be added to the classpath. You can configure the position of the lib folder in the appstart.properties file
* If a file named "splash.img" is found in the application folder, it is used as a splash screen of the child java process. This feature requires the use of Java 6.

The final command line run by Appstart is this:

`${java.home}/bin/java <app.vm.options> -cp <app.class.path> -splash:splash.img <app.main.class>`

## Appstart configuration

### Config file properties
* app.main.class: a fully-qualified class. *Required*
* app.libs.dir: the directory where appstart can find the dependent jars
* app.vm.options: a list of VM options
* app.class.path: additional class paths
* app.follow: set to true to follow the application output to console. Useful for debugging purposes

Furthermore, you should note that each key=value pair in the appstart.properties file is *put as a system property* in the spawned application, so you can specify other useful entries such as *java.util.logging.config.file, http.proxyHost, java.rmi.codebase*, ...

### Appstart system properties
* appstart.properties: overrides the path to the appstart.properties file
* appstart.verbose: print log information about the start process
