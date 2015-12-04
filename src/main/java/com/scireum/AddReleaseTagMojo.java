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
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Adds a tag for the current git commit hash containig the current project version.
 */
@Mojo(name = "addReleaseTag", defaultPhase = LifecyclePhase.DEPLOY)
public class AddReleaseTagMojo extends AbstractGithubTagMojo {

    /**
     * Determines if the goal should be skipped
     */
    @Parameter(property = "github.addReleaseTag.skip")
    private boolean skip;

    @Override
    protected void executeWithConfig() throws MojoExecutionException {
        if (skip) {
            getLog().info("Skipping (github.addReleaseTag.skip is true).");
            return;
        }
        String commit = getCurrentCommit();
        if (Strings.isNullOrEmpty(commit)) {
            getLog().info("No git commit hash (github.commitHash) is present. Skipping....");
            return;
        }
        if (project.getVersion().endsWith("SNAPSHOT")) {
            getLog().info("Not going to verify a SNAPSHOT version. Skipping...");
            return;
        }

        try {
            JSONObject input = new JSONObject();
            input.put("sha", commit);
            input.put("ref", "refs/tags/" + getEffectiveTagName(project.getVersion()));
            JSONObject obj = call("POST", "/git/refs", input);
            if (hasBadStatus(obj)) {
                getLog().warn("Cannot create tag: " + obj.get("message"));
            } else {
                getLog().info("Successfully created a release tag...");
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Cannot create tag: " + e.getMessage(), e);
        }
    }

}
