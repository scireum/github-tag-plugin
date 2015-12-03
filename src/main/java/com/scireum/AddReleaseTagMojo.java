/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

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
@Mojo(name = "addReleaseTag", defaultPhase = LifecyclePhase.DEPLOY)
public class AddReleaseTagMojo extends AbstractGithubTagMojo {

    @Parameter(property = "github.addReleaseTag.skip")
    private boolean skip;

    @Parameter(property = "project.version")
    private String version;

    @Override
    protected void executeWithConfig() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping (github.addReleaseTag.skip is true).");
            return;
        }
        String commit = getCurrentCommit();
        if (Strings.isNullOrEmpty(commit)) {
            getLog().info("No git commit hash (github.commitHash) is present. Skipping....");
            return;
        }
        if (version.endsWith("SNAPSHOT")) {
            getLog().info("Not going to verify a SNAPSHOT version. Skipping...");
            return;
        }

        try {
            JSONObject input = new JSONObject();
            input.put("sha", commit);
            input.put("ref", "refs/tags/" + version);
            JSONObject obj = call("POST", "/git/refs", input);
            if (obj.get("state") != Integer.valueOf(201)) {
                getLog().warn("Cannot create tag: " + obj.get("message"));
            } else {
                getLog().info("Successfully create a release tag...");
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Cannot create tag: " + e.getMessage(), e);
        }
    }
}
