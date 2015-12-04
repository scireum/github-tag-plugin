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
 * Adds a CI (or custom name) tag to the latest commit being build by the build system.
 */
@Mojo(name = "tagBuild", defaultPhase = LifecyclePhase.DEPLOY)
public class TagBuildMojo extends AbstractGithubTagMojo {

    @Parameter(property = "github.tagBuild.skip")
    private boolean skip;

    @Parameter(defaultValue = "CI")
    protected String tagName = "CI";

    @Override
    protected void executeWithConfig() throws MojoExecutionException {
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
                obj = call("POST", "/git/refs/tags/" + getEffectiveTagName(tagName), input);
            } catch (Exception e) {
                getLog().debug(e);
                getLog().info("Cannot update tag....Trying to create...");
            }
            if (obj == null || hasBadStatus(obj)) {
                input.put("ref", "refs/tags/" + getEffectiveTagName(tagName));
                obj = call("POST", "/git/refs", input);
                if (hasBadStatus(obj)) {
                    getLog().warn("Cannot create tag: " + obj.get("message"));
                }
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Cannot create tag ("
                                             + getEffectiveTagName(tagName)
                                             + "): "
                                             + e.getMessage(), e);
        }
    }
}
