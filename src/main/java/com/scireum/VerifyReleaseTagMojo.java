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
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.FileNotFoundException;

/**
 * Verifies that no tag with the current (to be deployed) project version already exisits.
 * <p>
 * This can be used to block accidential redeployments of maven artifacts as these will not be picked by the
 * client.
 */
@Mojo(name = "verifyReleaseTag", defaultPhase = LifecyclePhase.VALIDATE)
public class VerifyReleaseTagMojo extends AbstractGithubTagMojo {

    /**
     * Determines if the goal should be skipped
     */
    @Parameter(property = "github.verifyReleaseTag.skip")
    private boolean skip;

    /**
     * The current session to check if a deploy is scheduled
     */
    @Parameter(defaultValue = "${session}")
    private MavenSession session;

    private boolean tagVerified = false;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping (github.tag.verifyReleaseTag is true).");
            return;
        }
        if (!session.getGoals().contains("deploy")) {
            getLog().info("Skipping as no deploy is scheduled...");
            return;
        }
        if (project.getVersion().endsWith("SNAPSHOT")) {
            getLog().info("Not going to verify a SNAPSHOT version. Skipping...");
            return;
        }
        super.execute();

        if (!tagVerified) {
            throw new MojoExecutionException(
                    "Cannot verify if the version was already released as either the tag cannot be verified "
                    + "or I would be unable to set it later. Aborting deploy...");
        }
    }

    @Override
    protected void executeWithConfig() throws MojoExecutionException {
        if (Strings.isNullOrEmpty(getCurrentCommit())) {
            throw new MojoExecutionException(
                    "No commit hash (github.commitHash) is available. I would not be able to tag this release "
                    + "and therefore abort!");
        }
        try {
            JSONObject obj = call("GET", "/git/refs/tags/" + getEffectiveTagName(project.getVersion()), null);
            if (!hasBadStatus(obj)) {
                if (("refs/tags/" + getEffectiveTagName(project.getVersion())).equals(obj.get("ref"))) {
                    throw new MojoExecutionException("A release tag for "
                                                     + getEffectiveTagName(project.getVersion())
                                                     + " is already present! Aborting build");
                }
            }
        } catch (MojoExecutionException e) {
            throw e;
        } catch (FileNotFoundException e) {
            getLog().debug("Ignoring: " + e.getMessage());
        } catch (Exception e) {
            throw new MojoExecutionException("Cannot create tag: " + e.getMessage(), e);
        }

        tagVerified = true;
        getLog().info("Version " + getEffectiveTagName(project.getVersion()) + " seems unreleased so far...");
    }
}
