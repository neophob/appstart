/*
 *  The MIT License
 *
 *  Copyright 2010 Marco Dalla Vecchia <mdallav@gmail.com>.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package org.appstart;

import java.awt.GraphicsEnvironment;
import java.awt.SplashScreen;
import java.awt.Toolkit;
import java.beans.Beans;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 *
 * @author marco
 */
public class Starter {

    static Logger log = Logger.getLogger("AppStart");
    public static final String DEFAULT_SPLASH_IMG = "splash.img";
    public static final String APPSTART_MAIN_CLASS = "app.main.class";
    public static final String APP_VM_OPTIONS = "app.vm.options";
    public static final String APP_CLASS_PATH = "app.class.path";
    public static final String APP_LIBS_DIR = "app.libs.dir";
    public static final String APPSTART_FILE = "appstart.properties";
    public static final String JAVA_PATH = "bin" + File.separator + "java";
    /**
     * Holds the child process
     */
    static Process child;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        if (!Boolean.getBoolean("appstart.verbose")) {
            log.setLevel(Level.WARNING);
        }
        File java = new File(System.getProperty("java.home"),
                JAVA_PATH);
        log.info("using java in " + java.getParent());

        // get the class path URLs
        URL launcherUrl = ((URLClassLoader) Starter.class.getClassLoader()).getURLs()[0];

        // search for a appstart.properties file
        File appstartDir = new File(launcherUrl.toURI());
        if (appstartDir.isFile()) {
            appstartDir = appstartDir.getParentFile();
        }
        log.info("appstart dir: " + appstartDir);

        Properties launchProps = findLaunchProperties(appstartDir, new Properties());
        String appstartPath = System.getProperty(APPSTART_FILE);
        if (appstartPath != null) {
            try {
                InputStream in = new FileInputStream(appstartPath);
                try {
                    launchProps.load(in);
                    in.close();
                } finally {
                    in.close();
                }
            } catch (IOException ioe) {
                log.severe("Cannot load appstart config in " + appstartPath);
            }
        }

        if (launchProps == null) {
            log.severe("missing config file " + APPSTART_FILE);
            if (!GraphicsEnvironment.isHeadless()) {
                javax.swing.JOptionPane.showMessageDialog(
                        null,
                        "Missing config file " + APPSTART_FILE,
                        "Error starting application",
                        javax.swing.JOptionPane.ERROR_MESSAGE);
            }
            System.exit(1);
        }

        // build the class path
        String classpath = new File(
                appstartDir,
                launchProps.getProperty("app.class.path", "")).getPath().concat(File.pathSeparator);

        File libsDir = new File(appstartDir, launchProps.getProperty(
                "app.libs.dir", "lib"));
        log.info("libs dir: " + libsDir);
        if (libsDir.isDirectory()) {
            File[] libs = libsDir.listFiles();
            for (File lib : libs) {
                classpath = classpath.concat(lib.getAbsolutePath()).
                        concat(File.pathSeparator);
            }
        }
        log.info("classpath = " + classpath);

        List<String> vmOptions = new ArrayList<String>();
        String vmOptionsString = launchProps.getProperty(
                APP_VM_OPTIONS, "");
        log.info("vmoptions = " + vmOptionsString);
        for (StringTokenizer st = new StringTokenizer(vmOptionsString); st.hasMoreTokens();) {
            String token = st.nextToken();
            vmOptions.add(token);
        }

        // build system properties based on the key=value of the appstart.properties file
        List<String> systemProps = new ArrayList<String>();
        for (String prop : launchProps.stringPropertyNames()) {
            systemProps.add(String.format(
                    "-D%s=%s", prop, launchProps.getProperty(prop)));
        }
        String mainClass = launchProps.getProperty(
                APPSTART_MAIN_CLASS);
        log.info("main class = " + mainClass);
        if (mainClass == null) {
            log.severe("Missing " + APPSTART_MAIN_CLASS + " config entry");
            if (!GraphicsEnvironment.isHeadless()) {
                javax.swing.JOptionPane.showMessageDialog(
                        null,
                        "Missing config entry " + APPSTART_MAIN_CLASS,
                        "Error starting application",
                        javax.swing.JOptionPane.ERROR_MESSAGE);
            }
            System.exit(1);
        }

        // flag to follow child process output
        Boolean follow = Boolean.parseBoolean(launchProps.getProperty(
                "app.follow", "false"));
        File splashFile = new File(appstartDir, DEFAULT_SPLASH_IMG);

        ArrayList<String> cmd = new ArrayList<String>();
        cmd.add(java.getAbsolutePath());
        cmd.addAll(vmOptions);
        if (splashFile.isFile()) {
            log.info("splash file: " + splashFile.getAbsolutePath());
            cmd.add("-splash:" + splashFile.getAbsolutePath());
        }
        cmd.addAll(systemProps);
        cmd.add("-cp");
        cmd.add(classpath);
        cmd.add(mainClass);
        for (int i = 0; i < args.length; i++) {
            cmd.add(args[i]);
        }
        log.info("command line:\n" + cmd);
        ProcessBuilder pb = new ProcessBuilder(cmd);

        pb.redirectErrorStream(true);
        child = pb.start();

        SplashScreen splash = SplashScreen.getSplashScreen();
        if (splash != null) {
            splash.close();
        }
        if (follow) {
            log.info("starting follower thread");
            new FollowerThread().start();
        }
//        System.out.println(Runtime.getRuntime().maxMemory());
//        System.out.println("Bootstrap terminating");
        //System.exit(0);
    }

    private static Properties findLaunchProperties(File file, Properties launchProps) throws IOException {
//        System.out.println("searching launch.properties in " + file);
        File propFile = new File(file, APPSTART_FILE);
        if (propFile.isFile()) {
//            System.out.println("found launch.properties here");
            launchProps = new Properties(launchProps);
            InputStream in = new FileInputStream(propFile);
            launchProps.load(in);
            in.close();

            return launchProps;
        } else {
            return launchProps;
        }
    }

    /**
     * Writes on stdout the output of the child thread
     */
    static class FollowerThread extends Thread {

        public FollowerThread() {
            setPriority(MIN_PRIORITY);
        }

        @Override
        public void run() {
            InputStream stream = child.getInputStream();
            byte[] buf = new byte[1024];
            try {
                int read = stream.read(buf);
                while (read >= 0) {
                    System.out.write(buf, 0, read);
                    read = stream.read(buf);
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {
            }
        }
    }
}
