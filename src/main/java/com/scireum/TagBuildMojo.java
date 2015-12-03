package com.scireum;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Strings;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Created by aha on 03.12.15.
 */
@Mojo(name = "tagBuild", defaultPhase = LifecyclePhase.DEPLOY)
public class TagBuildMojo extends AbstractGithubTagMojo {

    @Parameter(property = "github.tagBuild.skip")
    private boolean skip;

    @Override
    protected void executeWithConfig() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping (github.tagBuild.skip is true).");
            return;
        }
        String commit = getCurrentCommit();
        if (Strings.isNullOrEmpty(commit)) {
            getLog().info("No git commit hash (github.coomitHash) is present. Skipping....");
            return;
        }

        JSONObject input = new JSONObject();
        input.put("sha", commit);
        input.put("force", true);
        try {
            JSONObject obj = null;
            try {
                obj = call("POST", "/git/refs/tags/" + tagName, input);
            } catch (Exception e) {
                getLog().debug(e);
                getLog().info("Cannot update tag....Trying to create...");
            }
            if (obj == null || !Integer.valueOf(200).equals(obj.get("state"))) {
                input.put("ref", "refs/tags/" + tagName);
                obj = call("POST", "/git/refs", input);
                if (obj.get("state") != Integer.valueOf(201)) {
                    getLog().warn("Cannot create tag: " + obj.get("message"));
                }
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Cannot create tag: " + e.getMessage(), e);
        }
    }

}
