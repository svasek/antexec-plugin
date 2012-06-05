package hudson.plugins.antexec;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.*;

import java.io.IOException;
import java.io.PrintStream;

class AntExecUtils {

    public static FilePath getAntHome(AbstractBuild build, PrintStream logger, EnvVars env, Boolean isUnix, String antHome, Boolean verbose) throws IOException, InterruptedException {
        String envAntHome = env.get("ANT_HOME");
        String useAntHome = null;
        String antExe = isUnix ? "/bin/ant" : "\\bin\\ant.bat";

        //Setup ANT_HOME from Environment or job configuration screen
        if (envAntHome != null && envAntHome.length() > 0 && !envAntHome.equals("")) {
            useAntHome = envAntHome;
            if (verbose != null && verbose) logger.println(Messages.AntExec_AntHomeEnvVarFound(useAntHome));
        } else {
            if (verbose != null && verbose) logger.println(Messages.AntExec_AntHomeEnvVarNotFound());
        }

        //Forcing configured ANT_HOME in Environment
        if (antHome != null && antHome.length() > 0 && !antHome.equals("")) {
            if (useAntHome != null) {
                logger.println(Messages._AntExec_AntHomeReplacing(useAntHome, antHome));
            } else {
                logger.println(Messages._AntExec_AntHomeReplacing("", antHome));
                if (build.getBuiltOn().createPath(antHome).exists()) {
                    logger.println(build.getBuiltOn().createPath(antHome) + " exists!");
                }
            }
            useAntHome = antHome;
            //Change ANT_HOME in environment
            env.put("ANT_HOME", useAntHome);
            logger.println(Messages.AntExec_EnvironmentChanged("ANT_HOME", useAntHome));

            //Add ANT_HOME/bin into the environment PATH
            String newAntPath = isUnix ? useAntHome + "/bin:" + env.get("PATH") : useAntHome + "\\bin;" + env.get("PATH");
            env.put("PATH", newAntPath);
            if (verbose != null && verbose) logger.println(Messages.AntExec_EnvironmentAdded("PATH", newAntPath));

            //Add JAVA_HOME/bin into the environment PATH
            if (env.containsKey("JAVA_HOME")) {
                env.put("PATH", isUnix ? env.get("JAVA_HOME") + "/bin:" + env.get("PATH") : env.get("JAVA_HOME") + "\\bin;" + env.get("PATH"));
                if (verbose != null && verbose)
                    logger.println(Messages.AntExec_EnvironmentAdded("PATH", isUnix ? env.get("JAVA_HOME") + "/bin" : env.get("JAVA_HOME") + "\\bin"));
            }
        }

        if (useAntHome == null) {
            logger.println(Messages.AntExec_AntHomeValidation());
            logger.println("Trying to run ant from PATH ...");
            return build.getBuiltOn().createPath("ant");
        } else {
            if (build.getBuiltOn().createPath(useAntHome + antExe).exists()) {
                if (verbose != null && verbose)
                    logger.println(build.getBuiltOn().createPath("OK:" + useAntHome + antExe) + " exists!");
            }
            return build.getBuiltOn().createPath(useAntHome + antExe);
        }
    }


    static String makeBuildFileXml(String scriptSource, String extendedScriptSource, String scriptName) {
        StringBuilder sb = new StringBuilder();
        String myScripName = AntExec.buildXml;
        if (scriptName != null && scriptName.length() > 0 && !scriptName.equals("")) {
            myScripName = scriptName;
        }
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        sb.append("<project default=\"" + myScripName + "\" xmlns:antcontrib=\"antlib:net.sf.antcontrib\" basedir=\".\">\n\n");
        sb.append("<target name=\"" + myScripName + "\">\n");
        sb.append("<!-- This is default target entered in the first textarea -->\n");
        sb.append(scriptSource);
        sb.append("\n</target>\n\n");
        sb.append("<!-- This is extended script source entered in the second textarea-->\n");
        sb.append(extendedScriptSource);
        sb.append("\n</project>\n");
        return sb.toString();
    }

    static FilePath makeBuildFile(String scriptName, String targetSource, String extendedScriptSource, AbstractBuild build) throws IOException, InterruptedException {
        String myScripName = AntExec.buildXml;
        if (scriptName != null && scriptName.length() > 0 && !scriptName.equals("")) {
            myScripName = scriptName;
        }
        FilePath buildFile = new FilePath(build.getWorkspace(), myScripName);
        buildFile.write(makeBuildFileXml(targetSource, extendedScriptSource, scriptName), null);
        return buildFile;
    }
}
